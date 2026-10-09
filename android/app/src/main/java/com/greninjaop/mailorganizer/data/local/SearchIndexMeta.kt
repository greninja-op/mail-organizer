package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Search-index version tracking (Phase 10, phase §23).
 *
 * One row per account. When [SearchIndexStore.INDEX_VERSION] exceeds the
 * stored [version], the index schema changed and the account's index is
 * rebuilt from normalized local data — never via Gmail resynchronization.
 */
@Entity(tableName = "search_index_meta")
data class SearchIndexMeta(
    @PrimaryKey val accountId: String,
    val version: Int,
)
