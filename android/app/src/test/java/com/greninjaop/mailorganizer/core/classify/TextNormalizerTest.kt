package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [TextNormalizer] (Phase 7 §11). */
class TextNormalizerTest {

    @Test
    fun `case and whitespace are normalized`() {
        // Punctuation is preserved by design (phase §11: don't damage
        // names/identifiers); only case/whitespace normalize.
        assertEquals(
            "your order confirmation!",
            TextNormalizer.normalizeForMatch("Your   ORDER Confirmation!"),
        )
    }

    @Test
    fun `tabs and newlines collapse to single spaces`() {
        assertEquals(
            "a b c",
            TextNormalizer.normalizeForMatch("a\tb\n\nc"),
        )
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertEquals("hello", TextNormalizer.normalizeForMatch("   hello   "))
    }

    @Test
    fun `empty input stays empty`() {
        assertEquals("", TextNormalizer.normalizeForMatch(""))
        assertEquals("", TextNormalizer.normalizeForMatch("   "))
    }

    @Test
    fun `long input is truncated to the bound`() {
        val long = "a".repeat(TextNormalizer.MAX_BODY_CHARS + 500)
        val out = TextNormalizer.normalizeForMatch(long)
        assertEquals(TextNormalizer.MAX_BODY_CHARS, out.length)
    }

    @Test
    fun `subject bound is smaller than body bound`() {
        assertTrue(TextNormalizer.MAX_SUBJECT_CHARS < TextNormalizer.MAX_BODY_CHARS)
        val long = "b".repeat(TextNormalizer.MAX_SUBJECT_CHARS + 10)
        assertEquals(TextNormalizer.MAX_SUBJECT_CHARS, TextNormalizer.normalizeSubject(long).length)
    }

    @Test
    fun `ascii lowercase is locale independent`() {
        // Hand-rolled ASCII fold: 'I' -> 'i' regardless of device locale
        // (Turkish-locale devices must classify identically).
        assertEquals("istanbul", TextNormalizer.asciiLowercase("ISTANBUL"))
        assertEquals("already lower", TextNormalizer.asciiLowercase("already lower"))
    }

    @Test
    fun `non-ascii text survives normalization`() {
        val malayalam = "വാർത്താക്കുറിപ്പ്"
        val out = TextNormalizer.normalizeForMatch(malayalam)
        assertTrue(out.isNotEmpty())
        // NFKC + ascii fold must not destroy the script.
        assertTrue(out.contains("വാർത്ത"))
    }

    @Test
    fun `domainOf extracts and normalizes domains`() {
        assertEquals("example.com", TextNormalizer.domainOf("User@Example.COM"))
        assertEquals("example.com", TextNormalizer.domainOf("user@example.com."))
        assertEquals("", TextNormalizer.domainOf("not-an-address"))
        assertEquals("", TextNormalizer.domainOf("user@"))
    }

    @Test
    fun `normalizeAddress lowercases and trims`() {
        assertEquals(
            "user@example.com",
            TextNormalizer.normalizeAddress("  User@Example.COM  "),
        )
    }
}
