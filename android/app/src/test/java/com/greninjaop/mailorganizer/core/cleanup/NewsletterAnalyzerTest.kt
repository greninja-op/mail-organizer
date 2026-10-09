package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsletterAnalyzerTest {

    private fun createInput(
        subject: String = "Test Subject",
        snippet: String = "Test snippet",
        category: MailCategory = MailCategory.UNCLASSIFIED,
        listUnsubscribeHeader: String? = null,
        isRecurringSender: Boolean = false,
        senderMessageCount: Int = 1,
    ) = CleanupAnalysisInput(
        messageId = "m1",
        threadId = "t1",
        accountId = "acc1",
        senderAddress = "weekly@tech.com",
        senderName = "Tech Weekly",
        senderDomain = "tech.com",
        companyId = "co:tech.com",
        subject = subject,
        snippet = snippet,
        bodyText = null,
        labels = emptyList(),
        timestampEpochMs = 1000L,
        unread = false,
        starred = false,
        category = category,
        priority = Priority.NORMAL,
        isRecurringSender = isRecurringSender,
        senderMessageCount = senderMessageCount,
        listUnsubscribeHeader = listUnsubscribeHeader,
        hasActionItem = false,
    )

    @Test
    fun newsletterDetected_withListUnsubscribeAndNewsletterKeywords() {
        val input = createInput(
            subject = "Tech Weekly Digest #42",
            listUnsubscribeHeader = "<https://tech.com/unsub>",
            isRecurringSender = true,
            senderMessageCount = 5,
        )
        val result = NewsletterAnalyzer.analyze(input)
        assertTrue(result.isNewsletter)
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals("https://tech.com/unsub", result.unsubscribeHttpUrl)
    }

    @Test
    fun newsletterDetected_withCategoryNewsletters() {
        val input = createInput(
            subject = "Your daily briefing",
            category = MailCategory.NEWSLETTERS,
            snippet = "Click here to unsubscribe from this list",
        )
        val result = NewsletterAnalyzer.analyze(input)
        assertTrue(result.isNewsletter)
        assertTrue(result.explanation.hasUnsubscribePhrases)
    }

    @Test
    fun protectedCategory_suppressesNewsletterClassification() {
        val input = createInput(
            subject = "Security alert from Tech Weekly digest",
            category = MailCategory.SECURITY,
            listUnsubscribeHeader = "<https://tech.com/unsub>",
        )
        val result = NewsletterAnalyzer.analyze(input)
        assertFalse(result.isNewsletter)
        assertTrue(result.explanation.reasonsSummary.any { it.contains("Protected category") })
    }
}
