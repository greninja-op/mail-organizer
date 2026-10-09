package com.greninjaop.mailorganizer.data.repository

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.ActionItemDao
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.ClassificationDao
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.CompanyDao
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemDao
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityDao
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.SenderDao
import com.greninjaop.mailorganizer.data.local.SenderRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Intelligence storage operations (Phase 2 — storage only).
 *
 * The classification/priority/action engines (Phases 7/9/14) will write
 * through this interface; it already enforces the precedence-relevant
 * invariants (single current classification/priority per message, explicit
 * completion states for action items).
 */
interface IntelligenceRepository {
    // senders
    suspend fun upsertSender(sender: SenderRecord)
    fun observeTopSenders(accountId: String, limit: Int = 50): Flow<List<SenderRecord>>

    /**
     * Sender row for one normalized address (Phase 8), or null when never
     * seen. Used to derive the real recurring-sender signal.
     */
    suspend fun getSenderByEmail(accountId: String, normalizedEmail: String): SenderRecord?

    /**
     * Records one observed message from a sender (Phase 8) — insert or
     * atomic increment. Returns the stored row.
     */
    suspend fun recordSenderMessage(
        accountId: String,
        emailAddress: String,
        normalizedEmail: String,
        displayName: String?,
        domain: String,
    ): SenderRecord

    // companies
    suspend fun upsertCompany(company: CompanyRecord)
    fun observeCompanyFilterList(accountId: String, limit: Int = 100): Flow<List<CompanyRecord>>
    suspend fun setCompanyPinned(companyId: String, pinned: Boolean)

    /** Company row for one canonical domain (Phase 8), or null. */
    suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String): CompanyRecord?

    // classification
    suspend fun setClassification(record: ClassificationRecord)
    suspend fun getClassification(messageId: String): ClassificationRecord?

    /**
     * Deletes a message's classification row (Phase 12) — used to clear a
     * stale user-sourced row so the deterministic base can be restored.
     * The row is only ever re-created by the engines, never resurrected
     * with user intent.
     */
    suspend fun deleteClassification(messageId: String)

    /**
     * Batch classification lookup for search results (Phase 10) — one
     * query, never N+1. Returns a map keyed by messageId.
     */
    suspend fun getClassifications(messageIds: List<String>): Map<String, ClassificationRecord>
    fun observeByCategory(
        accountId: String,
        category: MailCategory,
        limit: Int = 50,
    ): Flow<List<ClassificationRecord>>

    /**
     * Per-category classification counts for the dashboard (Phase 11).
     * One GROUP BY query — never N+1. Missing categories mean zero.
     */
    suspend fun categoryCounts(accountId: String): Map<MailCategory, Int>

    // priority
    suspend fun setPriority(record: PriorityRecord)
    suspend fun getPriority(messageId: String): PriorityRecord?

    /**
     * Deletes a message's priority row (Phase 12) — used to clear a stale
     * user-sourced row so the deterministic base can be restored.
     */
    suspend fun deletePriority(messageId: String)

    /**
     * Batch priority lookup for the visible page (Phase 9) — one query,
     * never N+1. Returns a map keyed by messageId.
     */
    suspend fun getPriorities(messageIds: List<String>): Map<String, PriorityRecord>

    /**
     * Priority rows at one level for the dashboard (Phase 11) — e.g. the
     * Home "High priority" section observes HIGH and CRITICAL. Bounded.
     */
    fun observeByPriority(
        accountId: String,
        priority: Priority,
        limit: Int = 50,
    ): Flow<List<PriorityRecord>>

    // action items
    suspend fun addActionItem(item: ActionItemRecord): Long
    fun observeOpenActionItems(accountId: String, limit: Int = 50): Flow<List<ActionItemRecord>>
    suspend fun completeActionItem(id: Long)
    suspend fun dismissActionItem(id: Long)

    // extracted items
    suspend fun addExtractedItem(item: ExtractedItemRecord): Long
    fun observeOpenExtracted(
        accountId: String,
        type: ExtractedItemType,
        limit: Int = 50,
    ): Flow<List<ExtractedItemRecord>>

    /** All extracted rows for one message (Phase 13 idempotency check). */
    suspend fun getExtractedItems(messageId: String): List<ExtractedItemRecord>

    /** Deletes specific extracted rows (Phase 13 re-extraction). */
    suspend fun deleteExtractedItems(ids: List<Long>)
}

