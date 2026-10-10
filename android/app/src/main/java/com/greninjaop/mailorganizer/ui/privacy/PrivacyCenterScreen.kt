package com.greninjaop.mailorganizer.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.privacy.DataInventoryItem
import com.greninjaop.mailorganizer.core.privacy.DataSensitivity
import com.greninjaop.mailorganizer.core.privacy.PermissionScopeDisclosure
import com.greninjaop.mailorganizer.core.privacy.SecurityAuditSnapshot
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.components.MoLoadingState
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Privacy Center screen (Phase 23 §40–§50).
 *
 * Transparent, verifiable disclosures about:
 * - Local-first data architecture & minimal collection
 * - Complete data inventory & storage locations
 * - Google OAuth scope disclosures with plain English explanations
 * - Security hardening status (logging, token isolation, backup boundaries)
 * - Safe account removal and local data wiping
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Privacy Center") },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            MoLoadingState(
                message = "Loading privacy audit…",
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                // Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = MoSpacing.md,
                ) {
                    Tab(
                        selected = state.selectedTab == PrivacyCenterTab.OVERVIEW,
                        onClick = { viewModel.selectTab(PrivacyCenterTab.OVERVIEW) },
                        text = { Text("Overview") },
                    )
                    Tab(
                        selected = state.selectedTab == PrivacyCenterTab.DATA_INVENTORY,
                        onClick = { viewModel.selectTab(PrivacyCenterTab.DATA_INVENTORY) },
                        text = { Text("Your Data") },
                    )
                    Tab(
                        selected = state.selectedTab == PrivacyCenterTab.PERMISSIONS,
                        onClick = { viewModel.selectTab(PrivacyCenterTab.PERMISSIONS) },
                        text = { Text("Google Access") },
                    )
                    Tab(
                        selected = state.selectedTab == PrivacyCenterTab.SECURITY_HARDENING,
                        onClick = { viewModel.selectTab(PrivacyCenterTab.SECURITY_HARDENING) },
                        text = { Text("Security") },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(MoSpacing.md),
                ) {
                    when (state.selectedTab) {
                        PrivacyCenterTab.OVERVIEW -> OverviewSection(
                            accounts = state.accounts,
                            auditSnapshot = state.auditSnapshot,
                            onRequestDeleteAccount = { viewModel.requestDeleteAccount(it) },
                            onRequestClearAll = { viewModel.requestClearAll() },
                        )
                        PrivacyCenterTab.DATA_INVENTORY -> DataInventorySection(
                            inventory = state.dataInventory,
                        )
                        PrivacyCenterTab.PERMISSIONS -> PermissionsSection(
                            scopes = state.scopes,
                        )
                        PrivacyCenterTab.SECURITY_HARDENING -> SecurityHardeningSection(
                            audit = state.auditSnapshot,
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog: Delete single account
    state.showDeleteAccountDialogFor?.let { account ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteAccount() },
            title = { Text("Disconnect Account") },
            text = {
                Text(
                    "Remove ${account.emailAddress} from Mailstack?\n\n" +
                        "• All locally stored emails, search index, and derived data for this account will be permanently deleted from this device.\n" +
                        "• Google OAuth access will be revoked.\n" +
                        "• Your Gmail messages are NOT deleted from Google servers.",
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteAccount(account.accountId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Disconnect & Delete Local Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteAccount() }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Confirmation dialog: Clear all data
    if (state.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAll() },
            title = { Text("Clear All Local Data") },
            text = {
                Text(
                    "Are you sure you want to clear all data?\n\n" +
                        "• All cached messages, classifications, rules, and search indexes across all accounts will be removed.\n" +
                        "• Remote email accounts and Gmail data remain safe on Google servers.",
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmClearAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Wipe All Local Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearAll() }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun OverviewSection(
    accounts: List<AccountRecord>,
    auditSnapshot: SecurityAuditSnapshot?,
    onRequestDeleteAccount: (AccountRecord) -> Unit,
    onRequestClearAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.md)) {
        // High level guarantee banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(MoSpacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(MoSpacing.sm))
                    Text(
                        text = "Local-First & Private by Design",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(MoSpacing.xs))
                Text(
                    text = "Mailstack organizes your email directly on your device. We do not operate cloud servers, track user behavior, or transmit your emails to third-party AI models.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Core Pillars
        PillarRow(
            icon = Icons.Default.Info,
            title = "Zero Mailstack Servers",
            description = "Your device connects directly to Google APIs. There is no intermediate server.",
        )
        PillarRow(
            icon = Icons.Default.Lock,
            title = "Strict Credential Separation",
            description = "OAuth secrets are never saved in the database or shared across accounts.",
        )
        PillarRow(
            icon = Icons.Default.CheckCircle,
            title = "Deterministic Local Intelligence",
            description = "Classification, priorities, and action detection run 100% on-device.",
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = MoSpacing.xs))

        // Connected Accounts Section
        Text(
            text = "Connected Accounts (${accounts.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        if (accounts.isEmpty()) {
            Text(
                text = "No accounts currently connected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            accounts.forEach { account ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(MoSpacing.md)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.emailAddress,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Local cache: Active · Account isolated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onRequestDeleteAccount(account) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Account Data",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(MoSpacing.md))

        // Data purge action
        OutlinedButton(
            onClick = onRequestClearAll,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(MoSpacing.xs))
            Text("Clear All Local Data & Reset")
        }
    }
}

@Composable
private fun PillarRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.width(MoSpacing.md))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DataInventorySection(inventory: List<DataInventoryItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.md)) {
        Text(
            text = "Data Inventory & Handling",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Here is an audit of every category of information stored by Mailstack, why it is needed, and how it is handled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        inventory.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        SensitivityBadge(item.sensitivity)
                    }

                    Spacer(Modifier.height(MoSpacing.xs))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(Modifier.height(MoSpacing.sm))
                    DetailRow(label = "Storage", value = item.storageLocation)
                    DetailRow(
                        label = "Transmission",
                        value = if (item.leavesDevice) "External (${item.destinationIfLeaves})" else "Never leaves device",
                    )
                    DetailRow(label = "Retention", value = item.retentionPolicy)
                    DetailRow(label = "Deletion", value = item.deletionBehavior)
                }
            }
        }
    }
}

