package com.greninjaop.mailorganizer.core.temporal

/**
 * Bounded, total regex patterns for temporal extraction (Phase 13).
 *
 * Every pattern is anchored to realistic email phrasing and compiled once.
 * Matching is always bounded (input is capped by the extractor); no pattern
 * can catastrophically backtrack on adversarial input — quantified groups
 * are kept simple and alternations are ordered longest-first.
 */
internal object TemporalPatterns {

    // ------------------------------------------------------------------
    // Date vocabulary
    // ------------------------------------------------------------------

    private const val MONTHS =
        "january|february|march|april|may|june|july|august|september|october|november|december"
    private const val MONTHS_ABBR =
        "jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec"

    /** "18 October 2026", "18 Oct 2026", "18 October, 2026". */
    val DAY_MONTH_YEAR = Regex(
        "\\b([12]?[0-9]|3[01])\\s+($MONTHS|$MONTHS_ABBR)\\b[,.]?\\s+((?:19|20)\\d{2})",
        RegexOption.IGNORE_CASE,
    )

    /** "October 18, 2026", "Oct 18 2026". */
    val MONTH_DAY_YEAR = Regex(
        "\\b($MONTHS|$MONTHS_ABBR)\\b\\s+([12]?[0-9]|3[01])(?:st|nd|rd|th)?[,.]?\\s+((?:19|20)\\d{2})",
        RegexOption.IGNORE_CASE,
    )

    /** "18 October", "Oct 18" — year resolved from reference time. */
    val DAY_MONTH = Regex(
        "\\b([12]?[0-9]|3[01])(?!\\d)\\s+($MONTHS|$MONTHS_ABBR)\\b(?!\\s+(?:19|20)\\d{2})",
        RegexOption.IGNORE_CASE,
    )

    /** "October 18", "Oct 18th" — year resolved from reference time. */
    val MONTH_DAY = Regex(
        "\\b($MONTHS|$MONTHS_ABBR)\\b\\s+([12]?[0-9]|3[01])(?!\\d)(?:st|nd|rd|th)?(?!\\s*,?\\s*(?:19|20)\\d{2})",
        RegexOption.IGNORE_CASE,
    )

    /** ISO "2026-10-18" / "2026/10/18". Unambiguous by construction. */
    val ISO_DATE = Regex("\\b((?:19|20)\\d{2})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])\\b")

    /**
     * Numeric "18/10/2026", "18-10-2026", "10/18/2026".
     * Ambiguity rule (phase §11): a component > 12 disambiguates; when both
     * are ≤ 12 the format is genuinely ambiguous → resolved as day-first
     * (documented app-locale default) with LOW confidence.
     */
    val NUMERIC_DATE = Regex(
        "\\b(0?[1-9]|[12][0-9]|3[01])[/.-](0?[1-9]|1[0-2])[/.-]((?:19|20)?\\d{2})\\b"
    )

    /** "October 10–12", "10 to 12 October", "10/10/2026 - 12/10/2026". */
    val DATE_RANGE_MONTH_SPAN = Regex(
        "\\b($MONTHS|$MONTHS_ABBR)\\b\\s+([12]?[0-9]|3[01])\\s*[–—-]\\s*([12]?[0-9]|3[01])\\b",
        RegexOption.IGNORE_CASE,
    )
    val DATE_RANGE_DAY_SPAN = Regex(
        "\\b([12]?[0-9]|3[01])\\s+(?:to|-)\\s+([12]?[0-9]|3[01])\\s+($MONTHS|$MONTHS_ABBR)\\b",
        RegexOption.IGNORE_CASE,
    )

    // ------------------------------------------------------------------
    // Relative dates (resolved against the reference timestamp, phase §12)
    // ------------------------------------------------------------------

