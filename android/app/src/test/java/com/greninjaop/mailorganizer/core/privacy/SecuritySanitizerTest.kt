package com.greninjaop.mailorganizer.core.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySanitizerTest {

    @Test
    fun sanitizeForLog_redactsBearerTokens() {
        val raw = "Error calling endpoint with Authorization: Bearer ya29.a0AfH6SMD_secret_token_123"
        val sanitized = SecuritySanitizer.sanitizeForLog(raw)
        assertFalse(sanitized.contains("ya29.a0AfH6SMD_secret_token_123"))
        assertTrue(sanitized.contains("[REDACTED"))
    }

    @Test
    fun sanitizeForLog_redactsOAuthTokens() {
        val raw = "Received token 1//0gSecretRefreshTokenXYZ during OAuth exchange"
        val sanitized = SecuritySanitizer.sanitizeForLog(raw)
        assertFalse(sanitized.contains("1//0gSecretRefreshTokenXYZ"))
        assertTrue(sanitized.contains("[REDACTED_TOKEN]"))
    }

    @Test
    fun sanitizeForLog_redactsEmailAddressesPreservingDomain() {
        val raw = "Sync failed for user alex.rivera@example.com on thread t123"
        val sanitized = SecuritySanitizer.sanitizeForLog(raw)
        assertFalse(sanitized.contains("alex.rivera@example.com"))
        assertTrue(sanitized.contains("a***a@example.com"))
        assertTrue(sanitized.contains("thread t123"))
    }

    @Test
    fun sanitizeForLog_handlesShortLocalPartEmail() {
        val redacted = SecuritySanitizer.redactEmail("me@domain.org")
        assertEquals("m***@domain.org", redacted)
    }

    @Test
    fun isSafeWebUrl_acceptsValidHttpAndHttps() {
        assertTrue(SecuritySanitizer.isSafeWebUrl("https://mail.google.com/mail"))
        assertTrue(SecuritySanitizer.isSafeWebUrl("http://example.com/unsub?id=123"))
    }

    @Test
    fun isSafeWebUrl_rejectsDangerousSchemesAndMalformedUrls() {
        assertFalse(SecuritySanitizer.isSafeWebUrl("javascript:alert(1)"))
        assertFalse(SecuritySanitizer.isSafeWebUrl("file:///android_asset/db.sqlite"))
        assertFalse(SecuritySanitizer.isSafeWebUrl("content://com.android.providers/contacts"))
        assertFalse(SecuritySanitizer.isSafeWebUrl("data:text/html;base64,PHNjcmlwdD4="))
        assertFalse(SecuritySanitizer.isSafeWebUrl(""))
        assertFalse(SecuritySanitizer.isSafeWebUrl(null))
        assertFalse(SecuritySanitizer.isSafeWebUrl("https://example.com/bad url with spaces"))
        assertFalse(SecuritySanitizer.isSafeWebUrl("not_a_url"))
    }

    @Test
    fun isSafeMailtoUri_validatesMailtoSafely() {
        assertTrue(SecuritySanitizer.isSafeMailtoUri("mailto:support@example.com"))
        assertTrue(SecuritySanitizer.isSafeMailtoUri("mailto:unsub@list.org?subject=Unsubscribe"))
        assertFalse(SecuritySanitizer.isSafeMailtoUri("mailto:"))
        assertFalse(SecuritySanitizer.isSafeMailtoUri("mailto:not-an-email"))
        assertFalse(SecuritySanitizer.isSafeMailtoUri("https://example.com"))
    }
}
