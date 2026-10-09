package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseAnalyzerTest {

    private fun createInput(
        senderAddress: String = "service@example.com",
        subject: String = "Test Subject",
        category: MailCategory = MailCategory.UNCLASSIFIED,
        priority: Priority = Priority.NORMAL,
        starred: Boolean = false,
        hasActionItem: Boolean = false,
    ) = CleanupAnalysisInput(
        messageId = "m1",
        threadId = "t1",
        accountId = "acc1",
        senderAddress = senderAddress,
        senderName = "Example Service",
        senderDomain = "example.com",
        companyId = "co:example.com",
        subject = subject,
        snippet = "Snippet",
        bodyText = null,
        labels = emptyList(),
        timestampEpochMs = 1000L,
        unread = false,
        starred = starred,
        category = category,
        priority = priority,
        isRecurringSender = false,
        senderMessageCount = 1,
        listUnsubscribeHeader = null,
        hasActionItem = hasActionItem,
    )

    @Test
    fun automatedNotification_detectedAsNoise() {
        val input = createInput(
            senderAddress = "noreply@updates.example.com",
            subject = "System Notification: Status Update",
            category = MailCategory.NOTIFICATIONS,
            priority = Priority.LOW,
        )
        val result = NoiseAnalyzer.analyze(input)
        assertTrue(result.isNoise)
        assertFalse(result.isProtected)
        assertTrue(result.explanation.isAutomatedNotification)
    }

    @Test
    fun protectedEmail_neverClassifiedAsNoise() {
        val input = createInput(
            senderAddress = "noreply@bank.com",
            subject = "OTP verification code",
            category = MailCategory.SECURITY,
            priority = Priority.CRITICAL,
        )
        val result = NoiseAnalyzer.analyze(input)
        assertFalse(result.isNoise)
        assertFalse(result.isLowValue)
        assertTrue(result.isProtected)
    }

    @Test
    fun receipts_areProtectedFromNoise() {
        val input = createInput(
            senderAddress = "orders@store.com",
            subject = "Your Order Receipt #9872",
            category = MailCategory.RECEIPTS_ORDERS,
        )
        val result = NoiseAnalyzer.analyze(input)
        assertTrue(result.isProtected)
        assertFalse(result.isNoise)
    }
}
