package com.greninjaop.mailorganizer.domain.priority

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.priority.PriorityLevel
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Priority use-case tests (Phase 9): override safety, idempotency,
 * versioning, account isolation, failure safety. In-memory fakes.
 */
class PrioritizeMessageUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private lateinit var mail: FakePriorityMailRepository
    private lateinit var intelligence: FakePriorityIntelligence
    private lateinit var useCase: PrioritizeMessageUseCase
    private lateinit var mailboxUseCase: PrioritizeMailboxUseCase

    @Before
    fun setup() {
        mail = FakePriorityMailRepository()
        intelligence = FakePriorityIntelligence()
        useCase = PrioritizeMessageUseCase(mail, intelligence, dispatchers) { 1_700_000_000_000L }
        mailboxUseCase = PrioritizeMailboxUseCase(mail, useCase, dispatchers)
    }

    private fun message(
        id: String,
        account: String = "acc1",
        from: String = "sender@example.com",
        subject: String = "Hello",
        body: String? = "Body",
        labels: List<String> = emptyList(),
        unread: Boolean = true,
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = "g-$id",
        threadId = "t-$id",
        accountId = account,
        fromAddress = from,
        fromName = null,
        subject = subject,
        snippet = null,
        bodyText = body,
        labels = labels,
        unread = unread,
        timestampEpochMs = 1_000L,
    )

    private fun classification(
        id: String,
        category: MailCategory,
        confidence: Float = 0.6f,
    ) = ClassificationRecord(
        messageId = id,
        accountId = "acc1",
        category = category,
        confidence = confidence,
        source = ClassificationSource.DETERMINISTIC,
        version = 2,
        explanation = "test",
        classifiedAtEpochMs = 1_000L,
    )

    // ---- Basic prioritization ----

    @Test
    fun `prioritizes and persists a deterministic result`() = runTest(testDispatcher) {
        mail.seed(message("m1"))
        intelligence.seedClassification(
            classification("m1", MailCategory.ACTION_REQUIRED, 0.6f),
        )
        val result = useCase.prioritize("m1", "acc1")
        assertNotNull(result)
        assertEquals(PriorityLevel.HIGH, result!!.priority)

        val stored = intelligence.getPriority("m1")
        assertNotNull(stored)
        assertEquals(Priority.HIGH, stored!!.priority)
        assertEquals("acc1", stored.accountId)
        assertTrue(stored.reason?.isNotBlank() == true)
    }

    @Test
    fun `unclassified mail defaults to normal`() = runTest(testDispatcher) {
        mail.seed(message("m1", from = "friend@example.com"))
        val result = useCase.prioritize("m1", "acc1")
        assertNotNull(result)
        assertEquals(PriorityLevel.NORMAL, result!!.priority)
    }

    @Test
    fun `newsletter is low priority`() = runTest(testDispatcher) {
        mail.seed(message("m1", from = "news@example.com"))
        intelligence.seedClassification(
            classification("m1", MailCategory.NEWSLETTERS, 0.9f),
        )
        val result = useCase.prioritize("m1", "acc1")
        assertEquals(PriorityLevel.LOW, result!!.priority)
    }

    @Test
    fun `manual override is never overwritten`() = runTest(testDispatcher) {
        mail.seed(message("m1"))
        intelligence.seedClassification(
            classification("m1", MailCategory.ACTION_REQUIRED, 0.9f),
        )
        intelligence.setPriority(
            PriorityRecord(
                messageId = "m1",
                accountId = "acc1",
                priority = Priority.LOW,
                manualOverride = true,
                reason = "user said so",
                version = 1,
                updatedAtEpochMs = 1_000L,
            ),
        )
        val result = useCase.prioritize("m1", "acc1")
        assertNull(result)
        assertEquals(Priority.LOW, intelligence.getPriority("m1")!!.priority)
    }

    @Test
    fun `current version is idempotent`() = runTest(testDispatcher) {
        mail.seed(message("m1"))
        intelligence.seedClassification(
            classification("m1", MailCategory.PROMOTIONS, 0.9f),
        )
        val first = useCase.prioritize("m1", "acc1")
        assertNotNull(first)
        val second = useCase.prioritize("m1", "acc1")
        assertNull(second)
    }

    @Test
    fun `older version is reprioritized`() = runTest(testDispatcher) {
        mail.seed(message("m1"))
        intelligence.seedClassification(
            classification("m1", MailCategory.PROMOTIONS, 0.9f),
        )
        intelligence.setPriority(
            PriorityRecord(
                messageId = "m1",
                accountId = "acc1",
                priority = Priority.NORMAL,
                manualOverride = false,
                reason = "old",
                version = 0,
                updatedAtEpochMs = 1_000L,
            ),
        )
        val result = useCase.prioritize("m1", "acc1")
        assertNotNull(result)
        assertEquals(Priority.LOW, intelligence.getPriority("m1")!!.priority)
    }

    @Test
    fun `account isolation is enforced`() = runTest(testDispatcher) {
        mail.seed(message("m1", account = "acc2"))
        val result = useCase.prioritize("m1", "acc1")
        assertNull(result)
        assertNull(intelligence.getPriority("m1"))
    }

    @Test
    fun `missing message returns null`() = runTest(testDispatcher) {
        assertNull(useCase.prioritize("nope", "acc1"))
    }

    @Test
    fun `mailbox use case prioritizes new messages`() = runTest(testDispatcher) {
        mail.seed(
            message("m1"),
            message("m2"),
        )
        intelligence.seedClassification(classification("m1", MailCategory.NEWSLETTERS))
        intelligence.seedClassification(classification("m2", MailCategory.IMPORTANT))
        val n = mailboxUseCase.prioritizeNew("acc1")
        assertEquals(2, n)
        assertEquals(Priority.LOW, intelligence.getPriority("m1")!!.priority)
        assertEquals(Priority.HIGH, intelligence.getPriority("m2")!!.priority)
    }

    @Test
    fun `mappers round-trip correctly`() {
        assertEquals(Priority.LOW, PriorityLevel.LOW.toPriority())
        assertEquals(Priority.NORMAL, PriorityLevel.NORMAL.toPriority())
        assertEquals(Priority.HIGH, PriorityLevel.HIGH.toPriority())
        assertEquals(Priority.CRITICAL, PriorityLevel.CRITICAL.toPriority())
    }

    // ---- Fakes ----

    private class FakePriorityMailRepository : MailRepository {
        private val messages = mutableListOf<MessageRecord>()

        fun seed(vararg records: MessageRecord) {
            messages.addAll(records)
        }

        override suspend fun saveThreadWithMessages(
            thread: com.greninjaop.mailorganizer.data.local.ThreadRecord,
            messages: List<MessageRecord>,
        ) = Unit

        override fun observeThreads(
            accountId: String,
            limit: Int,
        ): Flow<List<com.greninjaop.mailorganizer.data.local.ThreadRecord>> =
            MutableStateFlow(emptyList())

        override fun observeMessages(
            threadId: String,
            limit: Int,
        ): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())

        override fun observeUnread(accountId: String, limit: Int): Flow<List<MessageRecord>> =
            MutableStateFlow(emptyList())

        override fun observeStarred(accountId: String, limit: Int): Flow<List<MessageRecord>> =
            MutableStateFlow(emptyList())

        override suspend fun searchByText(
            accountId: String,
            query: String,
            limit: Int,
        ): List<MessageRecord> = emptyList()

        override suspend fun setRead(messageId: String, read: Boolean) = Unit
        override suspend fun setStarred(messageId: String, starred: Boolean) = Unit
        override suspend fun countByAccount(accountId: String): Int = 0

        override suspend fun getMessagesByIds(messageIds: List<String>): List<MessageRecord> =
            messages.filter { it.messageId in messageIds }

        override suspend fun getMessage(messageId: String): MessageRecord? =
            messages.firstOrNull { it.messageId == messageId }

        override suspend fun getUnclassifiedMessages(
            accountId: String,
            limit: Int,
        ): List<MessageRecord> =
            messages.filter { it.accountId == accountId }.take(limit)

        override suspend fun getUnprioritizedMessages(
            accountId: String,
            limit: Int,
        ): List<MessageRecord> =
            messages.filter { it.accountId == accountId }.take(limit)

        override fun observeByLabel(
            accountId: String,
            label: String,
            limit: Int,
        ): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())

        override suspend fun setMessageCompanyId(messageId: String, companyId: String?) = Unit

        override suspend fun getMessagesWithoutCompany(
            accountId: String,
            limit: Int,
        ): List<MessageRecord> = emptyList()

        override fun observeMessagesByCompany(
            accountId: String,
            companyId: String,
            limit: Int,
        ): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())

        override fun observeMessagesByCompanyAndCategory(
            accountId: String,
            category: MailCategory,
            companyId: String,
            limit: Int,
        ): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())

        override fun observeMessagesByCompanyAndLabel(
            accountId: String,
            companyId: String,
            label: String,
            limit: Int,
        ): Flow<List<MessageRecord>> = MutableStateFlow(emptyList())

        override suspend fun companyCountsForCategory(
            accountId: String,
            category: MailCategory,
        ): Map<String, Int> = emptyMap()

        override suspend fun companyCountsForLabel(
            accountId: String,
            label: String,
        ): Map<String, Int> = emptyMap()

        override suspend fun getThreadByGmailId(
            accountId: String,
            gmailThreadId: String,
        ): com.greninjaop.mailorganizer.data.local.ThreadRecord? = null

        override suspend fun updateThreadAggregates(threadId: String) = Unit

        override suspend fun existingGmailIds(
            accountId: String,
            gmailIds: List<String>,
        ): Set<String> = emptySet()

        override suspend fun deleteMessagesByGmailIds(
            accountId: String,
            gmailIds: List<String>,
        ): List<String> = emptyList()
    }

    private class FakePriorityIntelligence : IntelligenceRepository {
        private val classifications = mutableMapOf<String, ClassificationRecord>()
        private val priorities = mutableMapOf<String, PriorityRecord>()

        fun seedClassification(record: ClassificationRecord) {
            classifications[record.messageId] = record
        }

        override suspend fun upsertSender(
            sender: com.greninjaop.mailorganizer.data.local.SenderRecord,
        ) = Unit

        override fun observeTopSenders(
            accountId: String,
            limit: Int,
        ): Flow<List<com.greninjaop.mailorganizer.data.local.SenderRecord>> =
            MutableStateFlow(emptyList())

        override suspend fun getSenderByEmail(
            accountId: String,
            normalizedEmail: String,
        ): com.greninjaop.mailorganizer.data.local.SenderRecord? = null

        override suspend fun recordSenderMessage(
            accountId: String,
            emailAddress: String,
            normalizedEmail: String,
            displayName: String?,
            domain: String,
        ): com.greninjaop.mailorganizer.data.local.SenderRecord =
            throw UnsupportedOperationException()

        override suspend fun upsertCompany(
            company: com.greninjaop.mailorganizer.data.local.CompanyRecord,
        ) = Unit

        override fun observeCompanyFilterList(
            accountId: String,
            limit: Int,
        ): Flow<List<com.greninjaop.mailorganizer.data.local.CompanyRecord>> =
            MutableStateFlow(emptyList())

        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) = Unit

        override suspend fun getCompanyByDomain(
            accountId: String,
            normalizedDomain: String,
        ): com.greninjaop.mailorganizer.data.local.CompanyRecord? = null

        override suspend fun setClassification(record: ClassificationRecord) {
            classifications[record.messageId] = record
        }

        override suspend fun getClassification(messageId: String): ClassificationRecord? =
            classifications[messageId]

        override fun observeByCategory(
            accountId: String,
            category: MailCategory,
            limit: Int,
        ): Flow<List<ClassificationRecord>> = MutableStateFlow(emptyList())

        override suspend fun setPriority(record: PriorityRecord) {
            priorities[record.messageId] = record
        }

        override suspend fun getPriority(messageId: String): PriorityRecord? =
            priorities[messageId]

        override suspend fun getPriorities(messageIds: List<String>): Map<String, PriorityRecord> =
            priorities.filterKeys { it in messageIds }

        override suspend fun addActionItem(
            item: com.greninjaop.mailorganizer.data.local.ActionItemRecord,
        ): Long = 0L

        override fun observeOpenActionItems(
            accountId: String,
            limit: Int,
        ): Flow<List<com.greninjaop.mailorganizer.data.local.ActionItemRecord>> =
            MutableStateFlow(emptyList())

        override suspend fun completeActionItem(id: Long) = Unit
        override suspend fun dismissActionItem(id: Long) = Unit

        override suspend fun addExtractedItem(
            item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord,
        ): Long = 0L

        override fun observeOpenExtracted(
            accountId: String,
            type: com.greninjaop.mailorganizer.data.local.ExtractedItemType,
            limit: Int,
        ): Flow<List<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>> =
            MutableStateFlow(emptyList())
    }
}
