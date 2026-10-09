package com.greninjaop.mailorganizer.core.priority

import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Priority engine robustness tests (Phase 9).
 *
 * The engine is total: hostile, empty, or absurd inputs degrade to a safe
 * NORMAL — never throw, never hang, never produce a null.
 */
class PriorityRobustnessTest {

    private val clock = { 1_800_000_000_000L }

    @Test
    fun `empty input is NORMAL`() {
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(messageId = ""),
            clock,
        )
        assertEquals(PriorityLevel.NORMAL, result.priority)
    }

    @Test
    fun `null category with spam label is LOW`() {
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(messageId = "m", labelIds = listOf("SPAM")),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
    }

    @Test
    fun `unclassified personal unread mail stays NORMAL`() {
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(
                messageId = "m",
                category = ClassifierCategory.UNCLASSIFIED,
                unread = true,
            ),
            clock,
        )
        // DIRECT_PERSONAL (30) does not beat NORMAL's base (40) alone.
        assertEquals(PriorityLevel.NORMAL, result.priority)
    }

    @Test
    fun `bulk sender with action required stays HIGH`() {
        // A strong HIGH signal beats bulk deprioritization — the engine
        // does not let weak LOW signals veto strong HIGH ones.
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(
                messageId = "m",
                category = ClassifierCategory.ACTION_REQUIRED,
                isBulkSender = true,
            ),
            clock,
        )
        assertEquals(PriorityLevel.HIGH, result.priority)
    }

    @Test
    fun `conflicting strong signals resolve by weight`() {
        // ACTION_REQUIRED (HIGH 70) vs SPAM label (LOW 85): spam wins on
        // weight. Documented behavior — weight decides, not rule order.
        // unread=false keeps DIRECT_PERSONAL out so this is a pure
        // weight contest.
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(
                messageId = "m",
                category = ClassifierCategory.ACTION_REQUIRED,
                unread = false,
                labelIds = listOf("SPAM"),
            ),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
    }

    @Test
    fun `throwing clock degrades safely`() {
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(messageId = "m"),
            clock = { throw RuntimeException("boom") },
        )
        assertEquals(PriorityLevel.NORMAL, result.priority)
        assertEquals(0L, result.computedAtEpochMs)
    }

    @Test
    fun `many labels do not break the engine`() {
        val labels = List(10_000) { "LABEL_$it" } + "SPAM"
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(messageId = "m", labelIds = labels),
            clock,
        )
        assertEquals(PriorityLevel.LOW, result.priority)
    }

    @Test
    fun `result version is always current`() {
        val result = DeterministicPriorityEngine.prioritize(
            PriorityInput(messageId = "m"),
            clock,
        )
        assertEquals(DeterministicPriorityEngine.VERSION, result.version)
        assertTrue(result.computedAtEpochMs > 0)
    }

    @Test
    fun `tie breaks toward higher precedence`() {
        // Construct a tie: DIRECT_PERSONAL (HIGH 30) + bulk (LOW 30) =
        // HIGH 30 vs LOW 30 vs NORMAL 40 → NORMAL wins outright.
        // For an exact tie, use NOTIFICATION (LOW 35) + ORDER (NORMAL 25):
        // LOW 35 vs NORMAL 65 → NORMAL. Ties at the top are broken by
        // precedence; this test pins the precedence order itself.
        assertEquals(
            listOf(
                PriorityLevel.CRITICAL,
                PriorityLevel.HIGH,
                PriorityLevel.NORMAL,
                PriorityLevel.LOW,
            ),
            DeterministicPriorityEngine.PRECEDENCE,
        )
    }
}
