package com.greninjaop.mailorganizer.ui.search

import com.greninjaop.mailorganizer.core.search.SearchDatePreset
import com.greninjaop.mailorganizer.core.search.SearchIndexState
import com.greninjaop.mailorganizer.core.search.SearchOutcome
import com.greninjaop.mailorganizer.core.search.SearchResultType
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Search screen state (Phase 10).
 *
 * One explicit state object; the content is a sealed type so the UI can
 * never show a result list and an error at the same time. Filters are
 * structured data — the UI never builds query strings (phase §13).
 */
data class SearchFilters(
    val category: MailCategory? = null,
    val priority: Priority? = null,
    val actionRequiredOnly: Boolean = false,
    val unreadOnly: Boolean = false,
    val hasAttachmentOnly: Boolean = false,
    val datePreset: SearchDatePreset = SearchDatePreset.ANY_TIME,
    /** Set by tapping a sender suggestion (phase §8, §33). */
    val sender: String? = null,
    /** Set by tapping a company suggestion (phase §8, §34). */
    val companyId: String? = null,
    val companyName: String? = null,
) {
    val hasActive: Boolean
        get() = category != null || priority != null || actionRequiredOnly ||
            unreadOnly || hasAttachmentOnly ||
            datePreset != SearchDatePreset.ANY_TIME ||
            !sender.isNullOrBlank() || companyId != null
}

/** What the search content area shows. */
sealed interface SearchContent {
    /** Empty query, no filters: tips + filter shortcuts, no DB work (§51). */
    data object Landing : SearchContent

    /** A debounced query is executing. */
    data object Loading : SearchContent

    /** Ranked results. */
    data class Results(val outcome: SearchOutcome) : SearchContent

    /** The query ran fine but matched nothing — never faked (§52). */
    data class Empty(val hint: String) : SearchContent

    /**
     * The index is unavailable. Shows the message plus a rebuild action —
     * never silently incomplete results (§53).
     */
    data class Error(val message: String) : SearchContent
}

data class SearchScreenState(
    val queryText: String = "",
    val filters: SearchFilters = SearchFilters(),
    val resultType: SearchResultType = SearchResultType.MESSAGES,
    val activeAccount: AccountRecord? = null,
    val isUnified: Boolean = false,
    val accountsById: Map<String, AccountRecord> = emptyMap(),
    val indexState: SearchIndexState? = null,
    /** Background index catch-up running (phase §56). Search still works. */
    val indexing: Boolean = false,
    val isOffline: Boolean = false,
    val content: SearchContent = SearchContent.Landing,
)
