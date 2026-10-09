package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User-defined rule row (Phase 2 — storage only; engine is Phase 12).
 *
 * [matcher] semantics depend on [ruleType]: a sender address, a domain, or a
 * keyword. At most one of the target fields is expected to be set per rule;
 * enforcement is the engine's job, not the schema's.
 */
@Entity(
    tableName = "user_rules",
    foreignKeys = [
        ForeignKey(
            entity = AccountRecord::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class UserRuleRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val ruleType: RuleType,
    val matcher: String,
    val targetCategory: MailCategory?,
    val targetPriority: Priority?,
    val targetActionRequired: Boolean?,
    val enabled: Boolean = true,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
