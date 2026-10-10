package com.greninjaop.mailorganizer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.ai.AiCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * User preferences and configuration for the optional AI subsystem (Phase 26 §30, §31, §35).
 *
 * Defaults:
 * - AI assistance is OFF by default ([isAiEnabled] = false) per §31.
 * - Explicit user consent is required before remote data transmission ([hasUserConsented] = false).
 */
interface AiPreferences {
    val isAiEnabled: Flow<Boolean>
    val selectedProviderId: Flow<String>
    val allowedCapabilities: Flow<Set<AiCapability>>
    val hasUserConsented: Flow<Boolean>
    val apiKey: Flow<String?>

    suspend fun setAiEnabled(enabled: Boolean)
    suspend fun setSelectedProviderId(id: String)
    suspend fun setAllowedCapabilities(capabilities: Set<AiCapability>)
    suspend fun setUserConsented(consented: Boolean)
    suspend fun setApiKey(key: String?)
    suspend fun clearConfiguration()
}

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mo_ai_settings",
)

private val AI_ENABLED_KEY = booleanPreferencesKey("ai_enabled")
private val AI_PROVIDER_ID_KEY = stringPreferencesKey("ai_provider_id")
private val AI_CAPABILITIES_KEY = stringSetPreferencesKey("ai_capabilities")
private val AI_USER_CONSENT_KEY = booleanPreferencesKey("ai_user_consent")
private val AI_API_KEY_KEY = stringPreferencesKey("ai_api_key")

class DataStoreAiPreferences(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : AiPreferences {

    override val isAiEnabled: Flow<Boolean> =
        context.aiSettingsDataStore.data.map { prefs ->
            prefs[AI_ENABLED_KEY] ?: false
        }

    override val selectedProviderId: Flow<String> =
        context.aiSettingsDataStore.data.map { prefs ->
            prefs[AI_PROVIDER_ID_KEY] ?: "local_heuristic"
        }

    override val allowedCapabilities: Flow<Set<AiCapability>> =
        context.aiSettingsDataStore.data.map { prefs ->
            val set = prefs[AI_CAPABILITIES_KEY]
            if (set == null) {
                setOf(
                    AiCapability.CLASSIFY_EMAIL,
                    AiCapability.EXTRACT_DEADLINE,
                    AiCapability.SUMMARIZE_THREAD,
                )
            } else {
                set.mapNotNull { name ->
                    runCatching { AiCapability.valueOf(name) }.getOrNull()
                }.toSet()
            }
        }

    override val hasUserConsented: Flow<Boolean> =
        context.aiSettingsDataStore.data.map { prefs ->
            prefs[AI_USER_CONSENT_KEY] ?: false
        }

    override val apiKey: Flow<String?> =
        context.aiSettingsDataStore.data.map { prefs ->
            prefs[AI_API_KEY_KEY]
        }

    override suspend fun setAiEnabled(enabled: Boolean) {
        withContext(dispatchers.io) {
            context.aiSettingsDataStore.edit { prefs ->
                prefs[AI_ENABLED_KEY] = enabled
            }
        }
    }

    override suspend fun setSelectedProviderId(id: String) {
        withContext(dispatchers.io) {
            context.aiSettingsDataStore.edit { prefs ->
                prefs[AI_PROVIDER_ID_KEY] = id
            }
        }
    }

    override suspend fun setAllowedCapabilities(capabilities: Set<AiCapability>) {
        withContext(dispatchers.io) {
            context.aiSettingsDataStore.edit { prefs ->
                prefs[AI_CAPABILITIES_KEY] = capabilities.map { it.name }.toSet()
            }
        }
    }

    override suspend fun setUserConsented(consented: Boolean) {
        withContext(dispatchers.io) {
            context.aiSettingsDataStore.edit { prefs ->
                prefs[AI_USER_CONSENT_KEY] = consented
            }
        }
    }

    override suspend fun setApiKey(key: String?) {
        withContext(dispatchers.io) {
            context.aiSettingsDataStore.edit { prefs ->
                if (key.isNullOrBlank()) {
                    prefs.remove(AI_API_KEY_KEY)
                } else {
                    prefs[AI_API_KEY_KEY] = key.trim()
                }
            }
        }
    }

    override suspend fun clearConfiguration() {
        withContext(dispatchers.io) {
            context.aiSettingsDataStore.edit { prefs ->
                prefs[AI_ENABLED_KEY] = false
                prefs[AI_USER_CONSENT_KEY] = false
                prefs.remove(AI_API_KEY_KEY)
                prefs[AI_PROVIDER_ID_KEY] = "local_heuristic"
            }
        }
    }
}

/**
 * In-memory implementation of [AiPreferences] for unit testing.
 */
class FakeAiPreferences(
    initialEnabled: Boolean = false,
    initialProviderId: String = "local_heuristic",
    initialCapabilities: Set<AiCapability> = setOf(
        AiCapability.CLASSIFY_EMAIL,
        AiCapability.EXTRACT_DEADLINE,
        AiCapability.SUMMARIZE_THREAD,
    ),
    initialConsented: Boolean = false,
    initialApiKey: String? = null,
) : AiPreferences {

    private val _isAiEnabled = MutableStateFlow(initialEnabled)
    override val isAiEnabled: Flow<Boolean> = _isAiEnabled.asStateFlow()

    private val _selectedProviderId = MutableStateFlow(initialProviderId)
    override val selectedProviderId: Flow<String> = _selectedProviderId.asStateFlow()

    private val _allowedCapabilities = MutableStateFlow(initialCapabilities)
    override val allowedCapabilities: Flow<Set<AiCapability>> = _allowedCapabilities.asStateFlow()

    private val _hasUserConsented = MutableStateFlow(initialConsented)
    override val hasUserConsented: Flow<Boolean> = _hasUserConsented.asStateFlow()

    private val _apiKey = MutableStateFlow(initialApiKey)
    override val apiKey: Flow<String?> = _apiKey.asStateFlow()

    override suspend fun setAiEnabled(enabled: Boolean) {
        _isAiEnabled.value = enabled
    }

    override suspend fun setSelectedProviderId(id: String) {
        _selectedProviderId.value = id
    }

    override suspend fun setAllowedCapabilities(capabilities: Set<AiCapability>) {
        _allowedCapabilities.value = capabilities
    }

    override suspend fun setUserConsented(consented: Boolean) {
        _hasUserConsented.value = consented
    }

    override suspend fun setApiKey(key: String?) {
        _apiKey.value = key?.trim()
    }

    override suspend fun clearConfiguration() {
        _isAiEnabled.value = false
        _hasUserConsented.value = false
        _apiKey.value = null
        _selectedProviderId.value = "local_heuristic"
    }
}
