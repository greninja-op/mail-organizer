package com.greninjaop.mailorganizer.core.integrations

import com.greninjaop.mailorganizer.MoResult

/**
 * Integration adapter contract (Phase 17, phase §5–§6).
 *
 * Each integration (Gmail, Calendar, Tasks) exposes itself to the
 * [IntegrationManager] through this interface. The manager coordinates;
 * adapters own the integration-specific logic. No Google API classes may
 * leak through this boundary — the presentation layer only ever sees
 * [IntegrationSnapshot] and the domain models in [IntegrationModels.kt].
 *
 * Contract:
 * - [snapshot] / [refresh] are total and never throw: failures are
 *   represented as snapshots with [IntegrationStatus.ERROR]/[IntegrationStatus.OFFLINE],
 *   never as crashes.
 * - [connect] performs the user-initiated connection flow's local part.
 *   For integrations whose build phase is deferred it returns a failure
 *   with [IntegrationError.NotBuilt] — never a fake success.
 * - [disconnect] is idempotent and safe: it clears integration metadata
 *   only. It never deletes mail, classifications, rules, action history,
 *   or other integrations' data (phase §18).
 */
interface IntegrationAdapter {

    val id: IntegrationId
    val displayName: String
    val provider: String

    /** Capabilities this integration declares when connected (phase §8). */
    val declaredCapabilities: Set<IntegrationCapability>

    /** Least-privilege permissions (phase §23). Never more than needed. */
    val requiredPermissions: List<PermissionDescription>

    /**
     * Current account-scoped snapshot. [accountId] null = no account in
     * scope. Total: never throws.
     */
    suspend fun snapshot(accountId: String?): IntegrationSnapshot

    /**
     * User-initiated connect. Returns failure (never fake success) when
     * the integration cannot actually connect — e.g. deferred build phase
     * or missing OAuth (Phase 3 deferred).
     */
    suspend fun connect(): MoResult<Unit>

    /**
     * User-initiated disconnect. Idempotent; clears integration metadata
     * only (phase §18). Safe to call when not connected.
     */
    suspend fun disconnect(accountId: String?): MoResult<Unit>

    /**
     * Lightweight status re-validation (phase §25): credentials,
     * permission, account — without expensive or continuous network use.
     * Total: never throws.
     */
    suspend fun refresh(accountId: String?): IntegrationSnapshot
}
