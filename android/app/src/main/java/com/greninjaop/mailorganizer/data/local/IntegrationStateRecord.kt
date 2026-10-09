package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Persisted integration metadata (Phase 17, phase §34).
 *
 * One row per (integration, account): the last-known status snapshot the
 * Integration Manager computed. This is *metadata only* — status, reason,
 * timestamps, config version.
 *
 * Security boundary (phase §35): OAuth credentials are NEVER stored here
 * (or anywhere in this database). When Phase 3 lands, tokens live in
 * Android's secure credential storage; this table only records that an
 * integration is connected and when it was last checked.
 *
 * [accountId] is never null: the empty string means "no account in scope"
 * (Room forbids nullable primary keys; the sentinel is documented here,
 * not scattered across call sites).
 */
@Entity(
    tableName = "integration_states",
    primaryKeys = ["integrationId", "accountId"],
)
data class IntegrationStateRecord(
    val integrationId: String,
    val accountId: String,
    val lastStatus: String,
    val statusReason: String?,
    val updatedAtEpochMs: Long,
    val configVersion: Int,
)

/** DAO for [IntegrationStateRecord] (Phase 17). */
@Dao
interface IntegrationStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: IntegrationStateRecord)

    @Query("SELECT * FROM integration_states WHERE accountId = :accountId")
    suspend fun getForAccount(accountId: String): List<IntegrationStateRecord>

    /**
     * Account-removal cleanup (phase §36): deletes ONLY the removed
     * account's integration rows. Never touches other accounts or any
     * unrelated local data.
     */
    @Query("DELETE FROM integration_states WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)

    @Query("DELETE FROM integration_states WHERE integrationId = :integrationId AND accountId = :accountId")
    suspend fun delete(integrationId: String, accountId: String)
}
