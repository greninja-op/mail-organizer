package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred

/**
 * Deterministic fake [GmailSyncApi] for sync engine tests (phase §45).
 *
 * Scripted per account:
 * - [pages]: queue of [MessagePage]s returned in order for [fetchPage].
 *   The fake validates the requested page token against the script when
 *   [strictTokens] is true.
 * - [failures]: exceptions thrown before succeeding, keyed
 *   `"<account>|<pageToken or FIRST>"`, each consumed once.
 * - [changePages]: queue of [ChangePage]s for [fetchChanges].
 * - [historyCursor]: value returned by [latestHistoryCursor].
 * - [invalidateHistory]: accounts for which [fetchChanges] throws
 *   [SyncApiException.HistoryInvalid].
 * - [pageGate]: when set, every [fetchPage] suspends on it (cancellation
 *   tests).
 *
 * Uses only synthetic ids (`m-1`, `t-1`) — never real email data.
 */
class FakeGmailSyncApi : GmailSyncApi {

    data class PageCall(val accountId: String, val pageToken: String?)

    var strictTokens: Boolean = true
    var pageGate: CompletableDeferred<Unit>? = null

    private val pages = ConcurrentHashMap<String, List<MessagePage>>()
    private val failures = ConcurrentHashMap<String, ArrayDeque<SyncApiException>>()
    private val changePages = ConcurrentHashMap<String, ArrayDeque<ChangePage>>()
    private val historyCursor = ConcurrentHashMap<String, String>()
    private val invalidateHistory = ConcurrentHashMap.newKeySet<String>()

    val pageCalls = mutableListOf<PageCall>()
    val fetchPageCount = AtomicInteger(0)
    val changesCalls = AtomicInteger(0)

    fun scriptPages(accountId: String, vararg scriptPages: MessagePage) {
        pages[accountId] = scriptPages.toList()
    }

    /** Failures thrown (in order) before the scripted page is returned. */
    fun scriptFailures(accountId: String, pageToken: String?, vararg errors: SyncApiException) {
        val key = failureKey(accountId, pageToken)
        failures.getOrPut(key) { ArrayDeque() }.addAll(errors.toList())
    }

    fun scriptChanges(accountId: String, vararg scriptChanges: ChangePage) {
        changePages[accountId] = ArrayDeque(scriptChanges.toList())
    }

    fun scriptHistoryCursor(accountId: String, cursor: String) {
        historyCursor[accountId] = cursor
    }

    fun scriptHistoryInvalid(accountId: String) {
        invalidateHistory.add(accountId)
    }

    private fun failureKey(accountId: String, pageToken: String?) =
        "$accountId|${pageToken ?: "FIRST"}"

    override suspend fun fetchPage(
        accountId: AccountId,
        pageToken: String?,
        pageSize: Int,
    ): MessagePage {
        // Record the call before any gate/failure so tests can observe
        // "fetch started" deterministically.
        fetchPageCount.incrementAndGet()
        synchronized(pageCalls) { pageCalls.add(PageCall(accountId.value, pageToken)) }
        pageGate?.await()
        failures[failureKey(accountId.value, pageToken)]?.removeFirstOrNull()?.let { throw it }
        val scripted = pages[accountId.value]
            ?: throw SyncApiException.InvalidRequest("no pages scripted for ${accountId.value}")
        // Token-aware serving, like real pagination: page[i] is served for the
        // token that page[i-1] advertised (null for the first page).
        val index = if (pageToken == null) {
            0
        } else {
            val idx = scripted.indexOfFirst { it.nextPageToken == pageToken }
            if (idx == -1 || idx + 1 >= scripted.size) {
                throw SyncApiException.InvalidRequest(
                    "unexpected page token '$pageToken' for ${accountId.value}",
                )
            }
            idx + 1
        }
        return scripted[index]
    }

    override suspend fun fetchChanges(accountId: AccountId, historyCursor: String): ChangePage {
        changesCalls.incrementAndGet()
        if (invalidateHistory.contains(accountId.value)) throw SyncApiException.HistoryInvalid()
        return changePages[accountId.value]?.removeFirstOrNull()
            ?: throw SyncApiException.InvalidRequest("no changes scripted for ${accountId.value}")
    }

    override suspend fun latestHistoryCursor(accountId: AccountId): String =
        historyCursor[accountId.value]
            ?: throw SyncApiException.InvalidRequest("no history cursor for ${accountId.value}")
}

/** Builds a synthetic remote message for tests. */
fun fakeRemoteMessage(
    id: String,
    threadId: String = "t-$id",
    from: String = "sender@example.test",
    subject: String = "Subject $id",
    unread: Boolean = true,
    labels: List<String> = listOf("INBOX"),
    timestampEpochMs: Long = 1_700_000_000_000L,
): RemoteMessage = RemoteMessage(
    id = id,
    threadId = threadId,
    fromAddress = from,
    fromName = "Sender $id",
    subject = subject,
    snippet = "snippet $id",
    timestampEpochMs = timestampEpochMs,
    unread = unread,
    labels = labels,
)
