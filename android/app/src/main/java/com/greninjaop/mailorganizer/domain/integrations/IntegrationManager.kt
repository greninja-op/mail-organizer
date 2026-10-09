package com.greninjaop.mailorganizer.domain.integrations

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationError
import com.greninjaop.mailorganizer.core.integrations.IntegrationEvent
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.toIntegrationError
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.repository.IntegrationStateRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Integration Manager (Phase 17, phase §5).
 *
 * Coordinates the app's integrations (Gmail, Calendar, Tasks) without
 * absorbing their API-specific logic. Responsibilities:
 * - registry of [IntegrationAdapter]s (phase §5)
 * - account-scoped snapshots with strict account isolation (phase §15):
 *   state always carries account identity; one account's state never
 *   leaks into another's
 * - capability queries for the Action Engine (phase §28): which
 *   integration, if any, can satisfy an action — without the engine
 *   touching credentials or API clients
 * - executor routing (phase §30): action type → responsible integration
 * - safe account-removal cleanup boundary (phase §36): only the removed
 *   account's integration metadata is cleared
 * - lightweight internal events (phase §33) via SharedFlow
 *
 * The manager never performs external side effects and never bypasses
 * Phase 14's confirmation boundary (phase §29): it answers questions
 * about integrations; execution stays with the Action Engine's executors.
 *
 * All public functions are total: adapter failures are caught and
 * reported as [IntegrationStatus.ERROR] snapshots, never thrown.
 */
