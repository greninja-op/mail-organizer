package com.greninjaop.mailorganizer.core.company

/**
 * Company & sender intelligence models (Phase 8).
 *
 * Pure Kotlin — no Android, no Room, no network. Detection is a total,
 * deterministic function of the sender address: the same address always
 * yields the same company, on any device, in any locale.
 *
 * Privacy: everything here is derived locally from already-synced mail.
 * No sender or company data ever leaves the device.
 */

/** Input to company detection — the sender side of one message. */
data class CompanyDetectionInput(
    val fromAddress: String,
    val fromName: String?,
)

/**
 * A detected company (Phase 8).
 *
 * @property companyId stable deterministic id: `"co:<normalizedDomain>"`.
 *   Account scoping is enforced by the database unique index on
 *   (accountId, normalizedDomain), not by the id itself.
 * @property canonicalName display name, e.g. "Google".
 * @property normalizedDomain registrable domain, e.g. "google.com".
 * @property knownDomains subdomains observed mapping to this company,
 *   e.g. {"mail.google.com"} — informational, for debugging/explanation.
 */
data class DetectedCompany(
    val companyId: String,
    val canonicalName: String,
    val normalizedDomain: String,
    val knownDomains: Set<String> = emptySet(),
)

/**
 * Pure sender profile used by the intelligence use case (Phase 8).
 *
 * Mirrors [com.greninjaop.mailorganizer.data.local.SenderRecord] without
 * depending on the data layer — the same boundary pattern as Phase 5's
 * EmailMessage → MessageRecord mappers.
 */
data class SenderProfile(
    val normalizedEmail: String,
    val displayName: String?,
    val domain: String,
    val messageCount: Int,
    val firstSeenEpochMs: Long,
    val lastSeenEpochMs: Long,
) {
    /** True once the sender crosses the documented recurring threshold. */
    val isRecurring: Boolean
        get() = SenderIntelligence.isRecurring(messageCount)
}
