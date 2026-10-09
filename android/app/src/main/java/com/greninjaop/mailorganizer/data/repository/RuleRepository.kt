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
 * User rules + corrections storage (Phase 2 — storage only; engine is
 * Phase 12). Corrections are authoritative user intent; the unique index
 * makes [recordCorrection] a "latest wins" upsert.
 */
interface RuleRepository {
    suspend fun addRule(rule: UserRuleRecord): Long
    suspend fun deleteRule(id: Long)
    fun observeEnabledRules(accountId: String): Flow<List<UserRuleRecord>>
    suspend fun recordCorrection(correction: UserCorrectionRecord)
    suspend fun getCorrection(
        accountId: String,
        scope: CorrectionScope,
        scopeKey: String,
        field: CorrectionField,
    ): UserCorrectionRecord?
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
}
