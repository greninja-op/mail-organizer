package com.greninjaop.mailorganizer.domain.cleanup

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.cleanup.CleanupAnalysisInput
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidate
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateStatus
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateType
import com.greninjaop.mailorganizer.core.cleanup.CleanupGroup
import com.greninjaop.mailorganizer.core.cleanup.CleanupRecommendationEngine
import com.greninjaop.mailorganizer.core.cleanup.CleanupRecommendationType
import com.greninjaop.mailorganizer.core.cleanup.NewsletterAnalyzer
import com.greninjaop.mailorganizer.core.cleanup.NewsletterSenderProfile
import com.greninjaop.mailorganizer.core.cleanup.UnsubscribeSafety
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Noise, Newsletter & Cleanup Use Case (Phase 20).
 *
 * Coordinates:
 * - Deterministic newsletter & noise analysis across messages.
 * - Sender/domain newsletter profiles with safe unsubscribe handling.
 * - Grouped cleanup candidates for safe bulk review.
 * - Strict account scoping and multi-account awareness.
 * - Reversible candidate review status (SUGGESTED -> REVIEWED / DISMISSED).
 * - Read-only safety: never executes destructive actions or Gmail writes.
 */
class CleanupUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
) {
    private val candidateStatusOverrides = MutableStateFlow<Map<String, CleanupCandidateStatus>>(emptyMap())

    /**
     * Analyzes candidate messages for an account and returns active [CleanupCandidate]s.
     */
    suspend fun getCleanupCandidates(
        accountId: String,
        limit: Int = 100,
    ): List<CleanupCandidate> = withContext(dispatchers.io) {
        val messageIds = mail.getMessageIdsByAccount(accountId, limit)
        if (messageIds.isEmpty()) return@withContext emptyList()

        val messages = mail.getMessagesByIds(messageIds)
        val classifications = intelligence.getClassifications(messageIds)
        val priorities = intelligence.getPriorities(messageIds)

        val overrides = candidateStatusOverrides.value

        messages.mapNotNull { msg ->
            val classification = classifications[msg.messageId]
            val priority = priorities[msg.messageId]
            val sender = intelligence.getSenderByEmail(accountId, msg.fromAddress.lowercase().trim())

            val input = CleanupAnalysisInput(
                messageId = msg.messageId,
                threadId = msg.threadId,
                accountId = msg.accountId,
                senderAddress = msg.fromAddress,
                senderName = msg.fromName,
                senderDomain = msg.fromAddress.substringAfter('@', ""),
                companyId = msg.companyId,
                subject = msg.subject,
                snippet = msg.snippet ?: "",
                bodyText = msg.bodyText,
                labels = msg.labels,
                timestampEpochMs = msg.timestampEpochMs,
                unread = msg.unread,
                starred = msg.starred,
                category = classification?.category ?: MailCategory.UNCLASSIFIED,
                priority = priority?.priority ?: Priority.NORMAL,
                isRecurringSender = (sender?.messageCount ?: 1) >= 3,
                senderMessageCount = sender?.messageCount ?: 1,
                listUnsubscribeHeader = null,
                hasActionItem = false,
            )

            val candidate = CleanupRecommendationEngine.evaluate(input)
            val effectiveStatus = overrides[candidate.candidateId] ?: candidate.status

            if (effectiveStatus == CleanupCandidateStatus.DISMISSED) {
                null
            } else {
                candidate.copy(status = effectiveStatus)
            }
        }
    }

    /**
     * Assembles grouped candidates for safe bulk-review workflows (Phase 20 §24, §25).
     */
    suspend fun getCleanupGroups(
        accountId: String?,
        limit: Int = 100,
    ): List<CleanupGroup> = withContext(dispatchers.io) {
        val allCandidates = if (accountId != null) {
            getCleanupCandidates(accountId, limit)
        } else {
            emptyList()
        }

        val nonProtected = allCandidates.filter { !it.isProtected && it.recommendationType != CleanupRecommendationType.KEEP }

        val groups = mutableListOf<CleanupGroup>()

        val newsletters = nonProtected.filter { it.candidateType == CleanupCandidateType.NEWSLETTER }
        if (newsletters.isNotEmpty()) {
            groups.add(
                CleanupGroup(
                    groupKey = "group:newsletters",
                    title = "Newsletters",
                    description = "Subscription and recurring digest emails",
                    candidateType = CleanupCandidateType.NEWSLETTER,
                    recommendationType = CleanupRecommendationType.UNSUBSCRIBE_REVIEW,
                    candidateCount = newsletters.size,
                    candidates = newsletters,
                    accountIds = newsletters.map { it.accountId }.toSet(),
                )
            )
        }

        val notifications = nonProtected.filter { it.candidateType == CleanupCandidateType.NOTIFICATION }
        if (notifications.isNotEmpty()) {
            groups.add(
                CleanupGroup(
                    groupKey = "group:notifications",
                    title = "Automated Notifications",
                    description = "Automated system notices and status updates",
                    candidateType = CleanupCandidateType.NOTIFICATION,
                    recommendationType = CleanupRecommendationType.CLEANUP_REVIEW,
                    candidateCount = notifications.size,
                    candidates = notifications,
                    accountIds = notifications.map { it.accountId }.toSet(),
                )
            )
        }

        val promotions = nonProtected.filter { it.candidateType == CleanupCandidateType.PROMOTIONAL }
        if (promotions.isNotEmpty()) {
            groups.add(
                CleanupGroup(
                    groupKey = "group:promotions",
                    title = "Promotions",
                    description = "Marketing and commercial sales emails",
                    candidateType = CleanupCandidateType.PROMOTIONAL,
                    recommendationType = CleanupRecommendationType.REVIEW,
                    candidateCount = promotions.size,
                    candidates = promotions,
                    accountIds = promotions.map { it.accountId }.toSet(),
                )
            )
        }

        val lowValue = nonProtected.filter { it.candidateType == CleanupCandidateType.LOW_VALUE }
        if (lowValue.isNotEmpty()) {
            groups.add(
                CleanupGroup(
                    groupKey = "group:low_value",
                    title = "Low Value Emails",
                    description = "Infrequent and non-actionable emails",
                    candidateType = CleanupCandidateType.LOW_VALUE,
                    recommendationType = CleanupRecommendationType.CLEANUP_REVIEW,
                    candidateCount = lowValue.size,
                    candidates = lowValue,
                    accountIds = lowValue.map { it.accountId }.toSet(),
                )
            )
        }

        groups
    }

    /**
     * Builds newsletter sender profiles for an account (Phase 20 §12, §34, §35).
     */
    suspend fun getNewsletterSenders(
        accountId: String,
        limit: Int = 50,
    ): List<NewsletterSenderProfile> = withContext(dispatchers.io) {
        val messageIds = mail.getMessageIdsByAccount(accountId, limit * 2)
        if (messageIds.isEmpty()) return@withContext emptyList()

        val messages = mail.getMessagesByIds(messageIds)
        val classifications = intelligence.getClassifications(messageIds)

        val bySender = messages.groupBy { it.fromAddress.lowercase().trim() }

        val profiles = mutableListOf<NewsletterSenderProfile>()

        for ((senderEmail, msgs) in bySender) {
            val sample = msgs.first()
            val senderRecord = intelligence.getSenderByEmail(accountId, senderEmail)
            val classification = classifications[sample.messageId]

            val input = CleanupAnalysisInput(
                messageId = sample.messageId,
                threadId = sample.threadId,
                accountId = sample.accountId,
                senderAddress = sample.fromAddress,
                senderName = sample.fromName,
                senderDomain = sample.fromAddress.substringAfter('@', ""),
                companyId = sample.companyId,
                subject = sample.subject,
                snippet = sample.snippet ?: "",
                bodyText = sample.bodyText,
                labels = sample.labels,
                timestampEpochMs = sample.timestampEpochMs,
                unread = sample.unread,
                starred = sample.starred,
                category = classification?.category ?: MailCategory.UNCLASSIFIED,
                priority = Priority.NORMAL,
                isRecurringSender = msgs.size >= 2 || (senderRecord?.messageCount ?: 1) >= 2,
                senderMessageCount = senderRecord?.messageCount ?: msgs.size,
                listUnsubscribeHeader = null,
                hasActionItem = false,
            )

            val analysis = NewsletterAnalyzer.analyze(input)

            if (analysis.isNewsletter || classification?.category == MailCategory.NEWSLETTERS) {
                profiles.add(
                    NewsletterSenderProfile(
                        accountId = accountId,
                        senderAddress = sample.fromAddress,
                        displayName = sample.fromName,
                        domain = sample.fromAddress.substringAfter('@', ""),
                        companyId = sample.companyId,
                        messageCount = senderRecord?.messageCount ?: msgs.size,
                        latestMessageEpochMs = msgs.maxOf { it.timestampEpochMs },
                        isNewsletter = true,
                        newsletterConfidence = analysis.confidence,
                        hasUnsubscribe = analysis.unsubscribeHttpUrl != null || analysis.unsubscribeMailto != null || analysis.explanation.hasUnsubscribePhrases,
                        safeUnsubscribeUrl = analysis.unsubscribeHttpUrl,
                        safeUnsubscribeMailto = analysis.unsubscribeMailto,
                        primaryCategory = classification?.category ?: MailCategory.NEWSLETTERS,
                    )
                )
            }
        }

        profiles.sortedByDescending { it.latestMessageEpochMs }.take(limit)
    }

    /**
     * Updates candidate status (e.g. marked reviewed or dismissed).
     */
    fun updateCandidateStatus(candidateId: String, status: CleanupCandidateStatus) {
        candidateStatusOverrides.value = candidateStatusOverrides.value + (candidateId to status)
        MoLogger.i(TAG, "Updated candidate status $candidateId -> $status")
    }

    companion object {
        private const val TAG = "CleanupUseCase"
    }
}
