package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Noise, Newsletter & Cleanup Domain Models (Phase 20).
 *
 * Core principles:
 * - Identify and organize first; recommend cleanup second.
 * - low value != safe to delete; newsletter != unwanted.
 * - Protect critical mail: SECURITY, RECEIPTS_ORDERS, CAREER, EDUCATION,
 *   ACTION_REQUIRED, and HIGH/CRITICAL priority are heavily protected.
 * - Unsubscribe links/headers are UNTRUSTED content: never visited or invoked automatically.
 * - Thread vs Message semantics: a thread is never deleted because one message is low value.
 * - No destructive operations executed in Phase 20 (deferred to future write architecture).
 */

/**
 * Categorization of cleanup candidates.
 * Deliberately derived from evidence and existing intelligence.
 */
enum class CleanupCandidateType {
    NEWSLETTER,
    NOTIFICATION,
    PROMOTIONAL,
    LOW_VALUE,
    OLD_UNREAD,
}

/**
 * Cleanup recommendation action types that can be suggested to the user.
 * Reversible / review-oriented recommendations are prioritized over deletion.
 */
enum class CleanupRecommendationType {
    /** Keep and organize in its category (safe default). */
    KEEP,
    /** Review recent emails from this sender or group. */
    REVIEW,
    /** Review unsubscribe option safely (user-initiated only). */
    UNSUBSCRIBE_REVIEW,
    /** Create a user rule for this sender or domain (via Phase 12). */
    CREATE_SENDER_RULE,
    /** Create a user rule for this domain (via Phase 12). */
    CREATE_DOMAIN_RULE,
    /** Review for archive / cleanup in a future write action. */
    CLEANUP_REVIEW,
}

/**
 * Lifecycle status of a cleanup candidate item.
 */
enum class CleanupCandidateStatus {
    SUGGESTED,
    REVIEWED,
    DISMISSED,
    APPLIED,
}

/**
 * Explanation reasons for newsletter detection.
 */
data class NewsletterExplanation(
    val hasListUnsubscribeHeader: Boolean = false,
    val hasUnsubscribePhrases: Boolean = false,
    val isRecurringSender: Boolean = false,
    val hasNewsletterSubjectOrPattern: Boolean = false,
    val hasPromotionsOrUpdatesLabel: Boolean = false,
    val userRuleApplied: Boolean = false,
    val reasonsSummary: List<String> = emptyList(),
)

/**
 * Result of newsletter analysis.
 */
data class NewsletterAnalysisResult(
    val isNewsletter: Boolean,
    val confidence: Confidence,
    val explanation: NewsletterExplanation,
    val unsubscribeMailto: String? = null,
    val unsubscribeHttpUrl: String? = null,
    val version: Int = 1,
)

/**
 * Explanation reasons for noise / low-value detection.
 */
data class NoiseExplanation(
    val isAutomatedNotification: Boolean = false,
    val isPromotional: Boolean = false,
    val isRecurringLowValue: Boolean = false,
    val isOldUnread: Boolean = false,
    val isProtectedCategory: Boolean = false,
    val isProtectedPriority: Boolean = false,
    val isActionRequired: Boolean = false,
    val reasonsSummary: List<String> = emptyList(),
)

/**
 * Result of noise / low-value analysis.
 */
data class NoiseAnalysisResult(
    val isNoise: Boolean,
    val isLowValue: Boolean,
    val isProtected: Boolean,
    val confidence: Confidence,
    val explanation: NoiseExplanation,
    val version: Int = 1,
)

/**
 * Aggregated sender-level newsletter & noise profile.
 */
data class NewsletterSenderProfile(
    val accountId: String,
    val senderAddress: String,
    val displayName: String?,
    val domain: String,
    val companyId: String?,
    val messageCount: Int,
    val latestMessageEpochMs: Long,
    val isNewsletter: Boolean,
    val newsletterConfidence: Confidence,
    val hasUnsubscribe: Boolean,
    val safeUnsubscribeUrl: String?,
    val safeUnsubscribeMailto: String?,
    val primaryCategory: MailCategory,
)

/**
 * A structured cleanup candidate model (Phase 20 §20).
 */
data class CleanupCandidate(
    val candidateId: String,
    val accountId: String,
    val messageId: String,
    val threadId: String,
    val candidateType: CleanupCandidateType,
    val recommendationType: CleanupRecommendationType,
    val reason: String,
    val confidence: Confidence,
    val isProtected: Boolean,
    val createdAtEpochMs: Long,
    val analysisVersion: Int = 1,
    val status: CleanupCandidateStatus = CleanupCandidateStatus.SUGGESTED,
)

/**
 * Grouped candidate collection for safe bulk review.
 */
data class CleanupGroup(
    val groupKey: String,
    val title: String,
    val description: String,
    val candidateType: CleanupCandidateType,
    val recommendationType: CleanupRecommendationType,
    val candidateCount: Int,
    val candidates: List<CleanupCandidate>,
    val accountIds: Set<String>,
)

/**
 * Input for analyzing a message for noise, newsletter, and cleanup.
 */
data class CleanupAnalysisInput(
    val messageId: String,
    val threadId: String,
    val accountId: String,
    val senderAddress: String,
    val senderName: String?,
    val senderDomain: String,
    val companyId: String?,
    val subject: String,
    val snippet: String,
    val bodyText: String?,
    val labels: List<String>,
    val timestampEpochMs: Long,
    val unread: Boolean,
    val starred: Boolean,
    val category: MailCategory,
    val priority: Priority,
    val isRecurringSender: Boolean,
    val senderMessageCount: Int,
    val listUnsubscribeHeader: String? = null,
    val hasActionItem: Boolean = false,
)
