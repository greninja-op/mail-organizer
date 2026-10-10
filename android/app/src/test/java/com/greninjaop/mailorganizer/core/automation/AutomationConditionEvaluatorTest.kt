package com.greninjaop.mailorganizer.core.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AutomationConditionEvaluator (Phase 27 §16, §18, §19).
 */
class AutomationConditionEvaluatorTest {

    private val sampleContext = AutomationEvaluationContext(
        accountId = "acc-1",
        messageId = "msg-1",
        threadId = "th-1",
        fromAddress = "billing@aws.amazon.com",
        fromDomain = "amazon.com",
        companyId = "amazon",
        subject = "Your AWS Monthly Invoice #12345",
        category = "FINANCE",
        priority = "HIGH",
        isActionRequired = true,
        isUnread = true,
        isStarred = false,
        hasAttachment = true,
        hasUnsubscribe = false,
        gmailLabels = listOf("INBOX", "IMPORTANT", "FINANCE"),
        timestampEpochMs = 1700000000000L,
    )

    @Test
    fun emptyConditionGroup_evaluatesToTrue() {
        val group = AutomationConditionGroup(logic = ConditionGroupLogic.ALL, conditions = emptyList())
        assertTrue(AutomationConditionEvaluator.evaluateGroup(group, sampleContext))
    }

    @Test
    fun senderEmail_matching() {
        val condEquals = AutomationCondition(
            field = AutomationConditionField.SENDER_EMAIL,
            operator = AutomationOperator.EQUALS,
            value = "billing@aws.amazon.com",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(condEquals, sampleContext))

        val condNotEquals = AutomationCondition(
            field = AutomationConditionField.SENDER_EMAIL,
            operator = AutomationOperator.NOT_EQUALS,
            value = "other@domain.com",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(condNotEquals, sampleContext))

        val condContains = AutomationCondition(
            field = AutomationConditionField.SENDER_EMAIL,
            operator = AutomationOperator.CONTAINS,
            value = "aws.amazon",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(condContains, sampleContext))
    }

    @Test
    fun senderDomain_matching() {
        val condEquals = AutomationCondition(
            field = AutomationConditionField.SENDER_DOMAIN,
            operator = AutomationOperator.EQUALS,
            value = "amazon.com",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(condEquals, sampleContext))

        val condWrong = AutomationCondition(
            field = AutomationConditionField.SENDER_DOMAIN,
            operator = AutomationOperator.EQUALS,
            value = "google.com",
        )
        assertFalse(AutomationConditionEvaluator.evaluateCondition(condWrong, sampleContext))
    }

    @Test
    fun subject_matching_caseInsensitive() {
        val condContains = AutomationCondition(
            field = AutomationConditionField.SUBJECT,
            operator = AutomationOperator.CONTAINS,
            value = "invoice",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(condContains, sampleContext))

        val condNotContains = AutomationCondition(
            field = AutomationConditionField.SUBJECT,
            operator = AutomationOperator.NOT_CONTAINS,
            value = "urgent password reset",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(condNotContains, sampleContext))
    }

    @Test
    fun categoryAndPriority_matching() {
        val catCond = AutomationCondition(
            field = AutomationConditionField.CATEGORY,
            operator = AutomationOperator.EQUALS,
            value = "finance", // case-insensitive check
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(catCond, sampleContext))

        val priCond = AutomationCondition(
            field = AutomationConditionField.PRIORITY,
            operator = AutomationOperator.EQUALS,
            value = "HIGH",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(priCond, sampleContext))
    }

    @Test
    fun booleanFlags_matching() {
        val actionReq = AutomationCondition(
            field = AutomationConditionField.IS_ACTION_REQUIRED,
            operator = AutomationOperator.EQUALS,
            value = "true",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(actionReq, sampleContext))

        val isStarred = AutomationCondition(
            field = AutomationConditionField.IS_STARRED,
            operator = AutomationOperator.EQUALS,
            value = "true",
        )
        assertFalse(AutomationConditionEvaluator.evaluateCondition(isStarred, sampleContext))

        val hasAttachment = AutomationCondition(
            field = AutomationConditionField.HAS_ATTACHMENT,
            operator = AutomationOperator.EQUALS,
            value = "true",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(hasAttachment, sampleContext))
    }

    @Test
    fun gmailLabel_matching() {
        val labelCond = AutomationCondition(
            field = AutomationConditionField.GMAIL_LABEL,
            operator = AutomationOperator.CONTAINS,
            value = "inbox",
        )
        assertTrue(AutomationConditionEvaluator.evaluateCondition(labelCond, sampleContext))

        val labelNotPresent = AutomationCondition(
            field = AutomationConditionField.GMAIL_LABEL,
            operator = AutomationOperator.EQUALS,
            value = "SPAM",
        )
        assertFalse(AutomationConditionEvaluator.evaluateCondition(labelNotPresent, sampleContext))
    }

    @Test
    fun conditionGroupLogic_all_any_none() {
        val c1 = AutomationCondition(AutomationConditionField.SENDER_DOMAIN, AutomationOperator.EQUALS, "amazon.com")
        val c2 = AutomationCondition(AutomationConditionField.CATEGORY, AutomationOperator.EQUALS, "FINANCE")
        val c3 = AutomationCondition(AutomationConditionField.PRIORITY, AutomationOperator.EQUALS, "LOW")

        // ALL: c1 (true) + c2 (true) -> true
        val allTrue = AutomationConditionGroup(ConditionGroupLogic.ALL, listOf(c1, c2))
        assertTrue(AutomationConditionEvaluator.evaluateGroup(allTrue, sampleContext))

        // ALL: c1 (true) + c3 (false) -> false
        val allFalse = AutomationConditionGroup(ConditionGroupLogic.ALL, listOf(c1, c3))
        assertFalse(AutomationConditionEvaluator.evaluateGroup(allFalse, sampleContext))

        // ANY: c1 (true) + c3 (false) -> true
        val anyTrue = AutomationConditionGroup(ConditionGroupLogic.ANY, listOf(c1, c3))
        assertTrue(AutomationConditionEvaluator.evaluateGroup(anyTrue, sampleContext))

        // NONE: c3 (false) -> true
        val noneTrue = AutomationConditionGroup(ConditionGroupLogic.NONE, listOf(c3))
        assertTrue(AutomationConditionEvaluator.evaluateGroup(noneTrue, sampleContext))

        // NONE: c1 (true) -> false
        val noneFalse = AutomationConditionGroup(ConditionGroupLogic.NONE, listOf(c1, c3))
        assertFalse(AutomationConditionEvaluator.evaluateGroup(noneFalse, sampleContext))
    }
}
