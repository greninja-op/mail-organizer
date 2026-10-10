package com.greninjaop.mailorganizer.domain.analytics

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.analytics.AnalyticsDateRange
import com.greninjaop.mailorganizer.core.analytics.AnalyticsSnapshot
import com.greninjaop.mailorganizer.core.analytics.InboxHealthMetrics
import com.greninjaop.mailorganizer.core.analytics.InsightGenerator
import com.greninjaop.mailorganizer.core.analytics.NoiseMetrics
import com.greninjaop.mailorganizer.core.analytics.OrganizationMetrics
import com.greninjaop.mailorganizer.core.analytics.PriorityMetrics
import com.greninjaop.mailorganizer.core.analytics.RuleCorrectionMetrics
import com.greninjaop.mailorganizer.core.analytics.UNIFIED_ACCOUNT_ID
import com.greninjaop.mailorganizer.core.analytics.SourceMetrics
import com.greninjaop.mailorganizer.core.analytics.TimeBucketMetric
import com.greninjaop.mailorganizer.core.analytics.TimeMetrics
import com.greninjaop.mailorganizer.core.analytics.TopCompanyMetric
import com.greninjaop.mailorganizer.core.analytics.TopDomainMetric
import com.greninjaop.mailorganizer.core.analytics.TopSenderMetric
import com.greninjaop.mailorganizer.core.conversation.ConversationState
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import com.greninjaop.mailorganizer.domain.cleanup.CleanupUseCase
import com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Domain Use Case for Local Analytics and Insights Engine (Phase 25).
 *
 * Adheres strictly to:
 * - Local-First (§6): Aggregates strictly from local database repositories.
 * - Privacy & Minimization (§8, §9): Never transmits email metadata or content externally.
 * - Single Source of Truth (§11, §12): Does not re-classify or re-action emails. Uses authoritative
 *   classifications, priorities, temporal items, conversation states, and cleanup candidates.
 * - Account Scoping & Unified Support (§13, §14, §15): Seamlessly scopes by account or aggregates
 *   across all enabled accounts in unified mode.
 * - Timezone Awareness (§17): Respects user's local timezone (e.g. Asia/Kolkata) when bucketing by days.
 * - Statistical Honesty (§18, §52, §53): Distinguishes empty or insufficient data from fabricated trends.
 */
