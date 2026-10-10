package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.greninjaop.mailorganizer.core.automation.AutomationConfirmationPolicy
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionStatus
import com.greninjaop.mailorganizer.core.automation.AutomationLifecycleState
import com.greninjaop.mailorganizer.core.automation.AutomationScopeType
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType

/**
 * Stored automation rule definition (Phase 27).
 *
 * Scoped to an optional account or null (all accounts).
 */
@Entity(
    tableName = "automation_rules",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["targetAccountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("targetAccountId"),
        Index("state"),
        Index("triggerType"),
    ],
)
data class AutomationRuleRecord(
    @PrimaryKey
    val ruleId: String,
    val name: String,
    val description: String,
    val scopeType: AutomationScopeType,
    val targetAccountId: String?, // Null when scopeType == ALL_ACCOUNTS
    val state: AutomationLifecycleState,
    val triggerType: AutomationTriggerType,
    val triggerParam: String?,
    val conditionGroupJson: String,
    val actionsJson: String,
    val confirmationPolicy: AutomationConfirmationPolicy,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val lastRunEpochMs: Long?,
    val failureCount: Int,
    val version: Int = 1,
)

/**
 * Execution history record (Phase 27 §39, §67, §68).
 * Privacy-safe: never stores full email bodies.
 */
@Entity(
    tableName = "automation_execution_history",
    foreignKeys = [
        ForeignKey(
            entity = AutomationRuleRecord::class,
            parentColumns = ["ruleId"],
            childColumns = ["automationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("automationId"),
        Index("accountId"),
        Index("executedAtEpochMs"),
        Index(value = ["accountId", "sourceMessageId", "triggerType"], name = "index_automation_history_dedup"),
    ],
)
data class AutomationExecutionRecordEntity(
    @PrimaryKey
    val executionId: String,
    val automationId: String,
    val automationName: String,
    val accountId: String,
    val triggerType: AutomationTriggerType,
    val sourceMessageId: String?,
    val sourceThreadId: String?,
    val actionSummary: String,
    val status: AutomationExecutionStatus,
    val executedAtEpochMs: Long,
    val detailMessage: String?,
)
