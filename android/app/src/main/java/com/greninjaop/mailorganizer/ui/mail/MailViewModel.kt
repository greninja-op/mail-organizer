package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.sync.AccountSyncStatus
import com.greninjaop.mailorganizer.data.sync.AccountSyncStatusEvaluator
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.data.sync.SyncOutcome
import com.greninjaop.mailorganizer.data.sync.SyncProgress
import com.greninjaop.mailorganizer.data.sync.SyncScheduler
import com.greninjaop.mailorganizer.data.sync.SyncStage
import com.greninjaop.mailorganizer.data.sync.SyncTimeFormatter
import com.greninjaop.mailorganizer.data.sync.SyncTrigger
import com.greninjaop.mailorganizer.data.sync.UnifiedSyncStatus
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import com.greninjaop.mailorganizer.domain.actions.GenerateActionsUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMailboxUseCase
import com.greninjaop.mailorganizer.domain.temporal.ExtractMailboxUseCase
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
    val isUnified: Boolean,
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
    private val companyIntelligence: CompanyIntelligenceUseCase,
    private val prioritizeMailbox: PrioritizeMailboxUseCase,
    private val extractMailbox: ExtractMailboxUseCase,
    private val generateActions: GenerateActionsUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val connectivity: ConnectivityObserver,
    private val dispatchers: AppDispatchers,
    private val samplePolicy: SampleDataPolicy,
    private val seeder: SampleMailboxSeeder,
    private val activeAccountPreferences: com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    private val syncScheduler: SyncScheduler? = null,
    private val syncStateRepository: SyncStateRepository? = null,
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
     * Selected company filter (Phase 8). Scoped to the current category:
     * switching destinations clears it, because company selection is a
     * filter *within* a category, not global navigation (requirements.md).
     */
    private val selectedCompany = MutableStateFlow<String?>(null)

    /**
     * Action-required view (Phase 9). When on, the content shows mail
     * classified ACTION_REQUIRED by the deterministic engine — a global
     * "what needs my attention" view. Cleared on destination change like
     * the company filter.
     */
    private val actionRequiredOnly = MutableStateFlow(false)

    /**
     * Active account: earliest-created enabled account. Deterministic and
     * documented; real account switching arrives in Phase 18. Until then the
     * UI always shows which account is active (phase §8) and data is strictly
     * scoped to it (phase §33).
     */
    private val allAccounts: StateFlow<List<AccountRecord>> =
        accounts.observeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSelection: StateFlow<com.greninjaop.mailorganizer.data.prefs.AccountSelection> =
        if (activeAccountPreferences != null) {
            activeAccountPreferences.activeSelection
                .stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified,
                )
        } else {
            allAccounts.map { list ->
                val primary = list.filter { it.isEnabled }.minWithOrNull(
                    compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
                )
                if (primary != null) com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single(primary.accountId)
                else com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
            }.stateIn(viewModelScope, SharingStarted.Eagerly, com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified)
        }

    private val activeAccount: StateFlow<AccountRecord?> =
        combine(allAccounts, activeSelection) { list, selection ->
            val enabled = list.filter { it.isEnabled }
            when (selection) {
                is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single -> {
                    enabled.firstOrNull { it.accountId == selection.accountId }
                        ?: enabled.minWithOrNull(compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId })
                }
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified -> {
                    if (activeAccountPreferences == null) {
                        enabled.minWithOrNull(compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId })
                    } else {
                        null
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
        combine(
            seedDone,
            allAccounts,
            activeSelection,
            destination,
            filterText,
        ) { done, all, selection, dest, filter ->
            val enabled = all.filter { it.isEnabled }
            val account = when (selection) {
                is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single ->
                    enabled.firstOrNull { it.accountId == selection.accountId }
                        ?: enabled.minWithOrNull(compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId })
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified -> {
                    if (activeAccountPreferences == null) {
                        enabled.minWithOrNull(compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId })
                    } else {
                        null
                    }
                }
            }
            val isUnified = activeAccountPreferences != null && selection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
            // In debug the first paint waits for the seeding attempt.
            val resolved = done || !samplePolicy.shouldSeed()
            ContentKey(resolved, account, isUnified, dest, filter.trim(), PAGE_SIZE, null)
        }.combine(pageLimit) { key, limit ->
            key.copy(limit = limit)
        }.combine(selectedCompany) { key, companyId ->
            // Phase 8: the company filter rides along in the content key so
            // the list queries the exact company+destination slice.
            key.copy(companyId = companyId)
        }.combine(actionRequiredOnly) { key, actionOnly ->
            // Phase 9: the action-required view rides along too.
            key.copy(actionRequiredOnly = actionOnly)
        }.flatMapLatest { key ->
            when {
                !key.resolved -> flowOf(MailboxContent.Loading)
                !key.isUnified && key.account == null -> flowOf(MailboxContent.Empty(EmptyKind.NO_MAIL))
                // Unified mode presentation
                key.isUnified -> {
                    when (key.dest) {
                        MailboxDestination.STARRED ->
                            mail.observeUnifiedStarred(key.limit)
                                .mapLatest { messages ->
                                    val items = messages.map { it.toMessageItem() }
                                        .applyMessageFilter(key.filter)
                                    if (items.isEmpty()) {
                                        if (key.filter.isNotEmpty()) MailboxContent.Empty(EmptyKind.NO_FILTER_RESULTS)
                                        else MailboxContent.Empty(EmptyKind.NO_STARRED)
                                    } else MailboxContent.Messages(items)
                                }
                        else ->
                            mail.observeUnifiedThreads(key.limit)
                                .mapLatest { threads ->
                                    buildThreadContent(threads, key)
                                }
                    }
                }
                // Account-scoped presentation (key.account != null)
                key.actionRequiredOnly ->
                    intelligence.observeByCategory(
                        key.account!!.accountId,
                        MailCategory.ACTION_REQUIRED,
                        key.limit,
                    ).mapLatest { records ->
                        buildCategoryContent(records.map { it.messageId }, key)
                            ?: MailboxContent.Empty(EmptyKind.NO_ACTION_REQUIRED)
                    }
                key.dest == MailboxDestination.PROMOTIONAL ->
                    if (key.companyId != null) {
                        mail.observeMessagesByCompanyAndCategory(
                            key.account!!.accountId,
                            MailCategory.PROMOTIONS,
                            key.companyId,
                            key.limit,
                        ).mapLatest { messages ->
                            buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_COMPANY_RESULTS)
                        }
                    } else {
                        intelligence.observeByCategory(
                            key.account!!.accountId,
                            MailCategory.PROMOTIONS,
                            key.limit,
                        ).mapLatest { records ->
                            buildCategoryContent(records.map { it.messageId }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_PROMOTIONS)
                        }
                    }
                key.dest == MailboxDestination.SOCIAL ->
                    if (key.companyId != null) {
                        mail.observeMessagesByCompanyAndLabel(
                            key.account!!.accountId,
                            key.companyId,
                            "CATEGORY_SOCIAL",
                            key.limit,
                        ).mapLatest { messages ->
                            buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_COMPANY_RESULTS)
                        }
                    } else {
                        mail.observeByLabel(key.account!!.accountId, "CATEGORY_SOCIAL", key.limit)
                            .mapLatest { messages ->
                                buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_SOCIAL)
                            }
                    }
                key.dest == MailboxDestination.SPAM ->
                    if (key.companyId != null) {
                        mail.observeMessagesByCompanyAndLabel(
                            key.account!!.accountId,
                            key.companyId,
                            "SPAM",
                            key.limit,
                        ).mapLatest { messages ->
                            buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_COMPANY_RESULTS)
                        }
                    } else {
                        mail.observeByLabel(key.account!!.accountId, "SPAM", key.limit)
                            .mapLatest { messages ->
                                buildMessageContent(messages.map { it.toMessageItem() }, key)
                                ?: MailboxContent.Empty(EmptyKind.NO_SPAM)
                            }
                    }
                key.dest == MailboxDestination.STARRED ->
                    mail.observeStarred(key.account!!.accountId, key.limit)
                        .mapLatest { messages ->
                            val items = messages.map { it.toMessageItem() }
                                .applyMessageFilter(key.filter)
                            if (items.isEmpty()) {
                                if (key.filter.isNotEmpty()) MailboxContent.Empty(EmptyKind.NO_FILTER_RESULTS)
                                else MailboxContent.Empty(EmptyKind.NO_STARRED)
                            } else MailboxContent.Messages(items)
                        }
                else ->
                    mail.observeThreads(key.account!!.accountId, key.limit)
                        .mapLatest { threads ->
                            buildThreadContent(threads, key)
                        }
            }.catch { t ->
                // Never leak internals (§14): user-facing message only.
                MoLogger.e(TAG, "Mailbox load failed: ${t.javaClass.simpleName}")
                emit(MailboxContent.Error("Couldn't load your mail. Please try again."))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MailboxContent.Loading)

    /**
     * Company filter row state (Phase 8).
     *
     * Visible only on destinations where company grouping is meaningful
     * (Promotional/Social/Spam — the category/grouping surfaces). Entries
     * join the pinned-first company list with global per-destination
     * counts; only companies with mail in this destination appear, so
     * counts are never fabricated. Ordered pinned-first, then by count.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val companyFilter: StateFlow<CompanyFilterUiState> =
        combine(activeAccount, destination, selectedCompany) { account, dest, selected ->
            Triple(account, dest, selected)
        }.flatMapLatest { (account, dest, selected) ->
            val scope = when (dest) {
                MailboxDestination.PROMOTIONAL ->
                    CompanyScope.Category(MailCategory.PROMOTIONS)
                MailboxDestination.SOCIAL ->
                    CompanyScope.Label("CATEGORY_SOCIAL")
                MailboxDestination.SPAM ->
                    CompanyScope.Label("SPAM")
                else -> null
            }
            if (account == null || scope == null) {
                flowOf(CompanyFilterUiState(visible = false))
            } else {
                companyIntelligence.observeCompanyFilterList(account.accountId)
                    .mapLatest { companies ->
                        val counts = when (scope) {
                            is CompanyScope.Category ->
                                companyIntelligence.companyCountsForCategory(
                                    account.accountId,
                                    scope.category,
                                )
                            is CompanyScope.Label ->
                                companyIntelligence.companyCountsForLabel(
                                    account.accountId,
                                    scope.label,
                                )
                        }
                        val entries = companies.mapNotNull { company ->
                            val count = counts[company.companyId] ?: 0
                            if (count <= 0) null
                            else CompanyFilterEntry(
                                companyId = company.companyId,
                                displayName = company.userOverrideName
                                    ?: company.canonicalName,
                                messageCount = count,
                                pinned = company.pinned,
                            )
                        }.sortedWith(
                            compareByDescending<CompanyFilterEntry> { it.pinned }
                                .thenByDescending { it.messageCount }
                                .thenBy { it.displayName },
                        )
                        CompanyFilterUiState(
                            visible = true,
                            entries = entries,
                            selectedCompanyId = selected,
                        )
                    }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CompanyFilterUiState(),
        )

    /** Destination scope for company counts (Phase 8). */
    private sealed interface CompanyScope {
        data class Category(val category: MailCategory) : CompanyScope
        data class Label(val label: String) : CompanyScope
    }

    val state: StateFlow<MailScreenState> = combine(
        destination,
        content,
        activeAccount,
        activeSelection,
        allAccounts,
    ) { dest, cont, account, selection, all ->
        val isUnified = activeAccountPreferences != null && selection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
        MailboxCore(dest, cont, account, isUnified, all, filterText.value)
    }.let { core ->
        // Typed combine() only goes to 5 flows in coroutines 1.9: fold the
        // remaining flows in a second, still fully-typed combine.
        combine(
            core,
            syncUi,
            connectivity.isOnline,
            companyFilter,
            actionRequiredOnly,
        ) { c, sync, online, companies, actionOnly ->
            MailScreenState(
                destination = c.destination,
                content = c.content,
                activeAccount = c.account,
                isUnified = c.isUnified,
                accountsById = c.all.associateBy { it.accountId },
                accountCount = c.all.size,
                isSampleData = c.all.any { SampleMailboxSeeder.isFixtureAccount(it.accountId) },
                isOffline = !online,
                syncUi = sync,
                filterText = filterText.value,
                companyFilter = companies,
                actionRequiredOnly = actionOnly,
                lastSyncedText = lastSyncedText.value,
                unifiedSyncStatus = unifiedSyncStatus.value,
                accountSyncStatus = accountSyncStatus.value,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MailScreenState(),
    )

    val lastSyncedText: StateFlow<String> = combine(
        allAccounts,
        activeSelection,
        activeAccount,
        connectivity.isOnline,
    ) { all, selection, account, _ ->
        val repo = syncStateRepository ?: return@combine ""
        val isUnified = activeAccountPreferences != null && selection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
        if (isUnified) {
            val enabled = all.filter { it.isEnabled }
            var maxSync: Long? = null
            for (acc in enabled) {
                val record = repo.ensureForAccount(acc.accountId)
                val s = record.lastSuccessfulSyncEpochMs
                if (s != null && (maxSync == null || s > maxSync)) maxSync = s
            }
            SyncTimeFormatter.formatLastSynced(maxSync, clock())
        } else {
            val accId = account?.accountId ?: return@combine ""
            val record = repo.ensureForAccount(accId)
            SyncTimeFormatter.formatLastSynced(record.lastSuccessfulSyncEpochMs, clock())
        }
    }.catch { emit("") }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val accountSyncStatus: StateFlow<AccountSyncStatus> = combine(
        activeAccount,
        connectivity.isOnline,
        syncCoordinator.progress,
    ) { account, online, prog ->
        if (account == null) {
            AccountSyncStatus.NeverSynced
        } else {
            val repo = syncStateRepository
            val record = repo?.ensureForAccount(account.accountId)
            AccountSyncStatusEvaluator.evaluate(
                record = record,
                inProgress = prog,
                isOnline = online,
                account = account,
            )
        }
    }.catch { emit(AccountSyncStatus.NeverSynced) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountSyncStatus.NeverSynced)

    val unifiedSyncStatus: StateFlow<UnifiedSyncStatus?> = combine(
        allAccounts,
        activeSelection,
        connectivity.isOnline,
    ) { all, selection, online ->
        val isUnified = activeAccountPreferences != null && selection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
        if (!isUnified) return@combine null
        val repo = syncStateRepository
        val statuses = all.associate { acc ->
            val rec = repo?.ensureForAccount(acc.accountId)
            acc.accountId to AccountSyncStatusEvaluator.evaluate(
                record = rec,
                inProgress = syncCoordinator.progress.value,
                isOnline = online,
                account = acc,
            )
        }
        AccountSyncStatusEvaluator.evaluateUnified(all, statuses, online)
    }.catch { emit(null) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        syncScheduler?.onAppStart()

        // Honest sync progress (stages, never fabricated percentages — §16).
        syncCoordinator.progress
            .onEach { progress ->
                if (progress is SyncProgress.Running) {
                    syncUi.value = SyncUiState.Syncing(stageText(progress.stage))
                }
            }
            .launchIn(viewModelScope)

        // Phase 8: attribute senders/companies in the background (bounded,
        // incremental), BEFORE classification — the classifier's
        // recurring-sender signal is real only once sender frequency is
        // recorded. Best-effort — attribution must never break the inbox.
        // Runs after the fixture seeding attempt so debug sample mail is
        // attributed too.
        viewModelScope.launch(dispatchers.io) {
            try {
                withTimeoutOrNull(CLASSIFY_STARTUP_TIMEOUT_MS) {
                    seedDone.first { it }
                    val account = withTimeoutOrNull(CLASSIFY_ACCOUNT_TIMEOUT_MS) {
                        activeAccount.first { it != null }
                    }
                    if (account != null) {
                        // Phase 8 first: sender/company attribution.
                        val attributed =
                            companyIntelligence.processNew(account.accountId)
                        if (attributed > 0) {
                            MoLogger.i(TAG, "Background attribution: $attributed messages")
                        }
                        // Phase 7: then classify (now with the real
                        // recurring-sender signal).
                        val n = classifyMailbox.classifyNew(account.accountId)
                        if (n > 0) {
                            MoLogger.i(TAG, "Background classification: $n messages")
                        }
                        // Phase 9: then prioritize (consumes the fresh
                        // classifications as one signal among several).
                        val p = prioritizeMailbox.prioritizeNew(account.accountId)
                        if (p > 0) {
                            MoLogger.i(TAG, "Background prioritization: $p messages")
                        }
                        // Phase 13: then extract meetings/deadlines
                        // (best-effort, never breaks the inbox).
                        val e = extractMailbox.extractNew(account.accountId)
                        if (e > 0) {
                            MoLogger.i(TAG, "Background temporal extraction: $e messages")
                        }
                        // Phase 14: then generate action cards from the
                        // fresh intelligence (best-effort, never breaks
                        // the inbox).
                        val a = generateActions.generateNew(account.accountId)
                        if (a > 0) {
                            MoLogger.i(TAG, "Background action generation: $a cards")
                        }
                    }
                }
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Background attribution/classification failed: ${t.javaClass.simpleName}")
            }
        }
    }

    // ---- Intents ----

    fun setDestination(dest: MailboxDestination) {
        destination.value = dest
        pageLimit.value = PAGE_SIZE
        // Company selection is a filter *within* a category — switching
        // destinations clears it (requirements.md).
        selectedCompany.value = null
        // The action-required view is likewise destination-scoped.
        actionRequiredOnly.value = false
    }

    /**
     * Toggles the action-required view (Phase 9): when on, the content
     * shows ACTION_REQUIRED-classified mail regardless of destination.
     */
    fun toggleActionRequired() {
        actionRequiredOnly.value = !actionRequiredOnly.value
        pageLimit.value = PAGE_SIZE
    }

    /**
     * Selects (or clears, when null) the company filter (Phase 8).
     * The list re-queries the exact company+destination slice.
     */
    fun selectCompany(companyId: String?) {
        selectedCompany.value = companyId
        pageLimit.value = PAGE_SIZE
    }

    /**
     * Toggles company pinning (Phase 8). Pinning only reorders the
     * company filter list — it never moves mail, stars messages, or
     * touches navigation (requirements.md).
     */
    fun toggleCompanyPin(companyId: String) {
        viewModelScope.launch(dispatchers.io) {
            try {
                val entry = companyFilter.value.entries
                    .find { it.companyId == companyId } ?: return@launch
                companyIntelligence.setCompanyPinned(companyId, !entry.pinned)
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Pin toggle failed: ${t.javaClass.simpleName}")
            }
        }
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
        if (state.value.isOffline) {
            syncUi.value = SyncUiState.Result("You're offline — showing synced mail.")
            return
        }
        if (syncUi.value is SyncUiState.Syncing) return
        val selection = activeSelection.value
        val isUnified = activeAccountPreferences != null && selection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified

        viewModelScope.launch(dispatchers.io) {
            syncUi.value = SyncUiState.Syncing("Starting sync…")
            if (isUnified) {
                val results = syncScheduler?.syncAllEnabled(SyncTrigger.MANUAL)
                    ?: syncAllAccountsManually()
                val total = results.size
                val succeeded = results.values.count { it is SyncOutcome.Success }
                val failed = results.values.count { it is SyncOutcome.Failed }
                val msg = when {
                    total == 0 -> "No accounts connected."
                    succeeded == total -> "All accounts synced."
                    failed == total -> "Could not sync accounts."
                    else -> "$succeeded of $total accounts synced."
                }
                syncUi.value = SyncUiState.Result(msg)
            } else {
                val account = activeAccount.value ?: return@launch
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
    }

    private suspend fun syncAllAccountsManually(): Map<String, SyncOutcome> {
        val enabled = allAccounts.value.filter { it.isEnabled }
        val results = mutableMapOf<String, SyncOutcome>()
        for (acc in enabled) {
            results[acc.accountId] = syncCoordinator.syncNow(AccountId(acc.accountId), SyncTrigger.MANUAL)
        }
        return results
    }

    // ---- Internals ----

    private data class ContentKey(
        val resolved: Boolean,
        val account: AccountRecord?,
        val isUnified: Boolean,
        val dest: MailboxDestination,
        val filter: String,
        val limit: Int,
        /** Phase 8: selected company filter (null = no filter). */
        val companyId: String?,
        /** Phase 9: action-required view (global, overrides destination). */
        val actionRequiredOnly: Boolean = false,
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
    private suspend fun buildMessageContent(
        items: List<MessageItem>,
        key: ContentKey,
    ): MailboxContent? {
        // Phase 9: attach priorities (one bounded query for the page).
        val priorities = priorityMap(items.map { it.messageId })
        val withPriorities = items.map { item ->
            val p = priorities[item.messageId]
            if (p == null) item else item.copy(priority = p)
        }
        val filtered = withPriorities.applyMessageFilter(key.filter)
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
        // Phase 9: one bounded query for the visible page's priorities.
        val priorities = priorityMap(latestById.keys.toList())
        val items = threads
            .map {
                it.toThreadItem(latestById[it.latestMessageId]).withPriority(
                    latestById[it.latestMessageId]?.messageId?.let { id -> priorities[id] },
                )
            }
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

    /**
     * Phase 9: batch priority lookup for the visible page — one query,
     * never N+1. Failures degrade to "no priorities shown", never break
     * the list.
     */
    private suspend fun priorityMap(messageIds: List<String>): Map<String, Priority> {
        if (messageIds.isEmpty()) return emptyMap()
        return try {
            intelligence.getPriorities(messageIds)
                .mapValues { it.value.priority }
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Priority lookup failed: ${t.javaClass.simpleName}")
            emptyMap()
        }
    }

    private fun ThreadItem.withPriority(priority: Priority?): ThreadItem =
        if (priority == this.priority) this else copy(priority = priority)

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
        SyncStage.PROCESSING -> "Processing intelligence…"
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

/** Top-level screen state (phase §35, updated Phase 18). */
data class MailScreenState(
    val destination: MailboxDestination = MailboxDestination.ALL_INBOX,
    val content: MailboxContent = MailboxContent.Loading,
    val activeAccount: AccountRecord? = null,
    val isUnified: Boolean = false,
    val accountsById: Map<String, AccountRecord> = emptyMap(),
    val accountCount: Int = 0,
    val isSampleData: Boolean = false,
    val isOffline: Boolean = false,
    val syncUi: SyncUiState = SyncUiState.Idle,
    val filterText: String = "",
    /** Phase 8: company filter row state (visible on grouping destinations). */
    val companyFilter: CompanyFilterUiState = CompanyFilterUiState(),
    /** Phase 9: action-required view toggle state. */
    val actionRequiredOnly: Boolean = false,
    /** Phase 19: truthful last synced relative text. */
    val lastSyncedText: String = "",
    /** Phase 19: aggregate unified sync status. */
    val unifiedSyncStatus: UnifiedSyncStatus? = null,
    /** Phase 19: per-account sync status. */
    val accountSyncStatus: AccountSyncStatus = AccountSyncStatus.NeverSynced,
)

/** Sync affordance state: idle, honestly-staged progress, or a result message. */
sealed interface SyncUiState {
    data object Idle : SyncUiState
    data class Syncing(val stageText: String) : SyncUiState
    data class Result(val message: String) : SyncUiState
}
