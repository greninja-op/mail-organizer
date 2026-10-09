package com.greninjaop.mailorganizer.ui.integrations

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Integrations destination (Phase 17, phase §20).
 *
 * Replaces Phase 11's "No integrations yet" placeholder with the real
 * Integration Manager UI: every registered integration with its honest
 * status. Deferred integrations (Calendar → Phase 15, Tasks → Phase 16)
 * show UNAVAILABLE with an explicit reason — never a fake connection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    viewModel: IntegrationsViewModel,
    onBack: () -> Unit,
    onOpenDetail: (IntegrationId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDisconnect by remember { mutableStateOf<IntegrationId?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is IntegrationsEvent.Message -> snackbar.showSnackbar(event.text)
                is IntegrationsEvent.ConfirmDisconnect -> pendingDisconnect = event.integrationId
            }
        }
    }

    pendingDisconnect?.let { id ->
        DisconnectConfirmDialog(
            onConfirm = {
                pendingDisconnect = null
                viewModel.onDisconnectConfirmed(id)
            },
            onDismiss = { pendingDisconnect = null },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Integrations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onRefresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh integration status",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val state = uiState) {
            is IntegrationsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is IntegrationsUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load integrations", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(MoSpacing.sm))
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(MoSpacing.md))
                    Button(onClick = { viewModel.onRefresh() }) { Text("Retry") }
                }
            }

            is IntegrationsUiState.Loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (state.isOffline) {
                    item {
                        OfflineBanner()
                    }
                }
                if (state.accountEmail != null) {
                    item {
                        Text(
                            text = "Account: ${state.accountEmail}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            ),
                        )
                    }
                }
                item {
                    Text(
                        text = "Google",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.xs,
                        ),
                    )
                }
                items(state.snapshots, key = { it.id.value }) { snapshot ->
                    IntegrationRow(
                        snapshot = snapshot,
                        onClick = { onOpenDetail(snapshot.id) },
                    )
                    HorizontalDivider()
                }
                item {
                    Text(
                        text = "Mail Organizer only requests the permissions it needs. " +
                            "It never modifies or sends mail without your explicit action.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(MoSpacing.md),
                    )
                }
            }
        }
    }
}

