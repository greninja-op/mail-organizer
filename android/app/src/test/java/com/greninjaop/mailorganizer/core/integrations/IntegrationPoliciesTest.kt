package com.greninjaop.mailorganizer.core.integrations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure policy tests (Phase 17).
 *
 * Error normalization and retry semantics are total pure functions —
 * every branch is pinned here.
 */
class IntegrationPoliciesTest {

    @Test
    fun `retryDecision maps offline to retry later`() {
        assertEquals(RetryDecision.RETRY_LATER, retryDecision(IntegrationError.Offline))
        assertEquals(
            RetryDecision.RETRY_LATER,
            retryDecision(IntegrationError.ServiceUnavailable),
        )
    }

    @Test
    fun `retryDecision maps auth and permission problems to reconnect`() {
        assertEquals(RetryDecision.ASK_USER_RECONNECT, retryDecision(IntegrationError.AuthRequired))
        assertEquals(
            RetryDecision.ASK_USER_RECONNECT,
            retryDecision(IntegrationError.PermissionDenied),
        )
    }

    @Test
    fun `retryDecision maps rate limiting to backoff`() {
        assertEquals(
            RetryDecision.BACKOFF,
            retryDecision(IntegrationError.RateLimited(retryAfterSeconds = 30)),
        )
        assertEquals(
            RetryDecision.BACKOFF,
            retryDecision(IntegrationError.RateLimited(retryAfterSeconds = null)),
        )
    }

    @Test
    fun `retryDecision never retries invalid requests, mismatches, or not-built`() {
        assertEquals(RetryDecision.DO_NOT_RETRY, retryDecision(IntegrationError.InvalidRequest))
        assertEquals(RetryDecision.DO_NOT_RETRY, retryDecision(IntegrationError.NotFound))
        assertEquals(
            RetryDecision.DO_NOT_RETRY,
            retryDecision(IntegrationError.AccountMismatch("a", "b")),
        )
        assertEquals(
            RetryDecision.DO_NOT_RETRY,
            retryDecision(IntegrationError.NotBuilt("Phase 15")),
        )
        assertEquals(
            RetryDecision.DO_NOT_RETRY,
            retryDecision(IntegrationError.Unknown("weird")),
        )
    }

    @Test
    fun `toIntegrationError maps IO to offline and security to permission denied`() {
        assertEquals(
            IntegrationError.Offline,
            java.io.IOException("nope").toIntegrationError(),
        )
        assertEquals(
            IntegrationError.PermissionDenied,
            SecurityException("denied").toIntegrationError(),
        )
    }

    @Test
    fun `toIntegrationError preserves unknown messages for diagnostics`() {
        val error = IllegalStateException("odd state").toIntegrationError()
        assertTrue(error is IntegrationError.Unknown)
        assertEquals("odd state", (error as IntegrationError.Unknown).message)
    }

    @Test
    fun `usableCapabilities is empty unless connected`() {
        val declared = setOf(IntegrationCapability.READ_EMAIL)
        IntegrationStatus.values().forEach { status ->
            val snapshot = IntegrationSnapshot(
                id = IntegrationId.GMAIL,
                displayName = "Gmail",
                provider = "Google",
                accountId = "a1",
                accountEmail = "a@b.c",
                status = status,
                statusReason = "r",
                declaredCapabilities = declared,
                requiredPermissions = emptyList(),
                lastCheckedEpochMs = 0L,
            )
            if (status == IntegrationStatus.CONNECTED) {
                assertEquals(declared, snapshot.usableCapabilities)
                assertTrue(snapshot.isCapable(IntegrationCapability.READ_EMAIL))
            } else {
                assertTrue(
                    "status $status must not expose usable capabilities",
                    snapshot.usableCapabilities.isEmpty(),
                )
                assertFalse(snapshot.isCapable(IntegrationCapability.READ_EMAIL))
            }
        }
    }

    @Test
    fun `IntegrationId constants are stable`() {
        assertEquals("gmail", IntegrationId.GMAIL.value)
        assertEquals("calendar", IntegrationId.CALENDAR.value)
        assertEquals("tasks", IntegrationId.TASKS.value)
    }
}
