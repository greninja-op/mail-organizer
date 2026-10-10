package com.greninjaop.mailorganizer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.core.ai.AiProviderInfo
import com.greninjaop.mailorganizer.data.ai.AiProviderRegistry
import com.greninjaop.mailorganizer.data.prefs.AiPreferences
import com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for the AI Assistance settings screen (Phase 26 §30).
 */
data class AiSettingsUiState(
    val isAiEnabled: Boolean = false,
    val selectedProviderId: String = "local_heuristic",
    val availableProviders: List<AiProviderInfo> = emptyList(),
    val allowedCapabilities: Set<AiCapability> = emptySet(),
    val hasUserConsented: Boolean = false,
    val hasApiKey: Boolean = false,
    val providerAvailability: AiAvailabilityState = AiAvailabilityState.DISABLED,
)

/**
 * Backs the AI Settings screen (Phase 26 §30, §71, §123).
 */
class AiSettingsViewModel(
    private val preferences: AiPreferences,
    private val registry: AiProviderRegistry,
    private val aiFallbackUseCase: AiFallbackUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private data class PrefsTuple(
        val enabled: Boolean,
        val providerId: String,
        val capabilities: Set<AiCapability>,
        val consented: Boolean,
        val apiKey: String?,
    )

    private val providerAvailabilityFlow = MutableStateFlow(AiAvailabilityState.DISABLED)

    private val prefsFlow = combine(
        preferences.isAiEnabled,
        preferences.selectedProviderId,
        preferences.allowedCapabilities,
        preferences.hasUserConsented,
        preferences.apiKey,
    ) { enabled, providerId, capabilities, consented, apiKey ->
        PrefsTuple(enabled, providerId, capabilities, consented, apiKey)
    }

    val state: StateFlow<AiSettingsUiState> = combine(
        prefsFlow,
        providerAvailabilityFlow,
    ) { prefs, availability ->
        val currentAvailability = if (!prefs.enabled) {
            AiAvailabilityState.DISABLED
        } else {
            availability
        }

        AiSettingsUiState(
            isAiEnabled = prefs.enabled,
            selectedProviderId = prefs.providerId,
            availableProviders = registry.getAllProviderInfos(),
            allowedCapabilities = prefs.capabilities,
            hasUserConsented = prefs.consented,
            hasApiKey = !prefs.apiKey.isNullOrBlank(),
            providerAvailability = currentAvailability,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AiSettingsUiState(),
    )

    init {
        refreshProviderAvailability()
    }

    fun toggleAiEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            preferences.setAiEnabled(enabled)
            refreshProviderAvailability()
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch(dispatchers.main) {
            preferences.setSelectedProviderId(providerId)
            refreshProviderAvailability()
        }
    }

    fun toggleCapability(capability: AiCapability, enabled: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            val current = state.value.allowedCapabilities
            val updated = if (enabled) current + capability else current - capability
            preferences.setAllowedCapabilities(updated)
        }
    }

    fun setUserConsent(consented: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            preferences.setUserConsented(consented)
            refreshProviderAvailability()
        }
    }

    fun setApiKey(key: String?) {
        viewModelScope.launch(dispatchers.main) {
            preferences.setApiKey(key)
            refreshProviderAvailability()
        }
    }

    fun resetConfiguration() {
        viewModelScope.launch(dispatchers.main) {
            aiFallbackUseCase.disconnectProvider()
            refreshProviderAvailability()
        }
    }

    fun refreshProviderAvailability() {
        viewModelScope.launch(dispatchers.main) {
            val providerId = state.value.selectedProviderId
            val provider = registry.get(AiProviderId(providerId))
            if (provider == null) {
                providerAvailabilityFlow.value = AiAvailabilityState.NOT_CONFIGURED
            } else {
                providerAvailabilityFlow.value = provider.checkAvailability()
            }
        }
    }
}

class AiSettingsViewModelFactory(
    private val preferences: AiPreferences,
    private val registry: AiProviderRegistry,
    private val aiFallbackUseCase: AiFallbackUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AiSettingsViewModel(
            preferences = preferences,
            registry = registry,
            aiFallbackUseCase = aiFallbackUseCase,
            dispatchers = dispatchers,
        ) as T
    }
}
