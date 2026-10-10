package com.greninjaop.mailorganizer.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.greninjaop.mailorganizer.core.search.SearchRanking

/**
 * SQLite FTS5 search-index storage (Phase 10, phase §20–§25).
 *
 * The index is derived data: normalized `messages` rows remain the source
 * of truth, and every FTS row can be rebuilt from them (§22, §54). Design:
 *
 * - `messages_fts` is a standalone FTS5 virtual table (NOT a Room entity —
 *   Room never validates or manages it; all access here is raw SQL). It is
 *   created by [Migrations.MIGRATION_4_5] on upgrade and by the database
 *   `onCreate` callback on fresh installs.
 * - `messageId`/`accountId` are UNINDEXED bookkeeping columns: stored and
 *   filterable, never searchable — searching `acc-1` must not match every
 *   row of that account (phase §39).
 * - Identity is `messageId` (stable across re-syncs, phase §25): indexing
 *   is DELETE-then-INSERT, so repeated indexing never duplicates rows.
 * - Tokenizer `unicode61 remove_diacritics 1`: case-insensitive,
 *   Unicode-aware, accent-folding (`cafe` matches `café`, Malayalam
 *   matches verbatim).
 * - Callers run mutations inside a Room transaction together with the
 *   message write, so the index always reflects committed data (§57, §58).
 *
 * FTS5 availability is probed by [ensureCreated]: if the device SQLite
 * lacks FTS5, a typed [FtsUnavailableException] is thrown and callers
 * degrade to the honest error state (phase §53) instead of silently
 * returning incomplete results.
 */
class SearchIndexStore(private val db: SupportSQLiteDatabase) {

    companion object {
        const val TABLE = "messages_fts"

        /**
         * Index schema version (phase §23). Bump when the indexed columns,
         * tokenizer, or document assembly change; [domain.search.SearchIndexUseCase]
         * rebuilds stale accounts automatically.
         */
        const val INDEX_VERSION = 1

        /**
         * Canonical DDL. Column order must match
         * `com.greninjaop.mailorganizer.core.search.SearchRanking.COLUMN_ORDER`
         * exactly — bm25 weights are positional.
         */
        const val CREATE_SQL =
            "CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts5(" +
                "messageId UNINDEXED, accountId UNINDEXED, " +
                "subject, bodyText, fromName, fromAddress, snippet, companyName, labelsText, " +
                "tokenize='unicode61 remove_diacritics 1')"

        /** Indexed body text is capped (bounded index growth, phase §50). */
        const val MAX_INDEXED_BODY_CHARS = 20_000
    }

    /** One searchable document assembled from a message row + company name. */
    data class FtsDocument(
        val messageId: String,
        val accountId: String,
        val subject: String,
        val bodyText: String?,
        val fromName: String?,
        val fromAddress: String,
        val snippet: String?,
        val companyName: String?,
        /** Space-joined Gmail labels (e.g. "INBOX CATEGORY_PROMOTIONS"). */
        val labelsText: String,
    ) {
        companion object {
            fun fromMessage(message: MessageRecord, companyName: String?): FtsDocument =
                FtsDocument(
                    messageId = message.messageId,
                    accountId = message.accountId,
                    subject = message.subject,
                    bodyText = message.bodyText?.take(MAX_INDEXED_BODY_CHARS),
                    fromName = message.fromName,
                    fromAddress = message.fromAddress,
                    snippet = message.snippet,
                    companyName = companyName,
                    labelsText = message.labels.joinToString(" "),
                )
        }
    }

    /**
     * Creates the FTS table if missing. Throws [FtsUnavailableException]
     * when the device SQLite cannot create FTS5 tables.
     */
    fun ensureCreated() {
        try {
            db.execSQL(CREATE_SQL)
        } catch (t: Throwable) {
            throw FtsUnavailableException(
                "SQLite FTS5 unavailable: ${t.message}",
                t,
            )
        }
    }

    /**
     * Idempotent upsert of one document: DELETE-then-INSERT keyed on the
     * stable [FtsDocument.messageId] (phase §25). Null/blank text becomes
     * NULL so it contributes no tokens.
     */
    fun indexDocument(doc: FtsDocument) {
        indexDocuments(listOf(doc))
    }

