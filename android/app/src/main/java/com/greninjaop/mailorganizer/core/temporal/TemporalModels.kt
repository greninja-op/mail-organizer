package com.greninjaop.mailorganizer.core.temporal

/**
 * Temporal-intelligence domain models (Phase 13 — Meeting & Deadline Extraction).
 *
 * Pipeline position:
 * ```
 * [MessageRecord] (subject, snippet, bodyText, internalDate)
 *        ↓  (assembled at the domain use-case boundary)
 * ExtractionInput
 *        ↓  DeterministicTemporalExtractor (pattern scan → candidates
 *             → type resolution → validation → ranking → explanation)
 * ExtractionResult
 *        ↓  ExtractTemporalUseCase (domain) → IntelligenceRepository
 * ExtractedItemRecord (Phase 2 storage; temporal fields in payload JSON)
 * ```
 *
 * All types here are pure Kotlin (no Android imports) so this package can
 * move verbatim into a KMP shared module when the second platform arrives.
 *
 * Design discipline (same as Phases 7/9/12):
 * - Deterministic: [DeterministicTemporalExtractor.extract] is a pure
 *   function of ([ExtractionInput], patterns, [DeterministicTemporalExtractor.VERSION]).
 *   No randomness, no network, no device state. The reference time is the
 *   message's received timestamp, injected by the caller — never "now".
 * - Honest: ambiguous input degrades to LOW confidence or is skipped; the
 *   extractor never manufactures precision (no invented end times, no
 *   guessed timezones, no assumed meeting durations).
 * - Explainable: every item carries matched signals and a human-readable
 *   "why" recorded at extraction time — the UI never needs raw email
 *   content to explain an item.
 */

/**
 * Controlled temporal entity types (phase §8). Kept small on purpose —
 * each type has a distinct semantic role in the UI and in future rules.
 */
enum class TemporalItemType {
    /** Explicit deadline language near a date ("deadline is 18 Oct"). */
    DEADLINE,

    /** Generic meeting/event language near a date/time. */
    MEETING,

    /** Explicit appointment language. */
    APPOINTMENT,

    /** Generic event language without meeting framing. */
    EVENT,

    /** Explicit interview language near a date/time. */
    INTERVIEW,

    /** Submission/assignment language + deadline language. */
    SUBMISSION_DEADLINE,

    /** Application/admissions language + deadline language. */
    APPLICATION_DEADLINE,

    /** Payment/invoice/bill language + deadline language. */
    PAYMENT_DEADLINE,

    /** Registration/enrollment language + deadline language. */
    REGISTRATION_DEADLINE,

    /** Reminder language near a date ("reminder: renew by Friday"). */
    REMINDER_DATE,

    /** A bare date with no semantic trigger and no time component. */
    DATE_ONLY,

    /** A time expression resolved against the reference date. */
    TIME_ONLY,

    /** Date + time with no strong semantic trigger. */
    DATE_TIME,

    /** An explicit range ("October 10–12"); start/end both known. */
    DATE_RANGE,
}

/**
 * Extraction certainty — documented as *extraction strength*, not a
 * probability. A LOW item is one the patterns matched weakly (ambiguous
 * format, distant trigger); it is still a real textual match, never a guess.
 */
enum class TemporalConfidence {
    HIGH,
    MEDIUM,
    LOW,
    /** Genuinely unresolvable — the extractor skips these (never invents). */
    UNKNOWN,
}

/** Whether the extracted instant is past, upcoming, or indeterminate. */
enum class TemporalStatus {
    UPCOMING,
    PAST,
    UNKNOWN,
}

/**
 * Where the timezone came from. Deterministic hierarchy (phase §17):
 * explicit timezone in the email → sender/event context → app fallback →
 * unknown. The source is always stored; a fallback is never presented as
 * explicit.
 */
enum class TimezoneSource {
    EXPLICIT_IN_EMAIL,
    APP_FALLBACK,
    UNKNOWN,
}

/**
 * One matched extraction signal, kept for explainability.
 *
 * [name] is a stable snake_case key (e.g. "deadline_vocabulary");
 * [detail] is human-readable ("'deadline' appears near the date").
 */
data class TemporalSignal(
    val name: String,
    val detail: String,
)

/**
 * Normalized extraction input, assembled by the domain use case.
 *
 * [referenceEpochMs] is the message's received timestamp (phase §12) —
 * relative expressions ("tomorrow", "next Monday") resolve against it, so
 * extraction is stable and testable regardless of when it runs.
 */
data class ExtractionInput(
    /** Local message id (stable, account-namespaced by callers). */
    val messageId: String,
    val threadId: String,
    val subject: String,
    val snippet: String?,
    /** May be null when only headers/snippet have synced. */
    val bodyText: String?,
    /** Message received timestamp — the reference time for relatives. */
    val referenceEpochMs: Long,
    /** IANA zone id used only when the email states no timezone. */
    val fallbackZoneId: String = "UTC",
)

/**
 * One structured temporal item.
 *
 * Everything needed to explain, trace, and re-derive this item is here —
 * raw email content is never required to explain it.
 */
data class ExtractedTemporal(
    val type: TemporalItemType,
    /** Short human title, e.g. "Application deadline". Never a body dump. */
    val title: String,
    /** Start instant, epoch millis. Always resolved — never invented. */
    val startEpochMs: Long,
    /** End instant, or null when not explicitly stated (never assumed). */
    val endEpochMs: Long?,
    /** True when no time component was found (date granularity only). */
    val isDateOnly: Boolean,
    /** IANA zone id (or fixed-offset id) the instant was resolved in. */
    val timezoneId: String,
    val timezoneSource: TimezoneSource,
    /** Explicitly stated location, or null. Bounded length. */
    val location: String?,
    /** Meeting URL, stored only — never fetched or validated. */
    val meetingUrl: String?,
    val status: TemporalStatus,
    val confidence: TemporalConfidence,
    val signals: List<TemporalSignal>,
    val version: Int,
    val extractedAtEpochMs: Long,
    /**
     * Human-readable "why", e.g.
     * "Deadline — 'deadline' appears near the date; date is explicitly
     * stated as 18 October 2026." Never contains email bodies.
     */
    val explanation: String,
)

/** The extractor's output for one message. */
data class ExtractionResult(
    val items: List<ExtractedTemporal>,
    val version: Int,
    val extractedAtEpochMs: Long,
)
