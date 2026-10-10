package com.greninjaop.mailorganizer.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionRecord
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionStatus
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Advanced Automation Dashboard & Rule Management Screen (Phase 27 §58–§68).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel,
    onNavigateToEditor: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var confirmationRuleId by remember { mutableStateOf<String?>(null) }
    var confirmationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AutomationUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is AutomationUiEvent.RequireConfirmation -> {
                    confirmationRuleId = event.ruleId
                    confirmationMessage = event.message
                }
            }
        }
    }

    if (confirmationRuleId != null && confirmationMessage != null) {
        AlertDialog(
            onDismissRequest = {
                confirmationRuleId = null
                confirmationMessage = null
            },
            title = { Text("Confirm Automation Execution") },
            text = { Text(confirmationMessage ?: "") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rid = confirmationRuleId!!
                        confirmationRuleId = null
                        confirmationMessage = null
                        viewModel.runRuleManually(rid, state.activeAccountId ?: "", isConfirmed = true)
                    }
                ) {
                    Text("Confirm & Run")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmationRuleId = null
                        confirmationMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Advanced Automation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { onNavigateToEditor(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Automation Rule")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = MoSpacing.md,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Rules (${state.rules.size})") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Execution History (${state.history.size})") },
                )
            }

            if (state.isLoading) {
                MoLoadingState(message = "Loading automations…")
            } else when (selectedTab) {
                0 -> RulesListTab(
                    rules = state.rules,
                    activeAccountId = state.activeAccountId,
                    onToggle = { id, enabled -> viewModel.toggleRule(id, enabled) },
                    onRun = { id -> viewModel.runRuleManually(id, state.activeAccountId ?: "") },
                    onDelete = { id -> viewModel.deleteRule(id) },
                    onEdit = { id -> onNavigateToEditor(id) },
                )
                1 -> HistoryListTab(
                    history = state.history,
                )
            }
        }
    }
}

@Composable
private fun RulesListTab(
    rules: List<AutomationRule>,
    activeAccountId: String?,
    onToggle: (String, Boolean) -> Unit,
    onRun: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    if (rules.isEmpty()) {
        MoEmptyState(
            title = "No Automations Configured",
            message = "Automations execute safe actions when incoming emails match deterministic conditions.",
            actionLabel = null,
            onAction = null,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        items(rules, key = { it.id }) { rule ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(rule.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (rule.description.isNotBlank()) {
                                Text(
                                    text = rule.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { onToggle(rule.id, it) },
                        )
                    }

                    Spacer(modifier = Modifier.height(MoSpacing.xs))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(MoSpacing.xs))

                    Text(
                        text = "When: ${rule.trigger.type.name}" + (if (rule.trigger.parameter != null) " (${rule.trigger.parameter})" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Then: ${rule.actions.joinToString { it.type.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Scope: ${if (rule.scope.specificAccountId != null) "Account ${rule.scope.specificAccountId}" else "All accounts"} · Policy: ${rule.confirmationPolicy.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onRun(rule.id) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Run Manually")
                        }
                        IconButton(onClick = { onDelete(rule.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Rule")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryListTab(
    history: List<AutomationExecutionRecord>,
) {
    if (history.isEmpty()) {
        MoEmptyState(
            title = "No Execution History",
            message = "When automations run in the background or manually, execution receipts will appear here.",
            actionLabel = null,
            onAction = null,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        items(history, key = { it.executionId }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.automationName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        StatusPill(status = item.status)
                    }
                    Text(
                        text = "Actions: ${item.actionSummary}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (item.detailMessage != null) {
                        Text(
                            text = item.detailMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = MailFormatting.relativeTime(item.executedAtEpochMs, System.currentTimeMillis()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: AutomationExecutionStatus) {
    val (bgColor, textColor) = when (status) {
        AutomationExecutionStatus.SUCCEEDED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        AutomationExecutionStatus.FAILED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        AutomationExecutionStatus.WAITING_CONFIRMATION -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        AutomationExecutionStatus.SKIPPED -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        else -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
