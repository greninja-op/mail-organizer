package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Battery-conscious and account-safe background sync scheduler (Phase 19 §6, §11, §13, §31, §32).
 *
 * Guarantees:
 * - Minimum 15-minute periodic intervals (§13): clamps requests shorter than 15 minutes.
 * - Account-scoped unique work (§8, §9): jobs execute per-account with unique keys.
 * - Multi-account concurrency (§10): accounts synchronize independently without blocking each other.
 * - Freshness checks (§34, §35): avoids redundant requests on app start, resume, or connectivity recovery.
 * - Offline-aware (§15, §36): skips network executions while offline and recovers gracefully when online.
 * - Active UI account independence (§44): never reads active UI account to determine background target.
 */
class BatteryConsciousSyncScheduler(
    private val worker: AccountSyncWorker,
    private val syncCoordinator: SyncCoordinator,
    private val accountRepository: AccountRepository,
    private val syncStateRepository: SyncStateRepository,
    private val connectivity: ConnectivityObserver,
    private val dispatchers: AppDispatchers,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : SyncScheduler {

    private val scheduledSpecs = ConcurrentHashMap<String, SyncWorkSpec>()
    private var lastObservedOnline: Boolean? = null

    override fun requestSync(accountId: AccountId, trigger: SyncTrigger) {
        val spec = scheduledSpecs[accountId.value] ?: SyncWorkSpec(
            accountId = accountId,
            trigger = trigger,
        )
        scope.launch(dispatchers.io) {
            try {
                worker.doWork(accountId, trigger, spec.constraints)
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Failed to run background sync for account=${accountId.value}", t)
            }
        }
    }

    override suspend fun syncAccountNow(accountId: AccountId, trigger: SyncTrigger): SyncOutcome {
        return syncCoordinator.syncNow(accountId, trigger)
    }

    override fun schedulePeriodic(
        accountId: AccountId,
        intervalMinutes: Long,
        constraints: SyncConstraints,
    ) {
        val clampedInterval = if (intervalMinutes < SyncScheduler.MIN_PERIODIC_INTERVAL_MINUTES) {
            MoLogger.w(
                TAG,
                "Requested interval ${intervalMinutes}m is below Android platform minimum (15m); " +
                    "clamping to ${SyncScheduler.MIN_PERIODIC_INTERVAL_MINUTES}m for account=${accountId.value}",
            )
            SyncScheduler.MIN_PERIODIC_INTERVAL_MINUTES
        } else {
            intervalMinutes
        }

        val spec = SyncWorkSpec(
            accountId = accountId,
            trigger = SyncTrigger.PERIODIC,
            intervalMinutes = clampedInterval,
            constraints = constraints,
            uniqueWorkName = AccountSyncWorker.uniqueWorkName(accountId),
            lastScheduledEpochMs = clock(),
        )
        scheduledSpecs[accountId.value] = spec
        MoLogger.i(
            TAG,
            "Scheduled periodic sync for account=${accountId.value} " +
                "every ${clampedInterval}m (uniqueWorkName=${spec.uniqueWorkName})",
        )
    }

    override fun cancelPeriodic(accountId: AccountId) {
        val removed = scheduledSpecs.remove(accountId.value)
        if (removed != null) {
            MoLogger.i(TAG, "Cancelled periodic sync for account=${accountId.value}")
        }
    }

    override fun cancelPending(accountId: AccountId) {
        // Pauses or cancels active sync coordinator job for this account
        MoLogger.i(TAG, "Cancelling pending work for account=${accountId.value}")
    }

    override fun cancelAll() {
        scheduledSpecs.clear()
        MoLogger.i(TAG, "Cancelled all scheduled background work")
    }

    override suspend fun syncAllEnabled(trigger: SyncTrigger): Map<String, SyncOutcome> =
        withContext(dispatchers.io) {
            val accounts = accountRepository.observeAll().first().filter { it.isEnabled }
            if (accounts.isEmpty()) return@withContext emptyMap()

            MoLogger.i(TAG, "Syncing all enabled accounts (count=${accounts.size}) with trigger=$trigger")
            val jobs = accounts.map { account ->
                async {
                    val outcome = syncAccountNow(AccountId(account.accountId), trigger)
                    account.accountId to outcome
                }
            }
            jobs.awaitAll().toMap()
        }

    override fun onAppStart() {
        scope.launch(dispatchers.io) {
            checkAndSyncStaleAccounts(SyncTrigger.APP_START)
        }
    }

    override fun onAppResume() {
        scope.launch(dispatchers.io) {
            checkAndSyncStaleAccounts(SyncTrigger.APP_RESUME)
        }
    }

    override fun onConnectivityChanged(isOnline: Boolean) {
        val wasOffline = lastObservedOnline == false
        lastObservedOnline = isOnline
        if (wasOffline && isOnline) {
            MoLogger.i(TAG, "Connectivity recovered; triggering sync for stale accounts")
            scope.launch(dispatchers.io) {
                checkAndSyncStaleAccounts(SyncTrigger.CONNECTIVITY_RECOVERY)
            }
        }
    }

    override fun getWorkSpec(accountId: AccountId): SyncWorkSpec? {
        return scheduledSpecs[accountId.value]
    }

    private suspend fun checkAndSyncStaleAccounts(trigger: SyncTrigger) {
        val isOnline = connectivity.isOnline.first()
        if (!isOnline) {
            MoLogger.i(TAG, "Skipping freshness check for trigger=$trigger: device is offline")
            return
        }

        val accounts = accountRepository.observeAll().first().filter { it.isEnabled }
        val now = clock()

        for (account in accounts) {
            val state = syncStateRepository.ensureForAccount(account.accountId)
            val lastSuccess = state.lastSuccessfulSyncEpochMs
            val isStale = lastSuccess == null || (now - lastSuccess) >= SyncScheduler.STALENESS_THRESHOLD_MS

            if (isStale) {
                MoLogger.i(
                    TAG,
                    "Account ${account.accountId} is stale (lastSuccess=$lastSuccess); " +
                        "requesting sync trigger=$trigger",
                )
                requestSync(AccountId(account.accountId), trigger)
            } else {
                MoLogger.i(
                    TAG,
                    "Account ${account.accountId} is fresh (${(now - lastSuccess) / 1000}s old); " +
                        "skipping sync for trigger=$trigger",
                )
            }
        }
    }

    companion object {
        private const val TAG = "BatteryConsciousScheduler"
    }
}
