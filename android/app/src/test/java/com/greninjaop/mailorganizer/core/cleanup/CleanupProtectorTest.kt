package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupProtectorTest {

    @Test
    fun securityEmails_areUnconditionallyShielded() {
        assertTrue(CleanupProtector.isProtectedCategory(MailCategory.SECURITY))
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.SECURITY,
                priority = Priority.NORMAL,
                starred = false,
                hasActionItem = false,
            )
        )
        val reason = CleanupProtector.protectionReason(
            category = MailCategory.SECURITY,
            priority = Priority.NORMAL,
            starred = false,
            hasActionItem = false,
        )
        assertNotNull(reason)
        assertTrue(reason!!.contains("Security-critical"))
    }

    @Test
    fun financialReceipts_areShielded() {
        assertTrue(CleanupProtector.isProtectedCategory(MailCategory.RECEIPTS_ORDERS))
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.RECEIPTS_ORDERS,
                priority = Priority.LOW,
                starred = false,
                hasActionItem = false,
            )
        )
        val reason = CleanupProtector.protectionReason(
            category = MailCategory.RECEIPTS_ORDERS,
            priority = Priority.LOW,
            starred = false,
            hasActionItem = false,
        )
        assertNotNull(reason)
        assertTrue(reason!!.contains("Financial record"))
    }

    @Test
    fun careerAndEducation_areShielded() {
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.CAREER,
                priority = Priority.NORMAL,
                starred = false,
                hasActionItem = false,
            )
        )
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.EDUCATION,
                priority = Priority.NORMAL,
                starred = false,
                hasActionItem = false,
            )
        )
    }

    @Test
    fun highAndCriticalPriority_areShielded() {
        assertTrue(CleanupProtector.isProtectedPriority(Priority.HIGH))
        assertTrue(CleanupProtector.isProtectedPriority(Priority.CRITICAL))
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.PROMOTIONS, // even if promotional
                priority = Priority.HIGH,
                starred = false,
                hasActionItem = false,
            )
        )
    }

    @Test
    fun starredEmails_areShielded() {
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.LOW_VALUE,
                priority = Priority.LOW,
                starred = true,
                hasActionItem = false,
            )
        )
    }

    @Test
    fun actionRequiredEmails_areShielded() {
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.ACTION_REQUIRED,
                priority = Priority.NORMAL,
                starred = false,
                hasActionItem = false,
            )
        )
        assertTrue(
            CleanupProtector.isShielded(
                category = MailCategory.PROMOTIONS,
                priority = Priority.NORMAL,
                starred = false,
                hasActionItem = true,
            )
        )
    }

    @Test
    fun unprotectedEmails_areNotShielded() {
        assertFalse(
            CleanupProtector.isShielded(
                category = MailCategory.NOTIFICATIONS,
                priority = Priority.LOW,
                starred = false,
                hasActionItem = false,
            )
        )
        assertNull(
            CleanupProtector.protectionReason(
                category = MailCategory.NOTIFICATIONS,
                priority = Priority.LOW,
                starred = false,
                hasActionItem = false,
            )
        )
    }
}
