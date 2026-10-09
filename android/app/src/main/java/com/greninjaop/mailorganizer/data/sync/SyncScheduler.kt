package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId

/**
 * Background-sync scheduling interface (Phase 19 §6, §11, §13, §31, §32).
 *
 * Establishes battery-conscious background sync:
 * - Account-scoped work with unique work names (§8, §9)
 * - Android-supported periodic work (minimum 15-minute intervals) (§13)
 * - Network and battery constraints (§15, §31)
 * - Lifecycle triggers: app start, app resume, connectivity recovery (§34, §35, §36)
 * - Independent multi-account concurrency (§10)
 */
interface SyncScheduler {

    /** Request immediate background sync for [accountId]; coalesces duplicate jobs. */
    fun requestSync(accountId: AccountId, trigger: SyncTrigger)

    /** Executes a sync for [accountId] and returns the terminal [SyncOutcome]. */
    suspend fun syncAccountNow(accountId: AccountId, trigger: SyncTrigger): SyncOutcome

    /**
     * Schedules periodic background sync for [accountId].
     * Clamped to at least [MIN_PERIODIC_INTERVAL_MINUTES] (15m) per Android platform limits.
     */
    fun schedulePeriodic(
        accountId: AccountId,
        intervalMinutes: Long = DEFAULT_PERIODIC_INTERVAL_MINUTES,
        constraints: SyncConstraints = SyncConstraints(),
    )

    /** Cancels periodic background work for [accountId]. */
    fun cancelPeriodic(accountId: AccountId)

    /** Cancels pending or running work for [accountId]. */
    fun cancelPending(accountId: AccountId)

    /** Cancels all background work across all accounts. */
    fun cancelAll()

    /**
     * Syncs all enabled accounts independently (§10, §21).
     * Returns a map of account ID to [SyncOutcome].
     */
    suspend fun syncAllEnabled(trigger: SyncTrigger): Map<String, SyncOutcome>

    /** App startup hook (§34): performs freshness check and schedules needed syncs. */
    fun onAppStart()

    /** App resume hook (§35): performs freshness check (>15m stale), avoids excessive syncs. */
    fun onAppResume()

    /** Connectivity transition hook (§36): schedules sync for stale accounts when coming online. */
    fun onConnectivityChanged(isOnline: Boolean)

    /** Returns currently scheduled work specification for [accountId], if any. */
    fun getWorkSpec(accountId: AccountId): SyncWorkSpec?

    companion object {
        /** Android WorkManager minimum periodic interval. */
        const val MIN_PERIODIC_INTERVAL_MINUTES = 15L
        const val DEFAULT_PERIODIC_INTERVAL_MINUTES = 15L
        /** Freshness threshold before app resume or network recovery triggers sync. */
        const val STALENESS_THRESHOLD_MS = 15 * 60 * 1000L
    }
}

/**
 * Declarative description of an account's background sync work (Phase 19 §8, §9).
 */
data class SyncWorkSpec(
    val accountId: AccountId,
    val trigger: SyncTrigger,
    val intervalMinutes: Long = SyncScheduler.DEFAULT_PERIODIC_INTERVAL_MINUTES,
    val constraints: SyncConstraints = SyncConstraints(),
    val uniqueWorkName: String = AccountSyncWorker.uniqueWorkName(accountId),
    val lastScheduledEpochMs: Long = 0L,
)
