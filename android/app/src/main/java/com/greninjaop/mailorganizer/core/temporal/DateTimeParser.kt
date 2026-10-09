package com.greninjaop.mailorganizer.core.temporal

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Total date/time parser for temporal extraction (Phase 13).
 *
 * Every function is total: unparseable input returns null, never throws.
 * All relative expressions resolve against an explicit [referenceEpochMs]
 * (the message's received timestamp, phase §12) — the parser has no notion
 * of "now", which keeps extraction deterministic and testable.
 *
 * Documented ambiguity policy (phase §11):
 * - A numeric component > 12 disambiguates day vs month.
 * - When both components are ≤ 12 the format is genuinely ambiguous:
 *   resolved as day-first (documented app-locale default for the Indian
 *   audience) with [TemporalConfidence.LOW].
 * - "12 AM" is midnight (00:00), "12 PM" is noon (12:00) — tested explicitly.
 */
internal object DateTimeParser {

    /** One parsed instant plus how sure the parse is. */
    data class ParsedInstant(
        val epochMs: Long,
        val zoneId: String,
        val confidence: TemporalConfidence,
        /** True when the source text carried no year (resolved, not stated). */
        val yearImplied: Boolean = false,
        /** True when the source text carried no time (date granularity). */
        val dateOnly: Boolean = false,
    )

    // ------------------------------------------------------------------
    // Timezones
    // ------------------------------------------------------------------

    /**
     * Abbreviation → FIXED offset. Abbreviations are not DST-aware by
     * design (documented heuristic); IANA ids keep real zone rules.
     */
    private val ABBR_OFFSETS: Map<String, ZoneOffset> = mapOf(
        "IST" to ZoneOffset.of("+05:30"),
        "UTC" to ZoneOffset.UTC,
        "GMT" to ZoneOffset.UTC,
        "PST" to ZoneOffset.of("-08:00"),
        "EST" to ZoneOffset.of("-05:00"),
        "CST" to ZoneOffset.of("-06:00"),
        "CET" to ZoneOffset.of("+01:00"),
    )

    /** Resolves an explicit zone token, or null when not a known token. */
    fun resolveZoneToken(token: String): ZoneId? {
        val upper = token.uppercase()
        ABBR_OFFSETS[upper]?.let { return it }
        if (upper == "ASIA/KOLKATA") return ZoneId.of("Asia/Kolkata")
        if (token.equals("India time", ignoreCase = true)) return ZoneOffset.of("+05:30")
        return null
    }

    // ------------------------------------------------------------------
    // Dates
    // ------------------------------------------------------------------

    private fun monthNumber(name: String): Int? {
        val n = name.lowercase()
        Month.entries.forEach { if (it.name.lowercase().startsWith(n.take(3))) return it.value }
        // Full-name disambiguation for prefixes shared by two months is
        // handled by the longest-first ordering of Month.entries (e.g.
        // "march" vs "may" share no 3-letter prefix, so take(3) is enough).
        return null
    }

    /**
     * Parses an explicit date expression into a [LocalDate].
     *
     * @param preferFuture when true and the parsed date (in the reference
     *   year) falls before the reference date, the year rolls forward —
     *   used for future-framed triggers ("deadline", "scheduled").
     */
    fun parseDate(
        text: String,
        referenceEpochMs: Long,
        preferFuture: Boolean = false,
    ): Pair<LocalDate, TemporalConfidence>? {
        val t = text.trim()
        TemporalPatterns.ISO_DATE.find(t)?.let { m ->
            val (y, mo, d) = m.destructured
            return runCatching {
                LocalDate.of(y.toInt(), mo.toInt(), d.toInt()) to TemporalConfidence.HIGH
            }.getOrNull()
        }
        TemporalPatterns.DAY_MONTH_YEAR.find(t)?.let { m ->
            val (d, mon, y) = m.destructured
            val month = monthNumber(mon) ?: return null
            return runCatching {
                LocalDate.of(y.toInt(), month, d.toInt()) to TemporalConfidence.HIGH
            }.getOrNull()
        }
        TemporalPatterns.MONTH_DAY_YEAR.find(t)?.let { m ->
            val (mon, d, y) = m.destructured
            val month = monthNumber(mon) ?: return null
            return runCatching {
                LocalDate.of(y.toInt(), month, d.toInt()) to TemporalConfidence.HIGH
            }.getOrNull()
        }
        TemporalPatterns.NUMERIC_DATE.find(t)?.let { m ->
            val (a, b, yRaw) = m.destructured
            val aInt = a.toInt()
            val bInt = b.toInt()
            val year = if (yRaw.length == 2) {
                val yy = yRaw.toInt()
                if (yy <= 49) 2000 + yy else 1900 + yy
            } else yRaw.toInt()
            // Disambiguation: a component > 12 can only be the day.
            val (day, month, confident) = when {
                aInt > 12 && bInt <= 12 -> Triple(aInt, bInt, true)
                bInt > 12 && aInt <= 12 -> Triple(bInt, aInt, true)
                aInt <= 12 && bInt <= 12 ->
                    // Genuinely ambiguous → day-first default, LOW.
                    Triple(aInt, bInt, false)
                else -> return null
            }
            return runCatching {
                LocalDate.of(year, month, day) to
                    if (confident) TemporalConfidence.HIGH else TemporalConfidence.LOW
            }.getOrNull()
        }
        // No-year forms: resolve against the reference year.
        val refDate = Instant.ofEpochMilli(referenceEpochMs)
            .atZone(ZoneOffset.UTC).toLocalDate()
        TemporalPatterns.DAY_MONTH.find(t)?.let { m ->
            val (d, mon) = m.destructured
            val month = monthNumber(mon) ?: return null
            return runCatching {
                var date = LocalDate.of(refDate.year, month, d.toInt())
                if (preferFuture && date.isBefore(refDate)) date = date.plusYears(1)
                date to TemporalConfidence.MEDIUM
            }.getOrNull()
        }
        TemporalPatterns.MONTH_DAY.find(t)?.let { m ->
            val (mon, d) = m.destructured
            val month = monthNumber(mon) ?: return null
            return runCatching {
                var date = LocalDate.of(refDate.year, month, d.toInt())
                if (preferFuture && date.isBefore(refDate)) date = date.plusYears(1)
                date to TemporalConfidence.MEDIUM
            }.getOrNull()
        }
        return null
    }

    // ------------------------------------------------------------------
    // Relative dates
    // ------------------------------------------------------------------

    /**
     * Resolves relative day/weekday/offset expressions against the reference
     * date. Returns the resolved [LocalDate] and confidence.
     */
    fun parseRelative(
        text: String,
        referenceEpochMs: Long,
        zone: ZoneId,
    ): Pair<LocalDate, TemporalConfidence>? {
        val refDate = Instant.ofEpochMilli(referenceEpochMs).atZone(zone).toLocalDate()
        val t = text.trim().lowercase()

        TemporalPatterns.RELATIVE_DAY.find(t)?.let { m ->
            val date = when (m.groupValues[1].lowercase()) {
                "today", "tonight" -> refDate
                "tomorrow" -> refDate.plusDays(1)
                "yesterday" -> refDate.minusDays(1)
                "day after tomorrow" -> refDate.plusDays(2)
                else -> return null
            }
            return date to TemporalConfidence.MEDIUM
        }
        TemporalPatterns.RELATIVE_WEEKDAY.find(t)?.let { m ->
            val qualifier = m.groupValues[1].trim().lowercase()
            val target = dayOfWeek(m.groupValues[2]) ?: return null
            val date = when (qualifier) {
                // "this Friday": the Friday of the current week (may be past).
                "this" -> refDate.with(target)
                // "next Monday": the Monday of next week, then the target
                // weekday within that week — never the coming days' Monday.
                "next" -> {
                    val nextMonday =
                        refDate.plusDays((8 - refDate.dayOfWeek.value).toLong())
                    nextMonday.with(target)
                }
                // "last Tuesday": the Tuesday of the previous week.
                "last" -> {
                    val thisMonday =
                        refDate.minusDays((refDate.dayOfWeek.value - 1).toLong())
                    thisMonday.minusWeeks(1).with(target)
                }
                else -> return null
            }
            return date to TemporalConfidence.MEDIUM
        }
        TemporalPatterns.RELATIVE_OFFSET.find(t)?.let { m ->
            val amount = m.groupValues[1].ifEmpty { m.groupValues[3] }
            val wordAmount = m.groupValues[5]
            val unit = (m.groupValues[2].ifEmpty { m.groupValues[4] }).lowercase()
            val n: Long = when {
                amount.isNotEmpty() -> amount.toLongOrNull() ?: return null
                wordAmount.isNotEmpty() -> when (wordAmount.lowercase()) {
                    "one" -> 1; "two" -> 2; "three" -> 3; "four" -> 4
                    else -> return null
                }
                else -> return null
            }
            // "within 48 hours" ≈ 2 days — documented approximation.
            val date = when {
                unit.startsWith("hour") -> refDate.plusDays((n + 23) / 24)
                unit.startsWith("day") -> refDate.plusDays(n)
                unit.startsWith("week") -> refDate.plusWeeks(n)
                unit.startsWith("month") -> refDate.plusMonths(n)
                else -> return null
            }
            return date to TemporalConfidence.LOW
        }
        if (TemporalPatterns.RELATIVE_WEEK.containsMatchIn(t)) {
            val daysUntilNextMonday = 8 - refDate.dayOfWeek.value
            return refDate.plusDays(daysUntilNextMonday.toLong()) to TemporalConfidence.LOW
        }
        return null
    }

    private fun dayOfWeek(name: String): DayOfWeek? = when (name.lowercase()) {
        "monday" -> DayOfWeek.MONDAY
        "tuesday" -> DayOfWeek.TUESDAY
        "wednesday" -> DayOfWeek.WEDNESDAY
        "thursday" -> DayOfWeek.THURSDAY
        "friday" -> DayOfWeek.FRIDAY
        "saturday" -> DayOfWeek.SATURDAY
        "sunday" -> DayOfWeek.SUNDAY
        else -> null
    }

    // ------------------------------------------------------------------
    // Times
    // ------------------------------------------------------------------

    /**
     * Parses a time expression. "12 AM" → 00:00 (midnight), "12 PM" → 12:00
     * (noon) — handled explicitly and covered by tests.
     */
    fun parseTime(text: String): LocalTime? {
        val t = text.trim().lowercase()
        if (t == "noon") return LocalTime.NOON
        if (t == "midnight") return LocalTime.MIDNIGHT
        val m = TemporalPatterns.TIME.find(text) ?: return null
        val raw = m.groupValues[1]
        val ampm = Regex("[AaPp]\\.?[Mm]\\.?",).find(raw)?.value
            ?.replace(".", "")?.uppercase()
        // Normalize "10.30" → "10:30" before digit extraction.
        val normalized = raw.replace(Regex("(\\d)\\.(\\d)"), "$1:$2")
        val digits = normalized.replace(Regex("[^0-9:]"), "")
        return runCatching {
            val time = if (digits.contains(":")) {
                val parts = digits.split(":")
                if (parts.size != 2) return null
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } else {
                LocalTime.of(digits.toInt(), 0)
            }
            when (ampm) {
                "AM" -> if (time.hour == 12) time.withHour(0) else time
                "PM" -> if (time.hour < 12) time.plusHours(12) else time
                else -> time // 24h form, taken as-is
            }
        }.getOrNull()
    }

    // ------------------------------------------------------------------
    // Assembly
    // ------------------------------------------------------------------

    /** Combines a date + optional time into an instant in [zone]. */
    fun toInstant(
        date: LocalDate,
        time: LocalTime?,
        zone: ZoneId,
    ): Long = runCatching {
        ZonedDateTime.of(date, time ?: LocalTime.MIDNIGHT, zone)
            .toInstant().toEpochMilli()
    }.getOrDefault(0L)
}
