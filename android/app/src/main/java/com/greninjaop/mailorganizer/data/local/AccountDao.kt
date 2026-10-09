package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Account persistence (Phase 2 extends the Phase 0 seed).
 *
 * Deleting an account cascades through every account-owned table via
 * foreign keys — the data layer enforces isolation, not just UI filtering.
 */
@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountRecord)

    @Query("SELECT * FROM accounts WHERE accountId = :accountId")
    suspend fun getById(accountId: String): AccountRecord?

    @Query("SELECT * FROM accounts ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<AccountRecord>>

    @Query("SELECT * FROM accounts WHERE isEnabled = 1 ORDER BY createdAtEpochMs ASC")
    fun observeEnabled(): Flow<List<AccountRecord>>

    @Query(
        "UPDATE accounts SET connectionState = :state, updatedAtEpochMs = :updatedAt " +
            "WHERE accountId = :accountId",
    )
    suspend fun updateConnectionState(
        accountId: String,
        state: ConnectionState,
        updatedAt: Long,
    )

    @Query(
        "UPDATE accounts SET lastSyncEpochMs = :syncEpochMs, updatedAtEpochMs = :updatedAt " +
            "WHERE accountId = :accountId",
    )
    suspend fun updateLastSync(accountId: String, syncEpochMs: Long, updatedAt: Long)

    @Query("UPDATE accounts SET isEnabled = :enabled WHERE accountId = :accountId")
    suspend fun setEnabled(accountId: String, enabled: Boolean)

    @Query("DELETE FROM accounts WHERE accountId = :accountId")
    suspend fun deleteById(accountId: String)
}
