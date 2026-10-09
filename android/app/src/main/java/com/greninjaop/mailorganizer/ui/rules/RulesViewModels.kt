package com.greninjaop.mailorganizer.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.rules.EvaluableRule
import com.greninjaop.mailorganizer.core.rules.RuleAction
import com.greninjaop.mailorganizer.core.rules.RuleActionType
import com.greninjaop.mailorganizer.core.rules.RuleCondition
import com.greninjaop.mailorganizer.core.rules.RuleConditionField
import com.greninjaop.mailorganizer.core.rules.RuleConflict
import com.greninjaop.mailorganizer.core.rules.RuleOperator
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.CorrectionScope
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.domain.rules.RecordCorrectionUseCase
import com.greninjaop.mailorganizer.domain.rules.RuleManagementUseCase
import com.greninjaop.mailorganizer.domain.rules.RulePreview
import com.greninjaop.mailorganizer.domain.rules.UserRule
import com.greninjaop.mailorganizer.ui.rules.RuleEditorModels.ConditionDraft
import com.greninjaop.mailorganizer.ui.rules.RuleEditorModels.ActionDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Phase 12 — Rules list ViewModel (§9, §23).
 *
 * Surfaces all rules for the active account with enable/disable toggles
 * and delete-with-confirmation. Conflict rows are computed once at load.
 */
