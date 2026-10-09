package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Version bookkeeping for the derived FTS index (Phase 10). */
@Dao
interface SearchIndexMetaDao {

    @Query("SELECT version FROM search_index_meta WHERE accountId = :accountId")
    suspend fun getVersion(accountId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVersion(meta: SearchIndexMeta)
}
