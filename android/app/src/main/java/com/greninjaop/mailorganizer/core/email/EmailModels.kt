package com.greninjaop.mailorganizer.core.email

/**
 * Email data model — the canonical parsed representation of a Gmail message (Phase 5).
 *
 * Pipeline position:
 * ```
 * Gmail API JSON (users.messages.get, format=FULL)
 *        ↓  (Phase 3: raw transport; wire shape mirrored by [RawGmailMessage])
 * [RawGmailMessage]
 *        ↓  (Phase 5: [EmailParser] — this layer)
 * [EmailMessage]            ← canonical domain model; UI/intelligence consume this
 *        ↓  (Phase 5: EmailMappers → Phase 4 [RemoteMessage] → [SyncMappers])
 * MessageRecord             ← Room entity (Phase 2 schema, extended in Phase 5)
 * ```
 *
 * All types here are pure Kotlin (no Android imports) so this package can move
 * verbatim into a KMP shared module when the second platform arrives.
 *
 * Security: [EmailMessage.bodyHtml] is ALWAYS sanitized ([HtmlSanitizer]) —
 * raw HTML from the wire never reaches this model. Email is untrusted input.
 */

// ---------------------------------------------------------------------------
// Wire model — mirrors the Gmail API users.messages.get (format=FULL) shape.
// ---------------------------------------------------------------------------

/** One RFC 2822 header as returned by the Gmail API payload. */
data class RawGmailHeader(
    val name: String,
    val value: String,
)

/**
 * Body block of a MIME part.
 *
 * - For inline text parts, [data] holds base64url-encoded bytes.
 * - For attachments, [data] is absent and [attachmentId] identifies the
 *   attachment for a later users.messages.attachments.get call (Phase 6+).
 * - For multipart containers, both are null ([size] may still be set).
 */
data class RawGmailBody(
    val data: String?,
    val attachmentId: String?,
    val size: Long,
)

/** One node of the MIME part tree. */
data class RawGmailPart(
    val mimeType: String,
    val filename: String?,
    val headers: List<RawGmailHeader> = emptyList(),
    val body: RawGmailBody? = null,
    val parts: List<RawGmailPart> = emptyList(),
)

/**
 * Raw Gmail message as delivered by users.messages.get (format=FULL).
 *
 * This is a wire-format mirror, NOT an app model: field names follow the API
 * (e.g. base64url [RawGmailBody.data]) and values are untrusted until parsed.
 */
data class RawGmailMessage(
    val id: String,
    val threadId: String,
    val labelIds: List<String> = emptyList(),
    val snippet: String? = null,
    /** Gmail `internalDate` (ms since epoch); may be absent on malformed payloads. */
    val internalDateMs: Long? = null,
    val sizeEstimate: Long? = null,
    /** Null on malformed payloads — the parser degrades gracefully. */
    val payload: RawGmailPart? = null,
)

// ---------------------------------------------------------------------------
// Domain model — parsed, normalized, safe to consume.
// ---------------------------------------------------------------------------

/** A parsed RFC 5322 mailbox: optional display name + address. */
data class EmailAddress(
    /** Decoded display name (RFC 2047 decoded), or null when absent. */
    val name: String?,
    /** Raw address, e.g. `user@example.com`. Never blank in a parsed result. */
    val address: String,
)

/**
 * Attachment *metadata* (Phase 5).
 *
 * Binaries are NEVER part of this model and are never downloaded
 * automatically (Phase 6 renders indicators from this metadata only).
 * [attachmentId] is the Gmail attachment id for on-demand fetch later.
 */
data class AttachmentMeta(
    val filename: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val attachmentId: String?,
)

/**
 * Canonical parsed email (Phase 5).
 *
 * Produced by [EmailParser] from [RawGmailMessage]. Every field is
 * best-effort normalized; the parser is total — malformed input yields a
 * degraded model, never an exception.
 *
 * Account scoping is applied by the mapper layer ([EmailMessage] carries no
 * account id; callers namespace ids per account like the rest of the app).
 */
data class EmailMessage(
    /** Gmail message id (unscoped; callers namespace per account). */
    val gmailId: String,
    /** Gmail thread id (unscoped). */
    val gmailThreadId: String,
    val from: EmailAddress?,
    val to: List<EmailAddress> = emptyList(),
    val cc: List<EmailAddress> = emptyList(),
    /** Usually empty — Gmail does not return Bcc on received messages. */
    val bcc: List<EmailAddress> = emptyList(),
    val replyTo: List<EmailAddress> = emptyList(),
    /** RFC 2047-decoded subject; empty string when absent. */
    val subject: String = "",
    /**
     * Best-effort timestamp: `Date` header → Gmail `internalDate` → 0.
     * 0 means "unknown", never a fabricated date.
     */
    val dateEpochMs: Long = 0L,
    /** RFC `Message-ID` header value, if present (threading, Phase 21). */
    val messageIdHeader: String? = null,
    val inReplyTo: String? = null,
    val references: List<String> = emptyList(),
    /** Gmail snippet, or generated from [bodyText] when absent. */
    val snippet: String = "",
    /**
     * Plain-text body: preferred `text/plain` part, else HTML→text fallback.
     * Null when no usable body exists.
     */
    val bodyText: String? = null,
    /**
     * Sanitized HTML body ([HtmlSanitizer]), or null when the message has no
     * HTML part. Safe to render; never raw wire HTML.
     */
    val bodyHtml: String? = null,
    val attachments: List<AttachmentMeta> = emptyList(),
    /** Raw Gmail label ids (UNREAD, STARRED, CATEGORY_*, …). */
    val labelIds: List<String> = emptyList(),
    val sizeBytes: Long? = null,
)
