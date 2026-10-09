package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Structured-extraction persistence (Phase 2 — storage only; engine is Phase 13). */
@Dao
interface ExtractedItemDao {

    @Insert
    suspend fun insert(item: ExtractedItemRecord): Long

    @Query("SELECT * FROM extracted_items WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: String): List<ExtractedItemRecord>

    @Query(
        "SELECT * FROM extracted_items WHERE accountId = :accountId " +
            "AND itemType = :type AND completed = 0 " +
            "ORDER BY dueDateEpochMs ASC LIMIT :limit",
    )
    fun observeOpenByType(
        accountId: String,
        type: ExtractedItemType,
        limit: Int,
    ): Flow<List<ExtractedItemRecord>>

    @Query("UPDATE extracted_items SET completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM extracted_items WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
