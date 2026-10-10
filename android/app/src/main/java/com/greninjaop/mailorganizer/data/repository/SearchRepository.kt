package com.greninjaop.mailorganizer.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.search.ParsedQuery
import com.greninjaop.mailorganizer.core.search.QueryParser
import com.greninjaop.mailorganizer.core.search.SearchDatePreset
import com.greninjaop.mailorganizer.core.search.SearchHighlight
import com.greninjaop.mailorganizer.core.search.SearchIndexState
import com.greninjaop.mailorganizer.core.search.SearchOutcome
import com.greninjaop.mailorganizer.core.search.SearchQuery
import com.greninjaop.mailorganizer.core.search.SearchRanking
import com.greninjaop.mailorganizer.core.search.SearchResult
import com.greninjaop.mailorganizer.core.search.SearchResultType
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.SearchIndexStore
import java.util.Calendar
import kotlinx.coroutines.withContext

/**
 * Local search over synchronized mail (Phase 10).
 *
 * Contract:
 * - Everything is local: FTS5 index + Room. No network, no Gmail API, no
 *   AI (phase §5, §6, §38).
 * - Account isolation is structural: every query is scoped to
 *   [SearchQuery.accountId] (phase §26).
 * - No SQL is ever built from user text: the text becomes a [QueryParser]
 *   MATCH expression; every other value is a bound parameter (phase §48).
 * - Ranking is deterministic bm25 + documented tiebreak ([SearchRanking]).
 */
interface SearchRepository {
    /** Executes [query]; never throws for empty queries (landing state). */
    suspend fun search(query: SearchQuery): SearchOutcome

    /** Current health of the account's search index (phase §53). */
    suspend fun indexState(accountId: String): SearchIndexState
}

