package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User correction row (Phase 2 — storage only).
 *
 * Corrections are authoritative user intent and outrank every automated
 * signal (precedence: correction > rule > deterministic > AI). The unique
 * index on (accountId, scope, scopeKey, field) makes "latest correction
 * wins" a single upsert.
 */
@Entity(
    tableName = "user_corrections",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("accountId"),
        Index(
            value = ["accountId", "scope", "scopeKey", "field"],
            unique = true,
        ),
    ],
)
data class UserCorrectionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val scope: CorrectionScope,
    /** Normalized email, domain, or messageId depending on [scope]. */
    val scopeKey: String,
    val field: CorrectionField,
    /** Enum name (e.g. "CAREER") or boolean string ("true"/"false"). */
    val value: String,
    val createdAtEpochMs: Long,
)
