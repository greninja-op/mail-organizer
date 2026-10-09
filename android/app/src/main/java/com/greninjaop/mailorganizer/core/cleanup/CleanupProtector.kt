package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Protection layer for high-value and sensitive emails (Phase 20 §27–§32).
 *
 * Mail Organizer must NEVER recommend destructive cleanup for:
 * - Security emails (OTP, password resets, login alerts, account recovery)
 * - Financial receipts, invoices, and purchase confirmations
 * - Career and job application correspondence
 * - Education, university, and course alerts
 * - Action-required emails (pending reply, payment, deadline)
 * - Emails marked HIGH or CRITICAL priority
 * - User starred emails
 *
 * These emails are unconditionally shielded from destructive cleanup recommendations.
 */
object CleanupProtector {

    private val PROTECTED_CATEGORIES = setOf(
        MailCategory.SECURITY,
        MailCategory.RECEIPTS_ORDERS,
        MailCategory.CAREER,
        MailCategory.EDUCATION,
        MailCategory.ACTION_REQUIRED,
        MailCategory.IMPORTANT,
    )

    private val PROTECTED_PRIORITIES = setOf(
        Priority.HIGH,
        Priority.CRITICAL,
    )

    /**
     * Checks if a category is intrinsically protected from cleanup.
     */
    fun isProtectedCategory(category: MailCategory): Boolean =
        category in PROTECTED_CATEGORIES

    /**
     * Checks if a priority level is protected from cleanup.
     */
    fun isProtectedPriority(priority: Priority): Boolean =
        priority in PROTECTED_PRIORITIES

    /**
     * Evaluates total protection status for an email.
     * Returns true if the email must be shielded from cleanup suggestions.
     */
    fun isShielded(
        category: MailCategory,
        priority: Priority,
        starred: Boolean,
        hasActionItem: Boolean,
    ): Boolean {
        if (starred) return true
        if (hasActionItem) return true
        if (isProtectedCategory(category)) return true
        if (isProtectedPriority(priority)) return true
        return false
    }

    /**
     * Generates a human-readable explanation of why an email is protected.
     */
    fun protectionReason(
        category: MailCategory,
        priority: Priority,
        starred: Boolean,
        hasActionItem: Boolean,
    ): String? {
        return when {
            starred -> "Starred by you (manual intent preserved)"
            hasActionItem -> "Contains pending action or deadline"
            category == MailCategory.SECURITY -> "Security-critical email (login, verification, alert)"
            category == MailCategory.RECEIPTS_ORDERS -> "Financial record or purchase receipt"
            category == MailCategory.CAREER -> "Career or job application communication"
            category == MailCategory.EDUCATION -> "Educational or academic institution communication"
            category == MailCategory.ACTION_REQUIRED -> "Marked Action Required"
            category == MailCategory.IMPORTANT -> "Classified as Important"
            priority == Priority.CRITICAL -> "Critical priority email"
            priority == Priority.HIGH -> "High priority email"
            else -> null
        }
    }
}
