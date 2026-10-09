package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import com.greninjaop.mailorganizer.ui.mail.FakeConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import com.greninjaop.mailorganizer.ui.mail.FakeSyncStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 19 — Unit tests for [AccountSyncWorker].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountSyncWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)

    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var syncState: FakeSyncStateRepository

    @Before
    fun setUp() {
        accounts = FakeAccountRepository()
        mail = FakeMailRepository()
        syncState = FakeSyncStateRepository()
    }

    @Test
    fun `uniqueWorkName formats per-account unique work identifier`() {
        val accountId = AccountId("work-123")
        assertEquals("mail-organizer-sync-work-123", AccountSyncWorker.uniqueWorkName(accountId))
    }

    @Test
    fun `worker fails if account does not exist`() = runTest(testDispatcher) {
        val coordinator = SyncCoordinator(
            api = DeferredGmailSyncApi(),
            mail = mail,
            syncState = syncState,
            dispatchers = dispatchers,
        )
        val worker = AccountSyncWorker(
            syncCoordinator = coordinator,
            pipeline = null,
            accountRepository = accounts,
            dispatchers = dispatchers,
        )

        val result = worker.doWork(AccountId("missing-acc"), SyncTrigger.MANUAL)
        assertTrue(result is SyncWorkerResult.Failure)
        assertEquals("Account not found", (result as SyncWorkerResult.Failure).reason)
    }

    @Test
    fun `worker fails if account is disabled`() = runTest(testDispatcher) {
        val acc = AccountRecord("acc-disabled", "disabled@example.com", "Disabled", 1_000L, isEnabled = false)
        accounts.seed(acc)

        val coordinator = SyncCoordinator(
            api = DeferredGmailSyncApi(),
            mail = mail,
            syncState = syncState,
            dispatchers = dispatchers,
        )
        val worker = AccountSyncWorker(
            syncCoordinator = coordinator,
            pipeline = null,
            accountRepository = accounts,
            dispatchers = dispatchers,
        )

        val result = worker.doWork(AccountId("acc-disabled"), SyncTrigger.PERIODIC)
        assertTrue(result is SyncWorkerResult.Failure)
        assertEquals("Account is disabled", (result as SyncWorkerResult.Failure).reason)
    }

    @Test
    fun `worker defers with Retry when device is offline and network required`() = runTest(testDispatcher) {
        val acc = AccountRecord("acc-offline", "offline@example.com", "Offline", 1_000L, isEnabled = true)
        accounts.seed(acc)
        val offlineObserver = FakeConnectivityObserver(initial = false)

        val coordinator = SyncCoordinator(
            api = DeferredGmailSyncApi(),
            mail = mail,
            syncState = syncState,
            dispatchers = dispatchers,
        )
        val worker = AccountSyncWorker(
            syncCoordinator = coordinator,
            pipeline = null,
            accountRepository = accounts,
            connectivity = offlineObserver,
            dispatchers = dispatchers,
        )

        val result = worker.doWork(
            accountId = AccountId("acc-offline"),
            trigger = SyncTrigger.PERIODIC,
            constraints = SyncConstraints(requiresNetwork = true),
        )
        assertTrue(result is SyncWorkerResult.Retry)
        assertEquals("Device is offline", (result as SyncWorkerResult.Retry).reason)
    }

    @Test
    fun `worker executes sync and maps unconfigured to failure`() = runTest(testDispatcher) {
        val acc = AccountRecord("acc-1", "user@example.com", "User", 1_000L, isEnabled = true)
        accounts.seed(acc)
        val onlineObserver = FakeConnectivityObserver(initial = true)

        val coordinator = SyncCoordinator(
            api = DeferredGmailSyncApi(), // Not configured fail-closed stand-in
            mail = mail,
            syncState = syncState,
            dispatchers = dispatchers,
        )
        val worker = AccountSyncWorker(
            syncCoordinator = coordinator,
            pipeline = null,
            accountRepository = accounts,
            connectivity = onlineObserver,
            dispatchers = dispatchers,
        )

        val result = worker.doWork(AccountId("acc-1"), SyncTrigger.MANUAL)
        assertTrue(result is SyncWorkerResult.Failure)
    }
}
