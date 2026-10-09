package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** User-rule persistence (Phase 2 storage; Phase 12 engine + management). */
@Dao
interface UserRuleDao {

    @Insert
    suspend fun insert(rule: UserRuleRecord): Long

    @Update
    suspend fun update(rule: UserRuleRecord)

    @Query("DELETE FROM user_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM user_rules WHERE id = :id")
    suspend fun getById(id: Long): UserRuleRecord?

    @Query("UPDATE user_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE user_rules SET ruleOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Query("SELECT * FROM user_rules WHERE accountId = :accountId AND enabled = 1")
    fun observeEnabledByAccount(accountId: String): Flow<List<UserRuleRecord>>

    @Query("SELECT * FROM user_rules WHERE accountId = :accountId AND enabled = 1")
    suspend fun getEnabledByAccount(accountId: String): List<UserRuleRecord>

    /** All rules for management UI, deterministic order (order, then id). */
    @Query("SELECT * FROM user_rules WHERE accountId = :accountId ORDER BY ruleOrder ASC, id ASC")
    suspend fun getAllByAccount(accountId: String): List<UserRuleRecord>

    @Query("SELECT * FROM user_rules")
    fun observeAll(): Flow<List<UserRuleRecord>>
}
