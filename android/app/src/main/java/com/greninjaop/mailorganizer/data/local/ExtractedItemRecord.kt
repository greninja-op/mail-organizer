package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Structured-extraction item row (Phase 2 — storage foundation only).
 *
 * The extraction engine lands in Phase 13. Queryable attributes (type, due
 * date, completion) are typed columns; [payload] holds the remaining
 * item-specific fields as a small JSON object. Per the phase contract §25,
 * the JSON payload is NOT the only storage mechanism — anything we filter
 * or sort by lives in a real column.
 */
@Entity(
    tableName = "extracted_items",
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
        Index("itemType"),
        Index("dueDateEpochMs"),
        Index(value = ["accountId", "itemType"]),
    ],
)
data class ExtractedItemRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val accountId: String,
    val itemType: ExtractedItemType,
    val title: String,
    /** Small item-specific JSON (attendees, amounts, …); never the query key. */
    val payload: String?,
    val dueDateEpochMs: Long?,
    val detectedAtEpochMs: Long,
    val completed: Boolean = false,
)
