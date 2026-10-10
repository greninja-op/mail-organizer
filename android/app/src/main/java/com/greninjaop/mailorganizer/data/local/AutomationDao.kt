package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionStatus
import com.greninjaop.mailorganizer.core.automation.AutomationLifecycleState
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRuleDao {

    @Query("SELECT * FROM automation_rules ORDER BY updatedAtEpochMs DESC")
    fun getAllRules(): Flow<List<AutomationRuleRecord>>

    @Query("SELECT * FROM automation_rules WHERE targetAccountId = :accountId OR targetAccountId IS NULL ORDER BY updatedAtEpochMs DESC")
    fun getRulesForAccount(accountId: String): Flow<List<AutomationRuleRecord>>

    @Query("SELECT * FROM automation_rules WHERE ruleId = :ruleId")
    suspend fun getRuleById(ruleId: String): AutomationRuleRecord?

    @Query("SELECT * FROM automation_rules WHERE state = :state AND (targetAccountId = :accountId OR targetAccountId IS NULL)")
    suspend fun getActiveRulesForAccount(state: AutomationLifecycleState, accountId: String): List<AutomationRuleRecord>

    @Query("SELECT * FROM automation_rules WHERE state = :state AND triggerType = :triggerType AND (targetAccountId = :accountId OR targetAccountId IS NULL)")
    suspend fun getRulesByTrigger(
        state: AutomationLifecycleState,
        triggerType: AutomationTriggerType,
        accountId: String,
    ): List<AutomationRuleRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRuleRecord)

    @Update
    suspend fun updateRule(rule: AutomationRuleRecord)

    @Query("UPDATE automation_rules SET state = :newState, updatedAtEpochMs = :now WHERE ruleId = :ruleId")
    suspend fun updateRuleState(ruleId: String, newState: AutomationLifecycleState, now: Long)

    @Query("UPDATE automation_rules SET failureCount = failureCount + 1, updatedAtEpochMs = :now WHERE ruleId = :ruleId")
    suspend fun incrementFailureCount(ruleId: String, now: Long)

    @Query("UPDATE automation_rules SET lastRunEpochMs = :runTime, failureCount = 0, updatedAtEpochMs = :runTime WHERE ruleId = :ruleId")
    suspend fun recordSuccessfulRun(ruleId: String, runTime: Long)

    @Query("DELETE FROM automation_rules WHERE ruleId = :ruleId")
    suspend fun deleteRule(ruleId: String)

    @Query("DELETE FROM automation_rules WHERE targetAccountId = :accountId")
    suspend fun deleteRulesForAccount(accountId: String)
}

@Dao
interface AutomationHistoryDao {

    @Query("SELECT * FROM automation_execution_history ORDER BY executedAtEpochMs DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<AutomationExecutionRecordEntity>>

    @Query("SELECT * FROM automation_execution_history WHERE accountId = :accountId ORDER BY executedAtEpochMs DESC LIMIT :limit")
    fun getHistoryForAccount(accountId: String, limit: Int = 100): Flow<List<AutomationExecutionRecordEntity>>

    @Query("SELECT * FROM automation_execution_history WHERE automationId = :automationId ORDER BY executedAtEpochMs DESC LIMIT :limit")
    fun getHistoryForAutomation(automationId: String, limit: Int = 50): Flow<List<AutomationExecutionRecordEntity>>

    @Query(
        "SELECT COUNT(*) FROM automation_execution_history " +
            "WHERE accountId = :accountId AND sourceMessageId = :messageId AND triggerType = :triggerType AND status = :successStatus"
    )
    suspend fun countSuccessfulExecutions(
        accountId: String,
        messageId: String,
        triggerType: AutomationTriggerType,
        successStatus: AutomationExecutionStatus = AutomationExecutionStatus.SUCCEEDED,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(record: AutomationExecutionRecordEntity)

    @Query("DELETE FROM automation_execution_history WHERE accountId = :accountId")
    suspend fun deleteHistoryForAccount(accountId: String)

    @Query("DELETE FROM automation_execution_history WHERE executedAtEpochMs < :cutoffEpochMs")
    suspend fun pruneOldHistory(cutoffEpochMs: Long)
}
