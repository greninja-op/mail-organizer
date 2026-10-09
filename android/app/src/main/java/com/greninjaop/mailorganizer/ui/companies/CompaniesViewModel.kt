package com.greninjaop.mailorganizer.ui.companies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Company list state (Phase 11, phase §25–§28).
 */
sealed interface CompaniesContent {
    data object Loading : CompaniesContent
    data class Ready(
        val companies: List<CompanyEntry>,
        val account: AccountRecord,
    ) : CompaniesContent
    data class Empty(val hasAccount: Boolean) : CompaniesContent
    data class Error(val message: String) : CompaniesContent
}

/** One company row: record + honest total message count. */
data class CompanyEntry(
    val record: CompanyRecord,
    val messageCount: Int,
)

/** One row in a company detail list. */
data class CompanyMessage(
    val messageId: String,
    val threadId: String,
    val subject: String,
    val senderName: String,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val category: MailCategory?,
)

sealed interface CompanyDetailContent {
    data object Loading : CompanyDetailContent
    data class Loaded(
        val company: CompanyRecord,
        val messages: List<CompanyMessage>,
        val categoryCounts: Map<MailCategory, Int>,
    ) : CompanyDetailContent
    data class Error(val message: String) : CompanyDetailContent
    data object NotFound : CompanyDetailContent
}

data class CompaniesUiState(
    val content: CompaniesContent = CompaniesContent.Loading,
)

/**
 * Backs the Companies destination (Phase 11).
 *
 * Company → senders → messages (§25). Counts are real local counts;
 * companies with no messages are filtered out (never fabricated rows).
 * Unknown senders are never hidden (§28) — mail without a detected
 * company simply has no company row.
 */
class CompaniesViewModel(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    private val activeAccountPreferences: ActiveAccountPreferences? = null,
) : ViewModel() {

    companion object {
        private const val TAG = "CompaniesViewModel"
        private const val LIST_LIMIT = 100
        private const val DETAIL_LIMIT = 50
    }

    private val activeSelection = activeAccountPreferences?.activeAccountSelection
        ?: flowOf(AccountSelection.Unified)

    private val activeAccount: StateFlow<AccountRecord?> =
        combine(accounts.observeAll(), activeSelection) { list, selection ->
            val enabled = list.filter { it.isEnabled }
            when (selection) {
                is AccountSelection.Single -> {
                    if (selection.accountId != null) {
                        enabled.firstOrNull { it.accountId == selection.accountId }
                            ?: enabled.firstOrNull()
                    } else {
                        enabled.minWithOrNull(
                            compareBy<AccountRecord> { it.createdAtEpochMs }
                                .thenBy { it.accountId },
                        )
                    }
                }
                is AccountSelection.Unified -> {
                    enabled.minWithOrNull(
                        compareBy<AccountRecord> { it.createdAtEpochMs }
                            .thenBy { it.accountId },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CompaniesUiState> =
        activeAccount.flatMapLatest { account ->
            if (account == null) {
                flowOf(CompaniesUiState(CompaniesContent.Empty(hasAccount = false)))
            } else {
                intelligence.observeCompanyFilterList(account.accountId, LIST_LIMIT)
                    .map { records ->
                        val counts = mail.companyMessageCounts(account.accountId)
                        val entries = records.map { record ->
                            CompanyEntry(record, counts[record.companyId] ?: 0)
                        }.filter { it.messageCount > 0 }
                        val content = if (entries.isEmpty()) {
                            CompaniesContent.Empty(hasAccount = true)
                        } else {
                            CompaniesContent.Ready(entries, account)
                        }
                        CompaniesUiState(content)
                    }
                    .catch { e ->
                        MoLogger.e(TAG, "Companies failed: ${e.javaClass.simpleName}")
                        emit(CompaniesUiState(CompaniesContent.Error("Couldn't load companies.")))
                    }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CompaniesUiState(CompaniesContent.Loading),
        )

    private val selectedCompanyId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detail: StateFlow<CompanyDetailContent> =
        combine(activeAccount, selectedCompanyId) { account, companyId ->
            account to companyId
        }.flatMapLatest { (account, companyId) ->
            if (account == null || companyId == null) {
                flowOf(CompanyDetailContent.Loading)
            } else {
                combine(
                    mail.observeMessagesByCompany(account.accountId, companyId, DETAIL_LIMIT),
                    intelligence.observeCompanyFilterList(account.accountId, LIST_LIMIT),
                ) { messages, companies ->
                    val company = companies.firstOrNull { it.companyId == companyId }
                        ?: return@combine CompanyDetailContent.NotFound
                    val classifications =
                        intelligence.getClassifications(messages.map { it.messageId })
                    val items = messages.map { m ->
                        CompanyMessage(
                            messageId = m.messageId,
                            threadId = m.threadId,
                            subject = m.subject.ifBlank { "(no subject)" },
                            senderName = m.fromName?.ifBlank { m.fromAddress } ?: m.fromAddress,
                            timestampEpochMs = m.timestampEpochMs,
                            unread = m.unread,
                            category = classifications[m.messageId]?.category,
                        )
                    }
                    val categoryCounts = items
                        .groupingBy { it.category ?: MailCategory.UNCLASSIFIED }
                        .eachCount()
                    CompanyDetailContent.Loaded(company, items, categoryCounts)
                }.catch { e ->
                    MoLogger.e(TAG, "Company detail failed: ${e.javaClass.simpleName}")
                    emit(CompanyDetailContent.Error("Couldn't load this company."))
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CompanyDetailContent.Loading,
        )

    fun selectCompany(companyId: String?) {
        selectedCompanyId.value = companyId
    }

    /** Exposed for the shared top bar (§36). */
    val currentAccount: StateFlow<AccountRecord?> = activeAccount
}
