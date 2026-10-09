package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.core.email.HtmlSanitizer

/**
 * Parses *sanitized* email HTML into [BodyBlock]s for Compose rendering
 * (Phase 6, phase §22–24, §29–30).
 *
 * Security posture (defense in depth — the input already passed
 * [HtmlSanitizer], but this layer trusts nothing):
 * - No WebView is involved, so `<script>`, event handlers and
 *   `javascript:` URLs cannot execute — there is simply no JS engine.
 * - Links are re-validated here: only `http`/`https` become clickable.
 * - `<img>` never fetches: it becomes [BodyBlock.ImagePlaceholder].
 * - Inline `style` attributes were stripped by the sanitizer; any
 *   survivors are ignored (no style model exists here).
 *
 * Simplifications (documented, phase §30 "avoid excessive custom rendering"):
 * - Tables are flattened: each cell's text becomes its own paragraph.
 * - Nested lists beyond one level are flattened into the parent list.
 * - Unknown elements are dropped but their text is kept.
 *
 * Total function: never throws. On unexpected input it falls back to
 * plain stripped text.
 */
object SafeHtmlRenderer {

    private val tagRegex =
        Regex("<(/?)([a-zA-Z][a-zA-Z0-9]*)((?:[^\"'>]|\"[^\"]*\"|'[^']*')*)>")
    private val attrRegex =
        Regex("""([a-zA-Z-]+)\s*=\s*("([^"]*)"|'([^']*)'|([^\s>]+))""")

