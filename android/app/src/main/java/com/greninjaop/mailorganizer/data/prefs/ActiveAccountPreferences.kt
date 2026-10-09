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
 * Representation of the user's active account selection.
 *
 * In Phase 18, Mail Organizer supports selecting an isolated account or
 * viewing a safe unified inbox across all accounts.
 */
sealed interface AccountSelection {
    /** Unified inbox view across all enabled accounts (presentation only). */
    data object Unified : AccountSelection

    /** Scoped view to one specific account. */
    data class Single(val accountId: String) : AccountSelection
}

/**
 * Persists the user's active account selection (Phase 18).
 */
interface ActiveAccountPreferences {
    val activeSelection: Flow<AccountSelection>
    val activeAccountSelection: Flow<AccountSelection> get() = activeSelection
    suspend fun setActiveSelection(selection: AccountSelection)
    suspend fun setActiveAccount(accountId: String?) {
        setActiveSelection(if (accountId == null) AccountSelection.Unified else AccountSelection.Single(accountId))
    }
}

private val Context.activeAccountDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mo_active_account",
)

private val ACTIVE_ACCOUNT_KEY = stringPreferencesKey("active_account_id")
private const val UNIFIED_SENTINEL = "__UNIFIED__"

class DataStoreActiveAccountPreferences(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) : ActiveAccountPreferences {

    override val activeSelection: Flow<AccountSelection> =
        context.activeAccountDataStore.data.map { prefs ->
            val raw = prefs[ACTIVE_ACCOUNT_KEY]
            when {
                raw == null -> AccountSelection.Unified // Default to Unified or fallback
                raw == UNIFIED_SENTINEL -> AccountSelection.Unified
                else -> AccountSelection.Single(raw)
            }
        }

    override suspend fun setActiveSelection(selection: AccountSelection) {
        withContext(dispatchers.io) {
            context.activeAccountDataStore.edit { prefs ->
                when (selection) {
                    AccountSelection.Unified -> prefs[ACTIVE_ACCOUNT_KEY] = UNIFIED_SENTINEL
                    is AccountSelection.Single -> prefs[ACTIVE_ACCOUNT_KEY] = selection.accountId
                }
            }
        }
    }
}
