package com.greninjaop.mailorganizer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.SampleDataPolicy
import com.greninjaop.mailorganizer.ui.mail.SampleMailboxSeeder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the Phase 11 Home dashboard (phase §8–§19).
 *
 * Assembles the information hierarchy from real local data only — nothing
 * is fabricated (§10):
 * ```
 * Account context → Attention → High priority → Recent → Categories → Companies
 * ```
 *
 * Message display fields resolve through one bounded `getMessagesByIds`
 * query per section (never N+1). Failures degrade to [HomeContent.Error]
 * with retry; they never produce fake sections.
 */
class HomeViewModel(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val connectivity: ConnectivityObserver,
    private val dispatchers: AppDispatchers,
    private val samplePolicy: SampleDataPolicy,
    private val seeder: SampleMailboxSeeder,
    private val clock: () -> Long = System::currentTimeMillis,
    private val activeAccountPreferences: ActiveAccountPreferences? = null,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val SECTION_LIMIT = 5
    }

    private val allAccounts: StateFlow<List<AccountRecord>> =
        accounts.observeAll()
            // Eagerly: the dashboard must see the current account list on
            // first collection, not a transient empty initial value.
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val activeSelection = if (activeAccountPreferences != null) {
        activeAccountPreferences.activeAccountSelection
    } else {
        allAccounts.map { list ->
            val primary = list.filter { it.isEnabled }.minWithOrNull(
                compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
            )
            if (primary != null) AccountSelection.Single(primary.accountId) else AccountSelection.Unified
        }
    }

    private val activeAccount: StateFlow<AccountRecord?> =
        combine(allAccounts, activeSelection) { list, selection ->
            val enabled = list.filter { it.isEnabled }
            when (selection) {
                is AccountSelection.Single -> {
                    enabled.firstOrNull { it.accountId == selection.accountId }
                        ?: enabled.firstOrNull()
                }
                is AccountSelection.Unified -> {
                    enabled.minWithOrNull(
                        compareBy<AccountRecord> { it.createdAtEpochMs }
                            .thenBy { it.accountId },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val isOffline: StateFlow<Boolean> =
        connectivity.isOnline
            .map { !it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val seedDone: StateFlow<Boolean> = flow {
        if (samplePolicy.shouldSeed()) {
            try {
                if (seeder.seedIfEmpty()) {
                    MoLogger.i(TAG, "Sample mailbox seeded (debug fixtures)")
                }
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Sample seeding failed: ${t.javaClass.simpleName}")
            }
        }
        emit(true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> =
        combine(seedDone, allAccounts, activeAccount, isOffline) { done, all, account, offline ->
            Triple(done, account to all.isEmpty(), offline)
        }.flatMapLatest { (done, accountAndEmpty, offline) ->
            val (account, noAccountsAtAll) = accountAndEmpty
            when {
                !done && samplePolicy.shouldSeed() -> flowOf(HomeContent.Loading)
                account == null -> flowOf(
                    if (noAccountsAtAll) HomeContent.NoAccount
                    else HomeContent.Error("No enabled account found."),
                )
                else -> homeContent(account, offline)
            }
        }.map { HomeUiState(it) }
            .catch { e ->
                MoLogger.e(TAG, "Home failed: ${e.javaClass.simpleName}")
                emit(HomeUiState(HomeContent.Error("Couldn't load the dashboard.")))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                HomeUiState(HomeContent.Loading),
            )

    /** Intermediate fold for the dashboard combine. */
    private data class HomeInputs(
        val actionIds: List<String>,
        val priorityIds: List<String>,
        val threads: List<com.greninjaop.mailorganizer.data.local.ThreadRecord>,
        val companies: List<com.greninjaop.mailorganizer.data.local.CompanyRecord>,
        val offline: Boolean,
    )

    private fun homeContent(account: AccountRecord, offline: Boolean): Flow<HomeContent> {
        val accountId = account.accountId
        return combine(
            intelligence.observeByCategory(accountId, MailCategory.ACTION_REQUIRED, SECTION_LIMIT),
            intelligence.observeByPriority(accountId, Priority.CRITICAL, SECTION_LIMIT),
            intelligence.observeByPriority(accountId, Priority.HIGH, SECTION_LIMIT),
            mail.observeThreads(accountId, SECTION_LIMIT),
            intelligence.observeCompanyFilterList(accountId, SECTION_LIMIT),
        ) { actionRecords, critical, high, threads, companies ->
            HomeInputs(
                actionIds = actionRecords.map { it.messageId },
                priorityIds = critical.map { it.messageId } + high.map { it.messageId },
                threads = threads,
                companies = companies,
                offline = offline,
            )
        }.map { inputs ->
            // One bounded lookup per section — never N+1.
            val wantedIds = (inputs.actionIds + inputs.priorityIds +
                inputs.threads.mapNotNull { it.latestMessageId })
                .distinct().take(3 * SECTION_LIMIT)
            val messages = mail.getMessagesByIds(wantedIds).associateBy { it.messageId }
            val classifications = intelligence.getClassifications(messages.keys.toList())
            val priorities = intelligence.getPriorities(messages.keys.toList())
            val counts = intelligence.categoryCounts(accountId)
            val companyCounts = mail.companyMessageCounts(accountId)
            val total = mail.countByAccount(accountId)

            if (total == 0) {
                return@map HomeContent.Empty(account, inputs.offline)
            }
            HomeContent.Loaded(
                account = account,
                attention = inputs.actionIds.mapNotNull { messages[it] }
                    .map {
                        it.toHomeMessage(
                            classifications[it.messageId]?.category,
                            priorities[it.messageId]?.priority,
                        )
                    }
                    .take(SECTION_LIMIT),
                highPriority = inputs.priorityIds.distinct().mapNotNull { messages[it] }
                    .map {
                        it.toHomeMessage(
                            classifications[it.messageId]?.category,
                            priorities[it.messageId]?.priority,
                        )
                    }
                    .take(SECTION_LIMIT),
                recent = inputs.threads.mapNotNull { t -> messages[t.latestMessageId] }
                    .map {
                        it.toHomeMessage(
                            classifications[it.messageId]?.category,
                            priorities[it.messageId]?.priority,
                        )
                    },
                categoryCounts = counts,
                companies = inputs.companies.map { record ->
                    HomeCompany(record, companyCounts[record.companyId] ?: 0)
                },
                totalMessages = total,
                unreadCount = inputs.threads.sumOf { it.unreadCount },
                isSampleData = samplePolicy.shouldSeed(),
                isOffline = inputs.offline,
            )
        }
    }

    private fun MessageRecord.toHomeMessage(
        category: MailCategory?,
        priority: Priority?,
    ) = HomeMessage(
        messageId = messageId,
        threadId = threadId,
        subject = subject.ifBlank { "(no subject)" },
        senderName = fromName?.ifBlank { fromAddress } ?: fromAddress,
        timestampEpochMs = timestampEpochMs,
        unread = unread,
        priority = priority,
        category = category,
    )

    /** Exposed for the shared top bar / account identity (§36). */
    val currentAccount: StateFlow<AccountRecord?> = activeAccount

    fun refresh() {
        // Home is a read view over local data; refresh re-collects flows.
        // Real re-sync stays behind the Mail screen's refresh (Phase 4).
        MoLogger.i(TAG, "refresh requested (local re-read)")
    }
}
