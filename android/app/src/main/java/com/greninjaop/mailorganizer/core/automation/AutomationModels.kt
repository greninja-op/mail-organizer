package com.greninjaop.mailorganizer.core.automation

/**
 * Advanced Automation domain models (Phase 27).
 *
 * Core Principles:
 * - User-controlled: deliberately created or enabled by user.
 * - Explainable: provenance stamps on every execution.
 * - Account-scoped: strict boundaries, cross-account execution rejected.
 * - Privacy-safe: local-first, email content cannot execute arbitrary code.
 * - Conservative around destructive actions: strong confirmation defaults.
 *
 * All types in this file are pure Kotlin (no Android/Room imports) to
 * maintain clean KMP architecture.
 */

/** Automation scope across accounts (Phase 27 §8, §9). */
enum class AutomationScopeType {
    SPECIFIC_ACCOUNT,
    ALL_ACCOUNTS,
}

/** Explicit account target specification. */
data class AutomationAccountScope(
    val type: AutomationScopeType,
    val specificAccountId: String? = null,
) {
    fun appliesToAccount(targetAccountId: String): Boolean = when (type) {
        AutomationScopeType.ALL_ACCOUNTS -> true
        AutomationScopeType.SPECIFIC_ACCOUNT -> specificAccountId == targetAccountId
    }
}

/**
 * Deterministic automation triggers (Phase 27 §12, §13).
 */
enum class AutomationTriggerType {
    /** Fires when a new message is first synchronized locally. */
    NEW_EMAIL_SYNCED,
    /** Fires when a message is classified as Action Required. */
    ACTION_REQUIRED_DETECTED,
    /** Fires when category matches a specific category. */
    CATEGORY_ASSIGNED,
    /** Fires when priority matches a specific priority. */
    PRIORITY_ASSIGNED,
    /** Fires on scheduled / periodic background evaluation. */
    SCHEDULED,
    /** Fires only when explicitly invoked by the user in UI. */
    MANUAL_RUN,
}

/** Structured trigger definition. */
data class AutomationTrigger(
    val type: AutomationTriggerType,
    val parameter: String? = null, // e.g. Category name or Priority name
)

/** Structured condition field. */
enum class AutomationConditionField {
    SENDER_EMAIL,
    SENDER_DOMAIN,
    COMPANY_ID,
    SUBJECT,
    CATEGORY,
    PRIORITY,
    IS_ACTION_REQUIRED,
    IS_UNREAD,
    IS_STARRED,
    HAS_ATTACHMENT,
    HAS_UNSUBSCRIBE,
    GMAIL_LABEL,
}

/** Structured condition operator. */
enum class AutomationOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
}

/** A single structured condition (Phase 27 §16, §17). */
data class AutomationCondition(
    val field: AutomationConditionField,
    val operator: AutomationOperator,
    val value: String,
) {
    init {
        require(value.length <= MAX_CONDITION_VALUE_LENGTH) {
            "Condition value exceeds maximum length of $MAX_CONDITION_VALUE_LENGTH"
        }
    }

    companion object {
        const val MAX_CONDITION_VALUE_LENGTH = 200
    }
}

/** Logical group combination (Phase 27 §18). */
enum class ConditionGroupLogic {
    ALL,
    ANY,
    NONE,
}

/** A group of conditions evaluated together. */
data class AutomationConditionGroup(
    val logic: ConditionGroupLogic = ConditionGroupLogic.ALL,
    val conditions: List<AutomationCondition> = emptyList(),
)

/** Structured automation actions from a finite, safe set (Phase 27 §21). */
enum class AutomationActionType {
    // Non-destructive local mutations
    SET_CATEGORY,
    SET_PRIORITY,
    MARK_ACTION_REQUIRED,
    CREATE_LOCAL_REMINDER,

    // External proposals (routed via Action Engine & Integration Manager)
    CREATE_CALENDAR_PROPOSAL,
    CREATE_TASK_PROPOSAL,

    // Controlled Gmail mutations
    MARK_AS_READ,
    MARK_AS_UNREAD,
    STAR_MESSAGE,
    UNSTAR_MESSAGE,
    ARCHIVE_MESSAGE,
    TRASH_MESSAGE,
    ADD_GMAIL_LABEL,
    REMOVE_GMAIL_LABEL,
}

/** Structured action specification. */
data class AutomationAction(
    val type: AutomationActionType,
    val parameter: String? = null, // Category/Priority name, Label ID, reminder notes
) {
    /** True if this action modifies external Gmail or moves mail to trash. */
    val isDestructiveOrExternal: Boolean
        get() = when (type) {
            AutomationActionType.TRASH_MESSAGE,
            AutomationActionType.ARCHIVE_MESSAGE,
            AutomationActionType.REMOVE_GMAIL_LABEL,
            AutomationActionType.CREATE_CALENDAR_PROPOSAL,
            AutomationActionType.CREATE_TASK_PROPOSAL -> true
            else -> false
        }
}

/** Confirmation requirement policy (Phase 27 §23, §24, §25). */
enum class AutomationConfirmationPolicy {
    ALWAYS_CONFIRM,
    CONFIRM_FIRST_TIME,
    PRE_APPROVED,
}

/** High-level lifecycle state of an automation rule (Phase 27 §163). */
enum class AutomationLifecycleState {
    ENABLED,
    DISABLED,
    PAUSED_CIRCUIT_BREAKER,
    INVALID,
}

/**
 * The complete Automation entity model (Phase 27 §8).
 */
data class AutomationRule(
    val id: String,
    val name: String,
    val description: String = "",
    val scope: AutomationAccountScope,
    val state: AutomationLifecycleState = AutomationLifecycleState.ENABLED,
    val trigger: AutomationTrigger,
    val conditionGroup: AutomationConditionGroup = AutomationConditionGroup(),
    val actions: List<AutomationAction>,
    val confirmationPolicy: AutomationConfirmationPolicy = AutomationConfirmationPolicy.ALWAYS_CONFIRM,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val version: Int = 1,
    val lastRunEpochMs: Long? = null,
    val failureCount: Int = 0,
) {
    val isEnabled: Boolean get() = state == AutomationLifecycleState.ENABLED
}

/**
 * Execution history states (Phase 27 §40, §41).
 */
enum class AutomationExecutionStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED,
    CANCELLED,
    WAITING_CONFIRMATION,
    UNKNOWN,
}

/**
 * Detailed execution log entry (Phase 27 §39, §81).
 * Never stores full email bodies.
 */
data class AutomationExecutionRecord(
    val executionId: String,
    val automationId: String,
    val automationName: String,
    val accountId: String,
    val triggerType: AutomationTriggerType,
    val sourceMessageId: String?,
    val sourceThreadId: String?,
    val actionSummary: String,
    val status: AutomationExecutionStatus,
    val executedAtEpochMs: Long,
    val detailMessage: String? = null,
)

/**
 * Evaluation context passed to the automation engine for an event.
 */
data class AutomationEvaluationContext(
    val accountId: String,
    val messageId: String,
    val threadId: String,
    val fromAddress: String,
    val fromDomain: String,
    val companyId: String?,
    val subject: String,
    val category: String,
    val priority: String,
    val isActionRequired: Boolean,
    val isUnread: Boolean,
    val isStarred: Boolean,
    val hasAttachment: Boolean,
    val hasUnsubscribe: Boolean,
    val gmailLabels: List<String>,
    val timestampEpochMs: Long,
)
