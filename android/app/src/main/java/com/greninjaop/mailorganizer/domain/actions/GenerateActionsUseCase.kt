package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.actions.ActionCandidateGenerator
import com.greninjaop.mailorganizer.core.actions.ActionInput
import com.greninjaop.mailorganizer.core.actions.ActionSafety
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.SafetyVerdict
import com.greninjaop.mailorganizer.core.actions.TemporalRef
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.temporal.TemporalPayloadJson
import kotlinx.coroutines.withContext

/**
 * Generates action candidates for an account's mailbox (Phase 14).
 *
 * Architecture: background pipeline → this use case →
 * [ActionCandidateGenerator] (pure) → [ActionSafety] (pure) →
 * [IntelligenceRepository] (persist → `action_items`).
 *
 * Generation rules (phase §21–§23):
 * - Deterministic: same intelligence in → same candidates out. The
 *   deterministic dedup key (`messageId|actionType|targetKey`) makes
 *   re-runs idempotent — repeated syncs never create duplicate cards.
 * - Thread-level dedup: when sibling messages in one thread would produce
 *   the same (actionType, targetKey), only the first open one is kept.
 * - Effective intelligence only (phase §29): the generator consumes the
 *   *effective* category/priority/action-required after rules/corrections —
 *   a user correction of "no action required" suppresses candidates.
 * - Version-gated: rows stamped with an older generator version are
 *   replaced (delete + regenerate); current-version rows are skipped.
 *   Dismissed/completed/expired rows are user intent or history — never
 *   regenerated.
 * - Expiry: open items whose due date passed more than [EXPIRED_GRACE_MS]
 *   ago are marked EXPIRED (history is retained, never silently deleted).
 *
 * Failure safety: per-message failures are isolated; the batch never
 * aborts. Best-effort — generation must never break the inbox.
 */
class GenerateActionsUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "GenerateActions"
        const val DEFAULT_LIMIT = 200

        /**
         * Grace period after a due date before an open card expires
         * (phase §27). 24h keeps "yesterday's interview" from lingering
         * as urgent while tolerating clock skew.
         */
        const val EXPIRED_GRACE_MS = 24L * 60 * 60 * 1000

        /** Statuses that are history/intent — never regenerated or expired. */
        private val TERMINAL_STATUSES = setOf(
            ActionStatus.COMPLETED,
            ActionStatus.DISMISSED,
            ActionStatus.EXPIRED,
            ActionStatus.CANCELLED,
            ActionStatus.FAILED,
        )
    }

    /**
     * Generates action candidates for up to [limit] recent messages.
     *
     * @return how many new action cards were created.
     */
    suspend fun generateNew(
        accountId: String,
        limit: Int = DEFAULT_LIMIT,
    ): Int = withContext(dispatchers.io) {
        val ids = try {
            mail.getMessageIdsByAccount(accountId, limit.coerceIn(1, 1_000))
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Message lookup failed: ${t.javaClass.simpleName}")
            return@withContext 0
        }
        var created = 0
        for (messageId in ids) {
            try {
                if (generateForMessage(messageId, accountId)) created++
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Generation failed safely: ${t.javaClass.simpleName}")
            }
        }
        expireOverdue(accountId)
        if (created > 0) {
            MoLogger.i(TAG, "Generated $created action candidates (v${ActionCandidateGenerator.VERSION})")
        }
        created
    }

    /**
     * Generates candidates for one message.
     *
     * @return true if at least one new card was persisted.
     */
    suspend fun generateForMessage(messageId: String, accountId: String): Boolean {
        val message = mail.getMessage(messageId) ?: return false
        // Account isolation: never generate from another account's message.
        if (message.accountId != accountId) return false

        // Idempotency: current-version open rows mean "already generated".
        val existing = intelligence.getActionItemsByMessage(messageId)
        val open = existing.filter { it.status !in TERMINAL_STATUSES }
        if (open.isNotEmpty() &&
            open.all { payloadVersion(it) >= ActionCandidateGenerator.VERSION }
        ) {
            return false
        }
        // Replace stale-version open rows (user intent rows are terminal
        // and therefore never in `open`).
        val staleIds = open
            .filter { payloadVersion(it) < ActionCandidateGenerator.VERSION }
            .map { it.id }
        if (staleIds.isNotEmpty()) {
            intelligence.deleteActionItems(staleIds)
        }

        val classification = intelligence.getClassification(messageId)
        val priority = intelligence.getPriority(messageId)
        val temporal = intelligence.getExtractedItems(messageId)
            .filter { !it.completed }
            .mapNotNull { toTemporalRef(it) }

        val category = classification?.category
        val input = ActionInput(
            messageId = messageId,
            threadId = message.threadId,
            accountId = accountId,
            subject = message.subject,
            senderName = message.fromName,
            senderAddress = message.fromAddress,
            timestampEpochMs = message.timestampEpochMs,
            unread = message.unread,
            category = category,
            categorySourceName = classification?.source?.name,
            priority = priority?.priority,
            actionRequired = category == MailCategory.ACTION_REQUIRED,
            temporalItems = temporal,
        )

        val now = clock()
        var created = false
        for (candidate in ActionCandidateGenerator.generate(input, now)) {
            when (val verdict = ActionSafety.validate(candidate)) {
                is SafetyVerdict.Blocked -> {
                    MoLogger.e(TAG, "Candidate blocked: ${verdict.reason}")
                }
                is SafetyVerdict.Safe -> {
                    if (persistIfNew(verdict.candidate, degraded = null)) created = true
                }
                is SafetyVerdict.Degraded -> {
                    if (persistIfNew(verdict.candidate, verdict)) created = true
                }
            }
        }
        return created
    }

    /**
     * Persists [candidate] unless an open card for the same underlying
     * action already exists — either on this message (same dedup id) or on
     * a sibling message in the thread (phase §22–§23).
     */
    private suspend fun persistIfNew(
        candidate: com.greninjaop.mailorganizer.core.actions.ActionCandidate,
        degraded: SafetyVerdict.Degraded?,
    ): Boolean {
        val siblings = intelligence.getOpenActionItemsByThread(candidate.threadId)
        val duplicate = siblings.any { row ->
            row.accountId == candidate.accountId &&
                row.actionType == candidate.actionType &&
                ActionPayloadJson.decode(row.payloadJson)?.targetKey == candidate.targetKey
        }
        if (duplicate) return false

        val title = degraded?.degradedTitle ?: candidate.title
        val description = degraded?.degradedDescription ?: candidate.description
        intelligence.addActionItem(
            ActionItemRecord(
                messageId = candidate.messageId,
                accountId = candidate.accountId,
                threadId = candidate.threadId,
                actionType = candidate.actionType,
                title = title,
                description = description,
                urgency = candidate.urgency,
                source = candidate.source,
                status = ActionStatus.SUGGESTED,
                confidence = when (candidate.confidence) {
                    com.greninjaop.mailorganizer.core.actions.ActionConfidence.HIGH -> 1.0f
                    com.greninjaop.mailorganizer.core.actions.ActionConfidence.MEDIUM -> 0.7f
                    com.greninjaop.mailorganizer.core.actions.ActionConfidence.LOW -> 0.4f
                    com.greninjaop.mailorganizer.core.actions.ActionConfidence.UNKNOWN -> 0.0f
                },
                explanation = candidate.explanation,
                externalEffect = candidate.externalEffect,
                payloadJson = ActionPayloadJson.encode(
                    version = candidate.version,
                    signals = candidate.signals,
                    missingInfo = candidate.missingInfo,
                    targetKey = candidate.targetKey,
                ),
                dueDateEpochMs = candidate.dueDateEpochMs,
                detectedAtEpochMs = clock(),
                updatedAtEpochMs = clock(),
                version = candidate.version,
                completed = false,
                dismissed = false,
            ),
        )
        return true
    }

    /** Marks open, long-past-due cards EXPIRED (history retained). */
    private suspend fun expireOverdue(accountId: String) {
        try {
            val cutoff = clock() - EXPIRED_GRACE_MS
            val n = intelligence.expireOverdueActionItems(accountId, cutoff)
            if (n > 0) MoLogger.i(TAG, "Expired $n overdue action cards")
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Expiry pass failed: ${t.javaClass.simpleName}")
        }
    }

    private fun payloadVersion(row: ActionItemRecord): Int =
        ActionPayloadJson.decode(row.payloadJson)?.version ?: 0

    private fun toTemporalRef(
        record: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord,
    ): TemporalRef? {
        val decoded = TemporalPayloadJson.decode(record.payload) ?: return null
        return TemporalRef(
            itemId = record.id,
            itemTypeName = record.itemType.name,
            title = record.title,
            startEpochMs = record.dueDateEpochMs ?: return null,
            endEpochMs = decoded.endEpochMs,
            isDateOnly = decoded.isDateOnly,
            timezoneId = decoded.timezoneId,
            location = decoded.location,
            meetingUrl = decoded.meetingUrl,
            statusName = decoded.status.name,
            confidenceName = decoded.confidence.name,
            signalNames = decoded.signalNames,
        )
    }
}
