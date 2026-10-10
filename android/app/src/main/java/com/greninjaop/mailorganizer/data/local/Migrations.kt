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
            try {
                db.execSQL(SearchIndexStore.CREATE_SQL)
            } catch (_: Throwable) {
                // Handled gracefully: device SQLite may lack FTS5 extension (SearchIndexStore probes availability)
            }
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS search_index_meta (" +
                    "accountId TEXT NOT NULL PRIMARY KEY, " +
                    "version INTEGER NOT NULL)",
            )
        }
    }

    /**
     * v5 → v6 (Phase 12): user rules & corrections engine columns.
     *
     * - `user_rules`: structured rule vocabulary — `name`, `conditionsJson`,
     *   `actionsJson` (see `RuleJson`), explicit `ruleOrder` precedence,
     *   `ruleVersion`, and `source`. Legacy columns stay untouched; existing
     *   rows get `ruleOrder = id` so their relative precedence is the
     *   deterministic insertion order they already had.
     *
     * Purely additive — no mail, classification, sender, company, priority,
     * or search data is touched.
     */
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_rules ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE user_rules ADD COLUMN conditionsJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE user_rules ADD COLUMN actionsJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE user_rules ADD COLUMN ruleOrder INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE user_rules ADD COLUMN ruleVersion INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE user_rules ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
            db.execSQL("UPDATE user_rules SET ruleOrder = id")
        }
    }

    /**
     * Phase 14: action-engine columns on `action_items` (all additive).
     * Backfills the new [ActionStatus] from the legacy completed/dismissed
     * booleans and the thread id from the parent message row.
     */
    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE action_items ADD COLUMN threadId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE action_items ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE action_items ADD COLUMN description TEXT")
            db.execSQL("ALTER TABLE action_items ADD COLUMN urgency TEXT NOT NULL DEFAULT 'NORMAL'")
            db.execSQL("ALTER TABLE action_items ADD COLUMN source TEXT NOT NULL DEFAULT 'CLASSIFICATION'")
            db.execSQL("ALTER TABLE action_items ADD COLUMN status TEXT NOT NULL DEFAULT 'SUGGESTED'")
            db.execSQL("ALTER TABLE action_items ADD COLUMN externalEffect TEXT NOT NULL DEFAULT 'NONE'")
            db.execSQL("ALTER TABLE action_items ADD COLUMN payloadJson TEXT")
            db.execSQL("ALTER TABLE action_items ADD COLUMN updatedAtEpochMs INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE action_items ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_action_items_threadId ON action_items(threadId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_action_items_status ON action_items(status)")
            db.execSQL(
                "UPDATE action_items SET status = CASE " +
                    "WHEN completed = 1 THEN 'COMPLETED' " +
                    "WHEN dismissed = 1 THEN 'DISMISSED' " +
                    "ELSE 'SUGGESTED' END",
            )
            db.execSQL(
                "UPDATE action_items SET threadId = COALESCE(" +
                    "(SELECT threadId FROM messages WHERE messages.messageId = action_items.messageId), '') " +
                    "WHERE threadId = ''",
            )
            db.execSQL("UPDATE action_items SET updatedAtEpochMs = detectedAtEpochMs")
        }
    }

    /**
     * v7 → v8 (Phase 17): integration metadata table.
     *
     * Creates `integration_states` — last-known Integration Manager
     * snapshots, metadata only. No credentials are ever stored here
     * (phase §35); no existing data is touched.
     */
    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS integration_states (" +
                    "integrationId TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "lastStatus TEXT NOT NULL, " +
                    "statusReason TEXT, " +
                    "updatedAtEpochMs INTEGER NOT NULL, " +
                    "configVersion INTEGER NOT NULL, " +
                    "PRIMARY KEY(integrationId, accountId))",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_integration_states_accountId " +
                    "ON integration_states(accountId)",
            )
        }
    }

    /**
     * v8 → v9 (Phase 24): performance indexes.
     *
     * Composite indexes targeting high-frequency query and sort paths:
     * - messages(accountId, timestampEpochMs): mailbox recency list
     * - messages(accountId, threadId): account-scoped thread message lookups
     * - messages(threadId, timestampEpochMs): thread message rendering order
     * - messages(accountId, unread, timestampEpochMs): unread inbox queries
     * - messages(accountId, starred, timestampEpochMs): starred queries
     * - threads(accountId, latestMessageEpochMs): conversation list ordering
     * - classifications(accountId, category): category chip filtering
     * - priorities(accountId, priority): priority filtering
     * - action_items(accountId, status): action item status queries
     * - extracted_items(accountId, itemType): structured entity filtering
     *
     * Purely additive — no data modified.
     */
    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_accountId_timestampEpochMs " +
                    "ON messages(accountId, timestampEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_accountId_threadId " +
                    "ON messages(accountId, threadId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_threadId_timestampEpochMs " +
                    "ON messages(threadId, timestampEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_accountId_unread_timestampEpochMs " +
                    "ON messages(accountId, unread, timestampEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_messages_accountId_starred_timestampEpochMs " +
                    "ON messages(accountId, starred, timestampEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_threads_accountId_latestMessageEpochMs " +
                    "ON threads(accountId, latestMessageEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_classifications_accountId_category " +
                    "ON classifications(accountId, category)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_priorities_accountId_priority " +
                    "ON priorities(accountId, priority)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_action_items_accountId_status " +
                    "ON action_items(accountId, status)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_extracted_items_accountId_itemType " +
                    "ON extracted_items(accountId, itemType)",
            )
        }
    }

    /**
     * v9 → v10 (Phase 27): Advanced Automation engine tables.
     *
     * Creates `automation_rules` and `automation_execution_history`.
     * Purely additive: no existing data is touched.
     */
    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS automation_rules (" +
                    "ruleId TEXT NOT NULL PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "scopeType TEXT NOT NULL, " +
                    "targetAccountId TEXT, " +
                    "state TEXT NOT NULL, " +
                    "triggerType TEXT NOT NULL, " +
                    "triggerParam TEXT, " +
                    "conditionGroupJson TEXT NOT NULL, " +
                    "actionsJson TEXT NOT NULL, " +
                    "confirmationPolicy TEXT NOT NULL, " +
                    "createdAtEpochMs INTEGER NOT NULL, " +
                    "updatedAtEpochMs INTEGER NOT NULL, " +
                    "lastRunEpochMs INTEGER, " +
                    "failureCount INTEGER NOT NULL, " +
                    "version INTEGER NOT NULL DEFAULT 1, " +
                    "FOREIGN KEY(targetAccountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_rules_targetAccountId " +
                    "ON automation_rules(targetAccountId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_rules_state " +
                    "ON automation_rules(state)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_rules_triggerType " +
                    "ON automation_rules(triggerType)",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS automation_execution_history (" +
                    "executionId TEXT NOT NULL PRIMARY KEY, " +
                    "automationId TEXT NOT NULL, " +
                    "automationName TEXT NOT NULL, " +
                    "accountId TEXT NOT NULL, " +
                    "triggerType TEXT NOT NULL, " +
                    "sourceMessageId TEXT, " +
                    "sourceThreadId TEXT, " +
                    "actionSummary TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "executedAtEpochMs INTEGER NOT NULL, " +
                    "detailMessage TEXT, " +
                    "FOREIGN KEY(automationId) REFERENCES automation_rules(ruleId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(accountId) REFERENCES accounts(accountId) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_execution_history_automationId " +
                    "ON automation_execution_history(automationId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_execution_history_accountId " +
                    "ON automation_execution_history(accountId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_execution_history_executedAtEpochMs " +
                    "ON automation_execution_history(executedAtEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_automation_history_dedup " +
                    "ON automation_execution_history(accountId, sourceMessageId, triggerType)",
            )
        }
    }
}

