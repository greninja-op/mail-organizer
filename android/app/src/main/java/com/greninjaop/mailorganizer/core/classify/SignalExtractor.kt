package com.greninjaop.mailorganizer.core.classify

/**
 * Controlled signal extraction (Phase 7 §10).
 *
 * The classifier never blindly searches raw email. [SignalExtractor] derives
 * one documented [ExtractedSignals] snapshot from a [ClassificationInput];
 * rules then match against the snapshot only.
 *
 * Safety properties (phase §47, §59–60):
 * - Total: never throws, even on hostile input.
 * - Bounded: body text is truncated ([TextNormalizer.MAX_BODY_CHARS]); URL
 *   extraction caps the domain set; no regex with backtracking risk.
 * - No network: URLs are parsed as strings only, never visited.
 * - No rendering: classification uses the already-sanitized plain text.
 */
data class ExtractedSignals(
    val senderAddress: String,
    val senderDomain: String,
    val senderNameNorm: String,
    val subjectNorm: String,
    val bodyNorm: String,
    /** Gmail category labels present (SOCIAL, PROMOTIONS, UPDATES, FORUMS). */
    val gmailCategories: Set<String>,
    /** All raw label ids, uppercased for matching. */
    val labels: Set<String>,
    val hasUnsubscribe: Boolean,
    /** Domains found in body URLs (max [MAX_URL_DOMAINS]), never visited. */
    val urlDomains: Set<String>,
    /** Attachment filename hints (INVOICE_LIKE, RESUME_LIKE, ACADEMIC_LIKE). */
    val attachmentHints: Set<String>,
    /**
     * Minimal recurring-sender hint (Phase 8 builds the real sender/company
     * intelligence; Phase 7 only threads the flag through, default false).
     */
    val isRecurringSender: Boolean = false,
) {
    companion object {
        const val MAX_URL_DOMAINS = 20
    }
}

object SignalExtractor {

    // Simple, backtracking-free URL host pattern: scheme + host chars only.
    // Path/query are irrelevant for classification and never captured.
    private val URL_HOST = Regex("https?://([A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?)")

    private val UNSUBSCRIBE_PHRASES = listOf(
        "unsubscribe",
        "opt out",
        "opt-out",
        "manage preferences",
        "email preferences",
        "stop receiving",
    )

    private val KNOWN_GMAIL_CATEGORIES = setOf(
        "CATEGORY_PERSONAL",
        "CATEGORY_SOCIAL",
        "CATEGORY_PROMOTIONS",
        "CATEGORY_UPDATES",
        "CATEGORY_FORUMS",
    )

    fun extract(
        input: ClassificationInput,
        isRecurringSender: Boolean = false,
    ): ExtractedSignals {
        val senderAddress = TextNormalizer.normalizeAddress(input.fromAddress)
        val senderDomain = TextNormalizer.domainOf(senderAddress)
        val senderNameNorm = TextNormalizer.normalizeForMatch(
            input.fromName ?: "",
            TextNormalizer.MAX_SUBJECT_CHARS,
        )
        val subjectNorm = TextNormalizer.normalizeSubject(input.subject)
        val bodyNorm = TextNormalizer.normalizeForMatch(input.bodyText ?: "")
        val labels = input.labelIds
            .map { TextNormalizer.asciiLowercase(it.trim()).uppercase() }
            .toSet()
        return ExtractedSignals(
            senderAddress = senderAddress,
            senderDomain = senderDomain,
            senderNameNorm = senderNameNorm,
            subjectNorm = subjectNorm,
            bodyNorm = bodyNorm,
            gmailCategories = labels.intersect(KNOWN_GMAIL_CATEGORIES),
            labels = labels,
            hasUnsubscribe = detectUnsubscribe(subjectNorm, bodyNorm),
            urlDomains = extractUrlDomains(bodyNorm),
            attachmentHints = attachmentHints(input.attachmentFilenames),
            isRecurringSender = isRecurringSender,
        )
    }

    private fun detectUnsubscribe(subjectNorm: String, bodyNorm: String): Boolean {
        // Check the tail of the body too — footers live there — but the body
        // is already bounded, so a single pass is cheap.
        for (phrase in UNSUBSCRIBE_PHRASES) {
            if (subjectNorm.contains(phrase) || bodyNorm.contains(phrase)) return true
        }
        return false
    }

    private fun extractUrlDomains(bodyNorm: String): Set<String> {
        if (!bodyNorm.contains("http://") && !bodyNorm.contains("https://")) {
            return emptySet()
        }
        val domains = LinkedHashSet<String>()
        try {
            for (match in URL_HOST.findAll(bodyNorm)) {
                if (domains.size >= ExtractedSignals.MAX_URL_DOMAINS) break
                val host = match.groupValues[1].lowercase()
                if (host.isNotEmpty()) domains.add(host.trimEnd('.'))
            }
        } catch (t: Throwable) {
            // Total: hostile text must never break extraction.
        }
        return domains
    }

    private fun attachmentHints(filenames: List<String>): Set<String> {
        val hints = mutableSetOf<String>()
        for (raw in filenames) {
            val name = TextNormalizer.normalizeForMatch(raw, 256)
            when {
                name.contains("invoice") || name.contains("receipt") -> hints.add("INVOICE_LIKE")
                name.contains("resume") || name == "cv" || name.startsWith("cv.") -> hints.add("RESUME_LIKE")
                name.contains("assignment") || name.contains("homework") -> hints.add("ACADEMIC_LIKE")
            }
        }
        return hints
    }
}
