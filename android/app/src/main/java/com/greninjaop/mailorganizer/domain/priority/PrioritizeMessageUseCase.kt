package com.greninjaop.mailorganizer.domain.priority

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.core.company.SenderIntelligence
import com.greninjaop.mailorganizer.core.priority.DeterministicPriorityEngine
import com.greninjaop.mailorganizer.core.priority.PriorityInput
import com.greninjaop.mailorganizer.core.priority.PriorityLevel
import com.greninjaop.mailorganizer.core.priority.PriorityResult
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.classify.RecurringSenderProvider
import kotlinx.coroutines.withContext

/**
 * Computes one message's priority and persists it (Phase 9).
 *
 * Architecture: UI → ViewModel → this use case → [DeterministicPriorityEngine]
 * → [IntelligenceRepository]. Priority logic never lives in the UI; the
 * engine stays independently testable.
 *
 * Priority is INDEPENDENT from category (requirements.md): the engine
 * consumes the classification as one signal among several, never re-derives
 * it.
 *
 * Override safety:
 * - Rows with [PriorityRecord.manualOverride] are never touched — user
 *   intent outranks automation.
 * - Rows stamped with the current [DeterministicPriorityEngine.VERSION] are
 *   skipped unless [force] — re-running is idempotent.
 * - Rows with an older version are reprioritized unless manually overridden.
 *
 * Failure safety: any failure degrades to "not prioritized", never crashes
 * sync or the UI. Only safe diagnostics are logged (failure class and
 * message id — never email content).
 */
class PrioritizeMessageUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    /**
     * Real recurring-sender signal (Phase 8). Null keeps the signal false —
     * used by tests and callers without sender history.
     */
    private val recurringSenderProvider: RecurringSenderProvider? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "PrioritizeMessage"

        /**
         * Bounded scan for the unsubscribe heuristic — the marker almost
         * always appears in the snippet or early body; scanning the whole
         * body of a huge email is wasteful.
         */
        private const val UNSUBSCRIBE_SCAN_CHARS = 5_000

        /** Documented heuristic fragments — never a verdict on their own. */
        private val BULK_LOCAL_PARTS = listOf(
            "noreply", "no-reply", "donotreply", "do-not-reply",
            "bounce", "mailer-daemon", "postmaster",
        )
    }

    /**
     * Prioritizes [messageId] and stores the result.
     *
     * @return the priority result, or null when prioritization was skipped
     *   (message missing, manual override present, or already current).
     */
    suspend fun prioritize(
        messageId: String,
        accountId: String,
        force: Boolean = false,
    ): PriorityResult? = withContext(dispatchers.io) {
        try {
            prioritizeOrThrow(messageId, accountId, force)
        } catch (t: Throwable) {
            // Never break sync/UI because one email is odd.
            // Log only the failure class — no email content.
            MoLogger.e(TAG, "Prioritization failed safely: ${t.javaClass.simpleName}")
            null
        }
    }

    private suspend fun prioritizeOrThrow(
        messageId: String,
        accountId: String,
        force: Boolean,
    ): PriorityResult? {
        val existing = intelligence.getPriority(messageId)
        if (!force && existing != null) {
            // User intent is authoritative — never overwrite.
            if (existing.manualOverride) return null
            // Idempotent: same version already prioritized.
            if (existing.version >= DeterministicPriorityEngine.VERSION) return null
        }

        val message = mail.getMessage(messageId) ?: return null
        // Account isolation: never prioritize another account's message.
        if (message.accountId != accountId) return null

        val classification = intelligence.getClassification(messageId)
        val normalizedEmail = SenderIntelligence.normalizeEmail(message.fromAddress)
        val isRecurringSender = recurringSenderProvider
            ?.isRecurring(accountId, normalizedEmail)
            ?: false

        val input = PriorityInput(
            messageId = messageId,
            category = classification?.category?.toClassifierCategory(),
            confidence = classification?.confidence?.toConfidence(),
            isRecurringSender = isRecurringSender,
            unread = message.unread,
            labelIds = message.labels,
            hasUnsubscribeMarker = hasUnsubscribeMarker(message),
            isBulkSender = isBulkSender(message.fromAddress),
        )
        val result = DeterministicPriorityEngine.prioritize(input, clock)
        intelligence.setPriority(result.toRecord(messageId, accountId))
        return result
    }

    private fun PriorityResult.toRecord(
        messageId: String,
        accountId: String,
    ): PriorityRecord = PriorityRecord(
        messageId = messageId,
        accountId = accountId,
        priority = priority.toPriority(),
        manualOverride = false,
        reason = explanation,
        version = version,
        updatedAtEpochMs = computedAtEpochMs,
    )

    private fun hasUnsubscribeMarker(message: MessageRecord): Boolean {
        val haystack = buildString {
            append(message.snippet ?: "")
            append(' ')
            val body = message.bodyText
            if (body != null) append(body.take(UNSUBSCRIBE_SCAN_CHARS))
        }
        return haystack.contains("unsubscribe", ignoreCase = true)
    }

    private fun isBulkSender(fromAddress: String): Boolean {
        val localPart = fromAddress.substringBefore('@').lowercase()
        return BULK_LOCAL_PARTS.any { localPart.contains(it) }
    }
}

/**
 * Persistence mapping for [PriorityLevel].
 *
 * Lives in domain (not core) so `core.priority` keeps zero dependencies on
 * the data layer — the same boundary pattern as Phase 7's `toMailCategory`.
 */
fun PriorityLevel.toPriority(): Priority = when (this) {
    PriorityLevel.LOW -> Priority.LOW
    PriorityLevel.NORMAL -> Priority.NORMAL
    PriorityLevel.HIGH -> Priority.HIGH
    PriorityLevel.CRITICAL -> Priority.CRITICAL
}

/** Reverse mapping for the engine input (lossy only across UNCLASSIFIED). */
fun MailCategory.toClassifierCategory(): ClassifierCategory = when (this) {
    MailCategory.ACTION_REQUIRED -> ClassifierCategory.ACTION_REQUIRED
    MailCategory.IMPORTANT -> ClassifierCategory.IMPORTANT
    MailCategory.CAREER -> ClassifierCategory.CAREER
    MailCategory.EDUCATION -> ClassifierCategory.EDUCATION
    MailCategory.RECEIPTS_ORDERS -> ClassifierCategory.RECEIPTS_ORDERS
    MailCategory.SECURITY -> ClassifierCategory.SECURITY
    MailCategory.NOTIFICATIONS -> ClassifierCategory.NOTIFICATIONS
    MailCategory.NEWSLETTERS -> ClassifierCategory.NEWSLETTERS
    MailCategory.PROMOTIONS -> ClassifierCategory.PROMOTIONS
    MailCategory.LOW_VALUE -> ClassifierCategory.LOW_VALUE
    MailCategory.UNCLASSIFIED -> ClassifierCategory.UNCLASSIFIED
}

/**
 * Stored confidence float → engine confidence. Mirrors Phase 7's
 * `Confidence.toFloat()` (0.9/0.6/0.3) with thresholds, documented here.
 */
fun Float.toConfidence(): Confidence = when {
    this >= 0.9f -> Confidence.HIGH
    this >= 0.6f -> Confidence.MEDIUM
    else -> Confidence.LOW
}
