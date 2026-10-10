package com.greninjaop.mailorganizer.core.analytics

import com.greninjaop.mailorganizer.data.local.MailCategory

/**
 * Deterministic, explainable rule engine for generating insights (Phase 25 §47, §48, §49, §50).
 *
 * Requirements:
 * - Deterministic: Pure function of metrics and thresholds.
 * - Local-first: No network, no LLMs, no remote inference.
 * - Explainable: Every insight cites its metric source, thresholds, and clear reasoning.
 * - Conservative thresholds: Prevents noise on small datasets.
 */
object InsightGenerator {

    private const val MIN_SAMPLE_SIZE_FOR_TRENDS = 10

    /**
     * Evaluates metrics and outputs a prioritized list of explainable insights.
     */
    fun generateInsights(
        accountId: String,
        dateRange: AnalyticsDateRange,
        health: InboxHealthMetrics,
        organization: OrganizationMetrics,
        sources: SourceMetrics,
        noise: NoiseMetrics,
        time: TimeMetrics,
        ruleCorrections: RuleCorrectionMetrics,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // 1. ACTION_REQUIRED_SPIKE: High proportion of actionable emails
        if (health.totalSynchronizedMessages >= MIN_SAMPLE_SIZE_FOR_TRENDS) {
            val actionRatio = health.actionRequiredCount.toFloat() / health.totalSynchronizedMessages
            if (actionRatio >= 0.25f && health.actionRequiredCount >= 3) {
                val pct = (actionRatio * 100).toInt()
                insights += Insight(
                    id = "insight:action_spike:$accountId:${dateRange.name}",
                    accountId = accountId,
                    type = InsightType.ACTION_REQUIRED_SPIKE,
                    title = "High Attention Demand",
                    summary = "$pct% of your mail (${health.actionRequiredCount} emails) requires action.",
                    severity = InsightSeverity.ATTENTION,
                    timeRange = dateRange,
                    metricKey = "action_ratio",
                    metricValue = "$pct%",
                    generatedAtEpochMs = nowEpochMs,
                )
            }
        }

        // 2. UNRESOLVED_CONVERSATION_GROWTH / Awaiting Reply
        if (time.awaitingUserReplyConversations >= 3) {
            insights += Insight(
                id = "insight:awaiting_reply:$accountId:${dateRange.name}",
                accountId = accountId,
                type = InsightType.UNRESOLVED_CONVERSATION_GROWTH,
                title = "Conversations Awaiting Reply",
                summary = "You have ${time.awaitingUserReplyConversations} conversations waiting for your response.",
                severity = if (time.awaitingUserReplyConversations >= 5) InsightSeverity.ATTENTION else InsightSeverity.NOTICE,
                timeRange = dateRange,
                metricKey = "awaiting_user_reply",
                metricValue = time.awaitingUserReplyConversations.toString(),
                generatedAtEpochMs = nowEpochMs,
            )
        }

        // 3. NEWSLETTER_INCREASE / PROMOTION_INCREASE: Noise dominance
        if (health.totalSynchronizedMessages >= MIN_SAMPLE_SIZE_FOR_TRENDS) {
            val noiseRatio = noise.noisePercentageOfTotal
            if (noiseRatio >= 0.50f) {
                val pct = (noiseRatio * 100).toInt()
                val topNewsletterSender = noise.topNewsletterSenders.firstOrNull()
                val topDetail = if (topNewsletterSender != null) {
                    " Lead sender: ${topNewsletterSender.displayName ?: topNewsletterSender.emailAddress} (${topNewsletterSender.messageCount} emails)."
                } else ""

                insights += Insight(
                    id = "insight:noise_dominance:$accountId:${dateRange.name}",
                    accountId = accountId,
                    type = if (noise.newsletterCount >= noise.promotionCount) InsightType.NEWSLETTER_INCREASE else InsightType.PROMOTION_INCREASE,
                    title = "High Inbound Noise",
                    summary = "$pct% of your incoming mail consists of promotional, newsletter, or notification items.$topDetail",
                    severity = InsightSeverity.NOTICE,
                    timeRange = dateRange,
                    metricKey = "noise_percentage",
                    metricValue = "$pct%",
                    generatedAtEpochMs = nowEpochMs,
                )
            }
        }

        // 4. DEADLINE_CLUSTER: Multiple upcoming deadlines
        val totalDeadlines = time.upcomingDeadlinesCount + time.overdueDeadlinesCount
        if (totalDeadlines >= 3 || time.overdueDeadlinesCount > 0) {
            val overdueSummary = if (time.overdueDeadlinesCount > 0) {
                " (${time.overdueDeadlinesCount} overdue)"
            } else ""
            insights += Insight(
                id = "insight:deadline_cluster:$accountId:${dateRange.name}",
                accountId = accountId,
                type = InsightType.DEADLINE_CLUSTER,
                title = "Approaching Deadlines",
                summary = "Detected ${time.upcomingDeadlinesCount} upcoming deadlines$overdueSummary from your messages.",
                severity = if (time.overdueDeadlinesCount > 0) InsightSeverity.ATTENTION else InsightSeverity.NOTICE,
                timeRange = dateRange,
                metricKey = "deadlines_count",
                metricValue = totalDeadlines.toString(),
                generatedAtEpochMs = nowEpochMs,
            )
        }

        // 5. NEW_TOP_SENDER: Single sender generating excessive mail
        val topSender = sources.topSenders.firstOrNull()
        if (topSender != null && health.totalSynchronizedMessages >= MIN_SAMPLE_SIZE_FOR_TRENDS) {
            val senderRatio = topSender.messageCount.toFloat() / health.totalSynchronizedMessages
            if (senderRatio >= 0.20f && topSender.messageCount >= 5) {
                val pct = (senderRatio * 100).toInt()
                insights += Insight(
                    id = "insight:top_sender:$accountId:${dateRange.name}",
                    accountId = accountId,
                    type = InsightType.NEW_TOP_SENDER,
                    title = "Primary Source Volume",
                    summary = "${topSender.displayName ?: topSender.emailAddress} accounts for $pct% of your mail (${topSender.messageCount} emails).",
                    severity = InsightSeverity.INFO,
                    timeRange = dateRange,
                    metricKey = "top_sender_volume",
                    metricValue = "${topSender.messageCount}",
                    generatedAtEpochMs = nowEpochMs,
                )
            }
        }

        // 6. CATEGORY_SHIFT: Dominant category (e.g. receipts or career)
        val dominantCategory = organization.categoryDistribution.maxByOrNull { it.value }
        if (dominantCategory != null && dominantCategory.value >= 5 && health.totalSynchronizedMessages >= MIN_SAMPLE_SIZE_FOR_TRENDS) {
            val catRatio = dominantCategory.value.toFloat() / health.totalSynchronizedMessages
            if (catRatio >= 0.35f && dominantCategory.key != MailCategory.UNCLASSIFIED) {
                val pct = (catRatio * 100).toInt()
                val catName = dominantCategory.key.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                insights += Insight(
                    id = "insight:category_shift:$accountId:${dateRange.name}",
                    accountId = accountId,
                    type = InsightType.CATEGORY_SHIFT,
                    title = "Dominant Category: $catName",
                    summary = "$catName comprises $pct% of your synchronized mail (${dominantCategory.value} items).",
                    severity = InsightSeverity.INFO,
                    timeRange = dateRange,
                    metricKey = "dominant_category",
                    metricValue = dominantCategory.key.name,
                    generatedAtEpochMs = nowEpochMs,
                )
            }
        }

        // 7. ORGANIZATION_COVERAGE_CHANGE: High percentage of uncategorized mail
        if (health.totalSynchronizedMessages >= MIN_SAMPLE_SIZE_FOR_TRENDS && organization.coveragePercentage < 0.70f) {
            val uncategorizedPct = ((1f - organization.coveragePercentage) * 100).toInt()
            insights += Insight(
                id = "insight:coverage:$accountId:${dateRange.name}",
                accountId = accountId,
                type = InsightType.ORGANIZATION_COVERAGE_CHANGE,
                title = "Organization Opportunity",
                summary = "$uncategorizedPct% of messages remain uncategorized. Creating custom rules can automate organization.",
                severity = InsightSeverity.INFO,
                timeRange = dateRange,
                metricKey = "coverage_percentage",
                metricValue = "${(organization.coveragePercentage * 100).toInt()}%",
                generatedAtEpochMs = nowEpochMs,
            )
        }

        return insights
    }
}
