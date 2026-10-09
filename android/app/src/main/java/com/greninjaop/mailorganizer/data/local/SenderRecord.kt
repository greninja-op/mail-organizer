package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Normalized sender row (Phase 2 — storage foundation only).
 *
 * The sender-intelligence *engine* (normalization rules, known/unknown
 * heuristics) lands in Phase 8; this table stores its future output.
 * One row per (account, normalized email): the same address in two
 * accounts is intentionally two rows (account isolation).
 */
@Entity(
    tableName = "senders",
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
        Index(value = ["accountId", "normalizedEmail"], unique = true),
    ],
)
data class SenderRecord(
    @PrimaryKey val senderId: String,
    val accountId: String,
    val emailAddress: String,
    /** Lower-cased, trimmed address used for grouping. */
    val normalizedEmail: String,
    val displayName: String?,
    /** Domain part of [normalizedEmail]; denormalized for indexed lookup. */
    val domain: String,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long,
    val messageCount: Int = 0,
)
