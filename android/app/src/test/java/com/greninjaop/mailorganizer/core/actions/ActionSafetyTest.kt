package com.greninjaop.mailorganizer.core.actions

import com.greninjaop.mailorganizer.data.local.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Safety-layer tests (Phase 14, phase §10–§11, §16). */
class ActionSafetyTest {

    private fun candidate(
        actionType: ActionType = ActionType.MEETING,
        confidence: ActionConfidence = ActionConfidence.HIGH,
        urgency: ActionUrgency = ActionUrgency.HIGH,
        missingInfo: List<String> = emptyList(),
        dueDate: Long? = 1_800_000_000_000L,
        accountId: String = "a1",
        externalEffect: ExternalEffect = ExternalEffect.CALENDAR,
    ) = ActionCandidate(
        id = "m1|${actionType.name}|temporal:7",
        accountId = accountId,
        messageId = "m1",
        threadId = "t1",
        actionType = actionType,
        title = "Meeting: Sync",
        description = "A meeting is scheduled.",
        urgency = urgency,
        source = ActionSource.TEMPORAL_EXTRACTION,
        confidence = confidence,
        explanation = "Suggested because: ...",
        signals = listOf(ActionSignal("temporal_meeting", "detail")),
        externalEffect = externalEffect,
        missingInfo = missingInfo,
        dueDateEpochMs = dueDate,
        targetKey = "temporal:7",
        version = 1,
    )

    @Test
    fun `well-formed candidate is safe`() {
        val verdict = ActionSafety.validate(candidate())
        assertTrue(verdict is SafetyVerdict.Safe)
        assertEquals(ActionUrgency.HIGH, (verdict as SafetyVerdict.Safe).candidate.urgency)
    }

    @Test
    fun `missing identity is blocked`() {
        val verdict = ActionSafety.validate(candidate(accountId = ""))
        assertTrue(verdict is SafetyVerdict.Blocked)
    }

    @Test
    fun `missing required info degrades to review card`() {
        val verdict = ActionSafety.validate(candidate(missingInfo = listOf("start time")))
        assertTrue(verdict is SafetyVerdict.Degraded)
        val degraded = verdict as SafetyVerdict.Degraded
        assertTrue(degraded.degradedTitle.startsWith("Review"))
        assertTrue(degraded.degradedDescription.contains("start time"))
        // The original candidate is preserved for audit.
        assertEquals(listOf("start time"), degraded.candidate.missingInfo)
    }

    @Test
    fun `low confidence caps urgency at normal`() {
        val verdict = ActionSafety.validate(
            candidate(confidence = ActionConfidence.LOW, urgency = ActionUrgency.CRITICAL),
        )
        assertTrue(verdict is SafetyVerdict.Safe)
        assertEquals(
            ActionUrgency.NORMAL,
            (verdict as SafetyVerdict.Safe).candidate.urgency,
        )
    }

    @Test
    fun `unknown confidence caps urgency at normal`() {
        val verdict = ActionSafety.validate(
            candidate(confidence = ActionConfidence.UNKNOWN, urgency = ActionUrgency.HIGH),
        )
        assertEquals(
            ActionUrgency.NORMAL,
            (verdict as SafetyVerdict.Safe).candidate.urgency,
        )
    }

    @Test
    fun `meeting without due date degrades instead of inventing time`() {
        val verdict = ActionSafety.validate(
            candidate(actionType = ActionType.MEETING, dueDate = null),
        )
        assertTrue(verdict is SafetyVerdict.Degraded)
        assertTrue(
            (verdict as SafetyVerdict.Degraded).degradedDescription
                .contains("no date could be determined"),
        )
    }

    @Test
    fun `high confidence keeps urgency`() {
        val verdict = ActionSafety.validate(
            candidate(confidence = ActionConfidence.HIGH, urgency = ActionUrgency.CRITICAL),
        )
        assertEquals(
            ActionUrgency.CRITICAL,
            (verdict as SafetyVerdict.Safe).candidate.urgency,
        )
    }
}
