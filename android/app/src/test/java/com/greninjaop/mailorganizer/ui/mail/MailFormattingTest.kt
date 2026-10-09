package com.greninjaop.mailorganizer.ui.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MailFormattingTest {

    private val now = 1_800_000_000_000L // 2027-01-15T08:00:00Z (fixed for determinism)

    @Test
    fun `unknown timestamp renders empty`() {
        assertEquals("", MailFormatting.relativeTime(0L, now))
        assertEquals("", MailFormatting.relativeTime(-5L, now))
    }

    @Test
    fun `just now`() {
        assertEquals("Now", MailFormatting.relativeTime(now - 30_000, now))
        assertEquals("Now", MailFormatting.relativeTime(now, now))
    }

    @Test
    fun `minutes and hours`() {
        assertEquals("5m", MailFormatting.relativeTime(now - 5 * 60_000, now))
        assertEquals("59m", MailFormatting.relativeTime(now - 59 * 60_000, now))
        assertEquals("2h", MailFormatting.relativeTime(now - 2 * 3_600_000, now))
        assertEquals("23h", MailFormatting.relativeTime(now - 23 * 3_600_000, now))
    }

    @Test
    fun `yesterday and days`() {
        assertEquals("Yesterday", MailFormatting.relativeTime(now - 26 * 3_600_000, now))
        assertEquals("3d", MailFormatting.relativeTime(now - 3 * 86_400_000, now))
        assertEquals("6d", MailFormatting.relativeTime(now - 6 * 86_400_000, now))
    }

    @Test
    fun `older dates show month day, then year`() {
        val fortyDays = MailFormatting.relativeTime(now - 40L * 86_400_000, now)
        // 40 days before 2027-01-15 is 2026-12-06 → includes the year.
        assertTrue("expected year in '$fortyDays'", fortyDays.contains("2026"))
        val tenDays = MailFormatting.relativeTime(now - 10L * 86_400_000, now)
        // 10 days before is 2027-01-05 → same year, no year shown.
        assertTrue("expected month/day in '$tenDays'", tenDays.matches(Regex("[A-Z][a-z]{2} \\d{1,2}")))
    }

    @Test
    fun `future timestamps say now`() {
        assertEquals("Now", MailFormatting.relativeTime(now + 60_000, now))
    }

    @Test
    fun `formatBytes`() {
        assertEquals("", MailFormatting.formatBytes(null))
        assertEquals("", MailFormatting.formatBytes(-1))
        assertEquals("0 B", MailFormatting.formatBytes(0))
        assertEquals("512 B", MailFormatting.formatBytes(512))
        assertEquals("2 KB", MailFormatting.formatBytes(2048))
        assertEquals("1.2 MB", MailFormatting.formatBytes(1_258_291))
        assertEquals("2.0 GB", MailFormatting.formatBytes(2L * 1_024 * 1_024 * 1_024))
    }

    @Test
    fun `avatarInitial`() {
        assertEquals("A", MailFormatting.avatarInitial("Alex Reader", "alex@example.com"))
        assertEquals("P", MailFormatting.avatarInitial(null, "priya@example.com"))
        assertEquals("P", MailFormatting.avatarInitial("  ", "priya@example.com"))
        assertEquals("?", MailFormatting.avatarInitial(null, ""))
        // Unicode-safe: first code point, never crashes.
        assertEquals("പ", MailFormatting.avatarInitial("പ്രിയ", "x@example.com"))
    }

    @Test
    fun `sender and subject fallbacks`() {
        assertEquals("Unknown sender", MailFormatting.senderDisplay(null, ""))
        assertEquals("Unknown sender", MailFormatting.senderDisplay("  ", "   "))
        assertEquals("Google", MailFormatting.senderDisplay("Google", "g@example.com"))
        assertEquals("g@example.com", MailFormatting.senderDisplay(null, "g@example.com"))
        assertEquals("No subject", MailFormatting.subjectDisplay(""))
        assertEquals("No subject", MailFormatting.subjectDisplay("   "))
        assertEquals("Hi", MailFormatting.subjectDisplay("Hi"))
    }
}
