package com.greninjaop.mailorganizer.core.cleanup

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.MailCategory

/**
 * Deterministic newsletter analyzer (Phase 20 §8, §9, §12).
 *
 * Combines multiple orthogonal signals:
 * 1. `List-Unsubscribe` headers / body unsubscribe phrases.
 * 2. Pre-existing category classification (NEWSLETTERS, PROMOTIONS).
 * 3. Gmail labels (CATEGORY_UPDATES, CATEGORY_PROMOTIONS, CATEGORY_FORUMS).
 * 4. Recurring sender behavior (sender has sent multiple emails over time).
 * 5. Typical newsletter subject & content patterns (issue numbers, roundups, digests).
 *
 * Does NOT rely on a single signal alone.
 */
object NewsletterAnalyzer {

    const val VERSION = 1

    private val NEWSLETTER_SUBJECT_KEYWORDS = listOf(
        "newsletter",
        "digest",
        "weekly",
        "monthly",
        "daily",
        "roundup",
        "edition",
        "issue #",
        "issue no",
        "vol.",
        "briefing",
        "dispatch",
        "bulletin",
        "update from",
    )

    fun analyze(input: CleanupAnalysisInput): NewsletterAnalysisResult {
        val (safeUrl, safeMailto) = UnsubscribeSafety.parseListUnsubscribeHeader(input.listUnsubscribeHeader)
        val hasListHeader = safeUrl != null || safeMailto != null
        val hasUnsubPhrases = UnsubscribeSafety.containsUnsubscribePhrases(input.snippet) ||
            UnsubscribeSafety.containsUnsubscribePhrases(input.bodyText)

        val subjectLower = input.subject.lowercase()
        val hasSubjectPattern = NEWSLETTER_SUBJECT_KEYWORDS.any { subjectLower.contains(it) }

        val hasPromotionsOrUpdates = input.labels.any {
            it.contains("CATEGORY_PROMOTIONS", ignoreCase = true) ||
                it.contains("CATEGORY_UPDATES", ignoreCase = true)
        }

        val isCategoryNewsletter = input.category == MailCategory.NEWSLETTERS

        val reasons = mutableListOf<String>()
        var score = 0

        if (hasListHeader) {
            score += 35
            reasons.add("List-Unsubscribe header present")
        }
        if (hasUnsubPhrases) {
            score += 25
            reasons.add("Unsubscribe options detected in email content")
        }
        if (isCategoryNewsletter) {
            score += 40
            reasons.add("Classified as Newsletter")
        } else if (input.category == MailCategory.PROMOTIONS) {
            score += 15
            reasons.add("Commercial promotional context")
        }
        if (hasSubjectPattern) {
            score += 20
            reasons.add("Newsletter subject keywords (digest, issue, weekly, etc.)")
        }
        if (input.isRecurringSender) {
            score += 15
            reasons.add("Recurring sender (${input.senderMessageCount} emails seen)")
        }
        if (hasPromotionsOrUpdates) {
            score += 10
            reasons.add("Gmail promotional or update label detected")
        }

        // Special protection: If it is classified as SECURITY, RECEIPTS_ORDERS, CAREER, EDUCATION, or ACTION_REQUIRED,
        // it cannot be confirmed as a generic newsletter without heavy dampening.
        if (CleanupProtector.isProtectedCategory(input.category)) {
            score = 0
            reasons.clear()
            reasons.add("Protected category (${input.category.name}) overrides newsletter classification")
        }

        val isNewsletter = score >= 50
        val confidence = when {
            score >= 70 -> Confidence.HIGH
            score >= 50 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        val explanation = NewsletterExplanation(
            hasListUnsubscribeHeader = hasListHeader,
            hasUnsubscribePhrases = hasUnsubPhrases,
            isRecurringSender = input.isRecurringSender,
            hasNewsletterSubjectOrPattern = hasSubjectPattern,
            hasPromotionsOrUpdatesLabel = hasPromotionsOrUpdates,
            userRuleApplied = false,
            reasonsSummary = reasons,
        )

        return NewsletterAnalysisResult(
            isNewsletter = isNewsletter,
            confidence = confidence,
            explanation = explanation,
            unsubscribeMailto = safeMailto,
            unsubscribeHttpUrl = safeUrl,
            version = VERSION,
        )
    }
}
