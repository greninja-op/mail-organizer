package com.greninjaop.mailorganizer.core.temporal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Deterministic temporal extractor tests (Phase 13).
 *
 * Verifies: deadline/meeting detection, type resolution precedence, the
 * honesty rules (no invented end times/timezones, past vs future, no
 * precision manufacturing), multiple candidates, explainability, and
 * determinism. Every test fixes the reference time.
 */
class DeterministicTemporalExtractorTest {

    /** Fixed reference: 2026-10-09T12:00:00Z (a Friday). */
    private val ref = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()
    private val clock = { 1_800_000_000_000L }

    private fun input(
        subject: String = "",
        body: String = "",
        snippet: String? = null,
    ) = ExtractionInput(
        messageId = "m1",
        threadId = "t1",
        subject = subject,
        snippet = snippet,
        bodyText = body.ifEmpty { null },
        referenceEpochMs = ref,
        fallbackZoneId = "UTC",
    )

    private fun extract(subject: String = "", body: String = "") =
        DeterministicTemporalExtractor.extract(input(subject, body), clock)

    // ---- Deadline detection ----

    @Test
    fun `explicit deadline is extracted`() {
        val result = extract(
            subject = "Assignment",
            body = "Please submit your assignment. The final deadline is 18 October 2026.",
        )
        assertTrue(result.items.isNotEmpty())
        // "submit" + "deadline" → SUBMISSION_DEADLINE by type precedence.
        val item = result.items.first { it.type == TemporalItemType.SUBMISSION_DEADLINE }
        assertEquals(Instant.parse("2026-10-18T00:00:00Z").toEpochMilli(), item.startEpochMs)
        assertEquals(TemporalStatus.UPCOMING, item.status)
        assertTrue(item.isDateOnly)
        assertTrue(item.explanation.contains("deadline"))
    }

    @Test
    fun `past-tense date is not a deadline`() {
        // "The course started on September 1." — no deadline language.
        val result = extract(body = "The course started on September 1.")
        assertTrue(result.items.none { it.type == TemporalItemType.DEADLINE })
    }

    @Test
    fun `payment language plus deadline resolves to PAYMENT_DEADLINE`() {
        val result = extract(
            body = "Your invoice is ready. Payment due by 20 October 2026.",
        )
        assertTrue(result.items.any { it.type == TemporalItemType.PAYMENT_DEADLINE })
    }

    @Test
    fun `application deadline resolves correctly`() {
        val result = extract(
            body = "Applications close on 15 October 2026. Apply now!",
        )
        assertTrue(result.items.any { it.type == TemporalItemType.APPLICATION_DEADLINE })
    }

    // ---- Meeting detection ----

    @Test
    fun `scheduled interview produces an interview event`() {
        val result = extract(
            body = "Interview scheduled for next Monday at 10:30 AM IST.",
        )
        val item = result.items.firstOrNull { it.type == TemporalItemType.INTERVIEW }
        assertNotNull(item)
        assertEquals(false, item!!.isDateOnly)
        // IST maps to the fixed +05:30 offset (documented heuristic).
        assertEquals("+05:30", item.timezoneId)
        assertEquals(TemporalStatus.UPCOMING, item.status)
    }

    @Test
    fun `past meeting mention is not upcoming`() {
        val result = extract(body = "We discussed our previous Monday meeting on 5 October 2026.")
        val items = result.items.filter { it.type == TemporalItemType.MEETING }
        // Either skipped or marked PAST — never UPCOMING.
        assertTrue(items.none { it.status == TemporalStatus.UPCOMING })
    }

    @Test
    fun `meeting with explicit time range keeps start and end`() {
        val result = extract(
            body = "Team sync on 12 October 2026, 10:00 AM - 11:30 AM.",
        )
        val item = result.items.firstOrNull { it.type == TemporalItemType.MEETING }
        assertNotNull(item)
        assertNotNull(item!!.endEpochMs)
        assertTrue(item.endEpochMs!! > item.startEpochMs)
    }

    @Test
    fun `meeting with only start time invents no end`() {
        val result = extract(body = "Team sync on 12 October 2026 at 10:00 AM.")
        val item = result.items.firstOrNull { it.type == TemporalItemType.MEETING }
        assertNotNull(item)
        assertNull(item!!.endEpochMs)
    }

