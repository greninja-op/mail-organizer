package com.greninjaop.mailorganizer.data.integrations

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.PermissionDescription

/**
 * Shared base for integrations whose build phase is user-deferred
 * (Phase 17).
 *
 * These adapters are honest placeholders with real structure — not fake
 * integrations:
 * - [snapshot] always reports [IntegrationStatus.UNAVAILABLE] with an
 *   explicit reason naming the deferred phase. They never report
 *   CONNECTED, never claim capabilities are usable.
 * - [connect] returns a failure explaining the integration isn't built
 *   yet — never a fake success, never a fake OAuth flow.
 * - [disconnect] is a safe idempotent no-op: there is nothing to revoke.
 * - [declaredCapabilities] / [requiredPermissions] describe what the
 *   integration *will* offer, so the manager, the Action Engine, and the
 *   UI can reason about it without pretending it exists.
 */
abstract class DeferredIntegrationAdapter(
    override val id: IntegrationId,
    override val displayName: String,
    private val arrivesWithPhase: String,
    override val declaredCapabilities: Set<IntegrationCapability>,
    override val requiredPermissions: List<PermissionDescription>,
    private val clock: () -> Long = System::currentTimeMillis,
) : IntegrationAdapter {

    override val provider: String = "Google"

    private fun unavailableReason(): String =
        "$displayName isn't available yet — the integration arrives with " +
            "$arrivesWithPhase. Nothing is connected."

    override suspend fun snapshot(accountId: String?): IntegrationSnapshot =
        IntegrationSnapshot(
            id = id,
            displayName = displayName,
            provider = provider,
            accountId = accountId,
            accountEmail = null,
            status = IntegrationStatus.UNAVAILABLE,
            statusReason = unavailableReason(),
            declaredCapabilities = declaredCapabilities,
            requiredPermissions = requiredPermissions,
            lastCheckedEpochMs = clock(),
        )

    override suspend fun connect(): MoResult<Unit> =
        MoResult.Failure(
            MoError.Integration(
                integration = id.value,
                message = unavailableReason(),
            ),
        )

    override suspend fun disconnect(accountId: String?): MoResult<Unit> =
        MoResult.Success(Unit)

    override suspend fun refresh(accountId: String?): IntegrationSnapshot =
        snapshot(accountId)
}

/**
 * Google Calendar adapter — DEFERRED to Phase 15 (user decision).
 *
 * Honest UNAVAILABLE state until Phase 15 builds the real integration.
 * Declared capabilities and least-privilege permissions are documented
 * here so the Integration Manager and Action Engine can plan around them.
 */
class CalendarIntegrationAdapter(
    clock: () -> Long = System::currentTimeMillis,
) : DeferredIntegrationAdapter(
    id = IntegrationId.CALENDAR,
    displayName = "Google Calendar",
    arrivesWithPhase = "Phase 15",
    declaredCapabilities = setOf(
        IntegrationCapability.READ_CALENDAR_METADATA,
        IntegrationCapability.CREATE_EVENT,
    ),
    requiredPermissions = listOf(
        PermissionDescription(
            scope = "calendar.events",
            purpose = "Add events you explicitly choose to your calendar. " +
                "Mail Organizer never creates events on its own.",
            required = true,
        ),
    ),
    clock = clock,
)

/**
 * Google Tasks adapter — DEFERRED to Phase 16 (user decision).
 *
 * Honest UNAVAILABLE state until Phase 16 builds the real integration.
 */
class TasksIntegrationAdapter(
    clock: () -> Long = System::currentTimeMillis,
) : DeferredIntegrationAdapter(
    id = IntegrationId.TASKS,
    displayName = "Google Tasks",
    arrivesWithPhase = "Phase 16",
    declaredCapabilities = setOf(
        IntegrationCapability.READ_TASK_LISTS,
        IntegrationCapability.CREATE_TASK,
    ),
    requiredPermissions = listOf(
        PermissionDescription(
            scope = "tasks",
            purpose = "Create tasks you explicitly choose from your mail. " +
                "Mail Organizer never creates tasks on its own.",
            required = true,
        ),
    ),
    clock = clock,
)
