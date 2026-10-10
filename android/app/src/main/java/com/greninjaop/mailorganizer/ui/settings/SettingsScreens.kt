package com.greninjaop.mailorganizer.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import com.greninjaop.mailorganizer.data.prefs.ThemePreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings destination — Phase 11 (phase §39).
 *
 * Only sections backed by implemented functionality: Accounts, Appearance
 * (theme), Privacy, Integrations, About. Nothing speculative, nothing
 * that pretends to configure unbuilt features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearanceViewModel: AppearanceViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsRow(
                icon = Icons.Filled.AccountCircle,
                title = "Accounts",
                subtitle = "Connected Gmail accounts",
                onClick = { onNavigate(AppDestinations.ACCOUNTS) },
            )
            SettingsRow(
                icon = Icons.Filled.Lock,
                title = "Privacy Center",
                subtitle = "Data audit, Google scopes & security hardening",
                onClick = { onNavigate(AppDestinations.PRIVACY) },
            )
            // Phase 12: rules & corrections management (§9).
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Rules",
                subtitle = "Automatic sorting and your corrections",
                onClick = { onNavigate(AppDestinations.RULES) },
            )
            SettingsRow(
                icon = Icons.Filled.Build,
                title = "Integrations",
                subtitle = "Calendar, Tasks and more",
                onClick = { onNavigate(AppDestinations.INTEGRATIONS) },
            )
            // Phase 20: noise, newsletter & cleanup system (§34–§37).
            SettingsRow(
                icon = Icons.Filled.MailOutline,
                title = "Cleanup & Newsletters",
                subtitle = "Review newsletters, notifications and low-value mail",
                onClick = { onNavigate(AppDestinations.CLEANUP) },
            )
            // Phase 25: Analytics & Insights
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Analytics & Insights",
                subtitle = "Local workload, categories, sources & attention patterns",
                onClick = { onNavigate(AppDestinations.ANALYTICS) },
            )
            // Phase 26: Optional AI Fallback Architecture
            SettingsRow(
                icon = Icons.Filled.Build,
                title = "AI Assistance (Optional)",
                subtitle = "Optional secondary fallback for ambiguous emails",
                onClick = { onNavigate(AppDestinations.AI_SETTINGS) },
            )
            AppearanceSection(viewModel = appearanceViewModel)
            AboutSection()
        }
    }
}