    private val blockTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "pre", "tr", "td", "th", "table", "ul", "ol", "hr",
    )

    fun render(sanitizedHtml: String): List<BodyBlock> {
        return try {
            parse(sanitizedHtml)
        } catch (_: Exception) {
            // Fail closed: hostile input becomes inert plain text.
            val text = HtmlSanitizer.htmlToText(sanitizedHtml).trim()
            if (text.isEmpty()) emptyList()
            else listOf(BodyBlock.Paragraph(listOf(BodyRun(text))))
        }
    }

    // ------------------------------------------------------------------
    // Parser state
    // ------------------------------------------------------------------

    private class Frame {
        val blocks = mutableListOf<BodyBlock>()
        val paraRuns = mutableListOf<BodyRun>()
        var boldDepth = 0
        var italicDepth = 0
        var underlineDepth = 0
        val linkStack = ArrayDeque<String?>()
        var headingLevel = 0
        val headingRuns = mutableListOf<BodyRun>()
        var inPre = false
        val preText = StringBuilder()
        // Single-level list support; deeper nesting flattens into the parent.
        var listOrdered: Boolean? = null
        val listItems = mutableListOf<MutableList<BodyRun>>()
        var inListItem = false
        var quoteDepth = 0
        val quoteRuns = mutableListOf<BodyRun>()

        fun currentRuns(): MutableList<BodyRun> = when {
            inListItem && listItems.isNotEmpty() -> listItems.last()
            quoteDepth > 0 -> quoteRuns
            headingLevel > 0 -> headingRuns
            else -> paraRuns
        }

        fun emit(text: String) {
            if (text.isEmpty()) return
            val link = linkStack.lastOrNull()
            currentRuns().add(
                BodyRun(
                    text = text,
                    bold = boldDepth > 0,
                    italic = italicDepth > 0,
                    underline = underlineDepth > 0,
                    link = link,
                ),
            )
        }

        fun flushParagraph() {
            val runs = paraRuns.toList()
            paraRuns.clear()
            if (runs.any { it.text.isNotBlank() }) blocks.add(BodyBlock.Paragraph(runs))
        }

        fun flushHeading() {
            if (headingLevel > 0) {
                val runs = headingRuns.toList()
                headingRuns.clear()
                val level = headingLevel.coerceIn(1, 6)
                headingLevel = 0
                if (runs.any { it.text.isNotBlank() }) {
                    blocks.add(BodyBlock.Heading(level, runs))
                }
            }
        }

        fun flushList() {
            val ordered = listOrdered
            if (ordered != null) {
                val items = listItems.map { it.toList() }
                    .filter { item -> item.any { it.text.isNotBlank() } }
                listItems.clear()
                inListItem = false
                listOrdered = null
                if (items.isNotEmpty()) blocks.add(BodyBlock.ListBlock(ordered, items))
            }
        }

        fun flushQuote() {
            if (quoteDepth > 0) {
                val runs = quoteRuns.toList()
                quoteRuns.clear()
                quoteDepth = 0
                if (runs.any { it.text.isNotBlank() }) blocks.add(BodyBlock.Quote(runs))
            }
        }
    }

    // ------------------------------------------------------------------
    // Parse
    // ------------------------------------------------------------------

    private fun parse(html: String): List<BodyBlock> {
        val f = Frame()
        var pos = 0
        while (pos < html.length) {
            val m = tagRegex.find(html, pos)
            if (m == null) {
                emitText(f, html.substring(pos))
                break
            }
            if (m.range.first > pos) emitText(f, html.substring(pos, m.range.first))
            handleTag(f, m.groupValues[1] == "/", m.groupValues[2].lowercase(), m.groupValues[3])
            pos = m.range.last + 1
        }
        f.flushParagraph()
        f.flushHeading()
        f.flushList()
        f.flushQuote()
        if (f.inPre) {
            val t = f.preText.toString()
            if (t.isNotBlank()) f.blocks.add(BodyBlock.Code(t.trim('\n')))
        }
        return f.blocks
    }

    private fun emitText(f: Frame, raw: String) {
        if (f.inPre) {
            f.preText.append(decodeEntities(raw))
            return
        }
        // Collapse source-formatting whitespace; keep a single space so words
        // from adjacent inline elements don't glue together.
        val collapsed = decodeEntities(raw).replace(Regex("[\\s\\u00A0]+"), " ")
        if (collapsed.isBlank()) {
            // A whitespace-only node between two inline elements is a word
            // separator (e.g. "<b>a</b> <b>b</b>" → "a b"); keep one space so
            // words don't glue. Between block tags the current run list is
            // empty, so nothing is emitted and no stray paragraph appears.
            if (f.currentRuns().isNotEmpty()) f.emit(" ")
            return
        }
        f.emit(collapsed)
    }

    private fun handleTag(f: Frame, closing: Boolean, name: String, attrs: String) {
        when (name) {
            "br" -> if (!closing) f.emit("\n")
            "hr" -> if (!closing) {
                f.flushParagraph()
            }
            "p", "div", "tr", "td", "th", "table", "section", "article", "header", "footer" ->
                if (!closing) f.flushParagraph() else f.flushParagraph()
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                if (!closing) {
                    f.flushParagraph()
                    f.flushHeading()
                    f.headingLevel = name[1].digitToInt()
                } else {
                    f.flushHeading()
                }
            }
            "b", "strong" -> f.boldDepth = (f.boldDepth + if (closing) -1 else 1).coerceAtLeast(0)
            "i", "em" -> f.italicDepth = (f.italicDepth + if (closing) -1 else 1).coerceAtLeast(0)
            "u" -> f.underlineDepth =
                (f.underlineDepth + if (closing) -1 else 1).coerceAtLeast(0)
            "a" -> if (!closing) {
                f.linkStack.addLast(safeLink(attrs))
            } else if (f.linkStack.isNotEmpty()) {
                f.linkStack.removeLast()
            }
            "ul", "ol" -> if (!closing) {
                f.flushParagraph()
                // Nested lists flatten into the parent (documented).
                if (f.listOrdered == null) {
                    f.listOrdered = name == "ol"
                }
            } else {
                f.flushList()
            }
            "li" -> if (!closing) {
                if (f.listOrdered != null) {
                    f.listItems.add(mutableListOf())
                    f.inListItem = true
                } else {
                    f.flushParagraph()
                }
            } else {
                f.inListItem = false
            }
            "blockquote" -> if (!closing) {
                f.flushParagraph()
                f.quoteDepth++
            } else {
                f.flushQuote()
            }
            "pre" -> if (!closing) {
                f.flushParagraph()
                f.inPre = true
            } else if (f.inPre) {
                f.inPre = false
                val t = f.preText.toString()
                f.preText.clear()
                if (t.isNotBlank()) f.blocks.add(BodyBlock.Code(t.trim('\n')))
            }
            "img" -> if (!closing) {
                f.flushParagraph()
                val alt = attrValue(attrs, "alt")?.take(120).orEmpty()
                f.blocks.add(BodyBlock.ImagePlaceholder(alt))
            }
            // Dropped silently (content already removed by the sanitizer or
            // carries no display value): script/style already gone; keep the
            // rest inert.
            else -> Unit
        }
    }

    /** Returns the href when it is a safe http(s) URL, else null. */
    private fun safeLink(attrs: String): String? {
        val href = attrValue(attrs, "href")?.trim() ?: return null
        if (href.isEmpty()) return null
        val scheme = href.substringBefore(':').lowercase()
        // No colon → relative URL: not clickable (never guess a base).
        if (!href.contains(':')) return null
        if (scheme != "http" && scheme != "https") return null
        if (href.any { it.isWhitespace() || it.code < 0x20 }) return null
        return href
    }

    private fun attrValue(attrs: String, name: String): String? {
        for (m in attrRegex.findAll(attrs)) {
            if (m.groupValues[1].lowercase() == name) {
                return m.groupValues[3].ifEmpty { m.groupValues[4] }
                    .ifEmpty { m.groupValues[5] }
                    .ifEmpty { null }
            }
        }
        return null
    }

    private val namedEntities = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"",
        "apos" to "'", "#39" to "'", "nbsp" to " ",
    )

    private fun decodeEntities(s: String): String {
        if (!s.contains('&')) return s
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '&') {
                val semi = s.indexOf(';', i + 1).takeIf { it in (i + 1)..(i + 12) }
                if (semi != null) {
                    val name = s.substring(i + 1, semi)
                    val decoded = namedEntities[name]
                        ?: if (name.startsWith("#x") || name.startsWith("#X")) {
                            name.substring(2).toIntOrNull(16)?.let { code ->
                                if (code in 0x20..0x10FFFF) code.toChar().toString() else null
                            }
                        } else if (name.startsWith("#")) {
                            name.substring(1).toIntOrNull()?.let { code ->
                                if (code in 0x20..0x10FFFF) code.toChar().toString() else null
                            }
                        } else null
                    if (decoded != null) {
                        out.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }
}
