package com.greninjaop.mailorganizer.domain.rules

import com.greninjaop.mailorganizer.core.rules.EvaluableRule
import com.greninjaop.mailorganizer.core.rules.RuleAction
import com.greninjaop.mailorganizer.core.rules.RuleCondition
import com.greninjaop.mailorganizer.data.local.RuleSource

/**
 * A user rule in domain form (Phase 12).
 *
 * The storage row ([com.greninjaop.mailorganizer.data.local.UserRuleRecord])
 * carries both the structured Phase 12 vocabulary and the legacy Phase 2
 * columns; this model is always the structured form. See [toUserRule].
 */
data class UserRule(
    val id: Long = 0,
    val accountId: String,
    val name: String,
    val enabled: Boolean = true,
    val order: Int = 0,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val source: RuleSource = RuleSource.MANUAL,
    val version: Int = 1,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    /** A rule with no conditions or no actions can never do anything. */
    fun isWellFormed(): Boolean =
        name.isNotBlank() &&
            conditions.isNotEmpty() &&
            conditions.size <= RuleCondition.MAX_CONDITIONS_PER_RULE &&
            actions.isNotEmpty() &&
            actions.size <= RuleAction.MAX_ACTIONS_PER_RULE

    fun toEvaluable(): EvaluableRule = EvaluableRule(
        id = id,
        name = name,
        enabled = enabled,
        order = order,
        conditions = conditions,
        actions = actions,
    )

    /**
     * Human-readable summary, e.g. `When sender domain is "example.com"
     * then category = Career`. Used in the rules list and detail screens.
     */
    fun describe(): String {
        val whenPart = conditions.joinToString(" and ") { it.describe() }
        val thenPart = actions.joinToString(" and ") { it.describe() }
        return "When $whenPart then $thenPart"
    }
}

private fun RuleCondition.describe(): String = when (field) {
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.SENDER_EMAIL ->
        "sender is \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.SENDER_DOMAIN ->
        "sender domain is \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.COMPANY_ID ->
        "company is \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.SUBJECT ->
        if (operator == com.greninjaop.mailorganizer.core.rules.RuleOperator.CONTAINS) {
            "subject contains \"$value\""
        } else {
            "subject is \"$value\""
        }
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.GMAIL_LABEL ->
        "has Gmail label \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.GMAIL_CATEGORY ->
        "Gmail category is \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.BASE_CATEGORY ->
        "detected category is \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.BASE_PRIORITY ->
        "detected priority is \"$value\""
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.HAS_ATTACHMENT ->
        if (value == "true") "has attachments" else "has no attachments"
    com.greninjaop.mailorganizer.core.rules.RuleConditionField.HAS_UNSUBSCRIBE ->
        if (value == "true") "has unsubscribe link" else "has no unsubscribe link"
}

private fun RuleAction.describe(): String = when (type) {
    com.greninjaop.mailorganizer.core.rules.RuleActionType.SET_CATEGORY ->
        "category = ${value.prettyEnum()}"
    com.greninjaop.mailorganizer.core.rules.RuleActionType.SET_PRIORITY ->
        "priority = ${value.prettyEnum()}"
}

private fun String.prettyEnum(): String =
    lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
