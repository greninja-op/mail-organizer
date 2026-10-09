package com.greninjaop.mailorganizer.core.priority

import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.core.classify.SignalStrength

/**
 * Priority domain models (Phase 9 — Priority & Action-Required Engine).
 *
 * Pipeline position:
 * ```
 * [MessageRecord] + [ClassificationRecord?] + sender signals
 *        ↓  (assembled at the domain use-case boundary)
 * PriorityInput
 *        ↓  DeterministicPriorityEngine (rule evaluation + scoring)
 * PriorityResult
 *        ↓  PrioritizeMessageUseCase (domain) → IntelligenceRepository
 * PriorityRecord (Phase 2 storage)
 * ```
 *
 * All types here are pure Kotlin (no Android imports) so this package can
 * move verbatim into a KMP shared module when the second platform arrives.
 *
 * Priority is INDEPENDENT from category (requirements.md): a newsletter can
 * be correctly classified yet low priority; a personal email can be
 * unclassified yet high priority. The engine consumes the classification as
 * one signal among several — it never re-derives the category.
 *
 * Determinism contract: [DeterministicPriorityEngine.prioritize] is a pure
 * function of ([PriorityInput], rules, [DeterministicPriorityEngine.VERSION]).
 * No randomness, no network, no device state. The timestamp is injected by
 * the caller so tests can fix it.
 */

/**
 * The engine's own priority levels — mirrors requirements.md ("independent
 * priority") and maps to the persistence enum
 * [com.greninjaop.mailorganizer.data.local.Priority] at the domain boundary
 * via `toPriority()` (same pattern as Phase 7's `toMailCategory`).
 */
enum class PriorityLevel {
    /** Noise: newsletters, promotions, spam — safe to skim or ignore. */
    LOW,

    /** The default: ordinary mail with no strong priority signal either way. */
    NORMAL,

    /** Deserves attention: action required, security, important personal mail. */
    HIGH,

    /** Rare and urgent: strong security alerts, critical action items. */
    CRITICAL,
}

/**
 * Normalized priority input (assembled by the domain use case from the
 * message, its classification, and sender signals).
 */
data class PriorityInput(
    /** Local message id (stable, account-namespaced by callers). */
    val messageId: String,
    /** Classification category, or null when the message is unclassified. */
    val category: ClassifierCategory? = null,
    /** Classification confidence, or null when unclassified. */
    val confidence: Confidence? = null,
    /** True when the sender crossed the recurring-sender threshold. */
    val isRecurringSender: Boolean = false,
    /** True when the message is still unread. */
    val unread: Boolean = true,
    /** Raw Gmail label ids (UNREAD, SPAM, CATEGORY_SOCIAL, …). */
    val labelIds: List<String> = emptyList(),
    /** True when the body/snippet carries an unsubscribe marker. */
    val hasUnsubscribeMarker: Boolean = false,
    /**
     * True when the sender looks automated/bulk (noreply, no-reply,
     * bounce, notifications@, …). A documented heuristic, not a fact.
     */
    val isBulkSender: Boolean = false,
)

/**
 * One fired priority signal, kept for explainability.
 *
 * [name] is a stable snake_case key (e.g. "category_action_required");
 * [detail] is human-readable ("Classified as Action Required").
 */
data class MatchedPrioritySignal(
    val name: String,
    val detail: String,
    val strength: SignalStrength,
)

/**
 * Structured priority result.
 *
 * Everything needed to explain, trace, and re-derive this decision is here —
 * raw email content is never required to explain a priority (same discipline
 * as Phase 7's classification explanations).
 */
data class PriorityResult(
    val priority: PriorityLevel,
    val signals: List<MatchedPrioritySignal>,
    /** Primary rule id (highest-weight firing rule), or null when none fired. */
    val ruleId: String?,
    /** All firing rule ids, highest weight first. */
    val firingRuleIds: List<String>,
    val version: Int,
    val computedAtEpochMs: Long,
    /**
     * Human-readable "why", e.g.
     * "High priority — Action required: subject mentions a payment due;
     * sender is a recurring contact." Never contains email bodies.
     */
    val explanation: String,
)
