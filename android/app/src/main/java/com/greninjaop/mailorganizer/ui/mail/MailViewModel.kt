package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.data.sync.SyncOutcome
import com.greninjaop.mailorganizer.data.sync.SyncProgress
import com.greninjaop.mailorganizer.data.sync.SyncStage
import com.greninjaop.mailorganizer.data.sync.SyncTrigger
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Intermediate fold for the two-stage typed combine() in [MailViewModel]. */
private data class MailboxCore(
    val destination: MailboxDestination,
    val content: MailboxContent,
    val account: AccountRecord?,
    val all: List<AccountRecord>,
    val filter: String,
)

/**
 * Backs the Phase 6 mailbox screen.
 *
 * Architecture (phase §34–35):
 * ```
 * UI → MailViewModel → MailRepository/AccountRepository → DAO → Room
 * ```
 * UI state is one explicit [MailScreenState] with a sealed [MailboxContent];
 * no scattered isLoading/isError booleans.
 *
 * Read-only: no mark-read/star/archive actions (phase §6 — those are Gmail
 * write operations, Phase 22). The refresh action delegates to Phase 4's
 * [SyncCoordinator] and reports its honest outcome.
 */
class MailViewModel(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val classifyMailbox: ClassifyMailboxUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val connectivity: ConnectivityObserver,
    private val dispatchers: AppDispatchers,
    private val samplePolicy: SampleDataPolicy,
    private val seeder: SampleMailboxSeeder,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    companion object {
        /** Bounded page size — the list never loads a whole mailbox (§17). */
        const val PAGE_SIZE = 50
        private const val TAG = "MailViewModel"

        /**
         * Startup classification guards: the background pass waits for
         * seeding, then for an account, but never blocks the UI — both
         * waits are bounded.
         */
        private const val CLASSIFY_STARTUP_TIMEOUT_MS = 30_000L
        private const val CLASSIFY_ACCOUNT_TIMEOUT_MS = 10_000L
    }

    private val destination = MutableStateFlow(MailboxDestination.ALL_INBOX)
    private val filterText = MutableStateFlow("")
    private val pageLimit = MutableStateFlow(PAGE_SIZE)
    private val syncUi = MutableStateFlow<SyncUiState>(SyncUiState.Idle)

    /**
     * Active account: earliest-created enabled account. Deterministic and
     * documented; real account switching arrives in Phase 18. Until then the
     * UI always shows which account is active (phase §8) and data is strictly
     * scoped to it (phase §33).
     */
    private val allAccounts: StateFlow<List<AccountRecord>> =
        accounts.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activeAccount: StateFlow<AccountRecord?> =
        allAccounts
            .map { list ->
                list.filter { it.isEnabled }.minWithOrNull(
                    compareBy<AccountRecord> { it.createdAtEpochMs }
                        .thenBy { it.accountId },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Fixture seeding (debug only, never touches real data — see seeder).
     * The content pipeline waits for the seeding attempt so the first paint
     * never flashes a false empty state in debug builds.
     */
    private val seedDone: StateFlow<Boolean> = flow {
        if (samplePolicy.shouldSeed()) {
            try {
                if (seeder.seedIfEmpty()) {
                    MoLogger.i(TAG, "Sample mailbox seeded (debug fixtures)")
                }
            } catch (t: Throwable) {
                // Seeding must never break the inbox.
                MoLogger.e(TAG, "Sample seeding failed: ${t.javaClass.simpleName}")
            }
        }
        emit(true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content: StateFlow<MailboxContent> =
        combine(seedDone, allAccounts, destination, filterText, pageLimit) { done, all, dest, filter, limit ->
            val account = all.filter { it.isEnabled }.minWithOrNull(
                compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
            )
            // In debug the first paint waits for the seeding attempt.
            val resolved = done || !samplePolicy.shouldSeed()
            ContentKey(resolved, account, dest, filter.trim(), limit)
        }.flatMapLatest { key ->
            when {
                !key.resolved -> flowOf(MailboxContent.Loading)
                key.account == null -> flowOf(MailboxContent.Empty(EmptyKind.NO_MAIL))
                // Phase 7: classification-backed destinations are wired to
                // real data — PROMOTIONAL via the deterministic classifier,
                // SOCIAL/SPAM via Gmail's own labels. Never fabricated.
                key.dest == MailboxDestination.PROMOTIONAL ->
                    intelligence.observeByCategory(
                        key.account.accountId,
                        MailCategory.PROMOTIONS,
                        key.limit,
                    ).mapLatest { records ->
                        buildCategoryContent(records.map { it.messageId }, key)
                            ?: MailboxContent.Empty(EmptyKind.NO_PROMOTIONS)
                    }
                key.dest == MailboxDestination.SOCIAL ->
                    mail.observeByLabel(key.account.accountId, "CATEGORY_SOCIAL", key.limit)
                        .mapLatest { messages ->
                            buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_SOCIAL)
                        }
                key.dest == MailboxDestination.SPAM ->
                    mail.observeByLabel(key.account.accountId, "SPAM", key.limit)
                        .mapLatest { messages ->
                            buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_SPAM)
                        }
                key.dest == MailboxDestination.STARRED ->
                    mail.observeStarred(key.account.accountId, key.limit)
                        .mapLatest { messages ->
                            val items = messages.map { it.toMessageItem() }
                                .applyMessageFilter(key.filter)
                            if (items.isEmpty()) {
                                if (key.filter.isNotEmpty()) MailboxContent.Empty(EmptyKind.NO_FILTER_RESULTS)
                                else MailboxContent.Empty(EmptyKind.NO_STARRED)
                            } else MailboxContent.Messages(items)
                        }
                else ->
                    mail.observeThreads(key.account.accountId, key.limit)
                        .mapLatest { threads ->
                            buildThreadContent(threads, key)
                        }
            }.catch { t ->
                // Never leak internals (§14): user-facing message only.
                MoLogger.e(TAG, "Mailbox load failed: ${t.javaClass.simpleName}")
                emit(MailboxContent.Error("Couldn't load your mail. Please try again."))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MailboxContent.Loading)

    val state: StateFlow<MailScreenState> = combine(
        destination,
        content,
        activeAccount,
        allAccounts,
        filterText,
    ) { dest, cont, account, all, filter ->
        MailboxCore(dest, cont, account, all, filter)
    }.let { core ->
        // Typed combine() only goes to 5 flows in coroutines 1.9: fold the
        // remaining two in a second, still fully-typed combine.
        combine(core, syncUi, connectivity.isOnline) { c, sync, online ->
            MailScreenState(
                destination = c.destination,
                content = c.content,
                activeAccount = c.account,
                accountCount = c.all.size,
                isSampleData = c.all.any { SampleMailboxSeeder.isFixtureAccount(it.accountId) },
                isOffline = !online,
                syncUi = sync,
                filterText = c.filter,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MailScreenState(),
    )

    init {
        // Honest sync progress (stages, never fabricated percentages — §16).
        syncCoordinator.progress
            .onEach { progress ->
                if (progress is SyncProgress.Running) {
                    syncUi.value = SyncUiState.Syncing(stageText(progress.stage))
                }
            }
            .launchIn(viewModelScope)

        // Phase 7: classify new mail in the background (bounded, incremental).
        // Best-effort — classification must never break the inbox. Runs after
        // the fixture seeding attempt so debug sample mail is classified too.
        viewModelScope.launch(dispatchers.io) {
            try {
                withTimeoutOrNull(CLASSIFY_STARTUP_TIMEOUT_MS) {
                    seedDone.first { it }
                    val account = withTimeoutOrNull(CLASSIFY_ACCOUNT_TIMEOUT_MS) {
                        activeAccount.first { it != null }
                    }
                    if (account != null) {
                        val n = classifyMailbox.classifyNew(account.accountId)
                        if (n > 0) {
                            MoLogger.i(TAG, "Background classification: $n messages")
                        }
                    }
                }
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Background classification failed: ${t.javaClass.simpleName}")
            }
        }
    }

    // ---- Intents ----

    fun setDestination(dest: MailboxDestination) {
        destination.value = dest
        pageLimit.value = PAGE_SIZE
    }

    fun setFilterText(text: String) {
        filterText.value = text
    }

    fun clearFilter() {
        filterText.value = ""
    }

    fun loadMore() {
        pageLimit.value = pageLimit.value + PAGE_SIZE
    }

    fun consumeSyncResult() {
        if (syncUi.value is SyncUiState.Result) syncUi.value = SyncUiState.Idle
    }

    /**
     * User-initiated sync (phase §38): delegates to Phase 4's engine, which
     * dedups concurrent runs per account. The outcome is reported honestly —
     * with Phase 3 deferred, this surfaces "not connected" instead of
     * pretending to sync.
     */
    fun refresh() {
        val account = activeAccount.value ?: return
        if (syncUi.value is SyncUiState.Syncing) return
        viewModelScope.launch(dispatchers.io) {
            syncUi.value = SyncUiState.Syncing("Starting sync…")
            val outcome = syncCoordinator.syncNow(
                AccountId(account.accountId),
                SyncTrigger.MANUAL,
            )
            syncUi.value = SyncUiState.Result(
                when (outcome) {
                    is SyncOutcome.Success ->
                        "Sync complete — ${outcome.summary.messagesProcessed} messages processed."
                    is SyncOutcome.Failed -> friendlySyncError(outcome.error)
                    SyncOutcome.Cancelled -> "Sync cancelled."
                },
            )
        }
    }

    // ---- Internals ----

    private data class ContentKey(
        val resolved: Boolean,
        val account: AccountRecord?,
        val dest: MailboxDestination,
        val filter: String,
        val limit: Int,
    )

    /**
     * Builds message-list content for classification/label destinations.
     * Classification record ids resolve to messages with one bounded query;
     * stale ids (message deleted) are dropped silently.
     */
    private suspend fun buildCategoryContent(
        messageIds: List<String>,
        key: ContentKey,
    ): MailboxContent? {
        if (messageIds.isEmpty()) return null
        val messages = mail.getMessagesByIds(messageIds)
        return buildMessageContent(messages.map { it.toMessageItem() }, key)
    }

    /**
     * Shared message-list builder: applies the text filter and returns null
     * when there is nothing to show, so callers pick the honest empty kind
     * for their destination.
     */
    private fun buildMessageContent(
        items: List<MessageItem>,
        key: ContentKey,
    ): MailboxContent? {
        val filtered = items.applyMessageFilter(key.filter)
        if (filtered.isEmpty()) {
            return if (key.filter.isNotEmpty()) {
                MailboxContent.Empty(EmptyKind.NO_FILTER_RESULTS)
            } else {
                null
            }
        }
        return MailboxContent.Messages(filtered)
    }

    private suspend fun buildThreadContent(
        threads: List<com.greninjaop.mailorganizer.data.local.ThreadRecord>,
        key: ContentKey,
    ): MailboxContent {
        // One bounded query for the visible page's latest messages (§17).
        val latestById = mail.getMessagesByIds(
            threads.mapNotNull { it.latestMessageId },
        ).associateBy { it.messageId }
        val items = threads
            .map { it.toThreadItem(latestById[it.latestMessageId]) }
            .applyThreadFilter(key.filter)
        if (items.isEmpty()) {
            return if (key.filter.isNotEmpty()) {
                MailboxContent.Empty(EmptyKind.NO_FILTER_RESULTS)
            } else {
                MailboxContent.Empty(EmptyKind.NO_MAIL)
            }
        }
        return MailboxContent.Threads(items, hasMore = threads.size >= key.limit)
    }

    private fun List<ThreadItem>.applyThreadFilter(filter: String): List<ThreadItem> {
        if (filter.isEmpty()) return this
        return filter {
            it.senderDisplay.contains(filter, ignoreCase = true) ||
                it.subject.contains(filter, ignoreCase = true) ||
                it.snippet.contains(filter, ignoreCase = true)
        }
    }

    private fun List<MessageItem>.applyMessageFilter(filter: String): List<MessageItem> {
        if (filter.isEmpty()) return this
        return filter {
            it.fromName?.contains(filter, ignoreCase = true) == true ||
                it.fromAddress.contains(filter, ignoreCase = true) ||
                it.subject.contains(filter, ignoreCase = true) ||
                it.snippet.contains(filter, ignoreCase = true)
        }
    }

    private fun stageText(stage: SyncStage): String = when (stage) {
        SyncStage.CONNECTING -> "Connecting…"
        SyncStage.FETCHING -> "Fetching mail…"
        SyncStage.SAVING -> "Saving…"
        SyncStage.FINALIZING -> "Finishing…"
    }

    private fun friendlySyncError(error: MoError): String = when (error) {        is MoError.InvalidConfiguration ->
            "Gmail isn't connected yet — sync will work once you connect an account."
        is MoError.Network ->
            "No internet connection. Your synced mail is still available."
        is MoError.Authentication ->
            "Gmail authorization expired. Reconnect your account to sync."
        is MoError.RateLimited ->
            "Gmail is rate-limiting sync right now. Please try again in a bit."
        else -> "Sync failed. Please try again."
    }
}

/** Top-level screen state (phase §35). */
data class MailScreenState(
    val destination: MailboxDestination = MailboxDestination.ALL_INBOX,
    val content: MailboxContent = MailboxContent.Loading,
    val activeAccount: AccountRecord? = null,
    val accountCount: Int = 0,
    val isSampleData: Boolean = false,
    val isOffline: Boolean = false,
    val syncUi: SyncUiState = SyncUiState.Idle,
    val filterText: String = "",
)

/** Sync affordance state: idle, honestly-staged progress, or a result message. */
sealed interface SyncUiState {
    data object Idle : SyncUiState
    data class Syncing(val stageText: String) : SyncUiState
    data class Result(val message: String) : SyncUiState
}
