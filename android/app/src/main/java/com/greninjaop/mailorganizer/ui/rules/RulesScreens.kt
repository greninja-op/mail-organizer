package com.greninjaop.mailorganizer.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.greninjaop.mailorganizer.core.rules.RuleAction
import com.greninjaop.mailorganizer.core.rules.RuleActionType
import com.greninjaop.mailorganizer.core.rules.RuleConditionField
import com.greninjaop.mailorganizer.core.rules.RuleOperator
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.domain.rules.RulePreview
import com.greninjaop.mailorganizer.domain.rules.UserRule
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.rules.RuleEditorModels.ActionDraft
import com.greninjaop.mailorganizer.ui.rules.RuleEditorModels.ConditionDraft
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Phase 12 — Rules list screen (§9, §23).
 *
 * Shows every rule as a natural-language summary (UserRule.describe()),
 * grouped into Active and Disabled, with enable/disable switches and
 * delete-with-confirmation. Conflicts detected from local data are
 * surfaced at the top with the winning rule named.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: RulesViewModel,
    onAddRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Rules") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRule,
                modifier = Modifier.semantics { contentDescription = "Add rule" },
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            RuleLoading(modifier = Modifier.padding(padding))
        } else if (state.error != null) {
            RuleError(message = state.error!!, modifier = Modifier.padding(padding))
        } else if (state.noAccount) {
            RuleError(
                message = "Add an account before creating rules.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
            ) {
                if (state.conflicts.isNotEmpty()) {
                    item {
                        ConflictBanner(
                            conflicts = state.conflicts,
                            modifier = Modifier.padding(horizontal = MoSpacing.md),
                        )
                    }
                }
                val active = state.rules.filter { it.enabled }
                val disabled = state.rules.filter { !it.enabled }
                if (active.isNotEmpty()) {
                    item { SectionHeader("Active") }
                    items(active, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = { viewModel.toggleEnabled(rule) },
                            onEdit = { onEditRule(rule.id) },
                            onDelete = { viewModel.requestDelete(rule) },
                            modifier = Modifier.padding(horizontal = MoSpacing.md),
                        )
                    }
                }
                if (disabled.isNotEmpty()) {
                    item { SectionHeader("Disabled") }
                    items(disabled, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = { viewModel.toggleEnabled(rule) },
                            onEdit = { onEditRule(rule.id) },
                            onDelete = { viewModel.requestDelete(rule) },
                            modifier = Modifier.padding(horizontal = MoSpacing.md),
                        )
                    }
                }
                if (state.rules.isEmpty()) {
                    item {
                        Text(
                            text = "No rules yet. Rules apply your preferences " +
                                "automatically — create one to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(MoSpacing.lg),
                        )
                    }
                }
                item { Spacer(Modifier.height(MoSpacing.xl)) }
            }
        }
    }

    state.pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete rule?") },
            text = { Text("“${rule.name}” will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
    )
}

@Composable
private fun ConflictBanner(conflicts: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "Conflicting rules",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(MoSpacing.xs))
            for (conflict in conflicts) {
                Text(
                    text = conflict,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: UserRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Column(modifier = Modifier.padding(MoSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.semantics {
                        contentDescription = if (rule.enabled) "Disable ${rule.name}" else "Enable ${rule.name}"
                    },
                )
            }
            Text(
                text = rule.describe(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) { Text("Edit") }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.semantics { contentDescription = "Delete ${rule.name}" },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete ${rule.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleLoading(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "Loading rules…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(MoSpacing.lg),
        )
    }
}

@Composable
private fun RuleError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.padding(MoSpacing.lg),
    )
}

