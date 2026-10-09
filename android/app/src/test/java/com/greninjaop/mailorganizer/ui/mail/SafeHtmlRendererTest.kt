package com.greninjaop.mailorganizer.ui.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeHtmlRendererTest {

    private fun paragraphText(blocks: List<BodyBlock>): String =
        blocks.filterIsInstance<BodyBlock.Paragraph>()
            .joinToString("\n") { p -> p.runs.joinToString("") { it.text } }

    @Test
    fun `paragraphs and line breaks`() {
        val blocks = SafeHtmlRenderer.render("<p>Hello</p><p>World</p>")
        assertEquals(2, blocks.size)
        assertEquals("Hello", paragraphText(listOf(blocks[0])))
        assertEquals("World", paragraphText(listOf(blocks[1])))
    }

    @Test
    fun `headings keep level`() {
        val blocks = SafeHtmlRenderer.render("<h1>Title</h1><h3>Sub</h3>")
        val h1 = blocks[0] as BodyBlock.Heading
        val h3 = blocks[1] as BodyBlock.Heading
        assertEquals(1, h1.level)
        assertEquals(1, h1.runs.size)
        assertEquals(3, h3.level)
        assertEquals("Title", h1.runs.joinToString("") { it.text })
    }

    @Test
    fun `bold italic and underline runs`() {
        val blocks = SafeHtmlRenderer.render("<p><b>bold</b> <i>ital</i> <u>under</u> plain</p>")
        val runs = (blocks[0] as BodyBlock.Paragraph).runs
        // Whitespace-only nodes between inline elements are kept as single
        // spaces so words don't glue together.
        assertEquals(6, runs.size)
        assertTrue(runs[0].bold)
        assertTrue(runs[2].italic)
        assertTrue(runs[4].underline)
        assertEquals("bold ital under plain", runs.joinToString("") { it.text })
    }

    @Test
    fun `https links become clickable, javascript links do not`() {
        val blocks = SafeHtmlRenderer.render(
            "<p><a href=\"https://example.com/x\">ok</a> " +
                "<a href=\"javascript:alert(1)\">evil</a> " +
                "<a href=\"/relative\">rel</a></p>",
        )
        val runs = (blocks[0] as BodyBlock.Paragraph).runs
        val ok = runs.first { it.text == "ok" }
        val evil = runs.first { it.text == "evil" }
        val rel = runs.first { it.text == "rel" }
        assertEquals("https://example.com/x", ok.link)
        assertNull(evil.link)
        assertNull(rel.link)
    }

    @Test
    fun `lists`() {
        val blocks = SafeHtmlRenderer.render("<ul><li>a</li><li>b</li></ul><ol><li>c</li></ol>")
        val ul = blocks[0] as BodyBlock.ListBlock
        val ol = blocks[1] as BodyBlock.ListBlock
        assertEquals(false, ul.ordered)
        assertEquals(2, ul.items.size)
        assertEquals("a", ul.items[0].joinToString("") { it.text })
        assertEquals(true, ol.ordered)
        assertEquals("c", ol.items[0].joinToString("") { it.text })
    }

    @Test
    fun `images become placeholders, never fetched`() {
        val blocks = SafeHtmlRenderer.render("<p>x</p><img src=\"https://e.com/p.png\" alt=\"Banner\">")
        val ph = blocks[1] as BodyBlock.ImagePlaceholder
        assertEquals("Banner", ph.alt)
    }

    @Test
    fun `quote and code`() {
        val blocks = SafeHtmlRenderer.render(
            "<blockquote>quoted</blockquote><pre>code\n  indented</pre>",
        )
        val q = blocks[0] as BodyBlock.Quote
        assertEquals("quoted", q.runs.joinToString("") { it.text })
        val c = blocks[1] as BodyBlock.Code
        assertTrue(c.text.contains("indented"))
    }

    @Test
    fun `entities decoded`() {
        val blocks = SafeHtmlRenderer.render("<p>a &amp; b &lt;c&gt; &#65; &#x42;</p>")
        assertEquals("a & b <c> A B", paragraphText(blocks))
    }

    @Test
    fun `hostile input never throws and stays inert`() {
        // Full production pipeline: sanitizer first, then the renderer.
        val hostile = "<script>alert(1)</script><p onclick=\"x()\">t</p>" +
            "<a href=\"JaVaScRiPt:evil()\">e</a><style>body{display:none}</style>" +
            "<img src=x onerror=alert(1)>"
        val blocks = SafeHtmlRenderer.render(
            com.greninjaop.mailorganizer.core.email.HtmlSanitizer.sanitize(hostile),
        )
        val allText = blocks.joinToString(" ") {
            when (it) {
                is BodyBlock.Paragraph -> it.runs.joinToString("") { r -> r.text }
                is BodyBlock.ImagePlaceholder -> "IMG"
                else -> ""
            }
        }
        assertTrue("script text leaked: $allText", !allText.contains("alert(1)"))
        // No clickable dangerous links anywhere.
        val links = blocks.filterIsInstance<BodyBlock.Paragraph>()
            .flatMap { it.runs }.mapNotNull { it.link }
        assertTrue("dangerous link survived: $links", links.none {
            it.lowercase().startsWith("javascript")
        })
    }

    @Test
    fun `unclosed and malformed html does not crash`() {
        val blocks = SafeHtmlRenderer.render("<p>oops <b>bold <div>div")
        assertTrue(blocks.isNotEmpty())
        val blocks2 = SafeHtmlRenderer.render("<<<<<>>>>>")
        assertTrue(blocks2.isEmpty() || blocks2.all { it is BodyBlock.Paragraph })
    }

    @Test
    fun `tables flatten to paragraphs`() {
        val blocks = SafeHtmlRenderer.render("<table><tr><td>a</td><td>b</td></tr></table>")
        val text = paragraphText(blocks)
        assertTrue(text.contains("a") && text.contains("b"))
    }

    @Test
    fun `empty input renders empty`() {
        assertTrue(SafeHtmlRenderer.render("").isEmpty())
        assertTrue(SafeHtmlRenderer.render("   ").isEmpty())
    }
}
