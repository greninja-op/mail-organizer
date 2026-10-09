package com.greninjaop.mailorganizer.ui.actions

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.mail.PriorityBadge
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.navigation.MoAppTopBar
import com.greninjaop.mailorganizer.ui.navigation.MoNavBar
import com.greninjaop.mailorganizer.ui.navigation.PrimaryDestination
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/** Factory for the Phase 11 Actions destination. */
class ActionsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ActionsViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
        ) as T
    }
}

/**
 * Actions destination — Phase 11 (phase §29–§32).
 *
 * An organizational view over Phase 9's action-required detection. Tapping
 * an item opens the underlying email; **nothing here performs external
 * actions** (no reply/send/Calendar/Tasks/Gmail writes — §29, §30).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(
    viewModel: ActionsViewModel,
    onNavigate: (String) -> Unit,
    onOpenThread: (threadId: String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsState()
    val account by viewModel.currentAccount.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            MoAppTopBar(
                title = "Actions",
                account = account,
                onOpenSearch = onOpenSearch,
                onOpenAccounts = { onNavigate(AppDestinations.ACCOUNTS) },
                onOpenSecondary = onNavigate,
            )
        },
        bottomBar = {
            MoNavBar(
                currentRoute = PrimaryDestination.ACTIONS.route,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        when (val content = uiState.content) {
            is ActionsContent.Loading -> MoLoadingState(
                message = "Loading actions…",
                modifier = Modifier.padding(padding),
            )
            is ActionsContent.Loaded -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                item {
                    Text(
                        text = "${content.items.size} items need attention",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.xs,
                        ),
                    )
                }
                items(content.items, key = { it.messageId }) { item ->
                    ActionRow(item = item, onClick = { onOpenThread(item.threadId) })
                }
            }
            is ActionsContent.Empty -> MoEmptyState(
                title = "You're all caught up",
                message = if (content.hasAccount) {
                    "Nothing needs your attention right now."
                } else {
                    "Connect a Gmail account to start organizing your mail."
                },
                modifier = Modifier.padding(padding),
            )
            is ActionsContent.Error -> MoErrorState(
                message = content.message,
                onRetry = { /* re-collect on next subscription */ },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ActionRow(
    item: ActionItem,
    onClick: () -> Unit,
) {
    var showWhy by remember(item.messageId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
            .semantics {
                contentDescription = buildString {
                    append(if (item.unread) "Unread. " else "")
                    append("Needs attention: ${item.subject}, from ${item.senderName}.")
                }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.senderName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (item.unread) FontWeight.SemiBold
                        else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.priority != null) {
                PriorityBadge(priority = item.priority)
                Spacer(Modifier.width(MoSpacing.xs))
            }
            Text(
                text = MailFormatting.relativeTime(
                    item.timestampEpochMs,
                    System.currentTimeMillis(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!item.explanation.isNullOrBlank()) {
            Spacer(Modifier.height(MoSpacing.xs))
            androidx.compose.material3.TextButton(
                onClick = { showWhy = !showWhy },
                modifier = Modifier.semantics {
                    contentDescription = if (showWhy) "Hide explanation." else "Show why."
                },
            ) {
                Text(if (showWhy) "Hide why" else "Why?")
            }
            if (showWhy) {
                Text(
                    text = item.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MoSpacing.md),
                )
            }
        }
    }
}
