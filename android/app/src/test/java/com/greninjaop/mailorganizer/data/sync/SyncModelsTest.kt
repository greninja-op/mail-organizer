package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.SyncStateRecord
import com.greninjaop.mailorganizer.data.local.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 19 — Unit tests for [SyncModels], [AccountSyncStatusEvaluator], and [SyncTimeFormatter].
 */
class SyncModelsTest {

    @Test
    fun `SyncConstraints defaults are battery and network conscious`() {
        val constraints = SyncConstraints()
        assertTrue(constraints.requiresNetwork)
        assertTrue(constraints.requiresBatteryNotLow)
    }

    @Test
    fun `AccountSyncStatusEvaluator evaluates never synced when no record or never synced`() {
        val status1 = AccountSyncStatusEvaluator.evaluate(record = null, isOnline = true)
        assertEquals(AccountSyncStatus.NeverSynced, status1)

        val recordNever = SyncStateRecord(accountId = "acc-1", status = SyncStatus.NEVER_SYNCED)
        val status2 = AccountSyncStatusEvaluator.evaluate(record = recordNever, isOnline = true)
        assertEquals(AccountSyncStatus.NeverSynced, status2)
    }

    @Test
    fun `AccountSyncStatusEvaluator evaluates offline honestly when disconnected`() {
        // When device is offline, status is Offline, never AuthRequired (§16)
        val record = SyncStateRecord(accountId = "acc-1", status = SyncStatus.IDLE, lastSuccessfulSyncEpochMs = 1000L)
        val status = AccountSyncStatusEvaluator.evaluate(record = record, isOnline = false)
        assertEquals(AccountSyncStatus.Offline, status)

        val recordFailed = SyncStateRecord(accountId = "acc-1", status = SyncStatus.FAILED, errorCode = "network")
        val statusFailed = AccountSyncStatusEvaluator.evaluate(record = recordFailed, isOnline = false)
        assertEquals(AccountSyncStatus.Offline, statusFailed)
    }

    @Test
    fun `AccountSyncStatusEvaluator distinguishes auth required from offline`() {
        val acc = AccountRecord("acc-1", "user@gmail.com", "User", 1_000_000L, connectionState = ConnectionState.DISCONNECTED)
        val statusOnline = AccountSyncStatusEvaluator.evaluate(record = null, isOnline = true, account = acc)
        assertEquals(AccountSyncStatus.AuthRequired, statusOnline)

        val statusOffline = AccountSyncStatusEvaluator.evaluate(record = null, isOnline = false, account = acc)
        assertEquals(AccountSyncStatus.Offline, statusOffline)
    }

    @Test
    fun `AccountSyncStatusEvaluator evaluates running or in-progress as syncing`() {
        val inProg = SyncProgress.Running(SyncStage.SAVING, 42)
        val status = AccountSyncStatusEvaluator.evaluate(record = null, inProgress = inProg, isOnline = true)
        assertTrue(status is AccountSyncStatus.Syncing)
        assertEquals(SyncStage.SAVING, (status as AccountSyncStatus.Syncing).stage)
        assertEquals(42, status.messagesProcessed)

        val recordRunning = SyncStateRecord(accountId = "acc-1", status = SyncStatus.RUNNING)
        val statusRec = AccountSyncStatusEvaluator.evaluate(record = recordRunning, isOnline = true)
        assertTrue(statusRec is AccountSyncStatus.Syncing)
    }

    @Test
    fun `AccountSyncStatusEvaluator evaluates error codes properly`() {
        val rateLimited = SyncStateRecord(accountId = "acc-1", status = SyncStatus.FAILED, errorCode = "rate_limited")
        assertTrue(AccountSyncStatusEvaluator.evaluate(rateLimited, isOnline = true) is AccountSyncStatus.RateLimited)

        val permission = SyncStateRecord(accountId = "acc-1", status = SyncStatus.FAILED, errorCode = "permission")
        assertEquals(AccountSyncStatus.PermissionRequired, AccountSyncStatusEvaluator.evaluate(permission, isOnline = true))

        val transientErr = SyncStateRecord(accountId = "acc-1", status = SyncStatus.FAILED, errorCode = "network")
        assertTrue(AccountSyncStatusEvaluator.evaluate(transientErr, isOnline = true) is AccountSyncStatus.TransientError)
    }

