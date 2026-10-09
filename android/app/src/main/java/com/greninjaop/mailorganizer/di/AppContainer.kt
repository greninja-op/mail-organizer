package com.greninjaop.mailorganizer.di

import android.content.Context
import androidx.room.Room
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.Migrations
import com.greninjaop.mailorganizer.data.prefs.DataStoreThemePreferences
import com.greninjaop.mailorganizer.data.prefs.ThemePreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RoomAccountRepository
import com.greninjaop.mailorganizer.data.repository.RoomIntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.RoomMailRepository
import com.greninjaop.mailorganizer.data.repository.RoomRuleRepository
import com.greninjaop.mailorganizer.data.repository.RoomSyncStateRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import com.greninjaop.mailorganizer.data.sync.DeferredGmailSyncApi
import com.greninjaop.mailorganizer.data.sync.GmailSyncApi
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import com.greninjaop.mailorganizer.ui.mail.AndroidConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.DebugSampleDataPolicy
import com.greninjaop.mailorganizer.ui.mail.SampleDataPolicy
import com.greninjaop.mailorganizer.ui.mail.SampleMailboxSeeder

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

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            appContext.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            // Production migrations must preserve user data: no destructive
            // fallback. v1 -> v2 is covered by Migrations.MIGRATION_1_2;
            // v2 -> v3 (Phase 5 parser columns) by Migrations.MIGRATION_2_3.
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
            .build()
    }

    // ---- Phase 2: local data repositories ----
    // UI and (later) the sync engine depend on these interfaces, never on
    // DAOs or the database directly.

    val accountRepository: AccountRepository by lazy {
        RoomAccountRepository(database.accountDao(), dispatchers)
    }

    val mailRepository: MailRepository by lazy {
        RoomMailRepository(database, dispatchers)
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

    val classifyMessageUseCase: ClassifyMessageUseCase by lazy {
        ClassifyMessageUseCase(
            mail = mailRepository,
            intelligence = intelligenceRepository,
            dispatchers = dispatchers,
        )
    }

    val classifyMailboxUseCase: ClassifyMailboxUseCase by lazy {
        ClassifyMailboxUseCase(
            mail = mailRepository,
            classifyMessage = classifyMessageUseCase,
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

    private companion object {
        const val DATABASE_NAME = "mail_organizer.db"
    }
}
