package com.greninjaop.mailorganizer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User-defined rule row (Phase 2 — storage; engine is Phase 12).
 *
 * Two representations coexist:
 * - **Structured (Phase 12):** [conditionsJson]/[actionsJson] hold the
 *   [com.greninjaop.mailorganizer.core.rules.RuleCondition]/[com.greninjaop.mailorganizer.core.rules.RuleAction]
 *   lists as JSON (see `RuleJson`). The engine reads these.
 * - **Legacy (Phase 2):** [ruleType]/[matcher]/[targetCategory]/
 *   [targetPriority]/[targetActionRequired] describe a simple single-target
 *   rule. The Phase 12 engine derives an equivalent structured rule from
 *   these when the JSON columns are empty, so old rows keep working.
 *
 * New rules are always written in structured form; the legacy columns are
 * populated as a human-readable hint from the first condition/action.
 *
 * [ruleOrder] is the explicit, user-controllable precedence (lower wins);
 * ties break by [id]. Never depend on insertion or row order (phase §21).
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
    /** Human-readable rule name (Phase 12). */
    val name: String = "",
    /** JSON: List<RuleCondition> (Phase 12; `[]` = derive from legacy columns). */
    val conditionsJson: String = "[]",
    /** JSON: List<RuleAction> (Phase 12; `[]` = derive from legacy columns). */
    val actionsJson: String = "[]",
    /** Explicit precedence, lower wins (Phase 12). */
    val ruleOrder: Int = 0,
    /** Rule schema version (Phase 12). */
    val ruleVersion: Int = 1,
    /** Where the rule came from (Phase 12). */
    val source: RuleSource = RuleSource.MANUAL,
)
