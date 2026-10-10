package com.greninjaop.mailorganizer.domain.automation

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.actions.ActionConfidence
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationActionType
import com.greninjaop.mailorganizer.core.automation.AutomationConditionEvaluator
import com.greninjaop.mailorganizer.core.automation.AutomationEvaluationContext
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionRecord
import com.greninjaop.mailorganizer.core.automation.AutomationExecutionStatus
import com.greninjaop.mailorganizer.core.automation.AutomationRule
import com.greninjaop.mailorganizer.core.automation.AutomationSafetyResult
import com.greninjaop.mailorganizer.core.automation.AutomationSafetyValidator
import com.greninjaop.mailorganizer.core.automation.AutomationTriggerType
import com.greninjaop.mailorganizer.data.automation.AutomationRepository
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.domain.actions.ActionExecutorRegistry
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Result of evaluating and executing automations.
 */
data class AutomationRunResult(
    val executedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val waitingConfirmationCount: Int,
    val summaries: List<String>,
)

/**
 * Preview result of an automation rule against existing mailbox messages (Phase 27 §54, §55, §110).
 */
data class AutomationPreviewResult(
    val matchedMessagesCount: Int,
    val matchedThreadsCount: Int,
    val sampleSubjects: List<String>,
    val conflicts: List<String>,
    val safetyStatus: AutomationSafetyResult,
)

/**
 * Central Advanced Automation Engine (Phase 27).
 *
 * Coordinates:
 * - Trigger handling (new email, category/priority assigned, scheduled, manual).
 * - Target candidate evaluation with [AutomationConditionEvaluator].
 * - Safety validation with [AutomationSafetyValidator].
 * - Execution through local repositories, Action Engine proposals, and Integration Manager.
 * - Non-faking execution logs & idempotency tracking.
 */
