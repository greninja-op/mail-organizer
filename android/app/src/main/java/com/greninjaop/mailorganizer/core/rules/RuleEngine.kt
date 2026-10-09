package com.greninjaop.mailorganizer.core.rules

/**
 * Deterministic user-rule evaluator (Phase 12).
 *
 * Pure function of (normalized input, rules): no randomness, no network,
 * no device state, no database. Total — malformed conditions degrade to
 * "no match", never throw.
 *
 * Precedence (phase §7, §20, §21):
 * 1. Explicit user corrections outrank rules entirely — corrections are
 *    applied by the domain layer *before* rules are consulted, so the
 *    engine only ever sees rules.
 * 2. Among rules: lower [EvaluableRule.order] wins; ties break by lower
 *    [EvaluableRule.id]. Never insertion order, DB row order, or hash order.
 * 3. Conditions within a rule use AND semantics. A rule with no conditions
 *    never matches (it would otherwise match everything).
 * 4. At most one rule supplies each action type: the first matching rule
 *    in precedence order. The outcome is never ambiguous.
 */
object RuleEngine {

    /** Engine version, stamped on rows the rule layer writes (phase §26). */
    const val VERSION = 1

    fun evaluate(
        input: RuleMatchInput,
        rules: List<EvaluableRule>,
    ): RuleEvaluation {
        val matched = rules
            .asSequence()
            .filter { it.enabled }
            .filter { matchesAll(it.conditions, input) }
            .sortedWith(compareBy({ it.order }, { it.id }))
            .toList()
        val categoryRule = matched.firstOrNull { rule ->
            rule.actions.any { it.type == RuleActionType.SET_CATEGORY }
        }
        val priorityRule = matched.firstOrNull { rule ->
            rule.actions.any { it.type == RuleActionType.SET_PRIORITY }
        }
        return RuleEvaluation(
            matchedRules = matched,
            categoryRule = categoryRule,
            priorityRule = priorityRule,
            effectiveCategory = categoryRule
                ?.actions
                ?.first { it.type == RuleActionType.SET_CATEGORY }
                ?.value,
            effectivePriority = priorityRule
                ?.actions
                ?.first { it.type == RuleActionType.SET_PRIORITY }
                ?.value,
            conflicts = detectConflicts(matched),
        )
    }

    /**
     * Deterministic conflict detection (phase §20).
     *
     * Two enabled, matching rules conflict when their condition sets are
     * equivalent but they set different values for the same action type.
     * The winner is the precedence winner (lower order, then lower id) —
     * reported, not silently chosen.
     */
    fun detectConflicts(rules: List<EvaluableRule>): List<RuleConflict> {
        val conflicts = mutableListOf<RuleConflict>()
        for (i in rules.indices) {
            for (j in i + 1 until rules.size) {
                val a = rules[i]
                val b = rules[j]
                if (!sameConditions(a.conditions, b.conditions)) continue
                for (type in RuleActionType.entries) {
                    val va = a.actions.firstOrNull { it.type == type }?.value
                    val vb = b.actions.firstOrNull { it.type == type }?.value
                    if (va != null && vb != null && va != vb) {
                        val winner = if (compareBy<EvaluableRule>({ it.order }, { it.id })
                                .compare(a, b) <= 0
                        ) a else b
                        conflicts += RuleConflict(
                            ruleA = a,
                            ruleB = b,
                            field = type,
                            valueA = va,
                            valueB = vb,
                            winnerId = winner.id,
                        )
                    }
                }
            }
        }
        return conflicts
    }

    /** Canonical signature for a condition set (order-independent). */
    fun conditionSignature(conditions: List<RuleCondition>): String =
        conditions
            .map { "${it.field.name}:${it.operator.name}:${it.value}" }
            .sorted()
            .joinToString("|")

    private fun sameConditions(
        a: List<RuleCondition>,
        b: List<RuleCondition>,
    ): Boolean = conditionSignature(a) == conditionSignature(b)

    private fun matchesAll(
        conditions: List<RuleCondition>,
        input: RuleMatchInput,
    ): Boolean {
        if (conditions.isEmpty()) return false
        if (conditions.size > RuleCondition.MAX_CONDITIONS_PER_RULE) return false
        return conditions.all { matches(it, input) }
    }

    private fun matches(condition: RuleCondition, input: RuleMatchInput): Boolean =
        when (condition.field) {
            RuleConditionField.SENDER_EMAIL ->
                condition.operator == RuleOperator.EQUALS &&
                    input.senderEmail.equals(condition.value, ignoreCase = true)

            RuleConditionField.SENDER_DOMAIN ->
                condition.operator == RuleOperator.EQUALS &&
                    input.senderDomain.equals(condition.value, ignoreCase = true)

            RuleConditionField.COMPANY_ID ->
                condition.operator == RuleOperator.EQUALS &&
                    input.companyId == condition.value

            RuleConditionField.SUBJECT -> when (condition.operator) {
                RuleOperator.EQUALS ->
                    input.subject.equals(condition.value, ignoreCase = true)
                RuleOperator.CONTAINS ->
                    input.subject.contains(condition.value, ignoreCase = true)
            }

            RuleConditionField.GMAIL_LABEL ->
                condition.operator == RuleOperator.EQUALS &&
                    input.labelIds.any { it.equals(condition.value, ignoreCase = true) }

            RuleConditionField.GMAIL_CATEGORY ->
                condition.operator == RuleOperator.EQUALS &&
                    input.gmailCategory?.equals(condition.value, ignoreCase = true) == true

            RuleConditionField.BASE_CATEGORY ->
                condition.operator == RuleOperator.EQUALS &&
                    input.baseCategory?.equals(condition.value, ignoreCase = true) == true

            RuleConditionField.BASE_PRIORITY ->
                condition.operator == RuleOperator.EQUALS &&
                    input.basePriority?.equals(condition.value, ignoreCase = true) == true

            RuleConditionField.HAS_ATTACHMENT ->
                condition.operator == RuleOperator.EQUALS &&
                    input.hasAttachment == condition.value.toBooleanStrictOrNull()

            RuleConditionField.HAS_UNSUBSCRIBE ->
                condition.operator == RuleOperator.EQUALS &&
                    input.hasUnsubscribeMarker == condition.value.toBooleanStrictOrNull()
        }
}