    // ---- Multiple candidates ----

    @Test
    fun `multiple dates all extracted`() {
        val result = extract(
            body = "Applications open September 1. Applications close October 15. " +
                "Interviews begin October 20.",
        )
        assertTrue(result.items.size >= 2)
    }

    @Test
    fun `date range produces one range item`() {
        val result = extract(body = "The workshop runs October 10-12.")
        val item = result.items.firstOrNull { it.type == TemporalItemType.DATE_RANGE }
        assertNotNull(item)
        assertNotNull(item!!.endEpochMs)
        assertTrue(item.endEpochMs!! >= item.startEpochMs)
    }

    // ---- Timezones ----

    @Test
    fun `explicit IST is kept with explicit source`() {
        val result = extract(body = "Call at 10:30 AM IST on 12 October 2026.")
        val item = result.items.firstOrNull { it.type == TemporalItemType.MEETING }
        assertNotNull(item)
        assertEquals(TimezoneSource.EXPLICIT_IN_EMAIL, item!!.timezoneSource)
        // 10:30 IST = 05:00 UTC.
        assertEquals(
            Instant.parse("2026-10-12T05:00:00Z").toEpochMilli(),
            item.startEpochMs,
        )
    }

    @Test
    fun `missing timezone falls back honestly`() {
        val result = extract(body = "Deadline is 18 October 2026.")
        val item = result.items.firstOrNull { it.type == TemporalItemType.DEADLINE }
        assertNotNull(item)
        assertEquals(TimezoneSource.APP_FALLBACK, item!!.timezoneSource)
    }

    // ---- Relative dates ----

    @Test
    fun `tomorrow resolves against message date`() {
        val result = extract(body = "Please review by tomorrow.")
        val item = result.items.firstOrNull()
        assertNotNull(item)
        // Reference is 9 Oct → tomorrow is 10 Oct.
        assertEquals(
            Instant.parse("2026-10-10T00:00:00Z").toEpochMilli(),
            item!!.startEpochMs,
        )
    }

    @Test
    fun `tomorrow morning keeps date but no invented time`() {
        // Phase §21: "tomorrow morning" → date known, time unknown.
        val result = extract(body = "Let's catch up tomorrow morning.")
        val item = result.items.firstOrNull { it.type == TemporalItemType.DATE_ONLY }
        if (item != null) {
            assertTrue(item.isDateOnly)
        }
    }

    // ---- Meeting links & location ----

    @Test
    fun `meeting URL is stored not validated`() {
        val result = extract(
            body = "Join the meeting on 12 October 2026 at 10 AM: " +
                "https://meet.google.com/abc-defg-hij",
        )
        val item = result.items.firstOrNull { it.type == TemporalItemType.MEETING }
        assertNotNull(item)
        assertEquals("https://meet.google.com/abc-defg-hij", item!!.meetingUrl)
    }

    @Test
    fun `labeled location is captured`() {
        val result = extract(
            body = "Interview on 12 October 2026 at 10 AM.\nLocation: Room 203",
        )
        val item = result.items.firstOrNull { it.type == TemporalItemType.INTERVIEW }
        assertNotNull(item)
        assertEquals("Room 203", item!!.location)
    }

    // ---- Honesty ----

    @Test
    fun `vague language produces nothing`() {
        val result = extract(body = "Let's catch up soon. Ping me ASAP.")
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `empty input produces empty result`() {
        val result = DeterministicTemporalExtractor.extract(input(), clock)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `extraction is deterministic`() {
        val body = "Deadline is 18 October 2026. Meeting on 20 October at 3 PM IST."
        val a = extract(body = body)
        val b = extract(body = body)
        assertEquals(a.items.map { it.startEpochMs to it.type }, b.items.map { it.startEpochMs to it.type })
    }

    @Test
    fun `explanation never contains raw body`() {
        val body = "Secret project deadline is 18 October 2026."
        val result = extract(body = body)
        result.items.forEach {
            assertTrue(!it.explanation.contains("Secret project"))
        }
    }

    @Test
    fun `version is stamped`() {
        val result = extract(body = "Deadline is 18 October 2026.")
        assertEquals(DeterministicTemporalExtractor.VERSION, result.version)
        result.items.forEach {
            assertEquals(DeterministicTemporalExtractor.VERSION, it.version)
        }
    }
}
