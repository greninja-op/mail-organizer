package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Conversation/thread row (Phase 2).
 *
 * Threads are strictly account-scoped: a Gmail thread id is only unique
 * *within* an account, hence the composite unique index on
 * (accountId, gmailThreadId). Deleting an account cascades to its threads
 * (and, transitively, to messages).
 */
@Entity(
    tableName = "threads",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("accountId"),
        Index(value = ["accountId", "gmailThreadId"], unique = true),
    ],
)
data class ThreadRecord(
    /** Local primary key; stable across re-syncs. */
    @PrimaryKey val threadId: String,
    /** Gmail thread id; null until the sync engine (Phase 4) assigns one. */
    val gmailThreadId: String?,
    val accountId: String,
    val subject: String,
    val participantDisplayNames: List<String> = emptyList(),
    val messageCount: Int = 0,
    val unreadCount: Int = 0,
    val latestMessageId: String? = null,
    val latestMessageEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
)
