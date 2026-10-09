package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Action-item persistence (Phase 2 — storage only; engine is Phase 14). */
@Dao
interface ActionItemDao {

    @Insert
    suspend fun insert(item: ActionItemRecord): Long

    @Query("SELECT * FROM action_items WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: String): List<ActionItemRecord>

    @Query(
        "SELECT * FROM action_items WHERE accountId = :accountId " +
            "AND completed = 0 AND dismissed = 0 " +
            "ORDER BY dueDateEpochMs ASC LIMIT :limit",
    )
    fun observeOpenByAccount(accountId: String, limit: Int): Flow<List<ActionItemRecord>>

    @Query("UPDATE action_items SET completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("UPDATE action_items SET dismissed = 1 WHERE id = :id")
    suspend fun markDismissed(id: Long)

    @Query("DELETE FROM action_items WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
