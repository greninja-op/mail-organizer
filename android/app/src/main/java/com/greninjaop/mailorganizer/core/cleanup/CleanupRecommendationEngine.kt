package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Cleanup Candidate & Recommendation Generator (Phase 20 §20–§23).
 *
 * Architecture:
 * - Turns analysis of messages into explainable [CleanupCandidate] models.
 * - Always conservative: when evidence is weak or uncertain, recommends KEEP or REVIEW.
 * - Destructive action recommendations are NEVER generated for shielded/protected emails.
 * - All candidates are account-scoped and explainable with clear reasons.
 */
object CleanupRecommendationEngine {

    const val VERSION = 1

    fun evaluate(input: CleanupAnalysisInput): CleanupCandidate {
        val newsletterResult = NewsletterAnalyzer.analyze(input)
        val noiseResult = NoiseAnalyzer.analyze(input)

        val candidateId = "clean:${input.accountId}:${input.messageId}"

        // Case 1: Protected / Shielded emails
        if (noiseResult.isProtected) {
            val reason = noiseResult.explanation.reasonsSummary.firstOrNull() ?: "Protected email"
            return CleanupCandidate(
                candidateId = candidateId,
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                candidateType = when (input.category) {
                    MailCategory.NEWSLETTERS -> CleanupCandidateType.NEWSLETTER
                    MailCategory.NOTIFICATIONS -> CleanupCandidateType.NOTIFICATION
                    MailCategory.PROMOTIONS -> CleanupCandidateType.PROMOTIONAL
                    else -> CleanupCandidateType.LOW_VALUE
                },
                recommendationType = CleanupRecommendationType.KEEP,
                reason = reason,
                confidence = Confidence.HIGH,
                isProtected = true,
                createdAtEpochMs = input.timestampEpochMs,
                analysisVersion = VERSION,
                status = CleanupCandidateStatus.SUGGESTED,
            )
        }

        // Case 2: Newsletters
        if (newsletterResult.isNewsletter) {
            val recType = when {
                newsletterResult.unsubscribeHttpUrl != null || newsletterResult.unsubscribeMailto != null ->
                    CleanupRecommendationType.UNSUBSCRIBE_REVIEW
                input.isRecurringSender && input.senderMessageCount >= 5 ->
                    CleanupRecommendationType.CREATE_SENDER_RULE
                else ->
                    CleanupRecommendationType.REVIEW
            }
            val reason = newsletterResult.explanation.reasonsSummary.joinToString("; ").ifBlank { "Detected newsletter" }
            return CleanupCandidate(
                candidateId = candidateId,
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                candidateType = CleanupCandidateType.NEWSLETTER,
                recommendationType = recType,
                reason = reason,
                confidence = newsletterResult.confidence,
                isProtected = false,
                createdAtEpochMs = input.timestampEpochMs,
                analysisVersion = VERSION,
                status = CleanupCandidateStatus.SUGGESTED,
            )
        }

        // Case 3: Automated Notifications
        if (noiseResult.explanation.isAutomatedNotification) {
            val recType = if (input.priority == Priority.LOW) {
                CleanupRecommendationType.CLEANUP_REVIEW
            } else {
                CleanupRecommendationType.REVIEW
            }
            val reason = noiseResult.explanation.reasonsSummary.joinToString("; ").ifBlank { "Automated notification" }
            return CleanupCandidate(
                candidateId = candidateId,
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                candidateType = CleanupCandidateType.NOTIFICATION,
                recommendationType = recType,
                reason = reason,
                confidence = noiseResult.confidence,
                isProtected = false,
                createdAtEpochMs = input.timestampEpochMs,
                analysisVersion = VERSION,
                status = CleanupCandidateStatus.SUGGESTED,
            )
        }

        // Case 4: Commercial Promotions
        if (input.category == MailCategory.PROMOTIONS || noiseResult.explanation.isPromotional) {
            val reason = "Commercial promotional email"
            return CleanupCandidate(
                candidateId = candidateId,
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                candidateType = CleanupCandidateType.PROMOTIONAL,
                recommendationType = CleanupRecommendationType.REVIEW,
                reason = reason,
                confidence = noiseResult.confidence,
                isProtected = false,
                createdAtEpochMs = input.timestampEpochMs,
                analysisVersion = VERSION,
                status = CleanupCandidateStatus.SUGGESTED,
            )
        }

        // Case 5: Low Value
        if (noiseResult.isLowValue) {
            val reason = noiseResult.explanation.reasonsSummary.joinToString("; ").ifBlank { "Low-value non-actionable email" }
            return CleanupCandidate(
                candidateId = candidateId,
                accountId = input.accountId,
                messageId = input.messageId,
                threadId = input.threadId,
                candidateType = CleanupCandidateType.LOW_VALUE,
                recommendationType = CleanupRecommendationType.CLEANUP_REVIEW,
                reason = reason,
                confidence = noiseResult.confidence,
                isProtected = false,
                createdAtEpochMs = input.timestampEpochMs,
                analysisVersion = VERSION,
                status = CleanupCandidateStatus.SUGGESTED,
            )
        }

        // Default: Safe Keep
        return CleanupCandidate(
            candidateId = candidateId,
            accountId = input.accountId,
            messageId = input.messageId,
            threadId = input.threadId,
            candidateType = CleanupCandidateType.LOW_VALUE,
            recommendationType = CleanupRecommendationType.KEEP,
            reason = "Standard email (keep in inbox)",
            confidence = Confidence.LOW,
            isProtected = false,
            createdAtEpochMs = input.timestampEpochMs,
            analysisVersion = VERSION,
            status = CleanupCandidateStatus.SUGGESTED,
        )
    }
}
