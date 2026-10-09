package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** User-rule persistence (Phase 2 — storage only; engine is Phase 12). */
@Dao
interface UserRuleDao {

    @Insert
    suspend fun insert(rule: UserRuleRecord): Long

    @Query("DELETE FROM user_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM user_rules WHERE accountId = :accountId AND enabled = 1")
    fun observeEnabledByAccount(accountId: String): Flow<List<UserRuleRecord>>

    @Query("SELECT * FROM user_rules WHERE accountId = :accountId AND enabled = 1")
    suspend fun getEnabledByAccount(accountId: String): List<UserRuleRecord>
}
