package com.greninjaop.mailorganizer.ui.cleanup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidate
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateType
import com.greninjaop.mailorganizer.core.cleanup.CleanupGroup
import com.greninjaop.mailorganizer.core.cleanup.CleanupRecommendationType
import com.greninjaop.mailorganizer.core.cleanup.NewsletterSenderProfile
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Noise, Newsletter & Cleanup Screen (Phase 20).
 *
 * Provides:
 * - Review & recommendations overview (Newsletters, Notifications, Promotions, Low Value).
 * - Safe bulk review modal / detail view before any action.
 * - Dedicated newsletter sender management with safe unsubscribe info.
 * - Honest account indicator (Single Account vs All Accounts).
 * - Read-only safe execution notice (no deletion executed; future Gmail-write seam).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    viewModel: CleanupViewModel,
    onBack: () -> Unit,
    onOpenThread: (threadId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.actionMessage) {
        val msg = state.actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeActionMessage()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Cleanup & Newsletters")
                        val accountText = if (state.isUnified) {
                            "All Accounts"
                        } else {
                            state.currentAccount?.emailAddress ?: "No account"
                        }
                        Text(
                            text = accountText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recommendations") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Newsletters (${state.newsletterSenders.size})") },
                )
            }

            if (state.isLoading) {
                MoLoadingState(
                    message = "Analyzing inbox for cleanup recommendations…",
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (state.currentAccount == null) {
                MoEmptyState(
                    title = "No active account",
                    message = "Connect an account in Settings to view cleanup recommendations.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (selectedTab == 0) {
                RecommendationsTabContent(
                    state = state,
                    onSelectGroup = viewModel::selectGroup,
                    onDismissCandidate = viewModel::dismissCandidate,
                    onMarkReviewed = viewModel::markCandidateReviewed,
                    onOpenThread = onOpenThread,
                )
            } else {
                NewslettersTabContent(
                    senders = state.newsletterSenders,
                    onOpenSender = { /* Sender detail or filter */ },
                )
            }
        }
    }

    // Detail Group Review Dialog
    state.selectedGroup?.let { group ->
        GroupReviewDialog(
            group = group,
            onDismiss = { viewModel.selectGroup(null) },
            onDismissCandidate = { candidateId ->
                viewModel.dismissCandidate(candidateId)
            },
            onMarkReviewed = { candidateId ->
                viewModel.markCandidateReviewed(candidateId)
            },
            onOpenThread = { threadId ->
                viewModel.selectGroup(null)
                onOpenThread(threadId)
            },
        )
    }
}

@Composable
private fun RecommendationsTabContent(
    state: CleanupUiState,
    onSelectGroup: (CleanupGroup) -> Unit,
    onDismissCandidate: (String) -> Unit,
    onMarkReviewed: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.cleanupGroups.isEmpty()) {
        MoEmptyState(
            title = "Inbox clean & organized",
            message = "No low-value or cleanup candidates found. Your inbox is in great shape.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.md),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(MoSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(MoSpacing.sm))
                    Text(
                        text = "Identify & organize first. No emails are ever automatically deleted. All cleanup recommendations require your explicit confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(state.cleanupGroups, key = { it.groupKey }) { group ->
            CleanupGroupCard(
                group = group,
                onReviewClick = { onSelectGroup(group) },
            )
        }
    }
}

@Composable
private fun CleanupGroupCard(
    group: CleanupGroup,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = group.candidateType.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(MoSpacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${group.candidateCount} emails reviewable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(MoSpacing.xs))
            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(MoSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onReviewClick) {
                    Text("Review (${group.candidateCount})")
                }
            }
        }
    }
}

@Composable
private fun NewslettersTabContent(
    senders: List<NewsletterSenderProfile>,
    onOpenSender: (NewsletterSenderProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (senders.isEmpty()) {
        MoEmptyState(
            title = "No newsletters found",
            message = "No subscription newsletters detected in your synchronized mail.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        items(senders, key = { it.senderAddress }) { sender ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MoSpacing.md),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = sender.displayName ?: sender.senderAddress,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = sender.domain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${sender.messageCount} emails",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(MoSpacing.xs))
                    if (sender.hasUnsubscribe) {
                        Text(
                            text = "Unsubscribe available (safe user-review)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupReviewDialog(
    group: CleanupGroup,
    onDismiss: () -> Unit,
    onDismissCandidate: (String) -> Unit,
    onMarkReviewed: (String) -> Unit,
    onOpenThread: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Review: ${group.title}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Review each item before deciding. Accidental deletions are prohibited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MoSpacing.sm))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                ) {
                    items(group.candidates, key = { it.candidateId }) { candidate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenThread(candidate.threadId) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(Modifier.padding(MoSpacing.sm)) {
                                Text(
                                    text = candidate.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(MoSpacing.xs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { onDismissCandidate(candidate.candidateId) }) {
                                        Text("Keep")
                                    }
                                    TextButton(onClick = { onMarkReviewed(candidate.candidateId) }) {
                                        Text("Reviewed")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun CleanupCandidateType.icon(): ImageVector = when (this) {
    CleanupCandidateType.NEWSLETTER -> Icons.Filled.Email
    CleanupCandidateType.NOTIFICATION -> Icons.Filled.Notifications
    CleanupCandidateType.PROMOTIONAL -> Icons.Filled.ShoppingCart
    CleanupCandidateType.LOW_VALUE -> Icons.Filled.Info
    CleanupCandidateType.OLD_UNREAD -> Icons.Filled.Clear
}
