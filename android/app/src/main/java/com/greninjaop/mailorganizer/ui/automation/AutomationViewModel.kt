package com.greninjaop.mailorganizer.ui.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.automation.AutomationAccountScope
import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationCondition
import com.greninjaop.mailorganizer.core.automation.AutomationConditionGroup
import com.greninjaop.mailorganizer.core.automation.AutomationConfirmationPolicy
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionRecord
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.core.automation.AutomationScopeType
import com.greninjaop.mailorganizer.core.automation.AutomationTrigger
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.domain.automation.AutomationPreviewResult
import com.greninjaop.mailorganizer.domain.automation.AutomationRunResult
import com.greninjaop.mailorganizer.domain.automation.AutomationUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutomationUiState(
    val rules: List<AutomationRule> = emptyList(),
    val history: List<AutomationExecutionRecord> = emptyList(),
    val activeAccountId: String? = null,
    val isLoading: Boolean = false,
    val previewResult: AutomationPreviewResult? = null,
    val isPreviewLoading: Boolean = false,
    val lastRunResult: AutomationRunResult? = null,
)

sealed interface AutomationUiEvent {
    data class ShowMessage(val message: String) : AutomationUiEvent
    data class RequireConfirmation(val ruleId: String, val message: String) : AutomationUiEvent
}

class AutomationViewModel(
    private val automationUseCase: AutomationUseCase,
    private val accountRepository: AccountRepository,
    private val activeAccountPreferences: ActiveAccountPreferences,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _events = MutableSharedFlow<AutomationUiEvent>()
    val events: SharedFlow<AutomationUiEvent> = _events.asSharedFlow()

    private val _previewState = MutableStateFlow<AutomationPreviewResult?>(null)
    private val _isPreviewLoading = MutableStateFlow(false)
    private val _lastRunResult = MutableStateFlow<AutomationRunResult?>(null)

    val uiState: StateFlow<AutomationUiState> = activeAccountPreferences.activeSelection
        .flatMapLatest { selection ->
            val accountId = when (selection) {
                is AccountSelection.Single -> selection.accountId
                is AccountSelection.Unified -> null
            }
            combine(
                automationUseCase.getRules(accountId),
                automationUseCase.getHistory(accountId),
                _previewState,
                _isPreviewLoading,
                _lastRunResult,
            ) { rules, history, preview, isPreviewing, runResult ->
                AutomationUiState(
                    rules = rules,
                    history = history,
                    activeAccountId = accountId,
                    isLoading = false,
                    previewResult = preview,
                    isPreviewLoading = isPreviewing,
                    lastRunResult = runResult,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AutomationUiState(isLoading = true),
        )

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            automationUseCase.setRuleEnabled(ruleId, isEnabled)
            _events.emit(AutomationUiEvent.ShowMessage(if (isEnabled) "Automation enabled" else "Automation disabled"))
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch(dispatchers.io) {
            automationUseCase.deleteRule(ruleId)
            _events.emit(AutomationUiEvent.ShowMessage("Automation rule deleted"))
        }
    }

    fun previewRule(rule: AutomationRule, targetAccountId: String) {
        viewModelScope.launch(dispatchers.io) {
            _isPreviewLoading.value = true
            val preview = automationUseCase.previewRule(rule, targetAccountId)
            _previewState.value = preview
            _isPreviewLoading.value = false
        }
    }

    fun clearPreview() {
        _previewState.value = null
    }

    fun runRuleManually(ruleId: String, targetAccountId: String, isConfirmed: Boolean = false) {
        viewModelScope.launch(dispatchers.io) {
            val result = automationUseCase.runRuleManually(ruleId, targetAccountId, isConfirmed)
            _lastRunResult.value = result
            if (result.waitingConfirmationCount > 0 && !isConfirmed) {
                _events.emit(
                    AutomationUiEvent.RequireConfirmation(
                        ruleId = ruleId,
                        message = result.summaries.firstOrNull() ?: "Action requires confirmation",
                    )
                )
            } else if (result.failedCount > 0) {
                _events.emit(AutomationUiEvent.ShowMessage("Execution failed: ${result.summaries.firstOrNull()}"))
            } else {
                _events.emit(AutomationUiEvent.ShowMessage("Executed ${result.executedCount} action(s)"))
            }
        }
    }

    fun saveRule(
        ruleId: String?,
        name: String,
        description: String,
        scopeType: AutomationScopeType,
        targetAccountId: String?,
        triggerType: AutomationTriggerType,
        triggerParam: String?,
        conditionGroup: AutomationConditionGroup,
        actions: List<AutomationAction>,
        confirmationPolicy: AutomationConfirmationPolicy,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch(dispatchers.io) {
            automationUseCase.saveRule(
                ruleId = ruleId,
                name = name,
                description = description,
                scopeType = scopeType,
                targetAccountId = targetAccountId,
                triggerType = triggerType,
                triggerParam = triggerParam,
                conditionGroup = conditionGroup,
                actions = actions,
                confirmationPolicy = confirmationPolicy,
            )
            _events.emit(AutomationUiEvent.ShowMessage("Automation saved successfully"))
            onSuccess()
        }
    }
}

class AutomationViewModelFactory(
    private val container: com.greninjaop.mailorganizer.di.AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AutomationViewModel(
            automationUseCase = container.automationUseCase,
            accountRepository = container.accountRepository,
            activeAccountPreferences = container.activeAccountPreferences,
            dispatchers = container.dispatchers,
        ) as T
    }
}
