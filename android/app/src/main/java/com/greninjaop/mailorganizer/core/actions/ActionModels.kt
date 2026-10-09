package com.greninjaop.mailorganizer.core.actions

import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Action-engine domain models (Phase 14 — Action Cards & Action Engine).
 *
 * Pipeline position:
 * ```
 * Intelligence results (classification, priority, rules/corrections,
 *                       temporal extraction, company/sender)
 *        ↓  (assembled at the domain use-case boundary)
 * ActionInput
 *        ↓  ActionCandidateGenerator (deterministic rules)
 * ActionCandidate
 *        ↓  ActionSafety.validate
 * Safe / degraded / blocked
 *        ↓  domain/actions (persist → ActionItemRecord)
 * Action Card (UI) → user review → confirmation → executor (future)
 * ```
 *
 * All types here are pure Kotlin (no Android imports) so this package can
 * move verbatim into a KMP shared module when the second platform arrives.
 *
 * Design discipline (same as Phases 7/9/12/13):
 * - Deterministic: [ActionCandidateGenerator.generate] is a pure function
 *   of ([ActionInput]) and [ActionCandidateGenerator.VERSION]. No
 *   randomness, no network, no device state.
 * - Honest: the engine never invents precision — missing required
 *   information degrades a candidate to a review-style card instead of an
 *   executable action; low confidence caps urgency instead of faking it.
 * - Explainable: every candidate carries structured [ActionSignal]s and a
 *   human-readable explanation recorded at generation time — the UI never
 *   needs raw email content to explain a card.
 * - Safe: [ActionSafety] is a separate layer from generation (phase §5, §10).
 *   No candidate ever performs an external side effect; external effects
 *   are classified, require explicit user confirmation, and have no
 *   executor in Phase 14 (phase §31–§33).
 */

/**
 * Controlled confidence representation (phase §16). Documented as *rule
 * strength*, not a probability — a LOW candidate is still a real
 * intelligence match, never a guess.
 */
enum class ActionConfidence {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN,
}

/**
 * Action urgency — deliberately a separate type from email [Priority]
 * (phase §17). Derived from email priority *and* temporal urgency; a
 * low-priority newsletter never becomes CRITICAL no matter what.
 */
enum class ActionUrgency {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW,
}

/** Where an action candidate came from (phase §10 — source validation). */
enum class ActionSource {
    CLASSIFICATION,
    TEMPORAL_EXTRACTION,
    PRIORITY,
    USER_RULE,
    USER_CORRECTION,
    MESSAGE_SIGNALS,
}

/**
 * Action lifecycle (phase §24). Not all states are reachable in Phase 14:
 * EXECUTING/FAILED/COMPLETED-via-executor require a real external
 * executor (later phases). The states exist so the architecture — and the
 * stored rows — support them without a future migration.
 */
enum class ActionStatus {
    SUGGESTED,
    REVIEWED,
    CONFIRMED,
    EXECUTING,
    COMPLETED,
    DISMISSED,
    FAILED,
    EXPIRED,
    CANCELLED,
}

/**
 * External side-effect classification (phase §9). In Phase 14 no external
 * executor exists, so CALENDAR/TASKS/GMAIL_WRITE candidates are honest
 * *proposals*: the UI must present them as "not connected", never as done.
 */
enum class ExternalEffect {
    /** Purely internal suggestion (review the email, review details). */
    NONE,

    /** Proposed Google Calendar write — Phase 15's scope, not connected. */
    CALENDAR,

    /** Proposed Google Tasks write — Phase 16's scope, not connected. */
    TASKS,

    /** Proposed Gmail write — Phase 22's scope, not connected. */
    GMAIL_WRITE,
}

/**
 * One structured reason behind a candidate, kept for explainability.
 *
 * [name] is a stable snake_case key (e.g. "interview_detected");
 * [detail] is human-readable ("'interview' appears near a date").
 */
data class ActionSignal(
    val name: String,
    val detail: String,
)

/**
 * A temporal item as consumed by the action engine — a decoded view over
 * Phase 13's [com.greninjaop.mailorganizer.data.local.ExtractedItemRecord].
 * Assembled by the domain use case so core stays data-free.
 */
data class TemporalRef(
    /** Stable row id of the extracted item (for the dedup target key). */
    val itemId: Long,
    val itemTypeName: String,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val isDateOnly: Boolean,
    val timezoneId: String,
    val location: String?,
    val meetingUrl: String?,
    /** UPCOMING / PAST / UNKNOWN as a stable name. */
    val statusName: String,
    /** HIGH / MEDIUM / LOW / UNKNOWN as a stable name. */
    val confidenceName: String,
    val signalNames: List<String>,
)

/**
 * Normalized generator input, assembled by the domain use case from the
 * effective intelligence results (phase §29: the *effective* action-required
 * result after rules/corrections — never the raw classifier output alone).
 */
data class ActionInput(
    val messageId: String,
    val threadId: String,
    val accountId: String,
    val subject: String,
    val senderName: String?,
    val senderAddress: String,
    val timestampEpochMs: Long,
    val unread: Boolean,
    /** Effective category after rules/corrections. */
    val category: MailCategory?,
    /** ClassificationSource.name behind [category] (for ActionSource mapping). */
    val categorySourceName: String?,
    /** Effective priority after rules/corrections. */
    val priority: Priority?,
    /** Effective action-required flag (classification + user intent). */
    val actionRequired: Boolean,
    /** Decoded temporal items for this message (may be empty). */
    val temporalItems: List<TemporalRef>,
)

/**
 * One generated action suggestion.
 *
 * [id] is the deterministic dedup key
 * `"$messageId|$actionType|$targetKey"` (phase §22) — re-running the
 * generator never creates a second row for the same underlying action.
 * [targetKey] identifies *what* the action is about (a temporal item id or
 * "general") so thread-level dedup can compare across messages.
 */
data class ActionCandidate(
    val id: String,
    val accountId: String,
    val messageId: String,
    val threadId: String,
    val actionType: ActionType,
    /** Short human title, e.g. "Interview tomorrow". Never a body dump. */
    val title: String,
    /** One or two sentences on what is being suggested. */
    val description: String,
    val urgency: ActionUrgency,
    val source: ActionSource,
    val confidence: ActionConfidence,
    val explanation: String,
    val signals: List<ActionSignal>,
    val externalEffect: ExternalEffect,
    /**
     * Information the candidate is missing that would make it executable
     * (e.g. "start time"). Empty when nothing material is missing. The
     * safety layer turns a non-empty list into a degraded review-style card
     * instead of an executable action (phase §10).
     */
    val missingInfo: List<String>,
    val dueDateEpochMs: Long?,
    /** Distinguishes candidates about different targets of one message. */
    val targetKey: String,
    val version: Int,
)

/**
 * Safety-layer verdict for one candidate (phase §10). Separate from
 * generation so policy can evolve without touching the rules.
 */
sealed interface SafetyVerdict {
    /** Present the candidate as generated. */
    data class Safe(val candidate: ActionCandidate) : SafetyVerdict

    /**
     * Present a degraded review-style card instead: the title/description
     * are replaced with honest review copy; nothing is presented as
     * executable. [reason] is recorded, never shown as an error.
     */
    data class Degraded(
        val candidate: ActionCandidate,
        val degradedTitle: String,
        val degradedDescription: String,
        val reason: String,
    ) : SafetyVerdict

    /** Do not present this candidate at all. [reason] is logged, not shown. */
    data class Blocked(val reason: String) : SafetyVerdict
}
