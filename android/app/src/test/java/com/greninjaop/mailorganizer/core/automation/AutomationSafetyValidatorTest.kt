package com.greninjaop.mailorganizer.core.automation

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AutomationSafetyValidator (Phase 27 §30, §31, §51, §52, §75, §79, §150).
 */
class AutomationSafetyValidatorTest {

    private val safeRule = AutomationRule(
        id = "rule-1",
        name = "Auto Categorize Newsletters",
        scope = AutomationAccountScope(AutomationScopeType.SPECIFIC_ACCOUNT, "account-1"),
        state = AutomationLifecycleState.ENABLED,
        trigger = AutomationTrigger(AutomationTriggerType.NEW_EMAIL_SYNCED),
        actions = listOf(AutomationAction(AutomationActionType.SET_CATEGORY, "NEWSLETTERS")),
        confirmationPolicy = AutomationConfirmationPolicy.PRE_APPROVED,
        createdAtEpochMs = 1000L,
        updatedAtEpochMs = 1000L,
    )

    private val normalContext = AutomationEvaluationContext(
        accountId = "account-1",
        messageId = "msg-1",
        threadId = "th-1",
        fromAddress = "news@tech.com",
        fromDomain = "tech.com",
        companyId = null,
        subject = "Weekly Digest",
        category = "NEWSLETTERS",
        priority = "LOW",
        isActionRequired = false,
        isUnread = true,
        isStarred = false,
        hasAttachment = false,
        hasUnsubscribe = true,
        gmailLabels = listOf("INBOX"),
        timestampEpochMs = 1000L,
    )

    @Test
    fun safePreApprovedRule_isApproved() {
        val result = AutomationSafetyValidator.validate(safeRule, "account-1", normalContext)
        assertTrue(result is AutomationSafetyResult.Approved)
    }

    @Test
    fun disabledOrInvalidRule_isBlocked() {
        val disabledRule = safeRule.copy(state = AutomationLifecycleState.DISABLED)
        val result1 = AutomationSafetyValidator.validate(disabledRule, "account-1", normalContext)
        assertTrue(result1 is AutomationSafetyResult.Blocked)

        val invalidRule = safeRule.copy(state = AutomationLifecycleState.INVALID)
        val result2 = AutomationSafetyValidator.validate(invalidRule, "account-1", normalContext)
        assertTrue(result2 is AutomationSafetyResult.Blocked)
    }

    @Test
    fun circuitBreakerTripped_isBlocked() {
        val trippedRule = safeRule.copy(failureCount = AutomationSafetyValidator.MAX_FAILURE_THRESHOLD)
        val result = AutomationSafetyValidator.validate(trippedRule, "account-1", normalContext)
        assertTrue(result is AutomationSafetyResult.Blocked)
    }

    @Test
    fun crossAccountTarget_isBlocked() {
        // Rule belongs to account-1, but fired for account-2
        val result = AutomationSafetyValidator.validate(safeRule, "account-2", normalContext)
        assertTrue(result is AutomationSafetyResult.Blocked)
        assertTrue((result as AutomationSafetyResult.Blocked).reason.contains("Cross-account"))
    }

    @Test
    fun allAccountsScope_allowsAnyAccount() {
        val globalRule = safeRule.copy(
            scope = AutomationAccountScope(AutomationScopeType.ALL_ACCOUNTS)
        )
        val result = AutomationSafetyValidator.validate(globalRule, "account-99", normalContext)
        assertTrue(result is AutomationSafetyResult.Approved)
    }

    @Test
    fun protectedCategory_requiresConfirmationForTrash() {
        val trashAction = listOf(AutomationAction(AutomationActionType.TRASH_MESSAGE))
        val trashRule = safeRule.copy(
            actions = trashAction,
            confirmationPolicy = AutomationConfirmationPolicy.PRE_APPROVED,
        )
        val securityContext = normalContext.copy(category = "SECURITY")

        // Without explicit user confirmation, must be RequiresConfirmation
        val unconfirmedResult = AutomationSafetyValidator.validate(
            trashRule,
            "account-1",
            securityContext,
            isExplicitUserConfirmed = false,
        )
        assertTrue(unconfirmedResult is AutomationSafetyResult.RequiresConfirmation)

        // With explicit user confirmation, passes through
        val confirmedResult = AutomationSafetyValidator.validate(
            trashRule,
            "account-1",
            securityContext,
            isExplicitUserConfirmed = true,
        )
        assertTrue(confirmedResult is AutomationSafetyResult.Approved)
    }

    @Test
    fun protectedPriority_requiresConfirmationForArchive() {
        val archiveAction = listOf(AutomationAction(AutomationActionType.ARCHIVE_MESSAGE))
        val archiveRule = safeRule.copy(
            actions = archiveAction,
            confirmationPolicy = AutomationConfirmationPolicy.PRE_APPROVED,
        )
        val highPriorityContext = normalContext.copy(priority = "HIGH")

        val result = AutomationSafetyValidator.validate(
            archiveRule,
            "account-1",
            highPriorityContext,
            isExplicitUserConfirmed = false,
        )
        assertTrue(result is AutomationSafetyResult.RequiresConfirmation)
    }

    @Test
    fun alwaysConfirmPolicy_requiresConfirmation() {
        val confirmRule = safeRule.copy(
            confirmationPolicy = AutomationConfirmationPolicy.ALWAYS_CONFIRM,
        )
        val result = AutomationSafetyValidator.validate(
            confirmRule,
            "account-1",
            normalContext,
            isExplicitUserConfirmed = false,
        )
        assertTrue(result is AutomationSafetyResult.RequiresConfirmation)
    }

    @Test
    fun conflictingActions_areBlocked() {
        val conflictRule = safeRule.copy(
            actions = listOf(
                AutomationAction(AutomationActionType.MARK_AS_READ),
                AutomationAction(AutomationActionType.MARK_AS_UNREAD),
            )
        )
        val result = AutomationSafetyValidator.validate(conflictRule, "account-1", normalContext)
        assertTrue(result is AutomationSafetyResult.Blocked)
        assertTrue((result as AutomationSafetyResult.Blocked).reason.contains("Conflicting"))
    }
}