class RoomIntelligenceRepository(
    private val db: AppDatabase,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
) : IntelligenceRepository {

    private val senders: SenderDao get() = db.senderDao()
    private val companies: CompanyDao get() = db.companyDao()
    private val classifications: ClassificationDao get() = db.classificationDao()
    private val priorities: PriorityDao get() = db.priorityDao()
    private val actions: ActionItemDao get() = db.actionItemDao()
    private val extracted: ExtractedItemDao get() = db.extractedItemDao()

    override suspend fun upsertSender(sender: SenderRecord) =
        withContext(dispatchers.io) { senders.upsert(sender) }

    override fun observeTopSenders(accountId: String, limit: Int) =
        senders.observeTopByAccount(accountId, limit)

    override suspend fun getSenderByEmail(
        accountId: String,
        normalizedEmail: String,
    ): SenderRecord? = withContext(dispatchers.io) {
        senders.getByEmail(accountId, normalizedEmail)
    }

    override suspend fun recordSenderMessage(
        accountId: String,
        emailAddress: String,
        normalizedEmail: String,
        displayName: String?,
        domain: String,
    ): SenderRecord = withContext(dispatchers.io) {
        senders.recordMessage(
            accountId = accountId,
            emailAddress = emailAddress,
            normalizedEmail = normalizedEmail,
            displayName = displayName,
            domain = domain,
            nowEpochMs = clock(),
        )
    }

    override suspend fun getCompanyByDomain(
        accountId: String,
        normalizedDomain: String,
    ): CompanyRecord? = withContext(dispatchers.io) {
        companies.getByDomain(accountId, normalizedDomain)
    }

    override suspend fun upsertCompany(company: CompanyRecord) =
        withContext(dispatchers.io) { companies.upsert(company) }

    override fun observeCompanyFilterList(accountId: String, limit: Int) =
        companies.observeFilterList(accountId, limit)

    override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) =
        withContext(dispatchers.io) { companies.setPinned(companyId, pinned) }

    override suspend fun setClassification(record: ClassificationRecord) =
        withContext(dispatchers.io) { classifications.setClassification(record) }

    override suspend fun deleteClassification(messageId: String) =
        withContext(dispatchers.io) { classifications.deleteByMessage(messageId) }

    override suspend fun getClassification(messageId: String) =
        withContext(dispatchers.io) { classifications.getByMessage(messageId) }

    override suspend fun getClassifications(messageIds: List<String>) =
        withContext(dispatchers.io) {
            if (messageIds.isEmpty()) emptyMap()
            else classifications.getByMessages(messageIds).associateBy { it.messageId }
        }

    override fun observeByCategory(
        accountId: String,
        category: MailCategory,
        limit: Int,
    ): Flow<List<ClassificationRecord>> =
        classifications.observeByCategory(accountId, category, limit)

    override suspend fun categoryCounts(accountId: String): Map<MailCategory, Int> =
        withContext(dispatchers.io) {
            classifications.countByCategory(accountId)
                .associate { it.category to it.messageCount }
        }

    override suspend fun setPriority(record: PriorityRecord) =
        withContext(dispatchers.io) { priorities.setPriority(record) }

    override suspend fun deletePriority(messageId: String) =
        withContext(dispatchers.io) { priorities.deleteByMessage(messageId) }

    override suspend fun getPriority(messageId: String) =
        withContext(dispatchers.io) { priorities.getByMessage(messageId) }

    override suspend fun getPriorities(messageIds: List<String>) =
        withContext(dispatchers.io) {
            if (messageIds.isEmpty()) emptyMap()
            else priorities.getByMessages(messageIds).associateBy { it.messageId }
        }

    override fun observeByPriority(
        accountId: String,
        priority: Priority,
        limit: Int,
    ): Flow<List<PriorityRecord>> =
        priorities.observeByPriority(accountId, priority, limit)

    override suspend fun addActionItem(item: ActionItemRecord): Long =
        withContext(dispatchers.io) { actions.insert(item) }

    override fun observeOpenActionItems(accountId: String, limit: Int) =
        actions.observeOpenByAccount(accountId, limit)

    override suspend fun completeActionItem(id: Long) =
        withContext(dispatchers.io) { actions.markCompleted(id) }

    override suspend fun dismissActionItem(id: Long) =
        withContext(dispatchers.io) { actions.markDismissed(id) }

    override suspend fun addExtractedItem(item: ExtractedItemRecord): Long =
        withContext(dispatchers.io) { extracted.insert(item) }

    override fun observeOpenExtracted(
        accountId: String,
        type: ExtractedItemType,
        limit: Int,
    ): Flow<List<ExtractedItemRecord>> =
        extracted.observeOpenByType(accountId, type, limit)

    override suspend fun getExtractedItems(messageId: String): List<ExtractedItemRecord> =
        withContext(dispatchers.io) { extracted.getByMessage(messageId) }

    override suspend fun deleteExtractedItems(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(dispatchers.io) { extracted.deleteByIds(ids) }
    }

    /** Convenience for tests/future engines: stamp a deterministic classification. */
    suspend fun classifyDeterministic(
        messageId: String,
        accountId: String,
        category: MailCategory,
        confidence: Float,
        version: Int,
        explanation: String?,
    ) = setClassification(
        ClassificationRecord(
            messageId = messageId,
            accountId = accountId,
            category = category,
            confidence = confidence,
            source = ClassificationSource.DETERMINISTIC,
            version = version,
            explanation = explanation,
            classifiedAtEpochMs = clock(),
        ),
    )

    /** Convenience: stamp a priority, optionally as a manual override. */
    suspend fun prioritize(
        messageId: String,
        accountId: String,
        priority: Priority,
        manualOverride: Boolean,
        reason: String?,
        version: Int,
    ) = setPriority(
        PriorityRecord(
            messageId = messageId,
            accountId = accountId,
            priority = priority,
            manualOverride = manualOverride,
            reason = reason,
            version = version,
            updatedAtEpochMs = clock(),
        ),
    )

    /** Convenience: record a detected action item. */
    suspend fun detectAction(
        messageId: String,
        accountId: String,
        actionType: ActionType,
        confidence: Float,
        explanation: String?,
        dueDateEpochMs: Long?,
    ): Long = addActionItem(
        ActionItemRecord(
            messageId = messageId,
            accountId = accountId,
            actionType = actionType,
            confidence = confidence,
            explanation = explanation,
            dueDateEpochMs = dueDateEpochMs,
            detectedAtEpochMs = clock(),
        ),
    )
}
