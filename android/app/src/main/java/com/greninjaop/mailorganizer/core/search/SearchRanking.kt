package com.greninjaop.mailorganizer.core.search

/**
 * Deterministic ranking contract (Phase 10, phase §37–§38, §41–§42).
 *
 * No embeddings, no remote calls, no AI: ranking is SQLite FTS5 `bm25()`
 * with fixed per-column weights over [COLUMN_ORDER], then a deterministic
 * tiebreak. Same index + same query ⇒ same order, every device, every run.
 *
 * Weight rationale (documented per phase §41–§42):
 * - subject (4.0): a subject match outranks a deep body match.
 * - fromName / fromAddress (3.0 each): sender matches are strongly
 *   relevant, but do not blindly beat a direct subject match.
 * - companyName (2.5): a known-company match ranks appropriately.
 * - snippet (2.0): the snippet is curated summary text.
 * - bodyText (1.0): body matches count, but one mention in a long quoted
 *   thread must not outrank a subject hit.
 * - labelsText (1.0): Gmail-label tokens are a weak relevance signal.
 *
 * `messageId`/`accountId` are UNINDEXED bookkeeping columns; their weights
 * are 1.0 placeholders (ignored by bm25 — UNINDEXED columns contribute no
 * tokens). The weight list must cover every FTS column in order; see
 * [bm25WeightList].
 *
 * Tiebreak (total order): bm25 rank ASC, then timestamp DESC (recency),
 * then messageId ASC (stability).
 */
object SearchRanking {

    /**
     * FTS column order. Must match `messages_fts` DDL in
     * [com.greninjaop.mailorganizer.data.local.SearchIndexStore] exactly —
     * bm25 weights are positional.
     */
    val COLUMN_ORDER: List<String> = listOf(
        "messageId", // UNINDEXED
        "accountId", // UNINDEXED
        "subject",
        "bodyText",
        "fromName",
        "fromAddress",
        "snippet",
        "companyName",
        "labelsText",
    )

    /** Positional bm25 weights, one per [COLUMN_ORDER] entry. */
    val WEIGHTS: List<Double> = listOf(
        1.0, // messageId (UNINDEXED — ignored)
        1.0, // accountId (UNINDEXED — ignored)
        4.0, // subject
        1.0, // bodyText
        3.0, // fromName
        3.0, // fromAddress
        2.0, // snippet
        2.5, // companyName
        1.0, // labelsText
    )

    init {
        require(COLUMN_ORDER.size == WEIGHTS.size) {
            "bm25 weights must cover every FTS column positionally"
        }
    }

    /** `bm25(messages_fts, w0, w1, …)` fragment for ORDER BY clauses. */
    fun bm25WeightList(): String = WEIGHTS.joinToString(", ")

    /** Deterministic tiebreak applied after the bm25 rank. */
    const val TIEBREAK_SQL = "m.timestampEpochMs DESC, m.messageId ASC"
}
