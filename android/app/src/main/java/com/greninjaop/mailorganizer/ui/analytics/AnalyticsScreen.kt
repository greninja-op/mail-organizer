package com.greninjaop.mailorganizer.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.analytics.AnalyticsDateRange
import com.greninjaop.mailorganizer.core.analytics.AnalyticsSnapshot
import com.greninjaop.mailorganizer.core.analytics.InboxHealthMetrics
import com.greninjaop.mailorganizer.core.analytics.Insight
import com.greninjaop.mailorganizer.core.analytics.InsightSeverity
import com.greninjaop.mailorganizer.core.analytics.NoiseMetrics
import com.greninjaop.mailorganizer.core.analytics.OrganizationMetrics
import com.greninjaop.mailorganizer.core.analytics.PriorityMetrics
import com.greninjaop.mailorganizer.core.analytics.SourceMetrics
import com.greninjaop.mailorganizer.core.analytics.TimeMetrics
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Analytics & Insights screen (Phase 25).
 *
 * Designed with the central analytics philosophy:
 * "Analytics should help the user understand and act on their email—not encourage them to chase arbitrary metrics."
 *
 * Transparent sections:
 * 1. Scope & Freshness Header
 * 2. Deterministic Insights Cards (Explainable, action-focused)
 * 3. Inbox Health Overview (Action required, high priority, unread, awaiting reply, noise)
 * 4. Category Breakdown (Using canonical design system colors)
 * 5. Priority Distribution
 * 6. Top Sources (Senders & Companies)
 * 7. Inbound Noise & Cleanup Opportunities
 * 8. Attention & Conversation Backlog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val account by viewModel.currentAccount.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh analytics",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Date Range Selector
            DateRangeFilterRow(
                selectedRange = selectedRange,
                onSelectRange = viewModel::setDateRange,
            )

            HorizontalDivider()

            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    MoLoadingState(
                        message = "Analyzing local email data…",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is AnalyticsUiState.Error -> {
                    MoErrorState(
                        message = state.message,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is AnalyticsUiState.Success -> {
                    val snapshot = state.snapshot
                    if (!snapshot.hasEnoughData) {
                        MoEmptyState(
                            title = "Not enough local data yet",
                            message = "Mail Organizer needs at least 5 synchronized emails to generate meaningful, honest analytics for ${snapshot.accountEmail}.",
                            actionLabel = "Refresh",
                            onAction = viewModel::refresh,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        AnalyticsContent(snapshot = snapshot)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRangeFilterRow(
    selectedRange: AnalyticsDateRange,
    onSelectRange: (AnalyticsDateRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        AnalyticsDateRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            FilterChip(
                selected = isSelected,
                onClick = { onSelectRange(range) },
                label = { Text(range.label) },
            )
        }
    }
}

@Composable
private fun AnalyticsContent(
    snapshot: AnalyticsSnapshot,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.lg),
    ) {
        // Freshness & Scope Notice
        FreshnessHeader(snapshot = snapshot)

        // 1. Actionable Insights
        if (snapshot.generatedInsights.isNotEmpty()) {
            SectionTitle(title = "Key Insights")
            Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.sm)) {
                snapshot.generatedInsights.forEach { insight ->
                    InsightCard(insight = insight)
                }
            }
        }

        // 2. Inbox Health Overview
        SectionTitle(title = "Inbox Health")
        InboxHealthSummaryCard(health = snapshot.inboxHealth)

        // 3. Attention Demands & Conversations
        SectionTitle(title = "Attention & Conversations")
        AttentionConversationsCard(time = snapshot.time)

        // 4. Category Distribution
        SectionTitle(title = "Category Breakdown")
        CategoryDistributionCard(org = snapshot.organization)

        // 5. Priority Distribution
        SectionTitle(title = "Priority Levels")
        PriorityDistributionCard(priority = snapshot.priority)

        // 6. Sources (Top Senders & Companies)
        SectionTitle(title = "Top Sources")
        TopSourcesCard(sources = snapshot.sources)

        // 7. Noise & Cleanup
        SectionTitle(title = "Inbound Noise & Cleanup")
        NoiseSummaryCard(noise = snapshot.noise)

        Spacer(Modifier.height(MoSpacing.xl))
    }
}

@Composable
@Suppress("NonObservableLocale")
private fun FreshnessHeader(snapshot: AnalyticsSnapshot) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val syncText = if (snapshot.lastSyncEpochMs != null && snapshot.lastSyncEpochMs > 0) {
        val df = SimpleDateFormat("MMM d, HH:mm", locale)
        "Based on local data synchronized ${df.format(Date(snapshot.lastSyncEpochMs))}"
    } else {
        "Based on available synchronized local data"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(MoSpacing.xs))
            Text(
                text = "$syncText (${snapshot.accountEmail})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun InsightCard(insight: Insight) {
    val borderColor = when (insight.severity) {
        InsightSeverity.ATTENTION -> MaterialTheme.colorScheme.error
        InsightSeverity.NOTICE -> MaterialTheme.colorScheme.primary
        InsightSeverity.INFO -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MoSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (insight.severity == InsightSeverity.ATTENTION) Icons.Filled.Warning else Icons.Filled.Info,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(MoSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(MoSpacing.xs))
                Text(
                    text = insight.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InboxHealthSummaryCard(health: InboxHealthMetrics) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn(title = "Total Synced", value = health.totalSynchronizedMessages.toString())
                MetricColumn(title = "Action Required", value = health.actionRequiredCount.toString(), highlight = health.actionRequiredCount > 0)
                MetricColumn(title = "High Priority", value = health.highPriorityCount.toString())
            }
            Spacer(Modifier.height(MoSpacing.md))
            HorizontalDivider()
            Spacer(Modifier.height(MoSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn(title = "Unread", value = health.unreadCount.toString())
                MetricColumn(title = "Awaiting Reply", value = health.awaitingReplyCount.toString())
                MetricColumn(title = "Noise Mail", value = health.noiseCount.toString())
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    highlight: Boolean = false,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDistributionCard(org: OrganizationMetrics) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Text(
                text = "Organization Coverage: ${(org.coveragePercentage * 100).toInt()}% (${org.categorizedCount} of ${org.categorizedCount + org.unclassifiedCount} categorized)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MoSpacing.sm))

            val sortedCategories = org.categoryDistribution.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }

            if (sortedCategories.isEmpty()) {
                Text("No categorized emails in this time range.")
            } else {
                sortedCategories.forEach { (cat, count) ->
                    val visuals = cat.visuals()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MoSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(visuals.accentLight),
                            )
                            Spacer(Modifier.width(MoSpacing.sm))
                            Text(
                                text = visuals.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityDistributionCard(priority: PriorityMetrics) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoSpacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Priority.entries.forEach { p ->
                val count = priority.distribution[p] ?: 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = p.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(MoSpacing.xs))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttentionConversationsCard(time: TimeMetrics) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn(title = "Awaiting Your Reply", value = time.awaitingUserReplyConversations.toString())
                MetricColumn(title = "Waiting on Others", value = time.awaitingOtherPartyConversations.toString())
            }
            Spacer(Modifier.height(MoSpacing.md))
            HorizontalDivider()
            Spacer(Modifier.height(MoSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn(title = "Upcoming Deadlines", value = time.upcomingDeadlinesCount.toString())
                MetricColumn(
                    title = "Overdue Deadlines",
                    value = time.overdueDeadlinesCount.toString(),
                    highlight = time.overdueDeadlinesCount > 0,
                )
                MetricColumn(title = "Upcoming Meetings", value = time.upcomingMeetingsCount.toString())
            }
        }
    }
}

@Composable
private fun TopSourcesCard(sources: SourceMetrics) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Text(
                text = "Most Frequent Senders (${sources.totalUniqueSenders} total senders)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(MoSpacing.xs))
            if (sources.topSenders.isEmpty()) {
                Text("No senders recorded.", style = MaterialTheme.typography.bodySmall)
            } else {
                sources.topSenders.take(5).forEach { sender ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MoSpacing.xxs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = sender.displayName ?: sender.emailAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${sender.messageCount} emails",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (sources.topCompanies.isNotEmpty()) {
                Spacer(Modifier.height(MoSpacing.md))
                HorizontalDivider()
                Spacer(Modifier.height(MoSpacing.sm))
                Text(
                    text = "Most Frequent Companies",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(MoSpacing.xs))
                sources.topCompanies.take(5).forEach { company ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MoSpacing.xxs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = company.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${company.messageCount} emails",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoiseSummaryCard(noise: NoiseMetrics) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            val pct = (noise.noisePercentageOfTotal * 100).toInt()
            Text(
                text = "Noise makes up $pct% of your mail",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MoSpacing.sm))
            LinearProgressIndicator(
                progress = { noise.noisePercentageOfTotal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.height(MoSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn(title = "Newsletters", value = noise.newsletterCount.toString())
                MetricColumn(title = "Promotions", value = noise.promotionCount.toString())
                MetricColumn(title = "Notifications", value = noise.notificationCount.toString())
            }
            if (noise.cleanupCandidateCount > 0) {
                Spacer(Modifier.height(MoSpacing.sm))
                Text(
                    text = "${noise.cleanupCandidateCount} emails identified as potential cleanup candidates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