/** Offline banner (phase §26): offline is not disconnected. */
@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(MoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(MoSpacing.sm))
        Text(
            text = "You're offline — statuses below are the last known ones.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IntegrationRow(
    snapshot: IntegrationSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(MoSpacing.md)
            .semantics {
                contentDescription = "${snapshot.displayName}, ${snapshot.statusLabel()}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntegrationBadge(id = snapshot.id)
        Spacer(Modifier.width(MoSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snapshot.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = snapshot.statusLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = snapshot.statusColor(),
            )
            if (snapshot.accountEmail != null) {
                Text(
                    text = snapshot.accountEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatusPill(snapshot.status)
    }
}

/** Circular badge with the integration's initial — no invented icons. */
@Composable
private fun IntegrationBadge(id: IntegrationId) {
    Box(
        modifier = Modifier
            .size(MoSpacing.huge)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = id.value.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun StatusPill(status: IntegrationStatus) {
    val (label, container, content) = when (status) {
        IntegrationStatus.CONNECTED ->
            Triple("Connected", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        IntegrationStatus.CONNECTING ->
            Triple("Connecting…", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        IntegrationStatus.AUTH_REQUIRED ->
            Triple("Needs sign-in", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        IntegrationStatus.PERMISSION_REQUIRED ->
            Triple("Needs permission", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        IntegrationStatus.OFFLINE ->
            Triple("Offline", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        IntegrationStatus.UNAVAILABLE ->
            Triple("Coming soon", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        IntegrationStatus.ERROR ->
            Triple("Error", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        IntegrationStatus.DISCONNECTED, IntegrationStatus.AVAILABLE ->
            Triple("Not connected", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(MoSpacing.sm))
            .background(container)
            .padding(horizontal = MoSpacing.sm, vertical = MoSpacing.xs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

private fun IntegrationSnapshot.statusLabel(): String = when (status) {
    IntegrationStatus.CONNECTED -> "Connected"
    IntegrationStatus.CONNECTING -> "Connecting…"
    IntegrationStatus.AUTH_REQUIRED -> "Sign-in required"
    IntegrationStatus.PERMISSION_REQUIRED -> "Permission required"
    IntegrationStatus.OFFLINE -> "Offline (last known)"
    IntegrationStatus.UNAVAILABLE -> "Not available yet"
    IntegrationStatus.ERROR -> "Error"
    IntegrationStatus.DISCONNECTED, IntegrationStatus.AVAILABLE -> "Not connected"
}

@Composable
private fun IntegrationSnapshot.statusColor() =
    when (status) {
        IntegrationStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        IntegrationStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/**
 * Disconnect confirmation (phase §19): precise language about what
 * disconnecting does and doesn't do.
 */
@Composable
private fun DisconnectConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
        title = { Text("Disconnect this integration?") },
        text = {
            Text(
                "Mail Organizer will stop using this integration. " +
                    "Your mail, classifications, rules, and action history " +
                    "are not affected.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Disconnect") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Integration detail screen (phase §21): provider, status + reason,
 * account, capabilities, permissions with purposes, connect/disconnect.
 * Raw API ids are never exposed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationDetailScreen(
    snapshot: IntegrationSnapshot,
    viewModel: IntegrationsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    var pendingDisconnect by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is IntegrationsEvent.Message -> snackbar.showSnackbar(event.text)
                is IntegrationsEvent.ConfirmDisconnect -> pendingDisconnect = true
            }
        }
    }

    if (pendingDisconnect) {
        DisconnectConfirmDialog(
            onConfirm = {
                pendingDisconnect = false
                viewModel.onDisconnectConfirmed(snapshot.id)
            },
            onDismiss = { pendingDisconnect = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(snapshot.displayName) },
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
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                DetailHeader(snapshot)
                HorizontalDivider()
            }
            item {
                DetailSection(title = "Status") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(snapshot.status)
                        Spacer(Modifier.width(MoSpacing.sm))
                        Text(
                            text = snapshot.statusReason,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider()
            }
            if (snapshot.accountEmail != null) {
                item {
                    DetailSection(title = "Account") {
                        Text(
                            text = snapshot.accountEmail,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    HorizontalDivider()
                }
            }
            item {
                DetailSection(title = "Capabilities") {
                    if (snapshot.declaredCapabilities.isEmpty()) {
                        Text(
                            "No capabilities declared.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        snapshot.declaredCapabilities.forEach { capability ->
                            CapabilityRow(
                                capability = capability,
                                usable = snapshot.isCapable(capability),
                            )
                        }
                    }
                    if (snapshot.status != IntegrationStatus.CONNECTED) {
                        Spacer(Modifier.height(MoSpacing.sm))
                        Text(
                            text = "Capabilities become available when the integration is connected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
            item {
                DetailSection(title = "Permissions") {
                    if (snapshot.requiredPermissions.isEmpty()) {
                        Text(
                            "No permissions required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        snapshot.requiredPermissions.forEach { permission ->
                            Row(
                                modifier = Modifier.padding(vertical = MoSpacing.xs),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(MoSpacing.lg),
                                )
                                Spacer(Modifier.width(MoSpacing.sm))
                                Column {
                                    Text(
                                        text = permission.scope,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = permission.purpose,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
            item {
                DetailActions(
                    snapshot = snapshot,
                    onConnect = { viewModel.onConnect(snapshot.id) },
                    onDisconnect = { viewModel.onDisconnectTapped(snapshot.id) },
                )
                Spacer(Modifier.height(MoSpacing.lg))
            }
        }
    }
}

/** Honest not-found state for unknown integration ids — never a crash. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnknownIntegrationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Integration") },
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
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Integration not found",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(MoSpacing.sm))
                Text(
                    "This integration doesn't exist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(snapshot: IntegrationSnapshot) {
    Row(
        modifier = Modifier.padding(MoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntegrationBadge(id = snapshot.id)
        Spacer(Modifier.width(MoSpacing.md))
        Column {
            Text(
                text = snapshot.displayName,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "by ${snapshot.provider}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(MoSpacing.md)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MoSpacing.sm))
        content()
    }
}

@Composable
private fun CapabilityRow(capability: IntegrationCapability, usable: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = MoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (usable) Icons.Filled.CheckCircle else Icons.Filled.Info,
            contentDescription = null,
            tint = if (usable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MoSpacing.lg),
        )
        Spacer(Modifier.width(MoSpacing.sm))
        Text(
            text = capability.displayName(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun IntegrationCapability.displayName(): String = when (this) {
    IntegrationCapability.READ_EMAIL -> "Read email"
    IntegrationCapability.SYNC_EMAIL -> "Sync email"
    IntegrationCapability.READ_CALENDAR_METADATA -> "Read calendar metadata"
    IntegrationCapability.CREATE_EVENT -> "Create calendar events"
    IntegrationCapability.READ_TASK_LISTS -> "Read task lists"
    IntegrationCapability.CREATE_TASK -> "Create tasks"
}

@Composable
private fun DetailActions(
    snapshot: IntegrationSnapshot,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(MoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        when (snapshot.status) {
            // UNAVAILABLE: no Connect button — the integration isn't built.
            // The status reason already explains when it arrives (phase §27).
            IntegrationStatus.UNAVAILABLE -> {
                Card {
                    Text(
                        text = snapshot.statusReason,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(MoSpacing.md),
                    )
                }
            }
            IntegrationStatus.CONNECTED -> {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Disconnect") }
            }
            else -> {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Connect") }
                if (snapshot.status == IntegrationStatus.AUTH_REQUIRED ||
                    snapshot.status == IntegrationStatus.DISCONNECTED
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Disconnect") }
                }
            }
        }
    }
}
