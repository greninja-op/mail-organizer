package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Action-required item row (Phase 2 — storage only).
 *
 * The action engine lands in Phase 14. Unlike classifications, a message may
 * carry *multiple* open action items, hence no unique index on messageId.
 * Completion/dismissal are explicit user-intent states, never inferred.
 */
@Entity(
    tableName = "action_items",
    foreignKeys = [
        ForeignKey(
            entity = MessageRecord::class,
            parentColumns = ["messageId"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("messageId"),
        Index("accountId"),
        Index("dueDateEpochMs"),
    ],
)
data class ActionItemRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val accountId: String,
    val actionType: ActionType,
    val confidence: Float,
    val explanation: String?,
    val dueDateEpochMs: Long?,
    val detectedAtEpochMs: Long,
    val completed: Boolean = false,
    val dismissed: Boolean = false,
)
