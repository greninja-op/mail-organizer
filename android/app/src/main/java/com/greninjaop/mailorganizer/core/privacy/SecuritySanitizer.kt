package com.greninjaop.mailorganizer.core.privacy

/**
 * Hardened sanitizer and safety validator for logging, text presentation, and URLs (Phase 23 §12, §14, §22).
 */
object SecuritySanitizer {

    private val EMAIL_REGEX = Regex("""[a-zA-Z0-9+._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val OAUTH_TOKEN_REGEX = Regex("""(?i)(ya29\.[a-zA-Z0-9_-]+|1//[a-zA-Z0-9_-]+|bearer\s+[a-zA-Z0-9._-]+)""")
    private val AUTH_HEADER_REGEX = Regex("""(?i)Authorization:\s*Bearer\s+[^\s,]+""")

    /**
     * Sanitizes strings destined for logs, error messages, or crash diagnostics.
     * Strips Bearer tokens, raw email addresses, and potential auth headers.
     */
    fun sanitizeForLog(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var sanitized = input
        sanitized = AUTH_HEADER_REGEX.replace(sanitized, "Authorization: Bearer [REDACTED]")
        sanitized = OAUTH_TOKEN_REGEX.replace(sanitized, "[REDACTED_TOKEN]")
        sanitized = EMAIL_REGEX.replace(sanitized) { matchResult ->
            val email = matchResult.value
            redactEmail(email)
        }
        return sanitized
    }

    /**
     * Redacts an email address while preserving domain context for debugging (e.g., u***r@example.com).
     */
    fun redactEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "[REDACTED_EMAIL]"
        val local = parts[0]
        val domain = parts[1]
        val redactedLocal = when {
            local.length <= 2 -> "${local.firstOrNull() ?: '*'}***"
            else -> "${local.first()}***${local.last()}"
        }
        return "$redactedLocal@$domain"
    }

    /**
     * Validates whether a URL is strictly safe to be opened by external intents or shown to users.
     * Blocks javascript:, data:, file:, and content: schemes. Only allows http and https.
     */
    fun isSafeWebUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (trimmed.any { it.isWhitespace() || it.code < 0x20 }) return false
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) return false
        val scheme = trimmed.substring(0, colonIndex).lowercase()
        return scheme == "http" || scheme == "https"
    }

    /**
     * Validates an external mailto: URI.
     */
    fun isSafeMailtoUri(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        val trimmed = uri.trim()
        if (trimmed.any { it.isWhitespace() || it.code < 0x20 }) return false
        if (!trimmed.lowercase().startsWith("mailto:")) return false
        val addressPart = trimmed.substringAfter("mailto:").substringBefore("?").trim()
        return addressPart.isNotEmpty() && EMAIL_REGEX.matches(addressPart)
    }
}
