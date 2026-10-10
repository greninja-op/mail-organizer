package com.greninjaop.mailorganizer.core.automation

/**
 * Deterministic Condition Evaluator for Automation Rules (Phase 27 §16, §18, §19).
 *
 * Rules:
 * - Pure Kotlin, locale-independent, safe from regex backtracking or arbitrary code execution.
 * - Evaluates single conditions and structured condition groups (ALL, ANY, NONE).
 * - Bounded input comparisons.
 */
object AutomationConditionEvaluator {

    /**
     * Evaluates a full condition group against the message evaluation context.
     * Empty condition list matches true by definition (matches all trigger events).
     */
    fun evaluateGroup(
        group: AutomationConditionGroup,
        context: AutomationEvaluationContext,
    ): Boolean {
        if (group.conditions.isEmpty()) return true

        return when (group.logic) {
            ConditionGroupLogic.ALL -> group.conditions.all { evaluateCondition(it, context) }
            ConditionGroupLogic.ANY -> group.conditions.any { evaluateCondition(it, context) }
            ConditionGroupLogic.NONE -> group.conditions.none { evaluateCondition(it, context) }
        }
    }

    /**
     * Evaluates a single condition against the context.
     */
    fun evaluateCondition(
        condition: AutomationCondition,
        context: AutomationEvaluationContext,
    ): Boolean {
        val targetValue = condition.value.trim()

        return when (condition.field) {
            AutomationConditionField.SENDER_EMAIL -> compareString(
                actual = context.fromAddress.trim().lowercase(),
                operator = condition.operator,
                expected = targetValue.lowercase(),
            )

            AutomationConditionField.SENDER_DOMAIN -> compareString(
                actual = context.fromDomain.trim().lowercase(),
                operator = condition.operator,
                expected = targetValue.lowercase(),
            )

            AutomationConditionField.COMPANY_ID -> compareString(
                actual = context.companyId.orEmpty().trim().lowercase(),
                operator = condition.operator,
                expected = targetValue.lowercase(),
            )

            AutomationConditionField.SUBJECT -> compareString(
                actual = context.subject.lowercase(),
                operator = condition.operator,
                expected = targetValue.lowercase(),
            )

            AutomationConditionField.CATEGORY -> compareString(
                actual = context.category.trim().uppercase(),
                operator = condition.operator,
                expected = targetValue.uppercase(),
            )

            AutomationConditionField.PRIORITY -> compareString(
                actual = context.priority.trim().uppercase(),
                operator = condition.operator,
                expected = targetValue.uppercase(),
            )

            AutomationConditionField.IS_ACTION_REQUIRED -> compareBoolean(
                actual = context.isActionRequired,
                operator = condition.operator,
                expected = targetValue.toBoolean(),
            )

            AutomationConditionField.IS_UNREAD -> compareBoolean(
                actual = context.isUnread,
                operator = condition.operator,
                expected = targetValue.toBoolean(),
            )

            AutomationConditionField.IS_STARRED -> compareBoolean(
                actual = context.isStarred,
                operator = condition.operator,
                expected = targetValue.toBoolean(),
            )

            AutomationConditionField.HAS_ATTACHMENT -> compareBoolean(
                actual = context.hasAttachment,
                operator = condition.operator,
                expected = targetValue.toBoolean(),
            )

            AutomationConditionField.HAS_UNSUBSCRIBE -> compareBoolean(
                actual = context.hasUnsubscribe,
                operator = condition.operator,
                expected = targetValue.toBoolean(),
            )

            AutomationConditionField.GMAIL_LABEL -> compareLabel(
                labels = context.gmailLabels,
                operator = condition.operator,
                expected = targetValue,
            )
        }
    }

    private fun compareString(
        actual: String,
        operator: AutomationOperator,
        expected: String,
    ): Boolean = when (operator) {
        AutomationOperator.EQUALS -> actual == expected
        AutomationOperator.NOT_EQUALS -> actual != expected
        AutomationOperator.CONTAINS -> actual.contains(expected)
        AutomationOperator.NOT_CONTAINS -> !actual.contains(expected)
    }

    private fun compareBoolean(
        actual: Boolean,
        operator: AutomationOperator,
        expected: Boolean,
    ): Boolean = when (operator) {
        AutomationOperator.EQUALS -> actual == expected
        AutomationOperator.NOT_EQUALS -> actual != expected
        AutomationOperator.CONTAINS -> actual == expected
        AutomationOperator.NOT_CONTAINS -> actual != expected
    }

    private fun compareLabel(
        labels: List<String>,
        operator: AutomationOperator,
        expected: String,
    ): Boolean {
        val hasLabel = labels.any { it.equals(expected, ignoreCase = true) }
        return when (operator) {
            AutomationOperator.EQUALS,
            AutomationOperator.CONTAINS -> hasLabel
            AutomationOperator.NOT_EQUALS,
            AutomationOperator.NOT_CONTAINS -> !hasLabel
        }
    }
}
