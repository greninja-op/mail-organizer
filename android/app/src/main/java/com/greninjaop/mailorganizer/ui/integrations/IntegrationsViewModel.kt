package com.greninjaop.mailorganizer.ui.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Integrations destination (Phase 17).
 *
 * Snapshots come from the [IntegrationManager] — the single source of
 * truth for integration status. The UI never computes status itself and
 * never touches credentials or API clients.
 */
sealed interface IntegrationsUiState {
    data object Loading : IntegrationsUiState
    data class Loaded(
        val accountEmail: String?,
        val snapshots: List<IntegrationSnapshot>,
        val isOffline: Boolean,
    ) : IntegrationsUiState
    data class Error(val message: String) : IntegrationsUiState
}

/** One-shot UI events (dialogs, snackbars). */
sealed interface IntegrationsEvent {
    data class Message(val text: String) : IntegrationsEvent
    data class ConfirmDisconnect(val integrationId: IntegrationId) : IntegrationsEvent
}

class IntegrationsViewModel(
    private val manager: IntegrationManager,
    private val accounts: AccountRepository,
    private val dispatchers: AppDispatchers,
    private val activeAccountPreferences: ActiveAccountPreferences? = null,
) : ViewModel() {

    private val _events = MutableSharedFlow<IntegrationsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<IntegrationsEvent> = _events.asSharedFlow()

    private val activeAccount: StateFlow<AccountRecord?> =
        if (activeAccountPreferences != null) {
            combine(accounts.observeAll(), activeAccountPreferences.activeAccountSelection) { list, selection ->
                val enabled = list.filter { it.isEnabled }
                when (selection) {
                    is AccountSelection.Single -> {
                        enabled.firstOrNull { it.accountId == selection.accountId } ?: enabled.firstOrNull()
                    }
                    is AccountSelection.Unified -> {
                        enabled.minWithOrNull(
                            compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        } else {
            accounts.observeAll()
                .map { list ->
                    list.filter { it.isEnabled }.minWithOrNull(
                        compareBy<AccountRecord> { it.createdAtEpochMs }
                            .thenBy { it.accountId },
                    )
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        }

    private val refreshTick = MutableStateFlow(0L)

    val uiState: StateFlow<IntegrationsUiState> =
        combine(activeAccount, refreshTick) { account, _ -> account }
            .map { account ->
                try {
                    val snapshots = manager.snapshots(account?.accountId)
                    IntegrationsUiState.Loaded(
                        accountEmail = account?.emailAddress,
                        snapshots = snapshots,
                        isOffline = false,
                    )
                } catch (t: Throwable) {
                    IntegrationsUiState.Error(
                        "Couldn't load integrations: ${t.message ?: "unknown error"}",
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, IntegrationsUiState.Loading)

    /** Snapshot for the detail screen; null when the id is unknown. */
    suspend fun snapshotFor(id: IntegrationId): IntegrationSnapshot? {
        val accountId = activeAccount.value?.accountId
        return manager.snapshot(id, accountId)
    }

    /**
     * User tapped Connect. For deferred integrations (Calendar/Tasks) and
     * Gmail (OAuth deferred) this honestly reports why connection isn't
     * possible — never a fake flow (phase §16).
     */
    fun onConnect(id: IntegrationId) {
        viewModelScope.launch(dispatchers.io) {
            val accountId = activeAccount.value?.accountId
            when (val result = manager.connect(id, accountId)) {
                is MoResult.Success ->
                    _events.emit(IntegrationsEvent.Message("Connected."))
                is MoResult.Failure ->
                    _events.emit(
                        IntegrationsEvent.Message(
                            result.error.messageOf(),
                        ),
                    )
            }
            refreshTick.value = refreshTick.value + 1
        }
    }

    /** User tapped Disconnect: confirm first when the integration is connected. */
    fun onDisconnectTapped(id: IntegrationId) {
        viewModelScope.launch(dispatchers.io) {
            val accountId = activeAccount.value?.accountId
            val snapshot = manager.snapshot(id, accountId)
            val status = snapshot?.status
            if (status == com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.CONNECTED ||
                status == com.greninjaop.mailorganizer.core.integrations.IntegrationStatus.AUTH_REQUIRED
            ) {
                _events.emit(IntegrationsEvent.ConfirmDisconnect(id))
            } else {
                doDisconnect(id, accountId)
            }
        }
    }

    /** Confirmed disconnect (phase §18–§19): metadata only, never mail. */
    fun onDisconnectConfirmed(id: IntegrationId) {
        viewModelScope.launch(dispatchers.io) {
            doDisconnect(id, activeAccount.value?.accountId)
        }
    }

    fun onRefresh() {
        viewModelScope.launch(dispatchers.io) {
            val accountId = activeAccount.value?.accountId
            try {
                manager.refreshAll(accountId)
            } catch (t: Throwable) {
                _events.emit(
                    IntegrationsEvent.Message(
                        "Couldn't refresh: ${t.message ?: "unknown error"}",
                    ),
                )
            }
            refreshTick.value = refreshTick.value + 1
        }
    }

    private suspend fun doDisconnect(id: IntegrationId, accountId: String?) {
        when (val result = manager.disconnect(id, accountId)) {
            is MoResult.Success ->
                _events.emit(IntegrationsEvent.Message("Disconnected."))
            is MoResult.Failure ->
                _events.emit(IntegrationsEvent.Message(result.error.messageOf()))
        }
        refreshTick.value = refreshTick.value + 1
    }

    private fun com.greninjaop.mailorganizer.MoError.messageOf(): String = when (this) {
        is com.greninjaop.mailorganizer.MoError.Integration -> message
        is com.greninjaop.mailorganizer.MoError.InvalidConfiguration -> message
        is com.greninjaop.mailorganizer.MoError.Network -> message
        is com.greninjaop.mailorganizer.MoError.Authentication -> message
        else -> "Something went wrong: $this"
    }
}

/** Factory for [IntegrationsViewModel]. */
class IntegrationsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IntegrationsViewModel::class.java)) {
            return IntegrationsViewModel(
                manager = container.integrationManager,
                accounts = container.accountRepository,
                dispatchers = container.dispatchers,
                activeAccountPreferences = container.activeAccountPreferences,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
