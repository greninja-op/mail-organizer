package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Priority output row (Phase 2 — storage only).
 *
 * Priority is independent from category (requirements.md). One current row
 * per message. [manualOverride] marks user intent, which outranks any
 * computed value in later phases.
 */
@Entity(
    tableName = "priorities",
    foreignKeys = [
        ForeignKey(
            entity = MessageRecord::class,
            parentColumns = ["messageId"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["messageId"], unique = true),
        Index("accountId"),
        Index("priority"),
        Index(value = ["accountId", "priority"]),
    ],
)
data class PriorityRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    /** Denormalized for account-scoped queries without joining messages. */
    val accountId: String,
    val priority: Priority,
    val manualOverride: Boolean = false,
    val reason: String?,
    val version: Int,
    val updatedAtEpochMs: Long,
)
