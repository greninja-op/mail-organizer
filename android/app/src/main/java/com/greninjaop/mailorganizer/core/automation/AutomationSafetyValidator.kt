package com.greninjaop.mailorganizer.core.automation

/**
 * Result of automation safety validation.
 */
sealed interface AutomationSafetyResult {
    data object Approved : AutomationSafetyResult
    data class RequiresConfirmation(val reason: String) : AutomationSafetyResult
    data class Blocked(val reason: String) : AutomationSafetyResult
}

/**
 * Central Safety Validator for Advanced Automation (Phase 27 §30, §31, §51, §52, §75, §79, §150).
 *
 * Enforces:
 * - Automation enabled check.
 * - Account scope isolation: cross-account triggers are unconditionally rejected.
 * - Protected categories shield: Security, Action Required, Critical/High Priority, Receipts, Career, Education.
 * - Circuit breaker: automated pausing on repeated failures or recursion loops.
 * - Prohibition of auto-permanent-delete, auto-send, auto-reply, auto-unsubscribe.
 * - Safe default confirmation for external or destructive actions.
 */
object AutomationSafetyValidator {

    /** Maximum consecutive failures before circuit breaker pauses rule (§150). */
    const val MAX_FAILURE_THRESHOLD = 5

    /** Protected categories requiring heightened confirmation safeguards (§51, §52). */
    val PROTECTED_CATEGORIES = setOf(
        "SECURITY",
        "ACTION_REQUIRED",
        "RECEIPTS_ORDERS",
        "CAREER",
        "EDUCATION",
    )

    val PROTECTED_PRIORITIES = setOf(
        "CRITICAL",
        "HIGH",
    )

    /**
     * Validates whether a rule is safe to execute in the given context.
     */
    fun validate(
        rule: AutomationRule,
        targetAccountId: String,
        context: AutomationEvaluationContext?,
        isExplicitUserConfirmed: Boolean = false,
        circuitBreakerTripCount: Int = rule.failureCount,
    ): AutomationSafetyResult {
        // 1. Lifecycle state validation
        if (rule.state == AutomationLifecycleState.DISABLED) {
            return AutomationSafetyResult.Blocked("Automation '${rule.name}' is disabled")
        }
        if (rule.state == AutomationLifecycleState.PAUSED_CIRCUIT_BREAKER || circuitBreakerTripCount >= MAX_FAILURE_THRESHOLD) {
            return AutomationSafetyResult.Blocked("Automation '${rule.name}' is paused by safety circuit breaker ($circuitBreakerTripCount failures)")
        }
        if (rule.state == AutomationLifecycleState.INVALID) {
            return AutomationSafetyResult.Blocked("Automation '${rule.name}' is marked invalid")
        }

        // 2. Strict Account Scope Boundary (§10, §32, §33)
        if (!rule.scope.appliesToAccount(targetAccountId)) {
            return AutomationSafetyResult.Blocked(
                "Cross-account execution rejected: rule scoped to ${rule.scope.specificAccountId ?: "specific"} " +
                    "cannot run on account $targetAccountId"
            )
        }

        // 3. Prohibited Actions Safety Gate (§27, §28, §29)
        for (action in rule.actions) {
            val name = action.type.name
            if (name.contains("PERMANENT_DELETE") || name.contains("AUTO_SEND") ||
                name.contains("AUTO_REPLY") || name.contains("UNSUBSCRIBE")
            ) {
                return AutomationSafetyResult.Blocked("Prohibited automation action: $name")
            }
        }

        // 3b. Conflicting Actions Gate (§75, §76)
        val conflicts = detectConflicts(rule.actions)
        if (conflicts.isNotEmpty()) {
            return AutomationSafetyResult.Blocked("Conflicting actions: " + conflicts.joinToString("; "))
        }

        // 4. Protected Category & Priority Shield (§51, §52)
        if (context != null) {
            val isProtectedCategory = PROTECTED_CATEGORIES.contains(context.category.uppercase())
            val isProtectedPriority = PROTECTED_PRIORITIES.contains(context.priority.uppercase())
            val hasDestructiveAction = rule.actions.any {
                it.type == AutomationActionType.TRASH_MESSAGE ||
                    it.type == AutomationActionType.ARCHIVE_MESSAGE
            }

            if ((isProtectedCategory || isProtectedPriority) && hasDestructiveAction) {
                if (!isExplicitUserConfirmed) {
                    val categoryReason = if (isProtectedCategory) "category '${context.category}'" else "priority '${context.priority}'"
                    return AutomationSafetyResult.RequiresConfirmation(
                        "Message belongs to protected $categoryReason. Moving to Trash or Archive requires explicit confirmation."
                    )
                }
            }
        }

        // 5. Destructive / External Confirmation Policy (§23, §24)
        val hasExternalOrDestructive = rule.actions.any { it.isDestructiveOrExternal }
        if (rule.confirmationPolicy == AutomationConfirmationPolicy.ALWAYS_CONFIRM) {
            if (!isExplicitUserConfirmed) {
                return AutomationSafetyResult.RequiresConfirmation(
                    "Automation '${rule.name}' is set to always require user confirmation."
                )
            }
        } else if (hasExternalOrDestructive) {
            when (rule.confirmationPolicy) {
                AutomationConfirmationPolicy.CONFIRM_FIRST_TIME -> {
                    if (rule.lastRunEpochMs == null && !isExplicitUserConfirmed) {
                        return AutomationSafetyResult.RequiresConfirmation(
                            "First-time execution of external/destructive automation '${rule.name}' requires confirmation."
                        )
                    }
                }
                AutomationConfirmationPolicy.PRE_APPROVED,
                AutomationConfirmationPolicy.ALWAYS_CONFIRM -> {
                    // Handled above or pre-approved
                }
            }
        }

        return AutomationSafetyResult.Approved
    }

    /**
     * Detects conflicting actions within a single automation rule (§75, §76).
     */
    fun detectConflicts(actions: List<AutomationAction>): List<String> {
        val conflicts = mutableListOf<String>()
        val types = actions.map { it.type }.toSet()

        if (types.contains(AutomationActionType.TRASH_MESSAGE) && types.contains(AutomationActionType.ARCHIVE_MESSAGE)) {
            conflicts.add("Cannot simultaneously Trash and Archive a message.")
        }
        if (types.contains(AutomationActionType.MARK_AS_READ) && types.contains(AutomationActionType.MARK_AS_UNREAD)) {
            conflicts.add("Cannot simultaneously Mark as Read and Mark as Unread.")
        }
        if (types.contains(AutomationActionType.STAR_MESSAGE) && types.contains(AutomationActionType.UNSTAR_MESSAGE)) {
            conflicts.add("Cannot simultaneously Star and Unstar a message.")
        }

        return conflicts
    }
}
