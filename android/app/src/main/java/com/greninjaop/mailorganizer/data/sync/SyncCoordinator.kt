package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.local.SyncStatus
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What triggered a sync run. */
enum class SyncTrigger { MANUAL, PERIODIC, APP_START }

/** Honest progress stages (phase §31: stages, never fabricated percentages). */
enum class SyncStage { CONNECTING, FETCHING, SAVING, FINALIZING }

sealed interface SyncProgress {
    data object Idle : SyncProgress
    data class Running(val stage: SyncStage, val messagesProcessed: Int) : SyncProgress
    data class Succeeded(val summary: SyncSummary) : SyncProgress
    data class Failed(val error: MoError) : SyncProgress
}

data class SyncSummary(
    val accountId: String,
    val trigger: SyncTrigger,
    val messagesProcessed: Int,
    val inserted: Int,
    val updated: Int,
    val deleted: Int,
    val completedInitialSync: Boolean,
    val durationMs: Long,
)

sealed interface SyncOutcome {
    data class Success(val summary: SyncSummary) : SyncOutcome
    data class Failed(val error: MoError) : SyncOutcome
    data object Cancelled : SyncOutcome
}

data class SyncConfig(
    /** Gmail page size per API call (phase §13, §42: bounded memory). */
    val pageSize: Int = 50,
    /**
     * Max messages synced in one run (phase §11: never blindly download an
     * unlimited mailbox). The run stops early with the page cursor saved;
     * the next run resumes where this one stopped.
     */
    val maxMessagesPerRun: Int = 500,
)

/**
 * Gmail synchronization engine (Phase 4).
 *
 * Owns the whole pipeline: [GmailSyncApi] → normalize → [MailRepository] →
 * Room, with per-account [SyncStateRepository] bookkeeping. Guarantees:
 *
 * - Account isolation: one account per run; a per-account [Mutex] prevents
 *   concurrent syncs of the same account (phase §9, §30).
 * - Rapid-tap dedup: while a sync is in flight for an account, extra
 *   [syncNow] calls await the same result instead of starting new jobs
 *   (phase §29).
 * - Cursor safety: the sync cursor advances ONLY after the corresponding
 *   page is committed (phase §21–22). A failed sync never advances state.
 * - Idempotency: stable local ids (`account:gmailId`) + upsert semantics —
 *   re-running a sync inserts nothing twice (phase §18).
 * - Cancellation: coroutine cancellation pauses the sync (state → PAUSED),
 *   leaves committed data intact, and converts to [SyncOutcome.Cancelled]
 *   rather than being swallowed (phase §27).
 * - No writes to Gmail, ever (phase §6). No email bodies/subjects in logs.
 */
