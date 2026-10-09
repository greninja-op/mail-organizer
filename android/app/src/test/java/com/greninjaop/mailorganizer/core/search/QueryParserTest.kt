package com.greninjaop.mailorganizer.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Query parser tests (Phase 10, phase §13–§17, §48–§51). */
class QueryParserTest {

    @Test
    fun `blank query is empty - no DB work`() {
        for (raw in listOf("", "   ", "\t\n ")) {
            val parsed = QueryParser.parse(raw)
            assertTrue(parsed.isEmpty)
            assertNull(parsed.matchExpression)
        }
    }

    @Test
    fun `single term is quoted`() {
        val parsed = QueryParser.parse("invoice")
        assertFalse(parsed.isEmpty)
        assertEquals("\"invoice\"", parsed.matchExpression)
        assertEquals(listOf("invoice"), parsed.terms)
    }

    @Test
    fun `multi-word query uses AND semantics`() {
        val parsed = QueryParser.parse("job interview")
        assertEquals("\"job\" AND \"interview\"", parsed.matchExpression)
    }

    @Test
    fun `quoted phrase stays a phrase`() {
        val parsed = QueryParser.parse("\"job interview\"")
        assertEquals("\"job interview\"", parsed.matchExpression)
        assertEquals(listOf("job interview"), parsed.phrases)
        assertTrue(parsed.terms.isEmpty())
    }

    @Test
    fun `mixed phrase and terms`() {
        val parsed = QueryParser.parse("\"job interview\" amazon")
        assertEquals("\"job interview\" AND \"amazon\"", parsed.matchExpression)
    }

    @Test
    fun `fts keywords inside quotes stay literal`() {
        // "OR" must not become a boolean operator (phase §48).
        val parsed = QueryParser.parse("fish OR chips")
        assertEquals("\"fish\" AND \"OR\" AND \"chips\"", parsed.matchExpression)
    }

    @Test
    fun `special characters are neutralized`() {
        val parsed = QueryParser.parse("a*b (c) d:e ^f")
        assertFalse(parsed.isEmpty)
        // No raw syntax survives: every token is quoted.
        val expr = parsed.matchExpression!!
        assertTrue(expr.startsWith("\""))
        assertFalse(expr.contains(" ("))
    }

    @Test
    fun `sql injection probe degrades safely`() {
        val parsed = QueryParser.parse("'\" OR \"1\"=\"1")
        // Must not throw; the expression is fully quoted or empty.
        if (!parsed.isEmpty) {
            assertTrue(parsed.matchExpression!!.startsWith("\""))
        }
    }

    @Test
    fun `semicolons and comment markers do not escape`() {
        val parsed = QueryParser.parse("invoice; DROP TABLE x --")
        assertFalse(parsed.isEmpty)
        assertTrue(parsed.matchExpression!!.contains("\"invoice;\""))
    }

    @Test
    fun `pure punctuation is empty`() {
        for (raw in listOf("***", "\"", "';--", "()")) {
            assertTrue("expected empty for $raw", QueryParser.parse(raw).isEmpty)
        }
    }

    @Test
    fun `query is bounded in length`() {
        val parsed = QueryParser.parse("x".repeat(500))
        assertFalse(parsed.isEmpty)
        assertTrue(parsed.matchExpression!!.length <= QueryParser.MAX_QUERY_CHARS + 20)
    }

    @Test
    fun `term count is bounded`() {
        val parsed = QueryParser.parse((1..30).joinToString(" ") { "w$it" })
        assertFalse(parsed.isEmpty)
        assertTrue(parsed.terms.size + parsed.phrases.size <= QueryParser.MAX_TERMS)
    }

    @Test
    fun `unicode passes through untouched`() {
        val parsed = QueryParser.parse("കോളേജ് assignment")
        assertFalse(parsed.isEmpty)
        assertTrue(parsed.matchExpression!!.contains("കോളേജ്"))
    }

    @Test
    fun `email-like query is kept as one phrase`() {
        val parsed = QueryParser.parse("\"priya@company.com\"")
        assertEquals("\"priya@company.com\"", parsed.matchExpression)
    }

    @Test
    fun `unbalanced quote degrades to terms`() {
        val parsed = QueryParser.parse("\"unbalanced")
        assertFalse(parsed.isEmpty)
        // The stray quote is stripped; the word remains searchable.
        assertEquals("\"unbalanced\"", parsed.matchExpression)
    }

    @Test
    fun `apostrophes are preserved inside quotes`() {
        val parsed = QueryParser.parse("don't")
        assertEquals("\"don't\"", parsed.matchExpression)
    }

    @Test
    fun `never throws on hostile input`() {
        val hostile = listOf(
            "\u0000\u0001\u0002",
            "𝄞".repeat(50),
            "\"\"\"\"",
            "(".repeat(100),
            "a\u0308".repeat(100), // combining diaeresis
        )
        for (raw in hostile) {
            QueryParser.parse(raw) // must not throw
        }
    }
}
