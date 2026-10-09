package com.greninjaop.mailorganizer.core.email

import java.time.Instant
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parser tests (Phase 5): raw Gmail payloads → canonical [EmailMessage]. */
class EmailParserTest {

    // ---- fixtures ----

    private fun b64(text: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(text.toByteArray(Charsets.UTF_8))

    private fun textPart(
        text: String,
        mime: String = "text/plain",
        headers: List<RawGmailHeader> = listOf(
            RawGmailHeader("Content-Type", "$mime; charset=UTF-8"),
        ),
    ): RawGmailPart = RawGmailPart(
        mimeType = mime,
        filename = null,
        headers = headers,
        body = RawGmailBody(data = b64(text), attachmentId = null, size = text.length.toLong()),
    )

    private fun headers(vararg pairs: Pair<String, String>) =
        pairs.map { RawGmailHeader(it.first, it.second) }

    private fun simpleMessage(
        hdrs: List<RawGmailHeader>,
        payload: RawGmailPart?,
        snippet: String? = "snippet!",
        internalDateMs: Long? = 1_700_000_000_000L,
    ) = RawGmailMessage(
        id = "m-1",
        threadId = "t-1",
        labelIds = listOf("INBOX", "UNREAD"),
        snippet = snippet,
        internalDateMs = internalDateMs,
        // Gmail API carries message headers on the top-level payload part.
        payload = payload?.copy(headers = hdrs + payload.headers),
    )

    // ---- basic parsing ----

    @Test
    fun `simple text message parses all fields`() {
        val raw = simpleMessage(
            headers(
                "From" to "\"Jane Doe\" <jane@example.test>",
                "To" to "me@example.test",
                "Subject" to "Hello there",
                "Date" to "Wed, 8 Oct 2026 14:30:00 +0530",
                "Message-ID" to "<abc123@example.test>",
            ),
            RawGmailPart(
                mimeType = "text/plain",
                filename = null,
                headers = headers("Content-Type" to "text/plain; charset=UTF-8"),
                body = RawGmailBody(b64("Hello world"), null, 11),
            ),
        )
        val msg = EmailParser.parse(raw)
        assertEquals("m-1", msg.gmailId)
        assertEquals("jane@example.test", msg.from?.address)
        assertEquals("Jane Doe", msg.from?.name)
        assertEquals(listOf("me@example.test"), msg.to.map { it.address })
        assertEquals("Hello there", msg.subject)
        assertEquals("<abc123@example.test>", msg.messageIdHeader)
        assertEquals("Hello world", msg.bodyText)
        assertNull(msg.bodyHtml)
        assertEquals("snippet!", msg.snippet)
        assertEquals(
            Instant.parse("2026-10-08T09:00:00Z").toEpochMilli(),
            msg.dateEpochMs,
        )
    }

    @Test
    fun `multipart alternative prefers plain text but keeps sanitized html`() {
        val raw = simpleMessage(
            headers("From" to "a@example.test", "Subject" to "Hi"),
            RawGmailPart(
                mimeType = "multipart/alternative",
                filename = null,
                parts = listOf(
                    textPart("plain body"),
                    textPart(
                        "<p>html body</p><script>evil()</script>",
                        mime = "text/html",
                    ),
                ),
            ),
        )
        val msg = EmailParser.parse(raw)
        assertEquals("plain body", msg.bodyText)
        assertTrue(msg.bodyHtml!!.contains("html body"))
        assertFalse(msg.bodyHtml!!.contains("evil"))
    }

    @Test
    fun `html-only message falls back to text extraction`() {
        val raw = simpleMessage(
            headers("From" to "a@example.test", "Subject" to "Hi"),
            RawGmailPart(
                mimeType = "multipart/alternative",
                filename = null,
                parts = listOf(
                    textPart("<p>Hello <b>there</b></p>", mime = "text/html"),
                ),
            ),
        )
        val msg = EmailParser.parse(raw)
        assertEquals("Hello there", msg.bodyText)
        assertTrue(msg.bodyHtml!!.contains("<p>"))
    }

    @Test
    fun `attachments are detected as metadata only`() {
        val raw = simpleMessage(
            headers("From" to "a@example.test", "Subject" to "files"),
            RawGmailPart(
                mimeType = "multipart/mixed",
                filename = null,
                parts = listOf(
                    textPart("see attached"),
                    RawGmailPart(
                        mimeType = "application/pdf",
                        filename = "doc.pdf",
                        body = RawGmailBody(
                            data = null,
                            attachmentId = "att-9",
                            size = 1234,
                        ),
                    ),
                ),
            ),
        )
        val msg = EmailParser.parse(raw)
        assertEquals("see attached", msg.bodyText)
        assertEquals(1, msg.attachments.size)
        assertEquals("doc.pdf", msg.attachments[0].filename)
        assertEquals("application/pdf", msg.attachments[0].mimeType)
        assertEquals(1234L, msg.attachments[0].sizeBytes)
        assertEquals("att-9", msg.attachments[0].attachmentId)
    }

    // ---- headers ----

    @Test
    fun `rfc2047 encoded subject and from name are decoded`() {
        val raw = simpleMessage(
            headers(
                "From" to "=?UTF-8?B?SmFuZSBEb2U=?= <jane@example.test>",
                "Subject" to "=?UTF-8?Q?Hello_=E2=9C=93?=",
            ),
            textPart("x"),
        )
        val msg = EmailParser.parse(raw)
        assertEquals("Jane Doe", msg.from?.name)
        assertEquals("Hello ✓", msg.subject)
    }

    @Test
    fun `multiple addresses parse`() {
        val raw = simpleMessage(
            headers(
                "From" to "a@example.test",
                "To" to "\"Doe, John\" <john@example.test>, jane@example.test",
                "Cc" to "boss@example.test",
            ),
            textPart("x"),
        )
        val msg = EmailParser.parse(raw)
        assertEquals(
            listOf("john@example.test", "jane@example.test"),
            msg.to.map { it.address },
        )
        assertEquals("Doe, John", msg.to[0].name)
        assertEquals(listOf("boss@example.test"), msg.cc.map { it.address })
    }

    @Test
    fun `header lookup is case-insensitive`() {
        val raw = simpleMessage(
            headers("fRoM" to "a@example.test", "sUbJeCt" to "Case"),
            textPart("x"),
        )
        val msg = EmailParser.parse(raw)
        assertEquals("a@example.test", msg.from?.address)
        assertEquals("Case", msg.subject)
    }

    @Test
    fun `bad date falls back to internalDate then zero`() {
        val withInternal = simpleMessage(
            headers("From" to "a@example.test", "Date" to "not a date"),
            textPart("x"),
            internalDateMs = 1_700_000_111_000L,
        )
        assertEquals(1_700_000_111_000L, EmailParser.parse(withInternal).dateEpochMs)

        val noDate = simpleMessage(
            headers("From" to "a@example.test"),
            textPart("x"),
            internalDateMs = null,
        )
        assertEquals(0L, EmailParser.parse(noDate).dateEpochMs)
    }

    @Test
    fun `snippet falls back to generated text`() {
        val raw = simpleMessage(
            headers("From" to "a@example.test", "Subject" to "s"),
            textPart("This is the body text used for snippet generation."),
            snippet = null,
        )
        val msg = EmailParser.parse(raw)
        assertTrue(msg.snippet.startsWith("This is the body"))
    }

    // ---- robustness: parser is total ----

    @Test
    fun `null payload degrades gracefully without throwing`() {
        // With no payload there are no headers either (Gmail API shape) —
        // identity fields survive, content degrades to empty.
        val raw = simpleMessage(
            headers("From" to "a@example.test"),
            payload = null,
            snippet = null,
            internalDateMs = null,
        )
        val msg = EmailParser.parse(raw)
        assertEquals("m-1", msg.gmailId)
        assertEquals("t-1", msg.gmailThreadId)
        assertNull(msg.from)
        assertNull(msg.bodyText)
        assertEquals("", msg.snippet)
        assertEquals(listOf("INBOX", "UNREAD"), msg.labelIds)
    }

    @Test
    fun `corrupt base64 body does not throw`() {
        val raw = simpleMessage(
            headers("From" to "a@example.test", "Subject" to "s"),
            RawGmailPart(
                mimeType = "text/plain",
                filename = null,
                body = RawGmailBody(data = "!!!not-base64!!!", attachmentId = null, size = 16),
            ),
        )
        val msg = EmailParser.parse(raw)
        assertNull(msg.bodyText)
    }

    @Test
    fun `unknown charset falls back to utf-8`() {
        val raw = simpleMessage(
            headers("From" to "a@example.test", "Subject" to "s"),
            textPart(
                "hello",
                headers = listOf(RawGmailHeader("Content-Type", "text/plain; charset=X-BOGUS-99")),
            ),
        )
        assertEquals("hello", EmailParser.parse(raw).bodyText)
    }

    @Test
    fun `determinism - same input twice gives equal output`() {
        val raw = simpleMessage(
            headers(
                "From" to "\"Jane\" <j@example.test>",
                "Subject" to "=?UTF-8?B?SGk=?=",
                "Date" to "Wed, 8 Oct 2026 14:30:00 +0530",
            ),
            RawGmailPart(
                mimeType = "multipart/alternative",
                filename = null,
                parts = listOf(textPart("a"), textPart("<p>b</p>", mime = "text/html")),
            ),
        )
        assertEquals(EmailParser.parse(raw), EmailParser.parse(raw))
    }
}
