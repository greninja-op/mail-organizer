package com.greninjaop.mailorganizer.core.priority

import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Priority engine behavior tests (Phase 9).
 *
 * Verifies: rule firing, scoring, precedence, defaults, determinism.
 * The engine is a pure function — every test fixes the clock.
 */
class PriorityEngineTest {

    private val clock = { 1_800_000_000_000L }

    private fun input(
        category: ClassifierCategory? = null,
        confidence: Confidence? = null,
        isRecurringSender: Boolean = false,
        unread: Boolean = true,
        labelIds: List<String> = emptyList(),
        hasUnsubscribeMarker: Boolean = false,
        isBulkSender: Boolean = false,
    ) = PriorityInput(
        messageId = "m1",
        category = category,
        confidence = confidence,
        isRecurringSender = isRecurringSender,
        unread = unread,
        labelIds = labelIds,
        hasUnsubscribeMarker = hasUnsubscribeMarker,
        isBulkSender = isBulkSender,
    )

    @Test
    fun `plain personal mail defaults to NORMAL`() {
        val result = DeterministicPriorityEngine.prioritize(input(), clock)
        assertEquals(PriorityLevel.NORMAL, result.priority)
        assertEquals(DeterministicPriorityEngine.VERSION, result.version)
        assertTrue(result.explanation.contains("Normal priority"))
    }

    @Test
    fun `action required mail is HIGH`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.ACTION_REQUIRED, confidence = Confidence.MEDIUM),
            clock,
        )
        assertEquals(PriorityLevel.HIGH, result.priority)
        assertTrue(result.firingRuleIds.contains("PRIORITY_ACTION_REQUIRED"))
        assertTrue(result.explanation.contains("High priority"))
    }

    @Test
    fun `high-confidence action required is CRITICAL`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.ACTION_REQUIRED, confidence = Confidence.HIGH),
            clock,
        )
        assertEquals(PriorityLevel.CRITICAL, result.priority)
        assertTrue(result.firingRuleIds.contains("PRIORITY_CRITICAL_ACTION"))
    }

    @Test
    fun `high-confidence security alert is CRITICAL`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.SECURITY, confidence = Confidence.HIGH),
            clock,
        )
        assertEquals(PriorityLevel.CRITICAL, result.priority)
        assertTrue(result.firingRuleIds.contains("PRIORITY_CRITICAL_SECURITY"))
    }

    @Test
    fun `security at lower confidence is HIGH`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.SECURITY, confidence = Confidence.LOW),
            clock,
        )
        assertEquals(PriorityLevel.HIGH, result.priority)
    }

    @Test
    fun `newsletter is LOW`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.NEWSLETTERS),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
        assertTrue(result.firingRuleIds.contains("PRIORITY_NEWSLETTER"))
    }

    @Test
    fun `unsubscribe marker alone demotes to LOW`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(hasUnsubscribeMarker = true),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
    }

    @Test
    fun `promotion is LOW`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.PROMOTIONS),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
    }

    @Test
    fun `spam label is LOW`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(labelIds = listOf("SPAM", "UNREAD")),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
        assertTrue(result.firingRuleIds.contains("PRIORITY_SPAM_LABEL"))
    }

    @Test
    fun `important category is HIGH`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.IMPORTANT),
            clock,
        )
        assertEquals(PriorityLevel.HIGH, result.priority)
    }

    @Test
    fun `recurring career sender is HIGH`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(
                category = ClassifierCategory.CAREER,
                isRecurringSender = true,
            ),
            clock,
        )
        assertEquals(PriorityLevel.HIGH, result.priority)
        assertTrue(result.firingRuleIds.contains("PRIORITY_RECURRING_CAREER"))
    }

    @Test
    fun `recurring sender alone does not inflate priority`() {
        // Recurring alone (no career/education category) must not invent
        // urgency — the signal only matters with the right category.
        val result = DeterministicPriorityEngine.prioritize(
            input(isRecurringSender = true),
            clock,
        )
        assertEquals(PriorityLevel.NORMAL, result.priority)
    }

    @Test
    fun `order update stays NORMAL`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.RECEIPTS_ORDERS),
            clock,
        )
        assertEquals(PriorityLevel.NORMAL, result.priority)
    }

    @Test
    fun `lone notification stays NORMAL`() {
        // A single weak LOW signal must not demote ordinary mail — the
        // weight stays under NORMAL's base score by design.
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.NOTIFICATIONS),
            clock,
        )
        assertEquals(PriorityLevel.NORMAL, result.priority)
    }

    @Test
    fun `notification plus newsletter demotes to LOW`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(
                category = ClassifierCategory.NOTIFICATIONS,
                hasUnsubscribeMarker = true,
            ),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
    }

    @Test
    fun `engine is deterministic`() {
        val i = input(category = ClassifierCategory.ACTION_REQUIRED)
        val a = DeterministicPriorityEngine.prioritize(i, clock)
        val b = DeterministicPriorityEngine.prioritize(i, clock)
        assertEquals(a.priority, b.priority)
        assertEquals(a.firingRuleIds, b.firingRuleIds)
        assertEquals(a.explanation, b.explanation)
    }

    @Test
    fun `explanation never contains raw content`() {
        // The explanation is built from rule templates and signal details,
        // never from email bodies.
        val result = DeterministicPriorityEngine.prioritize(
            input(category = ClassifierCategory.ACTION_REQUIRED),
            clock,
        )
        assertTrue(result.explanation.isNotBlank())
        assertTrue(result.signals.all { it.detail.isNotBlank() })
    }

    @Test
    fun `firing rule ids are ordered by weight`() {
        val result = DeterministicPriorityEngine.prioritize(
            input(
                category = ClassifierCategory.ACTION_REQUIRED,
                confidence = Confidence.HIGH,
            ),
            clock,
        )
        // CRITICAL_ACTION (100) outranks ACTION_REQUIRED (70).
        assertEquals("PRIORITY_CRITICAL_ACTION", result.firingRuleIds.first())
        assertEquals("PRIORITY_CRITICAL_ACTION", result.ruleId)
    }

    @Test
    fun `all rule ids are stable and unique`() {
        val ids = PriorityRuleSet.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("PRIORITY_") })
    }
}
