package com.greninjaop.mailorganizer.data.automation

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.automation.AutomationAccountScope
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionRecord
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.core.automation.AutomationTrigger
import com.greninjaop.mailorganizer.data.local.AutomationExecutionRecordEntity
import com.greninjaop.mailorganizer.data.local.AutomationHistoryDao
import com.greninjaop.mailorganizer.data.local.AutomationRuleDao
import com.greninjaop.mailorganizer.data.local.AutomationRuleRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AutomationRepository {
    fun getAllRules(): Flow<List<AutomationRule>>
    fun getRulesForAccount(accountId: String): Flow<List<AutomationRule>>
    suspend fun getRuleById(ruleId: String): AutomationRule?
    suspend fun saveRule(rule: AutomationRule)
    suspend fun setRuleState(ruleId: String, isEnabled: Boolean)
    suspend fun deleteRule(ruleId: String)
    suspend fun recordSuccess(ruleId: String, timestampEpochMs: Long)
    suspend fun recordFailure(ruleId: String, timestampEpochMs: Long)

    fun getRecentHistory(limit: Int = 100): Flow<List<AutomationExecutionRecord>>
    fun getHistoryForAccount(accountId: String, limit: Int = 100): Flow<List<AutomationExecutionRecord>>
    suspend fun recordExecution(record: AutomationExecutionRecord)
    suspend fun isAlreadyExecuted(accountId: String, messageId: String, triggerType: com.greninjaop.mailorganizer.core.automation.AutomationTriggerType): Boolean
    suspend fun purgeAccountData(accountId: String)
    suspend fun pruneHistoryOlderThan(cutoffEpochMs: Long)
}

class RoomAutomationRepository(
    private val ruleDao: AutomationRuleDao,
    private val historyDao: AutomationHistoryDao,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : AutomationRepository {

    override fun getAllRules(): Flow<List<AutomationRule>> =
        ruleDao.getAllRules().map { list -> list.map { it.toDomain() } }

    override fun getRulesForAccount(accountId: String): Flow<List<AutomationRule>> =
        ruleDao.getRulesForAccount(accountId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRuleById(ruleId: String): AutomationRule? = withContext(dispatchers.io) {
        ruleDao.getRuleById(ruleId)?.toDomain()
    }

    override suspend fun saveRule(rule: AutomationRule): Unit = withContext(dispatchers.io) {
        ruleDao.insertRule(rule.toEntity())
    }

    override suspend fun setRuleState(ruleId: String, isEnabled: Boolean): Unit = withContext(dispatchers.io) {
        val state = if (isEnabled) {
            com.greninjaop.mailorganizer.core.automation.AutomationLifecycleState.ENABLED
        } else {
            com.greninjaop.mailorganizer.core.automation.AutomationLifecycleState.DISABLED
        }
        ruleDao.updateRuleState(ruleId, state, clock())
    }

    override suspend fun deleteRule(ruleId: String): Unit = withContext(dispatchers.io) {
        ruleDao.deleteRule(ruleId)
    }

    override suspend fun recordSuccess(ruleId: String, timestampEpochMs: Long): Unit = withContext(dispatchers.io) {
        ruleDao.recordSuccessfulRun(ruleId, timestampEpochMs)
    }

    override suspend fun recordFailure(ruleId: String, timestampEpochMs: Long): Unit = withContext(dispatchers.io) {
        ruleDao.incrementFailureCount(ruleId, timestampEpochMs)
    }

    override fun getRecentHistory(limit: Int): Flow<List<AutomationExecutionRecord>> =
        historyDao.getRecentHistory(limit).map { list -> list.map { it.toDomain() } }

    override fun getHistoryForAccount(accountId: String, limit: Int): Flow<List<AutomationExecutionRecord>> =
        historyDao.getHistoryForAccount(accountId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun recordExecution(record: AutomationExecutionRecord): Unit = withContext(dispatchers.io) {
        historyDao.insertHistory(record.toEntity())
    }

    override suspend fun isAlreadyExecuted(
        accountId: String,
        messageId: String,
        triggerType: com.greninjaop.mailorganizer.core.automation.AutomationTriggerType,
    ): Boolean = withContext(dispatchers.io) {
        historyDao.countSuccessfulExecutions(accountId, messageId, triggerType) > 0
    }

    override suspend fun purgeAccountData(accountId: String): Unit = withContext(dispatchers.io) {
        ruleDao.deleteRulesForAccount(accountId)
        historyDao.deleteHistoryForAccount(accountId)
    }

    override suspend fun pruneHistoryOlderThan(cutoffEpochMs: Long): Unit = withContext(dispatchers.io) {
        historyDao.pruneOldHistory(cutoffEpochMs)
    }

    private fun AutomationRuleRecord.toDomain(): AutomationRule =
        AutomationRule(
            id = ruleId,
            name = name,
            description = description,
            scope = AutomationAccountScope(
                type = scopeType,
                specificAccountId = targetAccountId,
            ),
            state = state,
            trigger = AutomationTrigger(
                type = triggerType,
                parameter = triggerParam,
            ),
            conditionGroup = AutomationJsonCodec.conditionGroupFromJson(conditionGroupJson),
            actions = AutomationJsonCodec.actionsFromJson(actionsJson),
            confirmationPolicy = confirmationPolicy,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
            version = version,
            lastRunEpochMs = lastRunEpochMs,
            failureCount = failureCount,
        )

    private fun AutomationRule.toEntity(): AutomationRuleRecord =
        AutomationRuleRecord(
            ruleId = id,
            name = name,
            description = description,
            scopeType = scope.type,
            targetAccountId = scope.specificAccountId,
            state = state,
            triggerType = trigger.type,
            triggerParam = trigger.parameter,
            conditionGroupJson = AutomationJsonCodec.conditionGroupToJson(conditionGroup),
            actionsJson = AutomationJsonCodec.actionsToJson(actions),
            confirmationPolicy = confirmationPolicy,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
            lastRunEpochMs = lastRunEpochMs,
            failureCount = failureCount,
            version = version,
        )

    private fun AutomationExecutionRecordEntity.toDomain(): AutomationExecutionRecord =
        AutomationExecutionRecord(
            executionId = executionId,
            automationId = automationId,
            automationName = automationName,
            accountId = accountId,
            triggerType = triggerType,
            sourceMessageId = sourceMessageId,
            sourceThreadId = sourceThreadId,
            actionSummary = actionSummary,
            status = status,
            executedAtEpochMs = executedAtEpochMs,
            detailMessage = detailMessage,
        )

    private fun AutomationExecutionRecord.toEntity(): AutomationExecutionRecordEntity =
        AutomationExecutionRecordEntity(
            executionId = executionId,
            automationId = automationId,
            automationName = automationName,
            accountId = accountId,
            triggerType = triggerType,
            sourceMessageId = sourceMessageId,
            sourceThreadId = sourceThreadId,
            actionSummary = actionSummary,
            status = status,
            executedAtEpochMs = executedAtEpochMs,
            detailMessage = detailMessage,
        )
}