    @Test
    fun `AccountSyncStatusEvaluator evaluates paused properly`() {
        val paused = SyncStateRecord(accountId = "acc-1", status = SyncStatus.PAUSED)
        assertEquals(AccountSyncStatus.Paused, AccountSyncStatusEvaluator.evaluate(paused, isOnline = true))
    }

    @Test
    fun `AccountSyncStatusEvaluator evaluates synced with timestamp`() {
        val synced = SyncStateRecord(
            accountId = "acc-1",
            status = SyncStatus.IDLE,
            lastSuccessfulSyncEpochMs = 1_700_000_000_000L,
        )
        val status = AccountSyncStatusEvaluator.evaluate(synced, isOnline = true)
        assertTrue(status is AccountSyncStatus.Synced)
        assertEquals(1_700_000_000_000L, (status as AccountSyncStatus.Synced).lastSuccessEpochMs)
    }

    @Test
    fun `evaluateUnified aggregates multiple accounts correctly`() {
        val acc1 = AccountRecord("acc-1", "one@gmail.com", "One", 1_000_000L, isEnabled = true)
        val acc2 = AccountRecord("acc-2", "two@gmail.com", "Two", 1_000_000L, isEnabled = true)
        val accounts = listOf(acc1, acc2)

        // Case 1: All synced
        val allSynced = mapOf(
            "acc-1" to AccountSyncStatus.Synced(1_000L),
            "acc-2" to AccountSyncStatus.Synced(2_000L),
        )
        val unified1 = AccountSyncStatusEvaluator.evaluateUnified(accounts, allSynced, isOnline = true)
        assertEquals(2, unified1.totalAccounts)
        assertEquals(2, unified1.upToDateCount)
        assertEquals(2_000L, unified1.lastSyncEpochMs)
        assertEquals("All accounts up to date", unified1.headline)

        // Case 2: One attention, one up to date
        val mixed = mapOf(
            "acc-1" to AccountSyncStatus.Synced(1_000L),
            "acc-2" to AccountSyncStatus.AuthRequired,
        )
        val unified2 = AccountSyncStatusEvaluator.evaluateUnified(accounts, mixed, isOnline = true)
        assertEquals(1, unified2.attentionCount)
        assertEquals(1, unified2.upToDateCount)
        assertEquals("1 account needs attention", unified2.headline)

        // Case 3: Offline
        val unifiedOffline = AccountSyncStatusEvaluator.evaluateUnified(accounts, allSynced, isOnline = false)
        assertEquals("Offline — showing saved mail", unifiedOffline.headline)
    }

    @Test
    fun `SyncTimeFormatter produces honest relative timestamps`() {
        val now = 1_000_000_000L

        assertEquals("Never synced", SyncTimeFormatter.formatLastSynced(null, now))
        assertEquals("Never synced", SyncTimeFormatter.formatLastSynced(0L, now))
        assertEquals("Just now", SyncTimeFormatter.formatLastSynced(now + 1000L, now))
        assertEquals("Just now", SyncTimeFormatter.formatLastSynced(now - 30_000L, now)) // 30s ago
        assertEquals("1 minute ago", SyncTimeFormatter.formatLastSynced(now - 65_000L, now))
        assertEquals("10 minutes ago", SyncTimeFormatter.formatLastSynced(now - 600_000L, now))
        assertEquals("1 hour ago", SyncTimeFormatter.formatLastSynced(now - 3_600_000L, now))
        assertEquals("3 hours ago", SyncTimeFormatter.formatLastSynced(now - 10_800_000L, now))
        assertEquals("Yesterday", SyncTimeFormatter.formatLastSynced(now - 86_400_000L, now))
        assertEquals("3 days ago", SyncTimeFormatter.formatLastSynced(now - 3 * 86_400_000L, now))
    }
}
