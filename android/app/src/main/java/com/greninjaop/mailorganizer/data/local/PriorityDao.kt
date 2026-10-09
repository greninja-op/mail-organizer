package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Priority persistence (Phase 2 — storage only; calculation is Phase 9). */
@Dao
interface PriorityDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: PriorityRecord): Long

    @Query("DELETE FROM priorities WHERE messageId = :messageId")
    suspend fun deleteByMessage(messageId: String)

    @Transaction
    suspend fun setPriority(record: PriorityRecord) {
        deleteByMessage(record.messageId)
        insert(record)
    }

    @Query("SELECT * FROM priorities WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: String): PriorityRecord?

    /**
     * Batch priority lookup for the visible page (Phase 9) — one query,
     * never N+1.
     */
    @Query("SELECT * FROM priorities WHERE messageId IN (:messageIds)")
    suspend fun getByMessages(messageIds: List<String>): List<PriorityRecord>

    @Query(
        "SELECT * FROM priorities WHERE accountId = :accountId AND priority = :priority " +
            "LIMIT :limit",
    )
    fun observeByPriority(
        accountId: String,
        priority: Priority,
        limit: Int,
    ): Flow<List<PriorityRecord>>
}
