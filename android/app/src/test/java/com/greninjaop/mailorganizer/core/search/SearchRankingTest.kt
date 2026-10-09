package com.greninjaop.mailorganizer.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ranking contract tests (Phase 10, phase §37–§42). */
class SearchRankingTest {

    @Test
    fun `weights cover every column positionally`() {
        assertEquals(SearchRanking.COLUMN_ORDER.size, SearchRanking.WEIGHTS.size)
    }

    @Test
    fun `subject outranks body`() {
        val subjectIdx = SearchRanking.COLUMN_ORDER.indexOf("subject")
        val bodyIdx = SearchRanking.COLUMN_ORDER.indexOf("bodyText")
        assertTrue(SearchRanking.WEIGHTS[subjectIdx] > SearchRanking.WEIGHTS[bodyIdx])
    }

    @Test
    fun `sender columns are strongly weighted`() {
        val nameIdx = SearchRanking.COLUMN_ORDER.indexOf("fromName")
        val addrIdx = SearchRanking.COLUMN_ORDER.indexOf("fromAddress")
        val bodyIdx = SearchRanking.COLUMN_ORDER.indexOf("bodyText")
        assertTrue(SearchRanking.WEIGHTS[nameIdx] > SearchRanking.WEIGHTS[bodyIdx])
        assertTrue(SearchRanking.WEIGHTS[addrIdx] > SearchRanking.WEIGHTS[bodyIdx])
    }

    @Test
    fun `bookkeeping columns exist and are first`() {
        // messageId/accountId are UNINDEXED: stored, filterable, never
        // searchable (phase §39 — no surprise matches).
        assertEquals("messageId", SearchRanking.COLUMN_ORDER[0])
        assertEquals("accountId", SearchRanking.COLUMN_ORDER[1])
    }

    @Test
    fun `weight list renders as valid bm25 fragment`() {
        val fragment = SearchRanking.bm25WeightList()
        val parts = fragment.split(",").map { it.trim().toDouble() }
        assertEquals(SearchRanking.WEIGHTS.size, parts.size)
        assertTrue(parts.all { it > 0 })
    }

    @Test
    fun `tiebreak is deterministic`() {
        // Timestamp DESC, then messageId ASC: a total order.
        assertTrue(SearchRanking.TIEBREAK_SQL.contains("timestampEpochMs DESC"))
        assertTrue(SearchRanking.TIEBREAK_SQL.contains("messageId ASC"))
    }
}
