package com.greninjaop.mailorganizer.core.conversation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSignalsTest {

    @Test
    fun `extracts direct request phrases`() {
        val signals = ConversationSignals.extractReplyExpectationSignals(
            bodyText = "Hi Alex, please let me know your thoughts on the proposal by tomorrow.",
            subject = "Q3 Planning",
        )
        assertTrue(signals.isNotEmpty())
        assertTrue(signals.any { it.contains("please let me know") })
    }

    @Test
    fun `extracts direct question patterns`() {
        val signals = ConversationSignals.extractReplyExpectationSignals(
            bodyText = "Thanks for the update.\nCan you send over the updated slide deck?\nBest,\nSarah",
            subject = "Project review",
        )
        assertTrue(signals.isNotEmpty())
        assertTrue(signals.any { it.contains("direct question") })
    }

    @Test
    fun `extracts request from subject line`() {
        val signals = ConversationSignals.extractReplyExpectationSignals(
            bodyText = "Here are the files attached.",
            subject = "Action required: please confirm receipt",
        )
        assertTrue(signals.isNotEmpty())
        assertTrue(signals.any { it.contains("action required") || it.contains("please confirm") })
    }

    @Test
    fun `ignores rhetorical questions in text without query prefix`() {
        val signals = ConversationSignals.extractReplyExpectationSignals(
            bodyText = "Summer sale is here!",
            subject = "Weekly Digest",
        )
        assertTrue(signals.isEmpty())
    }

    @Test
    fun `extracts explicit resolution statements`() {
        val signals = ConversationSignals.extractResolutionSignals(
            bodyText = "Thanks Alex, this is all set and resolved. Have a great weekend!",
            subject = "Re: Bug report 102",
        )
        assertTrue(signals.isNotEmpty())
        assertTrue(signals.any { it.contains("resolved") || it.contains("all set") })
    }

    @Test
    fun `extracts closed statement in subject`() {
        val signals = ConversationSignals.extractResolutionSignals(
            bodyText = "Everything looks good.",
            subject = "[Closed] Ticket #4591",
        )
        assertTrue(signals.isNotEmpty())
    }

    @Test
    fun `returns empty signals for regular informational text`() {
        val signals = ConversationSignals.extractResolutionSignals(
            bodyText = "We are continuing our development sprints next week.",
            subject = "Engineering sync notes",
        )
        assertTrue(signals.isEmpty())
    }
}
