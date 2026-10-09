package com.greninjaop.mailorganizer.data.local

/**
 * Per-company message count for the company filter UI (Phase 8).
 *
 * Returned by the GROUP BY queries in [MessageDao]; Room maps the
 * `companyId` / `messageCount` column aliases onto these fields.
 */
data class CompanyMessageCount(
    val companyId: String,
    val messageCount: Int,
)
