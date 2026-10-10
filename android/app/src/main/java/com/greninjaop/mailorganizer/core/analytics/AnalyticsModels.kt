package com.greninjaop.mailorganizer.core.analytics

import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Standard date ranges for local analytics aggregation (Phase 25 §16).
 */
enum class AnalyticsDateRange(val label: String, val days: Int) {
    TODAY("Today", 1),
    LAST_7_DAYS("Last 7 days", 7),
    LAST_30_DAYS("Last 30 days", 30),
    ALL_TIME("All synchronized", 0),
}

const val UNIFIED_ACCOUNT_ID = "__UNIFIED__"


/**
 * Deterministic insight types (Phase 25 §48).
 */
enum class InsightType {
    ACTION_REQUIRED_SPIKE,
    UNRESOLVED_CONVERSATION_GROWTH,
    NEWSLETTER_INCREASE,
    PROMOTION_INCREASE,
    CATEGORY_SHIFT,
    NEW_TOP_SENDER,
    DEADLINE_CLUSTER,
    MAILBOX_VOLUME_SPIKE,
    ORGANIZATION_COVERAGE_CHANGE,
}

/**
 * Insight severity / prominence for presentation.
 */
enum class InsightSeverity {
    INFO,
    NOTICE,
    ATTENTION,
}

/**
 * Structured, deterministic insight model (Phase 25 §47, §48, §49).
 */
data class Insight(
    val id: String,
    val accountId: String,
    val type: InsightType,
    val title: String,
    val summary: String,
    val severity: InsightSeverity = InsightSeverity.INFO,
    val timeRange: AnalyticsDateRange,
    val metricKey: String,
    val metricValue: String,
    val generatedAtEpochMs: Long,
    val version: Int = 1,
)

/**
 * Inbox Health metrics summary (Phase 25 §22, §23).
 */
data class InboxHealthMetrics(
    val totalSynchronizedMessages: Int,
    val unreadCount: Int,
    val actionRequiredCount: Int,
    val highPriorityCount: Int,
    val awaitingReplyCount: Int,
    val noiseCount: Int,
)

/**
 * Organization metrics summary (Phase 25 §22, §27, §45).
 */
data class OrganizationMetrics(
    val categoryDistribution: Map<MailCategory, Int>,
    val categorizedCount: Int,
    val unclassifiedCount: Int,
    val userCorrectionCount: Int,
    val activeRuleCount: Int,
    val coveragePercentage: Float, // categorized / total
)

/**
 * Top sender volume metric item.
 */
data class TopSenderMetric(
    val emailAddress: String,
    val displayName: String?,
    val messageCount: Int,
    val domain: String,
)

/**
 * Top company volume metric item.
 */
data class TopCompanyMetric(
    val companyId: String,
    val displayName: String,
    val domain: String,
    val messageCount: Int,
)

/**
 * Top domain volume metric item.
 */
data class TopDomainMetric(
    val domain: String,
    val messageCount: Int,
)

/**
 * Source metrics group (Phase 25 §22, §29, §30, §31).
 */
data class SourceMetrics(
    val topSenders: List<TopSenderMetric>,
    val topCompanies: List<TopCompanyMetric>,
    val topDomains: List<TopDomainMetric>,
    val totalUniqueSenders: Int,
)

/**
 * Noise metrics group (Phase 25 §22, §32, §33, §34, §35, §36).
 */
data class NoiseMetrics(
    val newsletterCount: Int,
    val promotionCount: Int,
    val notificationCount: Int,
    val lowValueCount: Int,
    val cleanupCandidateCount: Int,
    val topNewsletterSenders: List<TopSenderMetric>,
    val noisePercentageOfTotal: Float,
)

/**
 * Temporal breakdown for a bucket of time (e.g. daily counts).
 */
data class TimeBucketMetric(
    val label: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val messageCount: Int,
    val actionRequiredCount: Int,
)

/**
 * Time and trend metrics group (Phase 25 §22, §37, §38, §39, §40).
 */
data class TimeMetrics(
    val volumeBuckets: List<TimeBucketMetric>,
    val awaitingUserReplyConversations: Int,
    val awaitingOtherPartyConversations: Int,
    val upcomingDeadlinesCount: Int,
    val overdueDeadlinesCount: Int,
    val upcomingMeetingsCount: Int,
    val oldestUnresolvedEpochMs: Long?,
)

/**
 * Priority distribution metrics (Phase 25 §26).
 */
data class PriorityMetrics(
    val distribution: Map<Priority, Int>,
)

/**
 * User corrections and rules metrics (Phase 25 §41, §42).
 */
data class RuleCorrectionMetrics(
    val totalRules: Int,
    val activeRules: Int,
    val totalCorrections: Int,
    val topCorrectedCategories: List<Pair<MailCategory, Int>>,
)

/**
 * Complete consolidated analytics snapshot for a selected account scope and date range.
 */
data class AnalyticsSnapshot(
    val accountId: String,
    val isUnified: Boolean,
    val accountEmail: String,
    val dateRange: AnalyticsDateRange,
    val inboxHealth: InboxHealthMetrics,
    val organization: OrganizationMetrics,
    val priority: PriorityMetrics,
    val sources: SourceMetrics,
    val noise: NoiseMetrics,
    val time: TimeMetrics,
    val ruleCorrections: RuleCorrectionMetrics,
    val generatedInsights: List<Insight>,
    val lastSyncEpochMs: Long?,
    val generatedAtEpochMs: Long,
    val hasEnoughData: Boolean,
)
