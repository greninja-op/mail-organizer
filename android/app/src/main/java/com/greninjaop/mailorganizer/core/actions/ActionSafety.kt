package com.greninjaop.mailorganizer.core.actions

import com.greninjaop.mailorganizer.data.local.ActionType

/**
 * Action safety validation (Phase 14, phase §10–§11).
 *
 * A separate layer from [ActionCandidateGenerator] (phase §5): generation
 * decides *what could the user reasonably do next*, safety decides *what
 * may be presented and how*. [validate] is a pure function — deterministic
 * and independently testable.
 *
 * Safety policy:
 * - Identity: a candidate without account/message/thread identity is
 *   [SafetyVerdict.Blocked] — it can never be shown, persisted, or
 *   confirmed.
 * - Required information: when material information is missing for the
 *   proposed action (e.g. a meeting with no start time), the candidate is
 *   [SafetyVerdict.Degraded] to an honest review-style card. The system
 *   never fabricates the missing piece (phase §10).
 * - Confidence: LOW/UNKNOWN confidence caps urgency at NORMAL — a weak
 *   signal is never presented as urgent (phase §16).
 * - External effects: any non-[ExternalEffect.NONE] candidate requires
 *   explicit user confirmation before anything happens (phase §9, §25).
 *   Whether an executor exists is resolved by the domain layer, not here.
 */
object ActionSafety {

    fun validate(candidate: ActionCandidate): SafetyVerdict {
        if (candidate.accountId.isBlank() ||
            candidate.messageId.isBlank() ||
            candidate.threadId.isBlank()
        ) {
            return SafetyVerdict.Blocked("missing identity (account/message/thread)")
        }

        // Required-information check per action type.
        val missing = candidate.missingInfo
        if (missing.isNotEmpty()) {
            val what = missing.joinToString(", ")
            return SafetyVerdict.Degraded(
                candidate = candidate,
                degradedTitle = "Review: ${candidate.title}",
                degradedDescription = "Some details are missing ($what), so " +
                    "this is shown for review only. Open the email for " +
                    "the full picture.",
                reason = "missing required info: $what",
            )
        }

        // Confidence gating: weak signals are never urgent.
        val urgency = when (candidate.confidence) {
            ActionConfidence.LOW, ActionConfidence.UNKNOWN ->
                minUrgency(candidate.urgency, ActionUrgency.NORMAL)
            else -> candidate.urgency
        }

        // Meeting proposals without a due date cannot be scheduled —
        // degrade instead of inventing a time.
        if (candidate.actionType == ActionType.MEETING && candidate.dueDateEpochMs == null) {
            return SafetyVerdict.Degraded(
                candidate = candidate,
                degradedTitle = "Review meeting details",
                degradedDescription = "A meeting was detected but no date " +
                    "could be determined. Open the email for details.",
                reason = "meeting candidate without due date",
            )
        }

        val safe = if (urgency == candidate.urgency) {
            candidate
        } else {
            candidate.copy(urgency = urgency)
        }
        return SafetyVerdict.Safe(safe)
    }

    private fun minUrgency(a: ActionUrgency, b: ActionUrgency): ActionUrgency =
        if (a.ordinal > b.ordinal) a else b
}
