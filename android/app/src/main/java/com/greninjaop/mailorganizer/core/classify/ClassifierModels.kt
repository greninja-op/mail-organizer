package com.greninjaop.mailorganizer.core.classify

/**
 * Classification domain models (Phase 7 — Deterministic Classification Engine).
 *
 * Pipeline position:
 * ```
 * [EmailMessage] (Phase 5, normalized)
 *        ↓  ClassificationInput.fromEmailMessage / fromMessageRecord
 * ClassificationInput
 *        ↓  SignalExtractor
 * ExtractedSignals
 *        ↓  DeterministicClassifier (rule evaluation + scoring + resolution)
 * ClassificationResult
 *        ↓  ClassifyMessageUseCase (domain) → IntelligenceRepository
 * ClassificationRecord (Phase 2 storage)
 * ```
 *
 * All types here are pure Kotlin (no Android imports) so this package can move
 * verbatim into a KMP shared module when the second platform arrives.
 *
 * Determinism contract: [DeterministicClassifier.classify] is a pure function
 * of ([ClassificationInput], rules, [DeterministicClassifier.VERSION]).
 * No randomness, no network, no device state. The timestamp is injected by the
 * caller so tests can fix it.
 */

/**
 * The classifier's own category set — mirrors requirements.md §Categories.
 * Mapped to the persistence enum [MailCategory] at the use-case boundary via
 * [toMailCategory] (same pattern as Phase 5's EmailMessage → MessageRecord).
 */
enum class ClassifierCategory {
    ACTION_REQUIRED,
    IMPORTANT,
    CAREER,
    EDUCATION,
    RECEIPTS_ORDERS,
    SECURITY,
    NOTIFICATIONS,
    NEWSLETTERS,
    PROMOTIONS,
    LOW_VALUE,
    /** No rule fired with enough evidence; never a forced guess. */
    UNCLASSIFIED,
}

/**
 * Deterministic rule strength — NOT a statistical probability.
 *
 * - HIGH: a strong, specific rule fired (e.g. OTP pattern from a security
 *   sender) with a clear score margin over the runner-up.
 * - MEDIUM: solid evidence from one or more rules, but the match is less
 *   specific or the margin is narrow.
 * - LOW: only weak signals fired, or the winner barely beat the runner-up.
 *   The category is a best-effort hint; callers should treat it as uncertain.
 */
enum class Confidence {
    HIGH,
    MEDIUM,
    LOW,
}

/** Relative weight of a single matched signal inside rule scoring. */
enum class SignalStrength {
    STRONG,
    MEDIUM,
    WEAK,
}

/**
 * One fired signal, kept for explainability (phase §16).
 *
 * [name] is a stable snake_case key (e.g. "subject_otp_phrase");
 * [detail] is human-readable ("Subject contains \"verification code\"").
 */
data class MatchedSignal(
    val name: String,
    val detail: String,
    val strength: SignalStrength,
)

/**
 * Normalized classifier input (phase §8).
 *
 * Built from [com.greninjaop.mailorganizer.core.email.EmailMessage] (preferred)
 * or from a [com.greninjaop.mailorganizer.data.local.MessageRecord] at the
 * use-case boundary. Raw Gmail API models must never reach the classifier
 * (phase §9).
 */
data class ClassificationInput(
    /** Local message id (stable, account-namespaced by callers). */
    val messageId: String,
    val fromAddress: String,
    val fromName: String?,
    val subject: String,
    /** Plain-text body; null when unavailable. Bounded by the extractor. */
    val bodyText: String?,
    /** Raw Gmail label ids (UNREAD, CATEGORY_SOCIAL, SPAM, …). */
    val labelIds: List<String> = emptyList(),
    val attachmentFilenames: List<String> = emptyList(),
    val attachmentMimeTypes: List<String> = emptyList(),
    val snippet: String = "",
)

/**
 * Structured classification result (phase §14).
 *
 * Everything needed to explain, trace, and re-derive this decision is here —
 * raw email content is never required to explain a classification (phase §62).
 */
data class ClassificationResult(
    val category: ClassifierCategory,
    val confidence: Confidence,
    val matchedSignals: List<MatchedSignal>,
    /** Primary rule id (highest-weight firing rule), or null when none fired. */
    val ruleId: String?,
    /** All firing rule ids, highest weight first. */
    val firingRuleIds: List<String>,
    val classifierVersion: Int,
    val classifiedAtEpochMs: Long,
    /**
     * Human-readable "why" (phase §16), e.g.
     * "Security — Subject contains \"verification code\"; sender domain matches
     * a known account-security sender." Never contains email bodies.
     */
    val explanation: String,
)
