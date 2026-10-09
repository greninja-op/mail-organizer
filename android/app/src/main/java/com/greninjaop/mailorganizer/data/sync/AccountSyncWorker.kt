package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.ui.mail.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Result of a background account sync worker execution (Phase 19 §6, §28). */
sealed interface SyncWorkerResult {
    data class Success(val summary: SyncSummary, val processing: ProcessingSummary?) : SyncWorkerResult
    data class Retry(val reason: String) : SyncWorkerResult
    data class Failure(val reason: String) : SyncWorkerResult
    data object Cancelled : SyncWorkerResult
}

/**
 * Account sync worker contract & execution engine (Phase 19 §6, §8, §43, §44).
 *
 * CRITICAL RULE (§44):
 * Active UI account MUST NOT control background sync. A worker carries its own
 * explicit [AccountId] and never consults the active UI account.
 *
 * Designed to execute within WorkManager or standalone background schedulers.
 * Survives process restarts and cleans up on cancellation.
 */
class AccountSyncWorker(
    private val syncCoordinator: SyncCoordinator,
    private val pipeline: BackgroundProcessingPipeline?,
    private val accountRepository: AccountRepository,
    private val connectivity: ConnectivityObserver? = null,
    private val dispatchers: AppDispatchers,
) {

    suspend fun doWork(
        accountId: AccountId,
        trigger: SyncTrigger,
        constraints: SyncConstraints = SyncConstraints(),
    ): SyncWorkerResult = withContext(dispatchers.io) {
        MoLogger.i(TAG, "Starting sync worker for account=${accountId.value} trigger=$trigger")

        // 1. Validate account exists and is enabled
        val account = accountRepository.getById(accountId.value)
        if (account == null) {
            MoLogger.w(TAG, "Account ${accountId.value} does not exist; failing worker")
            return@withContext SyncWorkerResult.Failure("Account not found")
        }
        if (!account.isEnabled) {
            MoLogger.i(TAG, "Account ${accountId.value} is disabled; skipping sync worker")
            return@withContext SyncWorkerResult.Failure("Account is disabled")
        }

        // 2. Check network constraint if observer is provided
        if (constraints.requiresNetwork && connectivity != null) {
            val isOnline = connectivity.isOnline.first()
            if (!isOnline) {
                MoLogger.i(TAG, "Worker deferred: device is offline for account=${accountId.value}")
                return@withContext SyncWorkerResult.Retry("Device is offline")
            }
        }

        // 3. Execute remote synchronization
        try {
            val outcome = syncCoordinator.syncNow(accountId, trigger)
            when (outcome) {
                is SyncOutcome.Success -> {
                    // 4. Run local intelligence processing pipeline
                    val processing = try {
                        pipeline?.processAccount(accountId.value)
                    } catch (t: Throwable) {
                        MoLogger.w(TAG, "Pipeline warning for ${accountId.value}: ${t.javaClass.simpleName}")
                        null
                    }
                    MoLogger.i(
                        TAG,
                        "Worker succeeded for account=${accountId.value} " +
                            "processed=${outcome.summary.messagesProcessed}",
                    )
                    SyncWorkerResult.Success(outcome.summary, processing)
                }

                is SyncOutcome.Failed -> {
                    when (val err = outcome.error) {
                        is MoError.Network -> {
                            MoLogger.w(TAG, "Worker transient network error for ${accountId.value}: ${err.message}")
                            SyncWorkerResult.Retry(err.message)
                        }

                        is MoError.RateLimited -> {
                            MoLogger.w(TAG, "Worker rate limited for ${accountId.value}: retryAfter=${err.retryAfterSeconds}")
                            SyncWorkerResult.Retry("Rate limited (retry after ${err.retryAfterSeconds ?: 60}s)")
                        }

                        is MoError.Authentication -> {
                            MoLogger.w(TAG, "Worker auth failure for ${accountId.value}: ${err.message}")
                            SyncWorkerResult.Failure(err.message)
                        }

                        is MoError.PermissionDenied -> {
                            MoLogger.w(TAG, "Worker permission denied for ${accountId.value}: ${err.permission}")
                            SyncWorkerResult.Failure("Permission denied: ${err.permission}")
                        }

                        is MoError.InvalidConfiguration -> {
                            MoLogger.w(TAG, "Worker invalid config for ${accountId.value}: ${err.message}")
                            SyncWorkerResult.Failure(err.message)
                        }

                        else -> {
                            MoLogger.w(TAG, "Worker failed for ${accountId.value}: $err")
                            SyncWorkerResult.Failure("Sync failed: $err")
                        }
                    }
                }

                SyncOutcome.Cancelled -> {
                    MoLogger.i(TAG, "Worker cancelled for account=${accountId.value}")
                    SyncWorkerResult.Cancelled
                }
            }
        } catch (ce: CancellationException) {
            MoLogger.i(TAG, "Worker coroutine cancelled for account=${accountId.value}")
            SyncWorkerResult.Cancelled
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Worker unexpected exception for account=${accountId.value}", t)
            SyncWorkerResult.Failure(t.message ?: "Unexpected failure")
        }
    }

    companion object {
        const val TAG = "AccountSyncWorker"

        /**
         * Unique WorkManager work name for an account (Phase 19 §9).
         * Format: `mail-organizer-sync-{accountId}` to prevent cross-account collisions.
         */
        fun uniqueWorkName(accountId: AccountId): String = "mail-organizer-sync-${accountId.value}"
    }
}
