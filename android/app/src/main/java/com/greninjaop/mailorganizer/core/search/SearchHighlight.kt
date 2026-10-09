package com.greninjaop.mailorganizer.core.search

/**
 * Safe match highlighting (Phase 10, phase §45–§46).
 *
 * Operates only on already-safe display text (never raw HTML) and returns
 * character ranges; the UI turns them into styled spans. Pure Kotlin, no
 * Compose dependency, so the range logic is unit-testable.
 *
 * Matching is case-insensitive and Unicode-aware; overlapping matches are
 * merged so the UI never double-highlights. Ranges are clamped to the text
 * bounds — a total function that never throws.
 */
object SearchHighlight {

    /** One highlight span: [start] inclusive, [end] exclusive. */
    data class Span(val start: Int, val end: Int)

    /**
     * Finds [terms] and [phrases] in [text] (case-insensitive).
     * Returns merged, sorted, bounds-clamped spans.
     */
    fun findSpans(text: String, terms: List<String>, phrases: List<String>): List<Span> {
        if (text.isEmpty()) return emptyList()
        val needles = (terms + phrases)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (needles.isEmpty()) return emptyList()

        val lower = text.lowercase()
        val raw = mutableListOf<Span>()
        for (needle in needles) {
            val n = needle.lowercase()
            var from = 0
            while (from <= lower.length - n.length) {
                val idx = lower.indexOf(n, from)
                if (idx < 0) break
                raw.add(Span(idx, idx + n.length))
                from = idx + 1
            }
        }
        if (raw.isEmpty()) return emptyList()

        // Merge overlaps; clamp defensively.
        val sorted = raw.sortedWith(compareBy({ it.start }, { it.end }))
        val merged = mutableListOf<Span>()
        for (s in sorted) {
            val start = s.start.coerceIn(0, text.length)
            val end = s.end.coerceIn(start, text.length)
            if (start >= end) continue
            val last = merged.lastOrNull()
            if (last != null && start <= last.end) {
                merged[merged.lastIndex] = Span(last.start, maxOf(last.end, end))
            } else {
                merged.add(Span(start, end))
            }
        }
        return merged
    }

    /**
     * Builds a context snippet around the first match in [bodyText]:
     * up to [contextChars] characters on each side, prefixed/suffixed with
     * "…" when truncated (phase §46). Falls back to [fallback] (the stored
     * snippet) when there is no match or no body. Never exposes huge
     * bodies; never touches HTML.
     */
    fun buildSnippet(
        bodyText: String?,
        fallback: String?,
        terms: List<String>,
        phrases: List<String>,
        contextChars: Int = 48,
    ): String {
        val body = bodyText?.take(BODY_SNIPPET_CAP).orEmpty()
        val spans = findSpans(body, terms, phrases)
        val first = spans.firstOrNull()
        if (first == null) return fallback?.take(SNIPPET_CAP).orEmpty()
        val start = (first.start - contextChars).coerceAtLeast(0)
        val end = (first.end + contextChars).coerceAtMost(body.length)
        val core = body.substring(start, end).replace(Regex("\\s+"), " ").trim()
        return buildString {
            if (start > 0) append("…")
            append(core)
            if (end < body.length) append("…")
        }.take(SNIPPET_CAP)
    }

    /** Bodies are never scanned past this for snippets (bounded, phase §50). */
    const val BODY_SNIPPET_CAP = 20_000

    /** Snippets shown in the UI are capped (phase §46). */
    const val SNIPPET_CAP = 220
}
