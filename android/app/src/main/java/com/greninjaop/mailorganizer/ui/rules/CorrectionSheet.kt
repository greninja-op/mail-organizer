package com.greninjaop.mailorganizer.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.ui.mail.visuals
import com.greninjaop.mailorganizer.ui.rules.CorrectionViewModel.CorrectionScopeChoice
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Phase 12 — correction bottom sheet (§4–§6).
 *
 * Triggered by the "Correct" affordance on a message. Shows the current
 * category/priority (with a "Set by you" marker when the record's
 * overridden flag is true, §7), lets the user pick replacements, choose
 * the scope (message / sender / domain), and apply or undo.
 *
 * Corrections are explicit user authority: they win over rules (§6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrectionSheet(
    viewModel: CorrectionViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val current = state ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The parent closes the sheet once the correction is applied (§5).
    LaunchedEffect(current.done) {
        if (current.done) {
            viewModel.dismiss()
            onDismiss()
        }
    }

    var scope by remember(current.messageId) { mutableStateOf(CorrectionScopeChoice.MESSAGE) }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.dismiss()
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoSpacing.lg, vertical = MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.md),
        ) {
            Text(
                text = "Correct this email",
                style = MaterialTheme.typography.titleLarge,
            )

            // Current state with "Set by you" provenance (§7).
            CurrentStateRow(
                label = "Category",
                current = current.currentCategory?.visuals()?.label ?: "Not set",
                overridden = current.categoryOverridden,
            )
            CurrentStateRow(
                label = "Priority",
                current = current.currentPriority?.visuals()?.label
                    ?: current.currentPriority?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                    ?: "Not set",
                overridden = current.priorityOverridden,
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
            )
            CategoryPicker(
                selected = current.selectedCategory,
                onSelect = viewModel::selectCategory,
            )

            Text(
                text = "Priority",
                style = MaterialTheme.typography.titleSmall,
            )
            PriorityPicker(
                selected = current.selectedPriority,
                onSelect = viewModel::selectPriority,
            )

            Text(
                text = "Apply to",
                style = MaterialTheme.typography.titleSmall,
            )
            ScopePicker(
                scope = scope,
                senderEmail = current.senderEmail,
                senderDomain = current.senderDomain,
                onSelect = { scope = it },
            )

            current.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MoSpacing.sm),
            ) {
                OutlinedButton(
                    onClick = { viewModel.undoCorrection(scope) },
                    enabled = !current.isApplying && (current.categoryOverridden || current.priorityOverridden),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Undo my correction")
                }
                Button(
                    onClick = { viewModel.applyCorrection(scope) },
                    enabled = !current.isApplying &&
                        (current.selectedCategory != null || current.selectedPriority != null),
                    modifier = Modifier.weight(1f),
                ) {
                    if (current.isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics {
                                contentDescription = "Applying correction"
                            },
                        )
                    } else {
                        Text("Apply")
                    }
                }
            }
            Spacer(Modifier.height(MoSpacing.md))
        }
    }
}

@Composable
private fun CurrentStateRow(label: String, current: String, overridden: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        ) {
            Text(
                text = current,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (overridden) {
                Text(
                    text = "· Set by you",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { contentDescription = "Set by you" },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(
    selected: MailCategory?,
    onSelect: (MailCategory) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
    ) {
        for (category in MailCategory.entries) {
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category) },
                label = { Text(category.visuals().label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(),
                modifier = Modifier.semantics {
                    contentDescription = "Category ${category.visuals().label}"
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriorityPicker(
    selected: Priority?,
    onSelect: (Priority) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
    ) {
        for (priority in Priority.entries) {
            val isSelected = priority == selected
            val label = priority.visuals()?.label
                ?: priority.name.lowercase().replaceFirstChar { it.uppercase() }
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(priority) },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    }
                } else null,
                modifier = Modifier.semantics { contentDescription = "Priority $label" },
            )
        }
    }
}

@Composable
private fun ScopePicker(
    scope: CorrectionScopeChoice,
    senderEmail: String,
    senderDomain: String,
    onSelect: (CorrectionScopeChoice) -> Unit,
) {
    val options = listOf(
        CorrectionScopeChoice.MESSAGE to "This message",
        CorrectionScopeChoice.SENDER to "All from $senderEmail",
        CorrectionScopeChoice.DOMAIN to "All from $senderDomain",
    )
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xs)) {
        for ((choice, label) in options) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(choice) }
                    .padding(vertical = MoSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoSpacing.sm),
            ) {
                androidx.compose.material3.RadioButton(
                    selected = scope == choice,
                    onClick = { onSelect(choice) },
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { contentDescription = "Apply to $label" },
                )
            }
        }
    }
}
