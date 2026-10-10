package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.data.local.MessageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataMinimizerTest {

    private fun testMessage(
        messageId: String = "m1",
        accountId: String = "acc-1",
        threadId: String = "t-1",
        fromAddress: String = "support@store.example.com",
        subject: String = "Subject",
        snippet: String? = "Snippet",
        bodyText: String? = "Body",
        timestampEpochMs: Long = 123456789L,
    ) = MessageRecord(
        messageId = messageId,
        gmailMessageId = "gm-$messageId",
        threadId = threadId,
        accountId = accountId,
        fromAddress = fromAddress,
        fromName = "Store",
        toAddresses = listOf("user@test.org"),
        subject = subject,
        snippet = snippet,
        bodyText = bodyText,
        timestampEpochMs = timestampEpochMs,
    )

    @Test
    fun minimize_stripsPersonalInfoAndBoundsLength() {
        val longSubject = "A".repeat(200)
        val longSnippet = "B".repeat(1000)
        val message = testMessage(
            subject = longSubject,
            snippet = longSnippet,
            bodyText = "Secret Bearer ya29.1234567890abcdef",
        )

        val context = DataMinimizer.minimize(message)

        // Subject capped to 120
        assertEquals(120, context.subject.length)
        // Domain extracted
        assertEquals("store.example.com", context.senderDomain)
        // Snippet capped to 500
        assertEquals(500, context.snippet.length)
        // OAuth token sanitized
        assertFalse(context.snippet.contains("ya29."))
    }

    @Test
    fun minimizeThread_emptyListReturnsEmptyContext() {
        val context = DataMinimizer.minimizeThread(emptyList())
        assertEquals("", context.subject)
        assertEquals("", context.senderDomain)
        assertEquals("", context.snippet)
        assertEquals(0L, context.timestampEpochMs)
    }

    @Test
    fun minimizeThread_takesLastMessagesAndAggregates() {
        val messages = (1..5).map { i ->
            testMessage(
                messageId = "msg-$i",
                fromAddress = "alice@example.com",
                subject = "Discussion Thread",
                snippet = "Snippet $i",
                timestampEpochMs = 1000L * i,
            )
        }

        val context = DataMinimizer.minimizeThread(messages)
        assertEquals("Discussion Thread", context.subject)
        assertEquals("example.com", context.senderDomain)
        assertEquals(5000L, context.timestampEpochMs)
        assertTrue(context.snippet.contains("Snippet 5"))
        assertTrue(context.snippet.contains("Snippet 4"))
        assertTrue(context.snippet.contains("Snippet 3"))
        assertFalse(context.snippet.contains("Snippet 1"))
    }
}
