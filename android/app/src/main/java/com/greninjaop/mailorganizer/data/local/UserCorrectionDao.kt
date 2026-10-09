package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * User-correction persistence (Phase 2 — storage only).
 *
 * Corrections are authoritative: [upsertCorrection] enforces "latest wins"
 * per (account, scope, scopeKey, field) inside a transaction.
 */
@Dao
interface UserCorrectionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(correction: UserCorrectionRecord): Long

    @Query(
        "DELETE FROM user_corrections WHERE accountId = :accountId AND scope = :scope " +
            "AND scopeKey = :scopeKey AND field = :field",
    )
    suspend fun deleteExisting(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    )

    @Transaction
    suspend fun upsertCorrection(correction: UserCorrectionRecord) {
        deleteExisting(
            correction.accountId,
            correction.scope,
            correction.scopeKey,
            correction.field,
        )
        insert(correction)
    }

    @Query(
        "SELECT * FROM user_corrections WHERE accountId = :accountId AND scope = :scope " +
            "AND scopeKey = :scopeKey AND field = :field",
    )
    suspend fun get(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ): UserCorrectionRecord?

    /** All corrections for one account (Phase 12 batch lookup). */
    @Query("SELECT * FROM user_corrections WHERE accountId = :accountId")
    suspend fun getAllByAccount(accountId: String): List<UserCorrectionRecord>

    /** Deletes one correction (Phase 12 undo). No-op when absent. */
    @Query(
        "DELETE FROM user_corrections WHERE accountId = :accountId AND scope = :scope " +
            "AND scopeKey = :scopeKey AND field = :field",
    )
    suspend fun delete(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    )
}
