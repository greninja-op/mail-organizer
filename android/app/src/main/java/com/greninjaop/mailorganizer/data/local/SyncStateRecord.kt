package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Per-account sync state (Phase 2 — foundation only).
 *
 * The sync engine lands in Phase 4. [cursor] is intentionally opaque: Phase 4
 * will define what it holds (e.g. Gmail history tokens) once the Gmail API
 * implementation is understood — no invented token formats in Phase 2.
 * One row per account; the accountId PK doubles as the FK with cascade.
 */
@Entity(
    tableName = "sync_state",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SyncStateRecord(
    @PrimaryKey val accountId: String,
    val status: SyncStatus = SyncStatus.NEVER_SYNCED,
    val lastSuccessfulSyncEpochMs: Long? = null,
    val lastAttemptEpochMs: Long? = null,
    /** Opaque resume cursor; semantics defined by the Phase 4 sync engine. */
    val cursor: String? = null,
    val errorCode: String? = null,
    val retryCount: Int = 0,
    val syncVersion: Int = 1,
)