    /**
     * Batch idempotent upsert of multiple documents using compiled statements
     * (Phase 24 performance optimization).
     */
    fun indexDocuments(docs: List<FtsDocument>) {
        if (docs.isEmpty()) return
        val delStmt = db.compileStatement("DELETE FROM messages_fts WHERE messageId = ?")
        val insStmt = db.compileStatement(
            "INSERT INTO messages_fts " +
                "(messageId, accountId, subject, bodyText, fromName, fromAddress, " +
                "snippet, companyName, labelsText) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        try {
            for (doc in docs) {
                delStmt.bindString(1, doc.messageId)
                delStmt.executeUpdateDelete()

                insStmt.bindString(1, doc.messageId)
                insStmt.bindString(2, doc.accountId)
                insStmt.bindString(3, doc.subject)
                val body = doc.bodyText?.ifBlank { null }
                if (body != null) insStmt.bindString(4, body) else insStmt.bindNull(4)
                val fromName = doc.fromName?.ifBlank { null }
                if (fromName != null) insStmt.bindString(5, fromName) else insStmt.bindNull(5)
                insStmt.bindString(6, doc.fromAddress)
                val snippet = doc.snippet?.ifBlank { null }
                if (snippet != null) insStmt.bindString(7, snippet) else insStmt.bindNull(7)
                val company = doc.companyName?.ifBlank { null }
                if (company != null) insStmt.bindString(8, company) else insStmt.bindNull(8)
                val labels = doc.labelsText.ifBlank { null }
                if (labels != null) insStmt.bindString(9, labels) else insStmt.bindNull(9)
                insStmt.executeInsert()
            }
        } finally {
            try { delStmt.close() } catch (_: Throwable) {}
            try { insStmt.close() } catch (_: Throwable) {}
        }
    }

    /** Removes one document (message deleted locally, phase §24). */
    fun deleteDocument(messageId: String) {
        db.execSQL("DELETE FROM messages_fts WHERE messageId = ?", arrayOf<Any?>(messageId))
    }

    /** Removes every document of one account (account removed / rebuild). */
    fun deleteForAccount(accountId: String) {
        db.execSQL("DELETE FROM messages_fts WHERE accountId = ?", arrayOf<Any?>(accountId))
    }

    /** Removes all documents across all accounts (data purge / rebuild). */
    fun clearAll() {
        db.execSQL("DELETE FROM messages_fts")
    }

    /** Number of indexed documents for one account. */
    fun countForAccount(accountId: String): Int {
        db.query("SELECT COUNT(*) FROM messages_fts WHERE accountId = ?", arrayOf<Any?>(accountId))
            .use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    /** Total number of indexed documents across all accounts. */
    fun countAll(): Int {
        db.query("SELECT COUNT(*) FROM messages_fts")
            .use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    /**
     * Message ids with no FTS row yet (incremental indexing, phase §24).
     * Bounded; newest first so fresh mail becomes searchable first.
     */
    fun unindexedMessageIds(accountId: String, limit: Int): List<String> {
        val ids = mutableListOf<String>()
        db.query(
            "SELECT m.messageId FROM messages m LEFT JOIN messages_fts f " +
                "ON f.messageId = m.messageId " +
                "WHERE m.accountId = ? AND f.messageId IS NULL " +
                "ORDER BY m.timestampEpochMs DESC LIMIT ?",
            arrayOf<Any?>(accountId, limit),
        ).use { c ->
            while (c.moveToNext()) ids.add(c.getString(0))
        }
        return ids
    }

    /**
     * Message ids whose FTS row lacks a company name while the message is
     * now attributed (company intelligence ran after the first index pass).
     * Bounded; keeps company search truthful without a full rebuild.
     */
    fun idsMissingCompanyName(accountId: String, limit: Int): List<String> {
        val ids = mutableListOf<String>()
        db.query(
            "SELECT m.messageId FROM messages m JOIN messages_fts f " +
                "ON f.messageId = m.messageId " +
                "WHERE m.accountId = ? AND m.companyId IS NOT NULL " +
                "AND f.companyName IS NULL LIMIT ?",
            arrayOf<Any?>(accountId, limit),
        ).use { c ->
            while (c.moveToNext()) ids.add(c.getString(0))
        }
        return ids
    }

    /** True when the FTS table exists (cheap existence probe). */
    fun tableExists(): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='messages_fts'",
            emptyArray(),
        ).use { c -> return c.count > 0 }
    }
}

/** The device SQLite cannot provide FTS5 — search degrades honestly (§53). */
class FtsUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
