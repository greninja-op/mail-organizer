package com.greninjaop.mailorganizer.core.temporal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Deterministic meeting/deadline extractor (Phase 13).
 *
 * Pure function of ([ExtractionInput], patterns, [VERSION]): no network, no
 * randomness, no device state. Reference time is the message's received
 * timestamp (phase §12), injected by the caller.
 *
 * Pipeline:
 * ```
 * subject + snippet + body (bounded)
 *   ↓ pattern scan (ranges first — they consume two dates)
 * candidate date/time spans
 *   ↓ ±context window semantic analysis
 * type resolution (precedence-ordered vocabulary)
 *   ↓ validation (end >= start; resolvable instant)
 * confidence + status + explanation
 *   ↓ dedup + sort
 * ExtractionResult
 * ```
 *
 * Honesty rules (never violated):
 * - No end time is invented: [ExtractedTemporal.endEpochMs] is set only
 *   when the text states a range or an explicit end time.
 * - No timezone is invented: fallback zone is labeled APP_FALLBACK.
 * - Ambiguous input degrades to LOW confidence or is skipped — dates are
 *   never fabricated from vague language ("soon", "ASAP", "later").
 */
object DeterministicTemporalExtractor {

    const val VERSION = 1

    /** Hard cap on items per message (pathological-input guard). */
    private const val MAX_ITEMS = 20

    /** Max chars between a date and its time for them to combine. */
    private const val DATE_TIME_PROXIMITY = 80

    fun extract(
        input: ExtractionInput,
        clock: () -> Long = System::currentTimeMillis,
    ): ExtractionResult {
        val now = clock()
        val haystack = buildString {
            append(input.subject)
            append('\n')
            input.snippet?.let { append(it); append('\n') }
            input.bodyText?.let { append(it.take(TemporalPatterns.MAX_SCAN_CHARS)) }
        }.toString()
        if (haystack.isBlank()) return ExtractionResult(emptyList(), VERSION, now)

        val fallbackZone = runCatching { ZoneId.of(input.fallbackZoneId) }
            .getOrDefault(ZoneOffset.UTC)

        val candidates = mutableListOf<RawCandidate>()

        // Ranges first — they consume date spans other patterns would match.
        extractRanges(haystack, input, fallbackZone, candidates)
        val rangeSpans = candidates.map { it.span }.toSet()

        extractDates(haystack, input, fallbackZone, rangeSpans, candidates)
        extractRelatives(haystack, input, fallbackZone, rangeSpans, candidates)
        extractTimeOnly(haystack, input, fallbackZone, candidates)

        if (candidates.isEmpty()) return ExtractionResult(emptyList(), VERSION, now)

        val meetingUrl = TemporalPatterns.MEETING_URL.find(haystack)
            ?.value?.take(300)
        val location = TemporalPatterns.LOCATION_LABELED.find(haystack)
            ?.groupValues?.get(1)?.trim()
            ?.take(TemporalPatterns.MAX_LOCATION_CHARS)
            ?.takeIf { it.isNotEmpty() }

        val items = candidates
            .mapNotNull { resolve(it, haystack, input, fallbackZone, meetingUrl, location, now) }
            .distinctBy { it.type to it.startEpochMs }
            .sortedBy { it.startEpochMs }
            .take(MAX_ITEMS)

        return ExtractionResult(items, VERSION, now)
    }

    // ------------------------------------------------------------------
    // Candidate collection
    // ------------------------------------------------------------------

    private data class RawCandidate(
        val span: IntRange,
        val dateText: String,
        /** "explicit" | "relative" | "time-only". */
        val kind: String,
        val endDateText: String? = null,
    )

