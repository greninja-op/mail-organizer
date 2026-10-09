package com.greninjaop.mailorganizer.data.local

/**
 * Per-category classification count for the dashboard (Phase 11).
 *
 * Returned by the GROUP BY query in [ClassificationDao]; Room maps the
 * `category` / `messageCount` column aliases onto these fields. Categories
 * with zero classified messages produce no row — callers treat a missing
 * category as 0, never as fabricated data.
 */
data class CategoryMessageCount(
    val category: MailCategory,
    val messageCount: Int,
)
