package com.greninjaop.mailorganizer.ui.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.automation.AutomationAccountScope
import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationActionType
import com.greninjaop.mailorganizer.core.automation.AutomationCondition
import com.greninjaop.mailorganizer.core.automation.AutomationConditionField
import com.greninjaop.mailorganizer.core.automation.AutomationConditionGroup
import com.greninjaop.mailorganizer.core.automation.AutomationConfirmationPolicy
import com.greninjaop.mailorganizer.core.automation.AutomationOperator
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.core.automation.AutomationScopeType
import com.greninjaop.mailorganizer.core.automation.AutomationTrigger
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Automation Rule Editor & Preview Screen (Phase 27 §54, §55, §57, §170, §171).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationEditorScreen(
    ruleId: String?,
    viewModel: AutomationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val existingRule = state.rules.firstOrNull { it.id == ruleId }

    var name by remember(existingRule) { mutableStateOf(existingRule?.name ?: "") }
    var description by remember(existingRule) { mutableStateOf(existingRule?.description ?: "") }
    var triggerType by remember(existingRule) {
        mutableStateOf(existingRule?.trigger?.type ?: AutomationTriggerType.NEW_EMAIL_SYNCED)
    }
    var triggerParam by remember(existingRule) { mutableStateOf(existingRule?.trigger?.parameter ?: "") }

    var conditionField by remember(existingRule) {
        mutableStateOf(
            existingRule?.conditionGroup?.conditions?.firstOrNull()?.field ?: AutomationConditionField.SENDER_DOMAIN
        )
    }
    var conditionValue by remember(existingRule) {
        mutableStateOf(existingRule?.conditionGroup?.conditions?.firstOrNull()?.value ?: "")
    }

    var actionType by remember(existingRule) {
        mutableStateOf(existingRule?.actions?.firstOrNull()?.type ?: AutomationActionType.SET_CATEGORY)
    }
    var actionParam by remember(existingRule) {
        mutableStateOf(existingRule?.actions?.firstOrNull()?.parameter ?: "NEWSLETTERS")
    }

    var confirmationPolicy by remember(existingRule) {
        mutableStateOf(existingRule?.confirmationPolicy ?: AutomationConfirmationPolicy.ALWAYS_CONFIRM)
    }

    val previewResult = state.previewResult

    LaunchedEffect(Unit) {
        viewModel.clearPreview()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == null) "New Automation" else "Edit Automation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(MoSpacing.md)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.md),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Automation Name") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Trigger Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.md)) {
                    Text("When (Trigger)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(MoSpacing.xs))
                    DropdownSelector(
                        label = "Trigger Type",
                        items = AutomationTriggerType.values().toList(),
                        selectedItem = triggerType,
                        onSelect = { triggerType = it },
                    )
                    if (triggerType == AutomationTriggerType.CATEGORY_ASSIGNED || triggerType == AutomationTriggerType.PRIORITY_ASSIGNED) {
                        OutlinedTextField(
                            value = triggerParam,
                            onValueChange = { triggerParam = it },
                            label = { Text("Trigger Parameter (e.g. PROMOTIONAL, HIGH)") },
                            modifier = Modifier.fillMaxWidth().padding(top = MoSpacing.xs),
                        )
                    }
                }
            }

            // Condition Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.md)) {
                    Text("Only If (Condition)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(MoSpacing.xs))
                    DropdownSelector(
                        label = "Field",
                        items = AutomationConditionField.values().toList(),
                        selectedItem = conditionField,
                        onSelect = { conditionField = it },
                    )
                    OutlinedTextField(
                        value = conditionValue,
                        onValueChange = { conditionValue = it },
                        label = { Text("Match Value (e.g. github.com, true)") },
                        modifier = Modifier.fillMaxWidth().padding(top = MoSpacing.xs),
                    )
                }
            }

            // Action Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.md)) {
                    Text("Then (Action)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(MoSpacing.xs))
                    DropdownSelector(
                        label = "Action Type",
                        items = AutomationActionType.values().toList(),
                        selectedItem = actionType,
                        onSelect = { actionType = it },
                    )
                    OutlinedTextField(
                        value = actionParam,
                        onValueChange = { actionParam = it },
                        label = { Text("Action Parameter (Category/Priority/Label)") },
                        modifier = Modifier.fillMaxWidth().padding(top = MoSpacing.xs),
                    )
                }
            }

            // Confirmation Policy
            DropdownSelector(
                label = "Confirmation Policy",
                items = AutomationConfirmationPolicy.values().toList(),
                selectedItem = confirmationPolicy,
                onSelect = { confirmationPolicy = it },
            )

            // Preview Section
            OutlinedButton(
                onClick = {
                    val candidateRule = buildRule(
                        id = ruleId ?: "preview",
                        name = name,
                        desc = description,
                        triggerType = triggerType,
                        triggerParam = triggerParam,
                        field = conditionField,
                        value = conditionValue,
                        actionType = actionType,
                        actionParam = actionParam,
                        policy = confirmationPolicy,
                        accountId = state.activeAccountId,
                    )
                    viewModel.previewRule(candidateRule, state.activeAccountId ?: "")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Preview Matches on Current Mailbox")
            }

            if (previewResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                ) {
                    Column(modifier = Modifier.padding(MoSpacing.md)) {
                        Text(
                            text = "Matches ${previewResult.matchedMessagesCount} messages across ${previewResult.matchedThreadsCount} threads.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (previewResult.sampleSubjects.isNotEmpty()) {
                            Text(
                                text = "Sample matches: " + previewResult.sampleSubjects.joinToString("; "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val condGroup = if (conditionValue.isNotBlank()) {
                        AutomationConditionGroup(
                            conditions = listOf(
                                AutomationCondition(
                                    field = conditionField,
                                    operator = AutomationOperator.EQUALS,
                                    value = conditionValue,
                                )
                            )
                        )
                    } else AutomationConditionGroup()

                    viewModel.saveRule(
                        ruleId = ruleId,
                        name = if (name.isBlank()) "Untitled Automation" else name,
                        description = description,
                        scopeType = if (state.activeAccountId != null) AutomationScopeType.SPECIFIC_ACCOUNT else AutomationScopeType.ALL_ACCOUNTS,
                        targetAccountId = state.activeAccountId,
                        triggerType = triggerType,
                        triggerParam = triggerParam.ifBlank { null },
                        conditionGroup = condGroup,
                        actions = listOf(AutomationAction(actionType, actionParam.ifBlank { null })),
                        confirmationPolicy = confirmationPolicy,
                        onSuccess = onBack,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.height(MoSpacing.xs))
                Text("Save Automation")
            }
        }
    }
}

private fun buildRule(
    id: String,
    name: String,
    desc: String,
    triggerType: AutomationTriggerType,
    triggerParam: String,
    field: AutomationConditionField,
    value: String,
    actionType: AutomationActionType,
    actionParam: String,
    policy: AutomationConfirmationPolicy,
    accountId: String?,
): AutomationRule {
    val condGroup = if (value.isNotBlank()) {
        AutomationConditionGroup(
            conditions = listOf(
                AutomationCondition(
                    field = field,
                    operator = AutomationOperator.EQUALS,
                    value = value,
                )
            )
        )
    } else AutomationConditionGroup()

    return AutomationRule(
        id = id,
        name = name,
        description = desc,
        scope = AutomationAccountScope(
            type = if (accountId != null) AutomationScopeType.SPECIFIC_ACCOUNT else AutomationScopeType.ALL_ACCOUNTS,
            specificAccountId = accountId,
        ),
        trigger = AutomationTrigger(triggerType, triggerParam.ifBlank { null }),
        conditionGroup = condGroup,
        actions = listOf(AutomationAction(actionType, actionParam.ifBlank { null })),
        confirmationPolicy = policy,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> DropdownSelector(
    label: String,
    items: List<T>,
    selectedItem: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedItem.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