    private fun extractRanges(
        haystack: String,
        input: ExtractionInput,
        fallbackZone: ZoneId,
        out: MutableList<RawCandidate>,
    ) {
        val refYear = refYear(input.referenceEpochMs)
        for (pattern in listOf(
            TemporalPatterns.DATE_RANGE_MONTH_SPAN,
            TemporalPatterns.DATE_RANGE_DAY_SPAN,
        )) {
            for (m in pattern.findAll(haystack)) {
                val (startText, endText) = when (pattern) {
                    TemporalPatterns.DATE_RANGE_MONTH_SPAN -> {
                        val (mon, d1, d2) = m.destructured
                        "$d1 $mon $refYear" to "$d2 $mon $refYear"
                    }
                    else -> {
                        val (d1, d2, mon) = m.destructured
                        "$d1 $mon $refYear" to "$d2 $mon $refYear"
                    }
                }
                out += RawCandidate(m.range, startText, "explicit", endText)
            }
        }
    }

    private fun extractDates(
        haystack: String,
        input: ExtractionInput,
        fallbackZone: ZoneId,
        rangeSpans: Set<IntRange>,
        out: MutableList<RawCandidate>,
    ) {
        val patterns = listOf(
            TemporalPatterns.ISO_DATE,
            TemporalPatterns.DAY_MONTH_YEAR,
            TemporalPatterns.MONTH_DAY_YEAR,
            TemporalPatterns.NUMERIC_DATE,
            TemporalPatterns.DAY_MONTH,
            TemporalPatterns.MONTH_DAY,
        )
        for (pattern in patterns) {
            for (m in pattern.findAll(haystack)) {
                if (rangeSpans.any { it.contains(m.range.first) }) continue
                // Skip a span already claimed by an earlier (more specific) pattern.
                if (out.any { it.span.first <= m.range.first && m.range.last <= it.span.last }) continue
                out += RawCandidate(m.range, m.value, "explicit")
            }
        }
    }

    private fun extractRelatives(
        haystack: String,
        input: ExtractionInput,
        fallbackZone: ZoneId,
        rangeSpans: Set<IntRange>,
        out: MutableList<RawCandidate>,
    ) {
        val patterns = listOf(
            TemporalPatterns.RELATIVE_DAY,
            TemporalPatterns.RELATIVE_WEEKDAY,
            TemporalPatterns.RELATIVE_OFFSET,
            TemporalPatterns.RELATIVE_WEEK,
        )
        for (pattern in patterns) {
            for (m in pattern.findAll(haystack)) {
                if (rangeSpans.any { it.contains(m.range.first) }) continue
                if (out.any { it.span.first <= m.range.first && m.range.last <= it.span.last }) continue
                out += RawCandidate(m.range, m.value, "relative")
            }
        }
    }

    private fun extractTimeOnly(
        haystack: String,
        input: ExtractionInput,
        fallbackZone: ZoneId,
        out: MutableList<RawCandidate>,
    ) {
        for (pattern in listOf(TemporalPatterns.TIME, TemporalPatterns.TIME_WORD)) {
            for (m in pattern.findAll(haystack)) {
                // Skip times already adjacent to a date candidate (they combine).
                val nearDate = out.any {
                    kotlin.math.abs(it.span.first - m.range.first) <= DATE_TIME_PROXIMITY
                }
                if (nearDate) continue
                out += RawCandidate(m.range, m.value, "time-only")
            }
        }
    }

    // ------------------------------------------------------------------
    // Resolution
    // ------------------------------------------------------------------

