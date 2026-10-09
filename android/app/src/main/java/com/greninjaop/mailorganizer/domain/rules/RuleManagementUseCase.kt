package com.greninjaop.mailorganizer.domain.rules

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.rules.EvaluableRule
import com.greninjaop.mailorganizer.core.rules.RuleAction
import com.greninjaop.mailorganizer.core.rules.RuleCondition
import com.greninjaop.mailorganizer.core.rules.RuleConditionField
import com.greninjaop.mailorganizer.core.rules.RuleConflict
import com.greninjaop.mailorganizer.core.rules.RuleEngine
import com.greninjaop.mailorganizer.core.rules.RuleMatchInput
import com.greninjaop.mailorganizer.data.local.RuleSource
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * User-rule lifecycle management (Phase 12, phase §9–§13, §18–§23).
 *
 * - CRUD + enable/disable + deterministic reorder (phase §18, §21).
 * - Rule preview over local data only (phase §22): match count + samples.
 * - Conflict detection surfaced, never silently resolved (phase §20).
 * - Safe reprocessing on every mutation (phase §23): only affected
 *   messages are re-evaluated, bounded, on the IO dispatcher — never the
 *   UI thread. Full-mailbox reprocessing is the fallback when a rule has
 *   no selective condition, and stays bounded.
 *
 * All mutations are account-scoped (phase §12): a rule never affects
 * another account's mail.
 */
class RuleManagementUseCase(
    private val mail: MailRepository,
    private val rules: RuleRepository,
    private val apply: ApplyRulesUseCase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        private const val TAG = "RuleManagement"

        /** Bound for preview counts/samples and reprocessing. */
        const val CANDIDATE_LIMIT = 1_000

        /** How many sample messages a preview shows. */
        const val PREVIEW_SAMPLES = 8
    }

    /** All rules for the management UI, in deterministic (order, id) order. */
    fun observeRules(accountId: String): Flow<List<UserRule>> =
        rules.observeAllRules().map { records ->
            records.filter { it.accountId == accountId }
                .map { it.toUserRule() }
                .sortedWith(compareBy({ it.order }, { it.id }))
        }

    suspend fun getRule(id: Long): UserRule? = withContext(dispatchers.io) {
        rules.getRule(id)?.toUserRule()
    }

    /**
     * Creates a rule and applies it.
     *
     * @return the new rule id, or -1 when the rule is not well-formed.
     */
    suspend fun createRule(rule: UserRule): Long = withContext(dispatchers.io) {
        if (!rule.isWellFormed()) return@withContext -1L
        if (rule.actions.any { !isValidActionValue(it.type, it.value) }) return@withContext -1L
        val id = try {
            rules.addRule(
                rule.copy(
                    createdAtEpochMs = clock(),
                    updatedAtEpochMs = clock(),
                ).toRecord(),
            )
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Rule creation failed: ${t.javaClass.simpleName}")
            return@withContext -1L
        }
        reprocessForRule(rule.accountId, rule.copy(id = id))
        id
    }

    /**
     * Updates a rule and reprocesses affected mail.
     *
     * @return false when the rule is not well-formed or the update failed.
     */
    suspend fun updateRule(rule: UserRule): Boolean = withContext(dispatchers.io) {
        if (rule.id <= 0 || !rule.isWellFormed()) return@withContext false
        if (rule.actions.any { !isValidActionValue(it.type, it.value) }) return@withContext false
        val existing = rules.getRule(rule.id) ?: return@withContext false
        if (existing.accountId != rule.accountId) return@withContext false
        try {
            rules.updateRule(rule.copy(updatedAtEpochMs = clock()).toRecord())
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Rule update failed: ${t.javaClass.simpleName}")
            return@withContext false
        }
        reprocessForRule(rule.accountId, rule)
        true
    }

    /** Enables/disables a rule and reprocesses affected mail. */
    suspend fun setRuleEnabled(accountId: String, ruleId: Long, enabled: Boolean): Boolean =
        withContext(dispatchers.io) {
            val existing = rules.getRule(ruleId) ?: return@withContext false
            if (existing.accountId != accountId) return@withContext false
            try {
                rules.setRuleEnabled(ruleId, enabled)
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Rule enable failed: ${t.javaClass.simpleName}")
                return@withContext false
            }
            reprocessForRule(accountId, existing.toUserRule().copy(enabled = enabled))
            true
        }

    /** Deletes a rule (caller confirms) and reprocesses affected mail. */
    suspend fun deleteRule(accountId: String, ruleId: Long): Boolean =
        withContext(dispatchers.io) {
            val existing = rules.getRule(ruleId) ?: return@withContext false
            if (existing.accountId != accountId) return@withContext false
            try {
                rules.deleteRule(ruleId)
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Rule deletion failed: ${t.javaClass.simpleName}")
                return@withContext false
            }
            // The rule is gone: affected messages fall back to corrections,
            // other rules, then the deterministic base (phase §23, §27).
            reprocessForRule(accountId, existing.toUserRule().copy(enabled = false))
            true
        }

    /**
     * Moves a rule to an explicit position (phase §21).
     *
     * Deterministic: the full ordered id list is rewritten with contiguous
     * orders, so precedence never depends on anything implicit.
     */
    suspend fun reorderRules(accountId: String, orderedIds: List<Long>): Boolean =
        withContext(dispatchers.io) {
            val all = rules.getAllRules(accountId)
            if (all.map { it.id }.toSet() != orderedIds.toSet()) return@withContext false
            try {
                orderedIds.forEachIndexed { index, id ->
                    rules.getRule(id)?.let { record ->
                        if (record.accountId == accountId) {
                            rules.updateRule(record.copy(ruleOrder = index))
                        }
                    }
                }
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Rule reorder failed: ${t.javaClass.simpleName}")
                return@withContext false
            }
            // Precedence changed: reprocess everything the rules could touch.
            apply.refreshMailbox(accountId)
            true
        }

    /**
     * Previews a rule draft against local data only (phase §22).
     *
     * Never a Gmail live search — only rows already in the local database.
     */
    suspend fun previewRule(accountId: String, draft: UserRule): RulePreview =
        withContext(dispatchers.io) {
            val candidates = candidateMessageIds(accountId, draft)
            val samples = try {
                mail.getMessagesByIds(candidates.take(PREVIEW_SAMPLES))
            } catch (t: Throwable) {
                MoLogger.e(TAG, "Preview sample load failed: ${t.javaClass.simpleName}")
                emptyList()
            }
            RulePreview(
                matchCount = candidates.size,
                // The count is exact up to CANDIDATE_LIMIT; beyond that it is
                // reported as a lower bound, honestly.
                isExactCount = candidates.size < CANDIDATE_LIMIT,
                samples = samples.map {
                    RulePreviewSample(
                        messageId = it.messageId,
                        sender = it.fromAddress,
                        subject = it.subject,
                    )
                },
            )
        }

    /**
     * Deterministically detected conflicts across enabled rules
     * (phase §20) — surfaced in the UI, never silently resolved.
     */
    suspend fun detectConflicts(accountId: String): List<RuleConflict> =
        withContext(dispatchers.io) {
            val evaluable = rules.getAllRules(accountId)
                .map { it.toUserRule() }
                .filter { it.enabled }
                .map { it.toEvaluable() }
            RuleEngine.detectConflicts(evaluable)
        }

    /**
     * Candidate messages a rule could affect: the most selective structured
     * condition wins (indexed lookups); otherwise a bounded account scan.
     * Never the whole mailbox unbounded (phase §23).
     */
    suspend fun candidateMessageIds(accountId: String, rule: UserRule): List<String> =
        withContext(dispatchers.io) {
            val limit = CANDIDATE_LIMIT
            // Prefer the most selective condition for the candidate query.
            val senderEmail = rule.conditions.firstOrNull {
                it.field == RuleConditionField.SENDER_EMAIL
            }?.value
            if (senderEmail != null) {
                return@withContext mail.getMessageIdsBySender(accountId, senderEmail, limit)
            }
            val domain = rule.conditions.firstOrNull {
                it.field == RuleConditionField.SENDER_DOMAIN
            }?.value
            if (domain != null) {
                return@withContext mail.getMessageIdsByDomain(accountId, domain, limit)
            }
            val company = rule.conditions.firstOrNull {
                it.field == RuleConditionField.COMPANY_ID
            }?.value
            if (company != null) {
                return@withContext mail.getMessageIdsByCompany(accountId, company, limit)
            }
            // No selective condition: bounded full-account scan. The rule
            // still only *applies* where all its conditions match.
            mail.getMessageIdsByAccount(accountId, limit)
        }

    private suspend fun reprocessForRule(accountId: String, rule: UserRule) {
        val ids = candidateMessageIds(accountId, rule)
        for (id in ids) {
            // Per-message failures are isolated inside refreshMessage.
            apply.refreshMessage(id, accountId)
        }
    }
}

