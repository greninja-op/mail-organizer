package com.greninjaop.mailorganizer.core.email

/**
 * HTML sanitizer for untrusted email content (Phase 5).
 *
 * Email HTML is attacker-controlled input. This sanitizer enforces:
 * - No script execution: `<script>`/`<style>`/`<iframe>`/`<object>`/`<embed>`/
 *   `<applet>`/`<form>`/`<meta>`/`<link>`/`<base>`/`<frame>` elements (and their
 *   content) are removed entirely.
 * - No event-handler attributes (`on*`) and no inline `style` attributes
 *   (blocks `position:fixed` overlay tricks and `expression()`-style vectors).
 * - No `javascript:` / `vbscript:` / `data:` URLs in linkable attributes.
 * - HTML comments are stripped.
 *
 * Everything else (paragraphs, links with safe schemes, tables, images with
 * `http(s):` sources, …) is preserved so legitimate mail still renders.
 *
 * Pure Kotlin, no dependencies — the patterns are deliberately simple and
 * bounded to avoid catastrophic backtracking on hostile input.
 *
 * Also provides [htmlToText]: tag-stripping fallback used when a message has
 * no `text/plain` part. Script/style content never leaks into the text.
 */
object HtmlSanitizer {

    // Elements whose *content* is also removed (case-insensitive, DOTALL).
    private val dangerousElements = Regex(
        "<(script|style|iframe|object|embed|applet|form|frameset|frame|noframes|noscript)" +
            "\\b[^>]*>.*?</\\1\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    // Void/head elements with no meaningful content to keep.
    private val voidElements = Regex(
        "<(meta|link|base|title)\\b[^>]*/?>",
        RegexOption.IGNORE_CASE,
    )
    // Unclosed executable elements: browsers treat everything after `<script`
    // as script content, so remove to end of input (applied after paired removal).
    private val unclosedExecutable = Regex(
        "<(script|iframe|object|embed|applet)\\b.*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    // Leftover tag fragments (e.g. `<style` without `>`) after paired removal.
    private val danglingTags = Regex(
        "<(style|form|meta|link|base|frameset|frame|noscript)\\b[^>]*>?",
        RegexOption.IGNORE_CASE,
    )
    private val comments = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    // Event handlers: onload="…", onclick='…', onerror=… (unquoted).
    private val eventHandlers = Regex(
        "\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        RegexOption.IGNORE_CASE,
    )
    private val styleAttributes = Regex(
        "\\s+style\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        RegexOption.IGNORE_CASE,
    )
    // javascript:/vbscript:/data: in href/src/action/etc. — the attribute is dropped.
    private val dangerousUrls = Regex(
        "\\s+(href|src|action|cite|background|poster|data)\\s*=\\s*(\"[^\"]*\"|'[^']*')",
        RegexOption.IGNORE_CASE,
    )
    private val dangerousScheme = Regex(
        "^\\s*(javascript|vbscript|data)\\s*:",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Returns sanitized HTML safe for rendering. Never throws; on unexpected
     * input returns the input with all tags stripped.
     */
    fun sanitize(html: String): String {
        if (html.isEmpty()) return html
        return try {
            var out = stripDangerous(html)
            out = eventHandlers.replace(out, "")
            out = styleAttributes.replace(out, "")
            out = dangerousUrls.replace(out) { match ->
                val quoted = match.groupValues[2]
                val url = quoted.removeSurrounding("\"").removeSurrounding("'")
                if (dangerousScheme.containsMatchIn(url)) "" else match.value
            }
            out
        } catch (_: Exception) {
            // Fail closed: if anything goes wrong, return plain text.
            htmlToText(html)
        }
    }

    private fun stripDangerous(html: String): String {
        var out = comments.replace(html, "")
        out = dangerousElements.replace(out, "")
        out = voidElements.replace(out, "")
        out = unclosedExecutable.replace(out, "")
        out = danglingTags.replace(out, "")
        return out
    }

    // ---- HTML → plain text ----

    private val blockBreaks = Regex(
        "<(br|p|div|tr|h1|h2|h3|h4|h5|h6|li|ul|ol|blockquote|hr|table|section|article)\\b[^>]*>",
        RegexOption.IGNORE_CASE,
    )
    private val anyTag = Regex("<[^>]+>")
    private val entityMap = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&apos;" to "'",
        "&nbsp;" to " ",
    )
    private val numericEntity = Regex("&#(x?[0-9a-fA-F]+);")

    /**
     * Best-effort HTML→text conversion. Script/style/iframe content is dropped
     * (never leaks into the text), block elements become line breaks, entities
     * are decoded, whitespace is collapsed.
     */
    fun htmlToText(html: String): String {
        if (html.isEmpty()) return ""
        return try {
            var out = stripDangerous(html)
            out = blockBreaks.replace(out, "\n\n")
            out = anyTag.replace(out, "")
            out = decodeEntities(out)
            // Lone newlines are source-formatting whitespace, not paragraphs.
            out = out.replace(Regex("(?<!\\n)\\n(?!\\n)"), " ")
            // Horizontal whitespace collapses; paragraph breaks survive.
            out = out.replace(Regex("[^\\S\\n]+"), " ")
            out = out.replace(Regex("\\n{3,}"), "\n\n")
            out.trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeEntities(text: String): String {
        var out = text
        for ((entity, char) in entityMap) out = out.replace(entity, char)
        out = numericEntity.replace(out) { match ->
            val num = match.groupValues[1]
            try {
                val codePoint = if (num.startsWith("x", ignoreCase = true)) {
                    num.substring(1).toInt(16)
                } else {
                    num.toInt(10)
                }
                // Reject surrogates / out-of-range silently.
                if (codePoint in 1..0x10FFFF &&
                    codePoint !in 0xD800..0xDFFF
                ) {
                    String(Character.toChars(codePoint))
                } else {
                    ""
                }
            } catch (_: Exception) {
                ""
            }
        }
        return out
    }
}