class SyncCoordinator(
    private val api: GmailSyncApi,
    private val mail: MailRepository,
    private val syncState: SyncStateRepository,
    private val dispatchers: AppDispatchers,
    private val retryPolicy: SyncRetryPolicy = SyncRetryPolicy(),
    private val config: SyncConfig = SyncConfig(),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val _progress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    private val locks = ConcurrentHashMap<String, Mutex>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<SyncOutcome>>()

    /**
     * Runs a sync for [accountId], or joins the in-flight one.
     * Safe to call from UI ("Sync now"); never throws for expected sync
     * failures — those are returned as [SyncOutcome.Failed].
     */
    suspend fun syncNow(
        accountId: AccountId,
        trigger: SyncTrigger = SyncTrigger.MANUAL,
    ): SyncOutcome {
        // Fast path: join an already-running sync instead of stacking jobs.
        inFlight[accountId.value]?.let { if (!it.isCompleted) return it.await() }
        val lock = locks.getOrPut(accountId.value) { Mutex() }
        return lock.withLock {
            inFlight[accountId.value]?.let { if (!it.isCompleted) return@withLock it.await() }
            val deferred = CompletableDeferred<SyncOutcome>()
            inFlight[accountId.value] = deferred
            try {
                val outcome = runSync(accountId, trigger)
                deferred.complete(outcome)
                outcome
            } catch (t: Throwable) {
                // runSync converts expected failures (incl. cancellation) to
                // outcomes; anything reaching here is a bug — don't swallow.
                deferred.completeExceptionally(t)
                throw t
            } finally {
                inFlight.remove(accountId.value, deferred)
            }
        }
    }

    private suspend fun runSync(accountId: AccountId, trigger: SyncTrigger): SyncOutcome {
        // NOTE: the CancellationException catch MUST stay outside the
        // withContext below. withContext rethrows cancellation even when the
        // block itself caught it, so converting cancellation to a terminal
        // outcome has to happen here, not inside.
        return try {
            withContext(dispatchers.io) {
                doSync(accountId, trigger)
            }
        } catch (ce: CancellationException) {
            // Expected user/system cancellation: pause, keep committed
            // data, report honestly. Converted to an outcome (not
            // rethrown) so "Sync now" callers get a terminal result.
            // Cleanup runs NonCancellable: the coroutine is already
            // cancelled, so ordinary withContext would rethrow before
            // the PAUSED state is persisted.
            MoLogger.i(TAG, "sync cancelled account=${accountId.value}")
            withContext(NonCancellable) {
                syncState.markAttempt(accountId.value, SyncStatus.PAUSED)
            }
            _progress.value = SyncProgress.Idle
            SyncOutcome.Cancelled
        } catch (e: SyncApiException) {
            val error = e.toMoError()
            MoLogger.w(TAG, "sync failed account=${accountId.value} error=${e.errorCode()}")
            // State write here is best-effort: if we are being cancelled
            // concurrently it may throw; the PAUSED path above owns that case.
            syncState.markAttempt(accountId.value, SyncStatus.FAILED, e.errorCode())
            _progress.value = SyncProgress.Failed(error)
            SyncOutcome.Failed(error)
        } catch (t: Throwable) {
            // Persistence or unexpected failures: existing data is left
            // intact; only the sync state moves to FAILED (phase §34).
            MoLogger.e(TAG, "sync error account=${accountId.value}", t)
            syncState.markAttempt(accountId.value, SyncStatus.FAILED, ERROR_DATABASE)
            _progress.value = SyncProgress.Failed(MoError.Database("Sync failed", t))
            SyncOutcome.Failed(MoError.Database("Sync failed", t))
        }
    }

    /** The actual sync body; runs on [AppDispatchers.io]. */
    private suspend fun doSync(accountId: AccountId, trigger: SyncTrigger): SyncOutcome {
        val startMs = clock()
        MoLogger.i(TAG, "sync start account=${accountId.value} trigger=$trigger")
        _progress.value = SyncProgress.Running(SyncStage.CONNECTING, 0)
        val state = syncState.ensureForAccount(accountId.value)
        syncState.markAttempt(accountId.value, SyncStatus.RUNNING)
        val summary = when (val cursor = SyncCursor.decode(state.cursor)) {
            is SyncCursor.History -> runIncremental(accountId, cursor, trigger, startMs)
            is SyncCursor.Page -> runPaged(accountId, cursor.pageToken, trigger, startMs)
        }
        MoLogger.i(
            TAG,
            "sync success account=${accountId.value} " +
                "processed=${summary.messagesProcessed} " +
                "inserted=${summary.inserted} updated=${summary.updated} " +
                "deleted=${summary.deleted} durationMs=${summary.durationMs}",
        )
        _progress.value = SyncProgress.Succeeded(summary)
        return SyncOutcome.Success(summary)
    }

    /**
     * Initial (paged) sync. Loops pages, persisting each page transactionally
     * and advancing the cursor only after commit (phase §21–22). Bounded by
     * [SyncConfig.maxMessagesPerRun]; an early stop saves the page cursor so
     * the next run resumes seamlessly.
     */
    private suspend fun runPaged(
        accountId: AccountId,
        startToken: String?,
        trigger: SyncTrigger,
        startMs: Long,
    ): SyncSummary {
        var pageToken = startToken
        var processed = 0
        var inserted = 0
        var updated = 0
        do {
            _progress.value = SyncProgress.Running(SyncStage.FETCHING, processed)
            val page = retryPolicy.withRetries {
                api.fetchPage(accountId, pageToken, config.pageSize)
            }
            if (page.messages.isEmpty() && page.nextPageToken == null) break
            _progress.value = SyncProgress.Running(SyncStage.SAVING, processed)
            val (pageInserted, pageUpdated) = persistPage(accountId, page.messages)
            inserted += pageInserted
            updated += pageUpdated
            processed += page.messages.size
            pageToken = page.nextPageToken
            // Cursor advances only after the page is safely committed.
            syncState.updateCursor(accountId.value, SyncCursor.Page(pageToken).encode())
            if (processed >= config.maxMessagesPerRun && pageToken != null) break
        } while (pageToken != null)

        _progress.value = SyncProgress.Running(SyncStage.FINALIZING, processed)
        return if (pageToken == null) {
            // Initial sync reached end of pagination: seed incremental mode.
            val historyId = retryPolicy.withRetries { api.latestHistoryCursor(accountId) }
            val cursor = SyncCursor.History(historyId).encode()
            syncState.markSuccess(accountId.value, cursor)
            SyncSummary(
                accountId = accountId.value,
                trigger = trigger,
                messagesProcessed = processed,
                inserted = inserted,
                updated = updated,
                deleted = 0,
                completedInitialSync = true,
                durationMs = clock() - startMs,
            )
        } else {
            // Bounded early stop: page cursor already saved; stay resumable.
            syncState.markAttempt(accountId.value, SyncStatus.IDLE)
            SyncSummary(
                accountId = accountId.value,
                trigger = trigger,
                messagesProcessed = processed,
                inserted = inserted,
                updated = updated,
                deleted = 0,
                completedInitialSync = false,
                durationMs = clock() - startMs,
            )
        }
    }

    /**
     * Incremental sync via the history API (phase §23). An invalid/expired
     * history cursor triggers a controlled re-baseline, never a crash and
     * never a silent "still in sync" lie (phase §24).
     */
    private suspend fun runIncremental(
        accountId: AccountId,
        cursor: SyncCursor.History,
        trigger: SyncTrigger,
        startMs: Long,
    ): SyncSummary {
        _progress.value = SyncProgress.Running(SyncStage.FETCHING, 0)
        val changes = try {
            retryPolicy.withRetries { api.fetchChanges(accountId, cursor.historyId) }
        } catch (e: SyncApiException.HistoryInvalid) {
            MoLogger.i(TAG, "history cursor invalid account=${accountId.value}; re-baselining")
            return runPaged(accountId, startToken = null, trigger, startMs)
        }

        var processed = 0
        var inserted = 0
        var updated = 0
        var deleted = 0
        if (changes.messages.isNotEmpty()) {
            _progress.value = SyncProgress.Running(SyncStage.SAVING, 0)
            val (pageInserted, pageUpdated) = persistPage(accountId, changes.messages)
            inserted += pageInserted
            updated += pageUpdated
            processed += changes.messages.size
        }
        if (changes.deletedRemoteIds.isNotEmpty()) {
            deleted = applyDeletions(accountId, changes.deletedRemoteIds)
        }

        _progress.value = SyncProgress.Running(SyncStage.FINALIZING, processed)
        val nextCursor = SyncCursor.History(changes.nextHistoryCursor).encode()
        syncState.markSuccess(accountId.value, nextCursor)
        return SyncSummary(
            accountId = accountId.value,
            trigger = trigger,
            messagesProcessed = processed,
            inserted = inserted,
            updated = updated,
            deleted = deleted,
            completedInitialSync = false,
            durationMs = clock() - startMs,
        )
    }

    /**
     * Persists one page of remote messages, grouped by thread. Each
     * thread+messages group is written atomically via
     * [MailRepository.saveThreadWithMessages]; thread aggregates are then
     * recomputed from the database so counts stay truthful across pages.
     * Returns (inserted, updated) counts for observability (phase §40).
     */
    private suspend fun persistPage(
        accountId: AccountId,
        remote: List<RemoteMessage>,
    ): Pair<Int, Int> {
        if (remote.isEmpty()) return 0 to 0
        val gmailIds = remote.map { it.id }
        val knownIds = mail.existingGmailIds(accountId.value, gmailIds)
        var inserted = 0
        var updated = 0
        for ((gmailThreadId, threadRemote) in remote.groupBy { it.threadId }) {
            val records = threadRemote.map { it.toMessageRecord(accountId) }
            val thread = mail.getThreadByGmailId(accountId.value, gmailThreadId)
                ?: buildThreadRecord(accountId, gmailThreadId, records, clock())
            mail.saveThreadWithMessages(thread, records)
            mail.updateThreadAggregates(thread.threadId)
            for (id in gmailIds.filter { gid -> threadRemote.any { it.id == gid } }) {
                if (knownIds.contains(id)) updated++ else inserted++
            }
        }
        return inserted to updated
    }

    private suspend fun applyDeletions(accountId: AccountId, gmailIds: List<String>): Int {
        val touchedThreads = mail.deleteMessagesByGmailIds(accountId.value, gmailIds)
        for (threadId in touchedThreads) mail.updateThreadAggregates(threadId)
        return gmailIds.size
    }

    private fun SyncApiException.toMoError(): MoError = when (this) {
        is SyncApiException.NetworkError -> MoError.Network("Couldn't sync right now.", cause)
        is SyncApiException.ServerError -> MoError.Api(code, "Gmail API error $code.")
        is SyncApiException.RateLimited ->
            MoError.RateLimited(retryAfterSeconds)
        is SyncApiException.AuthExpired ->
            MoError.Authentication("Gmail authorization has expired. Reconnect your account.")
        is SyncApiException.PermissionDenied ->
            MoError.PermissionDenied("gmail")
        is SyncApiException.InvalidRequest ->
            MoError.Api(null, "Invalid sync request.")
        is SyncApiException.HistoryInvalid ->
            MoError.Unexpected(cause)
        is SyncApiException.NotConfigured ->
            MoError.InvalidConfiguration("Gmail account not connected yet.")
    }

    private fun SyncApiException.errorCode(): String = when (this) {
        is SyncApiException.NetworkError -> ERROR_NETWORK
        is SyncApiException.ServerError -> ERROR_API
        is SyncApiException.RateLimited -> ERROR_RATE_LIMITED
        is SyncApiException.AuthExpired -> ERROR_AUTH
        is SyncApiException.PermissionDenied -> ERROR_PERMISSION
        is SyncApiException.InvalidRequest -> ERROR_API
        is SyncApiException.HistoryInvalid -> ERROR_HISTORY_INVALID
        is SyncApiException.NotConfigured -> ERROR_NOT_CONFIGURED
    }

    private companion object {
        const val TAG = "SyncCoordinator"
        const val ERROR_NETWORK = "network"
        const val ERROR_API = "api"
        const val ERROR_RATE_LIMITED = "rate_limited"
        const val ERROR_AUTH = "auth"
        const val ERROR_PERMISSION = "permission"
        const val ERROR_HISTORY_INVALID = "history_invalid"
        const val ERROR_NOT_CONFIGURED = "not_configured"
        const val ERROR_DATABASE = "database"
    }
}
