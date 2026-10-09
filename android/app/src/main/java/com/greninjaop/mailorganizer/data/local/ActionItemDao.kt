package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Action-item persistence (Phase 2 storage foundation; Phase 14 engine).
 *
 * Phase 14 additions: per-message and per-thread open-item lookups for
 * deterministic dedup (§22–§23), status transitions, version-stale cleanup,
 * and the overdue-expiry pass (§27). History rows are never deleted by the
 * expiry pass — only transitioned to EXPIRED.
 */
@Dao
interface ActionItemDao {

    @Insert
    suspend fun insert(item: ActionItemRecord): Long

    @Query("SELECT * FROM action_items WHERE id = :id")
    suspend fun getById(id: Long): ActionItemRecord?

    @Query("SELECT * FROM action_items WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: String): List<ActionItemRecord>

    /**
     * Open (non-terminal) items in a thread — the thread-level dedup check.
     * Terminal statuses (completed/dismissed/expired/...) are history or
     * user intent and never suppress new candidates.
     */
    @Query(
        "SELECT * FROM action_items WHERE threadId = :threadId " +
            "AND status NOT IN " +
            "('COMPLETED','DISMISSED','EXPIRED','CANCELLED','FAILED')",
    )
    suspend fun getOpenByThread(threadId: String): List<ActionItemRecord>

    @Query(
        "SELECT * FROM action_items WHERE accountId = :accountId " +
            "AND completed = 0 AND dismissed = 0 " +
            "ORDER BY dueDateEpochMs ASC LIMIT :limit",
    )
    fun observeOpenByAccount(accountId: String, limit: Int): Flow<List<ActionItemRecord>>

    /** Phase 14: transition status (keeps legacy booleans in sync). */
    @Query(
        "UPDATE action_items SET status = :status, " +
            "completed = CASE WHEN :status = 'COMPLETED' THEN 1 ELSE completed END, " +
            "dismissed = CASE WHEN :status = 'DISMISSED' THEN 1 ELSE dismissed END, " +
            "updatedAtEpochMs = :nowEpochMs WHERE id = :id",
    )
    suspend fun setStatus(id: Long, status: ActionStatus, nowEpochMs: Long = System.currentTimeMillis())

    @Query("UPDATE action_items SET completed = 1, status = 'COMPLETED', updatedAtEpochMs = :nowEpochMs WHERE id = :id")
    suspend fun markCompleted(id: Long, nowEpochMs: Long = System.currentTimeMillis())

    @Query("UPDATE action_items SET dismissed = 1, status = 'DISMISSED', updatedAtEpochMs = :nowEpochMs WHERE id = :id")
    suspend fun markDismissed(id: Long, nowEpochMs: Long = System.currentTimeMillis())

    /** Phase 14: delete stale-version rows during re-generation. */
    @Query("DELETE FROM action_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Phase 14: expire open cards whose due date passed long ago (§27).
     * Returns the number of rows transitioned.
     */
    @Query(
        "UPDATE action_items SET status = 'EXPIRED', updatedAtEpochMs = :nowEpochMs " +
            "WHERE accountId = :accountId AND status = 'SUGGESTED' " +
            "AND dueDateEpochMs IS NOT NULL AND dueDateEpochMs < :cutoffEpochMs",
    )
    suspend fun expireOverdue(
        accountId: String,
        cutoffEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Int

    @Query("DELETE FROM action_items WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}
