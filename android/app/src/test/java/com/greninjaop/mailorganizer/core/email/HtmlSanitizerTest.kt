package com.greninjaop.mailorganizer.core.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Security tests for [HtmlSanitizer] (Phase 5): untrusted email HTML in, safe HTML out. */
class HtmlSanitizerTest {

    @Test
    fun `script elements and their content are removed`() {
        val out = HtmlSanitizer.sanitize("<p>Hi</p><script>alert('xss')</script><p>Bye</p>")
        assertFalse(out.contains("script", ignoreCase = true))
        assertFalse(out.contains("alert"))
        assertTrue(out.contains("Hi"))
        assertTrue(out.contains("Bye"))
    }

    @Test
    fun `style iframe object embed elements are removed`() {
        val html = "<style>.x{color:red}</style>" +
            "<iframe src=\"http://evil.test\"></iframe>" +
            "<object data=\"x\"></object><p>ok</p>"
        val out = HtmlSanitizer.sanitize(html)
        assertFalse(out.contains("iframe", ignoreCase = true))
        assertFalse(out.contains("object", ignoreCase = true))
        assertFalse(out.contains(".x{color:red}"))
        assertTrue(out.contains("ok"))
    }

    @Test
    fun `event handler attributes are stripped`() {
        val out = HtmlSanitizer.sanitize(
            "<img src=\"http://a.test/i.png\" onerror=\"alert(1)\"><a href=\"http://a.test\" onclick='x()'>t</a>",
        )
        assertFalse(out.contains("onerror", ignoreCase = true))
        assertFalse(out.contains("onclick", ignoreCase = true))
        assertTrue(out.contains("http://a.test/i.png"))
    }

    @Test
    fun `javascript urls are neutralized`() {
        val out = HtmlSanitizer.sanitize("<a href=\"javascript:alert(1)\">click</a>")
        assertFalse(out.contains("javascript:", ignoreCase = true))
        assertTrue(out.contains("click"))
    }

    @Test
    fun `data urls are neutralized`() {
        val out = HtmlSanitizer.sanitize("<a href=\"data:text/html,<script>alert(1)</script>\">x</a>")
        assertFalse(out.contains("data:text/html"))
    }

    @Test
    fun `safe markup is preserved`() {
        val html = "<p>Hello <b>World</b></p><a href=\"https://example.test\">link</a>"
        val out = HtmlSanitizer.sanitize(html)
        assertTrue(out.contains("<p>"))
        assertTrue(out.contains("<b>World</b>"))
        assertTrue(out.contains("https://example.test"))
    }

    @Test
    fun `html comments are stripped`() {
        val out = HtmlSanitizer.sanitize("a<!--[if mso]>x<![endif]-->b")
        assertEquals("ab", out)
    }

    @Test
    fun `inline style attributes are stripped`() {
        val out = HtmlSanitizer.sanitize(
            "<div style=\"position:fixed;top:0\">overlay</div>",
        )
        assertFalse(out.contains("position:fixed"))
        assertTrue(out.contains("overlay"))
    }

    // ---- htmlToText ----

    @Test
    fun `htmlToText converts blocks to line breaks`() {
        val text = HtmlSanitizer.htmlToText("<p>One</p><p>Two</p>")
        assertTrue(text.contains("One"))
        assertTrue(text.contains("Two"))
        assertTrue(text.indexOf("One") < text.indexOf("Two"))
    }

    @Test
    fun `htmlToText never leaks script content`() {
        val text = HtmlSanitizer.htmlToText("<p>Hi</p><script>secret()</script>")
        assertFalse(text.contains("secret"))
        assertTrue(text.contains("Hi"))
    }

    @Test
    fun `htmlToText decodes entities`() {
        assertEquals("a & b <c>", HtmlSanitizer.htmlToText("a &amp; b &lt;c&gt;"))
        assertEquals("it's", HtmlSanitizer.htmlToText("it&#39;s"))
        assertEquals("A", HtmlSanitizer.htmlToText("&#65;"))
    }

    @Test
    fun `htmlToText collapses whitespace`() {
        assertEquals("a b", HtmlSanitizer.htmlToText("<p>  a   \n  b </p>"))
    }

    @Test
    fun `sanitize is total on hostile input`() {
        // Unclosed tags, lone brackets — must not throw or hang.
        val out = HtmlSanitizer.sanitize("<<<<<script<script>alert(1)")
        assertFalse(out.contains("alert"))
    }
}
