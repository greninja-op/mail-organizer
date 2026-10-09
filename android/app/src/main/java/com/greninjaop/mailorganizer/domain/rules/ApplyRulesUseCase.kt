package com.greninjaop.mailorganizer.domain.rules

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.company.SenderIntelligence
import com.greninjaop.mailorganizer.core.rules.RuleEngine
import com.greninjaop.mailorganizer.core.rules.RuleMatchInput
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.CorrectionScope
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.UserCorrectionRecord
import com.greninjaop.mailorganizer.data.repository.CorrectionLookup
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMessageUseCase
import kotlinx.coroutines.withContext

/**
 * Computes and persists the *effective* intelligence result for messages
 * (Phase 12, phase §5–§7).
 *
 * The pipeline:
 * ```text
 * explicit user correction (narrowest scope first)
 *   → enabled user rule (ruleOrder, then id)
 *   → built-in deterministic intelligence
 *   → unknown
 * ```
 *
 * The deterministic engines are never modified: this use case ensures a
 * base result exists (via [ClassifyMessageUseCase]/[PrioritizeMessageUseCase],
 * which already skip user-sourced rows), then overlays user intent on top.
 * The stored row is the *effective* result — what the UI shows — with
 * [ClassificationRecord.source] / [PriorityRecord.manualOverride] recording
 * provenance and the human-readable explanation naming the correction or
 * rule (phase §15).
 *
 * Base results are never destroyed: the deterministic engines are pure
 * functions, so removing a correction/rule restores the base by
 * re-running them (phase §27). Corrections themselves live in
 * `user_corrections` and are only ever deleted by explicit user undo.
 *
 * Idempotent (phase §24): rows are written only when the effective value,
 * source, or engine version differs. Failure-safe: any failure degrades
 * to "no change", never crashes sync or the UI.
 */
class ApplyRulesUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val rules: RuleRepository,
    private val classify: ClassifyMessageUseCase,
    private val prioritize: PrioritizeMessageUseCase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "ApplyRules"

        /** Bound for one mailbox-wide refresh pass (phase §23). */
        const val DEFAULT_MAILBOX_LIMIT = 500
    }

    /**
     * Recomputes the effective category/priority for one message.
     *
     * @return true if any row was written.
     */
    suspend fun refreshMessage(messageId: String, accountId: String): Boolean =
        withContext(dispatchers.io) {
            try {
                refreshOrThrow(messageId, accountId)
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Rule application failed safely: ${t.javaClass.simpleName}")
                false
            }
        }

    /**
     * Recomputes effective results for up to [limit] account messages.
     * Controlled background reprocessing (phase §23) — never on the UI
     * thread, bounded, per-message failures isolated.
     *
     * @return how many messages had rows written.
     */
    suspend fun refreshMailbox(
        accountId: String,
        limit: Int = DEFAULT_MAILBOX_LIMIT,
    ): Int = withContext(dispatchers.io) {
        val ids = try {
            mail.getMessageIdsByAccount(accountId, limit.coerceIn(1, 5_000))
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Mailbox scan failed: ${t.javaClass.simpleName}")
            return@withContext 0
        }
        var written = 0
        for (id in ids) {
            if (refreshMessage(id, accountId)) written++
        }
        written
    }

    private suspend fun refreshOrThrow(messageId: String, accountId: String): Boolean {
        val message = mail.getMessage(messageId) ?: return false
        if (message.accountId != accountId) return false

        val overlay = computeOverlay(message, accountId)
        var written = false

        // ---- Category ----
        val currentCategory = intelligence.getClassification(messageId)
        if (overlay.category != null) {
            val (category, source, explanation) = overlay.category
            val needsWrite = currentCategory == null ||
                currentCategory.category != category ||
                currentCategory.source != source ||
                (source == ClassificationSource.USER_RULE &&
                    currentCategory.version < RuleEngine.VERSION)
            if (needsWrite) {
                intelligence.setClassification(
                    ClassificationRecord(
                        messageId = messageId,
                        accountId = accountId,
                        category = category,
                        // User intent is authoritative — not a probability.
                        confidence = 1.0f,
                        source = source,
                        version = RuleEngine.VERSION,
                        explanation = explanation,
                        overridden = true,
                        classifiedAtEpochMs = clock(),
                    ),
                )
                written = true
            }
        } else {
            // No user overlay: a stale user-sourced row must go so the
            // deterministic base can be restored (phase §27).
            if (currentCategory != null &&
                currentCategory.source != ClassificationSource.DETERMINISTIC
            ) {
                intelligence.deleteClassification(messageId)
                written = true
            }
            // Ensures a base row exists; skips when current (idempotent).
            classify.classify(messageId, accountId)
        }

        // ---- Priority ----
        val currentPriority = intelligence.getPriority(messageId)
        if (overlay.priority != null) {
            val (priority, source, explanation) = overlay.priority
            val needsWrite = currentPriority == null ||
                currentPriority.priority != priority ||
                !currentPriority.manualOverride ||
                (source == ClassificationSource.USER_RULE &&
                    currentPriority.version < RuleEngine.VERSION)
            if (needsWrite) {
                intelligence.setPriority(
                    PriorityRecord(
                        messageId = messageId,
                        accountId = accountId,
                        priority = priority,
                        manualOverride = true,
                        reason = explanation,
                        version = RuleEngine.VERSION,
                        updatedAtEpochMs = clock(),
                    ),
                )
                written = true
            }
        } else {
            if (currentPriority != null && currentPriority.manualOverride) {
                intelligence.deletePriority(messageId)
                written = true
            }
            prioritize.prioritize(messageId, accountId)
        }

        return written
    }

    /**
     * User overlay for one message: explicit correction first (narrowest
     * scope wins), then enabled rules in precedence order.
     */
    private suspend fun computeOverlay(
        message: MessageRecord,
        accountId: String,
    ): Overlay {
        val normalizedEmail = SenderIntelligence.normalizeEmail(message.fromAddress)
        val domain = SenderIntelligence.domainOf(normalizedEmail)

        val lookups = listOf(
            CorrectionLookup(accountId, CorrectionScope.MESSAGE, message.messageId, CorrectionField.CATEGORY),
            CorrectionLookup(accountId, CorrectionScope.SENDER, normalizedEmail, CorrectionField.CATEGORY),
            CorrectionLookup(accountId, CorrectionScope.DOMAIN, domain, CorrectionField.CATEGORY),
            CorrectionLookup(accountId, CorrectionScope.MESSAGE, message.messageId, CorrectionField.PRIORITY),
            CorrectionLookup(accountId, CorrectionScope.SENDER, normalizedEmail, CorrectionField.PRIORITY),
            CorrectionLookup(accountId, CorrectionScope.DOMAIN, domain, CorrectionField.PRIORITY),
        )
        val corrections = rules.getCorrections(accountId, lookups)

        val categoryCorrection = firstCorrection(
            corrections, accountId, CorrectionField.CATEGORY,
            listOf(
                CorrectionScope.MESSAGE to message.messageId,
                CorrectionScope.SENDER to normalizedEmail,
                CorrectionScope.DOMAIN to domain,
            ),
        )
        val priorityCorrection = firstCorrection(
            corrections, accountId, CorrectionField.PRIORITY,
            listOf(
                CorrectionScope.MESSAGE to message.messageId,
                CorrectionScope.SENDER to normalizedEmail,
                CorrectionScope.DOMAIN to domain,
            ),
        )

        val categoryOverlay = categoryCorrection?.let { (record, scopeLabel) ->
            val category = runCatching { MailCategory.valueOf(record.value) }.getOrNull()
            category?.let {
                Triple(
                    it,
                    ClassificationSource.USER_CORRECTION,
                    "You set this message's category to ${it.pretty()} ($scopeLabel).",
                )
            }
        }

        val priorityOverlay = priorityCorrection?.let { (record, scopeLabel) ->
            val priority = runCatching { Priority.valueOf(record.value) }.getOrNull()
            priority?.let {
                Triple(
                    it,
                    ClassificationSource.USER_CORRECTION,
                    "You set this message's priority to ${it.pretty()} ($scopeLabel).",
                )
            }
        }

        // Rules only where no correction claimed the field.
        val ruleOverlay = if (categoryOverlay == null || priorityOverlay == null) {
            val enabledRules = rules.getEnabledRules(accountId).map { it.toUserRule() }
            if (enabledRules.isNotEmpty()) {
                val baseCategory = intelligence.getClassification(message.messageId)
                    ?.takeIf { it.source == ClassificationSource.DETERMINISTIC }
                    ?.category?.name
                val basePriority = intelligence.getPriority(message.messageId)
                    ?.takeIf { !it.manualOverride }
                    ?.priority?.name
                val input = RuleMatchInput(
                    messageId = message.messageId,
                    accountId = accountId,
                    senderEmail = normalizedEmail,
                    senderDomain = domain,
                    companyId = message.companyId,
                    subject = message.subject,
                    labelIds = message.labels,
                    gmailCategory = message.labels.firstOrNull { it.startsWith("CATEGORY_") },
                    baseCategory = baseCategory,
                    basePriority = basePriority,
                    hasAttachment = message.attachments.isNotEmpty(),
                    hasUnsubscribeMarker = hasUnsubscribeMarker(message),
                )
                val evaluation = RuleEngine.evaluate(
                    input,
                    enabledRules.map { it.toEvaluable() },
                )
                evaluation
            } else {
                null
            }
        } else {
            null
        }

        val finalCategory = categoryOverlay ?: ruleOverlay?.let { eval ->
            eval.categoryRule?.let { rule ->
                val category = runCatching {
                    MailCategory.valueOf(eval.effectiveCategory!!)
                }.getOrNull()
                category?.let {
                    Triple(
                        it,
                        ClassificationSource.USER_RULE,
                        "Rule \"${rule.name}\" set this category.",
                    )
                }
            }
        }

        val finalPriority = priorityOverlay ?: ruleOverlay?.let { eval ->
            eval.priorityRule?.let { rule ->
                val priority = runCatching {
                    Priority.valueOf(eval.effectivePriority!!)
                }.getOrNull()
                priority?.let {
                    Triple(
                        it,
                        ClassificationSource.USER_RULE,
                        "Rule \"${rule.name}\" set this priority.",
                    )
                }
            }
        }

        return Overlay(category = finalCategory, priority = finalPriority)
    }

    private fun firstCorrection(
        corrections: Map<CorrectionLookup, UserCorrectionRecord>,
        accountId: String,
        field: CorrectionField,
        scopes: List<Pair<CorrectionScope, String>>,
    ): Pair<UserCorrectionRecord, String>? {
        for ((scope, key) in scopes) {
            val record = corrections[CorrectionLookup(accountId, scope, key, field)]
            if (record != null) {
                val label = when (scope) {
                    CorrectionScope.MESSAGE -> "this message"
                    CorrectionScope.SENDER -> "sender ${record.scopeKey}"
                    CorrectionScope.DOMAIN -> "domain ${record.scopeKey}"
                    CorrectionScope.COMPANY -> "company ${record.scopeKey}"
                }
                return record to label
            }
        }
        return null
    }

    private fun hasUnsubscribeMarker(message: MessageRecord): Boolean {
        val haystack = buildString {
            append(message.snippet ?: "")
            append(' ')
            message.bodyText?.let { append(it.take(5_000)) }
        }
        return haystack.contains("unsubscribe", ignoreCase = true)
    }

    private data class Overlay(
        val category: Triple<MailCategory, ClassificationSource, String>?,
        val priority: Triple<Priority, ClassificationSource, String>?,
    )

    private fun MailCategory.pretty(): String =
        name.lowercase().split('_').joinToString(" ") {
            it.replaceFirstChar(Char::titlecase)
        }

    private fun Priority.pretty(): String =
        name.lowercase().replaceFirstChar(Char::titlecase)
}
