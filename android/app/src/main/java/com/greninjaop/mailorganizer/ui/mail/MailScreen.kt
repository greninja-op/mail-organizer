package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlinx.coroutines.launch

/**
 * Core inbox screen (Phase 6).
 *
 * Gmail-familiar chrome (design.md): top app bar with drawer affordance,
 * search field and account avatar; navigation drawer with the six mailbox
 * destinations (All Inbox / Primary / Promotional / Social / Spam / Starred).
 * Classification-backed destinations show honest empty states until Phase 7 —
 * never fabricated categories (§59).
 *
 * Read-only: no mark-read/star/archive actions (phase §6, Phase 22 owns
 * Gmail writes). The refresh action delegates to Phase 4's sync engine and
 * reports its honest outcome.
 */
@Composable
fun MailScreen(
    viewModel: MailViewModel,
    onOpenThread: (threadId: String, focusMessageId: String?) -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.syncUi) {
        val result = state.syncUi as? SyncUiState.Result ?: return@LaunchedEffect
        snackbar.showSnackbar(result.message)
        viewModel.consumeSyncResult()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MailDrawer(
                state = state,
                onSelect = { dest ->
                    viewModel.setDestination(dest)
                    scope.launch { drawerState.close() }
                },
            )
        },
        modifier = modifier,
    ) {
        Scaffold(
            topBar = {
                MailTopBar(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onFilterChange = viewModel::setFilterText,
                    onRefresh = viewModel::refresh,
                    onAccountClick = onOpenAccounts,
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (state.isSampleData) {
                    SampleDataBanner()
                }
                if (state.isOffline) {
                    OfflineBanner()
                }
                val sync = state.syncUi
                if (sync is SyncUiState.Syncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = sync.stageText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.xs,
                        ),
                    )
                }
                MailContent(
                    state = state,
                    onOpenThread = onOpenThread,
                    onRetry = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    onClearFilter = viewModel::clearFilter,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MailTopBar(
    state: MailScreenState,
    onMenuClick: () -> Unit,
    onFilterChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit,
) {
    val syncing = state.syncUi is SyncUiState.Syncing
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Open mailbox menu")
            }
        },
        title = {
            TextField(
                value = state.filterText,
                onValueChange = onFilterChange,
                placeholder = { Text("Search mail") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.filterText.isNotEmpty()) {
                        IconButton(onClick = { onFilterChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(MoSpacing.xxl),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !syncing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
            }
            val account = state.activeAccount
            if (account != null) {
                IconButton(onClick = onAccountClick) {
                    AccountAvatar(account = account, size = MoSpacing.xxl)
                }
            }
        },
    )
}

@Composable
private fun MailDrawer(
    state: MailScreenState,
    onSelect: (MailboxDestination) -> Unit,
) {
    ModalDrawerSheet {
        val account = state.activeAccount
        Column(
            modifier = Modifier.padding(MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        ) {
            if (account != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountAvatar(account = account, size = MoSpacing.huge)
                    Spacer(Modifier.width(MoSpacing.sm))
                    Column {
                        Text(
                            text = account.displayName ?: account.emailAddress,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = account.emailAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = MoSpacing.xs))
            }
            MailboxDestination.entries.forEach { dest ->
                NavigationDrawerItem(
                    label = { Text(dest.title) },
                    selected = state.destination == dest,
                    onClick = { onSelect(dest) },
                    icon = {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

private val MailboxDestination.icon
    @Composable get() = when (this) {
        MailboxDestination.ALL_INBOX -> Icons.Filled.Email
        MailboxDestination.PRIMARY -> Icons.Filled.MailOutline
        MailboxDestination.PROMOTIONAL -> Icons.Filled.ShoppingCart
        MailboxDestination.SOCIAL -> Icons.Filled.Person
        MailboxDestination.SPAM -> Icons.Filled.Warning
        MailboxDestination.STARRED -> Icons.Filled.Star
    }

@Composable
private fun SampleDataBanner() {
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
                modifier = Modifier.semantics {
                    contentDescription =
                        "Sample data: the mail shown is fixture data for development, not real Gmail"
                },
            )
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(MoSpacing.xs))
            Text(
                text = "You're offline — showing synced mail",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MailContent(
    state: MailScreenState,
    onOpenThread: (threadId: String, focusMessageId: String?) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val content = state.content) {
        MailboxContent.Loading -> MoLoadingState(
            message = "Loading your mail…",
            modifier = modifier,
        )
        is MailboxContent.Threads -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(content.items, key = { it.threadId }) { item ->
                    ThreadRow(
                        item = item,
                        account = state.activeAccount,
                        showAccountIndicator =
                            state.destination == MailboxDestination.ALL_INBOX,
                        onClick = { onOpenThread(item.threadId, null) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = MoSpacing.md),
                    )
                }
                if (content.hasMore) {
                    item {
                        Button(
                            onClick = onLoadMore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MoSpacing.md),
                        ) {
                            Text("Load more")
                        }
                    }
                }
            }
        }
        is MailboxContent.Messages -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(content.items, key = { it.messageId }) { item ->
                    StarredMessageRow(
                        item = item,
                        account = state.activeAccount,
                        onClick = { onOpenThread(item.threadId, item.messageId) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = MoSpacing.md),
                    )
                }
            }
        }
        is MailboxContent.Empty -> {
            when (content.kind) {
                EmptyKind.NO_MAIL -> MoEmptyState(
                    title = "No synchronized email yet",
                    message = "Connect a Gmail account to see your mail here. " +
                        "Gmail connection arrives in a later phase.",
                    actionLabel = "Sync now",
                    onAction = onRetry,
                    modifier = modifier,
                )
                EmptyKind.NO_FILTER_RESULTS -> MoEmptyState(
                    title = "No matches",
                    message = "Nothing in this mailbox matches your search.",
                    actionLabel = "Clear search",
                    onAction = onClearFilter,
                    modifier = modifier,
                )
                EmptyKind.NOT_CLASSIFIED_YET -> MoEmptyState(
                    title = "Nothing here yet",
                    message = "${state.destination.title} mail will appear here once " +
                        "mail classification arrives in Phase 7.",
                    modifier = modifier,
                )
                EmptyKind.NO_STARRED -> MoEmptyState(
                    title = "No starred mail",
                    message = "Messages you star will appear here.",
                    modifier = modifier,
                )
            }
        }
        is MailboxContent.Error -> MoErrorState(
            message = content.message,
            onRetry = onRetry,
            modifier = modifier,
        )
    }
}