    private fun resolve(
        candidate: RawCandidate,
        haystack: String,
        input: ExtractionInput,
        fallbackZone: ZoneId,
        meetingUrl: String?,
        location: String?,
        now: Long,
    ): ExtractedTemporal? {
        val windowStart = maxOf(0, candidate.span.first - TemporalPatterns.CONTEXT_WINDOW_CHARS)
        val windowEnd = minOf(
            haystack.length,
            candidate.span.last + TemporalPatterns.CONTEXT_WINDOW_CHARS,
        )
        val window = haystack.substring(windowStart, windowEnd)

        val hasDeadline = TemporalPatterns.DEADLINE_VOCAB.containsMatchIn(window)
        val hasMeeting = TemporalPatterns.MEETING_VOCAB.containsMatchIn(window)
        val hasFuture = TemporalPatterns.FUTURE_MARKER.containsMatchIn(window)
        val hasPast = TemporalPatterns.PAST_MARKER.containsMatchIn(window)

        val type = resolveType(window, candidate)
        // Bare explicit dates with no semantic signal and no time are noise —
        // skip them rather than filling the UI with random dates. (Dates
        // with past-tense framing are still meaningful history.) Relative
        // dates ("tomorrow", "next Monday") are inherently intentional
        // references, so they are exempt from this filter.
        if (type == TemporalItemType.DATE_ONLY &&
            candidate.kind != "relative" &&
            !hasDeadline && !hasMeeting && !hasPast &&
            !TemporalPatterns.REMINDER_VOCAB.containsMatchIn(window)
        ) return null

        // Time: nearest time expression within proximity (either side).
        val timeText = nearestTime(haystack, candidate.span)
        val time = timeText?.let { DateTimeParser.parseTime(it) }

        // Explicit end time: "10:00 AM–11:30 AM" / "10 AM to 11 AM".
        val endTime = timeText?.let { findEndTime(haystack, candidate.span, it) }

        // Timezone: nearest explicit token near the date/time.
        val tzToken = nearestTimezone(haystack, candidate.span)
        val zone = tzToken?.let { DateTimeParser.resolveZoneToken(it) }
        val (zoneId, tzSource) = when {
            zone != null -> zone.id to TimezoneSource.EXPLICIT_IN_EMAIL
            else -> fallbackZone.id to TimezoneSource.APP_FALLBACK
        }

        // Date resolution.
        val preferFuture = hasDeadline || hasMeeting || hasFuture
        val (date, dateConfidence, yearImplied) = when (candidate.kind) {
            "relative" -> {
                val (d, c) = DateTimeParser.parseRelative(
                    candidate.dateText, input.referenceEpochMs, fallbackZone,
                ) ?: return null
                Triple(d, c, false)
            }
            "time-only" -> {
                val refDate = java.time.Instant.ofEpochMilli(input.referenceEpochMs)
                    .atZone(fallbackZone).toLocalDate()
                Triple(refDate, TemporalConfidence.LOW, false)
            }
            else -> {
                val (d, c) = DateTimeParser.parseDate(
                    candidate.dateText, input.referenceEpochMs, preferFuture,
                ) ?: return null
                val implied = !candidate.dateText.contains(Regex("(19|20)\\d{2}"))
                Triple(d, c, implied)
            }
        }

        val startEpochMs = DateTimeParser.toInstant(date, time, zone ?: fallbackZone)
        if (startEpochMs == 0L) return null

        val endEpochMs = when {
            candidate.endDateText != null -> {
                val (endDate, _) = DateTimeParser.parseDate(
                    candidate.endDateText, input.referenceEpochMs, preferFuture,
                ) ?: return null
                val end = DateTimeParser.toInstant(endDate, time, zone ?: fallbackZone)
                // phase §19: end >= start, else the range is invalid.
                if (end < startEpochMs) return null
                end
            }
            endTime != null -> {
                val end = DateTimeParser.toInstant(date, endTime, zone ?: fallbackZone)
                if (end <= startEpochMs) null else end
            }
            else -> null
        }

        val status = when {
            startEpochMs < input.referenceEpochMs -> TemporalStatus.PAST
            else -> TemporalStatus.UPCOMING
        }

        val signals = buildSignals(
            candidate, type, hasDeadline, hasMeeting, tzToken != null,
            time != null, endEpochMs != null, dateConfidence, yearImplied,
        )
        val confidence = resolveConfidence(
            type, dateConfidence, tzToken != null, time != null,
            hasDeadline || hasMeeting, candidate.kind,
        )
        val title = displayTitle(type)
        val explanation = buildExplanation(type, title, signals)

        return ExtractedTemporal(
            type = type,
            title = title,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            isDateOnly = time == null,
            timezoneId = zoneId,
            timezoneSource = tzSource,
            location = if (type.isEventLike()) location else null,
            meetingUrl = if (type.isEventLike()) meetingUrl else null,
            status = status,
            confidence = confidence,
            signals = signals,
            version = VERSION,
            extractedAtEpochMs = now,
            explanation = explanation,
        )
    }

