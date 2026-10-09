package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Email message persistence (Phase 2).
 *
 * All list queries are bounded ([limit]) — the data layer never loads an
 * entire mailbox into memory (phase §41). Pagination cursors arrive with the
 * sync engine (Phase 4); until then callers page with explicit limits.
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageRecord>)

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getById(messageId: String): MessageRecord?

    /**
     * Latest-message rows for the visible thread page (Phase 6): one bounded
     * query instead of N+1 per-row lookups. Never call with an empty list
     * (Room rejects empty IN clauses) — callers guard.
     */
    @Query("SELECT * FROM messages WHERE messageId IN (:messageIds)")
    suspend fun getByIds(messageIds: List<String>): List<MessageRecord>

    @Query(
        "SELECT * FROM messages WHERE threadId = :threadId " +
            "ORDER BY timestampEpochMs ASC LIMIT :limit",
    )
    fun observeByThread(threadId: String, limit: Int): Flow<List<MessageRecord>>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeByAccount(accountId: String, limit: Int): Flow<List<MessageRecord>>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND unread = 1 " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeUnreadByAccount(accountId: String, limit: Int): Flow<List<MessageRecord>>

    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND starred = 1 " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeStarredByAccount(accountId: String, limit: Int): Flow<List<MessageRecord>>

    /** Bounded subject/snippet search foundation (full local index is Phase 10). */
    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND " +
            "(subject LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%') " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    suspend fun searchByText(accountId: String, query: String, limit: Int): List<MessageRecord>

    @Query("UPDATE messages SET unread = :unread WHERE messageId = :messageId")
    suspend fun setUnread(messageId: String, unread: Boolean)

    @Query("UPDATE messages SET starred = :starred WHERE messageId = :messageId")
    suspend fun setStarred(messageId: String, starred: Boolean)

    @Query("SELECT COUNT(*) FROM messages WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: String): Int

    @Query("DELETE FROM messages WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    // ---- Phase 4 sync engine support ----

    /** Gmail ids already stored for [accountId] (idempotency pre-check, phase §18). */
    @Query(
        "SELECT gmailMessageId FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId IN (:gmailIds)",
    )
    suspend fun existingGmailIds(accountId: String, gmailIds: List<String>): List<String>

    /** All messages of one thread (bounded by thread size; phase §42). */
    @Query("SELECT * FROM messages WHERE threadId = :threadId")
    suspend fun getByThread(threadId: String): List<MessageRecord>

    /** Local thread ids owning any of [gmailIds] (for aggregate refresh after deletes). */
    @Query(
        "SELECT DISTINCT threadId FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId IN (:gmailIds)",
    )
    suspend fun threadIdsForGmailIds(accountId: String, gmailIds: List<String>): List<String>

    /**
     * Local message ids for Gmail ids (Phase 10): lets the FTS index drop
     * the exact documents when synced messages are deleted, so no index
     * row ever points at a missing message (phase §58).
     */
    @Query(
        "SELECT messageId FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId IN (:gmailIds)",
    )
    suspend fun messageIdsForGmailIds(
        accountId: String,
        gmailIds: List<String>,
    ): List<String>

    /** Removes one synced message by its Gmail id (incremental delete, phase §23). */
    @Query(
        "DELETE FROM messages WHERE accountId = :accountId " +
            "AND gmailMessageId = :gmailMessageId",
    )
    suspend fun deleteByGmailId(accountId: String, gmailMessageId: String)

    // ---- Phase 7 classification support ----

    /**
     * Messages with no classification row yet (incremental classification,
     * phase §49). Bounded; newest first so fresh mail classifies first.
     */
    @Query(
        "SELECT m.* FROM messages m LEFT JOIN classifications c " +
            "ON c.messageId = m.messageId WHERE m.accountId = :accountId " +
            "AND c.messageId IS NULL ORDER BY m.timestampEpochMs DESC LIMIT :limit",
    )
    suspend fun getUnclassified(accountId: String, limit: Int): List<MessageRecord>

    // ---- Phase 9 priority support ----

    /**
     * Messages with no priority row yet (incremental prioritization).
     * Bounded; newest first so fresh mail prioritizes first.
     */
    @Query(
        "SELECT m.* FROM messages m LEFT JOIN priorities p " +
            "ON p.messageId = m.messageId WHERE m.accountId = :accountId " +
            "AND p.messageId IS NULL ORDER BY m.timestampEpochMs DESC LIMIT :limit",
    )
    suspend fun getUnprioritized(accountId: String, limit: Int): List<MessageRecord>

    /**
     * Messages carrying one Gmail label. Labels are stored as a U+001F
     * (char(31)) delimited string ([MoConverters]); wrapping both sides in
     * the separator makes the match exact — "SPAM" never matches "SPAMMY".
     */
    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND " +
            "instr(char(31) || labels || char(31), char(31) || :label || char(31)) > 0 " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeByLabel(accountId: String, label: String, limit: Int): Flow<List<MessageRecord>>

    // ---- Phase 8 company intelligence support ----

    /**
     * Links a message to its detected company (Phase 8). Null clears the
     * link (personal sender, or re-processing). Set only by
     * [com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase].
     */
    @Query("UPDATE messages SET companyId = :companyId WHERE messageId = :messageId")
    suspend fun setCompanyId(messageId: String, companyId: String?)

    /**
     * Messages not yet processed by company intelligence (incremental,
     * phase-style bounded pass). Newest first so fresh mail is
     * attributed first.
     */
    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND companyId IS NULL " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    suspend fun getWithoutCompany(accountId: String, limit: Int): List<MessageRecord>

    /** One company's mail, newest first, bounded (company filter UI). */
    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND companyId = :companyId " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeByCompany(
        accountId: String,
        companyId: String,
        limit: Int,
    ): Flow<List<MessageRecord>>

    /**
     * One company's mail inside one classification category (company
     * filter applied to a category destination). Queried directly — never
     * derived by filtering a page — so the list always matches the chip
     * counts.
     */
    @Query(
        "SELECT m.* FROM messages m JOIN classifications c ON c.messageId = m.messageId " +
            "WHERE m.accountId = :accountId AND c.category = :category " +
            "AND m.companyId = :companyId ORDER BY m.timestampEpochMs DESC LIMIT :limit",
    )
    fun observeByCompanyAndCategory(
        accountId: String,
        category: MailCategory,
        companyId: String,
        limit: Int,
    ): Flow<List<MessageRecord>>

    /**
     * One company's mail inside one Gmail-label destination (Social/Spam).
     * Same direct-query contract as [observeByCompanyAndCategory].
     */
    @Query(
        "SELECT * FROM messages WHERE accountId = :accountId AND companyId = :companyId AND " +
            "instr(char(31) || labels || char(31), char(31) || :label || char(31)) > 0 " +
            "ORDER BY timestampEpochMs DESC LIMIT :limit",
    )
    fun observeByCompanyAndLabel(
        accountId: String,
        companyId: String,
        label: String,
        limit: Int,
    ): Flow<List<MessageRecord>>

    /**
     * Per-company message counts inside one classification category
     * (company filter chips, e.g. Promotional → Google 12). Counts are
     * global for the account+category, not page-limited — the chips must
     * never show fabricated numbers.
     */
    @Query(
        "SELECT m.companyId AS companyId, COUNT(*) AS messageCount " +
            "FROM messages m JOIN classifications c ON c.messageId = m.messageId " +
            "WHERE m.accountId = :accountId AND c.category = :category " +
            "AND m.companyId IS NOT NULL GROUP BY m.companyId",
    )
    suspend fun companyCountsForCategory(
        accountId: String,
        category: MailCategory,
    ): List<CompanyMessageCount>

    /**
     * Per-company message counts inside one Gmail-label destination
     * (Social → CATEGORY_SOCIAL, Spam → SPAM). Same honesty contract as
     * [companyCountsForCategory]; the label match reuses the exact
     * char(31)-delimited matching from [observeByLabel].
     */
    @Query(
        "SELECT m.companyId AS companyId, COUNT(*) AS messageCount FROM messages m " +
            "WHERE m.accountId = :accountId AND " +
            "instr(char(31) || m.labels || char(31), char(31) || :label || char(31)) > 0 " +
            "AND m.companyId IS NOT NULL GROUP BY m.companyId",
    )
    suspend fun companyCountsForLabel(
        accountId: String,
        label: String,
    ): List<CompanyMessageCount>

    /**
     * Total per-company message counts for the Companies destination
     * (Phase 11). Global for the account, not page-limited — the company
     * list must never show fabricated numbers.
     */
    @Query(
        "SELECT companyId AS companyId, COUNT(*) AS messageCount FROM messages " +
            "WHERE accountId = :accountId AND companyId IS NOT NULL GROUP BY companyId",
    )
    suspend fun companyMessageCounts(accountId: String): List<CompanyMessageCount>
}
