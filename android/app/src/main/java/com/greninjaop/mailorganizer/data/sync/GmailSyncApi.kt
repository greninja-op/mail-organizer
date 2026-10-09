package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId

/**
 * Remote Gmail access seam (Phase 4).
 *
 * This interface is the ONLY place the sync engine touches the network.
 * It deliberately models app-level concepts (pages, changes, cursors) —
 * not raw Gmail REST shapes — so the engine never depends on API wire
 * formats (phase §44 normalization boundary).
 *
 * The real implementation lands with Phase 3 (Google OAuth), once
 * credentials exist. Its mapping will be:
 * - [fetchPage]        → users.messages.list + users.messages.get (batch,
 *                        format=METADATA with minimal headers + snippet)
 * - [fetchChanges]     → users.history.list(startHistoryId)
 * - [latestHistoryCursor] → users.getProfile → historyId
 *
 * Until then, [DeferredGmailSyncApi] fails closed and tests use
 * [FakeGmailSyncApi][test doubles]. There is intentionally NO fake
 * implementation that pretends to sync.
 */

/** Minimal remote message representation (phase §14: minimum viable data). */
data class RemoteMessage(
    val id: String,
    val threadId: String,
    val fromAddress: String,
    val fromName: String?,
    val toAddresses: List<String> = emptyList(),
    val ccAddresses: List<String> = emptyList(),
    val subject: String = "",
    val snippet: String? = null,
    /** Minimal plain-text body; null when only headers/snippet were fetched. */
    val bodyText: String? = null,
    val timestampEpochMs: Long,
    val unread: Boolean = true,
    val starred: Boolean = false,
    /** Gmail label ids as remote metadata (phase §37–38: kept separate from MO categories). */
    val labels: List<String> = emptyList(),
    val sizeBytes: Long? = null,
    // NOTE (phase §36): attachment binaries are NEVER part of this model.
)

/** One page of an initial/paged listing (phase §12–13). */
data class MessagePage(
    val messages: List<RemoteMessage>,
    /** Null = end of pagination. Never ignored, never re-requested blindly. */
    val nextPageToken: String?,
)

/** Incremental change set (phase §23). */
data class ChangePage(
    val messages: List<RemoteMessage>,
    /** Remote ids the server reports as deleted. */
    val deletedRemoteIds: List<String> = emptyList(),
    val nextHistoryCursor: String,
)

/**
 * Failures the sync engine must distinguish (phase §25, §32).
 * Only transient categories are retried — see [SyncRetryPolicy].
 */
sealed class SyncApiException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** No connectivity / timeout / TLS — retryable. */
    class NetworkError(cause: Throwable? = null) : SyncApiException("Network unavailable", cause)

    /** 5xx from Gmail — retryable. */
    class ServerError(val code: Int) : SyncApiException("Gmail API error $code")

    /** 429/403 rateLimitExceeded — retryable, honor [retryAfterSeconds]. */
    class RateLimited(val retryAfterSeconds: Long?) : SyncApiException("Gmail rate limit hit")

    /** 401/invalid_grant — NOT retried; user must reconnect (Phase 3). */
    class AuthExpired : SyncApiException("Gmail authorization expired")

    /** 403 insufficient permissions — NOT retried. */
    class PermissionDenied : SyncApiException("Gmail permission denied")

    /** 400 malformed request — NOT retried; a bug, not a blip. */
    class InvalidRequest(message: String) : SyncApiException(message)

    /** 404 historyId invalid — NOT retried; triggers controlled re-baseline (§24). */
    class HistoryInvalid : SyncApiException("History cursor invalid")

    /**
     * Fail-closed stand-in state: no authenticated client is configured
     * (Phase 3 deferred). NOT retried as a network blip.
     */
    class NotConfigured :
        SyncApiException("Gmail account not connected — authorization is deferred")
}

interface GmailSyncApi {

    /**
     * Fetch one page of messages. [pageToken] null = first page.
     * Throws [SyncApiException] subclasses on failure; supports cancellation.
     */
    suspend fun fetchPage(
        accountId: AccountId,
        pageToken: String?,
        pageSize: Int,
    ): MessagePage

    /**
     * Fetch changes since [historyCursor] (Gmail history API).
     * Throws [SyncApiException.HistoryInvalid] when the cursor expired.
     */
    suspend fun fetchChanges(
        accountId: AccountId,
        historyCursor: String,
    ): ChangePage

    /** Current server history id, used to seed incremental sync after initial sync. */
    suspend fun latestHistoryCursor(accountId: AccountId): String
}

/**
 * Fail-closed [GmailSyncApi] used until Phase 3 provides the real
 * authenticated client. Every call fails with [SyncApiException.NotConfigured],
 * which the engine maps to [com.greninjaop.mailorganizer.MoError.InvalidConfiguration].
 *
 * This is NOT a fake sync implementation: it performs no synchronization
 * and returns no data. It exists so the app fails honestly ("not connected")
 * instead of crashing or pretending.
 */
class DeferredGmailSyncApi : GmailSyncApi {
    private fun fail(): Nothing =
        throw SyncApiException.NotConfigured()

    override suspend fun fetchPage(
        accountId: AccountId,
        pageToken: String?,
        pageSize: Int,
    ): MessagePage = fail()

    override suspend fun fetchChanges(
        accountId: AccountId,
        historyCursor: String,
    ): ChangePage = fail()

    override suspend fun latestHistoryCursor(accountId: AccountId): String = fail()
}