class IntegrationManager(
    private val adapters: List<IntegrationAdapter>,
    private val states: IntegrationStateRepository,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val _events =
        MutableSharedFlow<IntegrationEvent>(extraBufferCapacity = 32)
    /** Lightweight internal events; no heavyweight event bus (phase §33). */
    val events: SharedFlow<IntegrationEvent> = _events.asSharedFlow()

    fun adapter(id: IntegrationId): IntegrationAdapter? =
        adapters.firstOrNull { it.id == id }

    /**
     * Snapshots for every registered integration, scoped to [accountId].
     * [accountId] null = no account in scope. Order is stable: Gmail,
     * Calendar, Tasks, then any others in registration order.
     */
    suspend fun snapshots(accountId: String?): List<IntegrationSnapshot> =
        withContext(dispatchers.io) {
            orderedAdapters().map { adapter ->
                runCatching { adapter.snapshot(accountId) }
                    .getOrElse { error ->
                        errorSnapshot(adapter, accountId, error.toIntegrationError())
                    }
            }
        }

    /** Snapshot for one integration, or null when the id is unknown. */
    suspend fun snapshot(id: IntegrationId, accountId: String?): IntegrationSnapshot? =
        withContext(dispatchers.io) {
            val adapter = adapter(id) ?: return@withContext null
            runCatching { adapter.snapshot(accountId) }
                .getOrElse { error -> errorSnapshot(adapter, accountId, error.toIntegrationError()) }
        }

    /**
     * Capability query for the Action Engine (phase §28): is [capability]
     * actually usable on [id] for [accountId] right now? Usable means the
     * integration reports CONNECTED — a declaration alone is never enough.
     */
    suspend fun isCapable(
        id: IntegrationId,
        capability: IntegrationCapability,
        accountId: String?,
    ): Boolean = snapshot(id, accountId)?.isCapable(capability) == true

    /**
     * Executor routing (phase §30): which integration is responsible for
     * an action type, or null when no integration claims it.
     *
     * Mapping is conservative and documented: only unambiguous
     * assignments are made. MEETING → Calendar (events have times and
     * locations); REMINDER → Tasks. DEADLINE is deliberately unmapped —
     * it could be a calendar event or a task, and the manager does not
     * guess. Unmapped types keep Phase 14's honest "not connected"
     * behavior.
     */
    fun integrationForAction(actionType: ActionType): IntegrationId? = when (actionType) {
        ActionType.MEETING -> IntegrationId.CALENDAR
        ActionType.REMINDER -> IntegrationId.TASKS
        else -> null
    }

    /**
     * User-initiated connect. Emits [IntegrationEvent.Connected] only on
     * real success. Failures are returned as [MoResult.Failure] — never
     * a fake success — and emitted as [IntegrationEvent.IntegrationFailed].
     */
    suspend fun connect(id: IntegrationId, accountId: String?): MoResult<Unit> =
        withContext(dispatchers.io) {
            val adapter = adapter(id)
                ?: return@withContext MoResult.Failure(
                    com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                        "Unknown integration: ${id.value}",
                    ),
                )
            when (val result = runCatching { adapter.connect() }.getOrElse { error ->
                MoResult.Failure(
                    com.greninjaop.mailorganizer.MoError.Unexpected(error),
                )
            }) {
                is MoResult.Success -> {
                    if (accountId != null) {
                        _events.tryEmit(IntegrationEvent.Connected(id, accountId))
                    } else {
                        _events.tryEmit(
                            IntegrationEvent.StatusChanged(id, null, IntegrationStatus.CONNECTED),
                        )
                    }
                    persistSnapshot(id, accountId)
                    result
                }
                is MoResult.Failure -> {
                    _events.tryEmit(
                        IntegrationEvent.IntegrationFailed(
                            id,
                            com.greninjaop.mailorganizer.core.integrations.IntegrationError.Unknown(
                                result.error.toString(),
                            ),
                        ),
                    )
                    result
                }
            }
        }

    /**
     * User-initiated disconnect (phase §18). Idempotent and safe: clears
     * integration metadata only — never mail, classifications, rules,
     * action history, or other integrations' data.
     */
    suspend fun disconnect(id: IntegrationId, accountId: String?): MoResult<Unit> =
        withContext(dispatchers.io) {
            val adapter = adapter(id)
                ?: return@withContext MoResult.Failure(
                    com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                        "Unknown integration: ${id.value}",
                    ),
                )
            val result = runCatching { adapter.disconnect(accountId) }
                .getOrElse { error ->
                    MoResult.Failure(com.greninjaop.mailorganizer.MoError.Unexpected(error))
                }
            if (result is MoResult.Success) {
                states.clear(id.value, accountId)
                _events.tryEmit(IntegrationEvent.Disconnected(id, accountId))
            }
            result
        }

    /**
     * Lightweight health check (phase §25): re-validates every integration
     * without expensive or continuous network use. Total: never throws.
     */
    suspend fun refreshAll(accountId: String?): List<IntegrationSnapshot> =
        withContext(dispatchers.io) {
            orderedAdapters().map { adapter ->
                runCatching { adapter.refresh(accountId) }
                    .getOrElse { error ->
                        errorSnapshot(adapter, accountId, error.toIntegrationError())
                    }
                    .also { snapshot -> persistSnapshot(adapter.id, accountId, snapshot) }
            }
        }

    /**
     * Account-removal cleanup boundary (phase §36): clears ONLY the removed
     * account's integration metadata. Never touches another account's
     * integrations or any unrelated local data.
     */
    suspend fun handleAccountRemoved(accountId: String) =
        withContext(dispatchers.io) {
            states.clearForAccount(accountId)
            _events.tryEmit(IntegrationEvent.AccountRemoved(accountId))
        }

    private fun orderedAdapters(): List<IntegrationAdapter> {
        val priority = listOf(IntegrationId.GMAIL, IntegrationId.CALENDAR, IntegrationId.TASKS)
        return adapters.sortedBy { adapter ->
            priority.indexOf(adapter.id).let { if (it < 0) Int.MAX_VALUE else it }
        }
    }

    private fun errorSnapshot(
        adapter: IntegrationAdapter,
        accountId: String?,
        error: IntegrationError,
    ): IntegrationSnapshot = IntegrationSnapshot(
        id = adapter.id,
        displayName = adapter.displayName,
        provider = adapter.provider,
        accountId = accountId,
        accountEmail = null,
        status = IntegrationStatus.ERROR,
        statusReason = "Couldn't check ${adapter.displayName}: ${error.describe()}",
        declaredCapabilities = adapter.declaredCapabilities,
        requiredPermissions = adapter.requiredPermissions,
        lastCheckedEpochMs = clock(),
    )

    private suspend fun persistSnapshot(
        id: IntegrationId,
        accountId: String?,
        snapshot: IntegrationSnapshot? = null,
    ) {
        val resolved = snapshot ?: adapter(id)?.let {
            runCatching { it.snapshot(accountId) }.getOrNull()
        } ?: return
        states.upsert(
            com.greninjaop.mailorganizer.data.local.IntegrationStateRecord(
                integrationId = id.value,
                accountId = accountId.orEmpty(),
                lastStatus = resolved.status.name,
                statusReason = resolved.statusReason,
                updatedAtEpochMs = clock(),
                configVersion = INTEGRATION_STATE_VERSION,
            ),
        )
    }

    private companion object {
        const val INTEGRATION_STATE_VERSION = 1
    }
}

/** Human-readable one-line description of an [IntegrationError] (phase §31). */
fun IntegrationError.describe(): String = when (this) {
    is IntegrationError.AuthRequired -> "authorization is required"
    is IntegrationError.PermissionDenied -> "a required permission was denied"
    is IntegrationError.AccountMismatch ->
        "account mismatch (expected $expectedAccountId, got $actualAccountId)"
    is IntegrationError.Offline -> "the device is offline"
    is IntegrationError.RateLimited ->
        "rate-limited" + (retryAfterSeconds?.let { " (retry after ${it}s)" } ?: "")
    is IntegrationError.ServiceUnavailable -> "the service is unavailable"
    is IntegrationError.InvalidRequest -> "the request was invalid"
    is IntegrationError.NotFound -> "not found"
    is IntegrationError.NotBuilt -> "not built yet — arrives with $arrivesWithPhase"
    is IntegrationError.Unknown -> message
}
