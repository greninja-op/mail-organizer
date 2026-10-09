package com.greninjaop.mailorganizer.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations (Phase 2).
 *
 * v1 → v2 is purely additive: new columns on `accounts` (all with safe
 * defaults) plus the new mailbox tables and their indexes. No data is
 * dropped, rewritten, or backfilled — v1 databases only ever contained
 * account seeds from the Phase 0 foundation build.
 *
 * v2 → v3 (Phase 5) is purely additive: two nullable columns on `messages`
 * for the parser output (`bodyHtml`, `attachments`). Existing rows keep
 * their data; the new columns default to NULL/empty.
 *
 * v3 → v4 (Phase 8) is purely additive: one nullable column on `messages`
 * (`companyId`, the detected company link) plus its index. Existing rows
 * keep their data; `companyId` defaults to NULL ("not yet processed").
 *
 * v4 → v5 (Phase 10) is purely additive: the derived FTS5 search index
 * (`messages_fts`, created empty and backfilled from `messages` — never
 * requiring a Gmail resync) plus the `search_index_meta` version table.
 * No normalized data is touched.
 *
 * Production migrations must preserve user data; destructive fallback is
 * deliberately NOT enabled (see AppDatabase builder configuration).
 */
object Migrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ---- accounts: additive columns (defaults keep old rows valid) ----
            db.execSQL("ALTER TABLE accounts ADD COLUMN googleAccountId TEXT")
            db.execSQL("ALTER TABLE accounts ADD COLUMN provider TEXT NOT NULL DEFAULT 'google'")
            db.execSQL(
                "ALTER TABLE accounts ADD COLUMN connectionState TEXT NOT NULL " +
                    "DEFAULT 'DISCONNECTED'",
            )
            db.execSQL("ALTER TABLE accounts ADD COLUMN lastSyncEpochMs INTEGER")
            db.execSQL(
                "ALTER TABLE accounts ADD COLUMN updatedAtEpochMs INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL("ALTER TABLE accounts ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")

            // ---- threads ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS threads (" +
                    "threadId TEXT NOT NULL PRIMARY KEY, " +
                    "gmailThreadId TEXT, " +
                    "accountId TEXT NOT NULL, " +
                    "subject TEXT NOT NULL, " +
                    "participantDisplayNames TEXT NOT NULL, " +
                    "messageCount INTEGER NOT NULL DEFAULT 0, " +
                    "unreadCount INTEGER NOT NULL DEFAULT 0, " +
                    "latestMessageId TEXT, " +
                    "latestMessageEpochMs INTEGER NOT NULL DEFAULT 0, " +
                    "updatedAtEpochMs INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_threads_accountId ON threads(accountId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_threads_accountId_gmailThreadId " +
                    "ON threads(accountId, gmailThreadId)",
            )

            // ---- messages ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS messages (" +
                    "messageId TEXT NOT NULL PRIMARY KEY, " +
                    "gmailMessageId TEXT, " +
                    "threadId TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "fromAddress TEXT NOT NULL, " +
                    "fromName TEXT, " +
                    "toAddresses TEXT NOT NULL, " +
                    "ccAddresses TEXT NOT NULL, " +
                    "subject TEXT NOT NULL, " +
                    "snippet TEXT, " +
                    "bodyText TEXT, " +
                    "timestampEpochMs INTEGER NOT NULL, " +
                    "unread INTEGER NOT NULL DEFAULT 1, " +
                    "starred INTEGER NOT NULL DEFAULT 0, " +
                    "labels TEXT NOT NULL, " +
                    "sizeBytes INTEGER, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(threadId) REFERENCES threads(threadId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_accountId ON messages(accountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_threadId ON messages(threadId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_fromAddress ON messages(fromAddress)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_timestampEpochMs " +
                    "ON messages(timestampEpochMs)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_unread ON messages(unread)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_messages_accountId_gmailMessageId " +
                    "ON messages(accountId, gmailMessageId)",
            )

            // ---- senders ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS senders (" +
                    "senderId TEXT NOT NULL PRIMARY KEY, " +
                    "accountId TEXT NOT NULL, " +
                    "emailAddress TEXT NOT NULL, " +
                    "normalizedEmail TEXT NOT NULL, " +
                    "displayName TEXT, " +
                    "domain TEXT NOT NULL, " +
                    "firstSeenEpochMs INTEGER NOT NULL, " +
                    "lastSeenEpochMs INTEGER NOT NULL, " +
                    "messageCount INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_senders_accountId ON senders(accountId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_senders_accountId_normalizedEmail " +
                    "ON senders(accountId, normalizedEmail)",
            )

            // ---- companies ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS companies (" +
                    "companyId TEXT NOT NULL PRIMARY KEY, " +
                    "accountId TEXT NOT NULL, " +
                    "canonicalName TEXT NOT NULL, " +
                    "normalizedDomain TEXT NOT NULL, " +
                    "knownDomains TEXT NOT NULL, " +
                    "userOverrideName TEXT, " +
                    "pinned INTEGER NOT NULL DEFAULT 0, " +
                    "createdAtEpochMs INTEGER NOT NULL, " +
                    "updatedAtEpochMs INTEGER NOT NULL, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_companies_accountId ON companies(accountId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_companies_accountId_normalizedDomain " +
                    "ON companies(accountId, normalizedDomain)",
            )

            // ---- classifications ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS classifications (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "messageId TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "confidence REAL NOT NULL, " +
                    "source TEXT NOT NULL, " +
                    "version INTEGER NOT NULL, " +
                    "explanation TEXT, " +
                    "overridden INTEGER NOT NULL DEFAULT 0, " +
                    "classifiedAtEpochMs INTEGER NOT NULL, " +
                    "FOREIGN KEY(messageId) REFERENCES messages(messageId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_classifications_messageId ON classifications(messageId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_classifications_accountId " +
                    "ON classifications(accountId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_classifications_category " +
                    "ON classifications(category)",
            )

            // ---- priorities ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS priorities (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "messageId TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "priority TEXT NOT NULL, " +
                    "manualOverride INTEGER NOT NULL DEFAULT 0, " +
                    "reason TEXT, " +
                    "version INTEGER NOT NULL, " +
                    "updatedAtEpochMs INTEGER NOT NULL, " +
                    "FOREIGN KEY(messageId) REFERENCES messages(messageId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_priorities_messageId ON priorities(messageId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_priorities_accountId ON priorities(accountId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_priorities_priority ON priorities(priority)",
            )

            // ---- action_items ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS action_items (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "messageId TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "actionType TEXT NOT NULL, " +
                    "confidence REAL NOT NULL, " +
                    "explanation TEXT, " +
                    "dueDateEpochMs INTEGER, " +
                    "detectedAtEpochMs INTEGER NOT NULL, " +
                    "completed INTEGER NOT NULL DEFAULT 0, " +
                    "dismissed INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(messageId) REFERENCES messages(messageId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_action_items_messageId " +
                    "ON action_items(messageId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_action_items_accountId " +
                    "ON action_items(accountId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_action_items_dueDateEpochMs " +
                    "ON action_items(dueDateEpochMs)",
            )

            // ---- sync_state ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_state (" +
                    "accountId TEXT NOT NULL PRIMARY KEY, " +
                    "status TEXT NOT NULL DEFAULT 'NEVER_SYNCED', " +
                    "lastSuccessfulSyncEpochMs INTEGER, " +
                    "lastAttemptEpochMs INTEGER, " +
                    "cursor TEXT, " +
                    "errorCode TEXT, " +
                    "retryCount INTEGER NOT NULL DEFAULT 0, " +
                    "syncVersion INTEGER NOT NULL DEFAULT 1, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )

            // ---- user_rules ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS user_rules (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "accountId TEXT NOT NULL, " +
                    "ruleType TEXT NOT NULL, " +
                    "matcher TEXT NOT NULL, " +
                    "targetCategory TEXT, " +
                    "targetPriority TEXT, " +
                    "targetActionRequired INTEGER, " +
                    "enabled INTEGER NOT NULL DEFAULT 1, " +
                    "createdAtEpochMs INTEGER NOT NULL, " +
                    "updatedAtEpochMs INTEGER NOT NULL, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_user_rules_accountId " +
                    "ON user_rules(accountId)",
            )

            // ---- user_corrections ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS user_corrections (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "accountId TEXT NOT NULL, " +
                    "scope TEXT NOT NULL, " +
                    "scopeKey TEXT NOT NULL, " +
                    "field TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "createdAtEpochMs INTEGER NOT NULL, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_user_corrections_accountId " +
                    "ON user_corrections(accountId)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_user_corrections_accountId_scope_scopeKey_field " +
                    "ON user_corrections(accountId, scope, scopeKey, field)",
            )

            // ---- extracted_items ----
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS extracted_items (" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "messageId TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "itemType TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "payload TEXT, " +
                    "dueDateEpochMs INTEGER, " +
                    "detectedAtEpochMs INTEGER NOT NULL, " +
                    "completed INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(messageId) REFERENCES messages(messageId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_extracted_items_messageId " +
                    "ON extracted_items(messageId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_extracted_items_accountId " +
                    "ON extracted_items(accountId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_extracted_items_itemType " +
                    "ON extracted_items(itemType)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_extracted_items_dueDateEpochMs " +
                    "ON extracted_items(dueDateEpochMs)",
            )
        }
    }

    /**
     * v2 → v3 (Phase 5): parser output columns on `messages`.
     *
     * Purely additive — existing rows are untouched; `bodyHtml` defaults to
     * NULL and `attachments` to `''` (which [MoConverters] reads as an empty
     * list). No index changes: neither column is a query predicate.
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN bodyHtml TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN attachments TEXT NOT NULL DEFAULT ''")
        }
    }

    /**
     * v3 → v4 (Phase 8): company-intelligence link on `messages`.
     *
     * Purely additive — existing rows are untouched; `companyId` defaults
     * to NULL ("not yet processed by company intelligence"). The index
     * supports the company filter queries (`observeByCompany`,
     * per-category company counts).
     */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN companyId TEXT")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_companyId " +
                    "ON messages(companyId)",
            )
        }
    }

    /**
     * v4 → v5 (Phase 10): local search index.
     *
     * - `messages_fts`: standalone FTS5 virtual table holding the
     *   searchable document per message (see [SearchIndexStore]). It is
     *   *derived* data — created empty here and backfilled from `messages`
     *   by `SearchIndexUseCase` on next launch; no Gmail resynchronization
     *   is ever required (phase §22, §54).
     * - `search_index_meta`: Room-managed version table so a future index
     *   schema change triggers a rebuild instead of silent staleness
     *   (phase §23).
     *
     * Purely additive — no normalized email, classification, sender,
     * company, or priority data is touched.
     */
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(SearchIndexStore.CREATE_SQL)
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS search_index_meta (" +
                    "accountId TEXT NOT NULL PRIMARY KEY, " +
                    "version INTEGER NOT NULL)",
            )
        }
    }
}
