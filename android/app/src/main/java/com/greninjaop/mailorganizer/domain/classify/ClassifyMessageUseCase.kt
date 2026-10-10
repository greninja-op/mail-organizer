package com.greninjaop.mailorganizer.domain.classify

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.classify.ClassificationInput
import com.greninjaop.mailorganizer.core.classify.ClassificationResult
import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.core.classify.DeterministicClassifier
import com.greninjaop.mailorganizer.core.company.SenderIntelligence
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.withContext

/**
 * Classifies one message and persists the result (Phase 7).
 *
 * Architecture: UI → ViewModel → this use case → [DeterministicClassifier]
 * → [IntelligenceRepository]. Classification logic never lives in the UI
 * (phase §7); the engine stays independently testable (phase §6).
 *
 * Override safety (phase §30–31):
 * - Rows sourced from USER_CORRECTION or USER_RULE are never touched — user
 *   intent outranks automation, and the original classifier output is
 *   preserved underneath (the row is simply not overwritten).
 * - DETERMINISTIC rows stamped with the current [DeterministicClassifier.VERSION]
 *   are skipped unless [force] — re-running is idempotent (phase §34).
 * - Rows with an older version are reclassified (phase §33) unless they
 *   carry a user-sourced override.
 *
 * Failure safety (phase §56): any failure degrades to "not classified",
 * never crashes sync or the UI. Only safe diagnostics are logged
 * (rule id, version, message id — never email content, phase §57).
 */
class ClassifyMessageUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    /**
     * Real recurring-sender signal (Phase 8). Null keeps Phase 7 behavior
     * (signal hard-coded false) — used by tests and callers without
     * sender history.
     */
    private val recurringSenderProvider: RecurringSenderProvider? = null,
    /**
     * Optional AI fallback architecture (Phase 26).
     * AI is only queried if deterministic classification yields UNCLASSIFIED.
     */
    private val aiFallback: com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "ClassifyMessage"
    }

    /**
     * Classifies [messageId] and stores the result.
     *
     * @return the classifier result, or null when classification was skipped
     *   (message missing, user override present, or already current).
     */
    suspend fun classify(
        messageId: String,
        accountId: String,
        force: Boolean = false,
    ): ClassificationResult? = withContext(dispatchers.io) {
        try {
            classifyOrThrow(messageId, accountId, force)
        } catch (t: Throwable) {
            // Never break sync/UI because one email is odd (phase §56).
            // Log only the failure class — no email content (phase §57).
            MoLogger.e(TAG, "Classification failed safely: ${t.javaClass.simpleName}")
            null
        }
    }

    private suspend fun classifyOrThrow(
        messageId: String,
        accountId: String,
        force: Boolean,
    ): ClassificationResult? {
        val existing = intelligence.getClassification(messageId)
        if (!force && existing != null) {
            when (existing.source) {
                // User intent is authoritative — never overwrite (phase §30).
                ClassificationSource.USER_CORRECTION,
                ClassificationSource.USER_RULE,
                ClassificationSource.OPTIONAL_AI,
                -> return null
                ClassificationSource.DETERMINISTIC -> {
                    // Idempotent: same version already classified (phase §34).
                    if (existing.version >= DeterministicClassifier.VERSION) return null
                }
                ClassificationSource.UNKNOWN -> Unit // fall through: reclassify
            }
        }

        val message = mail.getMessage(messageId) ?: return null
        // Account isolation: never classify another account's message
        // (phase §36).
        if (message.accountId != accountId) return null

        // Phase 8: the recurring-sender signal is real now — looked up
        // from local sender frequency, never fabricated. Without a
        // provider the signal stays false (Phase 7 behavior).
        val normalizedEmail = SenderIntelligence.normalizeEmail(message.fromAddress)
        val isRecurringSender = recurringSenderProvider
            ?.isRecurring(accountId, normalizedEmail)
            ?: false

        val result = DeterministicClassifier.classify(
            input = message.toClassificationInput(),
            isRecurringSender = isRecurringSender,
            clock = clock,
        )

        // Phase 26: Optional AI Fallback Architecture.
        // Deterministic classification is authoritative. AI fallback is only attempted
        // when deterministic classification yields UNCLASSIFIED.
        val recordToSave = if (result.category == ClassifierCategory.UNCLASSIFIED && aiFallback != null) {
            val aiRecord = aiFallback.classifyWithFallback(message, MailCategory.UNCLASSIFIED)
            aiRecord ?: result.toRecord(messageId, accountId)
        } else {
            result.toRecord(messageId, accountId)
        }

        intelligence.setClassification(recordToSave)
        return result
    }

    private fun MessageRecord.toClassificationInput(): ClassificationInput =
        ClassificationInput(
            messageId = messageId,
            fromAddress = fromAddress,
            fromName = fromName,
            subject = subject,
            bodyText = bodyText,
            labelIds = labels,
            attachmentFilenames = attachments.mapNotNull { it.filename },
            attachmentMimeTypes = attachments.map { it.mimeType },
            snippet = snippet ?: "",
        )

    private fun ClassificationResult.toRecord(
        messageId: String,
        accountId: String,
    ): ClassificationRecord = ClassificationRecord(
        messageId = messageId,
        accountId = accountId,
        category = category.toMailCategory(),
        confidence = confidence.toFloat(),
        source = ClassificationSource.DETERMINISTIC,
        version = classifierVersion,
        explanation = explanation,
        overridden = false,
        classifiedAtEpochMs = classifiedAtEpochMs,
    )
}

/**
 * Persistence mapping for [ClassifierCategory].
 *
 * Lives in domain (not core) so `core.classify` keeps zero dependencies on
 * the data layer — the same boundary pattern as Phase 5's
 * EmailMessage → MessageRecord mappers.
 */
fun ClassifierCategory.toMailCategory(): MailCategory = when (this) {
    ClassifierCategory.ACTION_REQUIRED -> MailCategory.ACTION_REQUIRED
    ClassifierCategory.IMPORTANT -> MailCategory.IMPORTANT
    ClassifierCategory.CAREER -> MailCategory.CAREER
    ClassifierCategory.EDUCATION -> MailCategory.EDUCATION
    ClassifierCategory.RECEIPTS_ORDERS -> MailCategory.RECEIPTS_ORDERS
    ClassifierCategory.SECURITY -> MailCategory.SECURITY
    ClassifierCategory.NOTIFICATIONS -> MailCategory.NOTIFICATIONS
    ClassifierCategory.NEWSLETTERS -> MailCategory.NEWSLETTERS
    ClassifierCategory.PROMOTIONS -> MailCategory.PROMOTIONS
    ClassifierCategory.LOW_VALUE -> MailCategory.LOW_VALUE
    ClassifierCategory.UNCLASSIFIED -> MailCategory.UNCLASSIFIED
}

/** Deterministic confidence → stored 0..1 float (documented, not statistics). */
fun Confidence.toFloat(): Float = when (this) {
    Confidence.HIGH -> 0.9f
    Confidence.MEDIUM -> 0.6f
    Confidence.LOW -> 0.3f
}
