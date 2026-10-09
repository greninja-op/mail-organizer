package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import kotlinx.coroutines.withContext

/**
 * User decisions on action cards (Phase 14, phase §24–§26, §31).
 *
 * - [markReviewed]: the user opened the card's details. Non-destructive.
 * - [dismiss]: the user dismissed the suggestion. Affects only the action
 *   row — never the source email, never Gmail (phase §26).
 * - [complete]: the user marks the action done themselves. This records
 *   *their* completion; it is not an execution receipt.
 * - [confirm]: the user confirms the suggested action after reviewing the
 *   confirmation UI. For internal suggestions (NONE) this records the
 *   confirmed intent. For external effects, the executor registry is
 *   consulted: in Phase 14 no executor exists, so the outcome is honestly
 *   [ConfirmationOutcome.ExternalNotConnected] — never a fake success
 *   (phase §32).
 *
 * Every method is total and failure-safe: failures are logged (never email
 * content) and reported as outcomes, never thrown into the UI.
 */
class ReviewActionUseCase(
    private val intelligence: IntelligenceRepository,
    private val executors: ActionExecutorRegistry,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
    /**
     * Integration Manager (Phase 17, phase §28): consulted for honest
     * capability information when no executor is registered. Optional so
     * existing call sites keep working; when absent the outcome carries
     * no detail and the UI falls back to its generic copy.
     */
    private val integrations: IntegrationManager? = null,
) {

    companion object {
        private const val TAG = "ReviewAction"
    }

    /** Marks a card reviewed (user opened its details). */
    suspend fun markReviewed(id: Long): Boolean = withContext(dispatchers.io) {
        transition(id, ActionStatus.REVIEWED, allowedFrom = setOf(ActionStatus.SUGGESTED))
    }

    /** Dismisses a suggestion — affects only the action row. */
    suspend fun dismiss(id: Long): Boolean = withContext(dispatchers.io) {
        transition(
            id, ActionStatus.DISMISSED,
            allowedFrom = setOf(
                ActionStatus.SUGGESTED,
                ActionStatus.REVIEWED,
                ActionStatus.CONFIRMED,
            ),
        )
    }

    /** Records the user's own completion of the action. */
    suspend fun complete(id: Long): Boolean = withContext(dispatchers.io) {
        transition(
            id, ActionStatus.COMPLETED,
            allowedFrom = setOf(
                ActionStatus.SUGGESTED,
                ActionStatus.REVIEWED,
                ActionStatus.CONFIRMED,
            ),
        )
    }

    /**
     * Confirms the suggested action after the confirmation UI.
     *
     * The confirmation UI (phase §31) must have shown: what will happen,
     * which email caused it, what data is used, which external service is
     * affected, reversibility, and that confirmation is required (phase
     * §11). This method enforces the outcome side.
     */
    suspend fun confirm(id: Long): ConfirmationOutcome = withContext(dispatchers.io) {
        val row = try {
            intelligence.getActionItem(id)
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Confirm lookup failed: ${t.javaClass.simpleName}")
            return@withContext ConfirmationOutcome.Failed("Couldn't load the action.")
        } ?: return@withContext ConfirmationOutcome.Failed("Action not found.")

        if (row.status != ActionStatus.SUGGESTED && row.status != ActionStatus.REVIEWED) {
            return@withContext ConfirmationOutcome.Failed(
                "This action is already ${row.status.name.lowercase()}.",
            )
        }

        // Internal suggestions: record the confirmed intent. There is
        // nothing external to execute — the "action" is reviewing the
        // email, which the UI handles by opening the thread.
        if (row.externalEffect == ExternalEffect.NONE) {
            intelligence.setActionItemStatus(id, ActionStatus.CONFIRMED)
            return@withContext ConfirmationOutcome.RecordedInternal(row)
        }

        // External proposal: consult the executor registry. Phase 14
        // registers none, so this is honestly "not connected". Phase 17:
        // the Integration Manager names the responsible integration and
        // its honest state for the message (phase §28).
        val executor = executors.find(row.actionType)
        if (executor == null) {
            intelligence.setActionItemStatus(id, ActionStatus.CONFIRMED)
            return@withContext ConfirmationOutcome.ExternalNotConnected(
                row,
                detail = integrations?.let { manager ->
                    val integrationId = manager.integrationForAction(row.actionType)
                        ?: return@let null
                    val snapshot = manager.snapshot(integrationId, row.accountId)
                        ?: return@let null
                    "${snapshot.displayName}: ${snapshot.statusReason}"
                },
            )
        }

        // A future executor exists: validate, then execute — only ever
        // reached after explicit user confirmation.
        return@withContext when (val v = executor.validate(toCandidate(row))) {
            is com.greninjaop.mailorganizer.MoResult.Failure ->
                ConfirmationOutcome.Failed("This action can't run right now.")
            is com.greninjaop.mailorganizer.MoResult.Success -> {
                intelligence.setActionItemStatus(id, ActionStatus.EXECUTING)
                when (val r = executor.execute(toCandidate(row))) {
                    is com.greninjaop.mailorganizer.MoResult.Success -> {
                        intelligence.setActionItemStatus(id, ActionStatus.COMPLETED)
                        ConfirmationOutcome.Executed(row, r.value)
                    }
                    is com.greninjaop.mailorganizer.MoResult.Failure -> {
                        intelligence.setActionItemStatus(id, ActionStatus.FAILED)
                        ConfirmationOutcome.Failed("The action failed. Nothing was changed.")
                    }
                }
            }
        }
    }

    private suspend fun transition(
        id: Long,
        to: ActionStatus,
        allowedFrom: Set<ActionStatus>,
    ): Boolean {
        val row = try {
            intelligence.getActionItem(id)
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Transition lookup failed: ${t.javaClass.simpleName}")
            return false
        } ?: return false
        if (row.status !in allowedFrom) return false
        return try {
            intelligence.setActionItemStatus(id, to)
            true
        } catch (t: Throwable) {
            MoLogger.e(TAG, "Transition failed: ${t.javaClass.simpleName}")
            false
        }
    }

    private fun toCandidate(row: ActionItemRecord): com.greninjaop.mailorganizer.core.actions.ActionCandidate {
        val payload = ActionPayloadJson.decode(row.payloadJson)
        return com.greninjaop.mailorganizer.core.actions.ActionCandidate(
            id = "${row.messageId}|${row.actionType.name}|${payload?.targetKey.orEmpty()}",
            accountId = row.accountId,
            messageId = row.messageId,
            threadId = row.threadId,
            actionType = row.actionType,
            title = row.title,
            description = row.description.orEmpty(),
            urgency = row.urgency,
            source = row.source,
            confidence = when {
                row.confidence >= 0.9f -> com.greninjaop.mailorganizer.core.actions.ActionConfidence.HIGH
                row.confidence >= 0.6f -> com.greninjaop.mailorganizer.core.actions.ActionConfidence.MEDIUM
                row.confidence > 0f -> com.greninjaop.mailorganizer.core.actions.ActionConfidence.LOW
                else -> com.greninjaop.mailorganizer.core.actions.ActionConfidence.UNKNOWN
            },
            explanation = row.explanation.orEmpty(),
            signals = emptyList(),
            externalEffect = row.externalEffect,
            missingInfo = payload?.missingInfo.orEmpty(),
            dueDateEpochMs = row.dueDateEpochMs,
            targetKey = payload?.targetKey.orEmpty(),
            version = row.version,
        )
    }
}

/**
 * Outcome of confirming an action (phase §31–§32). The UI maps each case
 * to honest copy — "created" is only ever shown for [Executed].
 */
sealed interface ConfirmationOutcome {
    /** Internal suggestion confirmed; the UI opens the source email. */
    data class RecordedInternal(val item: ActionItemRecord) : ConfirmationOutcome

    /**
     * External effect proposed but no executor is connected (Phase 14:
     * always, for CALENDAR/TASKS/GMAIL_WRITE). The confirmed intent is
     * recorded locally; nothing external happened. [detail] optionally
     * carries the Integration Manager's honest per-integration reason
     * (Phase 17, phase §28); null keeps the UI's generic copy.
     */
    data class ExternalNotConnected(
        val item: ActionItemRecord,
        val detail: String? = null,
    ) : ConfirmationOutcome

    /** A real executor ran and returned a receipt (future phases). */
    data class Executed(val item: ActionItemRecord, val receipt: ExecutionReceipt) : ConfirmationOutcome

    /** The confirmation could not be recorded. Nothing happened. */
    data class Failed(val message: String) : ConfirmationOutcome
}
