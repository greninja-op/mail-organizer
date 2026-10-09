package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.SyncStateRecord
import com.greninjaop.mailorganizer.data.local.SyncStatus
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import com.greninjaop.mailorganizer.ui.mail.FakeConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import com.greninjaop.mailorganizer.ui.mail.FakeSyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryConsciousSyncSchedulerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private lateinit var accountsRepo: FakeAccountRepository
    private lateinit var syncStateRepo: TrackingSyncStateRepository
    private lateinit var mailRepo: FakeMailRepository
    private lateinit var connectivity: FakeConnectivityObserver
    private lateinit var syncCoordinator: SyncCoordinator
    private lateinit var worker: AccountSyncWorker
    private var currentTime: Long = 1_000_000_000L

    private fun createScheduler(scope: CoroutineScope): BatteryConsciousSyncScheduler {
        return BatteryConsciousSyncScheduler(
            worker = worker,
            syncCoordinator = syncCoordinator,
            accountRepository = accountsRepo,
            syncStateRepository = syncStateRepo,
            connectivity = connectivity,
            dispatchers = dispatchers,
            scope = scope,
            clock = { currentTime },
        )
    }

    @Before
    fun setup() {
        accountsRepo = FakeAccountRepository()
        syncStateRepo = TrackingSyncStateRepository()
        mailRepo = FakeMailRepository()
        connectivity = FakeConnectivityObserver(true)
        currentTime = 1_000_000_000L

        syncCoordinator = SyncCoordinator(
            api = DeferredGmailSyncApi(),
            mail = mailRepo,
            syncState = syncStateRepo,
            dispatchers = dispatchers,
        )
        worker = AccountSyncWorker(
            syncCoordinator = syncCoordinator,
            pipeline = null,
            accountRepository = accountsRepo,
            connectivity = connectivity,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `schedulePeriodic clamps interval below 15 minutes to 15 minutes`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        val accountId = AccountId("acc-1")

        scheduler.schedulePeriodic(
            accountId = accountId,
            intervalMinutes = 5, // below 15m
            constraints = SyncConstraints(requiresBatteryNotLow = true),
        )

        val spec = scheduler.getWorkSpec(accountId)
        assertNotNull(spec)
        assertEquals(15L, spec?.intervalMinutes)
        assertTrue(spec?.constraints?.requiresBatteryNotLow == true)
    }

    @Test
    fun `schedulePeriodic respects interval greater than or equal to 15 minutes`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        val accountId = AccountId("acc-1")

        scheduler.schedulePeriodic(
            accountId = accountId,
            intervalMinutes = 60,
            constraints = SyncConstraints(requiresCharging = true),
        )

        val spec = scheduler.getWorkSpec(accountId)
        assertNotNull(spec)
        assertEquals(60L, spec?.intervalMinutes)
        assertTrue(spec?.constraints?.requiresCharging == true)
    }

    @Test
    fun `syncAccountNow executes coordinator directly`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        val accountId = AccountId("acc-1")

        val outcome = scheduler.syncAccountNow(accountId, SyncTrigger.MANUAL)
        assertTrue(outcome is SyncOutcome.Failed)
    }

    @Test
    fun `syncAllEnabled runs all enabled accounts independently`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        accountsRepo.upsert(AccountRecord("acc-1", "user1@example.com", "User 1", 1_000L, isEnabled = true))
        accountsRepo.upsert(AccountRecord("acc-2", "user2@example.com", "User 2", 1_000L, isEnabled = true))
        accountsRepo.upsert(AccountRecord("acc-3", "user3@example.com", "User 3", 1_000L, isEnabled = false))

        val results = scheduler.syncAllEnabled(SyncTrigger.MANUAL)
        assertEquals(2, results.size)
        assertTrue(results.containsKey("acc-1"))
        assertTrue(results.containsKey("acc-2"))
        assertFalse(results.containsKey("acc-3"))
    }

    @Test
    fun `onAppStart and onAppResume skip fresh accounts and sync stale accounts`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        accountsRepo.upsert(AccountRecord("acc-fresh", "fresh@example.com", "Fresh", 1_000L, isEnabled = true))
        accountsRepo.upsert(AccountRecord("acc-stale", "stale@example.com", "Stale", 1_000L, isEnabled = true))

        // acc-fresh synced 5 minutes ago (< 15m threshold)
        syncStateRepo.setLastSync("acc-fresh", currentTime - (5 * 60 * 1000L))
        // acc-stale synced 30 minutes ago (> 15m threshold)
        syncStateRepo.setLastSync("acc-stale", currentTime - (30 * 60 * 1000L))

        scheduler.onAppStart()
        advanceUntilIdle()

        // Advance 20 minutes -> acc-fresh is now stale too
        currentTime += 20 * 60 * 1000L
        scheduler.onAppResume()
        advanceUntilIdle()
    }

    @Test
    fun `onConnectivityChanged triggers sync on stale accounts when coming online`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        accountsRepo.upsert(AccountRecord("acc-stale", "stale@example.com", "Stale", 1_000L, isEnabled = true))
        syncStateRepo.setLastSync("acc-stale", currentTime - (60 * 60 * 1000L))

        // Offline transition
        scheduler.onConnectivityChanged(isOnline = false)
        advanceUntilIdle()

        // Online transition triggers recovery
        scheduler.onConnectivityChanged(isOnline = true)
        advanceUntilIdle()
    }

    @Test
    fun `cancellation methods update scheduled state properly`() = runTest(testDispatcher) {
        val scheduler = createScheduler(this)
        val account1 = AccountId("acc-1")
        val account2 = AccountId("acc-2")

        scheduler.schedulePeriodic(account1, 15)
        scheduler.schedulePeriodic(account2, 30)

        scheduler.cancelPeriodic(account1)
        assertNull(scheduler.getWorkSpec(account1))
        assertNotNull(scheduler.getWorkSpec(account2))

        scheduler.cancelAll()
        assertNull(scheduler.getWorkSpec(account2))
    }

    // ---- Helper fake ----

    private class TrackingSyncStateRepository : com.greninjaop.mailorganizer.data.repository.SyncStateRepository {
        private val map = mutableMapOf<String, SyncStateRecord>()

        fun setLastSync(accountId: String, timeMs: Long) {
            val record = map[accountId]?.copy(lastSuccessfulSyncEpochMs = timeMs)
                ?: SyncStateRecord(
                    accountId = accountId,
                    status = SyncStatus.IDLE,
                    lastSuccessfulSyncEpochMs = timeMs,
                    lastAttemptEpochMs = timeMs,
                )
            map[accountId] = record
        }

        override suspend fun ensureForAccount(accountId: String): SyncStateRecord {
            return map.getOrPut(accountId) {
                SyncStateRecord(accountId, SyncStatus.NEVER_SYNCED)
            }
        }

        override fun observe(accountId: String): kotlinx.coroutines.flow.Flow<SyncStateRecord?> =
            kotlinx.coroutines.flow.MutableStateFlow(map[accountId])

        override suspend fun markAttempt(accountId: String, status: SyncStatus, errorCode: String?) {}
        override suspend fun markSuccess(accountId: String, cursor: String?) {}
        override suspend fun updateCursor(accountId: String, cursor: String) {}
    }
}
