package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.actions.ActionCandidateGenerator
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.SenderRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.domain.temporal.TemporalPayloadJson
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GenerateActionsUseCase tests (Phase 14).
 *
 * Covers: candidate generation from effective intelligence, idempotent
 * re-runs, thread-level dedup, stale-version replacement, user-correction
 * respect (effective ACTION_REQUIRED), account isolation, and the expiry
 * pass. Failures degrade safely — the batch never aborts.
 */
class GenerateActionsUseCaseTest {

    companion object {
        private const val NOW = 1_800_000_000_000L
        private const val DAY = 24L * 60 * 60 * 1000
        private const val ACCOUNT = "a1"
    }

    private val testDispatcher = StandardTestDispatcher()
    private fun dispatchers() = AppDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
    )

    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligence
    private lateinit var useCase: GenerateActionsUseCase

    @Before
    fun setup() {
        mail = FakeMailRepository()
        intelligence = FakeIntelligence()
        useCase = GenerateActionsUseCase(
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            clock = { NOW },
        )
    }

    private suspend fun seedMessage(
        messageId: String,
        threadId: String = "t1",
        accountId: String = ACCOUNT,
        subject: String = "Interview confirmation",
        fromAddress: String = "jobs@acme.com",
    ) {
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = "g-$threadId",
                accountId = accountId,
                subject = subject,
                latestMessageEpochMs = NOW - DAY,
            ),
            listOf(
                MessageRecord(
                    messageId = messageId,
                    gmailMessageId = "g-$messageId",
                    threadId = threadId,
                    accountId = accountId,
                    fromAddress = fromAddress,
                    fromName = "Acme",
                    subject = subject,
                    snippet = null,
                    bodyText = "body",
                    timestampEpochMs = NOW - DAY,
                ),
            ),
        )
    }

    private fun classification(
        messageId: String,
        category: MailCategory,
        source: ClassificationSource = ClassificationSource.DETERMINISTIC,
    ) = ClassificationRecord(
        messageId = messageId,
        accountId = ACCOUNT,
        category = category,
        confidence = 0.9f,
        source = source,
        version = 1,
        explanation = "test",
        classifiedAtEpochMs = NOW,
    )

    private fun temporalItem(
        messageId: String,
        itemType: ExtractedItemType = ExtractedItemType.INTERVIEW,
        dueInDays: Long = 2,
        confidence: String = "HIGH",
    ): ExtractedItemRecord {
        // Minimal payload JSON: the generator only needs the decoded view.
        val payload = "{\"end\":\"\",\"dateOnly\":\"0\",\"tz\":\"UTC\"," +
            "\"tzSource\":\"EXPLICIT_IN_EMAIL\",\"loc\":\"\",\"url\":\"\"," +
            "\"status\":\"UPCOMING\",\"conf\":\"$confidence\"," +
            "\"signals\":\"interview_vocabulary\",\"expl\":\"why\",\"ver\":\"1\"}"
        return ExtractedItemRecord(
            messageId = messageId,
            accountId = ACCOUNT,
            itemType = itemType,
            title = "Onsite interview",
            payload = payload,
            dueDateEpochMs = NOW + dueInDays * DAY,
            detectedAtEpochMs = NOW,
        )
    }

    @Test
    fun `interview mail produces a meeting card`() = runTest(testDispatcher) {
        seedMessage("m1")
        intelligence.setClassification(classification("m1", MailCategory.CAREER))
        intelligence.addExtractedItem(temporalItem("m1"))

        assertEquals(1, useCase.generateNew(ACCOUNT))

        val rows = intelligence.actionItems()
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(ActionType.MEETING, row.actionType)
        assertEquals(ActionStatus.SUGGESTED, row.status)
        assertEquals(ACCOUNT, row.accountId)
        assertEquals("t1", row.threadId)
        assertEquals(ExternalEffect.CALENDAR, row.externalEffect)
        assertTrue(row.title.contains("Interview"))
    }

    @Test
    fun `re-running is idempotent`() = runTest(testDispatcher) {
        seedMessage("m1")
        intelligence.setClassification(classification("m1", MailCategory.CAREER))
        intelligence.addExtractedItem(temporalItem("m1"))

        assertEquals(1, useCase.generateNew(ACCOUNT))
        assertEquals(0, useCase.generateNew(ACCOUNT))
        assertEquals(1, intelligence.actionItems().size)
    }

    @Test
    fun `thread-level dedup keeps one card per underlying action`() = runTest(testDispatcher) {
        // Two sibling messages in one thread, same interview item → one card.
        seedMessage("m1", threadId = "t9")
        seedMessage("m2", threadId = "t9")
        intelligence.setClassification(classification("m1", MailCategory.CAREER))
        intelligence.setClassification(classification("m2", MailCategory.CAREER))
        // Same temporal item id → same target key.
        intelligence.addExtractedItem(temporalItem("m1").copy(id = 42L))
        intelligence.addExtractedItem(temporalItem("m2").copy(id = 42L))

        assertEquals(1, useCase.generateNew(ACCOUNT))
        assertEquals(1, intelligence.actionItems().size)
    }

    @Test
    fun `user correction to action-required is respected`() = runTest(testDispatcher) {
        seedMessage("m1")
        intelligence.setClassification(
            classification(
                "m1",
                MailCategory.ACTION_REQUIRED,
                ClassificationSource.USER_CORRECTION,
            ),
        )

        assertEquals(1, useCase.generateNew(ACCOUNT))
        val row = intelligence.actionItems().single()
        assertEquals(ActionType.REPLY_REQUIRED, row.actionType)
        assertEquals(ActionSource.USER_CORRECTION, row.source)
    }

    @Test
    fun `user correction away from action-required suppresses reply cards`() =
        runTest(testDispatcher) {
            seedMessage("m1")
            intelligence.setClassification(
                classification(
                    "m1",
                    MailCategory.NEWSLETTERS,
                    ClassificationSource.USER_CORRECTION,
                ),
            )

            assertEquals(0, useCase.generateNew(ACCOUNT))
            assertTrue(intelligence.actionItems().isEmpty())
        }

    @Test
    fun `other accounts messages are never processed`() = runTest(testDispatcher) {
        seedMessage("mX", accountId = "other")
        assertEquals(0, useCase.generateNew(ACCOUNT))
        assertTrue(intelligence.actionItems().isEmpty())
    }

    @Test
    fun `overdue open cards expire but are retained`() = runTest(testDispatcher) {
        seedMessage("m1")
        intelligence.setClassification(classification("m1", MailCategory.CAREER))
        intelligence.addExtractedItem(temporalItem("m1", dueInDays = 1))
        assertEquals(1, useCase.generateNew(ACCOUNT))

        // Fast-forward past the grace period: the expiry pass runs inside
        // generateNew via the injected clock.
        val expiring = GenerateActionsUseCase(
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            clock = { NOW + 5 * DAY },
        )
        assertEquals(0, expiring.generateNew(ACCOUNT))

        val rows = intelligence.actionItems()
        assertEquals(1, rows.size)
        assertEquals(ActionStatus.EXPIRED, rows.single().status)
    }

    @Test
    fun `stale generator versions are replaced`() = runTest(testDispatcher) {
        seedMessage("m1")
        intelligence.setClassification(classification("m1", MailCategory.CAREER))
        intelligence.addExtractedItem(temporalItem("m1"))
        assertEquals(1, useCase.generateNew(ACCOUNT))

        // Simulate an older generator version row.
        val old = intelligence.actionItems().single()
        intelligence.replace(old.copy(version = 0, payloadJson = old.payloadJson?.replace("\"ver\":\"1\"", "\"ver\":\"0\"")))

        assertEquals(1, useCase.generateNew(ACCOUNT))
        val rows = intelligence.actionItems()
        assertEquals(1, rows.size)
        assertEquals(ActionCandidateGenerator.VERSION, rows.single().version)
    }

    // ---- Minimal in-memory IntelligenceRepository fake ----

    private class FakeIntelligence : IntelligenceRepository {
        private val classifications = mutableMapOf<String, ClassificationRecord>()
        private val priorities = mutableMapOf<String, PriorityRecord>()
        private val extracted = mutableListOf<ExtractedItemRecord>()
        private val actions = mutableListOf<ActionItemRecord>()
        private var nextId = 1L

        fun actionItems(): List<ActionItemRecord> = actions.toList()

        fun replace(row: ActionItemRecord) {
            actions.removeAll { it.id == row.id }
            actions += row
        }

        override suspend fun setClassification(record: ClassificationRecord) {
            classifications[record.messageId] = record
        }

        override suspend fun getClassification(messageId: String) = classifications[messageId]
        override suspend fun deleteClassification(messageId: String) {
            classifications.remove(messageId)
        }

        override suspend fun getClassifications(messageIds: List<String>) =
            messageIds.mapNotNull { id -> classifications[id]?.let { id to it } }.toMap()

        override fun observeByCategory(
            accountId: String, category: MailCategory, limit: Int,
        ): Flow<List<ClassificationRecord>> = flowOf(emptyList())

        override suspend fun categoryCounts(accountId: String) = emptyMap<MailCategory, Int>()

        override suspend fun setPriority(record: PriorityRecord) {
            priorities[record.messageId] = record
        }

        override suspend fun getPriority(messageId: String) = priorities[messageId]
        override suspend fun deletePriority(messageId: String) {
            priorities.remove(messageId)
        }

        override suspend fun getPriorities(messageIds: List<String>) =
            messageIds.mapNotNull { id -> priorities[id]?.let { id to it } }.toMap()

        override fun observeByPriority(
            accountId: String, priority: Priority, limit: Int,
        ): Flow<List<PriorityRecord>> = flowOf(emptyList())

        override suspend fun addActionItem(item: ActionItemRecord): Long {
            val id = nextId++
            actions += item.copy(id = id)
            return id
        }

        override fun observeOpenActionItems(accountId: String, limit: Int) =
            flowOf(actions.filter { it.accountId == accountId })

        override suspend fun completeActionItem(id: Long) {
            update(id) { it.copy(completed = true, status = ActionStatus.COMPLETED) }
        }

        override suspend fun dismissActionItem(id: Long) {
            update(id) { it.copy(dismissed = true, status = ActionStatus.DISMISSED) }
        }

        override suspend fun getActionItem(id: Long) = actions.firstOrNull { it.id == id }

        override suspend fun getActionItemsByMessage(messageId: String) =
            actions.filter { it.messageId == messageId }

        override suspend fun getOpenActionItemsByThread(threadId: String) =
            actions.filter {
                it.threadId == threadId &&
                    it.status !in setOf(
                        ActionStatus.COMPLETED, ActionStatus.DISMISSED,
                        ActionStatus.EXPIRED, ActionStatus.CANCELLED, ActionStatus.FAILED,
                    )
            }

        override suspend fun setActionItemStatus(id: Long, status: ActionStatus) {
            update(id) { it.copy(status = status) }
        }

        override suspend fun deleteActionItems(ids: List<Long>) {
            actions.removeAll { it.id in ids }
        }

        override suspend fun expireOverdueActionItems(
            accountId: String, cutoffEpochMs: Long,
        ): Int {
            var n = 0
            actions.replaceAll {
                if (it.accountId == accountId && it.status == ActionStatus.SUGGESTED &&
                    it.dueDateEpochMs != null && it.dueDateEpochMs < cutoffEpochMs
                ) {
                    n++
                    it.copy(status = ActionStatus.EXPIRED)
                } else {
                    it
                }
            }
            return n
        }

        override suspend fun addExtractedItem(item: ExtractedItemRecord): Long {
            // Room autoGenerate respects a preset non-zero id; mirror that so
            // tests can pin the item identity (thread-dedup key = item id).
            val id = if (item.id != 0L) item.id else nextId++
            extracted += item.copy(id = id)
            return id
        }

        override fun observeOpenExtracted(
            accountId: String, type: ExtractedItemType, limit: Int,
        ) = flowOf(emptyList<ExtractedItemRecord>())

        override suspend fun getExtractedItems(messageId: String) =
            extracted.filter { it.messageId == messageId }

        override suspend fun deleteExtractedItems(ids: List<Long>) {
            extracted.removeAll { it.id in ids }
        }

        private fun update(id: Long, f: (ActionItemRecord) -> ActionItemRecord) {
            val i = actions.indexOfFirst { it.id == id }
            if (i >= 0) actions[i] = f(actions[i])
        }

        // Unused by these tests — minimal defaults.
        override suspend fun upsertSender(sender: SenderRecord) {}
        override fun observeTopSenders(accountId: String, limit: Int) =
            flowOf(emptyList<SenderRecord>())

        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = null
        override suspend fun recordSenderMessage(
            accountId: String, emailAddress: String, normalizedEmail: String,
            displayName: String?, domain: String,
        ): SenderRecord = throw UnsupportedOperationException()

        override suspend fun upsertCompany(company: CompanyRecord) {}
        override fun observeCompanyFilterList(accountId: String, limit: Int) =
            flowOf(emptyList<CompanyRecord>())

        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
    }

    // Unused MutableStateFlow import guard (kept for parity with other fakes).
    @Suppress("unused")
    private val _unused = MutableStateFlow(0)
}
