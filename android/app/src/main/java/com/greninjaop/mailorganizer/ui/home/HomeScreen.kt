package com.greninjaop.mailorganizer.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.mail.CategoryChip
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.navigation.MoAppTopBar
import com.greninjaop.mailorganizer.ui.navigation.MoNavBar
import com.greninjaop.mailorganizer.ui.navigation.PrimaryDestination
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Home dashboard — Phase 11 (phase §8–§19).
 *
 * Answers "What should I pay attention to right now?" with the hierarchy:
 * account context → attention → high priority → recent → categories →
 * companies. Every number and row comes from local data (§10); empty
 * sections are omitted, never fabricated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    onOpenThread: (threadId: String) -> Unit,
    onOpenCategory: (MailCategory) -> Unit,
    onOpenCompany: (companyId: String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsState()
    val account by viewModel.currentAccount.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            MoAppTopBar(
                title = "Home",
                account = account,
                onOpenSearch = onOpenSearch,
                onOpenAccounts = { onNavigate(AppDestinations.ACCOUNTS) },
                onOpenSecondary = onNavigate,
            )
        },
        bottomBar = {
            MoNavBar(
                currentRoute = PrimaryDestination.HOME.route,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        when (val content = uiState.content) {
            is HomeContent.Loading -> MoLoadingState(
                message = "Loading your dashboard…",
                modifier = Modifier.padding(padding),
            )
            is HomeContent.Loaded -> HomeLoaded(
                content = content,
                onOpenThread = onOpenThread,
                onOpenCategory = onOpenCategory,
                onOpenCompany = onOpenCompany,
                onNavigate = onNavigate,
                modifier = Modifier.padding(padding),
            )
            is HomeContent.Empty -> MoEmptyState(
                title = "No mail yet",
                message = buildString {
                    append("Your account")
                    content.account?.let { append(" (${it.emailAddress})") }
                    append(" is connected. Mail will appear here once synchronization runs.")
                },
                actionLabel = "Open Mail",
                onAction = { onNavigate(AppDestinations.MAIL) },
                modifier = Modifier.padding(padding),
            )
            is HomeContent.NoAccount -> MoEmptyState(
                title = "Welcome to Mail Organizer",
                message = "Connect a Gmail account to start organizing your mail. " +
                    "Account connection arrives with a later phase — your data stays on this device.",
                modifier = Modifier.padding(padding),
            )
            is HomeContent.Error -> MoErrorState(
                message = content.message,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun HomeLoaded(
    content: HomeContent.Loaded,
    onOpenThread: (String) -> Unit,
    onOpenCategory: (MailCategory) -> Unit,
    onOpenCompany: (String) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MoSpacing.md),
    ) {
        if (content.isSampleData) {
            SampleDataNote()
        }
        if (content.isOffline) {
            OfflineNote()
        }

        AccountHeader(account = content.account, unreadCount = content.unreadCount)
        Spacer(Modifier.height(MoSpacing.xs))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(AppDestinations.ANALYTICS) }
                .padding(vertical = MoSpacing.xxs),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(MoSpacing.sm))
                    Column {
                        Text(
                            text = "Analytics & Insights",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Attention demands, categories, sources & noise",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "View",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(MoSpacing.md))

        if (content.attention.isNotEmpty()) {
            HomeSection(
                title = "Needs your attention",
                subtitle = "${content.attention.size} emails",
                onViewAll = { onNavigate(AppDestinations.ACTIONS) },
            ) {
                content.attention.forEach { message ->
                    HomeMessageRow(message = message, onClick = { onOpenThread(message.threadId) })
                }
            }
            Spacer(Modifier.height(MoSpacing.md))
        }

        if (content.highPriority.isNotEmpty()) {
            HomeSection(
                title = "High priority",
                onViewAll = { onNavigate(AppDestinations.MAIL) },
            ) {
                content.highPriority.forEach { message ->
                    HomeMessageRow(message = message, onClick = { onOpenThread(message.threadId) })
                }
            }
            Spacer(Modifier.height(MoSpacing.md))
        }

        if (content.recent.isNotEmpty()) {
            HomeSection(
                title = "Recent mail",
                onViewAll = { onNavigate(AppDestinations.MAIL) },
            ) {
                content.recent.forEach { message ->
                    HomeMessageRow(message = message, onClick = { onOpenThread(message.threadId) })
                }
            }
            Spacer(Modifier.height(MoSpacing.md))
        }

        HomeSection(title = "Categories") {
            CategoryOverviewGrid(
                counts = content.categoryCounts,
                onOpenCategory = onOpenCategory,
            )
        }
        Spacer(Modifier.height(MoSpacing.md))

        if (content.companies.isNotEmpty()) {
            HomeSection(
                title = "Companies",
                onViewAll = { onNavigate(AppDestinations.COMPANIES) },
            ) {
                content.companies.forEach { company ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCompany(company.record.companyId) }
                            .padding(vertical = MoSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = company.record.userOverrideName
                                    ?: company.record.canonicalName,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = company.record.normalizedDomain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${company.messageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics {
                                contentDescription = "${company.messageCount} messages"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(MoSpacing.md))
        }
        Spacer(Modifier.height(MoSpacing.xl))
    }
}

@Composable
private fun AccountHeader(account: AccountRecord, unreadCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = MoSpacing.sm),
    ) {
        AccountAvatar(account = account, size = MoSpacing.xxl)
        Spacer(Modifier.width(MoSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = account.displayName ?: account.emailAddress,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = account.emailAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (unreadCount > 0) {
            Text(
                text = "$unreadCount unread",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "$unreadCount unread messages"
                },
            )
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    onViewAll: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onViewAll != null) {
                TextButton(onClick = onViewAll) {
                    Text("View all")
                }
            }
        }
        Spacer(Modifier.height(MoSpacing.xs))
        content()
    }
}

@Composable
private fun HomeMessageRow(
    message: HomeMessage,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MoSpacing.xs)
            .semantics {
                contentDescription = buildString {
                    append(if (message.unread) "Unread. " else "")
                    append("From ${message.senderName}. ${message.subject}.")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (message.unread) FontWeight.SemiBold else FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = message.subject,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (message.category != null) {
            CategoryChip(category = message.category)
            Spacer(Modifier.width(MoSpacing.xs))
        }
        Text(
            text = MailFormatting.relativeTime(
                message.timestampEpochMs,
                System.currentTimeMillis(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryOverviewGrid(
    counts: Map<MailCategory, Int>,
    onOpenCategory: (MailCategory) -> Unit,
) {
    // Only categories with real data get a tile — never fabricated counts.
    val present = MailCategory.entries
        .filter { (counts[it] ?: 0) > 0 }
        .sortedByDescending { counts[it] ?: 0 }
    if (present.isEmpty()) {
        Text(
            text = "No categorized mail yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xs)) {
        present.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { category ->
                    val visuals = category.visuals()
                    Surface(
                        tonalElevation = MoSpacing.xs,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenCategory(category) }
                            .semantics {
                                contentDescription =
                                    "${visuals.label}, ${counts[category]} messages"
                            },
                    ) {
                        Column(Modifier.padding(MoSpacing.sm)) {
                            Text(
                                text = visuals.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${counts[category]} messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SampleDataNote() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(MoSpacing.xs))
            Text(
                text = "Sample data — fixtures for UI development",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun OfflineNote() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "You're offline — showing your synced mail",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
        )
    }
}
