package com.greninjaop.mailorganizer.domain.automation

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.automation.AutomationAccountScope
import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationConditionGroup
import com.greninjaop.mailorganizer.core.automation.AutomationConfirmationPolicy
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionRecord
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.core.automation.AutomationScopeType
import com.greninjaop.mailorganizer.core.automation.AutomationTrigger
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType
import com.greninjaop.mailorganizer.data.automation.AutomationRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Domain Use Case for managing Automation Rules (Phase 27).
 *
 * Provides:
 * - Rule CRUD with transactional persistence.
 * - Live preview evaluation before saving.
 * - Safe toggle / delete without side effects on emails.
 * - Execution history access and cleanup.
 */
class AutomationUseCase(
    private val repository: AutomationRepository,
    private val engine: AutomationEngine,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun getRules(accountId: String?): Flow<List<AutomationRule>> =
        if (accountId == null) {
            repository.getAllRules()
        } else {
            repository.getRulesForAccount(accountId)
        }

    fun getHistory(accountId: String?): Flow<List<AutomationExecutionRecord>> =
        if (accountId == null) {
            repository.getRecentHistory()
        } else {
            repository.getHistoryForAccount(accountId)
        }

    suspend fun getRuleById(ruleId: String): AutomationRule? =
        repository.getRuleById(ruleId)

    suspend fun previewRule(
        rule: AutomationRule,
        targetAccountId: String,
    ): AutomationPreviewResult = engine.previewRule(rule, targetAccountId)

    suspend fun saveRule(
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
    ): AutomationRule = withContext(dispatchers.io) {
        val now = clock()
        val id = ruleId ?: UUID.randomUUID().toString()
        val existing = if (ruleId != null) repository.getRuleById(ruleId) else null

        val rule = AutomationRule(
            id = id,
            name = name.trim(),
            description = description.trim(),
            scope = AutomationAccountScope(scopeType, if (scopeType == AutomationScopeType.ALL_ACCOUNTS) null else targetAccountId),
            state = existing?.state ?: com.greninjaop.mailorganizer.core.automation.AutomationLifecycleState.ENABLED,
            trigger = AutomationTrigger(triggerType, triggerParam?.trim()),
            conditionGroup = conditionGroup,
            actions = actions,
            confirmationPolicy = confirmationPolicy,
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
            version = (existing?.version ?: 0) + 1,
            lastRunEpochMs = existing?.lastRunEpochMs,
            failureCount = existing?.failureCount ?: 0,
        )

        repository.saveRule(rule)
        rule
    }

    suspend fun setRuleEnabled(ruleId: String, isEnabled: Boolean) {
        repository.setRuleState(ruleId, isEnabled)
    }

    suspend fun deleteRule(ruleId: String) {
        repository.deleteRule(ruleId)
    }

    suspend fun runRuleManually(
        ruleId: String,
        targetAccountId: String,
        isConfirmed: Boolean = false,
    ): AutomationRunResult = engine.runRule(ruleId, targetAccountId, isConfirmed)

    suspend fun purgeAccountAutomations(accountId: String) {
        repository.purgeAccountData(accountId)
    }
}