class RoomSearchRepository(
    private val db: AppDatabase,
    private val mail: MailRepository,
    private val intelligence: IntelligenceRepository,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : SearchRepository {

    companion object {
        /** Hard cap on results per search (bounded, phase §50). */
        const val MAX_LIMIT = 200

        /** Max sender/company suggestion rows per search. */
        const val SUGGESTION_LIMIT = 3
    }

    override suspend fun search(query: SearchQuery): SearchOutcome =
        withContext(dispatchers.io) {
            val start = clock()
            val parsed = QueryParser.parse(query.rawText)
            if (parsed.isEmpty && !query.hasActiveFilters) {
                // Landing state: no DB work at all (phase §51).
                return@withContext SearchOutcome(
                    results = emptyList(),
                    query = query,
                    parsed = parsed,
                    latencyMs = clock() - start,
                )
            }
            val outcome = try {
                runSearch(query, parsed)
            } catch (t: Throwable) {
                // FTS missing/corrupt → caller shows the honest error state
                // with a rebuild action (phase §53), never fake results.
                throw SearchFailedException("Search failed: ${t.message}", t)
            }
            outcome.copy(latencyMs = clock() - start)
        }

    private suspend fun runSearch(query: SearchQuery, parsed: ParsedQuery): SearchOutcome {
        val limit = query.limit.coerceIn(1, MAX_LIMIT)
        val (sql, args) = buildSql(query, parsed, limit)
        val hits = mutableListOf<Pair<String, Double>>()
        db.openHelper.writableDatabase
            .query(SimpleSQLiteQuery(sql, args.toTypedArray()))
            .use { c ->
                while (c.moveToNext()) hits.add(c.getString(0) to c.getDouble(1))
            }
        val ids = hits.map { it.first }
        val records = mail.getMessagesByIds(ids).associateBy { it.messageId }
        // Defensive: drop any index row pointing at a missing message (§58).
        val ordered = hits.mapNotNull { (id, rank) -> records[id]?.let { it to rank } }
        val classifications = intelligence.getClassifications(ids)
        val priorities = intelligence.getPriorities(ids)

        val results: List<SearchResult> = when (query.resultType) {
            SearchResultType.MESSAGES -> ordered.map { (record, rank) ->
                SearchResult.Message(
                    record = record,
                    rank = rank,
                    snippet = SearchHighlight.buildSnippet(
                        bodyText = record.bodyText,
                        fallback = record.snippet,
                        terms = parsed.terms,
                        phrases = parsed.phrases,
                    ),
                    classification = classifications[record.messageId],
                    priority = priorities[record.messageId],
                )
            }
            SearchResultType.THREADS -> buildThreadResults(ordered)
        }

        val (senders, companies) = suggestPeople(query, parsed)
        return SearchOutcome(
            results = results,
            query = query,
            parsed = parsed,
            latencyMs = 0,
            senderSuggestions = senders,
            companySuggestions = companies,
        )
    }

    private suspend fun buildThreadResults(
        ordered: List<Pair<com.greninjaop.mailorganizer.data.local.MessageRecord, Double>>,
    ): List<SearchResult.Thread> {
        if (ordered.isEmpty()) return emptyList()
        val byThread = ordered.groupBy { it.first.threadId }
        val threads = db.threadDao().getByIds(byThread.keys.toList())
            .associateBy { it.threadId }
        return byThread.mapNotNull { (threadId, hits) ->
            val thread = threads[threadId] ?: return@mapNotNull null
            val preview = hits.maxByOrNull { it.first.timestampEpochMs }?.first
                ?: return@mapNotNull null
            SearchResult.Thread(
                thread = thread,
                bestRank = hits.minOf { it.second },
                matchedMessageCount = hits.size,
                preview = preview,
            )
        }.sortedWith(
            compareBy<SearchResult.Thread> { it.bestRank }
                .thenByDescending { it.preview.timestampEpochMs }
                .thenBy { it.thread.threadId },
        )
    }

    private suspend fun suggestPeople(
        query: SearchQuery,
        parsed: ParsedQuery,
    ): Pair<List<com.greninjaop.mailorganizer.data.local.SenderRecord>, List<com.greninjaop.mailorganizer.data.local.CompanyRecord>> {
        val seed = parsed.terms.firstOrNull() ?: parsed.phrases.firstOrNull()
        if (seed.isNullOrBlank()) return emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>() to emptyList()
        val like = "%${seed.take(60)}%"
        val senders = if (query.accountId != null) {
            db.senderDao().suggestByText(query.accountId, like, SUGGESTION_LIMIT)
        } else {
            db.senderDao().suggestByTextUnified(like, SUGGESTION_LIMIT)
        }
        val companies = if (query.accountId != null) {
            db.companyDao().suggestByText(query.accountId, like, SUGGESTION_LIMIT)
        } else {
            db.companyDao().suggestByTextUnified(like, SUGGESTION_LIMIT)
        }
        return senders to companies
    }

    /**
     * Builds the search SQL. Table/column names are constants; the MATCH
     * expression comes from [QueryParser] (already quoted); every user
     * value is a `?` bound parameter (phase §48).
     */
    private fun buildSql(
        query: SearchQuery,
        parsed: ParsedQuery,
        limit: Int,
    ): Pair<String, List<Any?>> {
        val args = mutableListOf<Any?>()
        val filters = StringBuilder()
        query.category?.let {
            filters.append(" AND c.category = ?")
            args.add(it.name)
        }
        query.priority?.let {
            filters.append(" AND p.priority = ?")
            args.add(it.name)
        }
        if (query.actionRequiredOnly) {
            filters.append(" AND c.category = ?")
            args.add(MailCategory.ACTION_REQUIRED.name)
        }
        query.sender?.takeIf { it.isNotBlank() }?.let {
            filters.append(
                " AND (m.fromAddress LIKE '%' || ? || '%' COLLATE NOCASE" +
                    " OR m.fromName LIKE '%' || ? || '%' COLLATE NOCASE)",
            )
            args.add(it)
            args.add(it)
        }
        query.companyId?.let {
            filters.append(" AND m.companyId = ?")
            args.add(it)
        }
        if (query.unreadOnly) filters.append(" AND m.unread = 1")
        if (query.hasAttachmentOnly) {
            // Empty attachment lists encode as "" (MoConverters).
            filters.append(" AND m.attachments IS NOT NULL AND m.attachments != ''")
        }
        val (from, to) = dateBounds(query.datePreset)
        from?.let {
            filters.append(" AND m.timestampEpochMs >= ?")
            args.add(it)
        }
        to?.let {
            filters.append(" AND m.timestampEpochMs <= ?")
            args.add(it)
        }

        val joins =
            "LEFT JOIN classifications c ON c.messageId = m.messageId " +
                "LEFT JOIN priorities p ON p.messageId = m.messageId "

        return if (parsed.matchExpression != null && isFtsAvailable()) {
            val weights = SearchRanking.bm25WeightList()
            val sql = StringBuilder()
                .append("SELECT m.messageId AS mid, bm25(messages_fts, ")
                .append(weights)
                .append(") AS r FROM messages_fts ")
                .append("JOIN messages m ON m.messageId = messages_fts.messageId ")
                .append(joins)
            val allArgs = mutableListOf<Any?>()
            if (query.accountId != null) {
                sql.append("WHERE messages_fts.accountId = ? AND messages_fts MATCH ?")
                allArgs.add(query.accountId)
            } else {
                sql.append("JOIN accounts a ON a.accountId = messages_fts.accountId ")
                sql.append("WHERE a.isEnabled = 1 AND messages_fts MATCH ?")
            }
            allArgs.add(parsed.matchExpression)
            allArgs.addAll(args)
            allArgs.add(limit)
            sql.append(filters)
                .append(" ORDER BY r, ")
                .append(SearchRanking.TIEBREAK_SQL)
                .append(" LIMIT ?")
            sql.toString() to allArgs
        } else {
            val sql = StringBuilder()
                .append("SELECT m.messageId AS mid, 0.0 AS r FROM messages m ")
                .append(joins)
            val allArgs = mutableListOf<Any?>()
            if (query.accountId != null) {
                sql.append("WHERE m.accountId = ?")
                allArgs.add(query.accountId)
            } else {
                sql.append("JOIN accounts a ON a.accountId = m.accountId WHERE a.isEnabled = 1")
            }
            allArgs.addAll(args)
            if (parsed.terms.isNotEmpty() || parsed.phrases.isNotEmpty()) {
                val likeParts = mutableListOf<String>()
                for (term in parsed.terms) {
                    likeParts.add("(m.subject LIKE '%' || ? || '%' COLLATE NOCASE OR m.bodyText LIKE '%' || ? || '%' COLLATE NOCASE OR m.fromAddress LIKE '%' || ? || '%' COLLATE NOCASE)")
                    allArgs.add(term)
                    allArgs.add(term)
                    allArgs.add(term)
                }
                for (phrase in parsed.phrases) {
                    likeParts.add("(m.subject LIKE '%' || ? || '%' COLLATE NOCASE OR m.bodyText LIKE '%' || ? || '%' COLLATE NOCASE OR m.fromAddress LIKE '%' || ? || '%' COLLATE NOCASE)")
                    allArgs.add(phrase)
                    allArgs.add(phrase)
                    allArgs.add(phrase)
                }
                sql.append(" AND (").append(likeParts.joinToString(" AND ")).append(")")
            }
            allArgs.add(limit)
            sql.append(filters)
                .append(" ORDER BY m.timestampEpochMs DESC, m.messageId ASC LIMIT ?")
            sql.toString() to allArgs
        }
    }

    private fun isFtsAvailable(): Boolean {
        return try {
            SearchIndexStore(db.openHelper.writableDatabase).tableExists()
        } catch (_: Throwable) {
            false
        }
    }

    private fun dateBounds(preset: SearchDatePreset): Pair<Long?, Long?> {
        val now = clock()
        return when (preset) {
            SearchDatePreset.ANY_TIME -> null to null
            SearchDatePreset.TODAY -> startOfLocalDay(now) to now
            SearchDatePreset.LAST_7_DAYS -> now - 7 * 86_400_000L to now
            SearchDatePreset.LAST_30_DAYS -> now - 30 * 86_400_000L to now
        }
    }

    private fun startOfLocalDay(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override suspend fun indexState(accountId: String): SearchIndexState =
        withContext(dispatchers.io) {
            try {
                val store = SearchIndexStore(db.openHelper.writableDatabase)
                if (!store.tableExists()) return@withContext SearchIndexState.DEGRADED
                val version = db.searchIndexMetaDao().getVersion(accountId)
                if (version == SearchIndexStore.INDEX_VERSION) SearchIndexState.READY
                else SearchIndexState.INDEXING
            } catch (t: Throwable) {
                SearchIndexState.DEGRADED
            }
        }
}

/** Search failed at the storage layer — the UI shows error + rebuild (§53). */
class SearchFailedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
