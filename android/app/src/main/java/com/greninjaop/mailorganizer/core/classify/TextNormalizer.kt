package com.greninjaop.mailorganizer.core.classify

import java.text.Normalizer

/**
 * Normalization before matching (Phase 7 §11).
 *
 * Classification operates on normalized text so that
 * `"Your   ORDER Confirmation!"` matches `"your order confirmation"`.
 *
 * Rules:
 * - Unicode NFKC normalization (compatibility equivalents folded).
 * - Locale-independent lowercasing: only ASCII A–Z are folded by hand, so a
 *   Turkish-locale device can never classify differently from any other
 *   device (determinism, phase §19). Non-ASCII case is left intact.
 * - Whitespace runs (spaces, tabs, newlines) collapse to a single space.
 * - Leading/trailing whitespace trimmed.
 * - Bounded length: inputs are truncated to [maxLength] chars so a hostile
 *   multi-megabyte body cannot blow up classification time or memory
 *   (phase §60). Truncation is on char count — cheap and deterministic.
 *
 * Names, product identifiers, URLs and company names are never stemmed or
 * rewritten (phase §11).
 */
object TextNormalizer {

    /** Maximum chars of body text the classifier ever inspects. */
    const val MAX_BODY_CHARS = 20_000

    /** Maximum chars of subject text inspected. */
    const val MAX_SUBJECT_CHARS = 1_000

    fun normalizeForMatch(text: String, maxLength: Int = MAX_BODY_CHARS): String {
        if (text.isEmpty()) return ""
        val folded = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val lower = asciiLowercase(folded)
        val collapsed = collapseWhitespace(lower)
        val trimmed = collapsed.trim()
        return if (trimmed.length > maxLength) {
            trimmed.substring(0, maxLength)
        } else {
            trimmed
        }
    }

    fun normalizeSubject(subject: String): String =
        normalizeForMatch(subject, MAX_SUBJECT_CHARS)

    /**
     * Locale-independent ASCII lowercasing. Non-ASCII characters pass through
     * unchanged (after NFKC they are already in a canonical form).
     */
    fun asciiLowercase(text: String): String {
        var changed = false
        for (c in text) {
            if (c in 'A'..'Z') {
                changed = true
                break
            }
        }
        if (!changed) return text
        val out = StringBuilder(text.length)
        for (c in text) {
            out.append(if (c in 'A'..'Z') c + 32 else c)
        }
        return out.toString()
    }

    private fun collapseWhitespace(text: String): String {
        val out = StringBuilder(text.length)
        var lastWasSpace = true // trims leading whitespace too
        for (c in text) {
            if (c.isWhitespace()) {
                if (!lastWasSpace) {
                    out.append(' ')
                    lastWasSpace = true
                }
            } else {
                out.append(c)
                lastWasSpace = false
            }
        }
        // Trim a single trailing space if we added one.
        val len = out.length
        if (len > 0 && out[len - 1] == ' ') out.setLength(len - 1)
        return out.toString()
    }

    /**
     * Extracts the domain from an email address, normalized.
     * Returns "" when the address has no domain part.
     */
    fun domainOf(address: String): String {
        val at = address.lastIndexOf('@')
        if (at < 0 || at == address.length - 1) return ""
        return asciiLowercase(address.substring(at + 1).trim()).trimEnd('.')
    }

    /** Normalized address for sender matching (lowercased, trimmed). */
    fun normalizeAddress(address: String): String =
        asciiLowercase(address.trim())
}
