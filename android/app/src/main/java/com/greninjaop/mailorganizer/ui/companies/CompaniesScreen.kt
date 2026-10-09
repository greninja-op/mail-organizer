package com.greninjaop.mailorganizer.ui.companies

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

/** Factory for the Phase 11 Companies destination. */
class CompaniesViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CompaniesViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
        ) as T
    }
}

/**
 * Companies destination — Phase 11 (phase §25–§28).
 *
 * Company → senders → messages. Each row shows the company name, domain,
 * and a real message count (§26). The list never overloads: name, domain,
 * count, nothing more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompaniesScreen(
    viewModel: CompaniesViewModel,
    onNavigate: (String) -> Unit,
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
                title = "Companies",
                account = account,
                onOpenSearch = onOpenSearch,
                onOpenAccounts = { onNavigate(AppDestinations.ACCOUNTS) },
                onOpenSecondary = onNavigate,
            )
        },
        bottomBar = {
            MoNavBar(
                currentRoute = PrimaryDestination.COMPANIES.route,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        when (val content = uiState.content) {
            is CompaniesContent.Loading -> MoLoadingState(
                message = "Loading companies…",
                modifier = Modifier.padding(padding),
            )
            is CompaniesContent.Ready -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                items(content.companies, key = { it.record.companyId }) { entry ->
                    val name = entry.record.userOverrideName
                        ?: entry.record.canonicalName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCompany(entry.record.companyId) }
                            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
                            .semantics {
                                contentDescription =
                                    "$name, ${entry.messageCount} messages"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = entry.record.normalizedDomain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${entry.messageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            is CompaniesContent.Empty -> MoEmptyState(
                title = "No companies yet",
                message = if (content.hasAccount) {
                    "Companies appear here once your mail is organized. " +
                        "Senders that can't be resolved to a company are never hidden " +
                        "— they simply have no company row."
                } else {
                    "Connect a Gmail account to start organizing your mail."
                },
                modifier = Modifier.padding(padding),
            )
            is CompaniesContent.Error -> MoErrorState(
                message = content.message,
                onRetry = { /* re-collect on next subscription */ },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * Company detail — Phase 11 (phase §27): company name, domains, recent
 * messages, category distribution. Deliberately not a full analytics
 * dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    viewModel: CompaniesViewModel,
    onOpenThread: (threadId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail by viewModel.detail.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val d = detail) {
                            is CompanyDetailContent.Loaded ->
                                d.company.userOverrideName ?: d.company.canonicalName
                            else -> "Company"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to companies",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val content = detail) {
            is CompanyDetailContent.Loading -> MoLoadingState(
                message = "Loading company…",
                modifier = Modifier.padding(padding),
            )
            is CompanyDetailContent.Loaded -> {
                val name = content.company.userOverrideName
                    ?: content.company.canonicalName
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    item {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            ),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = content.company.normalizedDomain,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(MoSpacing.xs))
                            Text(
                                text = "${content.messages.size} messages",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (content.categoryCounts.isNotEmpty()) {
                                Spacer(Modifier.height(MoSpacing.xs))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                                ) {
                                    content.categoryCounts.entries
                                        .sortedByDescending { it.value }
                                        .take(4)
                                        .forEach { (category, count) ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                CategoryChip(category = category)
                                                Text(
                                                    text = "$count",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                }
                            }
                        }
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
                            if (message.category != null) {
                                CategoryChip(category = message.category)
                                Spacer(Modifier.width(MoSpacing.sm))
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
                }
            }
            is CompanyDetailContent.NotFound -> MoEmptyState(
                title = "Company not found",
                message = "This company is no longer in the list.",
                actionLabel = "Back",
                onAction = onBack,
                modifier = Modifier.padding(padding),
            )
            is CompanyDetailContent.Error -> MoErrorState(
                message = content.message,
                onRetry = onBack,
                retryLabel = "Back",
                modifier = Modifier.padding(padding),
            )
        }
    }
}
