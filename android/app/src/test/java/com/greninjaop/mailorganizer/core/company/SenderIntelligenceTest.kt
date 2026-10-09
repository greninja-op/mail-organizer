package com.greninjaop.mailorganizer.core.company

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for [SenderIntelligence] pure heuristics (Phase 8). */
class SenderIntelligenceTest {

    @Test
    fun `email normalization is locale-independent lowercase`() {
        assertEquals(
            "user@example.com",
            SenderIntelligence.normalizeEmail("  User@Example.COM "),
        )
    }

    @Test
    fun `recurring threshold is documented and stable`() {
        assertEquals(3, SenderIntelligence.RECURRING_SENDER_THRESHOLD)
        assertFalse(SenderIntelligence.isRecurring(0))
        assertFalse(SenderIntelligence.isRecurring(2))
        assertTrue(SenderIntelligence.isRecurring(3))
        assertTrue(SenderIntelligence.isRecurring(10_000))
    }

    @Test
    fun `nextCount saturates instead of overflowing`() {
        assertEquals(1, SenderIntelligence.nextCount(0))
        assertEquals(4, SenderIntelligence.nextCount(3))
        assertEquals(Int.MAX_VALUE, SenderIntelligence.nextCount(Int.MAX_VALUE))
    }

    @Test
    fun `sender profile exposes recurring honestly`() {
        val profile = SenderProfile(
            normalizedEmail = "a@example.com",
            displayName = null,
            domain = "example.com",
            messageCount = 2,
            firstSeenEpochMs = 1L,
            lastSeenEpochMs = 2L,
        )
        assertFalse(profile.isRecurring)
        assertTrue(profile.copy(messageCount = 3).isRecurring)
    }

    @Test
    fun `domainOf handles malformed input totally`() {
        assertEquals("", SenderIntelligence.domainOf(""))
        assertEquals("", SenderIntelligence.domainOf("no-at-sign"))
        assertEquals("example.com", SenderIntelligence.domainOf("u@example.com"))
    }
}
