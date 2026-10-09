package com.greninjaop.mailorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Sender persistence (Phase 2 — storage foundation; engine is Phase 8). */
@Dao
interface SenderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sender: SenderRecord)

    @Query(
        "SELECT * FROM senders WHERE accountId = :accountId AND normalizedEmail = :normalizedEmail",
    )
    suspend fun getByEmail(accountId: String, normalizedEmail: String): SenderRecord?

    /**
     * Records one observed message from a sender (Phase 8).
     *
     * Insert-or-increment inside a transaction: the first sighting creates
     * the row with `messageCount = 1`; later sightings bump the count and
     * refresh `lastSeenEpochMs` (and the display name when the new one is
     * non-blank and the stored one is blank). Returns the stored row so
     * callers can derive the recurring-sender signal honestly.
     */
    @Transaction
    suspend fun recordMessage(
        accountId: String,
        emailAddress: String,
        normalizedEmail: String,
        displayName: String?,
        domain: String,
        nowEpochMs: Long,
    ): SenderRecord {
        val existing = getByEmail(accountId, normalizedEmail)
        val updated = if (existing == null) {
            SenderRecord(
                senderId = "sender:$accountId:$normalizedEmail",
                accountId = accountId,
                emailAddress = emailAddress,
                normalizedEmail = normalizedEmail,
                displayName = displayName,
                domain = domain,
                firstSeenEpochMs = nowEpochMs,
                lastSeenEpochMs = nowEpochMs,
                messageCount = 1,
            )
        } else {
            val next = existing.messageCount + 1
            existing.copy(
                emailAddress = emailAddress,
                displayName = when {
                    !displayName.isNullOrBlank() &&
                        existing.displayName.isNullOrBlank() -> displayName
                    else -> existing.displayName
                },
                lastSeenEpochMs = maxOf(existing.lastSeenEpochMs, nowEpochMs),
                // Saturate instead of overflowing (see SenderIntelligence).
                messageCount = if (next < 0) Int.MAX_VALUE else next,
            )
        }
        upsert(updated)
        return updated
    }

    @Query(
        "SELECT * FROM senders WHERE accountId = :accountId " +
            "ORDER BY messageCount DESC LIMIT :limit",
    )
    fun observeTopByAccount(accountId: String, limit: Int): Flow<List<SenderRecord>>

    @Query("DELETE FROM senders WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    /**
     * Sender name/address suggestions for search (Phase 10). [like] must
     * be a caller-built `%…%` pattern passed as a bound parameter — never
     * string-concatenated into SQL (phase §48).
     */
    @Query(
        "SELECT * FROM senders WHERE accountId = :accountId AND " +
            "(displayName LIKE :like COLLATE NOCASE OR emailAddress LIKE :like COLLATE NOCASE) " +
            "ORDER BY messageCount DESC LIMIT :limit",
    )
    suspend fun suggestByText(accountId: String, like: String, limit: Int): List<SenderRecord>
}
