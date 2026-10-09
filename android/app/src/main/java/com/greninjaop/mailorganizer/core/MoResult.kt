package com.greninjaop.mailorganizer

/**
 * Explicit result type used across layers instead of throwing for expected
 * failures. Carries a typed [MoError] so callers handle failure cases
 * deliberately (per the Phase 0 error-handling foundation).
 */
sealed interface MoResult<out T> {

    data class Success<T>(val value: T) : MoResult<T>

    data class Failure(val error: MoError) : MoResult<Nothing>

    fun <R> map(transform: (T) -> R): MoResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun <R> fold(onSuccess: (T) -> R, onFailure: (MoError) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }
}

/**
 * Error taxonomy for the whole app (Phase 0 foundation; extended by later phases).
 * External integration failures must never crash the core experience — they are
 * represented here and surfaced as user-actionable states.
 */
sealed interface MoError {

    /** Transport-level failure (no connectivity, timeout, TLS, ...). */
    data class Network(val message: String, val cause: Throwable? = null) : MoError

    /** Authentication/authorization failure (expired token, revoked grant, ...). */
    data class Authentication(val message: String) : MoError

    /** OS permission denied by the user or policy. */
    data class PermissionDenied(val permission: String) : MoError

    /** Remote API returned an error payload. */
    data class Api(val code: Int?, val message: String) : MoError

    /** Rate-limited by the remote API; honor [retryAfterSeconds] when present. */
    data class RateLimited(val retryAfterSeconds: Long?) : MoError

    /** Local parsing/normalization failure. */
    data class Parsing(val message: String) : MoError

    /** Local persistence failure. */
    data class Database(val message: String, val cause: Throwable? = null) : MoError

    /** App is misconfigured (missing setup the user must complete). */
    data class InvalidConfiguration(val message: String) : MoError

    /** A modular integration (calendar, tasks, ...) failed independently. */
    data class Integration(val integration: String, val message: String) : MoError

    /** Anything else; always prefer a specific case above when possible. */
    data class Unexpected(val cause: Throwable? = null) : MoError
}
