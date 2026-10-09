package com.greninjaop.mailorganizer.domain.search

import com.greninjaop.mailorganizer.core.search.SearchIndexState

/**
 * Search-index maintenance contract (Phase 10).
 *
 * Implemented by [SearchIndexUseCase]; the search UI depends on this
 * abstraction so it stays unit-testable without a database.
 */
interface SearchIndexMaintenance {
    /** Ensures the account's index exists, is current, and has no backlog. */
    suspend fun ensureIndexed(accountId: String): SearchIndexState

    /** Rebuilds the account's index from normalized local data. */
    suspend fun rebuild(accountId: String): Int
}
