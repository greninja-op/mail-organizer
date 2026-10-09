package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Robustness tests (Phase 7 §47): hostile, malformed, and Unicode-heavy
 * input must never crash the classifier or execute content.
 */
class ClassificationRobustnessTest {

    private fun resultFor(i: ClassificationInput): ClassificationResult =
        DeterministicClassifier.classify(i) { 1_700_000_000_000L }

    private fun input(
        from: String = "sender@example.com",
        subject: String = "",
        body: String? = null,
    ) = ClassificationInput(
        messageId = "m1",
        fromAddress = from,
        fromName = null,
        subject = subject,
        bodyText = body,
    )

    @Test
    fun `script-like body is inert`() {
        val r = resultFor(
            input(
                subject = "Hi",
                body = "<script>alert('xss')</script><img src=x onerror=alert(1)>",
            ),
        )
        // Must not throw; classification treats it as plain text.
        assertTrue(r.classifierVersion == DeterministicClassifier.VERSION)
    }

    @Test
    fun `javascript urls are inert`() {
        val r = resultFor(
            input(body = "click javascript:alert(1) and data:text/html,<script>"),
        )
        assertTrue(r.matchedSignals.none { it.detail.contains("alert(1)") })
    }

    @Test
    fun `huge body is bounded and fast`() {
        val huge = "lorem ipsum ".repeat(200_000) // ~2.4MB
        val start = System.nanoTime()
        val r = resultFor(input(subject = "Big", body = huge))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("classification took ${ms}ms, expected < 2000ms", ms < 2000)
        assertEquals(1_700_000_000_000L, r.classifiedAtEpochMs)
    }

    @Test
    fun `unicode heavy input is safe`() {
        val uni = "🎉".repeat(5000) + "വാർത്താക്കുറിപ്പ്".repeat(500) + "é".repeat(5000)
        val r = resultFor(input(subject = uni, body = uni))
        assertTrue(r.explanation.isNotBlank())
    }

    @Test
    fun `empty everything degrades to unclassified`() {
        val r = resultFor(input(from = "", subject = "", body = ""))
        assertEquals(ClassifierCategory.UNCLASSIFIED, r.category)
    }

    @Test
    fun `null body degrades gracefully`() {
        val r = resultFor(input(subject = "Your verification code", body = null))
        // Subject signal alone still works.
        assertEquals(ClassifierCategory.SECURITY, r.category)
    }

    @Test
    fun `control characters do not break extraction`() {
        val r = resultFor(
            input(
                subject = "Hi\u0000\u0001\u0002",
                body = "Body\u001Fwith\u007Fcontrols",
            ),
        )
        assertEquals(DeterministicClassifier.VERSION, r.classifierVersion)
    }

    @Test
    fun `regex-heavy text cannot cause catastrophic backtracking`() {
        // Pathological input for naive regexes: long runs that used to
        // trigger catastrophic backtracking in simpler matchers.
        val evil = "a".repeat(50_000) + "!"
        val start = System.nanoTime()
        resultFor(input(subject = evil, body = evil))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("took ${ms}ms, expected < 2000ms", ms < 2000)
    }

    @Test
    fun `malformed address degrades`() {
        val r = resultFor(input(from = "@@@", subject = "Your invoice"))
        // No domain signal, but the invoice phrase still classifies.
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
    }
}
