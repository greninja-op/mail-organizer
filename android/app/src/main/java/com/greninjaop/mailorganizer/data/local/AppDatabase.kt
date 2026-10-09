package com.greninjaop.mailorganizer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local database — the rebuildable intelligence layer (Phase 2).
 *
 * Gmail remains the cloud source of truth; everything stored here must be
 * derivable from Gmail plus user intent (rules/corrections, whose backup
 * strategy is a later-phase concern).
 *
 * Version history:
 * - v1 (Phase 0): accounts seed only.
 * - v2 (Phase 2): full mailbox schema — threads, messages, senders,
 *   companies, classifications, priorities, action items, sync state,
 *   user rules/corrections, extracted items. See [Migrations].
 * - v3 (Phase 5): parser output columns on `messages` (`bodyHtml`,
 *   `attachments` metadata). See [Migrations.MIGRATION_2_3].
 *
 * Schema is exported to `app/schemas` so migrations stay verifiable.
 * Destructive fallback is deliberately NOT enabled: production migrations
 * must preserve user data.
 */
@Database(
    entities = [
        AccountRecord::class,
        ThreadRecord::class,
        MessageRecord::class,
        SenderRecord::class,
        CompanyRecord::class,
        ClassificationRecord::class,
        PriorityRecord::class,
        ActionItemRecord::class,
        SyncStateRecord::class,
        UserRuleRecord::class,
        UserCorrectionRecord::class,
        ExtractedItemRecord::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(MoConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun senderDao(): SenderDao
    abstract fun companyDao(): CompanyDao
    abstract fun classificationDao(): ClassificationDao
    abstract fun priorityDao(): PriorityDao
    abstract fun actionItemDao(): ActionItemDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun userRuleDao(): UserRuleDao
    abstract fun userCorrectionDao(): UserCorrectionDao
    abstract fun extractedItemDao(): ExtractedItemDao
}
