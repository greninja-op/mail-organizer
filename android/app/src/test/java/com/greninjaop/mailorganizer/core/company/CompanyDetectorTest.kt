package com.greninjaop.mailorganizer.core.company

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Detection tests for [CompanyDetector] (Phase 8).
 *
 * All fixtures are synthetic — no real user mail. Detection must be
 * total (never throws) and deterministic (same address → same company).
 */
class CompanyDetectorTest {

    private fun detect(from: String, name: String? = null) =
        CompanyDetector.detect(CompanyDetectionInput(from, name))

    // ---- Canonicalization ----

    @Test
    fun `subdomains canonicalize to one company`() {
        val a = detect("noreply@mail.google.com")
        val b = detect("support@google.com")
        assertNotNull(a)
        assertNotNull(b)
        assertEquals(a!!.companyId, b!!.companyId)
        assertEquals("google.com", a.normalizedDomain)
        assertEquals("Google", a.canonicalName)
    }

    @Test
    fun `mailing subdomains are stripped`() {
        val d = detect("deals@news.amazon.com")
        assertNotNull(d)
        assertEquals("amazon.com", d!!.normalizedDomain)
        assertEquals("Amazon", d.canonicalName)
    }

    @Test
    fun `detection is case-insensitive`() {
        val a = detect("Team@GitHub.COM")
        val b = detect("team@github.com")
        assertNotNull(a)
        assertNotNull(b)
        assertEquals(a!!.companyId, b!!.companyId)
    }

    @Test
    fun `well-known brands get display names`() {
        assertEquals("LinkedIn", detect("jobs@linkedin.com")!!.canonicalName)
        assertEquals("Facebook", detect("notify@facebook.com")!!.canonicalName)
    }

    @Test
    fun `unknown domains derive names from the domain label`() {
        val d = detect("hello@acme-corp.io")
        assertNotNull(d)
        assertEquals("acme-corp.io", d!!.normalizedDomain)
        assertEquals("Acme-corp", d.canonicalName)
    }

    @Test
    fun `multi-label public suffixes keep three labels`() {
        val d = detect("news@mail.bbc.co.uk")
        assertNotNull(d)
        assertEquals("bbc.co.uk", d!!.normalizedDomain)
    }

    @Test
    fun `company id is stable and deterministic`() {
        val a = detect("a@shop.example.com")!!.companyId
        val b = detect("b@example.com")!!.companyId
        assertEquals(a, b)
        assertTrue(a.startsWith("co:"))
    }

    // ---- Personal senders are not companies ----

    @Test
    fun `free mailbox domains return null`() {
        assertNull(detect("jane@gmail.com"))
        assertNull(detect("jane@YAHOO.COM"))
        assertNull(detect("jane@outlook.com"))
        assertNull(detect("jane@icloud.com"))
        assertNull(detect("jane@proton.me"))
    }

    @Test
    fun `corporate domains are not treated as personal`() {
        // A company's own Google Workspace domain is a company.
        assertNotNull(detect("team@acme.com"))
    }

    // ---- Totality ----

    @Test
    fun `malformed addresses return null instead of throwing`() {
        assertNull(detect(""))
        assertNull(detect("   "))
        assertNull(detect("not-an-address"))
        assertNull(detect("@"))
        assertNull(detect("user@"))
        assertNull(detect("@example.com"))
        assertNull(detect("user@exa mple.com"))
    }

    @Test
    fun `known subdomain of canonical domain is recorded`() {
        val d = detect("x@mail.example.com")
        assertNotNull(d)
        assertTrue(d!!.knownDomains.contains("mail.example.com"))
    }
}
