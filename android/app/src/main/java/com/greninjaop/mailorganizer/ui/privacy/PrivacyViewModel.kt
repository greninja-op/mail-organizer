package com.greninjaop.mailorganizer.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.privacy.DataInventoryItem
import com.greninjaop.mailorganizer.core.privacy.PermissionScopeDisclosure
import com.greninjaop.mailorganizer.core.privacy.SecurityAuditSnapshot
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.domain.privacy.PrivacyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Privacy Center (Phase 23 §40–§50).
 */
data class PrivacyUiState(
    val isLoading: Boolean = true,
    val accounts: List<AccountRecord> = emptyList(),
    val dataInventory: List<DataInventoryItem> = emptyList(),
    val scopes: List<PermissionScopeDisclosure> = emptyList(),
    val auditSnapshot: SecurityAuditSnapshot? = null,
    val selectedTab: PrivacyCenterTab = PrivacyCenterTab.OVERVIEW,
    val showClearAllDialog: Boolean = false,
    val showDeleteAccountDialogFor: AccountRecord? = null,
    val statusMessage: String? = null,
)

enum class PrivacyCenterTab {
    OVERVIEW,
    DATA_INVENTORY,
    PERMISSIONS,
    SECURITY_HARDENING,
}

/**
 * ViewModel driving Privacy Center interactions.
 */
class PrivacyViewModel(
    private val privacyUseCase: PrivacyUseCase,
    private val accountRepository: AccountRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeAccounts()
    }

    fun selectTab(tab: PrivacyCenterTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun requestDeleteAccount(account: AccountRecord) {
        _uiState.update { it.copy(showDeleteAccountDialogFor = account) }
    }

    fun dismissDeleteAccount() {
        _uiState.update { it.copy(showDeleteAccountDialogFor = null) }
    }

    fun confirmDeleteAccount(accountId: String) {
        viewModelScope.launch(dispatchers.io) {
            privacyUseCase.deleteAccountData(accountId)
            _uiState.update {
                it.copy(
                    showDeleteAccountDialogFor = null,
                    statusMessage = "Local account data and Google access disconnected.",
                )
            }
            refreshAudit()
        }
    }

    fun requestClearAll() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    fun dismissClearAll() {
        _uiState.update { it.copy(showClearAllDialog = false) }
    }

    fun confirmClearAll() {
        viewModelScope.launch(dispatchers.io) {
            privacyUseCase.clearAllLocalData()
            _uiState.update {
                it.copy(
                    showClearAllDialog = false,
                    statusMessage = "All local data has been purged.",
                )
            }
            refreshAudit()
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun refreshAudit() {
        viewModelScope.launch(dispatchers.io) {
            val audit = privacyUseCase.getSecurityAuditSnapshot()
            _uiState.update { it.copy(auditSnapshot = audit) }
        }
    }

    private fun loadData() {
        viewModelScope.launch(dispatchers.io) {
            val inventory = privacyUseCase.getDataInventory()
            val scopes = privacyUseCase.getScopeDisclosures()
            val audit = privacyUseCase.getSecurityAuditSnapshot()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    dataInventory = inventory,
                    scopes = scopes,
                    auditSnapshot = audit,
                )
            }
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch(dispatchers.io) {
            accountRepository.observeAll().collect { list ->
                _uiState.update { it.copy(accounts = list) }
            }
        }
    }
}
