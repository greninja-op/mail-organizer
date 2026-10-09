package com.greninjaop.mailorganizer.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.navigation.MoAppTopBar
import com.greninjaop.mailorganizer.ui.navigation.MoNavBar
import com.greninjaop.mailorganizer.ui.navigation.PrimaryDestination
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/** Factory for the Phase 14 Actions destination. */
class ActionsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ActionsViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            review = container.reviewActionUseCase,
            dispatchers = container.dispatchers,
            activeAccountPreferences = container.activeAccountPreferences,
        ) as T
    }
}

/**
 * Actions destination — Phase 14 (phase §12–§15, §31).
 *
 * Shows the action engine's cards. Every card identifies its source email
 * (§14); external proposals go through the confirmation dialog (§31) and
 * are honestly reported as "not connected" when no integration exists
 * (§32) — never a fake success.
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
    val snackbar = remember { SnackbarHostState() }
    var confirmCard by remember { mutableStateOf<ActionCardUi?>(null) }
    val now = System.currentTimeMillis()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActionsEvent.ConfirmAction -> confirmCard = event.card
                is ActionsEvent.OpenThread -> onOpenThread(event.threadId)
                is ActionsEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    confirmCard?.let { card ->
        ActionConfirmDialog(
            card = card,
            onDismiss = { confirmCard = null },
            onConfirm = {
                confirmCard = null
                viewModel.onConfirmDialogResult(card, confirmed = true)
            },
        )
    }

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
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val content = uiState.content) {
            is ActionsContent.Loading -> MoLoadingState(
                message = "Loading actions…",
                modifier = Modifier.padding(padding),
            )
            is ActionsContent.Loaded -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
            ) {
                item {
                    Text(
                        text = "${content.cards.size} suggested actions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.xs,
                        ),
                    )
                }
                items(content.cards, key = { it.id }) { card ->
                    ActionCard(
                        card = card,
                        nowEpochMs = now,
                        onOpenSource = { viewModel.onOpenSource(card) },
                        onReview = { viewModel.onReview(card) },
                        onAct = { viewModel.onAct(card) },
                        onDismiss = { viewModel.onDismiss(card) },
                        modifier = Modifier.padding(horizontal = MoSpacing.md),
                    )
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

/**
 * Confirmation dialog (phase §31): what will happen, which email caused
 * it, what data is used, which external service is affected, and that
 * confirmation is required (§11). Confirming never performs the external
 * action in Phase 14 — the outcome is reported honestly (§32).
 */
@Composable
private fun ActionConfirmDialog(
    card: ActionCardUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val effectLabel = when (card.externalEffect) {
        ExternalEffect.CALENDAR -> "Create a Calendar event"
        ExternalEffect.TASKS -> "Create a task"
        ExternalEffect.GMAIL_WRITE -> "Send via Gmail"
        ExternalEffect.NONE -> "Review"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm action") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                Text("You are about to:", style = MaterialTheme.typography.labelMedium)
                Text(effectLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    "From: ${card.senderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Source: “${card.subject}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                card.dueDateEpochMs?.let { due ->
                    Text(
                        "When: ${MailFormatting.relativeTime(due, System.currentTimeMillis())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "The Calendar integration is not connected yet. Confirming " +
                        "saves your intent locally — nothing external will happen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.semantics {
                    contentDescription = "Confirm. Nothing external will happen yet."
                },
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
