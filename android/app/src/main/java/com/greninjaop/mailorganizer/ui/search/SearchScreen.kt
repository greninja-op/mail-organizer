package com.greninjaop.mailorganizer.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.greninjaop.mailorganizer.core.search.SearchDatePreset
import com.greninjaop.mailorganizer.core.search.SearchIndexState
import com.greninjaop.mailorganizer.core.search.SearchResult
import com.greninjaop.mailorganizer.core.search.SearchResultType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoErrorState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Dedicated search experience (Phase 10, phase §9–§11).
 *
 * - Search field with clear action and proper IME behavior (§11).
 * - Structured filter controls (§28–§36); no filter builds SQL — the
 *   ViewModel assembles a [SearchQuery] (§13).
 * - Honest states: landing, loading, results, empty, error-with-rebuild,
 *   offline banner (§51–§53, §60).
 * - Calm, uncluttered Material 3 layout using app design tokens (§9, §65).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenThread: (threadId: String, focusMessageId: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        // Autofocus the field when the screen opens (§11).
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    TextField(
                        value = state.queryText,
                        onValueChange = viewModel::setQueryText,
                        placeholder = { Text("Search mail") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (state.queryText.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() },
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isOffline) {
                OfflineBanner()
            }
            if (state.indexing) {
                Text(
                    text = "Updating search index…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = MoSpacing.md,
                        vertical = MoSpacing.xs,
                    ),
                )
            }
            SearchFilterBar(
                filters = state.filters,
                onToggleActionRequired = viewModel::toggleActionRequired,
                onToggleUnread = viewModel::toggleUnreadOnly,
                onToggleAttachments = viewModel::toggleHasAttachment,
                onSetCategory = viewModel::setCategory,
                onSetPriority = viewModel::setPriority,
                onSetDatePreset = viewModel::setDatePreset,
                onClearSender = { viewModel.setSenderFilter(null) },
                onClearCompany = { viewModel.setCompanyFilter(null, null) },
                onClearAll = viewModel::clearFilters,
            )
            when (val content = state.content) {
                is SearchContent.Landing -> SearchLanding(
                    hasAccount = state.activeAccount != null,
                )
                is SearchContent.Loading -> MoLoadingState(
                    message = "Searching…",
                    modifier = Modifier.fillMaxSize(),
                )
                is SearchContent.Results -> SearchResults(
                    state = state,
                    outcome = content.outcome,
                    onOpenThread = onOpenThread,
                    onSetResultType = viewModel::setResultType,
                    onSenderClick = { viewModel.setSenderFilter(it.emailAddress) },
                    onCompanyClick = {
                        viewModel.setCompanyFilter(
                            it.companyId,
                            it.userOverrideName ?: it.canonicalName,
                        )
                    },
                )
                is SearchContent.Empty -> MoEmptyState(
                    title = "No messages found",
                    message = content.hint,
                    actionLabel = if (state.filters.hasActive) "Clear filters" else null,
                    onAction = if (state.filters.hasActive) viewModel::clearFilters else null,
                )
                is SearchContent.Error -> MoErrorState(
                    message = content.message,
                    onRetry = viewModel::rebuildIndex,
                    retryLabel = "Rebuild index",
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = "You're offline — search works on your synced mail",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
    )
}

/** Landing state: what search can do, no database work (phase §51). */
@Composable
private fun SearchLanding(hasAccount: Boolean, modifier: Modifier = Modifier) {
    MoEmptyState(
        title = "Search your mail",
        message = if (hasAccount) {
            "Search senders, subjects, and message text across your synced mail. " +
                "Use \"quotes\" for exact phrases. Everything stays on this device."
        } else {
            "Connect an account to search your mail."
        },
        modifier = modifier,
    )
}

@Composable
private fun SearchResults(
    state: SearchScreenState,
    outcome: com.greninjaop.mailorganizer.core.search.SearchOutcome,
    onOpenThread: (threadId: String, focusMessageId: String?) -> Unit,
    onSetResultType: (SearchResultType) -> Unit,
    onSenderClick: (com.greninjaop.mailorganizer.data.local.SenderRecord) -> Unit,
    onCompanyClick: (com.greninjaop.mailorganizer.data.local.CompanyRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val terms = outcome.parsed.terms
    val phrases = outcome.parsed.phrases
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = MoSpacing.md)) {
                Spacer(Modifier.height(MoSpacing.xs))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedButton(
                        selected = state.resultType == SearchResultType.MESSAGES,
                        onClick = { onSetResultType(SearchResultType.MESSAGES) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("Messages") },
                    )
                    SegmentedButton(
                        selected = state.resultType == SearchResultType.THREADS,
                        onClick = { onSetResultType(SearchResultType.THREADS) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("Threads") },
                    )
                }
                Spacer(Modifier.height(MoSpacing.xs))
            }
        }
        if (outcome.senderSuggestions.isNotEmpty() || outcome.companySuggestions.isNotEmpty()) {
            item {
                PeopleCompanySuggestions(
                    senders = outcome.senderSuggestions,
                    companies = outcome.companySuggestions,
                    onSenderClick = onSenderClick,
                    onCompanyClick = onCompanyClick,
                )
            }
        }
        items(
            items = outcome.results,
            key = { result ->
                when (result) {
                    is SearchResult.Message -> "m:${result.record.messageId}"
                    is SearchResult.Thread -> "t:${result.thread.threadId}"
                    is SearchResult.Sender -> "s:${result.sender.senderId}"
                    is SearchResult.Company -> "c:${result.company.companyId}"
                }
            },
        ) { result ->
            when (result) {
                is SearchResult.Message -> SearchMessageRow(
                    result = result,
                    terms = terms,
                    phrases = phrases,
                    onClick = {
                        onOpenThread(result.record.threadId, result.record.messageId)
                    },
                )
                is SearchResult.Thread -> SearchThreadRow(
                    result = result,
                    terms = terms,
                    phrases = phrases,
                    onClick = { onOpenThread(result.thread.threadId, null) },
                )
                // Sender/Company results render as suggestion rows above;
                // they never appear as primary hits (phase §8).
                is SearchResult.Sender,
                is SearchResult.Company,
                -> Unit
            }
        }
    }
}

