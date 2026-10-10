package com.greninjaop.mailorganizer.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.analytics.AnalyticsDateRange
import com.greninjaop.mailorganizer.core.analytics.AnalyticsSnapshot
import com.greninjaop.mailorganizer.core.analytics.UNIFIED_ACCOUNT_ID
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.domain.analytics.AnalyticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(val snapshot: AnalyticsSnapshot) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}

/**
 * ViewModel for Analytics & Insights Screen (Phase 25).
 *
 * Supports:
 * - Dynamic date range filtering (Today, Last 7 days, Last 30 days, All time)
 * - Multi-account switching & unified inbox view
 * - Auto-refreshes on account changes or pull-to-refresh
 */
class AnalyticsViewModel(
    private val analyticsUseCase: AnalyticsUseCase,
    private val accountRepository: AccountRepository,
    private val activeAccountPreferences: ActiveAccountPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _selectedRange = MutableStateFlow(AnalyticsDateRange.LAST_7_DAYS)
    val selectedRange: StateFlow<AnalyticsDateRange> = _selectedRange.asStateFlow()

    val currentAccount: StateFlow<AccountRecord?> = combine(
        accountRepository.observeAll(),
        activeAccountPreferences.activeAccountSelection,
    ) { allAccounts, selection ->
        when (selection) {
            is AccountSelection.Unified -> AccountRecord(
                accountId = UNIFIED_ACCOUNT_ID,
                emailAddress = "All Accounts",
                displayName = "Unified Inbox",
                createdAtEpochMs = 0L,
            )
            is AccountSelection.Single -> allAccounts.firstOrNull { it.accountId == selection.accountId }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            combine(
                activeAccountPreferences.activeAccountSelection,
                _selectedRange,
            ) { selection, range ->
                val targetId = when (selection) {
                    is AccountSelection.Unified -> UNIFIED_ACCOUNT_ID
                    is AccountSelection.Single -> selection.accountId
                }
                targetId to range
            }.collect { (accountId, range) ->
                loadAnalytics(accountId, range)
            }
        }
    }

    fun setDateRange(range: AnalyticsDateRange) {
        if (_selectedRange.value != range) {
            _selectedRange.value = range
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val selection = activeAccountPreferences.activeAccountSelection.first()
            val targetId = when (selection) {
                is AccountSelection.Unified -> UNIFIED_ACCOUNT_ID
                is AccountSelection.Single -> selection.accountId
            }
            loadAnalytics(targetId, _selectedRange.value)
        }
    }

    private suspend fun loadAnalytics(accountId: String?, range: AnalyticsDateRange) {
        _uiState.value = AnalyticsUiState.Loading
        try {
            val snapshot = analyticsUseCase.getAnalyticsSnapshot(accountId, range)
            _uiState.value = AnalyticsUiState.Success(snapshot)
        } catch (e: Exception) {
            _uiState.value = AnalyticsUiState.Error(e.message ?: "Failed to generate analytics")
        }
    }
}
