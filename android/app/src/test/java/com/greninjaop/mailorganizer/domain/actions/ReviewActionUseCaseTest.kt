package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ReviewActionUseCase tests (Phase 14, phase §24–§26, §31–§32).
 *
 * Pins the safety-critical behavior: dismissal never touches the email,
 * confirming an external proposal with no connected integration records
 * the intent and honestly reports "not connected" — never a fake success.
 */
class ReviewActionUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private fun dispatchers() = AppDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
    )

    private lateinit var store: ActionStore
    private lateinit var useCase: ReviewActionUseCase

    @Before
    fun setup() {
        store = ActionStore()
        useCase = ReviewActionUseCase(
            intelligence = store,
            executors = ActionExecutorRegistry(),
            dispatchers = dispatchers(),
        )
    }

    private fun card(
        externalEffect: ExternalEffect = ExternalEffect.NONE,
        status: ActionStatus = ActionStatus.SUGGESTED,
    ) = ActionItemRecord(
        messageId = "m1",
        accountId = "a1",
        threadId = "t1",
        actionType = ActionType.MEETING,
        title = "Meeting: Sync",
        urgency = ActionUrgency.HIGH,
        source = ActionSource.TEMPORAL_EXTRACTION,
        status = status,
        confidence = 1.0f,
        explanation = "why",
        externalEffect = externalEffect,
        dueDateEpochMs = 1_800_000_000_000L,
        detectedAtEpochMs = 1_799_000_000_000L,
    )

    @Test
    fun `dismiss transitions to dismissed and keeps history`() = runTest(testDispatcher) {
        val id = store.add(card())
        assertTrue(useCase.dismiss(id))
        val row = store.get(id)!!
        assertEquals(ActionStatus.DISMISSED, row.status)
        assertTrue(row.dismissed)
    }

    @Test
    fun `dismiss of a completed card is refused`() = runTest(testDispatcher) {
        val id = store.add(card(status = ActionStatus.COMPLETED))
        assertEquals(false, useCase.dismiss(id))
        assertEquals(ActionStatus.COMPLETED, store.get(id)!!.status)
    }

    @Test
    fun `mark reviewed records attention without side effects`() = runTest(testDispatcher) {
        val id = store.add(card())
        assertTrue(useCase.markReviewed(id))
        assertEquals(ActionStatus.REVIEWED, store.get(id)!!.status)
    }

    @Test
    fun `confirm internal suggestion records intent`() = runTest(testDispatcher) {
        val id = store.add(card(externalEffect = ExternalEffect.NONE))
        val outcome = useCase.confirm(id)
        assertTrue(outcome is ConfirmationOutcome.RecordedInternal)
        assertEquals(ActionStatus.CONFIRMED, store.get(id)!!.status)
    }

    @Test
    fun `confirm external proposal with no executor is honestly not-connected`() =
        runTest(testDispatcher) {
            val id = store.add(card(externalEffect = ExternalEffect.CALENDAR))
            val outcome = useCase.confirm(id)
            assertTrue(
                "expected ExternalNotConnected, got $outcome",
                outcome is ConfirmationOutcome.ExternalNotConnected,
            )
            // The intent is recorded locally — nothing external happened.
            assertEquals(ActionStatus.CONFIRMED, store.get(id)!!.status)
        }

    @Test
    fun `confirm already-dismissed card fails safely`() = runTest(testDispatcher) {
        val id = store.add(card(status = ActionStatus.DISMISSED))
        val outcome = useCase.confirm(id)
        assertTrue(outcome is ConfirmationOutcome.Failed)
    }

    @Test
    fun `confirm missing card fails safely`() = runTest(testDispatcher) {
        val outcome = useCase.confirm(999L)
        assertTrue(outcome is ConfirmationOutcome.Failed)
    }

    @Test
    fun `executor registry is empty in phase 14`() {
        val registry = ActionExecutorRegistry()
        for (type in ActionType.values()) {
            assertEquals(null, registry.find(type))
        }
    }

    @Test
    fun `a registered executor is consulted after confirmation`() = runTest(testDispatcher) {
        val fakeExecutor = object : ActionExecutor {
            var executed = 0
            override fun supports(actionType: ActionType) = true
            override fun validate(candidate: ActionCandidate) = MoResult.Success(Unit)
            override suspend fun execute(candidate: ActionCandidate): MoResult<ExecutionReceipt> {
                executed++
                return MoResult.Success(
                    ExecutionReceipt("fake", "ext-1", 1_800_000_000_000L, "created"),
                )
            }
        }
        val registry = ActionExecutorRegistry(listOf(fakeExecutor))
        val uc = ReviewActionUseCase(store, registry, dispatchers())
        val id = store.add(card(externalEffect = ExternalEffect.CALENDAR))

        val outcome = uc.confirm(id)

        assertTrue(outcome is ConfirmationOutcome.Executed)
        assertEquals(1, fakeExecutor.executed)
        assertEquals(ActionStatus.COMPLETED, store.get(id)!!.status)
    }

    @Test
    fun `executor failure transitions to failed without fake success`() =
        runTest(testDispatcher) {
            val failing = object : ActionExecutor {
                override fun supports(actionType: ActionType) = true
                override fun validate(candidate: ActionCandidate) = MoResult.Success(Unit)
                override suspend fun execute(candidate: ActionCandidate) =
                    MoResult.Failure(
                        com.greninjaop.mailorganizer.MoError.Unexpected(),
                    )
            }
            val uc = ReviewActionUseCase(
                store, ActionExecutorRegistry(listOf(failing)), dispatchers(),
            )
            val id = store.add(card(externalEffect = ExternalEffect.CALENDAR))

            val outcome = uc.confirm(id)

            assertTrue(outcome is ConfirmationOutcome.Failed)
            assertEquals(ActionStatus.FAILED, store.get(id)!!.status)
        }

    /** Minimal action-item store behind a test IntelligenceRepository. */
    private class ActionStore : com.greninjaop.mailorganizer.data.repository.IntelligenceRepository {
        private val rows = mutableMapOf<Long, ActionItemRecord>()
        private var nextId = 1L

        fun add(row: ActionItemRecord): Long {
            val id = nextId++
            rows[id] = row.copy(id = id)
            return id
        }

        fun get(id: Long) = rows[id]

        override suspend fun getActionItem(id: Long) = rows[id]

        override suspend fun setActionItemStatus(id: Long, status: ActionStatus) {
            // Mirrors the real DAO: legacy booleans stay in sync with status.
            rows[id]?.let {
                rows[id] = it.copy(
                    status = status,
                    completed = it.completed || status == ActionStatus.COMPLETED,
                    dismissed = it.dismissed || status == ActionStatus.DISMISSED,
                )
            }
        }

        override suspend fun addActionItem(item: ActionItemRecord): Long = add(item)
        override suspend fun completeActionItem(id: Long) {
            rows[id]?.let { rows[id] = it.copy(completed = true, status = ActionStatus.COMPLETED) }
        }

        override suspend fun dismissActionItem(id: Long) {
            rows[id]?.let { rows[id] = it.copy(dismissed = true, status = ActionStatus.DISMISSED) }
        }

        override fun observeOpenActionItems(accountId: String, limit: Int) =
            kotlinx.coroutines.flow.flowOf(emptyList<ActionItemRecord>())

        override suspend fun getActionItemsByMessage(messageId: String) =
            rows.values.filter { it.messageId == messageId }

        override suspend fun getOpenActionItemsByThread(threadId: String) =
            rows.values.filter { it.threadId == threadId }

        override suspend fun deleteActionItems(ids: List<Long>) {
            ids.forEach { rows.remove(it) }
        }

        override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long) = 0

        // Unused here.
        override suspend fun upsertSender(sender: com.greninjaop.mailorganizer.data.local.SenderRecord) {}
        override fun observeTopSenders(accountId: String, limit: Int) =
            kotlinx.coroutines.flow.flowOf(emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>())

        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = null
        override suspend fun recordSenderMessage(
            accountId: String, emailAddress: String, normalizedEmail: String,
            displayName: String?, domain: String,
        ): com.greninjaop.mailorganizer.data.local.SenderRecord =
            throw UnsupportedOperationException()

        override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) {}
        override fun observeCompanyFilterList(accountId: String, limit: Int) =
            kotlinx.coroutines.flow.flowOf(emptyList<com.greninjaop.mailorganizer.data.local.CompanyRecord>())

        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
        override suspend fun setClassification(record: com.greninjaop.mailorganizer.data.local.ClassificationRecord) {}
        override suspend fun getClassification(messageId: String) = null
        override suspend fun deleteClassification(messageId: String) {}
        override suspend fun getClassifications(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.ClassificationRecord>()
        override fun observeByCategory(
            accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, limit: Int,
        ) = kotlinx.coroutines.flow.flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ClassificationRecord>())

        override suspend fun categoryCounts(accountId: String) =
            emptyMap<com.greninjaop.mailorganizer.data.local.MailCategory, Int>()

        override suspend fun setPriority(record: com.greninjaop.mailorganizer.data.local.PriorityRecord) {}
        override suspend fun getPriority(messageId: String) = null
        override suspend fun deletePriority(messageId: String) {}
        override suspend fun getPriorities(messageIds: List<String>) =
            emptyMap<String, com.greninjaop.mailorganizer.data.local.PriorityRecord>()

        override fun observeByPriority(
            accountId: String, priority: com.greninjaop.mailorganizer.data.local.Priority, limit: Int,
        ) = kotlinx.coroutines.flow.flowOf(emptyList<com.greninjaop.mailorganizer.data.local.PriorityRecord>())

        override suspend fun addExtractedItem(item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord) = 0L
        override fun observeOpenExtracted(
            accountId: String, type: com.greninjaop.mailorganizer.data.local.ExtractedItemType, limit: Int,
        ) = kotlinx.coroutines.flow.flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>())

        override suspend fun getExtractedItems(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>()
        override suspend fun deleteExtractedItems(ids: List<Long>) {}
    }
}