/** Horizontally scrollable structured filter controls (phase §28). */
@Composable
private fun SearchFilterBar(
    filters: SearchFilters,
    onToggleActionRequired: () -> Unit,
    onToggleUnread: () -> Unit,
    onToggleAttachments: () -> Unit,
    onSetCategory: (MailCategory?) -> Unit,
    onSetPriority: (Priority?) -> Unit,
    onSetDatePreset: (SearchDatePreset) -> Unit,
    onClearSender: () -> Unit,
    onClearCompany: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            MenuFilterChip(
                label = "Category",
                selectedLabel = filters.category?.let(::categoryLabel),
                options = listOf(null to "Any") + MailCategory.entries.map { it to categoryLabel(it) },
                onSelect = onSetCategory,
            )
        }
        item {
            MenuFilterChip(
                label = "Priority",
                selectedLabel = filters.priority?.let(::priorityLabel),
                options = listOf(null to "Any") + Priority.entries.map { it to priorityLabel(it) },
                onSelect = onSetPriority,
            )
        }
        item {
            MenuFilterChip(
                label = "Date",
                selectedLabel = if (filters.datePreset == SearchDatePreset.ANY_TIME) {
                    null
                } else {
                    datePresetLabel(filters.datePreset)
                },
                options = SearchDatePreset.entries.map { it to datePresetLabel(it) },
                onSelect = onSetDatePreset,
            )
        }
        item {
            FilterChip(
                selected = filters.actionRequiredOnly,
                onClick = onToggleActionRequired,
                label = { Text("Action required") },
            )
        }
        item {
            FilterChip(
                selected = filters.unreadOnly,
                onClick = onToggleUnread,
                label = { Text("Unread") },
            )
        }
        item {
            FilterChip(
                selected = filters.hasAttachmentOnly,
                onClick = onToggleAttachments,
                label = { Text("Attachments") },
            )
        }
        if (!filters.sender.isNullOrBlank()) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearSender,
                    label = { Text("From: ${filters.sender!!.take(24)}") },
                    trailingIcon = {
                        Icon(Icons.Filled.Close, contentDescription = "Remove sender filter")
                    },
                )
            }
        }
        if (filters.companyId != null) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearCompany,
                    label = { Text(filters.companyName ?: "Company") },
                    trailingIcon = {
                        Icon(Icons.Filled.Close, contentDescription = "Remove company filter")
                    },
                )
            }
        }
        if (filters.hasActive) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onClearAll,
                    label = { Text("Clear all") },
                )
            }
        }
    }
}

/** A filter chip that opens a small option menu. */
@Composable
private fun <T> MenuFilterChip(
    label: String,
    selectedLabel: String?,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterChip(
            selected = selectedLabel != null,
            onClick = { expanded = true },
            label = { Text(selectedLabel ?: label) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for ((value, text) in options) {
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun categoryLabel(category: MailCategory): String = when (category) {
    MailCategory.ACTION_REQUIRED -> "Action required"
    MailCategory.IMPORTANT -> "Important"
    MailCategory.CAREER -> "Career"
    MailCategory.EDUCATION -> "Education"
    MailCategory.RECEIPTS_ORDERS -> "Receipts & orders"
    MailCategory.SECURITY -> "Security"
    MailCategory.NOTIFICATIONS -> "Notifications"
    MailCategory.NEWSLETTERS -> "Newsletters"
    MailCategory.PROMOTIONS -> "Promotions"
    MailCategory.LOW_VALUE -> "Low value"
    MailCategory.UNCLASSIFIED -> "Unclassified"
}

private fun priorityLabel(priority: Priority): String = when (priority) {
    Priority.LOW -> "Low"
    Priority.NORMAL -> "Normal"
    Priority.HIGH -> "High"
    Priority.CRITICAL -> "Critical"
}

private fun datePresetLabel(preset: SearchDatePreset): String = when (preset) {
    SearchDatePreset.ANY_TIME -> "Any time"
    SearchDatePreset.TODAY -> "Today"
    SearchDatePreset.LAST_7_DAYS -> "Last 7 days"
    SearchDatePreset.LAST_30_DAYS -> "Last 30 days"
}

/** Index-degraded hint shown above results when the index needs attention. */
@Composable
@Suppress("unused")
private fun IndexStateHint(indexState: SearchIndexState?) {
    if (indexState == SearchIndexState.DEGRADED) {
        Text(
            text = "Search index unavailable — results may be incomplete.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
        )
    }
}