@Composable
private fun SensitivityBadge(sensitivity: DataSensitivity) {
    val (color, text) = when (sensitivity) {
        DataSensitivity.HIGHLY_SENSITIVE -> MaterialTheme.colorScheme.error to "Highly Sensitive"
        DataSensitivity.SENSITIVE -> MaterialTheme.colorScheme.tertiary to "Sensitive"
        DataSensitivity.DERIVED_INTELLIGENCE -> MaterialTheme.colorScheme.primary to "Derived Local"
        DataSensitivity.NON_SENSITIVE -> MaterialTheme.colorScheme.secondary to "Non-Sensitive"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PermissionsSection(scopes: List<PermissionScopeDisclosure>) {
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.md)) {
        Text(
            text = "Google Permissions & Scopes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Mailstack requests only minimum necessary access to provide email organization features.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        scopes.forEach { scope ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(MoSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scope.userFriendlyDescription,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        val statusColor = if (scope.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        val statusText = if (scope.isGranted) "Granted" else "Deferred / Inactive"
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(MoSpacing.xs))
                    Text(
                        text = scope.whyNeeded,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(Modifier.height(MoSpacing.xs))
                    Text(
                        text = "Technical Scope: ${scope.technicalName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Level: ${scope.accessLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityHardeningSection(audit: SecurityAuditSnapshot?) {
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.md)) {
        Text(
            text = "Security Hardening Audit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Verified boundaries protecting your data from leakage and unauthorized access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (audit != null) {
            SecurityCheckItem(
                title = "Credential Isolation",
                detail = audit.credentialStorageMechanism,
                isSafe = !audit.plainSecretsInDatabase,
            )
            SecurityCheckItem(
                title = "No Secrets in Database",
                detail = "Local SQLite store contains zero OAuth tokens, refresh tokens, or passwords.",
                isSafe = !audit.plainSecretsInDatabase,
            )
            SecurityCheckItem(
                title = "Sanitized Logging Active",
                detail = "MoLogger strips email addresses, Bearer tokens, and auth headers from all log streams.",
                isSafe = audit.loggingSanitizationActive,
            )
            SecurityCheckItem(
                title = "Backup Leakage Shield",
                detail = "Android auto-backup rules explicitly exclude databases and internal cache files.",
                isSafe = audit.backupRulesActive,
            )
            SecurityCheckItem(
                title = "Network Security",
                detail = audit.networkSecurityStatus,
                isSafe = true,
            )
            SecurityCheckItem(
                title = "Third-Party Telemetry & AI",
                detail = "Zero telemetry SDKs and zero third-party AI inference servers.",
                isSafe = !audit.thirdPartyAiTelemetry,
            )
        }
    }
}

@Composable
private fun SecurityCheckItem(
    title: String,
    detail: String,
    isSafe: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .padding(MoSpacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.width(MoSpacing.md))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
