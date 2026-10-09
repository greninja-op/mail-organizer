package com.greninjaop.mailorganizer.domain.rules

import com.greninjaop.mailorganizer.core.rules.RuleAction
import com.greninjaop.mailorganizer.core.rules.RuleActionType
import com.greninjaop.mailorganizer.core.rules.RuleCondition
import com.greninjaop.mailorganizer.core.rules.RuleConditionField
import com.greninjaop.mailorganizer.core.rules.RuleJson
import com.greninjaop.mailorganizer.core.rules.RuleOperator
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.RuleSource
import com.greninjaop.mailorganizer.data.local.RuleType
import com.greninjaop.mailorganizer.data.local.UserRuleRecord

/**
 * Mappings between the storage row and the structured domain model
 * (Phase 12).
 *
 * Legacy Phase 2 rows (empty [UserRuleRecord.conditionsJson]) are derived
 * into the equivalent structured rule so old rows keep working through the
 * Phase 12 engine. New rows are always written in structured form; the
 * legacy columns are populated as a human-readable hint from the first
 * condition/action.
 */

/** Structured form of a stored rule, deriving from legacy columns when needed. */
fun UserRuleRecord.toUserRule(): UserRule {
    val conditions = RuleJson.decodeConditions(conditionsJson)
        .ifEmpty { legacyConditions() }
    val actions = RuleJson.decodeActions(actionsJson)
        .ifEmpty { legacyActions() }
    return UserRule(
        id = id,
        accountId = accountId,
        name = name.ifBlank { legacyName() },
        enabled = enabled,
        order = ruleOrder,
        conditions = conditions,
        actions = actions,
        source = source,
        version = ruleVersion,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}

/** Structured domain rule → storage row (always writes the JSON columns). */
fun UserRule.toRecord(): UserRuleRecord {
    val firstCondition = conditions.firstOrNull()
    val firstAction = actions.firstOrNull { it.type == RuleActionType.SET_CATEGORY }
        ?: actions.firstOrNull()
    return UserRuleRecord(
        id = id,
        accountId = accountId,
        ruleType = firstCondition?.toLegacyRuleType() ?: RuleType.SENDER_TO_CATEGORY,
        matcher = firstCondition?.value ?: "",
        targetCategory = firstAction
            ?.takeIf { it.type == RuleActionType.SET_CATEGORY }
            ?.let { runCatching { MailCategory.valueOf(it.value) }.getOrNull() },
        targetPriority = actions.firstOrNull { it.type == RuleActionType.SET_PRIORITY }
            ?.let { runCatching { Priority.valueOf(it.value) }.getOrNull() },
        targetActionRequired = null,
        enabled = enabled,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        name = name,
        conditionsJson = RuleJson.encodeConditions(conditions),
        actionsJson = RuleJson.encodeActions(actions),
        ruleOrder = order,
        ruleVersion = version,
        source = source,
    )
}

private fun RuleCondition.toLegacyRuleType(): RuleType = when (field) {
    RuleConditionField.SENDER_EMAIL -> RuleType.SENDER_TO_CATEGORY
    RuleConditionField.SENDER_DOMAIN -> RuleType.DOMAIN_TO_CATEGORY
    RuleConditionField.COMPANY_ID -> RuleType.COMPANY_TO_CATEGORY
    RuleConditionField.SUBJECT -> RuleType.KEYWORD_TO_CATEGORY
    else -> RuleType.SENDER_TO_CATEGORY
}

private fun UserRuleRecord.legacyConditions(): List<RuleCondition> {
    val field = when (ruleType) {
        RuleType.SENDER_TO_CATEGORY,
        RuleType.SENDER_TO_PRIORITY,
        RuleType.SENDER_TO_ACTION_REQUIRED,
        -> RuleConditionField.SENDER_EMAIL
        RuleType.DOMAIN_TO_CATEGORY -> RuleConditionField.SENDER_DOMAIN
        RuleType.COMPANY_TO_CATEGORY,
        RuleType.COMPANY_TO_PRIORITY,
        -> RuleConditionField.COMPANY_ID
        RuleType.KEYWORD_TO_CATEGORY -> RuleConditionField.SUBJECT
    }
    val operator = when (ruleType) {
        RuleType.KEYWORD_TO_CATEGORY -> RuleOperator.CONTAINS
        else -> RuleOperator.EQUALS
    }
    if (matcher.isBlank()) return emptyList()
    return listOf(RuleCondition(field, operator, matcher))
}

private fun UserRuleRecord.legacyActions(): List<RuleAction> {
    val actions = mutableListOf<RuleAction>()
    targetCategory?.let {
        actions += RuleAction(RuleActionType.SET_CATEGORY, it.name)
    }
    targetPriority?.let {
        actions += RuleAction(RuleActionType.SET_PRIORITY, it.name)
    }
    if (targetActionRequired == true) {
        actions += RuleAction(
            RuleActionType.SET_CATEGORY,
            MailCategory.ACTION_REQUIRED.name,
        )
    }
    return actions
}

private fun UserRuleRecord.legacyName(): String = buildString {
    append(
        when (ruleType) {
            RuleType.SENDER_TO_CATEGORY,
            RuleType.SENDER_TO_PRIORITY,
            RuleType.SENDER_TO_ACTION_REQUIRED,
            -> "Sender "
            RuleType.DOMAIN_TO_CATEGORY -> "Domain "
            RuleType.COMPANY_TO_CATEGORY,
            RuleType.COMPANY_TO_PRIORITY,
            -> "Company "
            RuleType.KEYWORD_TO_CATEGORY -> "Keyword "
        },
    )
    append(matcher)
    append(" → ")
    append(
        targetCategory?.name?.lowercase()?.replace('_', ' ')
            ?: targetPriority?.name?.lowercase()
            ?: "action required",
    )
}

/** Normalize a user-typed condition value the way the engine expects. */
fun normalizeConditionValue(field: RuleConditionField, raw: String): String =
    when (field) {
        RuleConditionField.SENDER_EMAIL,
        RuleConditionField.SENDER_DOMAIN,
        -> raw.trim().lowercase()
        RuleConditionField.GMAIL_LABEL,
        RuleConditionField.GMAIL_CATEGORY,
        RuleConditionField.BASE_CATEGORY,
        RuleConditionField.BASE_PRIORITY,
        -> raw.trim().uppercase()
        else -> raw.trim()
    }

/** Validate an action value name against the known enums. */
fun isValidActionValue(type: RuleActionType, value: String): Boolean =
    when (type) {
        RuleActionType.SET_CATEGORY ->
            runCatching { MailCategory.valueOf(value) }.isSuccess
        RuleActionType.SET_PRIORITY ->
            runCatching { Priority.valueOf(value) }.isSuccess
    }
