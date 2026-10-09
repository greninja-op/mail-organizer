package com.greninjaop.mailorganizer.core.integrations

/**
 * Integration Manager models (Phase 17).
 *
 * A coherent application-level system for managing the app's external
 * integrations (Gmail, and later Calendar/Tasks). The manager coordinates
 * integrations; it never absorbs their API-specific logic and never
 * performs external side effects itself.
 *
 * Honesty is the core design constraint:
 * - [IntegrationStatus.CONNECTED] is reported ONLY when an integration is
 *   genuinely usable. Declared capabilities mean nothing until then —
 *   [IntegrationSnapshot.usableCapabilities] is empty for every other
 *   status, so callers cannot mistake a declaration for availability.
 * - Integrations whose build phase is user-deferred (Calendar → Phase 15,
 *   Tasks → Phase 16) report [IntegrationStatus.UNAVAILABLE] with an
 *   explicit reason. They never report success, never fake a connection.
 * - No Google API classes leak past this layer; the UI only sees these
 *   domain-level models.
 */

/** Stable identifier for an integration. Extensible for future providers. */
@JvmInline
value class IntegrationId(val value: String) {
    companion object {
        val GMAIL = IntegrationId("gmail")
        val CALENDAR = IntegrationId("calendar")
        val TASKS = IntegrationId("tasks")
    }

    override fun toString(): String = value
}

/**
 * Deterministic integration status model (phase §7).
 *
 * Only states meaningful to the actual architecture are used:
 * - [AVAILABLE]: built, not connected, ready to connect.
 * - [CONNECTED]: genuinely usable — verified, not assumed.
 * - [CONNECTING]: a connection attempt is in flight.
 * - [DISCONNECTED]: built but not connected (and connectable in principle).
 * - [AUTH_REQUIRED]: needs (re)authorization the user could grant.
 * - [PERMISSION_REQUIRED]: connected identity but a permission is missing.
 * - [OFFLINE]: previously healthy; the device is offline. This is NOT a
 *   disconnect — the integration must not be marked disconnected merely
 *   because the network is down (phase §26).
 * - [UNAVAILABLE]: the integration is not built yet (its phase is deferred).
 * - [ERROR]: a non-recoverable-by-retry failure; see [statusReason].
 */
enum class IntegrationStatus {
    AVAILABLE,
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    AUTH_REQUIRED,
    PERMISSION_REQUIRED,
    OFFLINE,
    UNAVAILABLE,
    ERROR,
}

/**
 * Capabilities an integration can provide (phase §8).
 *
 * These are *declared* capabilities — what the integration offers when it
 * is [IntegrationStatus.CONNECTED]. [IntegrationSnapshot.usableCapabilities]
 * is the only source of truth for what is actually usable right now.
 * Unsupported capabilities are never advertised: SEND_EMAIL, for example,
 * is deliberately absent until the Gmail write phase (22) lands.
 */
enum class IntegrationCapability {
    READ_EMAIL,
    SYNC_EMAIL,
    READ_CALENDAR_METADATA,
    CREATE_EVENT,
    READ_TASK_LISTS,
    CREATE_TASK,
}

/** Least-privilege permission declaration (phase §23). */
data class PermissionDescription(
    /** Scope or permission name, e.g. "gmail.readonly". */
    val scope: String,
    /** Human-readable, precise purpose. Never a false claim. */
    val purpose: String,
    /** Whether the permission is needed to use the integration at all. */
    val required: Boolean,
)

/**
 * Point-in-time, account-scoped view of one integration (phase §6).
 *
 * [accountId] is null when no account is in scope; every snapshot carries
 * account identity so cross-account leakage is impossible by construction
 * (phase §15).
 */
data class IntegrationSnapshot(
    val id: IntegrationId,
    val displayName: String,
    val provider: String,
    val accountId: String?,
    val accountEmail: String?,
    val status: IntegrationStatus,
    /** Honest human-readable reason, e.g. "Google sign-in arrives with Phase 3". */
    val statusReason: String,
    val declaredCapabilities: Set<IntegrationCapability>,
    val requiredPermissions: List<PermissionDescription>,
    val lastCheckedEpochMs: Long,
) {
    /**
     * Capabilities actually usable right now. Only [IntegrationStatus.CONNECTED]
     * integrations expose any — a declaration is not availability (phase §8).
     */
    val usableCapabilities: Set<IntegrationCapability>
        get() = if (status == IntegrationStatus.CONNECTED) declaredCapabilities else emptySet()

    fun isCapable(capability: IntegrationCapability): Boolean =
        capability in usableCapabilities
}

/**
 * Application-level error categories (phase §31).
 *
 * Individual integrations may fail differently; the manager normalizes
 * into these without erasing provider-specific detail (kept in the
 * message where available).
 */
sealed interface IntegrationError {
    data object AuthRequired : IntegrationError
    data object PermissionDenied : IntegrationError
    data class AccountMismatch(val expectedAccountId: String, val actualAccountId: String) :
        IntegrationError
    data object Offline : IntegrationError
    data class RateLimited(val retryAfterSeconds: Long?) : IntegrationError
    data object ServiceUnavailable : IntegrationError
    data object InvalidRequest : IntegrationError
    data object NotFound : IntegrationError
    /** The integration is not built yet — its phase is deferred. Never retried. */
    data class NotBuilt(val arrivesWithPhase: String) : IntegrationError
    data class Unknown(val message: String) : IntegrationError
}

/** Application-level retry semantics (phase §32). One policy, not per-screen logic. */
enum class RetryDecision {
    /** Wait and retry later (e.g. offline). */
    RETRY_LATER,
    /** Ask the user to reconnect (e.g. auth required). */
    ASK_USER_RECONNECT,
    /** Never retry automatically (e.g. invalid request, not-built). */
    DO_NOT_RETRY,
    /** Back off before retrying (e.g. rate-limited). */
    BACKOFF,
}

/**
 * Lightweight internal integration events (phase §33).
 *
 * Delivered via a SharedFlow on the manager — no heavyweight event bus.
 */
sealed interface IntegrationEvent {
    data class StatusChanged(
        val id: IntegrationId,
        val accountId: String?,
        val status: IntegrationStatus,
    ) : IntegrationEvent

    data class Connected(val id: IntegrationId, val accountId: String) : IntegrationEvent
    data class Disconnected(val id: IntegrationId, val accountId: String?) : IntegrationEvent
    data class AccountRemoved(val accountId: String) : IntegrationEvent
    data class IntegrationFailed(val id: IntegrationId, val error: IntegrationError) :
        IntegrationEvent
}