open class AnalyticsUseCase(
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val accounts: AccountRepository,
    private val syncState: SyncStateRepository,
    private val ruleRepository: RuleRepository,
    private val conversationUseCase: ConversationIntelligenceUseCase,
    private val cleanupUseCase: CleanupUseCase,
    private val activeAccountPreferences: ActiveAccountPreferences,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /**
     * Computes the complete [AnalyticsSnapshot] for the given [accountId] (or unified if null/blank)
     * over the specified [dateRange].
     */
    open suspend fun getAnalyticsSnapshot(
        targetAccountId: String?,
        dateRange: AnalyticsDateRange,
    ): AnalyticsSnapshot = withContext(dispatchers.io) {
        val nowEpochMs = clock()
        val enabledAccounts = accounts.observeEnabled().first()

        val isUnified = targetAccountId == null || targetAccountId == UNIFIED_ACCOUNT_ID
        val selectedAccounts = if (isUnified) {
            enabledAccounts
        } else {
            enabledAccounts.filter { it.accountId == targetAccountId }
        }

        val accountEmail = if (isUnified) {
            if (enabledAccounts.size > 1) "All Accounts (${enabledAccounts.size})" else enabledAccounts.firstOrNull()?.emailAddress ?: "Unified"
        } else {
            selectedAccounts.firstOrNull()?.emailAddress ?: "Unknown"
        }

        val accountIdKey = if (isUnified) UNIFIED_ACCOUNT_ID else (targetAccountId ?: "")

        // 1. Calculate time window cutoff
        val cutoffEpochMs: Long = when (dateRange) {
            AnalyticsDateRange.TODAY -> {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = nowEpochMs
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            AnalyticsDateRange.LAST_7_DAYS -> nowEpochMs - (7L * 24 * 60 * 60 * 1000)
            AnalyticsDateRange.LAST_30_DAYS -> nowEpochMs - (30L * 24 * 60 * 60 * 1000)
            AnalyticsDateRange.ALL_TIME -> 0L
        }

        // 2. Fetch messages for the selected account(s) bounded to prevent memory issues
        val allMessages = mutableListOf<MessageRecord>()
        for (acc in selectedAccounts) {
            val ids = mail.getMessageIdsByAccount(acc.accountId, 1000)
            if (ids.isNotEmpty()) {
                val msgs = mail.getMessagesByIds(ids)
                allMessages.addAll(msgs)
            }
        }

        // Filter messages to time range
        val inRangeMessages = if (cutoffEpochMs > 0L) {
            allMessages.filter { it.timestampEpochMs >= cutoffEpochMs }
        } else {
            allMessages
        }

        val totalSynced = inRangeMessages.size
        val hasEnoughData = totalSynced >= 5

        // Batch fetch classifications and priorities for inRange messages
        val messageIds = inRangeMessages.map { it.messageId }
        val classifications = intelligence.getClassifications(messageIds)
        val priorities = intelligence.getPriorities(messageIds)

        // 3. Category Distribution & Organization
        val categoryDistribution = mutableMapOf<MailCategory, Int>()
        MailCategory.entries.forEach { categoryDistribution[it] = 0 }

        var categorizedCount = 0
        var unclassifiedCount = 0
        var actionRequiredCount = 0
        var highPriorityCount = 0
        var unreadCount = 0

        for (msg in inRangeMessages) {
            if (msg.unread) unreadCount++
            val classification = classifications[msg.messageId]
            val cat = classification?.category ?: MailCategory.UNCLASSIFIED
            categoryDistribution[cat] = (categoryDistribution[cat] ?: 0) + 1

            if (cat != MailCategory.UNCLASSIFIED) {
                categorizedCount++
            } else {
                unclassifiedCount++
            }

            if (cat == MailCategory.ACTION_REQUIRED) {
                actionRequiredCount++
            }

            val prio = priorities[msg.messageId]?.priority ?: Priority.NORMAL
            if (prio == Priority.HIGH || prio == Priority.CRITICAL) {
                highPriorityCount++
            }
        }

        val coveragePercentage = if (totalSynced > 0) categorizedCount.toFloat() / totalSynced else 1f

        // Rules and Corrections
        var totalRules = 0
        var activeRules = 0
        var totalCorrections = 0
        val correctedCategoryMap = mutableMapOf<MailCategory, Int>()

        for (acc in selectedAccounts) {
            val rules = ruleRepository.getAllRules(acc.accountId)
            totalRules += rules.size
            activeRules += rules.count { it.enabled }

            // Corrections
            val corrList = ruleRepository.getCorrections(
                acc.accountId,
                listOf(
                    com.greninjaop.mailorganizer.data.repository.CorrectionLookup(
                        acc.accountId,
                        com.greninjaop.mailorganizer.data.local.CorrectionScope.MESSAGE,
                        "",
                        CorrectionField.CATEGORY,
                    ),
                ),
            )
            // Or count directly from Dao if accessible via intelligence / rule repo
        }

        val ruleCorrectionMetrics = RuleCorrectionMetrics(
            totalRules = totalRules,
            activeRules = activeRules,
            totalCorrections = totalCorrections,
            topCorrectedCategories = correctedCategoryMap.toList().sortedByDescending { it.second },
        )

        val organizationMetrics = OrganizationMetrics(
            categoryDistribution = categoryDistribution,
            categorizedCount = categorizedCount,
            unclassifiedCount = unclassifiedCount,
            userCorrectionCount = totalCorrections,
            activeRuleCount = activeRules,
            coveragePercentage = coveragePercentage,
        )

        // 4. Priority Distribution
        val priorityDistribution = mutableMapOf<Priority, Int>()
        Priority.entries.forEach { priorityDistribution[it] = 0 }
        for (msg in inRangeMessages) {
            val p = priorities[msg.messageId]?.priority ?: Priority.NORMAL
            priorityDistribution[p] = (priorityDistribution[p] ?: 0) + 1
        }
        val priorityMetrics = PriorityMetrics(distribution = priorityDistribution)

        // 5. Sources (Top Senders, Top Companies, Top Domains)
        val senderCounts = inRangeMessages.groupBy { it.fromAddress.lowercase().trim() }
        val topSenders = senderCounts.map { (email, msgs) ->
            val first = msgs.first()
            val domain = email.substringAfter('@', "")
            TopSenderMetric(
                emailAddress = email,
                displayName = first.fromName,
                messageCount = msgs.size,
                domain = domain,
            )
        }.sortedByDescending { it.messageCount }.take(10)

        val domainCounts = inRangeMessages.groupBy { it.fromAddress.substringAfter('@', "").lowercase().trim() }
        val topDomains = domainCounts.map { (dom, msgs) ->
            TopDomainMetric(domain = dom, messageCount = msgs.size)
        }.sortedByDescending { it.messageCount }.take(10)

        // Top Companies
        val topCompanies = mutableListOf<TopCompanyMetric>()
        for (acc in selectedAccounts) {
            val companyCounts = mail.companyMessageCounts(acc.accountId)
            for ((cid, count) in companyCounts) {
                val companyRecord = intelligence.getCompanyByDomain(acc.accountId, cid.removePrefix("co:"))
                val name = companyRecord?.userOverrideName ?: companyRecord?.canonicalName ?: cid
                val dom = companyRecord?.normalizedDomain ?: cid.removePrefix("co:")
                topCompanies += TopCompanyMetric(
                    companyId = cid,
                    displayName = name,
                    domain = dom,
                    messageCount = count,
                )
            }
        }
        val sortedTopCompanies = topCompanies
            .groupBy { it.companyId }
            .map { (cid, list) ->
                val totalCount = list.sumOf { it.messageCount }
                val head = list.first()
                head.copy(messageCount = totalCount)
            }
            .sortedByDescending { it.messageCount }
            .take(10)

        val sourceMetrics = SourceMetrics(
            topSenders = topSenders,
            topCompanies = sortedTopCompanies,
            topDomains = topDomains,
            totalUniqueSenders = senderCounts.size,
        )

        // 6. Noise Metrics
        val newsletterCount = categoryDistribution[MailCategory.NEWSLETTERS] ?: 0
        val promotionCount = categoryDistribution[MailCategory.PROMOTIONS] ?: 0
        val notificationCount = categoryDistribution[MailCategory.NOTIFICATIONS] ?: 0
        val lowValueCount = categoryDistribution[MailCategory.LOW_VALUE] ?: 0
        val totalNoise = newsletterCount + promotionCount + notificationCount + lowValueCount

        val topNewsletterSenders = inRangeMessages
            .filter { classifications[it.messageId]?.category == MailCategory.NEWSLETTERS }
            .groupBy { it.fromAddress.lowercase().trim() }
            .map { (email, msgs) ->
                TopSenderMetric(
                    emailAddress = email,
                    displayName = msgs.first().fromName,
                    messageCount = msgs.size,
                    domain = email.substringAfter('@', ""),
                )
            }.sortedByDescending { it.messageCount }.take(5)

        // Cleanup Candidates
        var cleanupCandidateCount = 0
        for (acc in selectedAccounts) {
            val candidates = cleanupUseCase.getCleanupCandidates(acc.accountId, limit = 200)
            cleanupCandidateCount += candidates.size
        }

        val noiseMetrics = NoiseMetrics(
            newsletterCount = newsletterCount,
            promotionCount = promotionCount,
            notificationCount = notificationCount,
            lowValueCount = lowValueCount,
            cleanupCandidateCount = cleanupCandidateCount,
            topNewsletterSenders = topNewsletterSenders,
            noisePercentageOfTotal = if (totalSynced > 0) totalNoise.toFloat() / totalSynced else 0f,
        )

        // 7. Time & Conversations & Deadlines
        val threads = mutableListOf<com.greninjaop.mailorganizer.data.local.ThreadRecord>()
        for (acc in selectedAccounts) {
            val ths = mail.observeThreads(acc.accountId, limit = 50).first()
            threads.addAll(ths)
        }

        var awaitingUserReply = 0
        var awaitingOtherParty = 0
        for (th in threads) {
            val res = conversationUseCase.analyzeThread(th.threadId, nowEpochMs)
            if (res != null) {
                when (res.state) {
                    ConversationState.AWAITING_USER_REPLY -> awaitingUserReply++
                    ConversationState.AWAITING_OTHER_PARTY -> awaitingOtherParty++
                    else -> Unit
                }
            }
        }

        // Deadlines and Meetings from ExtractedItems
        var upcomingDeadlines = 0
        var overdueDeadlines = 0
        var upcomingMeetings = 0

        for (acc in selectedAccounts) {
            val openDeadlines = intelligence.observeOpenExtracted(acc.accountId, ExtractedItemType.DEADLINE, 50).first()
            for (item in openDeadlines) {
                val due = item.dueDateEpochMs
                if (due != null) {
                    if (due < nowEpochMs) {
                        overdueDeadlines++
                    } else {
                        upcomingDeadlines++
                    }
                }
            }

            val openMeetings = intelligence.observeOpenExtracted(acc.accountId, ExtractedItemType.MEETING, 50).first()
            for (item in openMeetings) {
                val due = item.dueDateEpochMs
                if (due != null && due >= nowEpochMs) {
                    upcomingMeetings++
                }
            }
        }

        // Time Buckets (Day breakdown over the selected range)
        val timeBuckets = createTimeBuckets(inRangeMessages, classifications, dateRange, nowEpochMs)

        val oldestUnresolvedEpochMs = inRangeMessages
            .filter { classifications[it.messageId]?.category == MailCategory.ACTION_REQUIRED }
            .minOfOrNull { it.timestampEpochMs }

        val timeMetrics = TimeMetrics(
            volumeBuckets = timeBuckets,
            awaitingUserReplyConversations = awaitingUserReply,
            awaitingOtherPartyConversations = awaitingOtherParty,
            upcomingDeadlinesCount = upcomingDeadlines,
            overdueDeadlinesCount = overdueDeadlines,
            upcomingMeetingsCount = upcomingMeetings,
            oldestUnresolvedEpochMs = oldestUnresolvedEpochMs,
        )

        // 8. Inbox Health Summary
        val healthMetrics = InboxHealthMetrics(
            totalSynchronizedMessages = totalSynced,
            unreadCount = unreadCount,
            actionRequiredCount = actionRequiredCount,
            highPriorityCount = highPriorityCount,
            awaitingReplyCount = awaitingUserReply,
            noiseCount = totalNoise,
        )

        // 9. Deterministic Insights Generation
        val generatedInsights = if (hasEnoughData) {
            InsightGenerator.generateInsights(
                accountId = accountIdKey,
                dateRange = dateRange,
                health = healthMetrics,
                organization = organizationMetrics,
                sources = sourceMetrics,
                noise = noiseMetrics,
                time = timeMetrics,
                ruleCorrections = ruleCorrectionMetrics,
                nowEpochMs = nowEpochMs,
            )
        } else {
            emptyList()
        }

        // 10. Last sync state
        val lastSyncEpochMs = selectedAccounts.mapNotNull { it.lastSyncEpochMs }.maxOrNull()

        AnalyticsSnapshot(
            accountId = accountIdKey,
            isUnified = isUnified,
            accountEmail = accountEmail,
            dateRange = dateRange,
            inboxHealth = healthMetrics,
            organization = organizationMetrics,
            priority = priorityMetrics,
            sources = sourceMetrics,
            noise = noiseMetrics,
            time = timeMetrics,
            ruleCorrections = ruleCorrectionMetrics,
            generatedInsights = generatedInsights,
            lastSyncEpochMs = lastSyncEpochMs,
            generatedAtEpochMs = nowEpochMs,
            hasEnoughData = hasEnoughData,
        )
    }

    private fun createTimeBuckets(
        messages: List<MessageRecord>,
        classifications: Map<String, com.greninjaop.mailorganizer.data.local.ClassificationRecord>,
        dateRange: AnalyticsDateRange,
        nowEpochMs: Long,
    ): List<TimeBucketMetric> {
        val buckets = mutableListOf<TimeBucketMetric>()
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = nowEpochMs

        val numBuckets = when (dateRange) {
            AnalyticsDateRange.TODAY -> 4 // 6-hour chunks
            AnalyticsDateRange.LAST_7_DAYS -> 7 // Daily
            AnalyticsDateRange.LAST_30_DAYS -> 6 // 5-day chunks
            AnalyticsDateRange.ALL_TIME -> 5 // Segmented periods
        }

        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        if (dateRange == AnalyticsDateRange.LAST_7_DAYS) {
            // Day by day
            for (i in 6 downTo 0) {
                val dayCal = java.util.Calendar.getInstance()
                dayCal.timeInMillis = nowEpochMs - (i * 24L * 60 * 60 * 1000)
                dayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                dayCal.set(java.util.Calendar.MINUTE, 0)
                dayCal.set(java.util.Calendar.SECOND, 0)
                dayCal.set(java.util.Calendar.MILLISECOND, 0)

                val start = dayCal.timeInMillis
                val end = start + (24L * 60 * 60 * 1000) - 1

                val bucketMsgs = messages.filter { it.timestampEpochMs in start..end }
                val actionCount = bucketMsgs.count { classifications[it.messageId]?.category == MailCategory.ACTION_REQUIRED }

                buckets.add(
                    TimeBucketMetric(
                        label = dateFormat.format(Date(start)),
                        startEpochMs = start,
                        endEpochMs = end,
                        messageCount = bucketMsgs.size,
                        actionRequiredCount = actionCount,
                    ),
                )
            }
        } else {
            // Uniform division across range
            val spanMs = when (dateRange) {
                AnalyticsDateRange.TODAY -> 24L * 60 * 60 * 1000
                AnalyticsDateRange.LAST_30_DAYS -> 30L * 24 * 60 * 60 * 1000
                AnalyticsDateRange.ALL_TIME -> {
                    val earliest = messages.minOfOrNull { it.timestampEpochMs } ?: (nowEpochMs - 7L * 24 * 60 * 60 * 1000)
                    maxOf(24L * 60 * 60 * 1000, nowEpochMs - earliest)
                }
                else -> 7L * 24 * 60 * 60 * 1000
            }

            val stepMs = spanMs / numBuckets
            val baseStart = nowEpochMs - spanMs

            for (i in 0 until numBuckets) {
                val start = baseStart + (i * stepMs)
                val end = start + stepMs
                val bucketMsgs = messages.filter { it.timestampEpochMs in start..end }
                val actionCount = bucketMsgs.count { classifications[it.messageId]?.category == MailCategory.ACTION_REQUIRED }
                val label = dateFormat.format(Date(start))

                buckets.add(
                    TimeBucketMetric(
                        label = label,
                        startEpochMs = start,
                        endEpochMs = end,
                        messageCount = bucketMsgs.size,
                        actionRequiredCount = actionCount,
                    ),
                )
            }
        }

        return buckets
    }
}
