package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.data.local.ActionType

/**
 * External action executor abstraction (Phase 14, phase §31–§33).
 *
 * The engine proposes; the user confirms; an executor performs. In
 * Phase 14 **no executors are registered** — [ActionExecutorRegistry] is
 * intentionally empty — so confirming an external action records the
 * intent locally and reports "not connected" honestly. Future phases
 * (15/16/22) plug real executors in behind this interface without
 * touching the engine.
 *
 * Contract:
 * - [supports] is pure and total.
 * - [validate] checks the candidate is executable *right now* (required
 *   fields, connected integration) without side effects.
 * - [execute] performs the external side effect and returns a receipt, or
 *   a typed failure. It must never be called without explicit user
 *   confirmation (enforced by the use case, not the interface).
 */
interface ActionExecutor {

    /** Whether this executor handles [actionType]. Pure, total. */
    fun supports(actionType: ActionType): Boolean

    /**
     * Pre-execution validation without side effects: is the candidate
     * well-formed and is the integration connected?
     */
    fun validate(candidate: ActionCandidate): MoResult<Unit>

    /** Performs the external side effect. Only after user confirmation. */
    suspend fun execute(candidate: ActionCandidate): MoResult<ExecutionReceipt>
}

/** Proof that an external action really happened (never faked). */
data class ExecutionReceipt(
    val executorName: String,
    val externalId: String?,
    val executedAtEpochMs: Long,
    val summary: String,
)

/**
 * Executor registry (Phase 14).
 *
 * Deliberately empty: no external integrations exist yet. [find] returns
 * null for every action type, which the use case turns into an honest
 * "integration not connected" outcome — never a fake success (phase §32).
 */
class ActionExecutorRegistry(
    private val executors: List<ActionExecutor> = emptyList(),
) {
    fun find(actionType: ActionType): ActionExecutor? =
        executors.firstOrNull { it.supports(actionType) }
}
