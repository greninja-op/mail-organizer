package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Company persistence (Phase 2 — storage foundation; detection is Phase 8).
 *
 * The filter-list query orders pinned companies first, then by name —
 * pinning never moves mail between categories (requirements.md).
 */
@Dao
interface CompanyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(company: CompanyRecord)

    @Query("SELECT * FROM companies WHERE companyId = :companyId")
    suspend fun getById(companyId: String): CompanyRecord?

    /**
     * Company row for one id *within one account* (Phase 10): company ids
     * (`co:<domain>`) are deterministic across accounts, so the account
     * scope must be explicit to avoid cross-account leakage.
     */
    @Query(
        "SELECT * FROM companies WHERE accountId = :accountId AND companyId = :companyId",
    )
    suspend fun getByAccountAndId(accountId: String, companyId: String): CompanyRecord?

    /**
     * Company row for one canonical domain in one account (Phase 8).
     * The (accountId, normalizedDomain) unique index makes this the
     * canonical lookup for detection output.
     */
    @Query(
        "SELECT * FROM companies WHERE accountId = :accountId " +
            "AND normalizedDomain = :normalizedDomain",
    )
    suspend fun getByDomain(accountId: String, normalizedDomain: String): CompanyRecord?

    @Query(
        "SELECT * FROM companies WHERE accountId = :accountId " +
            "ORDER BY pinned DESC, canonicalName ASC LIMIT :limit",
    )
    fun observeFilterList(accountId: String, limit: Int): Flow<List<CompanyRecord>>

    @Query("UPDATE companies SET pinned = :pinned WHERE companyId = :companyId")
    suspend fun setPinned(companyId: String, pinned: Boolean)

    @Query("DELETE FROM companies WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    /**
     * Company-name suggestions for search (Phase 10). [like] must be a
     * caller-built `%…%` pattern passed as a bound parameter — never
     * string-concatenated into SQL (phase §48).
     */
    @Query(
        "SELECT * FROM companies WHERE accountId = :accountId AND " +
            "(canonicalName LIKE :like COLLATE NOCASE OR userOverrideName LIKE :like COLLATE NOCASE) " +
            "ORDER BY pinned DESC, canonicalName ASC LIMIT :limit",
    )
    suspend fun suggestByText(accountId: String, like: String, limit: Int): List<CompanyRecord>

    /** Unified company suggestions across all enabled accounts (Phase 18). */
    @Query(
        "SELECT c.* FROM companies c JOIN accounts a ON a.accountId = c.accountId " +
            "WHERE a.isEnabled = 1 AND " +
            "(c.canonicalName LIKE :like COLLATE NOCASE OR c.userOverrideName LIKE :like COLLATE NOCASE) " +
            "ORDER BY c.pinned DESC, c.canonicalName ASC LIMIT :limit",
    )
    suspend fun suggestByTextUnified(like: String, limit: Int): List<CompanyRecord>
}
