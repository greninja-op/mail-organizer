package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Email message row (Phase 2).
 *
 * Data-minimization notes (phase §38):
 * - Only a minimal plain-text body is stored; full MIME payloads and
 *   attachment binaries are NOT stored (attachment *metadata* is out of
 *   scope for Phase 2 and arrives with the sync engine).
 * - `bodyText` may be null when only headers/snippet have been synced.
 *
 * Query patterns supported by the indexes: per-account mailbox, per-thread
 * listing, sender lookup, recency ordering, unread filtering, and the
 * Gmail-id upsert path used by the future sync engine.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ThreadRecord::class,
            parentColumns = ["threadId"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("threadId"),
        Index("fromAddress"),
        Index("timestampEpochMs"),
        Index("unread"),
        Index(value = ["accountId", "gmailMessageId"], unique = true),
    ],
)
data class MessageRecord(
    /** Local primary key; stable across re-syncs. */
    @PrimaryKey val messageId: String,
    /** Gmail message id; null until the sync engine (Phase 4) assigns one. */
    val gmailMessageId: String?,
    val threadId: String,
    val accountId: String,
    val fromAddress: String,
    val fromName: String?,
    val toAddresses: List<String> = emptyList(),
    val ccAddresses: List<String> = emptyList(),
    val subject: String,
    val snippet: String?,
    /** Minimal plain-text body (nullable); sanitized HTML is a later phase. */
    val bodyText: String?,
    val timestampEpochMs: Long,
    val unread: Boolean = true,
    val starred: Boolean = false,
    val labels: List<String> = emptyList(),
    val sizeBytes: Long? = null,
)
