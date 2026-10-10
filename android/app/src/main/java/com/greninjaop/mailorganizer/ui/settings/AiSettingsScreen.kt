package com.greninjaop.mailorganizer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * AI Assistance Settings Screen (Phase 26 §30, §70, §71, §123, §158).
 *
 * Clearly communicates:
 * - AI is strictly optional and off by default.
 * - Deterministic, explainable intelligence remains the authoritative core.
 * - Remote processing requires explicit opt-in and consent.
 * - OAuth tokens and credentials are never transmitted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: AiSettingsViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("AI Assistance (Optional)") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.lg),
        ) {
            // Status and Policy Notice Card
            StatusCard(state = state)

            // Master Enable Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MoSpacing.md))
                    .clickable { viewModel.toggleAiEnabled(!state.isAiEnabled) }
                    .padding(vertical = MoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable AI Assistance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(MoSpacing.xs))
                    Text(
                        text = "Allows optional secondary fallback for ambiguous emails. Disabled by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isAiEnabled,
                    onCheckedChange = { viewModel.toggleAiEnabled(it) },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle AI Assistance"
                    },
                )
            }

            HorizontalDivider()

            if (state.isAiEnabled) {
                // Provider Selection Section
                Text(
                    text = "AI Provider",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.sm)) {
                    for (provider in state.availableProviders) {
                        val isSelected = provider.id.value == state.selectedProviderId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectProvider(provider.id.value) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(MoSpacing.md)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = provider.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        if (provider.isRemote) {
                                            Spacer(Modifier.width(MoSpacing.xs))
                                            Text(
                                                text = "Remote",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                            )
                                        } else {
                                            Spacer(Modifier.width(MoSpacing.xs))
                                            Text(
                                                text = "On-Device",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(MoSpacing.xs))
                                    Text(
                                        text = provider.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                val currentSelected = state.availableProviders.firstOrNull { it.id.value == state.selectedProviderId }

                // Remote-specific configuration (BYOK & Consent)
                if (currentSelected?.isRemote == true) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(MoSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
                        ) {
                            Text(
                                text = "Remote Provider Consent & Key",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Checkbox(
                                    checked = state.hasUserConsented,
                                    onCheckedChange = { viewModel.setUserConsent(it) },
                                )
                                Spacer(Modifier.width(MoSpacing.xs))
                                Text(
                                    text = "I consent to sending minimized email snippets to the configured remote provider.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (state.hasApiKey) "API Key: Configured" else "API Key: Not Set",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                                TextButton(onClick = { showApiKeyDialog = true }) {
                                    Text(if (state.hasApiKey) "Change Key" else "Set Key")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Allowed Capabilities Section
                Text(
                    text = "Allowed Capabilities",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                CapabilityRow(
                    title = "Email Classification Fallback",
                    subtitle = "Classifies ambiguous emails when deterministic rules are unconfident",
                    enabled = state.allowedCapabilities.contains(AiCapability.CLASSIFY_EMAIL),
                    onToggle = { viewModel.toggleCapability(AiCapability.CLASSIFY_EMAIL, it) },
                )

                CapabilityRow(
                    title = "Deadline Extraction Fallback",
                    subtitle = "Identifies complex deadlines when regex parsing finds ambiguity",
                    enabled = state.allowedCapabilities.contains(AiCapability.EXTRACT_DEADLINE),
                    onToggle = { viewModel.toggleCapability(AiCapability.EXTRACT_DEADLINE, it) },
                )

                CapabilityRow(
                    title = "On-Demand Thread Summarization",
                    subtitle = "Generates summaries when explicitly requested on thread screen",
                    enabled = state.allowedCapabilities.contains(AiCapability.SUMMARIZE_THREAD),
                    onToggle = { viewModel.toggleCapability(AiCapability.SUMMARIZE_THREAD, it) },
                )

                HorizontalDivider()

                // Reset / Disconnect Action
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Disconnect & Reset AI Configuration",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Privacy Center disclosure shortcut
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppDestinations.PRIVACY) },
            ) {
                Row(
                    modifier = Modifier.padding(MoSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MoSpacing.lg),
                    )
                    Spacer(Modifier.width(MoSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Center Audit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Inspect technical data inventory, token boundaries, and cascading deletion policies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onSave = { key ->
                viewModel.setApiKey(key)
                showApiKeyDialog = false
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset AI Configuration") },
            text = {
                Text("This will disable AI assistance, remove any stored provider credentials, and clear cached AI suggestions. Mail Organizer's deterministic intelligence will continue working normally.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetConfiguration()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun StatusCard(state: AiSettingsUiState) {
    val (bgColor, textColor, message) = when {
        !state.isAiEnabled -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "AI assistance is off. Mail Organizer's deterministic organization continues to work normally.",
        )
        state.providerAvailability == AiAvailabilityState.AVAILABLE -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "AI fallback is active and ready. Deterministic rules always evaluate first.",
        )
        state.providerAvailability == AiAvailabilityState.OFFLINE -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Device is offline. Deterministic organization remains fully functional.",
        )
        state.providerAvailability == AiAvailabilityState.NOT_CONFIGURED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Provider is not configured. Add credentials or select the on-device assistant.",
        )
        else -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "AI assistance status: ${state.providerAvailability.name}. Deterministic organization is active.",
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(MoSpacing.lg),
            )
            Spacer(Modifier.width(MoSpacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

@Composable
private fun CapabilityRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(vertical = MoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun ApiKeyDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Provider API Key") },
        text = {
            Column {
                Text(
                    text = "Enter your personal API key for the remote AI provider. The key is stored securely on your device and is never logged or synced.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(MoSpacing.md))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