    private fun resolveType(window: String, candidate: RawCandidate): TemporalItemType {
        if (candidate.endDateText != null) return TemporalItemType.DATE_RANGE
        val hasDeadline = TemporalPatterns.DEADLINE_VOCAB.containsMatchIn(window)
        val hasInterview = TemporalPatterns.INTERVIEW_VOCAB.containsMatchIn(window)
        val hasAppointment = TemporalPatterns.APPOINTMENT_VOCAB.containsMatchIn(window)
        val hasMeeting = TemporalPatterns.MEETING_VOCAB.containsMatchIn(window)
        val hasPayment = TemporalPatterns.PAYMENT_VOCAB.containsMatchIn(window)
        val hasApplication = TemporalPatterns.APPLICATION_VOCAB.containsMatchIn(window)
        val hasRegistration = TemporalPatterns.REGISTRATION_VOCAB.containsMatchIn(window)
        val hasSubmission = TemporalPatterns.SUBMISSION_VOCAB.containsMatchIn(window)
        val hasReminder = TemporalPatterns.REMINDER_VOCAB.containsMatchIn(window)
        return when {
            hasInterview -> TemporalItemType.INTERVIEW
            hasPayment && hasDeadline -> TemporalItemType.PAYMENT_DEADLINE
            hasApplication && hasDeadline -> TemporalItemType.APPLICATION_DEADLINE
            hasRegistration && hasDeadline -> TemporalItemType.REGISTRATION_DEADLINE
            hasSubmission && hasDeadline -> TemporalItemType.SUBMISSION_DEADLINE
            hasDeadline -> TemporalItemType.DEADLINE
            hasAppointment -> TemporalItemType.APPOINTMENT
            hasMeeting -> TemporalItemType.MEETING
            hasReminder -> TemporalItemType.REMINDER_DATE
            candidate.kind == "time-only" -> TemporalItemType.TIME_ONLY
            else -> TemporalItemType.DATE_ONLY
        }
    }

    private fun nearestTime(haystack: String, span: IntRange): String? {
        var best: String? = null
        var bestDist = Int.MAX_VALUE
        for (pattern in listOf(TemporalPatterns.TIME, TemporalPatterns.TIME_WORD)) {
            for (m in pattern.findAll(haystack)) {
                if (m.range.first in span || span.first in m.range) continue
                val dist = kotlin.math.min(
                    kotlin.math.abs(m.range.first - span.last),
                    kotlin.math.abs(span.first - m.range.last),
                )
                if (dist <= DATE_TIME_PROXIMITY && dist < bestDist) {
                    bestDist = dist
                    best = m.value
                }
            }
        }
        return best
    }

    /** "10:00 AM–11:30 AM" / "10 AM to 11 AM": explicit end time. */
    private fun findEndTime(
        haystack: String,
        span: IntRange,
        timeText: String,
    ): LocalTime? {
        val idx = haystack.indexOf(timeText, span.first - DATE_TIME_PROXIMITY)
        if (idx < 0) return null
        val after = haystack.substring(
            idx + timeText.length,
            minOf(haystack.length, idx + timeText.length + 20),
        )
        val m = Regex("^\\s*(?:–|—|-|to)\\s*(.+)").find(after) ?: return null
        return DateTimeParser.parseTime(m.groupValues[1].take(20))
    }

    private fun nearestTimezone(haystack: String, span: IntRange): String? {
        var best: String? = null
        var bestDist = Int.MAX_VALUE
        for (m in TemporalPatterns.TIMEZONE.findAll(haystack)) {
            val dist = kotlin.math.min(
                kotlin.math.abs(m.range.first - span.last),
                kotlin.math.abs(span.first - m.range.last),
            )
            if (dist <= DATE_TIME_PROXIMITY && dist < bestDist) {
                bestDist = dist
                best = m.value
            }
        }
        return best
    }

