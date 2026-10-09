package com.greninjaop.mailorganizer.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cursor codec tests (Phase 4): the opaque string round-trips, and garbage degrades safely. */
class SyncCursorTest {

    @Test
    fun `page cursor round-trips`() {
        val cursor = SyncCursor.Page("token-abc-123")
        val decoded = SyncCursor.decode(cursor.encode())
        assertEquals(cursor, decoded)
    }

    @Test
    fun `first-page cursor round-trips`() {
        val decoded = SyncCursor.decode(SyncCursor.Page(null).encode())
        assertEquals(SyncCursor.Page(null), decoded)
    }

    @Test
    fun `history cursor round-trips`() {
        val cursor = SyncCursor.History("987654321")
        val decoded = SyncCursor.decode(cursor.encode())
        assertEquals(cursor, decoded)
    }

    @Test
    fun `null decodes to initial sync`() {
        assertEquals(SyncCursor.Page(null), SyncCursor.decode(null))
    }

    @Test
    fun `blank decodes to initial sync`() {
        assertEquals(SyncCursor.Page(null), SyncCursor.decode("   "))
    }

    @Test
    fun `unknown format degrades to safe initial sync, never crashes`() {
        assertEquals(SyncCursor.Page(null), SyncCursor.decode("garbage"))
        assertEquals(SyncCursor.Page(null), SyncCursor.decode("v2:page:x"))
        assertEquals(SyncCursor.Page(null), SyncCursor.decode("v1:bogus:x"))
        assertEquals(SyncCursor.Page(null), SyncCursor.decode("v1:history:"))
    }

    @Test
    fun `tokens containing separators survive`() {
        val token = "a:b:c/with?special=chars&more"
        val decoded = SyncCursor.decode(SyncCursor.Page(token).encode())
        assertEquals(SyncCursor.Page(token), decoded)
    }

    @Test
    fun `page token distinguishes null from empty`() {
        // Empty token encodes as first-page marker; decode normalizes to null.
        val decoded = SyncCursor.decode("v1:page:")
        assertTrue(decoded is SyncCursor.Page)
        assertNull((decoded as SyncCursor.Page).pageToken)
    }
}
