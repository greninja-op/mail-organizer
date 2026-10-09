package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.ThreadRecord

/**
 * Mailbox destinations (Phase 6, design.md §Navigation drawer).
 *
 * The drawer is fixed to these six destinations. Phase 7 wires the
 * classification-backed destinations to real data:
 * - PROMOTIONAL → MailCategory.PROMOTIONS (deterministic classifier)
 * - SOCIAL → Gmail CATEGORY_SOCIAL label (Gmail's own social signal)
 * - SPAM → Gmail SPAM label (Gmail's own spam signal)
 * Nothing is fabricated — empty destinations show honest empty states.
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
}

/** Kinds of honest empty states (phase §12: never fabricate sample emails). */
enum class EmptyKind {
    /** No synchronized mail at all. */
    NO_MAIL,

    /** Text filter matched nothing. */
    NO_FILTER_RESULTS,

    /** Starred destination with no starred messages. */
    NO_STARRED,

    /** Promotional destination with no classified promotions. */
    NO_PROMOTIONS,

    /** Social destination with no social mail. */
    NO_SOCIAL,

    /** Spam destination with no spam. */
    NO_SPAM,

    /** Company filter selected but no mail from that company here. */
    NO_COMPANY_RESULTS,

    /** Action-required filter with no action-required mail. */
    NO_ACTION_REQUIRED,
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
    /**
     * Phase 9: priority of the latest message (null when not yet computed).
     * Priority is independent from category (requirements.md).
     */
    val priority: Priority? = null,
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
    /**
     * Phase 9: priority (null when not yet computed). Independent from
     * category (requirements.md).
     */
    val priority: Priority? = null,
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

/** One company entry in the filter row. */
data class CompanyFilterEntry(
    val companyId: String,
    val displayName: String,
    val messageCount: Int,
    val pinned: Boolean,
)

/** Filter-row state for the mailbox screen. */
data class CompanyFilterUiState(
    /** False when the destination doesn't support company filtering. */
    val visible: Boolean = false,
    val entries: List<CompanyFilterEntry> = emptyList(),
    val selectedCompanyId: String? = null,
)
