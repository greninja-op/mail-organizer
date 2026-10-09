package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Thread/conversation persistence (Phase 2). */
@Dao
interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(thread: ThreadRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(threads: List<ThreadRecord>)

    @Query("SELECT * FROM threads WHERE threadId = :threadId")
    suspend fun getById(threadId: String): ThreadRecord?

    @Query(
        "SELECT * FROM threads WHERE accountId = :accountId " +
            "ORDER BY latestMessageEpochMs DESC LIMIT :limit",
    )
    fun observeByAccount(accountId: String, limit: Int): Flow<List<ThreadRecord>>

    @Query(
        "SELECT * FROM threads WHERE accountId = :accountId " +
            "ORDER BY latestMessageEpochMs DESC LIMIT :limit",
    )
    suspend fun getByAccount(accountId: String, limit: Int): List<ThreadRecord>

    @Query(
        "SELECT * FROM threads WHERE accountId = :accountId AND gmailThreadId = :gmailThreadId",
    )
    suspend fun getByGmailThreadId(accountId: String, gmailThreadId: String): ThreadRecord?

    @Query(
        "UPDATE threads SET messageCount = :messageCount, unreadCount = :unreadCount, " +
            "latestMessageId = :latestMessageId, latestMessageEpochMs = :latestMessageEpochMs, " +
            "updatedAtEpochMs = :updatedAtEpochMs WHERE threadId = :threadId",
    )
    suspend fun updateCounts(
        threadId: String,
        messageCount: Int,
        unreadCount: Int,
        latestMessageId: String?,
        latestMessageEpochMs: Long,
        updatedAtEpochMs: Long,
    )

    @Query("DELETE FROM threads WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
