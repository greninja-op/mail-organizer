package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Classification persistence (Phase 2 — storage only; classifier is Phase 7).
 *
 * One *current* row per message: [setClassification] replaces any previous
 * row inside a transaction, so readers never see a half-updated state.
 */
@Dao
interface ClassificationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: ClassificationRecord): Long

    @Query("DELETE FROM classifications WHERE messageId = :messageId")
    suspend fun deleteByMessage(messageId: String)

    @Transaction
    suspend fun setClassification(record: ClassificationRecord) {
        deleteByMessage(record.messageId)
        insert(record)
    }

    @Query("SELECT * FROM classifications WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: String): ClassificationRecord?

    @Query(
        "SELECT * FROM classifications WHERE accountId = :accountId AND category = :category " +
            "LIMIT :limit",
    )
    fun observeByCategory(
        accountId: String,
        category: MailCategory,
        limit: Int,
    ): Flow<List<ClassificationRecord>>
}
