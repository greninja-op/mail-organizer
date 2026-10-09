package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord

/**
 * Mailbox destinations (Phase 6, design.md §Navigation drawer).
 *
 * The drawer is fixed to these six destinations. Classification-backed
 * destinations (PROMOTIONAL / SOCIAL / SPAM) show honest empty states until
 * the Phase 7 classification engine populates them — no fake categorization
 * (phase §59).
 */
enum class MailboxDestination {
    ALL_INBOX,
    PRIMARY,
    PROMOTIONAL,
    SOCIAL,
    SPAM,
    STARRED,
    ;

    val title: String
        get() = when (this) {
            ALL_INBOX -> "All Inbox"
            PRIMARY -> "Primary"
            PROMOTIONAL -> "Promotional"
            SOCIAL -> "Social"
            SPAM -> "Spam"
            STARRED -> "Starred"
        }

    /**
     * True for destinations that need the classification engine (Phase 7).
     * They render an honest empty state, never fabricated content.
     */
    val needsClassification: Boolean
        get() = this == PROMOTIONAL || this == SOCIAL || this == SPAM
}

/** Kinds of honest empty states (phase §12: never fabricate sample emails). */
enum class EmptyKind {
    /** No synchronized mail at all. */
    NO_MAIL,

    /** Text filter matched nothing. */
    NO_FILTER_RESULTS,

    /** Destination needs Phase 7 classification. */
    NOT_CLASSIFIED_YET,

    /** Starred destination with no starred messages. */
    NO_STARRED,
}

/** Explicit mailbox content state (phase §35: sealed, no scattered booleans). */
sealed interface MailboxContent {
    data object Loading : MailboxContent
    data class Threads(
        val items: List<ThreadItem>,
        /** True when exactly [limit] rows loaded — more may exist. */
        val hasMore: Boolean,
    ) : MailboxContent

    /** Starred destination is message-centric (star is per-message). */
    data class Messages(val items: List<MessageItem>) : MailboxContent
    data class Empty(val kind: EmptyKind) : MailboxContent

    /** User-facing message only — never stack traces/SQL/class names (§14). */
    data class Error(val message: String) : MailboxContent
}

/** Row model for the thread list. */
data class ThreadItem(
    val threadId: String,
    val accountId: String,
    val senderDisplay: String,
    val subject: String,
    val snippet: String,
    val timestampEpochMs: Long,
    val unread: Boolean,
    /** True when any message in the thread is starred (display only, §6). */
    val anyStarred: Boolean,
    val hasAttachment: Boolean,
    val messageCount: Int,
)

/** Full message model for thread/detail views. */
data class MessageItem(
    val messageId: String,
    val threadId: String,
    val accountId: String,
    val fromName: String?,
    val fromAddress: String,
    val toAddresses: List<String>,
    val ccAddresses: List<String>,
    val subject: String,
    val snippet: String,
    val bodyText: String?,
    /** Sanitized HTML (Phase 5) — safe to render, never raw wire HTML. */
    val bodyHtml: String?,
    /** Metadata only; binaries are never downloaded automatically (§27). */
    val attachments: List<AttachmentMeta>,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val starred: Boolean,
) {
    /** Sanitized HTML takes precedence; plain text fallback; never an error (§29). */
    val hasRenderableBody: Boolean
        get() = !bodyHtml.isNullOrBlank() || !bodyText.isNullOrBlank()
}

/**
 * Maps a [ThreadRecord] plus its latest [MessageRecord] to a [ThreadItem].
 * The latest message is fetched with one bounded query for the visible page
 * (never N+1 per row) — see MailViewModel.
 */
fun ThreadRecord.toThreadItem(latest: MessageRecord?): ThreadItem {
    val sender = participantDisplayNames.firstOrNull()
        ?: latest?.fromName?.takeIf { it.isNotBlank() }
        ?: latest?.fromAddress
        ?: "Unknown sender"
    return ThreadItem(
        threadId = threadId,
        accountId = accountId,
        senderDisplay = sender,
        subject = MailFormatting.subjectDisplay(subject),
        snippet = latest?.snippet?.trim().orEmpty(),
        timestampEpochMs = latestMessageEpochMs,
        unread = unreadCount > 0,
        anyStarred = latest?.starred == true,
        hasAttachment = (latest?.attachments?.size ?: 0) > 0,
        messageCount = messageCount,
    )
}

fun MessageRecord.toMessageItem(): MessageItem = MessageItem(
    messageId = messageId,
    threadId = threadId,
    accountId = accountId,
    fromName = fromName?.takeIf { it.isNotBlank() },
    fromAddress = fromAddress,
    toAddresses = toAddresses,
    ccAddresses = ccAddresses,
    subject = subject,
    snippet = snippet.orEmpty(),
    bodyText = bodyText,
    bodyHtml = bodyHtml,
    attachments = attachments,
    timestampEpochMs = timestampEpochMs,
    unread = unread,
    starred = starred,
)

/** Thread ordering (phase §51): oldest message first, newest last — documented. */
fun List<MessageItem>.inThreadOrder(): List<MessageItem> =
    sortedBy { it.timestampEpochMs }
