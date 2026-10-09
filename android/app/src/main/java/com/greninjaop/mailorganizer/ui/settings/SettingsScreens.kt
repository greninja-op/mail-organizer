package com.greninjaop.mailorganizer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
                title = "Privacy",
                subtitle = "How your data is handled",
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
    accountRepository: AccountRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {
    val accounts: StateFlow<List<AccountRecord>> =
        accountRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The active account (first enabled, oldest) — same rule as the mailbox. */
    val activeAccountId: StateFlow<String?> =
        accountRepository.observeAll().map { list ->
            list.filter { it.isEnabled }.minWithOrNull(
                compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
            )?.accountId
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Factory for the Accounts screen. */
class AccountsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountsViewModel(
            accountRepository = container.accountRepository,
            dispatchers = container.dispatchers,
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
    val activeId by viewModel.activeAccountId.collectAsState()

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
            )
        },
    ) { padding ->
        if (accounts.isEmpty()) {
            MoEmptyState(
                title = "No accounts connected",
                message = "Gmail account connection uses the official Google " +
                    "sign-in flow and arrives with Phase 3. Until then, the app " +
                    "works with local sample data in debug builds.",
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                accounts.forEach { account ->
                    val isActive = account.accountId == activeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
                            .semantics {
                                contentDescription = buildString {
                                    append(account.displayName ?: account.emailAddress)
                                    if (isActive) append(". Active account.")
                                    append(" Connection: ${account.connectionState.name.lowercase()}.")
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isActive) {
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
                        }
                    }
                }
                Spacer(Modifier.height(MoSpacing.md))
                Text(
                    text = "Adding another Gmail account uses the official Google " +
                        "sign-in flow and arrives with Phase 3.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MoSpacing.md),
                )
                Spacer(Modifier.height(MoSpacing.xl))
            }
        }
    }
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
 * Integrations destination — Phase 11 (phase §40).
 *
 * A basic entry point. Calendar and Tasks are NOT implemented — the
 * screen says so plainly instead of presenting them as active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            )
        },
    ) { padding ->
        MoEmptyState(
            title = "No integrations yet",
            message = "Calendar and Tasks integrations will appear here as they " +
                "are connected (Phases 15–16). Nothing is connected right now.",
            modifier = Modifier.padding(padding),
        )
    }
}

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
