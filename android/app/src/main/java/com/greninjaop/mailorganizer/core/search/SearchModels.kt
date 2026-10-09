package com.greninjaop.mailorganizer.core.search

import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.SenderRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord

/**
 * Search domain models (Phase 10).
 *
 * Pure Kotlin — no Android, no Room, no network. The UI builds a
 * [SearchQuery] (via [QueryParser] for the text part); the data layer
 * executes it. Queries are structured data all the way down: the UI never
 * constructs SQL strings (phase §13, §48).
 */

/** Which shape the result list takes. */
enum class SearchResultType {
    MESSAGES,
    THREADS,
}

/** Basic date-range presets for the date filter (phase §36). */
enum class SearchDatePreset {
    ANY_TIME,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
}

/**
 * A structured search request (Phase 10, updated in Phase 18 for unified search).
 *
 * When [accountId] is non-null, search is strictly scoped to that account (phase §26).
 * When [accountId] is null, search operates across all enabled accounts in unified mode (phase §39).
 * [rawText] is parsed by [QueryParser] into a safe FTS5
 * expression; every other field becomes a parameterized predicate.
 */
data class SearchQuery(
    val rawText: String = "",
    val accountId: String? = null,
    val category: MailCategory? = null,
    val priority: Priority? = null,
    val actionRequiredOnly: Boolean = false,
    /** Free text matched against sender name/address (phase §33). */
    val sender: String? = null,
    /** Canonical company id from company intelligence (phase §34). */
    val companyId: String? = null,
    val unreadOnly: Boolean = false,
    val hasAttachmentOnly: Boolean = false,
    val datePreset: SearchDatePreset = SearchDatePreset.ANY_TIME,
    val resultType: SearchResultType = SearchResultType.MESSAGES,
    val limit: Int = 50,
) {
    /** True when any filter beyond the text query constrains the search. */
    val hasActiveFilters: Boolean
        get() = category != null || priority != null || actionRequiredOnly ||
            !sender.isNullOrBlank() || companyId != null || unreadOnly ||
            hasAttachmentOnly || datePreset != SearchDatePreset.ANY_TIME
}

/**
 * Output of [QueryParser.parse]: a safe FTS5 MATCH expression plus the
 * original terms/phrases (kept for result highlighting, phase §45).
 *
 * [matchExpression] is null when there is no searchable text — the caller
 * must then run a filter-only query and must NOT issue a MATCH (phase §51).
 */
data class ParsedQuery(
    val matchExpression: String?,
    val terms: List<String>,
    val phrases: List<String>,
) {
    val isEmpty: Boolean get() = matchExpression == null
}

/**
 * One search hit. Message results are primary; thread results group hits
 * by conversation; sender/company results preserve the type distinction
 * (phase §8) and act as filter shortcuts in the UI.
 */
sealed interface SearchResult {

    data class Message(
        val record: MessageRecord,
        /** Deterministic bm25-based rank (lower is better); see [SearchRanking]. */
        val rank: Double,
        /** Safe display snippet with the match in context (phase §46). */
        val snippet: String,
        val classification: ClassificationRecord?,
        val priority: PriorityRecord?,
    ) : SearchResult

    data class Thread(
        val thread: ThreadRecord,
        /** Best (lowest) rank among the thread's matched messages. */
        val bestRank: Double,
        val matchedMessageCount: Int,
        /** Newest matched message, shown as the row preview. */
        val preview: MessageRecord,
    ) : SearchResult

    data class Sender(val sender: SenderRecord) : SearchResult

    data class Company(val company: CompanyRecord) : SearchResult
}

/** Everything the UI needs to render one search execution. */
data class SearchOutcome(
    val results: List<SearchResult>,
    val query: SearchQuery,
    val parsed: ParsedQuery,
    /** Measured end-to-end latency of the repository call (benchmark, phase §62). */
    val latencyMs: Long,
    /** Sender name/address matches for the "people" section (phase §8). */
    val senderSuggestions: List<SenderRecord> = emptyList(),
    /** Company-name matches for the "companies" section (phase §8). */
    val companySuggestions: List<CompanyRecord> = emptyList(),
)

/** Index health as observed by the UI (phase §53). */
enum class SearchIndexState {
    /** Index table exists and is at the current [SearchIndexVersions.CURRENT]. */
    READY,

    /** A (re)build or catch-up pass is running. Search still works. */
    INDEXING,

    /**
     * The FTS table is missing or unusable (e.g. FTS5 unavailable).
     * Search shows an error with a rebuild action instead of silently
     * returning incomplete results.
     */
    DEGRADED,
}
