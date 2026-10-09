package com.greninjaop.mailorganizer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.greninjaop.mailorganizer.core.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * App-level settings. Interface + implementation so tests use a fake and the
 * account-scoped variant (Phase 2: one DataStore file per account, keyed by
 * [com.greninjaop.mailorganizer.core.AccountId]) can share the contract.
 */
interface ThemePreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mo_settings",
)

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class DataStoreThemePreferences(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : ThemePreferences {

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            prefs[THEME_MODE_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        withContext(dispatchers.io) {
            context.settingsDataStore.edit { prefs ->
                prefs[THEME_MODE_KEY] = mode.name
            }
        }
    }
}
