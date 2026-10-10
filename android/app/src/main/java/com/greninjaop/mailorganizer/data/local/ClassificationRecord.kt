package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Deterministic-classification output row (Phase 2 — storage only).
 *
 * The classifier itself lands in Phase 7. One *current* row per message
 * (unique index on messageId); history/provenance beyond "overridden" is a
 * later-phase concern. [source] records the precedence chain
 * (user correction > user rule > deterministic > optional AI).
 */
@Entity(
    tableName = "classifications",
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
        Index("category"),
        Index(value = ["accountId", "category"]),
    ],
)
data class ClassificationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    /** Denormalized for account-scoped queries without joining messages. */
    val accountId: String,
    val category: MailCategory,
    val confidence: Float,
    val source: ClassificationSource,
    /** Classifier version that produced this row (for future re-runs). */
    val version: Int,
    val explanation: String?,
    val overridden: Boolean = false,
    val classifiedAtEpochMs: Long,
)
