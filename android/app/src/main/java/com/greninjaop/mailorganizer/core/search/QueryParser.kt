package com.greninjaop.mailorganizer.core.search

/**
 * Structured query parser (Phase 10, phase §13–§17).
 *
 * Turns raw user text into a safe FTS5 MATCH expression. Total function —
 * never throws, never returns SQL. Safety properties (phase §48–§50):
 *
 * - Every term/phrase is wrapped in double quotes, so FTS5 keywords
 *   (`OR`, `AND`, `NOT`, `NEAR`) and special characters (`*`, `(`, `)`,
 *   `:`, `^`) inside user input are always literal — they can never
 *   alter the query structure or escape a parameter boundary.
 * - Terms are joined with AND (documented, predictable — phase §15).
 *   `"job interview"` (quoted) is a phrase; `job interview` (unquoted)
 *   requires both terms.
 * - Bounded: input is truncated to [MAX_QUERY_CHARS] and [MAX_TERMS]
 *   terms — a pathological query can neither freeze the app nor blow up
 *   the MATCH expression (phase §50).
 * - Tokens with no word characters (`***`, `;--`, `'`) are dropped; when
 *   nothing searchable remains the result is [ParsedQuery.isEmpty] and the
 *   caller must show the landing state instead of querying (phase §51).
 * - Case-insensitivity and Unicode handling come from the FTS5
 *   `unicode61 remove_diacritics 1` tokenizer, not from here (phase §17,
 *   §18): `AMAZON` ≡ `amazon`; `cafe` matches `café`; Malayalam text
 *   matches verbatim.
 */
object QueryParser {

    /** Hard cap on raw input length (phase §50). */
    const val MAX_QUERY_CHARS = 200

    /** Hard cap on terms+phrases per query (phase §50). */
    const val MAX_TERMS = 10

    private val PHRASE_REGEX = Regex("\"([^\"]{1,100})\"")

    /**
     * True when the token carries at least one letter or digit — used to
     * drop pure-punctuation tokens that would otherwise produce empty or
     * meaningless FTS5 phrases.
     */
    private val HAS_WORD_CHAR = Regex("[\\p{L}\\p{N}]")

    /**
     * Parses [raw] into a [ParsedQuery]. Never throws: any input —
     * including SQL-injection probes and pathological Unicode — degrades
     * to an empty query or a safely quoted expression.
     */
    fun parse(raw: String): ParsedQuery {
        val text = raw.trim().take(MAX_QUERY_CHARS)
        if (text.isEmpty()) return ParsedQuery(null, emptyList(), emptyList())

        val phrases = PHRASE_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .filter { HAS_WORD_CHAR.containsMatchIn(it) }
            .take(MAX_TERMS)
            .toList()

        val rest = PHRASE_REGEX.replace(text, " ")
        val terms = rest.split(Regex("\\s+"))
            .map { it.replace("\"", "") }
            .filter { it.isNotEmpty() && HAS_WORD_CHAR.containsMatchIn(it) }
            .take(MAX_TERMS - phrases.size.coerceAtMost(MAX_TERMS))

        val parts = buildList {
            phrases.forEach { add(quote(it)) }
            terms.forEach { add(quote(it)) }
        }
        if (parts.isEmpty()) return ParsedQuery(null, emptyList(), emptyList())
        return ParsedQuery(
            matchExpression = parts.joinToString(" AND "),
            terms = terms,
            phrases = phrases,
        )
    }

    /**
     * Wraps one term/phrase in FTS5 double quotes after stripping embedded
     * quotes. Inside quotes every character is literal to FTS5.
     */
    private fun quote(token: String): String = "\"${token.replace("\"", "")}\""
}
