package com.greninjaop.mailorganizer.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Highlight span + snippet tests (Phase 10, phase §45–§46). */
class SearchHighlightTest {

    @Test
    fun `finds case-insensitive spans`() {
        val spans = SearchHighlight.findSpans(
            "Interview INVITATION tomorrow",
            terms = listOf("interview", "invitation"),
            phrases = emptyList(),
        )
        assertEquals(2, spans.size)
        assertEquals(SearchHighlight.Span(0, 9), spans[0])
        assertEquals(SearchHighlight.Span(10, 20), spans[1])
    }

    @Test
    fun `overlapping spans merge`() {
        val spans = SearchHighlight.findSpans(
            "interview",
            terms = listOf("interview", "terv"),
            phrases = emptyList(),
        )
        assertEquals(listOf(SearchHighlight.Span(0, 9)), spans)
    }

    @Test
    fun `empty text or no terms yields no spans`() {
        assertTrue(
            SearchHighlight.findSpans("", listOf("x"), emptyList()).isEmpty(),
        )
        assertTrue(
            SearchHighlight.findSpans("hello", emptyList(), emptyList()).isEmpty(),
        )
    }

    @Test
    fun `spans never escape text bounds`() {
        val spans = SearchHighlight.findSpans("hi", listOf("hello world"), emptyList())
        assertTrue(spans.isEmpty())
    }

    @Test
    fun `snippet centers on the first match`() {
        val body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Your interview is scheduled for Monday morning. " +
            "Please bring your documents."
        val snippet = SearchHighlight.buildSnippet(
            bodyText = body,
            fallback = "fallback",
            terms = listOf("interview"),
            phrases = emptyList(),
        )
        assertTrue(snippet.contains("interview"))
        assertTrue(snippet.length <= SearchHighlight.SNIPPET_CAP)
    }

    @Test
    fun `snippet falls back when nothing matches`() {
        val snippet = SearchHighlight.buildSnippet(
            bodyText = "nothing relevant here",
            fallback = "stored snippet",
            terms = listOf("zzz"),
            phrases = emptyList(),
        )
        assertEquals("stored snippet", snippet)
    }

    @Test
    fun `snippet handles null body`() {
        val snippet = SearchHighlight.buildSnippet(
            bodyText = null,
            fallback = "fallback",
            terms = listOf("x"),
            phrases = emptyList(),
        )
        assertEquals("fallback", snippet)
    }

    @Test
    fun `unicode matching works`() {
        val spans = SearchHighlight.findSpans(
            "കോളേജ് assignment",
            terms = listOf("കോളേജ്"),
            phrases = emptyList(),
        )
        assertEquals(1, spans.size)
    }
}
