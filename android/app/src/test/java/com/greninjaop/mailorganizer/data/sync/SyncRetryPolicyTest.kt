package com.greninjaop.mailorganizer.data.sync

import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retry policy tests (Phase 4, phase §25): transient retried, permanent never. */
class SyncRetryPolicyTest {

    private fun policy() = SyncRetryPolicy(
        maxAttempts = 3,
        baseDelayMs = 1_000L,
        maxDelayMs = 30_000L,
        random = Random(42), // deterministic jitter
    )

    @Test
    fun `transient network errors are retried`() {
        val p = policy()
        val d1 = p.decisionFor(SyncApiException.NetworkError(), 1)
        assertTrue(d1 is RetryDecision.RetryAfter)
    }

    @Test
    fun `server errors are retried`() {
        val p = policy()
        assertTrue(p.decisionFor(SyncApiException.ServerError(500), 1) is RetryDecision.RetryAfter)
        assertTrue(p.decisionFor(SyncApiException.ServerError(503), 2) is RetryDecision.RetryAfter)
    }

    @Test
    fun `auth expiry is never retried`() {
        val p = policy()
        assertEquals(RetryDecision.DoNotRetry, p.decisionFor(SyncApiException.AuthExpired(), 1))
    }

    @Test
    fun `permission denial is never retried`() {
        val p = policy()
        assertEquals(RetryDecision.DoNotRetry, p.decisionFor(SyncApiException.PermissionDenied(), 1))
    }

    @Test
    fun `invalid requests are never retried`() {
        val p = policy()
        assertEquals(
            RetryDecision.DoNotRetry,
            p.decisionFor(SyncApiException.InvalidRequest("bad"), 1),
        )
    }

    @Test
    fun `history invalidation is never retried (re-baseline handles it)`() {
        val p = policy()
        assertEquals(RetryDecision.DoNotRetry, p.decisionFor(SyncApiException.HistoryInvalid(), 1))
    }

    @Test
    fun `not-configured is never retried`() {
        val p = policy()
        assertEquals(RetryDecision.DoNotRetry, p.decisionFor(SyncApiException.NotConfigured(), 1))
    }

    @Test
    fun `attempt ceiling stops retries`() {
        val p = policy() // maxAttempts = 3
        assertEquals(
            RetryDecision.DoNotRetry,
            p.decisionFor(SyncApiException.NetworkError(), 3),
        )
    }

    @Test
    fun `rate limiting honors server hint`() {
        val p = policy()
        val decision = p.decisionFor(SyncApiException.RateLimited(retryAfterSeconds = 5), 1)
        assertTrue(decision is RetryDecision.RetryAfter)
        // Hint is 5s; jitter only applies to computed backoff, so expect ~5000ms.
        assertEquals(5_000L, (decision as RetryDecision.RetryAfter).delayMs)
    }

    @Test
    fun `rate limit hint is capped at max delay`() {
        val p = policy()
        val decision = p.decisionFor(SyncApiException.RateLimited(retryAfterSeconds = 3_600), 1)
        assertEquals(30_000L, (decision as RetryDecision.RetryAfter).delayMs)
    }

    @Test
    fun `backoff grows exponentially within bounds`() {
        val p = SyncRetryPolicy(
            maxAttempts = 10,
            baseDelayMs = 1_000L,
            maxDelayMs = 1_000_000L,
            random = Random(7),
        )
        val d1 = p.backoffDelayMs(1)
        val d2 = p.backoffDelayMs(2)
        val d3 = p.backoffDelayMs(3)
        // ±20% jitter bands around 1000 / 2000 / 4000
        assertTrue(d1 in 800L..1200L)
        assertTrue(d2 in 1600L..2400L)
        assertTrue(d3 in 3200L..4800L)
    }

    @Test
    fun `backoff never exceeds max delay`() {
        val p = policy()
        assertTrue(p.backoffDelayMs(100) <= 30_000L)
    }

    @Test
    fun `withRetries retries transient then succeeds`() = runTest {
        val p = SyncRetryPolicy(maxAttempts = 3, baseDelayMs = 1L, maxDelayMs = 10L, random = Random(1))
        var calls = 0
        val result = p.withRetries {
            calls++
            if (calls < 3) throw SyncApiException.NetworkError() else "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, calls)
    }

    @Test
    fun `withRetries gives up after ceiling and rethrows last error`() = runTest {
        val p = SyncRetryPolicy(maxAttempts = 2, baseDelayMs = 1L, maxDelayMs = 10L, random = Random(1))
        var calls = 0
        try {
            p.withRetries {
                calls++
                throw SyncApiException.NetworkError()
            }
            error("should have thrown")
        } catch (e: SyncApiException.NetworkError) {
            assertEquals(2, calls)
        }
    }

    @Test
    fun `withRetries does not retry permanent errors`() = runTest {
        val p = SyncRetryPolicy(maxAttempts = 5, baseDelayMs = 1L, maxDelayMs = 10L, random = Random(1))
        var calls = 0
        try {
            p.withRetries {
                calls++
                throw SyncApiException.AuthExpired()
            }
            error("should have thrown")
        } catch (e: SyncApiException.AuthExpired) {
            assertEquals(1, calls)
        }
    }
}
