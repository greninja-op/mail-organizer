package com.greninjaop.mailorganizer.core.integrations

/**
 * Pure integration policies (Phase 17, phase §31–§32).
 *
 * Application-level error normalization and retry semantics live here as
 * total pure functions — not scattered across screens. No Android, no I/O.
 */

/**
 * Normalizes any failure into an [IntegrationError] without erasing useful
 * detail (phase §31). Unknown throwables become [IntegrationError.Unknown]
 * with their message preserved for diagnostics.
 */
fun Throwable.toIntegrationError(): IntegrationError = when (this) {
    is java.io.IOException -> IntegrationError.Offline
    is SecurityException -> IntegrationError.PermissionDenied
    else -> IntegrationError.Unknown(message ?: this::class.simpleName ?: "unknown error")
}

/**
 * Maps an [IntegrationError] to application-level retry semantics
 * (phase §32). External side effects are never retried blindly:
 * [RetryDecision.DO_NOT_RETRY] covers invalid requests and not-built
 * integrations.
 */
fun retryDecision(error: IntegrationError): RetryDecision = when (error) {
    is IntegrationError.Offline -> RetryDecision.RETRY_LATER
    is IntegrationError.AuthRequired -> RetryDecision.ASK_USER_RECONNECT
    is IntegrationError.PermissionDenied -> RetryDecision.ASK_USER_RECONNECT
    is IntegrationError.AccountMismatch -> RetryDecision.DO_NOT_RETRY
    is IntegrationError.RateLimited -> RetryDecision.BACKOFF
    is IntegrationError.ServiceUnavailable -> RetryDecision.RETRY_LATER
    is IntegrationError.InvalidRequest -> RetryDecision.DO_NOT_RETRY
    is IntegrationError.NotFound -> RetryDecision.DO_NOT_RETRY
    is IntegrationError.NotBuilt -> RetryDecision.DO_NOT_RETRY
    is IntegrationError.Unknown -> RetryDecision.DO_NOT_RETRY
}
