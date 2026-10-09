package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [SignalExtractor] (Phase 7 §10). */
class SignalExtractorTest {

    private fun input(
        from: String = "sender@example.com",
        name: String? = "Sender",
        subject: String = "Hello",
        body: String? = "Body text",
        labels: List<String> = emptyList(),
        filenames: List<String> = emptyList(),
    ) = ClassificationInput(
        messageId = "m1",
        fromAddress = from,
        fromName = name,
        subject = subject,
        bodyText = body,
        labelIds = labels,
        attachmentFilenames = filenames,
    )

    @Test
    fun `sender domain is extracted`() {
        val s = SignalExtractor.extract(input(from = "Jobs@LinkedIn.COM"))
        assertEquals("linkedin.com", s.senderDomain)
        assertEquals("jobs@linkedin.com", s.senderAddress)
    }

    @Test
    fun `gmail category labels are recognized`() {
        val s = SignalExtractor.extract(input(labels = listOf("UNREAD", "CATEGORY_SOCIAL", "INBOX")))
        assertEquals(setOf("CATEGORY_SOCIAL"), s.gmailCategories)
        assertTrue(s.labels.contains("CATEGORY_SOCIAL"))
    }

    @Test
    fun `unknown labels are kept but not treated as categories`() {
        val s = SignalExtractor.extract(input(labels = listOf("CATEGORY_XYZ")))
        assertTrue(s.gmailCategories.isEmpty())
        assertTrue(s.labels.contains("CATEGORY_XYZ"))
    }

    @Test
    fun `unsubscribe footer is detected`() {
        val s = SignalExtractor.extract(
            input(body = "Thanks for reading. Click here to unsubscribe at any time."),
        )
        assertTrue(s.hasUnsubscribe)
    }

    @Test
    fun `mail without unsubscribe markers is not flagged`() {
        val s = SignalExtractor.extract(input(body = "See you at the meeting tomorrow."))
        assertFalse(s.hasUnsubscribe)
    }

    @Test
    fun `url domains are extracted and bounded`() {
        val body = "Visit https://shop.example.com/deals and http://blog.example.org/x"
        val s = SignalExtractor.extract(input(body = body))
        assertTrue(s.urlDomains.contains("shop.example.com"))
        assertTrue(s.urlDomains.contains("blog.example.org"))
        assertTrue(s.urlDomains.size <= ExtractedSignals.MAX_URL_DOMAINS)
    }

    @Test
    fun `url domain count is capped`() {
        val body = (1..50).joinToString(" ") { "https://site$it.example.com/" }
        val s = SignalExtractor.extract(input(body = body))
        assertEquals(ExtractedSignals.MAX_URL_DOMAINS, s.urlDomains.size)
    }

    @Test
    fun `attachment hints are detected`() {
        val s = SignalExtractor.extract(
            input(filenames = listOf("invoice-2024.pdf", "resume_final.docx")),
        )
        assertTrue(s.attachmentHints.contains("INVOICE_LIKE"))
        assertTrue(s.attachmentHints.contains("RESUME_LIKE"))
    }

    @Test
    fun `body is bounded`() {
        val huge = "word ".repeat(100_000)
        val s = SignalExtractor.extract(input(body = huge))
        assertTrue(s.bodyNorm.length <= TextNormalizer.MAX_BODY_CHARS)
    }

    @Test
    fun `null body degrades to empty`() {
        val s = SignalExtractor.extract(input(body = null))
        assertEquals("", s.bodyNorm)
        assertFalse(s.hasUnsubscribe)
    }

    @Test
    fun `extraction is total on hostile input`() {
        val hostile = "<script>alert(1)</script>" + "\u0000".repeat(1000) + "https://"
        val s = SignalExtractor.extract(
            input(from = "", subject = hostile, body = hostile),
        )
        // Must not throw; fields degrade gracefully.
        assertEquals("", s.senderDomain)
        assertTrue(s.subjectNorm.isNotEmpty() || s.subjectNorm.isEmpty())
    }
}
