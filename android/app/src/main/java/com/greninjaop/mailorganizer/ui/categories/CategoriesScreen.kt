package com.greninjaop.mailorganizer.ui.categories

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.CategoryChip
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.navigation.MoAppTopBar
import com.greninjaop.mailorganizer.ui.navigation.MoNavBar
import com.greninjaop.mailorganizer.ui.navigation.PrimaryDestination
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/** Factory for the Phase 11 Categories destination. */
class CategoriesViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CategoriesViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
            activeAccountPreferences = container.activeAccountPreferences,
        ) as T
    }
}

/**
 * Categories destination — Phase 11 (phase §21–§24).
 *
 * Category browsing backed by Phase 7 classification. Counts are real
 * local counts (§23); empty categories get an honest empty state (§24).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigate: (String) -> Unit,
    onOpenCategory: (MailCategory) -> Unit,
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
                title = "Categories",
                account = account,
                onOpenSearch = onOpenSearch,
                onOpenAccounts = { onNavigate(AppDestinations.ACCOUNTS) },
                onOpenSecondary = onNavigate,
            )
        },
        bottomBar = {
            MoNavBar(
                currentRoute = PrimaryDestination.CATEGORIES.route,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        when (val content = uiState.content) {
            is CategoriesContent.Loading -> MoLoadingState(
                message = "Loading categories…",
                modifier = Modifier.padding(padding),
            )
            is CategoriesContent.Ready -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                items(
                    MailCategory.entries
                        .filter { (content.counts[it] ?: 0) > 0 }
                        .sortedByDescending { content.counts[it] ?: 0 },
                    key = { it.name },
                ) { category ->
                    val visuals = category.visuals()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCategory(category) }
                            .padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            )
                            .semantics {
                                contentDescription =
                                    "${visuals.label}, ${content.counts[category]} messages"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = visuals.label,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${content.counts[category]} messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        CategoryChip(category = category)
                    }
                }
            }
            is CategoriesContent.Empty -> MoEmptyState(
                title = "No categories yet",
                message = if (content.hasAccount) {
                    "Mail hasn't been classified yet. Categories appear here once " +
                        "your mail is organized."
                } else {
                    "Connect a Gmail account to start organizing your mail."
                },
                modifier = Modifier.padding(padding),
            )
            is CategoriesContent.Error -> MoErrorState(
                message = content.message,
                onRetry = { /* re-collect on next subscription */ },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * Category detail — messages in one category (phase §22).
 *
 * Shown as a separate route (`category/{name}`); the category name stays
 * visible as the title so context is never lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    viewModel: CategoriesViewModel,
    category: MailCategory,
    onOpenThread: (threadId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail by viewModel.detail.collectAsState()
    val visuals = category.visuals()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(visuals.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to categories",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val content = detail) {
            is CategoryDetailContent.Loading -> MoLoadingState(
                message = "Loading ${visuals.label.lowercase()}…",
                modifier = Modifier.padding(padding),
            )
            is CategoryDetailContent.Loaded -> {
                if (content.messages.isEmpty()) {
                    MoEmptyState(
                        title = "No ${visuals.label.lowercase()} emails yet",
                        message = "Nothing in this category right now.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                        item {
                            Text(
                                text = "${content.messages.size} messages",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = MoSpacing.md,
                                    vertical = MoSpacing.xs,
                                ),
                            )
                        }
                        items(content.messages, key = { it.messageId }) { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenThread(message.threadId) }
                                    .padding(
                                        horizontal = MoSpacing.md,
                                        vertical = MoSpacing.sm,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = message.senderName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (message.unread) FontWeight.SemiBold
                                            else FontWeight.Normal,
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
                                Spacer(Modifier.width(MoSpacing.sm))
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
                    }
                }
            }
            is CategoryDetailContent.Error -> MoErrorState(
                message = content.message,
                onRetry = onBack,
                retryLabel = "Back",
                modifier = Modifier.padding(padding),
            )
        }
    }
}
