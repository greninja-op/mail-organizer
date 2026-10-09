package com.greninjaop.mailorganizer.core.cleanup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsubscribeSafetyTest {

    @Test
    fun parseListUnsubscribeHeader_withBothHttpAndMailto() {
        val header = "<https://example.com/unsub?id=123>, <mailto:unsub@example.com?subject=unsub>"
        val (httpUrl, mailto) = UnsubscribeSafety.parseListUnsubscribeHeader(header)
        assertEquals("https://example.com/unsub?id=123", httpUrl)
        assertEquals("unsub@example.com?subject=unsub", mailto)
    }

    @Test
    fun parseListUnsubscribeHeader_withHttpOnly() {
        val header = "<https://newsletter.tech/opt-out>"
        val (httpUrl, mailto) = UnsubscribeSafety.parseListUnsubscribeHeader(header)
        assertEquals("https://newsletter.tech/opt-out", httpUrl)
        assertNull(mailto)
    }

    @Test
    fun parseListUnsubscribeHeader_withMailtoOnly() {
        val header = "<mailto:leave-list@news.org>"
        val (httpUrl, mailto) = UnsubscribeSafety.parseListUnsubscribeHeader(header)
        assertNull(httpUrl)
        assertEquals("leave-list@news.org", mailto)
    }

    @Test
    fun parseListUnsubscribeHeader_nullOrEmpty() {
        val (url1, mailto1) = UnsubscribeSafety.parseListUnsubscribeHeader(null)
        assertNull(url1)
        assertNull(mailto1)

        val (url2, mailto2) = UnsubscribeSafety.parseListUnsubscribeHeader("   ")
        assertNull(url2)
        assertNull(mailto2)
    }

    @Test
    fun containsUnsubscribePhrases_detectsCommonPhrases() {
        assertTrue(UnsubscribeSafety.containsUnsubscribePhrases("Click here to unsubscribe from this list"))
        assertTrue(UnsubscribeSafety.containsUnsubscribePhrases("You can opt-out at any time"))
        assertTrue(UnsubscribeSafety.containsUnsubscribePhrases("Manage your email preferences"))
        assertTrue(UnsubscribeSafety.containsUnsubscribePhrases("To stop receiving these updates, click"))
        assertFalse(UnsubscribeSafety.containsUnsubscribePhrases("Hey, here is your project update for today."))
    }
}
