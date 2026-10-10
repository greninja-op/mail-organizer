package com.greninjaop.mailorganizer.core.company

import com.greninjaop.mailorganizer.core.classify.TextNormalizer

/**
 * Deterministic sender → company detection (Phase 8).
 *
 * Design (requirements.md §"Company grouping and filtering",
 * design.md §"Category company filtering"):
 * - The same sender address always yields the same company — pure function
 *   of the normalized address, no device state, no network.
 * - Subdomains canonicalize: `noreply@mail.google.com`,
 *   `support@google.com` → one company, "Google" (`google.com`).
 * - Free/personal mailbox domains (gmail.com, yahoo.com, …) are people,
 *   not companies → returns null. Their mail still gets sender tracking;
 *   it just doesn't create a company grouping.
 *
 * Honest limitations (documented, not hidden):
 * - Registrable-domain extraction uses last-two-labels plus a small
 *   built-in public-suffix list — it is NOT a full PSL. Obscure ccTLD
 *   structures may canonicalize imperfectly; the result stays stable.
 * - The well-known name map is display-only and intentionally small.
 *   Unknown domains derive a name from the domain label itself.
 *
 * Total: malformed input yields null, never an exception.
 */
object CompanyDetector {

    /**
     * Mailbox-provider domains whose senders are treated as people, not
     * companies. Kept small and documented; corporate Google Workspace
     * domains are NOT in this list (they keep their own domain).
     */
    private val PERSONAL_DOMAINS = setOf(
        "gmail.com", "googlemail.com",
        "yahoo.com", "yahoo.co.uk", "yahoo.co.in", "yahoo.ca", "yahoo.com.au",
        "outlook.com", "hotmail.com", "live.com", "msn.com",
        "icloud.com", "me.com", "mac.com",
        "protonmail.com", "proton.me", "pm.me",
        "aol.com", "zoho.com", "gmx.com", "mail.com", "yandex.com",
    )

    /**
     * Mailing-infrastructure subdomains stripped before canonicalization.
     * Conservative: only labels that unambiguously indicate bulk/transactional
     * mail plumbing, never brand labels.
     */
    private val MAILING_SUBDOMAINS = setOf(
        "mail", "email", "news", "newsletter", "newsletters",
        "promo", "promos", "deals", "offers", "marketing",
        "notify", "notification", "notifications", "alert", "alerts",
        "bounce", "bounces", "mailer", "send", "smtp", "transactional",
    )

    /**
     * Minimal public-suffix knowledge for registrable-domain extraction.
     * Only multi-label suffixes that break the naive last-two-labels rule.
     * Everything else falls back to last-two-labels (documented above).
     */
    private val MULTI_LABEL_SUFFIXES = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk",
        "com.au", "net.au", "org.au",
        "co.jp", "ne.jp", "or.jp",
        "co.in", "net.in", "org.in",
        "com.br", "net.br", "org.br",
        "co.za", "co.nz", "com.mx", "co.kr",
    )

    /**
     * Display-name overrides for widely recognized brands. Display-only:
     * detection identity is always the normalized domain. Small by design —
     * unknown domains fall back to the capitalized domain label.
     */
    private val WELL_KNOWN_NAMES = mapOf(
        "google" to "Google",
        "facebook" to "Facebook",
        "meta" to "Meta",
        "instagram" to "Instagram",
        "amazon" to "Amazon",
        "apple" to "Apple",
        "microsoft" to "Microsoft",
        "linkedin" to "LinkedIn",
        "netflix" to "Netflix",
        "spotify" to "Spotify",
        "twitter" to "X",
        "x" to "X",
        "github" to "GitHub",
        "gitlab" to "GitLab",
        "coursera" to "Coursera",
        "udemy" to "Udemy",
        "edx" to "edX",
        "naukri" to "Naukri",
        "indeed" to "Indeed",
        "flipkart" to "Flipkart",
        "swiggy" to "Swiggy",
        "zomato" to "Zomato",
    )

    private const val MAX_DOMAIN_CHARS = 253

    private const val CACHE_CAPACITY = 512
    private val detectionCache = object : LinkedHashMap<String, DetectedCompany?>(CACHE_CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DetectedCompany?>?): Boolean {
            return size > CACHE_CAPACITY
        }
    }

    /**
     * Detects the company for a sender, or null when the sender is not
     * attributable to a company (personal mailbox, malformed address).
     */
    fun detect(input: CompanyDetectionInput): DetectedCompany? {
        val rawAddress = input.fromAddress
        synchronized(detectionCache) {
            if (detectionCache.containsKey(rawAddress)) {
                return detectionCache[rawAddress]
            }
        }
        val result = try {
            detectOrThrow(input)
        } catch (t: Throwable) {
            // Total: hostile input degrades to "no company", never crashes.
            null
        }
        synchronized(detectionCache) {
            detectionCache[rawAddress] = result
        }
        return result
    }

    private fun detectOrThrow(input: CompanyDetectionInput): DetectedCompany? {
        val address = TextNormalizer.normalizeAddress(input.fromAddress)
        if (address.isBlank() || address.length > MAX_DOMAIN_CHARS + 320) return null
        // Malformed: no "@" or nothing before it (e.g. "@example.com").
        val at = address.lastIndexOf('@')
        if (at <= 0) return null
        val domain = TextNormalizer.domainOf(address)
        if (domain.isEmpty() || domain.length > MAX_DOMAIN_CHARS) return null
        if (domain in PERSONAL_DOMAINS) return null

        val canonical = canonicalizeDomain(domain) ?: return null
        if (canonical in PERSONAL_DOMAINS) return null

        return DetectedCompany(
            companyId = "co:$canonical",
            canonicalName = displayNameFor(canonical),
            normalizedDomain = canonical,
            knownDomains = if (domain != canonical) setOf(domain) else emptySet(),
        )
    }

    /**
     * Strips mailing-infrastructure subdomains, then reduces to the
     * registrable domain (last two labels, or three under a known
     * multi-label public suffix).
     */
    fun canonicalizeDomain(domain: String): String? {
        val lower = TextNormalizer.asciiLowercase(domain.trim().trimEnd('.'))
        if (lower.isEmpty() || lower.length > MAX_DOMAIN_CHARS) return null
        if (!lower.all { it.isLetterOrDigit() || it == '.' || it == '-' }) return null

        val labels = lower.split('.').filter { it.isNotEmpty() }
        if (labels.size < 2) return null

        // Strip leading mailing-infrastructure subdomains, keeping at
        // least two labels so we never reduce to a bare TLD.
        val stripped = labels.toMutableList()
        while (stripped.size > 2 && stripped.first() in MAILING_SUBDOMAINS) {
            stripped.removeAt(0)
        }

        val registrable = when {
            stripped.size >= 3 &&
                "${stripped[stripped.size - 2]}.${stripped.last()}" in MULTI_LABEL_SUFFIXES ->
                stripped.takeLast(3).joinToString(".")
            else -> stripped.takeLast(2).joinToString(".")
        }
        return registrable.takeIf { it.contains('.') }
    }

    /**
     * Display name for a canonical domain: well-known override, else the
     * capitalized registrable label ("acme" → "Acme").
     */
    fun displayNameFor(normalizedDomain: String): String {
        val label = normalizedDomain.substringBefore('.')
        WELL_KNOWN_NAMES[label]?.let { return it }
        return label.replaceFirstChar { it.uppercaseChar() }
    }
}
