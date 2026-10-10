package com.greninjaop.mailorganizer.core.analytics

import com.greninjaop.mailorganizer.data.local.MailCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightGeneratorTest {

    @Test
    fun `generateInsights detects high action required spike`() {
        val health = InboxHealthMetrics(
            totalSynchronizedMessages = 20,
            unreadCount = 5,
            actionRequiredCount = 6, // 30% -> >= 25% threshold
            highPriorityCount = 4,
            awaitingReplyCount = 2,
            noiseCount = 5,
        )

        val org = OrganizationMetrics(
            categoryDistribution = mapOf(MailCategory.ACTION_REQUIRED to 6),
            categorizedCount = 18,
            unclassifiedCount = 2,
            userCorrectionCount = 0,
            activeRuleCount = 0,
            coveragePercentage = 0.9f,
        )

        val sources = SourceMetrics(emptyList(), emptyList(), emptyList(), 10)
        val noise = NoiseMetrics(2, 2, 1, 0, 0, emptyList(), 0.25f)
        val time = TimeMetrics(emptyList(), 2, 1, 0, 0, 0, null)
        val rules = RuleCorrectionMetrics(0, 0, 0, emptyList())

        val insights = InsightGenerator.generateInsights(
            accountId = "acc_test",
            dateRange = AnalyticsDateRange.LAST_7_DAYS,
            health = health,
            organization = org,
            sources = sources,
            noise = noise,
            time = time,
            ruleCorrections = rules,
            nowEpochMs = 1000000L,
        )

        val actionInsight = insights.find { it.type == InsightType.ACTION_REQUIRED_SPIKE }
        assertTrue("Should detect ACTION_REQUIRED_SPIKE", actionInsight != null)
        assertEquals(InsightSeverity.ATTENTION, actionInsight?.severity)
        assertEquals("30%", actionInsight?.metricValue)
    }

    @Test
    fun `generateInsights detects awaiting reply conversations`() {
        val health = InboxHealthMetrics(15, 2, 1, 1, 4, 3)
        val org = OrganizationMetrics(emptyMap(), 14, 1, 0, 0, 0.93f)
        val sources = SourceMetrics(emptyList(), emptyList(), emptyList(), 8)
        val noise = NoiseMetrics(1, 1, 1, 0, 0, emptyList(), 0.2f)
        val time = TimeMetrics(emptyList(), awaitingUserReplyConversations = 4, awaitingOtherPartyConversations = 1, upcomingDeadlinesCount = 0, overdueDeadlinesCount = 0, upcomingMeetingsCount = 0, oldestUnresolvedEpochMs = null)
        val rules = RuleCorrectionMetrics(0, 0, 0, emptyList())

        val insights = InsightGenerator.generateInsights(
            accountId = "acc_test",
            dateRange = AnalyticsDateRange.LAST_7_DAYS,
            health = health,
            organization = org,
            sources = sources,
            noise = noise,
            time = time,
            ruleCorrections = rules,
        )

        val replyInsight = insights.find { it.type == InsightType.UNRESOLVED_CONVERSATION_GROWTH }
        assertTrue("Should detect UNRESOLVED_CONVERSATION_GROWTH", replyInsight != null)
        assertEquals("4", replyInsight?.metricValue)
    }

    @Test
    fun `generateInsights detects noise dominance when over 50 percent`() {
        val health = InboxHealthMetrics(30, 5, 2, 2, 1, 18) // 18 / 30 = 60%
        val org = OrganizationMetrics(
            categoryDistribution = mapOf(
                MailCategory.NEWSLETTERS to 10,
                MailCategory.PROMOTIONS to 8,
            ),
            categorizedCount = 28,
            unclassifiedCount = 2,
            userCorrectionCount = 0,
            activeRuleCount = 0,
            coveragePercentage = 0.93f,
        )
        val sources = SourceMetrics(emptyList(), emptyList(), emptyList(), 15)
        val noise = NoiseMetrics(
            newsletterCount = 10,
            promotionCount = 8,
            notificationCount = 0,
            lowValueCount = 0,
            cleanupCandidateCount = 12,
            topNewsletterSenders = listOf(
                TopSenderMetric("news@tech.com", "TechNews", 7, "tech.com"),
            ),
            noisePercentageOfTotal = 0.60f,
        )
        val time = TimeMetrics(emptyList(), 1, 0, 0, 0, 0, null)
        val rules = RuleCorrectionMetrics(0, 0, 0, emptyList())

        val insights = InsightGenerator.generateInsights(
            accountId = "acc_test",
            dateRange = AnalyticsDateRange.LAST_7_DAYS,
            health = health,
            organization = org,
            sources = sources,
            noise = noise,
            time = time,
            ruleCorrections = rules,
        )

        val noiseInsight = insights.find { it.type == InsightType.NEWSLETTER_INCREASE }
        assertTrue("Should detect NEWSLETTER_INCREASE", noiseInsight != null)
        assertEquals("60%", noiseInsight?.metricValue)
        assertTrue(noiseInsight!!.summary.contains("TechNews"))
    }

    @Test
    fun `generateInsights detects deadline cluster and overdue items`() {
        val health = InboxHealthMetrics(15, 2, 2, 2, 0, 2)
        val org = OrganizationMetrics(emptyMap(), 15, 0, 0, 0, 1.0f)
        val sources = SourceMetrics(emptyList(), emptyList(), emptyList(), 5)
        val noise = NoiseMetrics(1, 1, 0, 0, 0, emptyList(), 0.13f)
        val time = TimeMetrics(
            volumeBuckets = emptyList(),
            awaitingUserReplyConversations = 0,
            awaitingOtherPartyConversations = 0,
            upcomingDeadlinesCount = 2,
            overdueDeadlinesCount = 1,
            upcomingMeetingsCount = 1,
            oldestUnresolvedEpochMs = null,
        )
        val rules = RuleCorrectionMetrics(0, 0, 0, emptyList())

        val insights = InsightGenerator.generateInsights(
            accountId = "acc_test",
            dateRange = AnalyticsDateRange.LAST_7_DAYS,
            health = health,
            organization = org,
            sources = sources,
            noise = noise,
            time = time,
            ruleCorrections = rules,
        )

        val deadlineInsight = insights.find { it.type == InsightType.DEADLINE_CLUSTER }
        assertTrue("Should detect DEADLINE_CLUSTER", deadlineInsight != null)
        assertEquals(InsightSeverity.ATTENTION, deadlineInsight?.severity)
        assertTrue(deadlineInsight!!.summary.contains("1 overdue"))
    }

    @Test
    fun `generateInsights avoids noisy insights on small datasets`() {
        val health = InboxHealthMetrics(
            totalSynchronizedMessages = 3, // Less than MIN_SAMPLE_SIZE_FOR_TRENDS (10)
            unreadCount = 1,
            actionRequiredCount = 2,
            highPriorityCount = 1,
            awaitingReplyCount = 0,
            noiseCount = 1,
        )
        val org = OrganizationMetrics(emptyMap(), 3, 0, 0, 0, 1.0f)
        val sources = SourceMetrics(emptyList(), emptyList(), emptyList(), 2)
        val noise = NoiseMetrics(1, 0, 0, 0, 0, emptyList(), 0.33f)
        val time = TimeMetrics(emptyList(), 0, 0, 0, 0, 0, null)
        val rules = RuleCorrectionMetrics(0, 0, 0, emptyList())

        val insights = InsightGenerator.generateInsights(
            accountId = "acc_test",
            dateRange = AnalyticsDateRange.LAST_7_DAYS,
            health = health,
            organization = org,
            sources = sources,
            noise = noise,
            time = time,
            ruleCorrections = rules,
        )

        assertTrue("Should not generate noise or action spike insights for small sample", insights.isEmpty())
    }
}
