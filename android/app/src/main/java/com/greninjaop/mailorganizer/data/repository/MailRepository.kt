package com.greninjaop.mailorganizer.data.repository

import androidx.room.withTransaction
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Mailbox operations: threads and messages (Phase 2).
 *
 * [saveThreadWithMessages] is the transaction pattern the Phase 4 sync
 * engine will reuse: a thread and its messages are written atomically, so
 * a crash mid-sync can never leave orphaned message rows.
 */
interface MailRepository {
    suspend fun saveThreadWithMessages(thread: ThreadRecord, messages: List<MessageRecord>)
    fun observeThreads(accountId: String, limit: Int = 50): Flow<List<ThreadRecord>>
    fun observeMessages(threadId: String, limit: Int = 100): Flow<List<MessageRecord>>
    fun observeUnread(accountId: String, limit: Int = 50): Flow<List<MessageRecord>>
    fun observeStarred(accountId: String, limit: Int = 50): Flow<List<MessageRecord>>
    suspend fun searchByText(accountId: String, query: String, limit: Int = 50): List<MessageRecord>
    suspend fun setRead(messageId: String, read: Boolean)
    suspend fun setStarred(messageId: String, starred: Boolean)
    suspend fun countByAccount(accountId: String): Int

    /**
     * Message rows by local id — one bounded query (Phase 6: the thread list
     * resolves each visible thread's latest message without N+1 lookups).
     * Empty input returns empty output (Room rejects empty IN clauses).
     */
    suspend fun getMessagesByIds(messageIds: List<String>): List<MessageRecord>

    // ---- Phase 4 sync engine support ----

    /** Thread row for a Gmail thread id, or null if never synced. */
    suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String): ThreadRecord?

    /**
     * Recomputes a thread's aggregates (message/unread counts, latest
     * message) from its stored messages. Called after each page persist so
     * thread rows stay truthful across paged/incremental syncs.
     */
    suspend fun updateThreadAggregates(threadId: String)

    /** Gmail ids already stored for [accountId] (idempotency pre-check). */
    suspend fun existingGmailIds(accountId: String, gmailIds: List<String>): Set<String>

    /**
     * Deletes synced messages by Gmail id; returns the touched local thread
     * ids so aggregates can be refreshed. No-op for empty input.
     */
    suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>): List<String>

    // ---- Phase 7 classification support ----

    /** One message by local id, or null. */
    suspend fun getMessage(messageId: String): MessageRecord?

    /**
     * Messages with no classification row yet (incremental classification).
     * Bounded; newest first.
     */
    suspend fun getUnclassifiedMessages(accountId: String, limit: Int): List<MessageRecord>

    /** Messages carrying one Gmail label (e.g. "SPAM", "CATEGORY_SOCIAL"). */
    fun observeByLabel(accountId: String, label: String, limit: Int = 50): Flow<List<MessageRecord>>
}

class RoomMailRepository(
    private val db: AppDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : MailRepository {

    override suspend fun saveThreadWithMessages(
        thread: ThreadRecord,
        messages: List<MessageRecord>,
    ) = withContext(dispatchers.io) {
        db.withTransaction {
            db.threadDao().upsert(thread)
            db.messageDao().upsertAll(messages)
        }
    }

    override fun observeThreads(accountId: String, limit: Int): Flow<List<ThreadRecord>> =
        db.threadDao().observeByAccount(accountId, limit)

    override fun observeMessages(threadId: String, limit: Int): Flow<List<MessageRecord>> =
        db.messageDao().observeByThread(threadId, limit)

    override fun observeUnread(accountId: String, limit: Int): Flow<List<MessageRecord>> =
        db.messageDao().observeUnreadByAccount(accountId, limit)

    override fun observeStarred(accountId: String, limit: Int): Flow<List<MessageRecord>> =
        db.messageDao().observeStarredByAccount(accountId, limit)

    override suspend fun searchByText(
        accountId: String,
        query: String,
        limit: Int,
    ): List<MessageRecord> = withContext(dispatchers.io) {
        db.messageDao().searchByText(accountId, query, limit)
    }

    override suspend fun setRead(messageId: String, read: Boolean) =
        withContext(dispatchers.io) {
            db.messageDao().setUnread(messageId, !read)
        }

    // ---- Phase 7 classification support ----

    override suspend fun getMessage(messageId: String): MessageRecord? =
        withContext(dispatchers.io) {
            db.messageDao().getById(messageId)
        }

    override suspend fun getUnclassifiedMessages(
        accountId: String,
        limit: Int,
    ): List<MessageRecord> = withContext(dispatchers.io) {
        db.messageDao().getUnclassified(accountId, limit)
    }

    override fun observeByLabel(
        accountId: String,
        label: String,
        limit: Int,
    ): Flow<List<MessageRecord>> =
        db.messageDao().observeByLabel(accountId, label, limit)

    override suspend fun setStarred(messageId: String, starred: Boolean) =
        withContext(dispatchers.io) {
            db.messageDao().setStarred(messageId, starred)
        }

    override suspend fun countByAccount(accountId: String): Int =
        withContext(dispatchers.io) {
            db.messageDao().countByAccount(accountId)
        }

    override suspend fun getMessagesByIds(messageIds: List<String>): List<MessageRecord> =
        withContext(dispatchers.io) {
            if (messageIds.isEmpty()) emptyList()
            else db.messageDao().getByIds(messageIds)
        }

    override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) =
        withContext(dispatchers.io) {
            db.threadDao().getByGmailThreadId(accountId, gmailThreadId)
        }

    override suspend fun updateThreadAggregates(threadId: String) =
        withContext(dispatchers.io) {
            val messages = db.messageDao().getByThread(threadId)
            val latest = messages.maxByOrNull { it.timestampEpochMs } ?: return@withContext
            db.threadDao().updateCounts(
                threadId = threadId,
                messageCount = messages.size,
                unreadCount = messages.count { it.unread },
                latestMessageId = latest.messageId,
                latestMessageEpochMs = latest.timestampEpochMs,
                updatedAtEpochMs = clock(),
            )
        }

    override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>): Set<String> =
        withContext(dispatchers.io) {
            if (gmailIds.isEmpty()) emptySet()
            else db.messageDao().existingGmailIds(accountId, gmailIds).toSet()
        }

    override suspend fun deleteMessagesByGmailIds(
        accountId: String,
        gmailIds: List<String>,
    ): List<String> = withContext(dispatchers.io) {
        if (gmailIds.isEmpty()) return@withContext emptyList()
        db.withTransaction {
            val threadIds = db.messageDao().threadIdsForGmailIds(accountId, gmailIds)
            for (gmailId in gmailIds) db.messageDao().deleteByGmailId(accountId, gmailId)
            threadIds
        }
    }
}
