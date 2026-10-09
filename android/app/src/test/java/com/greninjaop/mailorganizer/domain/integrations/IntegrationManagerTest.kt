package com.greninjaop.mailorganizer.domain.integrations

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationError
import com.greninjaop.mailorganizer.core.integrations.IntegrationEvent
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.PermissionDescription
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.IntegrationStateRecord
import com.greninjaop.mailorganizer.data.repository.IntegrationStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IntegrationManager tests (Phase 17).
 *
 * Uses fake adapters and a fake state repository — no Android, no Room,
 * no network. Every honesty invariant is pinned: account isolation,
 * capability gating on CONNECTED, no fake success, cleanup boundaries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    /** Controllable fake adapter. */
    private class FakeAdapter(
        override val id: IntegrationId,
        override val displayName: String = "Fake",
        var status: IntegrationStatus = IntegrationStatus.DISCONNECTED,
        var reason: String = "fake",
        var connectResult: MoResult<Unit> = MoResult.Success(Unit),
        var failSnapshotWith: Throwable? = null,
    ) : IntegrationAdapter {
        override val provider: String = "FakeProvider"
        override val declaredCapabilities: Set<IntegrationCapability> =
            setOf(IntegrationCapability.READ_EMAIL)
        override val requiredPermissions: List<PermissionDescription> = emptyList()

        override suspend fun snapshot(accountId: String?): IntegrationSnapshot {
            failSnapshotWith?.let { throw it }
            return IntegrationSnapshot(
                id = id,
                displayName = displayName,
                provider = provider,
                accountId = accountId,
                accountEmail = accountId?.let { "$it@example.com" },
                status = status,
                statusReason = reason,
                declaredCapabilities = declaredCapabilities,
                requiredPermissions = requiredPermissions,
                lastCheckedEpochMs = 0L,
            )
        }

        override suspend fun connect(): MoResult<Unit> = connectResult
        override suspend fun disconnect(accountId: String?): MoResult<Unit> =
            MoResult.Success(Unit)
        override suspend fun refresh(accountId: String?): IntegrationSnapshot =
            snapshot(accountId)
    }

    private class FakeStates : IntegrationStateRepository {
        val rows = mutableListOf<IntegrationStateRecord>()
        override suspend fun upsert(record: IntegrationStateRecord) {
            rows.removeIf {
                it.integrationId == record.integrationId && it.accountId == record.accountId
            }
            rows.add(record)
        }
        override suspend fun getForAccount(accountId: String?): List<IntegrationStateRecord> =
            rows.filter { it.accountId == accountId }
        override suspend fun clear(integrationId: String, accountId: String?) {
            rows.removeIf { it.integrationId == integrationId && it.accountId == accountId }
        }
        override suspend fun clearForAccount(accountId: String?) {
            rows.removeIf { it.accountId == accountId }
        }
    }

    private fun manager(
        adapters: List<IntegrationAdapter>,
        states: FakeStates = FakeStates(),
    ) = IntegrationManager(adapters, states, dispatchers, clock = { 0L }) to states

    @Test
    fun `snapshots carry the requested account id on every integration`() = runTest(testDispatcher) {
        val (manager, _) = manager(listOf(FakeAdapter(IntegrationId.GMAIL)))
        val snapshots = manager.snapshots("acct-1")
        assertEquals(1, snapshots.size)
        assertEquals("acct-1", snapshots[0].accountId)
    }

    @Test
    fun `snapshots from different accounts never mix`() = runTest(testDispatcher) {
        val (manager, _) = manager(listOf(FakeAdapter(IntegrationId.GMAIL)))
        val a = manager.snapshots("acct-A")
        val b = manager.snapshots("acct-B")
        assertEquals("acct-A", a[0].accountId)
        assertEquals("acct-B", b[0].accountId)
    }

    @Test
    fun `unknown integration id returns null snapshot`() = runTest(testDispatcher) {
        val (manager, _) = manager(listOf(FakeAdapter(IntegrationId.GMAIL)))
        assertNull(manager.snapshot(IntegrationId("nope"), "a1"))
    }

    @Test
    fun `isCapable is true only when connected`() = runTest(testDispatcher) {
        val adapter = FakeAdapter(IntegrationId.GMAIL)
        val (manager, _) = manager(listOf(adapter))
        // DISCONNECTED: declared but not usable.
        assertFalse(manager.isCapable(IntegrationId.GMAIL, IntegrationCapability.READ_EMAIL, "a1"))
        adapter.status = IntegrationStatus.CONNECTED
        assertTrue(manager.isCapable(IntegrationId.GMAIL, IntegrationCapability.READ_EMAIL, "a1"))
        // AUTH_REQUIRED is not usable either.
        adapter.status = IntegrationStatus.AUTH_REQUIRED
        assertFalse(manager.isCapable(IntegrationId.GMAIL, IntegrationCapability.READ_EMAIL, "a1"))
    }

    @Test
    fun `integrationForAction maps only unambiguous types`() {
        val (manager, _) = manager(emptyList())
        assertEquals(IntegrationId.CALENDAR, manager.integrationForAction(ActionType.MEETING))
        assertEquals(IntegrationId.TASKS, manager.integrationForAction(ActionType.REMINDER))
        // DEADLINE is deliberately unmapped — the manager does not guess.
        assertNull(manager.integrationForAction(ActionType.DEADLINE))
        assertNull(manager.integrationForAction(ActionType.REPLY_REQUIRED))
        assertNull(manager.integrationForAction(ActionType.PAYMENT))
        assertNull(manager.integrationForAction(ActionType.OTHER))
    }

    @Test
    fun `connect failure emits IntegrationFailed and never Connected`() = runTest(testDispatcher) {
        val adapter = FakeAdapter(
            IntegrationId.CALENDAR,
            connectResult = MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.Integration("calendar", "not built"),
            ),
        )
        val (manager, _) = manager(listOf(adapter))
        val collected = mutableListOf<IntegrationEvent>()
        // NOTE: launch on the test scope (not backgroundScope): backgroundScope
        // launches don't get scheduled on the test dispatcher in a way that
        // SharedFlow collection can observe (verified empirically). The job
        // is cancelled before the test ends so runTest doesn't hang.
        val job = launch { manager.events.collect { collected.add(it) } }
        runCurrent() // let the collector subscribe before connect() emits
        val result = manager.connect(IntegrationId.CALENDAR, "a1")
        assertTrue(result is MoResult.Failure)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(collected.none { it is IntegrationEvent.Connected })
        assertTrue(collected.any { it is IntegrationEvent.IntegrationFailed })
        job.cancelAndJoin()
    }

    @Test
    fun `disconnect clears only that integration and account`() = runTest(testDispatcher) {
        val states = FakeStates()
        states.upsert(
            IntegrationStateRecord("gmail", "a1", "CONNECTED", null, 0L, 1),
        )
        states.upsert(
            IntegrationStateRecord("calendar", "a1", "UNAVAILABLE", null, 0L, 1),
        )
        states.upsert(
            IntegrationStateRecord("gmail", "a2", "CONNECTED", null, 0L, 1),
        )
        val (manager, _) = manager(listOf(FakeAdapter(IntegrationId.GMAIL)), states)
        val result = manager.disconnect(IntegrationId.GMAIL, "a1")
        assertTrue(result is MoResult.Success)
        val remaining = states.rows.map { it.integrationId to it.accountId }
        assertFalse(remaining.contains("gmail" to "a1"))
        assertTrue(remaining.contains("calendar" to "a1"))
        assertTrue(remaining.contains("gmail" to "a2"))
    }

    @Test
    fun `handleAccountRemoved clears only the removed account`() = runTest(testDispatcher) {
        val states = FakeStates()
        states.upsert(IntegrationStateRecord("gmail", "a1", "CONNECTED", null, 0L, 1))
        states.upsert(IntegrationStateRecord("gmail", "a2", "CONNECTED", null, 0L, 1))
        val (manager, _) = manager(listOf(FakeAdapter(IntegrationId.GMAIL)), states)
        manager.handleAccountRemoved("a1")
        assertEquals(listOf("a2"), states.rows.map { it.accountId })
    }

    @Test
    fun `adapter exceptions become ERROR snapshots, never thrown`() = runTest(testDispatcher) {
        val adapter = FakeAdapter(
            IntegrationId.GMAIL,
            failSnapshotWith = RuntimeException("boom"),
        )
        val (manager, _) = manager(listOf(adapter))
        val snapshots = manager.snapshots("a1")
        assertEquals(IntegrationStatus.ERROR, snapshots[0].status)
        assertTrue(snapshots[0].statusReason.contains("boom"))
    }

    @Test
    fun `connect to unknown integration fails honestly`() = runTest(testDispatcher) {
        val (manager, _) = manager(emptyList())
        val result = manager.connect(IntegrationId("nope"), "a1")
        assertTrue(result is MoResult.Failure)
    }

    @Test
    fun `IntegrationError describe is human readable`() {
        assertEquals(
            "not built yet — arrives with Phase 15",
            IntegrationError.NotBuilt("Phase 15").describe(),
        )
        assertEquals("the device is offline", IntegrationError.Offline.describe())
    }
}
