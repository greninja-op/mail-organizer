package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Email message persistence (Phase 2).
 *
 * All list queries are bounded ([limit]) — the data layer never loads an
 * entire mailbox into memory (phase §41). Pagination cursors arrive with the
 * sync engine (Phase 4); until then callers page with explicit limits.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageRecord>)

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getById(messageId: String): MessageRecord?

    @Query(
        "SELECT * FROM messages WHERE threadId = :threadId " +
            "ORDER BY timestampEpochMs ASC LIMIT :limit",
    )
    fun observeByThread(threadId: String, limit: Int): Flow<List<MessageRecord>>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeByAccount(accountId: String, limit: Int): Flow<List<MessageRecord>>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND unread = 1 " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeUnreadByAccount(accountId: String, limit: Int): Flow<List<MessageRecord>>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND starred = 1 " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeStarredByAccount(accountId: String, limit: Int): Flow<List<MessageRecord>>

    /** Bounded subject/snippet search foundation (full local index is Phase 10). */
    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND " +
            "(subject LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%') " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    suspend fun searchByText(accountId: String, query: String, limit: Int): List<MessageRecord>

    @Query("UPDATE messages SET unread = :unread WHERE messageId = :messageId")
    suspend fun setUnread(messageId: String, unread: Boolean)

    @Query("UPDATE messages SET starred = :starred WHERE messageId = :messageId")
    suspend fun setStarred(messageId: String, starred: Boolean)

    @Query("SELECT COUNT(*) FROM messages WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: String): Int

    @Query("DELETE FROM messages WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    // ---- Phase 4 sync engine support ----

    /** Gmail ids already stored for [accountId] (idempotency pre-check, phase §18). */
    @Query(
        "SELECT gmailMessageId FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId IN (:gmailIds)",
    )
    suspend fun existingGmailIds(accountId: String, gmailIds: List<String>): List<String>

    /** All messages of one thread (bounded by thread size; phase §42). */
    @Query("SELECT * FROM messages WHERE threadId = :threadId")
    suspend fun getByThread(threadId: String): List<MessageRecord>

    /** Local thread ids owning any of [gmailIds] (for aggregate refresh after deletes). */
    @Query(
        "SELECT DISTINCT threadId FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId IN (:gmailIds)",
    )
    suspend fun threadIdsForGmailIds(accountId: String, gmailIds: List<String>): List<String>

    /** Removes one synced message by its Gmail id (incremental delete, phase §23). */
    @Query(
        "DELETE FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId = :gmailMessageId",
    )
    suspend fun deleteByGmailId(accountId: String, gmailMessageId: String)
}