    /** "tomorrow", "today", "tonight", "yesterday", "day after tomorrow". */
    val RELATIVE_DAY = Regex(
        "\\b(today|tonight|tomorrow|yesterday|day after tomorrow)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** "next Monday", "this Friday", "last Tuesday". */
    val RELATIVE_WEEKDAY = Regex(
        "\\b((?:next|this|last)\\s+)(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** "in 3 days", "in 2 weeks", "within 48 hours", "two weeks from now". */
    val RELATIVE_OFFSET = Regex(
        "\\b(?:in\\s+(\\d+)\\s+(day|days|week|weeks|month|months)|" +
            "within\\s+(\\d+)\\s+(hours?|days?)|" +
            "(one|two|three|four)\\s+weeks?\\s+from\\s+now)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** "next week" → Monday of next week (date-only, LOW). */
    val RELATIVE_WEEK = Regex("\\bnext week\\b", RegexOption.IGNORE_CASE)

    // ------------------------------------------------------------------
    // Times (phase §16)
    // ------------------------------------------------------------------

    /** "10:30 AM", "10:30AM", "10:30 a.m.", "10.30 AM", "22:30", "10 AM". */
    val TIME = Regex(
        "\\b((?:[01]?[0-9]|2[0-3])[:.]([0-5][0-9])\\s*(?:[AaPp]\\.?[Mm]\\.?)?|" +
            "(?:1[0-2]|0?[1-9])\\s*[AaPp]\\.?[Mm]\\.?)\\b"
    )

    /** "noon", "midnight" — explicit, unambiguous. */
    val TIME_WORD = Regex("\\b(noon|midnight)\\b", RegexOption.IGNORE_CASE)

    // ------------------------------------------------------------------
    // Timezones (phase §17–18)
    // ------------------------------------------------------------------

    /**
     * Explicit zone tokens. Abbreviations map to FIXED offsets (documented
     * heuristic — abbreviations are not DST-aware by design).
     */
    val TIMEZONE = Regex(
        "\\b(IST|UTC|GMT|PST|EST|CST|CET|Asia/Kolkata)\\b|" +
            "\\bIndia\\s+time\\b",
        RegexOption.IGNORE_CASE,
    )

    // ------------------------------------------------------------------
    // Semantic vocabulary (phases §14–15)
    // ------------------------------------------------------------------

    private const val DEADLINE_WORDS =
        "deadline|due\\s+by|due|submit\\s+by|submit|submission\\s+closes|applications?\\s+close|" +
            "last\\s+date|final\\s+date|must\\s+be\\s+completed\\s+by|complete\\s+before|" +
            "register\\s+by|payment\\s+due|expires?\\s+on|valid\\s+until|closing\\s+date"
    val DEADLINE_VOCAB = Regex("\\b($DEADLINE_WORDS)\\b", RegexOption.IGNORE_CASE)

    private const val MEETING_WORDS =
        "meeting|call|conference|appointment|interview|session|webinar|" +
            "discussion|sync(?:-up)?|demo|orientation"
    val MEETING_VOCAB = Regex("\\b($MEETING_WORDS)\\b", RegexOption.IGNORE_CASE)

    val PAYMENT_VOCAB = Regex(
        "\\b(payment|invoice|bill|payable|amount\\s+due)\\b",
        RegexOption.IGNORE_CASE,
    )
    val APPLICATION_VOCAB = Regex(
        "\\b(application|apply|admissions?)\\b",
        RegexOption.IGNORE_CASE,
    )
    val REGISTRATION_VOCAB = Regex(
        "\\b(regist(?:er|ration)|enrol(?:l|ment))\\b",
        RegexOption.IGNORE_CASE,
    )
    val SUBMISSION_VOCAB = Regex(
        "\\b(submit|submission|assignment)\\b",
        RegexOption.IGNORE_CASE,
    )
    val INTERVIEW_VOCAB = Regex("\\binterview\\b", RegexOption.IGNORE_CASE)
    val APPOINTMENT_VOCAB = Regex("\\bappointment\\b", RegexOption.IGNORE_CASE)
    val REMINDER_VOCAB = Regex("\\breminder|remind\\b", RegexOption.IGNORE_CASE)

    /** Future framing: "scheduled", "upcoming", "will be held", "save the date". */
    val FUTURE_MARKER = Regex(
        "\\b(scheduled|upcoming|will\\s+be\\s+held|planned|save\\s+the\\s+date|" +
            "looking\\s+forward\\s+to\\s+seeing)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** Past framing: "was held", "took place", "previous", "last week's". */
    val PAST_MARKER = Regex(
        "\\b(was\\s+held|took\\s+place|happened|previous|last\\s+week'?s|concluded)\\b",
        RegexOption.IGNORE_CASE,
    )

    // ------------------------------------------------------------------
    // Meeting links & locations (phases §30–31)
    // ------------------------------------------------------------------

    /** Recognized meeting-URL hosts — captured, never fetched. */
    val MEETING_URL = Regex(
        "https?://(?:[\\w-]+\\.)?(?:" +
            "meet\\.google\\.com|zoom\\.us|teams\\.microsoft\\.com|" +
            "webex\\.com|meet\\.zoom\\.us)[\\w\\-./?=&%+#]*",
        RegexOption.IGNORE_CASE,
    )

    /** "Location: X" / "Venue: X" — captured to end of line, bounded. */
    val LOCATION_LABELED = Regex(
        "(?m)^\\s*(?:location|venue|meeting\\s+location)\\s*[:\\-]\\s*(.+)$",
        RegexOption.IGNORE_CASE,
    )

    // ------------------------------------------------------------------
    // Bounds
    // ------------------------------------------------------------------

    /** Max chars of body scanned — matches the search index cap. */
    const val MAX_SCAN_CHARS = 20_000

    /** Context window (±) around a date/time match for semantic triggers. */
    const val CONTEXT_WINDOW_CHARS = 200

    /** Max captured location / title lengths. */
    const val MAX_LOCATION_CHARS = 120
    const val MAX_TITLE_CHARS = 80
}
