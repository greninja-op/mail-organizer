package com.greninjaop.mailorganizer.data.repository

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.CorrectionScope
import com.greninjaop.mailorganizer.data.local.UserCorrectionDao
import com.greninjaop.mailorganizer.data.local.UserCorrectionRecord
import com.greninjaop.mailorganizer.data.local.UserRuleDao
import com.greninjaop.mailorganizer.data.local.UserRuleRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Key for batch correction lookups (Phase 12).
 *
 * One query per (account, scope, key, field) would be N+1; the repository
 * batches these into a single lookup.
 */
data class CorrectionLookup(
    val accountId: String,
    val scope: CorrectionScope,
    val scopeKey: String,
    val field: CorrectionField,
)

/**
 * User rules + corrections storage (Phase 2 storage; Phase 12 engine).
 * Corrections are authoritative user intent; the unique index
 * makes [recordCorrection] a "latest wins" upsert.
 */
interface RuleRepository {
    suspend fun addRule(rule: UserRuleRecord): Long
    suspend fun deleteRule(id: Long)
    fun observeEnabledRules(accountId: String): Flow<List<UserRuleRecord>>

    /** Enabled rules for one account, as a one-shot list (Phase 12). */
    suspend fun getEnabledRules(accountId: String): List<UserRuleRecord>

    /** All rule rows, for the management UI (Phase 12). */
    fun observeAllRules(): Flow<List<UserRuleRecord>>

    suspend fun recordCorrection(correction: UserCorrectionRecord)
    suspend fun getCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ): UserCorrectionRecord?

    // ---- Phase 12: rule management ----

    suspend fun updateRule(rule: UserRuleRecord)
    suspend fun getRule(id: Long): UserRuleRecord?
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)

    /** All rules for one account in deterministic (order, id) order. */
    suspend fun getAllRules(accountId: String): List<UserRuleRecord>

    /**
     * Batch correction lookup (Phase 12) — one pass over the corrections
     * table per call, never N+1. Returns the rows that exist, keyed by
     * their lookup.
     */
    suspend fun getCorrections(
        accountId: String,
        lookups: List<CorrectionLookup>,
    ): Map<CorrectionLookup, UserCorrectionRecord>

    /** Deletes one correction (undo). No-op when absent. */
    suspend fun deleteCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    )
}

class RoomRuleRepository(
    private val db: AppDatabase,
    private val dispatchers: AppDispatchers,
) : RuleRepository {

    private val rules: UserRuleDao get() = db.userRuleDao()
    private val corrections: UserCorrectionDao get() = db.userCorrectionDao()

    override suspend fun addRule(rule: UserRuleRecord): Long =
        withContext(dispatchers.io) { rules.insert(rule) }

    override suspend fun deleteRule(id: Long) =
        withContext(dispatchers.io) { rules.deleteById(id) }

    override fun observeEnabledRules(accountId: String): Flow<List<UserRuleRecord>> =
        rules.observeEnabledByAccount(accountId)

    override suspend fun getEnabledRules(accountId: String): List<UserRuleRecord> =
        withContext(dispatchers.io) { rules.getEnabledByAccount(accountId) }

    override fun observeAllRules(): Flow<List<UserRuleRecord>> = rules.observeAll()

    override suspend fun recordCorrection(correction: UserCorrectionRecord) =
        withContext(dispatchers.io) { corrections.upsertCorrection(correction) }

    override suspend fun getCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ): UserCorrectionRecord? = withContext(dispatchers.io) {
        corrections.get(accountId, scope, scopeKey, field)
    }

    override suspend fun updateRule(rule: UserRuleRecord) =
        withContext(dispatchers.io) { rules.update(rule) }

    override suspend fun getRule(id: Long): UserRuleRecord? =
        withContext(dispatchers.io) { rules.getById(id) }

    override suspend fun setRuleEnabled(id: Long, enabled: Boolean) =
        withContext(dispatchers.io) { rules.setEnabled(id, enabled) }

    override suspend fun getAllRules(accountId: String): List<UserRuleRecord> =
        withContext(dispatchers.io) { rules.getAllByAccount(accountId) }

    override suspend fun getCorrections(
        accountId: String,
        lookups: List<CorrectionLookup>,
    ): Map<CorrectionLookup, UserCorrectionRecord> =
        withContext(dispatchers.io) {
            val wanted = lookups.filter { it.accountId == accountId }.toSet()
            if (wanted.isEmpty()) return@withContext emptyMap()
            // One DAO call; corrections tables stay small (user-authored
            // only), so the in-memory filter is cheaper than N+1 queries.
            val byKey = corrections.getAllByAccount(accountId).associateBy {
                CorrectionLookup(it.accountId, it.scope, it.scopeKey, it.field)
            }
            wanted.mapNotNull { lookup ->
                byKey[lookup]?.let { lookup to it }
            }.toMap()
        }

    override suspend fun deleteCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ) = withContext(dispatchers.io) {
        corrections.delete(accountId, scope, scopeKey, field)
    }
}