class RulesViewModel(
    private val accounts: AccountRepository,
    private val management: RuleManagementUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    data class UiState(
        val rules: List<UserRule> = emptyList(),
        val conflicts: List<String> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val pendingDelete: UserRule? = null,
        val noAccount: Boolean = false,
    )

    private val _deleteRequested = MutableStateFlow<UserRule?>(null)
    val deleteRequested: StateFlow<UserRule?> = _deleteRequested

    /** Active account: earliest-created enabled account (same rule as Categories). */
    private val activeAccount: StateFlow<AccountRecord?> =
        accounts.observeAll()
            .map { list ->
                list.filter { it.isEnabled }.minWithOrNull(
                    compareBy<AccountRecord> { it.createdAtEpochMs }
                        .thenBy { it.accountId },
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentAccount: StateFlow<AccountRecord?> = activeAccount

    /**
     * Rules flow joined with local conflict detection (§23). Conflicts
     * are computed from the same rule list the UI shows, so they stay
     * in sync without extra queries.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState> = activeAccount.flatMapLatest { account ->
        if (account == null) {
            kotlinx.coroutines.flow.flowOf(UiState(isLoading = false, noAccount = true))
        } else {
            val accountId = account.accountId
            kotlinx.coroutines.flow.combine(
                management.observeRules(accountId),
                _deleteRequested,
            ) { rules, pendingDelete ->
                val conflicts = management.detectConflicts(accountId).map { it.describeForUi() }
                UiState(
                    rules = rules,
                    conflicts = conflicts,
                    isLoading = false,
                    pendingDelete = pendingDelete,
                )
            }
        }
    }.catch { emit(UiState(error = "Couldn't load rules", isLoading = false)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun toggleEnabled(rule: UserRule) {
        val accountId = currentAccount.value?.accountId ?: return
        viewModelScope.launch(dispatchers.io) {
            runCatching { management.setRuleEnabled(accountId, rule.id, !rule.enabled) }
        }
    }

    fun requestDelete(rule: UserRule) {
        _deleteRequested.value = rule
    }

    fun dismissDelete() {
        _deleteRequested.value = null
    }

    fun confirmDelete() {
        val rule = _deleteRequested.value ?: return
        val accountId = currentAccount.value?.accountId ?: return
        _deleteRequested.value = null
        viewModelScope.launch(dispatchers.io) {
            runCatching { management.deleteRule(accountId, rule.id) }
        }
    }
}

/**
 * Phase 12 — Rule editor ViewModel (§16–§23).
 *
 * Draft-based: conditions/actions are edited locally, previewed against
 * real mailbox data, and only persisted on save.
 */
class RuleEditorViewModel(
    private val ruleId: Long?,
    private val accounts: AccountRepository,
    private val management: RuleManagementUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    data class EditorState(
        val accountId: String? = null,
        val name: String = "",
        val conditions: List<ConditionDraft> = listOf(ConditionDraft()),
        val actions: List<ActionDraft> = listOf(ActionDraft()),
        val enabled: Boolean = true,
        val order: String = "0",
        val isSaving: Boolean = false,
        val isLoading: Boolean = true,
        val error: String? = null,
        val preview: RulePreview? = null,
        val conflicts: List<String> = emptyList(),
    )

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    init {
        viewModelScope.launch(dispatchers.io) {
            val account = accounts.observeAll()
                .map { list ->
                    list.filter { it.isEnabled }.minWithOrNull(
                        compareBy<AccountRecord> { it.createdAtEpochMs }.thenBy { it.accountId },
                    )
                }
                .first()
            if (account == null) {
                _state.value = _state.value.copy(isLoading = false, error = "No account")
                return@launch
            }
            if (ruleId != null) {
                val rule = management.getRule(ruleId)
                if (rule == null || rule.accountId != account.accountId) {
                    _state.value = _state.value.copy(
                        accountId = account.accountId,
                        isLoading = false,
                        error = "Rule not found",
                    )
                } else {
                    _state.value = EditorState(
                        accountId = account.accountId,
                        name = rule.name,
                        conditions = rule.conditions.map { ConditionDraft(it.field, it.operator, it.value) },
                        actions = rule.actions.map { ActionDraft(it.type, it.value) },
                        enabled = rule.enabled,
                        order = rule.order.toString(),
                        isLoading = false,
                    )
                }
            } else {
                _state.value = _state.value.copy(accountId = account.accountId, isLoading = false)
            }
        }
    }

    fun setName(name: String) { _state.value = _state.value.copy(name = name) }

    fun setEnabled(enabled: Boolean) { _state.value = _state.value.copy(enabled = enabled) }

    fun setOrder(order: String) {
        // Digits only; precedence is lower-number-wins (§18).
        _state.value = _state.value.copy(order = order.filter { it.isDigit() }.take(4))
    }

    fun addCondition() {
        _state.value = _state.value.copy(
            conditions = _state.value.conditions + ConditionDraft(),
        )
    }

    fun updateCondition(index: Int, draft: ConditionDraft) {
        _state.value = _state.value.copy(
            conditions = _state.value.conditions.toMutableList().also { it[index] = draft },
        )
    }

    fun removeCondition(index: Int) {
        _state.value = _state.value.copy(
            conditions = _state.value.conditions.filterIndexed { i, _ -> i != index },
        )
    }

    fun addAction() {
        _state.value = _state.value.copy(actions = _state.value.actions + ActionDraft())
    }

    fun updateAction(index: Int, draft: ActionDraft) {
        _state.value = _state.value.copy(
            actions = _state.value.actions.toMutableList().also { it[index] = draft },
        )
    }

    fun removeAction(index: Int) {
        _state.value = _state.value.copy(
            actions = _state.value.actions.filterIndexed { i, _ -> i != index },
        )
    }

    /** Runs the preview (§19) and conflict check (§23) against local data. */
    fun refreshPreview() {
        val accountId = _state.value.accountId ?: run {
            _state.value = _state.value.copy(error = "No account")
            return
        }
        val rule = buildRule(accountId) ?: run {
            _state.value = _state.value.copy(
                preview = null,
                conflicts = emptyList(),
                error = "Add at least one complete condition and one action to preview",
            )
            return
        }
        viewModelScope.launch(dispatchers.io) {
            val preview = runCatching { management.previewRule(accountId, rule) }
                .getOrElse { return@launch }
            val conflicts = runCatching {
                management.detectConflicts(accountId)
                    .filter { it.ruleA.id == ruleId || it.ruleB.id == ruleId }
                    .map { it.describeForUi() }
            }.getOrDefault(emptyList())
            _state.value = _state.value.copy(preview = preview, conflicts = conflicts, error = null)
        }
    }

    fun save(onDone: () -> Unit) {
        val accountId = _state.value.accountId ?: run {
            _state.value = _state.value.copy(error = "No account")
            return
        }
        val rule = buildRule(accountId) ?: run {
            _state.value = _state.value.copy(error = "Complete all conditions and actions before saving")
            return
        }
        _state.value = _state.value.copy(isSaving = true, error = null)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                if (ruleId == null) {
                    val id = management.createRule(rule)
                    check(id > 0) { "create failed" }
                } else {
                    check(management.updateRule(rule)) { "update failed" }
                }
            }
            _state.value = _state.value.copy(isSaving = false)
            result.onSuccess { onDone() }
                .onFailure { _state.value = _state.value.copy(error = "Couldn't save the rule") }
        }
    }

    private fun buildRule(accountId: String): UserRule? {
        val s = _state.value
        val conditions = s.conditions.mapNotNull { d ->
            if (d.value.isBlank()) null
            else RuleCondition(d.field, d.operator, d.value.trim())
        }
        val actions = s.actions.mapNotNull { d ->
            if (d.value.isBlank()) null
            else RuleAction(d.type, d.value.trim())
        }
        if (conditions.isEmpty() || actions.isEmpty()) return null
        val order = s.order.toIntOrNull() ?: 0
        val now = System.currentTimeMillis()
        val fallbackName = "If ${conditions.first().field.name.lowercase().replace('_', ' ')} " +
            "is ${conditions.first().value} then ${actions.first().type.name.lowercase().replace('_', ' ')}"
        return UserRule(
            id = ruleId ?: 0L,
            accountId = accountId,
            name = s.name.ifBlank { fallbackName },
            enabled = s.enabled,
            order = order,
            conditions = conditions,
            actions = actions,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
    }
}

/** Human-readable conflict summary for the rules UI (§23). */
private fun RuleConflict.describeForUi(): String {
    val winner = if (winnerId == ruleA.id) ruleA.name else ruleB.name
    val fieldName = field.name.lowercase().replace('_', ' ')
    return "“${ruleA.name}” and “${ruleB.name}” both set $fieldName " +
        "(\"${valueA}\" vs \"${valueB}\"). “$winner” wins by order."
}

/** Draft models for the rule editor — UI-only, never persisted directly. */
object RuleEditorModels {
    data class ConditionDraft(
        val field: RuleConditionField = RuleConditionField.SENDER_EMAIL,
        val operator: RuleOperator = RuleOperator.EQUALS,
        val value: String = "",
    )

    data class ActionDraft(
        val type: RuleActionType = RuleActionType.SET_CATEGORY,
        val value: String = MailCategory.NEWSLETTERS.name,
    )
}

/**
 * Phase 12 — Correction sheet ViewModel (§4–§6).
 *
 * Records category/priority corrections at message, sender, or domain
 * scope, then re-applies the effective pipeline for the affected message.
 */
class CorrectionViewModel(
    private val record: RecordCorrectionUseCase,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    data class CorrectionState(
        val accountId: String,
        val messageId: String,
        val senderEmail: String,
        val senderDomain: String,
        val companyName: String? = null,
        val currentCategory: MailCategory? = null,
        val currentPriority: Priority? = null,
        val categoryOverridden: Boolean = false,
        val priorityOverridden: Boolean = false,
        val selectedCategory: MailCategory? = null,
        val selectedPriority: Priority? = null,
        val isApplying: Boolean = false,
        val error: String? = null,
        val done: Boolean = false,
    )

    private val _state = MutableStateFlow<CorrectionState?>(null)
    val state: StateFlow<CorrectionState?> = _state

    fun start(
        accountId: String,
        messageId: String,
        senderEmail: String,
        senderDomain: String,
        companyName: String?,
        currentCategory: MailCategory?,
        currentPriority: Priority?,
        categoryOverridden: Boolean,
        priorityOverridden: Boolean,
    ) {
        _state.value = CorrectionState(
            accountId = accountId,
            messageId = messageId,
            senderEmail = senderEmail,
            senderDomain = senderDomain,
            companyName = companyName,
            currentCategory = currentCategory,
            currentPriority = currentPriority,
            categoryOverridden = categoryOverridden,
            priorityOverridden = priorityOverridden,
        )
    }

    fun dismiss() {
        _state.value = null
    }

    fun selectCategory(category: MailCategory) {
        _state.value = _state.value?.copy(selectedCategory = category, error = null)
    }

    fun selectPriority(priority: Priority) {
        _state.value = _state.value?.copy(selectedPriority = priority, error = null)
    }

    /**
     * Applies the correction at the chosen scope (§5: undo deletes the
     * correction row; §6: explicit corrections win over rules).
     *
     * The use case records the correction and re-applies the effective
     * pipeline for affected messages — no separate refresh needed.
     */
    fun applyCorrection(scope: CorrectionScopeChoice) {
        val s = _state.value ?: return
        if (s.selectedCategory == null && s.selectedPriority == null) {
            _state.value = s.copy(error = "Pick a category or priority first")
            return
        }
        _state.value = s.copy(isApplying = true, error = null)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                when (scope) {
                    CorrectionScopeChoice.MESSAGE -> {
                        s.selectedCategory?.let {
                            record.correctMessage(
                                s.accountId, s.messageId,
                                CorrectionField.CATEGORY, it.name,
                            )
                        }
                        s.selectedPriority?.let {
                            record.correctMessage(
                                s.accountId, s.messageId,
                                CorrectionField.PRIORITY, it.name,
                            )
                        }
                    }
                    CorrectionScopeChoice.SENDER -> {
                        s.selectedCategory?.let {
                            record.correctSender(
                                s.accountId, s.senderEmail,
                                CorrectionField.CATEGORY, it.name,
                            )
                        }
                        s.selectedPriority?.let {
                            record.correctSender(
                                s.accountId, s.senderEmail,
                                CorrectionField.PRIORITY, it.name,
                            )
                        }
                    }
                    CorrectionScopeChoice.DOMAIN -> {
                        s.selectedCategory?.let {
                            record.correctDomain(
                                s.accountId, s.senderDomain,
                                CorrectionField.CATEGORY, it.name,
                            )
                        }
                        s.selectedPriority?.let {
                            record.correctDomain(
                                s.accountId, s.senderDomain,
                                CorrectionField.PRIORITY, it.name,
                            )
                        }
                    }
                }
            }
            result.onSuccess { _state.value = _state.value?.copy(isApplying = false, done = true) }
                .onFailure { _state.value = _state.value?.copy(isApplying = false, error = "Couldn't apply the correction") }
        }
    }

    fun undoCorrection(scope: CorrectionScopeChoice) {
        val s = _state.value ?: return
        _state.value = s.copy(isApplying = true, error = null)
        viewModelScope.launch(dispatchers.io) {
            val result = runCatching {
                val (correctionScope, scopeKey) = when (scope) {
                    CorrectionScopeChoice.MESSAGE ->
                        CorrectionScope.MESSAGE to s.messageId
                    CorrectionScopeChoice.SENDER ->
                        CorrectionScope.SENDER to s.senderEmail
                    CorrectionScopeChoice.DOMAIN ->
                        CorrectionScope.DOMAIN to s.senderDomain
                }
                // Undo removes both category and priority corrections at
                // this scope (§5); the deterministic base is restored by
                // the pipeline's re-application.
                record.undoCorrection(s.accountId, correctionScope, scopeKey, CorrectionField.CATEGORY)
                record.undoCorrection(s.accountId, correctionScope, scopeKey, CorrectionField.PRIORITY)
            }
            result.onSuccess { _state.value = _state.value?.copy(isApplying = false, done = true) }
                .onFailure { _state.value = _state.value?.copy(isApplying = false, error = "Couldn't undo the correction") }
        }
    }

    enum class CorrectionScopeChoice { MESSAGE, SENDER, DOMAIN }
}

/** Factories — same discipline as the mail factories. */
class RulesViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RulesViewModel(container.accountRepository, container.ruleManagementUseCase, container.dispatchers) as T
}

class RuleEditorViewModelFactory(
    private val container: AppContainer,
    private val ruleId: Long?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RuleEditorViewModel(ruleId, container.accountRepository, container.ruleManagementUseCase, container.dispatchers) as T
}

class CorrectionViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CorrectionViewModel(container.recordCorrectionUseCase, container.dispatchers) as T
}