/**
 * Phase 12 — rule editor screen (§16–§23).
 *
 * Structured builder: rule name, condition rows (field / operator /
 * value), action rows (type / value), live preview against real mailbox
 * data (exact count + samples, §19), and conflict warnings (§23).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    viewModel: RuleEditorViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isLoading) "Rule" else "Edit rule") },
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
    ) { padding ->
        if (state.isLoading) {
            RuleLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.md),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Rule name") },
                placeholder = { Text("e.g. Newsletters from work") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(text = "When", style = MaterialTheme.typography.titleSmall)
            state.conditions.forEachIndexed { index, draft ->
                ConditionRow(
                    draft = draft,
                    onChange = { viewModel.updateCondition(index, it) },
                    onRemove = { viewModel.removeCondition(index) },
                    showRemove = state.conditions.size > 1,
                )
            }
            OutlinedButton(onClick = viewModel::addCondition) {
                Text("Add condition")
            }

            Text(text = "Then", style = MaterialTheme.typography.titleSmall)
            state.actions.forEachIndexed { index, draft ->
                ActionRow(
                    draft = draft,
                    onChange = { viewModel.updateAction(index, it) },
                    onRemove = { viewModel.removeAction(index) },
                    showRemove = state.actions.size > 1,
                )
            }
            OutlinedButton(onClick = viewModel::addAction) {
                Text("Add action")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Enabled", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled,
                )
            }

            OutlinedTextField(
                value = state.order,
                onValueChange = viewModel::setOrder,
                label = { Text("Precedence order") },
                placeholder = { Text("0") },
                supportingText = {
                    Text("Lower numbers win when rules overlap. Leave 0 to append last.")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::refreshPreview,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Preview matches")
            }

            state.preview?.let { preview ->
                PreviewCard(preview = preview)
            }

            if (state.conflicts.isNotEmpty()) {
                ConflictBanner(conflicts = state.conflicts)
            }

            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving…" else "Save rule")
            }
            Spacer(Modifier.height(MoSpacing.lg))
        }
    }
}

@Composable
private fun ConditionRow(
    draft: ConditionDraft,
    onChange: (ConditionDraft) -> Unit,
    onRemove: () -> Unit,
    showRemove: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
        ) {
            EnumDropdown(
                label = "Field",
                options = RuleConditionField.entries.toList(),
                selected = draft.field,
                labelOf = ::fieldLabel,
                onSelect = { onChange(draft.copy(field = it)) },
            )
            EnumDropdown(
                label = "Operator",
                options = RuleOperator.entries.toList(),
                selected = draft.operator,
                labelOf = ::operatorLabel,
                onSelect = { onChange(draft.copy(operator = it)) },
            )
            OutlinedTextField(
                value = draft.value,
                onValueChange = { onChange(draft.copy(value = it)) },
                label = { Text("Value") },
                placeholder = { Text(valueHint(draft.field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showRemove) {
                TextButton(onClick = onRemove) {
                    Text("Remove condition", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    draft: ActionDraft,
    onChange: (ActionDraft) -> Unit,
    onRemove: () -> Unit,
    showRemove: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
        ) {
            EnumDropdown(
                label = "Action",
                options = RuleActionType.entries.toList(),
                selected = draft.type,
                labelOf = { it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.uppercase() } },
                onSelect = { onChange(draft.copy(type = it, value = defaultActionValue(it))) },
            )
            when (draft.type) {
                RuleActionType.SET_CATEGORY -> EnumDropdown(
                    label = "Category",
                    options = MailCategory.entries.toList(),
                    selected = runCatching { MailCategory.valueOf(draft.value) }
                        .getOrDefault(MailCategory.NEWSLETTERS),
                    labelOf = { it.visuals().label },
                    onSelect = { onChange(draft.copy(value = it.name)) },
                )
                RuleActionType.SET_PRIORITY -> EnumDropdown(
                    label = "Priority",
                    options = Priority.entries.toList(),
                    selected = runCatching { Priority.valueOf(draft.value) }
                        .getOrDefault(Priority.NORMAL),
                    labelOf = { it.visuals()?.label ?: it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onSelect = { onChange(draft.copy(value = it.name)) },
                )
            }
            if (showRemove) {
                TextButton(onClick = onRemove) {
                    Text("Remove action", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun defaultActionValue(type: RuleActionType): String = when (type) {
    RuleActionType.SET_CATEGORY -> MailCategory.NEWSLETTERS.name
    RuleActionType.SET_PRIORITY -> Priority.NORMAL.name
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewCard(preview: RulePreview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        ) {
            Text(
                text = "Matches ${preview.matchCount} message${if (preview.matchCount == 1) "" else "s"}",
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                for (sample in preview.samples) {
                    Text(
                        text = "• ${sample.subject.ifBlank { "(no subject)" }} — ${sample.sender}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Text("▾", style = MaterialTheme.typography.titleMedium)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun fieldLabel(field: RuleConditionField): String = when (field) {
    RuleConditionField.SENDER_EMAIL -> "Sender email"
    RuleConditionField.SENDER_DOMAIN -> "Sender domain"
    RuleConditionField.COMPANY_ID -> "Company"
    RuleConditionField.SUBJECT -> "Subject"
    RuleConditionField.GMAIL_LABEL -> "Gmail label"
    RuleConditionField.GMAIL_CATEGORY -> "Gmail category"
    RuleConditionField.BASE_CATEGORY -> "Current category"
    RuleConditionField.BASE_PRIORITY -> "Current priority"
    RuleConditionField.HAS_ATTACHMENT -> "Has attachment"
    RuleConditionField.HAS_UNSUBSCRIBE -> "Has unsubscribe link"
}

private fun operatorLabel(op: RuleOperator): String = when (op) {
    RuleOperator.EQUALS -> "is"
    RuleOperator.CONTAINS -> "contains"
}

private fun valueHint(field: RuleConditionField): String = when (field) {
    RuleConditionField.SENDER_EMAIL -> "name@example.com"
    RuleConditionField.SENDER_DOMAIN -> "example.com"
    RuleConditionField.COMPANY_ID -> "company domain, e.g. acme.com"
    RuleConditionField.SUBJECT -> "text in the subject"
    RuleConditionField.GMAIL_LABEL -> "e.g. INBOX"
    RuleConditionField.GMAIL_CATEGORY -> "e.g. promotions"
    RuleConditionField.BASE_CATEGORY -> MailCategory.NEWSLETTERS.name
    RuleConditionField.BASE_PRIORITY -> Priority.HIGH.name
    RuleConditionField.HAS_ATTACHMENT -> "true or false"
    RuleConditionField.HAS_UNSUBSCRIBE -> "true or false"
}
