package com.greninjaop.mailorganizer.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.Migrations
import com.greninjaop.mailorganizer.data.local.SearchIndexStore
import com.greninjaop.mailorganizer.data.integrations.CalendarIntegrationAdapter
import com.greninjaop.mailorganizer.data.integrations.GmailIntegrationAdapter
import com.greninjaop.mailorganizer.data.integrations.TasksIntegrationAdapter
import com.greninjaop.mailorganizer.data.prefs.DataStoreThemePreferences
import com.greninjaop.mailorganizer.data.prefs.ThemePreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntegrationStateRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RoomAccountRepository
import com.greninjaop.mailorganizer.data.repository.RoomIntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.RoomIntegrationStateRepository
import com.greninjaop.mailorganizer.data.repository.RoomMailRepository
import com.greninjaop.mailorganizer.data.repository.RoomRuleRepository
import com.greninjaop.mailorganizer.data.repository.RoomSearchRepository
import com.greninjaop.mailorganizer.data.repository.RoomSyncStateRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import com.greninjaop.mailorganizer.data.repository.SearchRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import com.greninjaop.mailorganizer.data.sync.DeferredGmailSyncApi
import com.greninjaop.mailorganizer.data.sync.GmailSyncApi
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.domain.actions.ActionExecutorRegistry
import com.greninjaop.mailorganizer.domain.actions.GenerateActionsUseCase
import com.greninjaop.mailorganizer.domain.actions.ReviewActionUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMailboxUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMessageUseCase
import com.greninjaop.mailorganizer.domain.rules.ApplyRulesUseCase
import com.greninjaop.mailorganizer.domain.rules.RecordCorrectionUseCase
import com.greninjaop.mailorganizer.domain.rules.RuleManagementUseCase
import com.greninjaop.mailorganizer.domain.search.SearchIndexUseCase
import com.greninjaop.mailorganizer.domain.temporal.ExtractMailboxUseCase
import com.greninjaop.mailorganizer.domain.temporal.ExtractTemporalUseCase
import com.greninjaop.mailorganizer.ui.mail.AndroidConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.DebugSampleDataPolicy
import com.greninjaop.mailorganizer.ui.mail.SampleDataPolicy
import com.greninjaop.mailorganizer.ui.mail.SampleMailboxSeeder
import kotlinx.coroutines.flow.first

/**
 * Manual dependency container (Phase 0; data layer wired in Phase 2).
 *
 * Kept deliberately small: only foundation + local-data collaborators are
 * wired here.
 * Deferred to their own phases (documented, not implemented):
 * - Phase 3: AuthRepository (Google OAuth, token lifecycle) — the real
 *   GmailSyncApi implementation plugs in here once credentials exist.
 * - Phases 15/16: Calendar/Tasks integration adapters
 *
 * If the graph grows beyond comfortable manual wiring, adopt Hilt or Koin
 * (decision recorded in docs/development-status.md).
 */
class AppContainer(private val appContext: Context) {

    val dispatchers: AppDispatchers = AppDispatchers()

    val themePreferences: ThemePreferences by lazy {
        DataStoreThemePreferences(appContext.applicationContext, dispatchers)
    }