/** Result of [RuleManagementUseCase.previewRule]. */
data class RulePreview(
    val matchCount: Int,
    /** False when the count hit the candidate bound (a lower bound). */
    val isExactCount: Boolean,
    val samples: List<RulePreviewSample>,
)

data class RulePreviewSample(
    val messageId: String,
    val sender: String,
    val subject: String,
)

/**
 * Builds a rule from a correction ("also create a rule", phase §9).
 *
 * A MESSAGE-scope correction becomes a sender- or domain-scoped rule so it
 * applies to future mail too — the user picks the breadth explicitly.
 */
fun ruleFromCorrection(
    accountId: String,
    ruleName: String,
    senderEmail: String?,
    senderDomain: String?,
    preferDomain: Boolean,
    field: com.greninjaop.mailorganizer.data.local.CorrectionField,
    value: String,
    nowEpochMs: Long,
): UserRule? {
    val condition = when {
        preferDomain && !senderDomain.isNullOrBlank() -> RuleCondition(
            RuleConditionField.SENDER_DOMAIN,
            com.greninjaop.mailorganizer.core.rules.RuleOperator.EQUALS,
            senderDomain.lowercase(),
        )
        !senderEmail.isNullOrBlank() -> RuleCondition(
            RuleConditionField.SENDER_EMAIL,
            com.greninjaop.mailorganizer.core.rules.RuleOperator.EQUALS,
            senderEmail.lowercase(),
        )
        else -> return null
    }
    val actionType = when (field) {
        com.greninjaop.mailorganizer.data.local.CorrectionField.CATEGORY ->
            com.greninjaop.mailorganizer.core.rules.RuleActionType.SET_CATEGORY
        com.greninjaop.mailorganizer.data.local.CorrectionField.PRIORITY ->
            com.greninjaop.mailorganizer.core.rules.RuleActionType.SET_PRIORITY
        else -> return null
    }
    if (!isValidActionValue(actionType, value)) return null
    return UserRule(
        accountId = accountId,
        name = ruleName.ifBlank { "Rule from correction" },
        conditions = listOf(condition),
        actions = listOf(RuleAction(actionType, value)),
        source = RuleSource.FROM_CORRECTION,
        createdAtEpochMs = nowEpochMs,
        updatedAtEpochMs = nowEpochMs,
    )
}
