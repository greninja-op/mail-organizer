package com.greninjaop.mailorganizer.data.sync

import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Controlled retry policy for Gmail API calls (Phase 4, phase §25–26).
 *
 * - Retried: transient network failures, 5xx server errors, rate limiting
 *   (honoring the server's retry-after hint, capped).
 * - NEVER retried: auth expiry, permission denial, malformed requests,
 *   invalid history cursors (those need user action or re-baseline, not
 *   blind repetition).
 * - Exponential backoff with jitter; a hard attempt ceiling — no infinite
 *   retry loops, no API hammering.
 */
sealed interface RetryDecision {
    data object DoNotRetry : RetryDecision
    data class RetryAfter(val delayMs: Long) : RetryDecision
}

class SyncRetryPolicy(
    val maxAttempts: Int = 5,
    val baseDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 30_000L,
    private val random: Random = Random.Default,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(baseDelayMs > 0) { "baseDelayMs must be > 0" }
        require(maxDelayMs >= baseDelayMs) { "maxDelayMs must be >= baseDelayMs" }
    }

    /**
     * Decides what to do after [attempt] (1-based) failed with [error].
     * Pure and deterministic given [random] — fully unit-testable.
     */
    fun decisionFor(error: SyncApiException, attempt: Int): RetryDecision {
        if (attempt >= maxAttempts) return RetryDecision.DoNotRetry
        return when (error) {
            is SyncApiException.NetworkError,
            is SyncApiException.ServerError,
            -> RetryDecision.RetryAfter(backoffDelayMs(attempt))
            is SyncApiException.RateLimited -> {
                val hintedMs = error.retryAfterSeconds?.let { (it * 1_000L).coerceAtMost(maxDelayMs) }
                RetryDecision.RetryAfter(hintedMs ?: backoffDelayMs(attempt))
            }
            // Auth, permission, malformed requests, invalid history, and the
            // not-configured stand-in must never be blindly retried.
            is SyncApiException.AuthExpired,
            is SyncApiException.PermissionDenied,
            is SyncApiException.InvalidRequest,
            is SyncApiException.HistoryInvalid,
            is SyncApiException.NotConfigured,
            -> RetryDecision.DoNotRetry
        }
    }

    /**
     * Runs [block], retrying transient [SyncApiException] failures per
     * [decisionFor]. Non-API throwables (bugs, DB failures, cancellation)
     * are never retried here. Cancellation-aware: [delay] cooperates with
     * coroutine cancellation (phase §27 — cancellation is never swallowed).
     */
    suspend fun <T> withRetries(block: suspend (attempt: Int) -> T): T {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return block(attempt)
            } catch (e: SyncApiException) {
                when (val decision = decisionFor(e, attempt)) {
                    is RetryDecision.DoNotRetry -> throw e
                    is RetryDecision.RetryAfter -> delay(decision.delayMs)
                }
            }
        }
    }

    /** Exponential backoff: base * 2^(attempt-1), capped, ±20% jitter. */
    fun backoffDelayMs(attempt: Int): Long {
        val exponential = baseDelayMs * 2.0.pow(attempt - 1).toLong()
        val capped = exponential.coerceAtMost(maxDelayMs)
        val jitter = 1.0 + (random.nextDouble() - 0.5) * 0.4 // ±20%
        return (capped * jitter).toLong().coerceIn(1L, maxDelayMs)
    }
}