    val activeAccountPreferences: com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences by lazy {
        com.greninjaop.mailorganizer.data.prefs.DataStoreActiveAccountPreferences(
            appContext.applicationContext,
            dispatchers,
        )
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            // Production migrations must preserve user data: no destructive
            // fallback. v1 -> v2 is covered by Migrations.MIGRATION_1_2;
            // v2 -> v3 (Phase 5 parser columns) by Migrations.MIGRATION_2_3;
            // v3 -> v4 (Phase 8 company link) by Migrations.MIGRATION_3_4;
            // v4 -> v5 (Phase 10 search index) by Migrations.MIGRATION_4_5;
            // v5 -> v6 (Phase 12 rule engine columns) by Migrations.MIGRATION_5_6;
            // v6 -> v7 (Phase 14 action-engine columns) by Migrations.MIGRATION_6_7;
            // v7 -> v8 (Phase 17 integration metadata) by Migrations.MIGRATION_7_8;
            // v8 -> v9 (Phase 24 performance indexes) by Migrations.MIGRATION_8_9.
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6,
                Migrations.MIGRATION_6_7,
                Migrations.MIGRATION_7_8,
                Migrations.MIGRATION_8_9,
                Migrations.MIGRATION_9_10,
            )
            // Fresh installs: the FTS search index is a standalone virtual
            // table (not a Room entity), so it is created here. Upgrades
            // are covered by MIGRATION_4_5.
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(SearchIndexStore.CREATE_SQL)
                    }
                },
            )
            .build()
    }

    // ---- Phase 10: local search ----
    // FTS5 index store: raw-SQLite access to the derived `messages_fts`
    // virtual table. Wired into the repositories' write paths so the index
    // always reflects committed data (phase §57, §58).

    val searchIndexStore: SearchIndexStore by lazy {
        SearchIndexStore(database.openHelper.writableDatabase)
    }

    // ---- Phase 2: local data repositories ----
    // UI and (later) the sync engine depend on these interfaces, never on
    // DAOs or the database directly.

    val accountRepository: AccountRepository by lazy {
        RoomAccountRepository(
            accountDao = database.accountDao(),
            dispatchers = dispatchers,
            searchIndex = searchIndexStore,
        )
    }

    val mailRepository: MailRepository by lazy {
        RoomMailRepository(
            db = database,
            dispatchers = dispatchers,
            searchIndex = searchIndexStore,
        )
    }

    val intelligenceRepository: IntelligenceRepository by lazy {
        RoomIntelligenceRepository(database, dispatchers)
    }

    val ruleRepository: RuleRepository by lazy {
        RoomRuleRepository(database, dispatchers)
    }

    val syncStateRepository: SyncStateRepository by lazy {
        RoomSyncStateRepository(database, dispatchers)
    }

    // ---- Phase 4: sync engine ----
    // gmailSyncApi is the fail-closed stand-in until Phase 3 lands: any sync
    // attempt surfaces "not connected" honestly instead of crashing. The
    // coordinator itself is fully implemented and unit-tested against a fake
    // API; only the LIVE Gmail wire-up is deferred (no credentials).

    val gmailSyncApi: GmailSyncApi by lazy {
        DeferredGmailSyncApi()
    }

    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(
            api = gmailSyncApi,
            mail = mailRepository,
            syncState = syncStateRepository,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 7: deterministic classification engine ----
    // Pure-Kotlin engine (core.classify) driven through use cases so the UI
    // never holds classification logic. Local-only: no network, no AI.

    // ---- Phase 8: company & sender intelligence ----
    // Pure-Kotlin detection (core.company) driven through a use case.
    // Also feeds the classifier's real recurring-sender signal.

    val companyIntelligenceUseCase: CompanyIntelligenceUseCase by lazy {
        CompanyIntelligenceUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
            searchIndex = searchIndexStore,
        )
    }

    val classifyMessageUseCase: ClassifyMessageUseCase by lazy {
        ClassifyMessageUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
            recurringSenderProvider = companyIntelligenceUseCase,
            aiFallback = aiFallbackUseCase,
        )
    }

    val classifyMailboxUseCase: ClassifyMailboxUseCase by lazy {
        ClassifyMailboxUseCase(
            mail = mailRepository,
            classifyMessage = classifyMessageUseCase,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 9: priority & action-required engine ----
    // Pure-Kotlin engine (core.priority) driven through use cases so the UI
    // never holds priority logic. Local-only: no network, no AI. Priority is
    // independent from category (requirements.md).

    val prioritizeMessageUseCase: PrioritizeMessageUseCase by lazy {
        PrioritizeMessageUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
            recurringSenderProvider = companyIntelligenceUseCase,
        )
    }

    val prioritizeMailboxUseCase: PrioritizeMailboxUseCase by lazy {
        PrioritizeMailboxUseCase(
            mail = mailRepository,
            prioritizeMessage = prioritizeMessageUseCase,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 13: meeting & deadline extraction ----
    // Pure-Kotlin extractor (core.temporal) driven through use cases so the
    // UI never holds extraction logic. Local-only: no network, no AI.
    // Reference time is the message's received timestamp (never "now").

    val extractTemporalUseCase: ExtractTemporalUseCase by lazy {
        ExtractTemporalUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
        )
    }

    val extractMailboxUseCase: ExtractMailboxUseCase by lazy {
        ExtractMailboxUseCase(
            mail = mailRepository,
            extractMessage = extractTemporalUseCase,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 10: search & local indexing ----
    // FTS5 index over synchronized mail. Local-only: no network, no AI,
    // no Gmail search endpoints. The index is derived data — rebuildable
    // from `messages` without resynchronization.

    val searchRepository: SearchRepository by lazy {
        RoomSearchRepository(
            db = database,
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
        )
    }

    val searchIndexUseCase: SearchIndexUseCase by lazy {
        SearchIndexUseCase(
            db = database,
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 12: rules & user corrections ----
    // The effective-intelligence layer: explicit user corrections outrank
    // enabled user rules, which outrank the deterministic engines
    // (Phases 7/9). Pure rule evaluation lives in core.rules; these use
    // cases orchestrate persistence, reprocessing, and the management UI.

    val applyRulesUseCase: ApplyRulesUseCase by lazy {
        ApplyRulesUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            rules = ruleRepository,
            classify = classifyMessageUseCase,
            prioritize = prioritizeMessageUseCase,
            dispatchers = dispatchers,
        )
    }

    val recordCorrectionUseCase: RecordCorrectionUseCase by lazy {
        RecordCorrectionUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            rules = ruleRepository,
            apply = applyRulesUseCase,
            dispatchers = dispatchers,
        )
    }

    val ruleManagementUseCase: RuleManagementUseCase by lazy {
        RuleManagementUseCase(
            mail = mailRepository,
            rules = ruleRepository,
            apply = applyRulesUseCase,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 14: action cards & action engine ----
    // Deterministic, on-device: the generator (core.actions) turns
    // effective intelligence into action candidates; the safety layer
    // validates them; this use case persists idempotent, account-isolated
    // cards. No external executor is registered — confirming an external
    // proposal records the intent locally and reports "not connected"
    // honestly (phase §31–§33). External integrations are later phases'.

    val actionExecutorRegistry: ActionExecutorRegistry by lazy {
        ActionExecutorRegistry()
    }

    val generateActionsUseCase: GenerateActionsUseCase by lazy {
        GenerateActionsUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
        )
    }

    val reviewActionUseCase: ReviewActionUseCase by lazy {
        ReviewActionUseCase(
            intelligence = intelligenceRepository,
            executors = actionExecutorRegistry,
            dispatchers = dispatchers,
            integrations = integrationManager,
        )
    }

    // ---- Phase 17: Integration Manager ----
    // Coordinates the app's integrations (Gmail, Calendar, Tasks) without
    // absorbing their API-specific logic. Calendar/Tasks adapters are
    // honest UNAVAILABLE placeholders (Phases 15/16 deferred); the Gmail
    // adapter wraps Phase 4's fail-closed sync seam. Nothing here performs
    // external side effects or bypasses Phase 14's confirmation boundary.

    val integrationStateRepository: IntegrationStateRepository by lazy {
        RoomIntegrationStateRepository(
            dao = database.integrationStateDao(),
            dispatchers = dispatchers,
        )
    }

    val gmailIntegrationAdapter: GmailIntegrationAdapter by lazy {
        GmailIntegrationAdapter(
            syncApi = gmailSyncApi,
            accounts = accountRepository,
        )
    }

    val calendarIntegrationAdapter: CalendarIntegrationAdapter by lazy {
        CalendarIntegrationAdapter()
    }

    val tasksIntegrationAdapter: TasksIntegrationAdapter by lazy {
        TasksIntegrationAdapter()
    }

    val integrationManager: IntegrationManager by lazy {
        IntegrationManager(
            adapters = listOf(
                gmailIntegrationAdapter,
                calendarIntegrationAdapter,
                tasksIntegrationAdapter,
            ),
            states = integrationStateRepository,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 6: mailbox UI collaborators ----
    // Connectivity only drives the honest offline banner; browsing
    // synchronized mail never needs the network.

    val connectivityObserver: ConnectivityObserver by lazy {
        AndroidConnectivityObserver(appContext.applicationContext)
    }

    // Fixture policy: debug builds may seed clearly-labeled sample mail so
    // the inbox is developable without live Gmail (Phase 3 deferred).
    // Release builds never seed; the seeder never touches real data.

    val sampleDataPolicy: SampleDataPolicy by lazy {
        DebugSampleDataPolicy()
    }

    val sampleMailboxSeeder: SampleMailboxSeeder by lazy {
        SampleMailboxSeeder(accountRepository, mailRepository)
    }

    // ---- Phase 27: Advanced Automation Engine ----
    val automationRepository: com.greninjaop.mailorganizer.data.automation.AutomationRepository by lazy {
        com.greninjaop.mailorganizer.data.automation.RoomAutomationRepository(
            ruleDao = database.automationRuleDao(),
            historyDao = database.automationHistoryDao(),
            dispatchers = dispatchers,
        )
    }

    val automationEngine: com.greninjaop.mailorganizer.domain.automation.AutomationEngine by lazy {
        com.greninjaop.mailorganizer.domain.automation.AutomationEngine(
            automationRepository = automationRepository,
            mailRepository = mailRepository,
            intelligenceRepository = intelligenceRepository,
            integrationManager = integrationManager,
            executorRegistry = actionExecutorRegistry,
            dispatchers = dispatchers,
        )
    }

    val automationUseCase: com.greninjaop.mailorganizer.domain.automation.AutomationUseCase by lazy {
        com.greninjaop.mailorganizer.domain.automation.AutomationUseCase(
            repository = automationRepository,
            engine = automationEngine,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 19: Background sync & offline behavior ----
    val backgroundProcessingPipeline: com.greninjaop.mailorganizer.data.sync.BackgroundProcessingPipeline by lazy {
        com.greninjaop.mailorganizer.data.sync.BackgroundProcessingPipeline(
            companyIntelligence = companyIntelligenceUseCase,
            classifyMailbox = classifyMailboxUseCase,
            prioritizeMailbox = prioritizeMailboxUseCase,
            extractMailbox = extractMailboxUseCase,
            generateActions = generateActionsUseCase,
            searchIndex = searchIndexUseCase,
            automationEngine = automationEngine,
            mailRepository = mailRepository,
            dispatchers = dispatchers,
        )
    }

    val accountSyncWorker: com.greninjaop.mailorganizer.data.sync.AccountSyncWorker by lazy {
        com.greninjaop.mailorganizer.data.sync.AccountSyncWorker(
            syncCoordinator = syncCoordinator,
            pipeline = backgroundProcessingPipeline,
            accountRepository = accountRepository,
            connectivity = connectivityObserver,
            dispatchers = dispatchers,
        )
    }

    val syncScheduler: com.greninjaop.mailorganizer.data.sync.SyncScheduler by lazy {
        com.greninjaop.mailorganizer.data.sync.BatteryConsciousSyncScheduler(
            worker = accountSyncWorker,
            syncCoordinator = syncCoordinator,
            accountRepository = accountRepository,
            syncStateRepository = syncStateRepository,
            connectivity = connectivityObserver,
            dispatchers = dispatchers,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + dispatchers.default),
        )
    }

    // ---- Phase 20: Noise, Newsletter & Cleanup System ----
    val cleanupUseCase: com.greninjaop.mailorganizer.domain.cleanup.CleanupUseCase by lazy {
        com.greninjaop.mailorganizer.domain.cleanup.CleanupUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 21: Waiting-for-Reply & Conversation Intelligence ----
    val conversationUseCase: com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase by lazy {
        com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            accounts = accountRepository,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 23: Privacy Center & Security Hardening ----
    val privacyUseCase: com.greninjaop.mailorganizer.domain.privacy.PrivacyUseCase by lazy {
        com.greninjaop.mailorganizer.domain.privacy.PrivacyUseCase(
            appDatabase = database,
            accountRepository = accountRepository,
            activeAccountPreferences = activeAccountPreferences,
            integrationManager = integrationManager,
            integrationStateRepository = integrationStateRepository,
            searchIndexStore = searchIndexStore,
            aiFallbackUseCase = aiFallbackUseCase,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 25: Analytics & Insights Engine ----
    val analyticsUseCase: com.greninjaop.mailorganizer.domain.analytics.AnalyticsUseCase by lazy {
        com.greninjaop.mailorganizer.domain.analytics.AnalyticsUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            accounts = accountRepository,
            syncState = syncStateRepository,
            ruleRepository = ruleRepository,
            conversationUseCase = conversationUseCase,
            cleanupUseCase = cleanupUseCase,
            activeAccountPreferences = activeAccountPreferences,
            dispatchers = dispatchers,
        )
    }

    // ---- Phase 26: Optional AI Fallback Architecture ----
    val aiPreferences: com.greninjaop.mailorganizer.data.prefs.AiPreferences by lazy {
        com.greninjaop.mailorganizer.data.prefs.DataStoreAiPreferences(
            appContext.applicationContext,
            dispatchers,
        )
    }

    val aiProviderRegistry: com.greninjaop.mailorganizer.data.ai.AiProviderRegistry by lazy {
        com.greninjaop.mailorganizer.data.ai.AiProviderRegistry().apply {
            register(com.greninjaop.mailorganizer.data.ai.LocalRuleAiProvider())
            register(
                com.greninjaop.mailorganizer.data.ai.StubRemoteAiProvider(
                    apiKeyProvider = {
                        kotlinx.coroutines.runBlocking(dispatchers.io) {
                            aiPreferences.apiKey.first()
                        }
                    },
                    isNetworkAvailable = {
                        kotlinx.coroutines.runBlocking(dispatchers.io) {
                            connectivityObserver.isOnline.first()
                        }
                    },
                )
            )
        }
    }

    val aiManager: com.greninjaop.mailorganizer.domain.ai.AiManager by lazy {
        com.greninjaop.mailorganizer.domain.ai.AiManager(
            preferences = aiPreferences,
            registry = aiProviderRegistry,
            dispatchers = dispatchers,
        )
    }

    val aiFallbackUseCase: com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase by lazy {
        com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase(
            aiManager = aiManager,
            preferences = aiPreferences,
            mailRepository = mailRepository,
            dispatchers = dispatchers,
        )
    }

    private companion object {
        const val DATABASE_NAME = "mail_organizer.db"
    }
}
