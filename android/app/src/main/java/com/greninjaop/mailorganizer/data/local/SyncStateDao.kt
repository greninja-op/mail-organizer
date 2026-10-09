package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Sync-state persistence (Phase 2 — foundation; engine is Phase 4). */
@Dao
interface SyncStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateRecord)

    @Query("SELECT * FROM sync_state WHERE accountId = :accountId")
    suspend fun getByAccount(accountId: String): SyncStateRecord?

    @Query("SELECT * FROM sync_state WHERE accountId = :accountId")
    fun observeByAccount(accountId: String): Flow<SyncStateRecord?>

    @Query(
        "UPDATE sync_state SET status = :status, lastAttemptEpochMs = :attemptEpochMs, " +
            "errorCode = :errorCode WHERE accountId = :accountId",
    )
    suspend fun updateAttempt(
        accountId: String,
        status: SyncStatus,
        attemptEpochMs: Long,
        errorCode: String?,
    )

    /**
     * Advances the opaque sync cursor WITHOUT touching status/timestamps
     * (Phase 4, phase §22: the cursor moves only after the corresponding
     * page is safely committed).
     */
    @Query("UPDATE sync_state SET cursor = :cursor WHERE accountId = :accountId")
    suspend fun updateCursor(accountId: String, cursor: String)
}
