package com.greninjaop.mailorganizer.core.rules

/**
 * Structured rule vocabulary (Phase 12).
 *
 * Rules are data, never code: a rule is a list of [RuleCondition]s (AND
 * semantics) plus a list of [RuleAction]s. There is intentionally no regex,
 * no scripting, and no arbitrary expressions — every condition is a
 * (field, operator, value) triple over bounded, normalized inputs.
 *
 * This file is pure Kotlin: no Android, no Room, no network. The engine
 * that evaluates these models lives in [RuleEngine].
 */

/** Fields a rule condition can match against. */
enum class RuleConditionField {
    /** Exact normalized sender address (lower-cased, trimmed). */
    SENDER_EMAIL,

    /** Registrable sender domain (e.g. `example.com`). */
    SENDER_DOMAIN,

    /** Canonical company id (`co:<domain>`, Phase 8). */
    COMPANY_ID,

    /** Keyword or phrase in the subject (case-insensitive). */
    SUBJECT,

    /** Gmail label id present on the message. */
    GMAIL_LABEL,

    /** Gmail category label (`CATEGORY_PERSONAL`, …). */
    GMAIL_CATEGORY,

    /** The deterministic classifier's category (pre-rule). */
    BASE_CATEGORY,

    /** The deterministic priority engine's level (pre-rule). */
    BASE_PRIORITY,

    /** Whether the message has attachments. Value: `"true"`/`"false"`. */
    HAS_ATTACHMENT,

    /** Whether an unsubscribe marker was detected. Value: `"true"`/`"false"`. */
    HAS_UNSUBSCRIBE,
}

/** Operators a rule condition can use. Deliberately small (phase §28). */
enum class RuleOperator {
    /** Exact match; text comparisons are case-insensitive. */
    EQUALS,

    /**
     * Substring match; only meaningful for [RuleConditionField.SUBJECT].
     * Case-insensitive. Never a regex — no backtracking risk by construction.
     */
    CONTAINS,
}

/**
 * One structured condition.
 *
 * [value] is normalized at rule-creation time: emails/domains lower-cased
 * and trimmed, subjects trimmed, booleans as `"true"`/`"false"`, enum
 * fields as enum names. The engine compares against equally normalized
 * message data, so matching is locale-independent.
 */
data class RuleCondition(
    val field: RuleConditionField,
    val operator: RuleOperator,
    val value: String,
) {
    init {
        require(value.length <= MAX_CONDITION_VALUE_CHARS) {
            "Rule condition value too long (${value.length} > $MAX_CONDITION_VALUE_CHARS)"
        }
    }

    companion object {
        const val MAX_CONDITION_VALUE_CHARS = 200
        const val MAX_CONDITIONS_PER_RULE = 10
    }
}

/** Actions a rule may take. Stays inside the intelligence/organization layer. */
enum class RuleActionType {
    /** Set the effective category. [RuleAction.value] is a MailCategory name. */
    SET_CATEGORY,

    /** Set the effective priority. [RuleAction.value] is a Priority name. */
    SET_PRIORITY,
}

/**
 * One structured action.
 *
 * [value] carries an enum *name* (e.g. `"CAREER"`, `"HIGH"`) rather than a
 * typed enum so `core.rules` keeps zero dependencies on the data layer —
 * the same boundary pattern as Phase 7's classifier. The domain layer
 * validates names when mapping to [com.greninjaop.mailorganizer.data.local.MailCategory]
 * / [com.greninjaop.mailorganizer.data.local.Priority].
 */
data class RuleAction(
    val type: RuleActionType,
    val value: String,
) {
    companion object {
        const val MAX_ACTIONS_PER_RULE = 4
    }
}

/**
 * A user rule as the engine sees it.
 *
 * Pure data: no database, no Android. [order] is the explicit,
 * user-controllable precedence (lower wins); [id] is the deterministic
 * tie-break — never insertion order, never hash order (phase §21).
 */
data class EvaluableRule(
    val id: Long,
    val name: String,
    val enabled: Boolean,
    val order: Int,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
)

/**
 * Normalized message view the engine matches against.
 *
 * All text is pre-normalized (lower-cased emails/domains, trimmed subject)
 * so evaluation is a pure function with no locale dependence.
 */
data class RuleMatchInput(
    val messageId: String,
    val accountId: String,
    val senderEmail: String,
    val senderDomain: String,
    val companyId: String?,
    val subject: String,
    val labelIds: List<String>,
    val gmailCategory: String?,
    val baseCategory: String?,
    val basePriority: String?,
    val hasAttachment: Boolean,
    val hasUnsubscribeMarker: Boolean,
)

/**
 * Result of evaluating rules against one message.
 *
 * [matchedRules] is in precedence order (order, then id). At most one rule
 * supplies each action type — the first matching rule in precedence order —
 * so the outcome is never ambiguous (phase §7, §20).
 */
data class RuleEvaluation(
    val matchedRules: List<EvaluableRule>,
    /** First matching rule carrying a SET_CATEGORY action, if any. */
    val categoryRule: EvaluableRule?,
    /** First matching rule carrying a SET_PRIORITY action, if any. */
    val priorityRule: EvaluableRule?,
    val effectiveCategory: String?,
    val effectivePriority: String?,
    /** Deterministically detected conflicts (same conditions, different actions). */
    val conflicts: List<RuleConflict>,
)

/**
 * Two enabled rules whose conditions are equivalent but whose actions
 * disagree — surfaced to the user instead of silently resolved (phase §20).
 * Resolution itself stays deterministic: lower [RuleConflict.winnerOrder]
 * (then lower id) wins. [ruleA]/[ruleB] are in precedence order (the same
 * order as [RuleEvaluation.matchedRules]), so the report is stable
 * regardless of the input list order.
 */
data class RuleConflict(
    val ruleA: EvaluableRule,
    val ruleB: EvaluableRule,
    val field: RuleActionType,
    val valueA: String,
    val valueB: String,
    val winnerId: Long,
)
