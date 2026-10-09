package com.greninjaop.mailorganizer.core.cleanup

/**
 * Unsubscribe signal extractor & safety validator (Phase 20 §10, §11).
 *
 * Safety Invariant:
 * - Unsubscribe content is UNTRUSTED external input.
 * - Under NO circumstances does Mail Organizer automatically visit URLs,
 *   send emails, or execute network requests to unsubscribe.
 * - Any future action must be explicitly initiated by the user.
 * - URLs must be validated for safe schemes (http / https) and mailto: addresses
 *   before being presented to the user.
 */
object UnsubscribeSafety {

    private val MAILTO_PATTERN = Regex("""<mailto:([^>]+)>|mailto:([^\s,>]+)""", RegexOption.IGNORE_CASE)
    private val HTTP_PATTERN = Regex("""<(https?://[^>]+)>|(https?://[^\s,>]+)""", RegexOption.IGNORE_CASE)

    /**
     * Parses the RFC 2369 `List-Unsubscribe` header safely.
     * Returns a pair of (safeHttpUrl, safeMailto).
     */
    fun parseListUnsubscribeHeader(headerValue: String?): Pair<String?, String?> {
        if (headerValue.isNullOrBlank()) return Pair(null, null)

        var mailto: String? = null
        var httpUrl: String? = null

        // RFC 2369 formats: <mailto:list-request@host.com?subject=unsubscribe>, <https://example.com/unsub>
        val mailtoMatch = MAILTO_PATTERN.find(headerValue)
        if (mailtoMatch != null) {
            val raw = mailtoMatch.groupValues[1].ifBlank { mailtoMatch.groupValues[2] }
            if (raw.isNotBlank() && isValidMailto(raw)) {
                mailto = sanitizeMailto(raw)
            }
        }

        val httpMatch = HTTP_PATTERN.find(headerValue)
        if (httpMatch != null) {
            val raw = httpMatch.groupValues[1].ifBlank { httpMatch.groupValues[2] }
            if (raw.isNotBlank() && isValidHttpUrl(raw)) {
                httpUrl = sanitizeHttpUrl(raw)
            }
        }

        return Pair(httpUrl, mailto)
    }

    /**
     * Inspects body text or snippet for typical unsubscribe phrases.
     */
    fun containsUnsubscribePhrases(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return lower.contains("unsubscribe") ||
            lower.contains("opt out") ||
            lower.contains("opt-out") ||
            lower.contains("manage preferences") ||
            lower.contains("email preferences") ||
            lower.contains("subscription preferences") ||
            lower.contains("stop receiving") ||
            lower.contains("to unsubscribe click here")
    }

    private fun isValidHttpUrl(url: String): Boolean {
        val trimmed = url.trim()
        return (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)) &&
            !trimmed.contains('\n') &&
            !trimmed.contains('\r') &&
            trimmed.length <= 1024
    }

    private fun isValidMailto(mailto: String): Boolean {
        val trimmed = mailto.trim()
        return !trimmed.contains('\n') &&
            !trimmed.contains('\r') &&
            trimmed.length <= 320
    }

    private fun sanitizeHttpUrl(url: String): String {
        return url.trim().replace("\"", "").replace("'", "")
    }

    private fun sanitizeMailto(mailto: String): String {
        return mailto.trim().replace("\"", "").replace("'", "")
    }
}
