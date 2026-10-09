package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord

/**
 * Normalization boundary (Phase 4, phase §44).
 *
 * Remote Gmail models ([RemoteMessage]) are converted here — and only here —
 * into local database entities. The rest of the app never sees [RemoteMessage].
 *
 * Rules enforced by this mapper:
 * - Identity: local ids are stable and namespaced —
 *   `"<accountId>:<gmailId>"`. Re-syncing the same Gmail message always
 *   yields the same local primary key, which is what makes upserts
 *   idempotent (phase §16–18).
 * - Account scoping: every produced record carries the account id; a Gmail
 *   id is never trusted across account boundaries.
 * - Separation: only Gmail-derived fields are mapped. Mail Organizer-derived
 *   metadata (classifications, priorities, rules, corrections) lives in its
 *   own tables and is NEVER touched by sync (phase §19–20).
 * - Gmail `starred`/`unread`/`labels` are remote state and ARE synced;
 *   they must not be confused with MO's own priority/category system
 *   (phase §37–39).
 */
fun RemoteMessage.toMessageRecord(accountId: AccountId): MessageRecord =
    MessageRecord(
        messageId = localMessageId(accountId, id),
        gmailMessageId = id,
        threadId = localThreadId(accountId, threadId),
        accountId = accountId.value,
        fromAddress = fromAddress,
        fromName = fromName,
        toAddresses = toAddresses,
        ccAddresses = ccAddresses,
        subject = subject,
        snippet = snippet,
        bodyText = bodyText,
        timestampEpochMs = timestampEpochMs,
        unread = unread,
        starred = starred,
        labels = labels,
        sizeBytes = sizeBytes,
    )

/** Stable local primary key for a synced message (phase §16). */
fun localMessageId(accountId: AccountId, gmailMessageId: String): String =
    "${accountId.value}:$gmailMessageId"

/** Stable local primary key for a synced thread (phase §17). */
fun localThreadId(accountId: AccountId, gmailThreadId: String): String =
    "${accountId.value}:$gmailThreadId"

/**
 * Builds a [ThreadRecord] from one page's worth of messages of a single
 * Gmail thread. Aggregate counts are best-effort here; the coordinator
 * refreshes them from the database after persisting ([MailRepository]).
 */
fun buildThreadRecord(
    accountId: AccountId,
    gmailThreadId: String,
    messages: List<MessageRecord>,
    nowMs: Long,
): ThreadRecord {
    require(messages.isNotEmpty()) { "cannot build a thread with no messages" }
    val latest = messages.maxBy { it.timestampEpochMs }
    val participants = messages
        .map { it.fromName?.takeIf(String::isNotBlank) ?: it.fromAddress }
        .distinct()
    return ThreadRecord(
        threadId = localThreadId(accountId, gmailThreadId),
        gmailThreadId = gmailThreadId,
        accountId = accountId.value,
        subject = latest.subject,
        participantDisplayNames = participants,
        messageCount = messages.size,
        unreadCount = messages.count { it.unread },
        latestMessageId = latest.messageId,
        latestMessageEpochMs = latest.timestampEpochMs,
        updatedAtEpochMs = nowMs,
    )
}