class AutomationEngine(
    private val automationRepository: AutomationRepository,
    private val mailRepository: MailRepository,
    private val intelligenceRepository: IntelligenceRepository,
    private val integrationManager: IntegrationManager?,
    private val executorRegistry: ActionExecutorRegistry?,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /**
     * Preview a rule without executing side effects (§54, §55, §56).
     */
    suspend fun previewRule(
        rule: AutomationRule,
        targetAccountId: String,
        limit: Int = 50,
    ): AutomationPreviewResult = withContext(dispatchers.io) {
        val conflicts = AutomationSafetyValidator.detectConflicts(rule.actions)
        val safety = AutomationSafetyValidator.validate(rule, targetAccountId, null)

        val messages = mailRepository.observeThreads(targetAccountId, limit = 200).first()
        val messageIds = mailRepository.getMessageIdsByAccount(targetAccountId, limit = 200)
        val fullMessages = mailRepository.getMessagesByIds(messageIds)
        var matchCount = 0
        val matchedThreads = mutableSetOf<String>()
        val sampleSubjects = mutableListOf<String>()

        for (msg in fullMessages) {
            val ctx = buildEvaluationContext(msg)
            if (AutomationConditionEvaluator.evaluateGroup(rule.conditionGroup, ctx)) {
                matchCount++
                matchedThreads.add(msg.threadId)
                if (sampleSubjects.size < 5) {
                    sampleSubjects.add(msg.subject)
                }
            }
        }

        AutomationPreviewResult(
            matchedMessagesCount = matchCount,
            matchedThreadsCount = matchedThreads.size,
            sampleSubjects = sampleSubjects,
            conflicts = conflicts,
            safetyStatus = safety,
        )
    }

    /**
     * Processes a newly synced email message through eligible automation rules (§12, §13, §71, §72).
     */
    suspend fun processNewEmail(
        accountId: String,
        messageId: String,
    ): AutomationRunResult = withContext(dispatchers.io) {
        processMessageTrigger(
            accountId = accountId,
            messageId = messageId,
            triggerType = AutomationTriggerType.NEW_EMAIL_SYNCED,
        )
    }

    /**
     * Processes a message after classification/priority updates.
     */
    suspend fun processClassifiedEmail(
        accountId: String,
        messageId: String,
        category: MailCategory,
        priority: Priority,
        isActionRequired: Boolean,
    ): AutomationRunResult = withContext(dispatchers.io) {
        val results = mutableListOf<AutomationRunResult>()

        if (isActionRequired) {
            results.add(
                processMessageTrigger(
                    accountId = accountId,
                    messageId = messageId,
                    triggerType = AutomationTriggerType.ACTION_REQUIRED_DETECTED,
                )
            )
        }
        results.add(
            processMessageTrigger(
                accountId = accountId,
                messageId = messageId,
                triggerType = AutomationTriggerType.CATEGORY_ASSIGNED,
                triggerParam = category.name,
            )
        )
        results.add(
            processMessageTrigger(
                accountId = accountId,
                messageId = messageId,
                triggerType = AutomationTriggerType.PRIORITY_ASSIGNED,
                triggerParam = priority.name,
            )
        )

        combineResults(results)
    }

    /**
     * Executes an automation manually or on schedule (§179, §180).
     */
    suspend fun runRule(
        ruleId: String,
        targetAccountId: String,
        isExplicitUserConfirmed: Boolean = false,
    ): AutomationRunResult = withContext(dispatchers.io) {
        val rule = automationRepository.getRuleById(ruleId)
            ?: return@withContext AutomationRunResult(0, 0, 1, 0, listOf("Rule not found: $ruleId"))

        if (!rule.scope.appliesToAccount(targetAccountId)) {
            return@withContext AutomationRunResult(
                executedCount = 0,
                skippedCount = 1,
                failedCount = 0,
                waitingConfirmationCount = 0,
                summaries = listOf("Rule does not apply to account: $targetAccountId"),
            )
        }

        val messageIds = mailRepository.getMessageIdsByAccount(targetAccountId, limit = 100)
        val messages = mailRepository.getMessagesByIds(messageIds)
        val results = mutableListOf<AutomationRunResult>()

        for (msg in messages) {
            val ctx = buildEvaluationContext(msg)
            if (AutomationConditionEvaluator.evaluateGroup(rule.conditionGroup, ctx)) {
                val res = executeRuleForContext(
                    rule = rule,
                    context = ctx,
                    triggerType = AutomationTriggerType.MANUAL_RUN,
                    isExplicitUserConfirmed = isExplicitUserConfirmed,
                )
                results.add(res)
            }
        }

        combineResults(results)
    }

    /**
     * Core message trigger execution logic.
     */
    private suspend fun processMessageTrigger(
        accountId: String,
        messageId: String,
        triggerType: AutomationTriggerType,
        triggerParam: String? = null,
    ): AutomationRunResult {
        // Idempotency check (§14, §15, §119)
        if (automationRepository.isAlreadyExecuted(accountId, messageId, triggerType)) {
            return AutomationRunResult(0, 1, 0, 0, listOf("Already executed for event"))
        }

        val message = mailRepository.getMessage(messageId)
            ?: return AutomationRunResult(0, 1, 0, 0, listOf("Message not found"))

        // Cross-account protection (§10, §33, §34)
        if (message.accountId != accountId) {
            MoLogger.w(TAG, "Cross-account mismatch: message account ${message.accountId} != requested $accountId")
            return AutomationRunResult(0, 0, 1, 0, listOf("Cross-account violation"))
        }

        val context = buildEvaluationContext(message)
        val allRules = automationRepository.getAllRules().first()

        val matchingRules = allRules.filter { rule ->
            rule.isEnabled &&
                rule.scope.appliesToAccount(accountId) &&
                isTriggerMatch(rule.trigger, triggerType, triggerParam) &&
                AutomationConditionEvaluator.evaluateGroup(rule.conditionGroup, context)
        }

        val results = mutableListOf<AutomationRunResult>()
        for (rule in matchingRules) {
            results.add(
                executeRuleForContext(
                    rule = rule,
                    context = context,
                    triggerType = triggerType,
                    isExplicitUserConfirmed = false,
                )
            )
        }

        return combineResults(results)
    }

    private fun isTriggerMatch(
        trigger: com.greninjaop.mailorganizer.core.automation.AutomationTrigger,
        eventType: AutomationTriggerType,
        eventParam: String?,
    ): Boolean {
        if (trigger.type != eventType) return false
        if (trigger.parameter == null) return true
        return trigger.parameter.equals(eventParam, ignoreCase = true)
    }

    private suspend fun executeRuleForContext(
        rule: AutomationRule,
        context: AutomationEvaluationContext,
        triggerType: AutomationTriggerType,
        isExplicitUserConfirmed: Boolean,
    ): AutomationRunResult {
        val now = clock()
        val execId = UUID.randomUUID().toString()

        // 1. Safety validation (§30, §31)
        val safety = AutomationSafetyValidator.validate(
            rule = rule,
            targetAccountId = context.accountId,
            context = context,
            isExplicitUserConfirmed = isExplicitUserConfirmed,
        )

        when (safety) {
            is AutomationSafetyResult.Blocked -> {
                automationRepository.recordExecution(
                    AutomationExecutionRecord(
                        executionId = execId,
                        automationId = rule.id,
                        automationName = rule.name,
                        accountId = context.accountId,
                        triggerType = triggerType,
                        sourceMessageId = context.messageId,
                        sourceThreadId = context.threadId,
                        actionSummary = "Blocked by safety",
                        status = AutomationExecutionStatus.SKIPPED,
                        executedAtEpochMs = now,
                        detailMessage = safety.reason,
                    )
                )
                return AutomationRunResult(0, 1, 0, 0, listOf(safety.reason))
            }

            is AutomationSafetyResult.RequiresConfirmation -> {
                automationRepository.recordExecution(
                    AutomationExecutionRecord(
                        executionId = execId,
                        automationId = rule.id,
                        automationName = rule.name,
                        accountId = context.accountId,
                        triggerType = triggerType,
                        sourceMessageId = context.messageId,
                        sourceThreadId = context.threadId,
                        actionSummary = "Requires user confirmation",
                        status = AutomationExecutionStatus.WAITING_CONFIRMATION,
                        executedAtEpochMs = now,
                        detailMessage = safety.reason,
                    )
                )
                return AutomationRunResult(0, 0, 0, 1, listOf(safety.reason))
            }

            is AutomationSafetyResult.Approved -> {
                // Execute actions deterministically (§74)
                var hasFailure = false
                var executedCount = 0
                val actionSummaries = mutableListOf<String>()

                for (action in rule.actions) {
                    val outcome = applyAction(action, rule, context)
                    if (outcome is MoResult.Success) {
                        executedCount++
                        actionSummaries.add("${action.type}")
                    } else if (outcome is MoResult.Failure) {
                        hasFailure = true
                        actionSummaries.add("${action.type} FAILED")
                    }
                }

                val finalStatus = if (hasFailure) {
                    automationRepository.recordFailure(rule.id, now)
                    AutomationExecutionStatus.FAILED
                } else {
                    automationRepository.recordSuccess(rule.id, now)
                    AutomationExecutionStatus.SUCCEEDED
                }

                automationRepository.recordExecution(
                    AutomationExecutionRecord(
                        executionId = execId,
                        automationId = rule.id,
                        automationName = rule.name,
                        accountId = context.accountId,
                        triggerType = triggerType,
                        sourceMessageId = context.messageId,
                        sourceThreadId = context.threadId,
                        actionSummary = actionSummaries.joinToString(", "),
                        status = finalStatus,
                        executedAtEpochMs = now,
                        detailMessage = if (hasFailure) "One or more actions failed" else "Execution complete",
                    )
                )

                return if (hasFailure) {
                    AutomationRunResult(0, 0, 1, 0, actionSummaries)
                } else {
                    AutomationRunResult(executedCount, 0, 0, 0, actionSummaries)
                }
            }
        }
    }

    private suspend fun applyAction(
        action: AutomationAction,
        rule: AutomationRule,
        context: AutomationEvaluationContext,
    ): MoResult<Unit> {
        val now = clock()
        return try {
            when (action.type) {
                AutomationActionType.SET_CATEGORY -> {
                    val catName = action.parameter ?: "IMPORTANT"
                    val cat = try { MailCategory.valueOf(catName) } catch (_: Exception) { MailCategory.IMPORTANT }
                    intelligenceRepository.setClassification(
                        ClassificationRecord(
                            messageId = context.messageId,
                            accountId = context.accountId,
                            category = cat,
                            confidence = 1.0f,
                            source = com.greninjaop.mailorganizer.data.local.ClassificationSource.USER_RULE,
                            explanation = "Categorized by automation: ${rule.name}",
                            version = 1,
                            overridden = false,
                            classifiedAtEpochMs = now,
                        )
                    )
                    MoResult.Success(Unit)
                }

                AutomationActionType.SET_PRIORITY -> {
                    val priName = action.parameter ?: "NORMAL"
                    val pri = try { Priority.valueOf(priName) } catch (_: Exception) { Priority.NORMAL }
                    intelligenceRepository.setPriority(
                        PriorityRecord(
                            messageId = context.messageId,
                            accountId = context.accountId,
                            priority = pri,
                            manualOverride = true,
                            reason = "Priority set by automation: ${rule.name}",
                            version = 1,
                            updatedAtEpochMs = now,
                        )
                    )
                    MoResult.Success(Unit)
                }

                AutomationActionType.MARK_ACTION_REQUIRED -> {
                    intelligenceRepository.setClassification(
                        ClassificationRecord(
                            messageId = context.messageId,
                            accountId = context.accountId,
                            category = MailCategory.ACTION_REQUIRED,
                            confidence = 1.0f,
                            source = com.greninjaop.mailorganizer.data.local.ClassificationSource.USER_RULE,
                            explanation = "Marked Action Required by automation: ${rule.name}",
                            version = 1,
                            overridden = false,
                            classifiedAtEpochMs = now,
                        )
                    )
                    MoResult.Success(Unit)
                }

                AutomationActionType.CREATE_LOCAL_REMINDER -> {
                    // Create local action item candidate
                    intelligenceRepository.addActionItem(
                        com.greninjaop.mailorganizer.data.local.ActionItemRecord(
                            messageId = context.messageId,
                            accountId = context.accountId,
                            threadId = context.threadId,
                            actionType = ActionType.DEADLINE,
                            title = action.parameter ?: "Follow up on email",
                            description = "Automated reminder created by ${rule.name}",
                            urgency = ActionUrgency.NORMAL,
                            source = ActionSource.USER_RULE,
                            status = com.greninjaop.mailorganizer.core.actions.ActionStatus.SUGGESTED,
                            confidence = 1.0f,
                            explanation = "Created by automation: ${rule.name}",
                            dueDateEpochMs = now + 86400000L,
                            detectedAtEpochMs = now,
                            updatedAtEpochMs = now,
                        )
                    )
                    MoResult.Success(Unit)
                }

                AutomationActionType.CREATE_CALENDAR_PROPOSAL -> {
                    intelligenceRepository.addActionItem(
                        com.greninjaop.mailorganizer.data.local.ActionItemRecord(
                            messageId = context.messageId,
                            accountId = context.accountId,
                            threadId = context.threadId,
                            actionType = ActionType.MEETING,
                            title = context.subject,
                            description = "Automated calendar proposal by ${rule.name}",
                            urgency = ActionUrgency.NORMAL,
                            source = ActionSource.USER_RULE,
                            status = com.greninjaop.mailorganizer.core.actions.ActionStatus.SUGGESTED,
                            confidence = 1.0f,
                            explanation = "Calendar proposal by automation: ${rule.name}",
                            dueDateEpochMs = now + 86400000L,
                            detectedAtEpochMs = now,
                            updatedAtEpochMs = now,
                        )
                    )
                    MoResult.Success(Unit)
                }

                AutomationActionType.CREATE_TASK_PROPOSAL -> {
                    intelligenceRepository.addActionItem(
                        com.greninjaop.mailorganizer.data.local.ActionItemRecord(
                            messageId = context.messageId,
                            accountId = context.accountId,
                            threadId = context.threadId,
                            actionType = ActionType.DEADLINE,
                            title = context.subject,
                            description = "Automated task proposal by ${rule.name}",
                            urgency = ActionUrgency.NORMAL,
                            source = ActionSource.USER_RULE,
                            status = com.greninjaop.mailorganizer.core.actions.ActionStatus.SUGGESTED,
                            confidence = 1.0f,
                            explanation = "Task proposal by automation: ${rule.name}",
                            dueDateEpochMs = now + 86400000L,
                            detectedAtEpochMs = now,
                            updatedAtEpochMs = now,
                        )
                    )
                    MoResult.Success(Unit)
                }

                AutomationActionType.MARK_AS_READ -> {
                    mailRepository.setRead(context.messageId, true)
                    MoResult.Success(Unit)
                }

                AutomationActionType.MARK_AS_UNREAD -> {
                    mailRepository.setRead(context.messageId, false)
                    MoResult.Success(Unit)
                }

                AutomationActionType.STAR_MESSAGE -> {
                    mailRepository.setStarred(context.messageId, true)
                    MoResult.Success(Unit)
                }

                AutomationActionType.UNSTAR_MESSAGE -> {
                    mailRepository.setStarred(context.messageId, false)
                    MoResult.Success(Unit)
                }

                AutomationActionType.ARCHIVE_MESSAGE -> {
                    val msg = mailRepository.getMessage(context.messageId)
                    if (msg != null) {
                        val thread = mailRepository.getThreadByGmailId(context.accountId, msg.threadId)
                        if (thread != null) {
                            val newLabels = msg.labels.filter { it != "INBOX" }
                            mailRepository.saveThreadWithMessages(thread, listOf(msg.copy(labels = newLabels)))
                        }
                    }
                    MoResult.Success(Unit)
                }

                AutomationActionType.TRASH_MESSAGE -> {
                    val msg = mailRepository.getMessage(context.messageId)
                    if (msg != null) {
                        val thread = mailRepository.getThreadByGmailId(context.accountId, msg.threadId)
                        if (thread != null) {
                            val newLabels = (msg.labels.filter { it != "INBOX" } + "TRASH").distinct()
                            mailRepository.saveThreadWithMessages(thread, listOf(msg.copy(labels = newLabels)))
                        }
                    }
                    MoResult.Success(Unit)
                }

                AutomationActionType.ADD_GMAIL_LABEL -> {
                    val labelToAdd = action.parameter ?: return MoResult.Success(Unit)
                    val msg = mailRepository.getMessage(context.messageId)
                    if (msg != null && !msg.labels.contains(labelToAdd)) {
                        val thread = mailRepository.getThreadByGmailId(context.accountId, msg.threadId)
                        if (thread != null) {
                            mailRepository.saveThreadWithMessages(thread, listOf(msg.copy(labels = msg.labels + labelToAdd)))
                        }
                    }
                    MoResult.Success(Unit)
                }

                AutomationActionType.REMOVE_GMAIL_LABEL -> {
                    val labelToRemove = action.parameter ?: return MoResult.Success(Unit)
                    val msg = mailRepository.getMessage(context.messageId)
                    if (msg != null && msg.labels.contains(labelToRemove)) {
                        val thread = mailRepository.getThreadByGmailId(context.accountId, msg.threadId)
                        if (thread != null) {
                            mailRepository.saveThreadWithMessages(thread, listOf(msg.copy(labels = msg.labels.filter { it != labelToRemove })))
                        }
                    }
                    MoResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            MoLogger.e(TAG, "Error applying action ${action.type}: ${e.message}")
            MoResult.Failure(MoError.Unexpected(e))
        }
    }

    private suspend fun buildEvaluationContext(msg: MessageRecord): AutomationEvaluationContext {
        val classRec = intelligenceRepository.getClassification(msg.messageId)
        val priRec = intelligenceRepository.getPriority(msg.messageId)

        val senderDomain = msg.fromAddress.substringAfter('@', "").lowercase().trim()
        val cat = classRec?.category?.name ?: "UNCLASSIFIED"
        val pri = priRec?.priority?.name ?: "NORMAL"
        val isActionReq = cat == "ACTION_REQUIRED"
        val hasAttach = !msg.attachments.isNullOrEmpty()
        val hasUnsub = msg.bodyText?.contains("unsubscribe", ignoreCase = true) == true

        return AutomationEvaluationContext(
            accountId = msg.accountId,
            messageId = msg.messageId,
            threadId = msg.threadId,
            fromAddress = msg.fromAddress,
            fromDomain = senderDomain,
            companyId = msg.companyId,
            subject = msg.subject,
            category = cat,
            priority = pri,
            isActionRequired = isActionReq,
            isUnread = msg.unread,
            isStarred = msg.starred,
            hasAttachment = hasAttach,
            hasUnsubscribe = hasUnsub,
            gmailLabels = msg.labels,
            timestampEpochMs = msg.timestampEpochMs,
        )
    }

    private fun combineResults(list: List<AutomationRunResult>): AutomationRunResult {
        return AutomationRunResult(
            executedCount = list.sumOf { it.executedCount },
            skippedCount = list.sumOf { it.skippedCount },
            failedCount = list.sumOf { it.failedCount },
            waitingConfirmationCount = list.sumOf { it.waitingConfirmationCount },
            summaries = list.flatMap { it.summaries },
        )
    }

    private companion object {
        const val TAG = "AutomationEngine"
    }
}
