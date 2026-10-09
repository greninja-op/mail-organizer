package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect

/**
 * Action-item row (Phase 2 storage foundation; Phase 14 action engine).
 *
 * Phase 14 additions (migration v6→v7, all additive):
 * - [threadId]: thread-level dedup across sibling messages (phase §23).
 * - [title]/[description]: card copy, possibly degraded by the safety
 *   layer — never raw email content.
 * - [urgency]: action urgency, independent from email priority (§17).
 * - [source]: which intelligence produced the candidate (§10).
 * - [status]: lifecycle state (§24). The legacy [completed]/[dismissed]
 *   booleans are kept and backfilled into [status] by the migration; new
 *   writes keep both in sync.
 * - [externalEffect]: classified external side effect (§9). NONE means a
 *   purely internal suggestion.
 * - [payloadJson]: versioned payload (generator version, signals,
 *   missing-info, dedup target key) — the idempotency key lives here.
 * - [version]: generator version that produced the row (re-generation).
 * - [updatedAtEpochMs]: last state transition.
 *
 * Unlike classifications, a message may carry *multiple* open action
 * items, hence no unique index on messageId. Completion/dismissal are
 * explicit user-intent states, never inferred.
 */
@Entity(
    tableName = "action_items",
    foreignKeys = [
        ForeignKey(
            entity = MessageRecord::class,
            parentColumns = ["messageId"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("messageId"),
        Index("accountId"),
        Index("threadId"),
        Index("dueDateEpochMs"),
        Index("status"),
    ],
)
data class ActionItemRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val accountId: String,
    /** Phase 14: thread scope for cross-message dedup. */
    val threadId: String = "",
    val actionType: ActionType,
    /** Phase 14: card title (possibly safety-degraded). */
    val title: String = "",
    /** Phase 14: card description (possibly safety-degraded). */
    val description: String? = null,
    /** Phase 14: action urgency, independent from email priority. */
    val urgency: ActionUrgency = ActionUrgency.NORMAL,
    /** Phase 14: which intelligence produced the candidate. */
    val source: ActionSource = ActionSource.CLASSIFICATION,
    /** Phase 14: lifecycle status. */
    val status: ActionStatus = ActionStatus.SUGGESTED,
    val confidence: Float,
    val explanation: String?,
    /** Phase 14: classified external side effect (NONE = internal). */
    val externalEffect: ExternalEffect = ExternalEffect.NONE,
    /** Phase 14: versioned JSON (generator version, signals, target key). */
    val payloadJson: String? = null,
    val dueDateEpochMs: Long?,
    val detectedAtEpochMs: Long,
    /** Phase 14: last state transition. */
    val updatedAtEpochMs: Long = 0L,
    /** Phase 14: generator version that produced the row. */
    val version: Int = 1,
    val completed: Boolean = false,
    val dismissed: Boolean = false,
)
