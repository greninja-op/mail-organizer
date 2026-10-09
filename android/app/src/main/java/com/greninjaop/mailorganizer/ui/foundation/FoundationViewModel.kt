package com.greninjaop.mailorganizer.ui.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import com.greninjaop.mailorganizer.data.prefs.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Phase 0 foundation screen. Owns the persisted theme-mode
 * preference — a real, minimal slice proving ViewModel + DataStore + theme
 * wiring end to end.
 */
class FoundationViewModel(
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
