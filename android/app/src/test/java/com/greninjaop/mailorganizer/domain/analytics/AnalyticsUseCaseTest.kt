package com.greninjaop.mailorganizer.domain.analytics

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.analytics.AnalyticsDateRange
import com.greninjaop.mailorganizer.core.conversation.ConversationAnalysisResult
import com.greninjaop.mailorganizer.core.conversation.ConversationState
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.local.UserRuleRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.CorrectionLookup
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RuleRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import com.greninjaop.mailorganizer.domain.cleanup.CleanupUseCase
import com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
    )

    private val account = AccountRecord(
        accountId = "acc_test",
        emailAddress = "user@example.com",
        displayName = "Test User",
        createdAtEpochMs = 1700000000000L,
        isEnabled = true,
        lastSyncEpochMs = 1700000000000L,
    )

    private val fakeAccountRepo = object : AccountRepository {
        override suspend fun upsert(account: AccountRecord) {}
        override suspend fun getById(accountId: String): AccountRecord? = if (accountId == "acc_test") account else null
        override fun observeAll(): Flow<List<AccountRecord>> = flowOf(listOf(account))
        override fun observeEnabled(): Flow<List<AccountRecord>> = flowOf(listOf(account))
        override suspend fun updateConnectionState(accountId: String, state: com.greninjaop.mailorganizer.data.local.ConnectionState) {}
        override suspend fun recordSync(accountId: String, syncEpochMs: Long) {}
        override suspend fun setEnabled(accountId: String, enabled: Boolean) {}
        override suspend fun deleteById(accountId: String) {}
    }

    private val messages = mutableListOf<MessageRecord>()
    private val classifications = mutableMapOf<String, ClassificationRecord>()
    private val priorities = mutableMapOf<String, PriorityRecord>()

    private val fakeMailRepo = object : MailRepository {
        override suspend fun saveThreadWithMessages(thread: ThreadRecord, messages: List<MessageRecord>) {}
        override fun observeThreads(accountId: String, limit: Int): Flow<List<ThreadRecord>> = flowOf(emptyList())
        override fun observeMessages(threadId: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override fun observeUnread(accountId: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override fun observeStarred(accountId: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override suspend fun searchByText(accountId: String, query: String, limit: Int): List<MessageRecord> = emptyList()
        override suspend fun setRead(messageId: String, read: Boolean) {}
        override suspend fun setStarred(messageId: String, starred: Boolean) {}
        override suspend fun countByAccount(accountId: String): Int = messages.size
        override suspend fun getMessagesByIds(messageIds: List<String>): List<MessageRecord> =
            messages.filter { it.messageId in messageIds }
        override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String): ThreadRecord? = null
        override suspend fun updateThreadAggregates(threadId: String) {}
        override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>): Set<String> = emptySet()
        override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>): List<String> = emptyList()
        override suspend fun getMessage(messageId: String): MessageRecord? = messages.find { it.messageId == messageId }
        override suspend fun getUnclassifiedMessages(accountId: String, limit: Int): List<MessageRecord> = emptyList()
        override suspend fun getUnprioritizedMessages(accountId: String, limit: Int): List<MessageRecord> = emptyList()
        override suspend fun getUnextractedMessages(accountId: String, limit: Int): List<MessageRecord> = emptyList()
        override fun observeByLabel(accountId: String, label: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override suspend fun setMessageCompanyId(messageId: String, companyId: String?) {}
        override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int): List<MessageRecord> = emptyList()
        override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override fun observeMessagesByCompanyAndCategory(accountId: String, category: MailCategory, companyId: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override fun observeMessagesByCompanyAndLabel(accountId: String, companyId: String, label: String, limit: Int): Flow<List<MessageRecord>> = flowOf(emptyList())
        override suspend fun companyCountsForCategory(accountId: String, category: MailCategory): Map<String, Int> = emptyMap()
        override suspend fun companyCountsForLabel(accountId: String, label: String): Map<String, Int> = emptyMap()
        override suspend fun companyMessageCounts(accountId: String): Map<String, Int> = emptyMap()
        override suspend fun getMessageIdsByAccount(accountId: String, limit: Int): List<String> =
            messages.filter { it.accountId == accountId }.map { it.messageId }
        override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int): List<String> = emptyList()
        override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int): List<String> = emptyList()
        override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int): List<String> = emptyList()
    }

    private val fakeIntelligenceRepo = object : IntelligenceRepository {
        override suspend fun upsertSender(sender: com.greninjaop.mailorganizer.data.local.SenderRecord) {}
        override fun observeTopSenders(accountId: String, limit: Int): Flow<List<com.greninjaop.mailorganizer.data.local.SenderRecord>> = flowOf(emptyList())
        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String): com.greninjaop.mailorganizer.data.local.SenderRecord? = null
        override suspend fun recordSenderMessage(accountId: String, emailAddress: String, normalizedEmail: String, displayName: String?, domain: String): com.greninjaop.mailorganizer.data.local.SenderRecord =
            com.greninjaop.mailorganizer.data.local.SenderRecord("s1", accountId, emailAddress, normalizedEmail, displayName, domain, 0L, 0L, 1)
        override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) {}
        override fun observeCompanyFilterList(accountId: String, limit: Int): Flow<List<com.greninjaop.mailorganizer.data.local.CompanyRecord>> = flowOf(emptyList())
        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String): com.greninjaop.mailorganizer.data.local.CompanyRecord? = null
        override suspend fun setClassification(record: ClassificationRecord) { classifications[record.messageId] = record }
        override suspend fun getClassification(messageId: String): ClassificationRecord? = classifications[messageId]
        override suspend fun deleteClassification(messageId: String) { classifications.remove(messageId) }
        override suspend fun getClassifications(messageIds: List<String>): Map<String, ClassificationRecord> =
            classifications.filterKeys { it in messageIds }
        override fun observeByCategory(accountId: String, category: MailCategory, limit: Int): Flow<List<ClassificationRecord>> = flowOf(emptyList())
        override suspend fun categoryCounts(accountId: String): Map<MailCategory, Int> = emptyMap()
        override suspend fun setPriority(record: PriorityRecord) { priorities[record.messageId] = record }
        override suspend fun deletePriority(messageId: String) { priorities.remove(messageId) }
        override suspend fun getPriority(messageId: String): PriorityRecord? = priorities[messageId]
        override suspend fun getPriorities(messageIds: List<String>): Map<String, PriorityRecord> =
            priorities.filterKeys { it in messageIds }
        override fun observeByPriority(accountId: String, priority: Priority, limit: Int): Flow<List<PriorityRecord>> = flowOf(emptyList())
        override suspend fun addActionItem(item: com.greninjaop.mailorganizer.data.local.ActionItemRecord): Long = 1L
        override fun observeOpenActionItems(accountId: String, limit: Int): Flow<List<com.greninjaop.mailorganizer.data.local.ActionItemRecord>> = flowOf(emptyList())
        override suspend fun completeActionItem(id: Long) {}
        override suspend fun dismissActionItem(id: Long) {}
        override suspend fun getActionItem(id: Long): com.greninjaop.mailorganizer.data.local.ActionItemRecord? = null
        override suspend fun getActionItemsByMessage(messageId: String): List<com.greninjaop.mailorganizer.data.local.ActionItemRecord> = emptyList()
        override suspend fun getOpenActionItemsByThread(threadId: String): List<com.greninjaop.mailorganizer.data.local.ActionItemRecord> = emptyList()
        override suspend fun setActionItemStatus(id: Long, status: com.greninjaop.mailorganizer.core.actions.ActionStatus) {}
        override suspend fun deleteActionItems(ids: List<Long>) {}
        override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long): Int = 0
        override suspend fun addExtractedItem(item: ExtractedItemRecord): Long = 1L
        override fun observeOpenExtracted(accountId: String, type: ExtractedItemType, limit: Int): Flow<List<ExtractedItemRecord>> = flowOf(emptyList())
        override suspend fun getExtractedItems(messageId: String): List<ExtractedItemRecord> = emptyList()
        override suspend fun deleteExtractedItems(ids: List<Long>) {}
    }

    private val fakeRuleRepo = object : RuleRepository {
        override suspend fun addRule(rule: UserRuleRecord): Long = 1L
        override suspend fun deleteRule(id: Long) {}
        override fun observeEnabledRules(accountId: String): Flow<List<UserRuleRecord>> = flowOf(emptyList())
        override suspend fun getEnabledRules(accountId: String): List<UserRuleRecord> = emptyList()
        override fun observeAllRules(): Flow<List<UserRuleRecord>> = flowOf(emptyList())
        override suspend fun recordCorrection(correction: com.greninjaop.mailorganizer.data.local.UserCorrectionRecord) {}
        override suspend fun getCorrection(accountId: String, scope: com.greninjaop.mailorganizer.data.local.CorrectionScope, scopeKey: String, field: com.greninjaop.mailorganizer.data.local.CorrectionField): com.greninjaop.mailorganizer.data.local.UserCorrectionRecord? = null
        override suspend fun updateRule(rule: UserRuleRecord) {}
        override suspend fun getRule(id: Long): UserRuleRecord? = null
        override suspend fun setRuleEnabled(id: Long, enabled: Boolean) {}
        override suspend fun getAllRules(accountId: String): List<UserRuleRecord> = emptyList()
        override suspend fun getCorrections(accountId: String, lookups: List<CorrectionLookup>): Map<CorrectionLookup, com.greninjaop.mailorganizer.data.local.UserCorrectionRecord> = emptyMap()
        override suspend fun deleteCorrection(accountId: String, scope: com.greninjaop.mailorganizer.data.local.CorrectionScope, scopeKey: String, field: com.greninjaop.mailorganizer.data.local.CorrectionField) {}
    }

    private val fakeSyncStateRepo = object : SyncStateRepository {
        override suspend fun ensureForAccount(accountId: String): com.greninjaop.mailorganizer.data.local.SyncStateRecord =
            com.greninjaop.mailorganizer.data.local.SyncStateRecord(accountId = accountId)
        override fun observe(accountId: String): Flow<com.greninjaop.mailorganizer.data.local.SyncStateRecord?> = flowOf(null)
        override suspend fun markAttempt(accountId: String, status: com.greninjaop.mailorganizer.data.local.SyncStatus, errorCode: String?) {}
        override suspend fun markSuccess(accountId: String, cursor: String?) {}
        override suspend fun updateCursor(accountId: String, cursor: String) {}
    }

    private val conversationUseCase = ConversationIntelligenceUseCase(
        mail = fakeMailRepo,
        intelligence = fakeIntelligenceRepo,
        accounts = fakeAccountRepo,
        dispatchers = dispatchers,
    )

    private val cleanupUseCase = CleanupUseCase(
        mail = fakeMailRepo,
        intelligence = fakeIntelligenceRepo,
        dispatchers = dispatchers,
    )

    private val fakePrefs = object : ActiveAccountPreferences {
        private var selection: AccountSelection = AccountSelection.Single("acc_test")
        override val activeSelection: Flow<AccountSelection> get() = flowOf(selection)
        override suspend fun setActiveSelection(selection: AccountSelection) { this.selection = selection }
    }

    private lateinit var useCase: AnalyticsUseCase

    @Before
    fun setup() {
        messages.clear()
        classifications.clear()
        priorities.clear()

        useCase = AnalyticsUseCase(
            mail = fakeMailRepo,
            intelligence = fakeIntelligenceRepo,
            accounts = fakeAccountRepo,
            syncState = fakeSyncStateRepo,
            ruleRepository = fakeRuleRepo,
            conversationUseCase = conversationUseCase,
            cleanupUseCase = cleanupUseCase,
            activeAccountPreferences = fakePrefs,
            dispatchers = dispatchers,
            clock = { 10000000L },
        )
    }

    @Test
    fun `getAnalyticsSnapshot returns not enough data when messages count is low`() = runTest(testDispatcher) {
        messages.add(
            MessageRecord(
                messageId = "m1",
                gmailMessageId = "g1",
                threadId = "t1",
                accountId = "acc_test",
                fromAddress = "alice@test.com",
                fromName = "Alice",
                subject = "Hello",
                snippet = "Snippet",
                bodyText = "Body",
                bodyHtml = null,
                timestampEpochMs = 9999000L,
                unread = true,
                starred = false,
                labels = emptyList(),
            ),
        )

        val snapshot = useCase.getAnalyticsSnapshot("acc_test", AnalyticsDateRange.LAST_7_DAYS)

        assertEquals("acc_test", snapshot.accountId)
        assertEquals(1, snapshot.inboxHealth.totalSynchronizedMessages)
        assertFalse("Should report hasEnoughData=false when under 5 emails", snapshot.hasEnoughData)
        assertTrue("No insights should be generated when insufficient data", snapshot.generatedInsights.isEmpty())
    }

    @Test
    fun `getAnalyticsSnapshot aggregates health, categories, and priority distribution correctly`() = runTest(testDispatcher) {
        // Seed 10 messages
        for (i in 1..10) {
            val mid = "m$i"
            val category = when {
                i <= 3 -> MailCategory.ACTION_REQUIRED
                i <= 6 -> MailCategory.PROMOTIONS
                i <= 8 -> MailCategory.RECEIPTS_ORDERS
                else -> MailCategory.UNCLASSIFIED
            }
            val priority = if (i <= 3) Priority.HIGH else Priority.NORMAL
            val unread = i % 2 == 0

            messages.add(
                MessageRecord(
                    messageId = mid,
                    gmailMessageId = "g_$mid",
                    threadId = "t_$mid",
                    accountId = "acc_test",
                    fromAddress = "sender$i@domain.com",
                    fromName = "Sender $i",
                    subject = "Subject $i",
                    snippet = "Snippet $i",
                    bodyText = "Body $i",
                    bodyHtml = null,
                    timestampEpochMs = 9999000L,
                    unread = unread,
                    starred = false,
                    labels = emptyList(),
                ),
            )

            classifications[mid] = ClassificationRecord(
                messageId = mid,
                accountId = "acc_test",
                category = category,
                confidence = 1.0f,
                source = ClassificationSource.DETERMINISTIC,
                version = 1,
                explanation = null,
                classifiedAtEpochMs = 9999000L,
            )

            priorities[mid] = PriorityRecord(
                messageId = mid,
                accountId = "acc_test",
                priority = priority,
                manualOverride = false,
                reason = "rule",
                version = 1,
                updatedAtEpochMs = 9999000L,
            )
        }

        val snapshot = useCase.getAnalyticsSnapshot("acc_test", AnalyticsDateRange.LAST_7_DAYS)

        assertTrue(snapshot.hasEnoughData)
        assertEquals(10, snapshot.inboxHealth.totalSynchronizedMessages)
        assertEquals(5, snapshot.inboxHealth.unreadCount)
        assertEquals(3, snapshot.inboxHealth.actionRequiredCount)
        assertEquals(3, snapshot.inboxHealth.highPriorityCount)

        // Category breakdown
        assertEquals(3, snapshot.organization.categoryDistribution[MailCategory.ACTION_REQUIRED])
        assertEquals(3, snapshot.organization.categoryDistribution[MailCategory.PROMOTIONS])
        assertEquals(2, snapshot.organization.categoryDistribution[MailCategory.RECEIPTS_ORDERS])
        assertEquals(2, snapshot.organization.categoryDistribution[MailCategory.UNCLASSIFIED])

        // Priority breakdown
        assertEquals(3, snapshot.priority.distribution[Priority.HIGH])
        assertEquals(7, snapshot.priority.distribution[Priority.NORMAL])
    }
}
