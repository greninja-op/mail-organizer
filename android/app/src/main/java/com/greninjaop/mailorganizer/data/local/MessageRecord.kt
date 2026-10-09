package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.greninjaop.mailorganizer.core.email.AttachmentMeta

/**
 * Email message row (Phase 2).
 *
 * Data-minimization notes (phase §38):
 * - Only a minimal plain-text body is stored, plus the *sanitized* HTML body
 *   (Phase 5); full MIME payloads and attachment binaries are NOT stored
 *   (attachment *metadata* arrives with the Phase 5 parser).
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
    /**
     * Sanitized HTML body (Phase 5, [HtmlSanitizer]); null when the message
     * has no HTML part. Never raw wire HTML — email is untrusted input.
     */
    val bodyHtml: String? = null,
    /**
     * Attachment *metadata* (Phase 5); binaries are never stored.
     * Persisted as JSON via [MoConverters] (never a query predicate).
     */
    val attachments: List<AttachmentMeta> = emptyList(),
    val timestampEpochMs: Long,
    val unread: Boolean = true,
    val starred: Boolean = false,
    val labels: List<String> = emptyList(),
    val sizeBytes: Long? = null,
    /**
     * Detected company id (Phase 8, [CompanyDetector]); null when the
     * sender is not attributable to a company (personal mailbox) or the
     * message hasn't been through company intelligence yet. Set by
     * [com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase],
     * never by sync or classification directly.
     */
    val companyId: String? = null,
)
