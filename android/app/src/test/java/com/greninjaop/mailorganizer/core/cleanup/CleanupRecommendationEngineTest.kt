package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupRecommendationEngineTest {

    private fun createInput(
        category: MailCategory = MailCategory.UNCLASSIFIED,
        priority: Priority = Priority.NORMAL,
        listUnsubscribeHeader: String? = null,
        starred: Boolean = false,
        hasActionItem: Boolean = false,
        isRecurringSender: Boolean = false,
        senderMessageCount: Int = 1,
    ) = CleanupAnalysisInput(
        messageId = "m1",
        threadId = "t1",
        accountId = "acc1",
        senderAddress = "sender@news.com",
        senderName = "News Service",
        senderDomain = "news.com",
        companyId = "co:news.com",
        subject = "Monthly Round-up",
        snippet = "Read here",
        bodyText = null,
        labels = emptyList(),
        timestampEpochMs = 1000L,
        unread = false,
        starred = starred,
        category = category,
        priority = priority,
        isRecurringSender = isRecurringSender,
        senderMessageCount = senderMessageCount,
        listUnsubscribeHeader = listUnsubscribeHeader,
        hasActionItem = hasActionItem,
    )

    @Test
    fun protectedEmail_generatesKeepRecommendation() {
        val input = createInput(
            category = MailCategory.SECURITY,
            priority = Priority.HIGH,
        )
        val candidate = CleanupRecommendationEngine.evaluate(input)
        assertTrue(candidate.isProtected)
        assertEquals(CleanupRecommendationType.KEEP, candidate.recommendationType)
    }

    @Test
    fun newsletterWithUnsubscribe_generatesUnsubscribeReviewRecommendation() {
        val input = createInput(
            category = MailCategory.NEWSLETTERS,
            listUnsubscribeHeader = "<https://news.com/unsub>",
        )
        val candidate = CleanupRecommendationEngine.evaluate(input)
        assertFalse(candidate.isProtected)
        assertEquals(CleanupCandidateType.NEWSLETTER, candidate.candidateType)
        assertEquals(CleanupRecommendationType.UNSUBSCRIBE_REVIEW, candidate.recommendationType)
    }

    @Test
    fun notificationWithLowPriority_generatesCleanupReviewRecommendation() {
        val input = createInput(
            category = MailCategory.NOTIFICATIONS,
            priority = Priority.LOW,
        )
        val candidate = CleanupRecommendationEngine.evaluate(input)
        assertEquals(CleanupCandidateType.NOTIFICATION, candidate.candidateType)
        assertEquals(CleanupRecommendationType.CLEANUP_REVIEW, candidate.recommendationType)
    }
}