    private fun buildSignals(
        candidate: RawCandidate,
        type: TemporalItemType,
        hasDeadline: Boolean,
        hasMeeting: Boolean,
        hasExplicitTz: Boolean,
        hasTime: Boolean,
        hasEnd: Boolean,
        dateConfidence: TemporalConfidence,
        yearImplied: Boolean,
    ): List<TemporalSignal> {
        val signals = mutableListOf<TemporalSignal>()
        if (hasDeadline) signals += TemporalSignal(
            "deadline_vocabulary",
            "deadline language appears near the date",
        )
        if (hasMeeting) signals += TemporalSignal(
            "meeting_vocabulary",
            "meeting/event language appears near the date",
        )
        signals += TemporalSignal(
            "explicit_date",
            "date is explicitly stated as \"${candidate.dateText.trim()}\"",
        )
        if (hasTime) signals += TemporalSignal("explicit_time", "time is explicitly stated")
        if (hasEnd) signals += TemporalSignal("explicit_end", "end time/date is explicitly stated")
        if (hasExplicitTz) signals += TemporalSignal(
            "explicit_timezone",
            "timezone is explicitly stated in the email",
        )
        if (yearImplied) signals += TemporalSignal(
            "year_implied",
            "year was not stated; resolved from the message date",
        )
        if (candidate.kind == "relative") signals += TemporalSignal(
            "relative_date",
            "relative expression resolved against the message date",
        )
        if (dateConfidence == TemporalConfidence.LOW) signals += TemporalSignal(
            "ambiguous_format",
            "date format is ambiguous; resolved with the documented default",
        )
        return signals
    }

    private fun resolveConfidence(
        type: TemporalItemType,
        dateConfidence: TemporalConfidence,
        hasExplicitTz: Boolean,
        hasTime: Boolean,
        hasTrigger: Boolean,
        kind: String,
    ): TemporalConfidence {
        if (dateConfidence == TemporalConfidence.LOW) return TemporalConfidence.LOW
        if (kind == "relative" || kind == "time-only") return TemporalConfidence.LOW
        if (type == TemporalItemType.DATE_ONLY && !hasTrigger) return TemporalConfidence.LOW
        if (hasExplicitTz && hasTime && hasTrigger) return TemporalConfidence.HIGH
        return TemporalConfidence.MEDIUM
    }

    private fun displayTitle(type: TemporalItemType): String = when (type) {
        TemporalItemType.DEADLINE -> "Deadline"
        TemporalItemType.MEETING -> "Meeting"
        TemporalItemType.APPOINTMENT -> "Appointment"
        TemporalItemType.EVENT -> "Event"
        TemporalItemType.INTERVIEW -> "Interview"
        TemporalItemType.SUBMISSION_DEADLINE -> "Submission deadline"
        TemporalItemType.APPLICATION_DEADLINE -> "Application deadline"
        TemporalItemType.PAYMENT_DEADLINE -> "Payment deadline"
        TemporalItemType.REGISTRATION_DEADLINE -> "Registration deadline"
        TemporalItemType.REMINDER_DATE -> "Reminder"
        TemporalItemType.DATE_ONLY -> "Date"
        TemporalItemType.TIME_ONLY -> "Time"
        TemporalItemType.DATE_TIME -> "Scheduled time"
        TemporalItemType.DATE_RANGE -> "Date range"
    }

    private fun buildExplanation(
        type: TemporalItemType,
        title: String,
        signals: List<TemporalSignal>,
    ): String = buildString {
        append(title)
        append(" detected because:\n")
        signals.forEach { append("• ").append(it.detail).append('\n') }
    }.trimEnd()

    private fun TemporalItemType.isEventLike(): Boolean = when (this) {
        TemporalItemType.MEETING, TemporalItemType.APPOINTMENT,
        TemporalItemType.EVENT, TemporalItemType.INTERVIEW -> true
        else -> false
    }

    private fun refYear(referenceEpochMs: Long): Int =
        java.time.Instant.ofEpochMilli(referenceEpochMs)
            .atZone(ZoneOffset.UTC).year
}
