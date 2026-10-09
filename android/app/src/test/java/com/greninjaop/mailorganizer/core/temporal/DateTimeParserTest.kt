package com.greninjaop.mailorganizer.core.temporal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneOffset

/**
 * Date/time parser tests (Phase 13).
 *
 * The parser is total and deterministic: every test fixes the reference
 * time. Covers all plan-mandated formats, the 12AM/12PM edge cases, the
 * ambiguity policy, and relative-date resolution.
 */
class DateTimeParserTest {

    /** Fixed reference: 2026-10-09T12:00:00Z (a Friday). */
    private val ref = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()

    // ---- Explicit dates ----

    @Test
    fun `parses day month year`() {
        val (date, conf) = DateTimeParser.parseDate("18 October 2026", ref)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 18), date)
        assertEquals(TemporalConfidence.HIGH, conf)
    }

    @Test
    fun `parses month day year with ordinal`() {
        val (date, conf) = DateTimeParser.parseDate("October 18th, 2026", ref)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 18), date)
        assertEquals(TemporalConfidence.HIGH, conf)
    }

    @Test
    fun `parses ISO date`() {
        val (date, conf) = DateTimeParser.parseDate("2026-10-18", ref)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 18), date)
        assertEquals(TemporalConfidence.HIGH, conf)
    }

    @Test
    fun `numeric date disambiguates when a component exceeds 12`() {
        val (date, conf) = DateTimeParser.parseDate("18/10/2026", ref)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 18), date)
        assertEquals(TemporalConfidence.HIGH, conf)
    }

    @Test
    fun `ambiguous numeric date resolves day-first with LOW confidence`() {
        // 03/04/2026 is genuinely ambiguous — documented default, LOW.
        val (date, conf) = DateTimeParser.parseDate("03/04/2026", ref)!!
        assertEquals(LocalDate.of(2026, Month.APRIL, 3), date)
        assertEquals(TemporalConfidence.LOW, conf)
    }

    @Test
    fun `no-year date resolves to reference year`() {
        val (date, conf) = DateTimeParser.parseDate("18 Oct", ref)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 18), date)
        assertEquals(TemporalConfidence.MEDIUM, conf)
    }

    @Test
    fun `no-year past date rolls forward when preferFuture`() {
        // Reference is 9 Oct 2026; "5 Oct" with preferFuture → 2027.
        val (date, _) = DateTimeParser.parseDate("5 Oct", ref, preferFuture = true)!!
        assertEquals(LocalDate.of(2027, Month.OCTOBER, 5), date)
    }

    @Test
    fun `no-year past date stays when not preferFuture`() {
        val (date, _) = DateTimeParser.parseDate("5 Oct", ref, preferFuture = false)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 5), date)
    }

    @Test
    fun `invalid date returns null`() {
        assertNull(DateTimeParser.parseDate("32 October 2026", ref))
        assertNull(DateTimeParser.parseDate("not a date", ref))
    }

    // ---- Times ----

    @Test
    fun `parses 12-hour times`() {
        assertEquals(LocalTime.of(10, 30), DateTimeParser.parseTime("10:30 AM"))
        assertEquals(LocalTime.of(22, 30), DateTimeParser.parseTime("10:30 PM"))
        assertEquals(LocalTime.of(10, 0), DateTimeParser.parseTime("10 AM"))
    }

    @Test
    fun `twelve AM is midnight and twelve PM is noon`() {
        assertEquals(LocalTime.MIDNIGHT, DateTimeParser.parseTime("12 AM"))
        assertEquals(LocalTime.of(0, 30), DateTimeParser.parseTime("12:30 AM"))
        assertEquals(LocalTime.NOON, DateTimeParser.parseTime("12 PM"))
        assertEquals(LocalTime.of(12, 30), DateTimeParser.parseTime("12:30 PM"))
    }

    @Test
    fun `parses 24-hour and dotted times`() {
        assertEquals(LocalTime.of(22, 30), DateTimeParser.parseTime("22:30"))
        assertEquals(LocalTime.of(10, 30), DateTimeParser.parseTime("10.30 AM"))
    }

    @Test
    fun `parses noon and midnight words`() {
        assertEquals(LocalTime.NOON, DateTimeParser.parseTime("noon"))
        assertEquals(LocalTime.MIDNIGHT, DateTimeParser.parseTime("midnight"))
    }

    // ---- Relative dates ----

    @Test
    fun `tomorrow resolves against reference`() {
        val zone = ZoneOffset.UTC
        val (date, _) = DateTimeParser.parseRelative("tomorrow", ref, zone)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 10), date)
    }

    @Test
    fun `next Monday is the Monday of next week`() {
        // Reference is Friday 9 Oct 2026 → next Monday is 12 Oct.
        val zone = ZoneOffset.UTC
        val (date, _) = DateTimeParser.parseRelative("next Monday", ref, zone)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 12), date)
    }

    @Test
    fun `this Friday is the Friday of the current week`() {
        val zone = ZoneOffset.UTC
        val (date, _) = DateTimeParser.parseRelative("this Friday", ref, zone)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 9), date)
    }

    @Test
    fun `in 3 days resolves`() {
        val zone = ZoneOffset.UTC
        val (date, _) = DateTimeParser.parseRelative("in 3 days", ref, zone)!!
        assertEquals(LocalDate.of(2026, Month.OCTOBER, 12), date)
    }

    // ---- Timezones ----

    @Test
    fun `IST resolves to plus-0530`() {
        val zone = DateTimeParser.resolveZoneToken("IST")
        assertNotNull(zone)
        val instant = DateTimeParser.toInstant(
            LocalDate.of(2026, Month.OCTOBER, 18),
            LocalTime.of(10, 30),
            zone!!,
        )
        // 10:30 IST = 05:00 UTC.
        assertEquals(
            Instant.parse("2026-10-18T05:00:00Z").toEpochMilli(),
            instant,
        )
    }

    @Test
    fun `unknown zone token returns null`() {
        assertNull(DateTimeParser.resolveZoneToken("MARS"))
    }

    @Test
    fun `Asia Kolkata resolves`() {
        assertNotNull(DateTimeParser.resolveZoneToken("Asia/Kolkata"))
    }

    // ---- Assembly ----

    @Test
    fun `toInstant combines date and time`() {
        val instant = DateTimeParser.toInstant(
            LocalDate.of(2026, Month.OCTOBER, 18),
            LocalTime.of(10, 30),
            ZoneOffset.UTC,
        )
        assertEquals(Instant.parse("2026-10-18T10:30:00Z").toEpochMilli(), instant)
    }

    @Test
    fun `toInstant defaults to midnight without time`() {
        val instant = DateTimeParser.toInstant(
            LocalDate.of(2026, Month.OCTOBER, 18),
            null,
            ZoneOffset.UTC,
        )
        assertEquals(Instant.parse("2026-10-18T00:00:00Z").toEpochMilli(), instant)
        assertTrue(instant > 0)
    }
}
