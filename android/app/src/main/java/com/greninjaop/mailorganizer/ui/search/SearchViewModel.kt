package com.greninjaop.mailorganizer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.search.QueryParser
import com.greninjaop.mailorganizer.core.search.SearchIndexState
import com.greninjaop.mailorganizer.core.search.SearchQuery
import com.greninjaop.mailorganizer.core.search.SearchResultType
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.SearchRepository
import com.greninjaop.mailorganizer.domain.search.SearchIndexMaintenance
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Search screen ViewModel (Phase 10).
 *
 * - Text input is debounced ([SEARCH_DEBOUNCE_MS]) so typing doesn't fan
 *   out database queries; FTS itself is fast enough that short queries
 *   feel instant after the debounce (phase §12, §63).
 * - The query is a structured [SearchQuery]; parsing lives in
 *   [QueryParser], never in the UI (phase §13).
 * - Index catch-up runs best-effort in the background on start and never
 *   blocks search (phase §56).
 * - Search queries are never logged (phase §76).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val accounts: AccountRepository,
    private val search: SearchRepository,
    private val searchIndex: SearchIndexMaintenance,
    private val connectivity: ConnectivityObserver,
    private val dispatchers: AppDispatchers,
    initialQuery: String = "",
) : ViewModel() {

    companion object {
        /** Debounce for live search (phase §12). */
        const val SEARCH_DEBOUNCE_MS = 300L

        private const val TAG = "SearchViewModel"
    }

    private val queryText = MutableStateFlow(initialQuery)
    private val filters = MutableStateFlow(SearchFilters())
    private val resultType = MutableStateFlow(SearchResultType.MESSAGES)
    private val indexState = MutableStateFlow<SearchIndexState?>(null)
    private val indexing = MutableStateFlow(false)

    /**
     * Bumped to force the search flow to re-run without changing the
     * query (e.g. after an index rebuild) — `distinctUntilChanged` would
     * otherwise swallow it.
     */
    private val rerunNonce = MutableStateFlow(0)

    private val activeAccount: StateFlow<AccountRecord?> =
        accounts.observeEnabled()
            .map { list ->
                list.minWithOrNull(
                    compareBy<AccountRecord> { it.createdAtEpochMs }
                        .thenBy { it.accountId },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val isOffline: StateFlow<Boolean> =
        connectivity.isOnline
            .map { online -> !online }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Best-effort background index catch-up; never blocks the UI.
        viewModelScope.launch(dispatchers.io) {
            val account = activeAccount.value
                ?: return@launch
            indexing.value = true
            try {
                indexState.value = searchIndex.ensureIndexed(account.accountId)
            } finally {
                indexing.value = false
            }
        }
    }

    private data class QueryParams(
        val text: String,
        val filters: SearchFilters,
        val resultType: SearchResultType,
        val account: AccountRecord?,
    )

    private data class Params(
        val query: QueryParams,
        val indexState: SearchIndexState?,
        val indexing: Boolean,
        val offline: Boolean,
    )

    val state: StateFlow<SearchScreenState> =
        combine(
            queryText.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
            filters,
            resultType,
            activeAccount,
        ) { text, f, rt, account ->
            QueryParams(text, f, rt, account)
        }.combine(indexState) { q, idx -> q to idx }
            .combine(indexing) { (q, idx), idxing -> Triple(q, idx, idxing) }
            .combine(isOffline) { (q, idx, idxing), offline -> Params(q, idx, idxing, offline) }
            .combine(rerunNonce) { p, _ -> p }
            .flatMapLatest { params -> searchFlow(params) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                SearchScreenState(queryText = initialQuery),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun searchFlow(params: Params) = flow {
        val base = SearchScreenState(
            queryText = params.query.text,
            filters = params.query.filters,
            resultType = params.query.resultType,
            activeAccount = params.query.account,
            indexState = params.indexState,
            indexing = params.indexing,
            isOffline = params.offline,
            content = SearchContent.Landing,
        )
        val account = params.query.account
        if (account == null) {
            emit(base.copy(content = SearchContent.Error("No account available.")))
            return@flow
        }
        val parsed = QueryParser.parse(params.query.text)
        if (parsed.isEmpty && !params.query.filters.hasActive) {
            emit(base) // Landing — no DB work (phase §51).
            return@flow
        }
        emit(base.copy(content = SearchContent.Loading))
        val searchQuery = SearchQuery(
            rawText = params.query.text,
            accountId = account.accountId,
            category = params.query.filters.category,
            priority = params.query.filters.priority,
            actionRequiredOnly = params.query.filters.actionRequiredOnly,
            sender = params.query.filters.sender,
            companyId = params.query.filters.companyId,
            unreadOnly = params.query.filters.unreadOnly,
            hasAttachmentOnly = params.query.filters.hasAttachmentOnly,
            datePreset = params.query.filters.datePreset,
            resultType = params.query.resultType,
        )
        val content = try {
            val outcome = search.search(searchQuery)
            if (outcome.results.isEmpty()) {
                SearchContent.Empty(emptyHint(params.query.text, params.query.filters))
            } else {
                SearchContent.Results(outcome)
            }
        } catch (t: Throwable) {
            // Never log the query (phase §76) — only the failure class.
            MoLogger.e(TAG, "Search failed: ${t.javaClass.simpleName}")
            SearchContent.Error(
                "Search couldn't run. Your mail is safe — try rebuilding the index.",
            )
        }
        emit(base.copy(content = content))
    }

    private fun emptyHint(text: String, filters: SearchFilters): String {
        val what = if (text.isBlank()) "your filters" else "“${text.trim().take(60)}”"
        return "No messages found for $what. Try a different search or clear filters."
    }

    // ---- UI intents ----

    fun setQueryText(text: String) {
        queryText.value = text.take(QueryParser.MAX_QUERY_CHARS * 2)
    }

    fun clearQuery() {
        queryText.value = ""
    }

    fun setResultType(type: SearchResultType) {
        resultType.value = type
    }

    fun toggleActionRequired() {
        filters.value = filters.value.copy(
            actionRequiredOnly = !filters.value.actionRequiredOnly,
        )
    }

    fun toggleUnreadOnly() {
        filters.value = filters.value.copy(unreadOnly = !filters.value.unreadOnly)
    }

    fun toggleHasAttachment() {
        filters.value = filters.value.copy(
            hasAttachmentOnly = !filters.value.hasAttachmentOnly,
        )
    }

    fun setCategory(category: MailCategory?) {
        filters.value = filters.value.copy(category = category)
    }

    fun setPriority(priority: Priority?) {
        filters.value = filters.value.copy(priority = priority)
    }

    fun setDatePreset(preset: com.greninjaop.mailorganizer.core.search.SearchDatePreset) {
        filters.value = filters.value.copy(datePreset = preset)
    }

    fun setSenderFilter(sender: String?) {
        filters.value = filters.value.copy(sender = sender?.takeIf { it.isNotBlank() })
    }

    fun setCompanyFilter(companyId: String?, companyName: String?) {
        filters.value = filters.value.copy(companyId = companyId, companyName = companyName)
    }

    fun clearFilters() {
        filters.value = SearchFilters()
    }

    /** Error-state action: rebuilds the account's index from local data (§54). */
    fun rebuildIndex() {
        val accountId = activeAccount.value?.accountId ?: return
        viewModelScope.launch(dispatchers.io) {
            indexing.value = true
            try {
                searchIndex.rebuild(accountId)
                indexState.value = SearchIndexState.READY
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Index rebuild failed: ${t.javaClass.simpleName}")
                indexState.value = SearchIndexState.DEGRADED
            } finally {
                indexing.value = false
            }
            // Re-run the current query against the rebuilt index.
            rerunNonce.value += 1
        }
    }
}
