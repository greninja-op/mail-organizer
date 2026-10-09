package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Sender persistence (Phase 2 — storage foundation; engine is Phase 8). */
@Dao
interface SenderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sender: SenderRecord)

    @Query(
        "SELECT * FROM senders WHERE accountId = :accountId AND normalizedEmail = :normalizedEmail",
    )
    suspend fun getByEmail(accountId: String, normalizedEmail: String): SenderRecord?

    @Query(
        "SELECT * FROM senders WHERE accountId = :accountId " +
            "ORDER BY messageCount DESC LIMIT :limit",
    )
    fun observeTopByAccount(accountId: String, limit: Int): Flow<List<SenderRecord>>

    @Query("DELETE FROM senders WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
