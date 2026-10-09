package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.email.EmailMessage
import com.greninjaop.mailorganizer.data.local.MessageRecord

/**
 * Parsed-email → database mapping (Phase 5).
 *
 * Completes the parse pipeline:
 * ```
 * RawGmailMessage → EmailParser → EmailMessage → toMessageRecord → DB
 * ```
 * (Phase 3's real Gmail client will feed raw payloads into this pipeline;
 * Phase 4's [RemoteMessage]-based path is untouched — both converge on the
 * same stable, namespaced ids from [SyncMappers].)
 *
 * Rules:
 * - Identity: reuses [localMessageId]/[localThreadId] so a parsed message
 *   upserts idempotently against rows written by the sync engine.
 * - Gmail `UNREAD`/`STARRED` label ids are remote state and map to the
 *   record's `unread`/`starred` flags — exactly like [RemoteMessage].
 *   They must not be confused with MO's own priority/category system.
 * - `bodyHtml` is sanitized HTML from the parser (never raw wire HTML);
 *   `attachments` is metadata only (binaries never stored).
 */
fun EmailMessage.toMessageRecord(accountId: AccountId): MessageRecord =
    MessageRecord(
        messageId = localMessageId(accountId, gmailId),
        gmailMessageId = gmailId,
        threadId = localThreadId(accountId, gmailThreadId),
        accountId = accountId.value,
        fromAddress = from?.address.orEmpty(),
        fromName = from?.name,
        toAddresses = to.map { it.address },
        ccAddresses = cc.map { it.address },
        subject = subject,
        snippet = snippet,
        bodyText = bodyText,
        bodyHtml = bodyHtml,
        attachments = attachments,
        timestampEpochMs = dateEpochMs,
        unread = "UNREAD" in labelIds,
        starred = "STARRED" in labelIds,
        labels = labelIds,
        sizeBytes = sizeBytes,
    )