@Composable
private fun AboutSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
    ) {
        Text(text = "About", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(MoSpacing.xs))
        Text(
            text = "Mail Organizer — a personal email organization and action " +
                "system. Your Gmail, organized on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
            .semantics { contentDescription = "$title. $subtitle." },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(MoSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** ViewModel for the Accounts screen. */
class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val activeAccountPreferences: com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences,
    private val integrationManager: com.greninjaop.mailorganizer.domain.integrations.IntegrationManager?,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
    private val syncStateRepository: com.greninjaop.mailorganizer.data.repository.SyncStateRepository? = null,
) : ViewModel() {
    val accounts: StateFlow<List<AccountRecord>> =
        accountRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncStatesByAccountId: StateFlow<Map<String, String>> =
        if (syncStateRepository != null) {
            accounts.flatMapLatest { list ->
                if (list.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val flows = list.map { acc ->
                        syncStateRepository.observe(acc.accountId).map { record ->
                            acc.accountId to com.greninjaop.mailorganizer.data.sync.SyncTimeFormatter.formatLastSynced(
                                record?.lastSuccessfulSyncEpochMs,
                                clock(),
                            )
                        }
                    }
                    combine(flows) { pairs -> pairs.toMap() }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        } else {
            MutableStateFlow(emptyMap())
        }

    val activeSelection: StateFlow<com.greninjaop.mailorganizer.data.prefs.AccountSelection> =
        activeAccountPreferences.activeSelection
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified,
            )

    /** The active account id when a single account is active (null if Unified). */
    val activeAccountId: StateFlow<String?> =
        combine(accounts, activeSelection) { list, selection ->
            when (selection) {
                is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single -> {
                    list.firstOrNull { it.accountId == selection.accountId && it.isEnabled }?.accountId
                }
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified -> null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectAccount(accountId: String?) {
        viewModelScope.launch(dispatchers.io) {
            val target = if (accountId != null) {
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single(accountId)
            } else {
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
            }
            activeAccountPreferences.setActiveSelection(target)
        }
    }

    fun toggleEnabled(account: AccountRecord) {
        viewModelScope.launch(dispatchers.io) {
            val newEnabled = !account.isEnabled
            accountRepository.setEnabled(account.accountId, newEnabled)
            // If the currently active single account was disabled, fallback to next or unified
            val currentSel = activeAccountPreferences.activeSelection.first()
            if (!newEnabled && currentSel is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single && currentSel.accountId == account.accountId) {
                activeAccountPreferences.setActiveSelection(com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified)
            }
        }
    }

    fun disconnectAccount(accountId: String) {
        viewModelScope.launch(dispatchers.io) {
            // Integration cleanup
            integrationManager?.handleAccountRemoved(accountId)
            // Delete account cascades to all account-owned rows in Room and cleans FTS
            accountRepository.deleteById(accountId)
            // If deleted was active, fallback to unified
            val currentSel = activeAccountPreferences.activeSelection.first()
            if (currentSel is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single && currentSel.accountId == accountId) {
                activeAccountPreferences.setActiveSelection(com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified)
            }
        }
    }

    fun addLocalAccount(email: String, displayName: String?) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            val now = clock()
            val id = "local-" + abs(trimmed.lowercase().hashCode())
            val record = AccountRecord(
                accountId = id,
                emailAddress = trimmed,
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                connectionState = ConnectionState.DISCONNECTED, // Honest local state until Phase 3
                isEnabled = true,
            )
            accountRepository.upsert(record)
        }
    }
}

/** Factory for the Accounts screen. */
class AccountsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountsViewModel(
            accountRepository = container.accountRepository,
            activeAccountPreferences = container.activeAccountPreferences,
            integrationManager = container.integrationManager,
            dispatchers = container.dispatchers,
            syncStateRepository = container.syncStateRepository,
        ) as T
    }
}

/**
 * Accounts destination — Phase 11 (phase §35–§37).
 *
 * Lists connected accounts with identity and connection state. Adding an
 * account honestly states that OAuth connection arrives with Phase 3 —
 * no fake login form (§35, editor-rules). Real account switching is
 * Phase 18's scope; until then the active account is shown, never hidden
 * (§36, §37).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeSelection by viewModel.activeSelection.collectAsState()
    val syncStates by viewModel.syncStatesByAccountId.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var disconnectTarget by remember { mutableStateOf<AccountRecord?>(null) }

    if (showAddDialog) {
        AddLocalAccountDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { email, name ->
                showAddDialog = false
                viewModel.addLocalAccount(email, name)
            },
        )
    }

    disconnectTarget?.let { account ->
        AlertDialog(
            onDismissRequest = { disconnectTarget = null },
            title = { Text("Disconnect account?") },
            text = {
                Text(
                    "Disconnecting ${account.displayName ?: account.emailAddress} removes its synced data from this device. Other accounts will remain completely unaffected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = account.accountId
                        disconnectTarget = null
                        viewModel.disconnectAccount(id)
                    },
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { disconnectTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("Add Local")
                    }
                },
            )
        },
    ) { padding ->
        if (accounts.isEmpty()) {
            MoEmptyState(
                title = "No accounts connected",
                message = "Gmail account connection uses the official Google " +
                    "sign-in flow and arrives with Phase 3. You can add a local test " +
                    "account using the 'Add Local' action above.",
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Unified Inbox option
                val isUnified = activeSelection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAccount(null) }
                        .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(MoSpacing.xxl),
                        tint = if (isUnified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(MoSpacing.md))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "All Accounts (Unified Inbox)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isUnified) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (isUnified) {
                                Spacer(Modifier.width(MoSpacing.xs))
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            text = "View combined threads across all enabled accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = MoSpacing.xs))

                accounts.forEach { account ->
                    val isAccountActive = when (val sel = activeSelection) {
                        is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single -> sel.accountId == account.accountId
                        com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified -> false
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (account.isEnabled) {
                                    viewModel.selectAccount(account.accountId)
                                }
                            }
                            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
                            .semantics {
                                contentDescription = buildString {
                                    append(account.displayName ?: account.emailAddress)
                                    if (isAccountActive) append(". Active account.")
                                    append(" Connection: ${account.connectionState.name.lowercase()}.")
                                    if (!account.isEnabled) append(" Account is disabled.")
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccountAvatar(account = account, size = MoSpacing.xxl)
                        Spacer(Modifier.width(MoSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.displayName ?: account.emailAddress,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isAccountActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isAccountActive) {
                                    Spacer(Modifier.width(MoSpacing.xs))
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Text(
                                text = account.emailAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = connectionText(account),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val lastSync = syncStates[account.accountId]
                            if (!lastSync.isNullOrEmpty()) {
                                Text(
                                    text = "Last synced: $lastSync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { disconnectTarget = account }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Disconnect account ${account.emailAddress}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(MoSpacing.md))
                Text(
                    text = "Google OAuth sign-in arrives with Phase 3. Local accounts can be added for multi-account testing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MoSpacing.md),
                )
                Spacer(Modifier.height(MoSpacing.xl))
            }
        }
    }
}

@Composable
private fun AddLocalAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit,
) {
    var accountEmail by remember { mutableStateOf("") }
    var accountDisplayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Local Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.sm)) {
                TextField(
                    value = accountEmail,
                    onValueChange = { accountEmail = it },
                    label = { Text("Email address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = accountDisplayName,
                    onValueChange = { accountDisplayName = it },
                    label = { Text("Display name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(accountEmail, accountDisplayName.takeIf { it.isNotBlank() }) },
                enabled = accountEmail.isNotBlank() && accountEmail.contains("@"),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun connectionText(account: AccountRecord): String = when (account.connectionState) {
    ConnectionState.CONNECTED -> "Connected" + (
        account.lastSyncEpochMs?.let {
            " · synced ${MailFormatting.relativeTime(it, System.currentTimeMillis())}"
        } ?: ""
        )
    ConnectionState.CONNECTING -> "Connecting…"
    ConnectionState.TOKEN_EXPIRED -> "Sign-in expired — reconnect in Phase 3"
    ConnectionState.DISABLED -> "Disabled"
    ConnectionState.DISCONNECTED -> "Not connected"
}

/**
 * Privacy destination — Phase 11 (phase §38).
 *
 * Only claims what is actually implemented. No privacy theater.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Privacy") },
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
                .padding(horizontal = MoSpacing.md),
        ) {
            PrivacyPoint(
                title = "Local-first",
                body = "Classification, company detection, priority, and search all " +
                    "run on this device. Your email content is never sent to " +
                    "Mail Organizer servers — there are none.",
            )
            PrivacyPoint(
                title = "Gmail is the source of truth",
                body = "Mail Organizer keeps a local copy for offline use, search, " +
                    "and organization. The authoritative copy of your mail stays " +
                    "in Gmail.",
            )
            PrivacyPoint(
                title = "Account isolation",
                body = "Every stored item is scoped to the Gmail account it came " +
                    "from. Accounts never mix.",
            )
            PrivacyPoint(
                title = "No credential storage by Mail Organizer",
                body = "Google sign-in (Phase 3) will use the official Google " +
                    "authentication flow. Mail Organizer never asks for your " +
                    "Google password.",
            )
            PrivacyPoint(
                title = "No remote AI",
                body = "There is no AI fallback sending your mail anywhere. " +
                    "Intelligence is deterministic and on-device.",
            )
            Spacer(Modifier.height(MoSpacing.xl))
        }
    }
}

@Composable
private fun PrivacyPoint(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = MoSpacing.sm)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(MoSpacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Integrations destination — Phase 17 (phase §20).
 *
 * The Phase 11 placeholder lived here; the real Integration Manager UI
 * now lives in `ui.integrations` (IntegrationsScreen + detail). This file
 * keeps no integration UI anymore.
 */

/** ViewModel for the Appearance section (theme). */
class AppearanceViewModel(
    private val themePreferences: ThemePreferences,
    private val dispatchers: AppDispatchers,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(dispatchers.io) {
            themePreferences.setThemeMode(mode)
        }
    }
}

/** Factory for the Appearance section. */
class AppearanceViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppearanceViewModel(
            themePreferences = container.themePreferences,
            dispatchers = container.dispatchers,
        ) as T
    }
}

/**
 * Appearance settings — the one real setting implemented (theme).
 */
@Composable
fun AppearanceSection(
    viewModel: AppearanceViewModel,
    modifier: Modifier = Modifier,
) {
    val themeMode by viewModel.themeMode.collectAsState()

    Column(modifier = modifier.padding(horizontal = MoSpacing.md)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = MoSpacing.sm),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                    label = {
                        Text(mode.name.lowercase().replaceFirstChar { it.titlecase() })
                    },
                )
            }
        }
        Spacer(Modifier.height(MoSpacing.md))
    }
}
