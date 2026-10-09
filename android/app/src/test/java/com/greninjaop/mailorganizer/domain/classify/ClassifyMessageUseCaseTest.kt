package com.greninjaop.mailorganizer.domain.classify

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.core.classify.DeterministicClassifier
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
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Use-case tests (Phase 7 §30–34): override safety, idempotency,
 * versioning, account isolation. Uses in-memory fakes (no Robolectric).
 */
class ClassifyMessageUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private lateinit var mail: FakeClassifyMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository
    private lateinit var useCase: ClassifyMessageUseCase
    private lateinit var mailboxUseCase: ClassifyMailboxUseCase

    @Before
    fun setup() {
        mail = FakeClassifyMailRepository()
        intelligence = FakeIntelligenceRepository()
        useCase = ClassifyMessageUseCase(mail, intelligence, dispatchers) { 1_700_000_000_000L }
        mailboxUseCase = ClassifyMailboxUseCase(mail, useCase, dispatchers)
    }

    private fun message(
        id: String,
        account: String = "acc1",
        from: String = "sender@example.com",
        subject: String = "Hello",
        body: String? = "Body",
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
        timestampEpochMs = 1_000L,
    )

    // ---- Basic classification ----

    @Test
    fun `classifies and persists a deterministic result`() = runTest(testDispatcher) {
        mail.seed(message("m1", subject = "Your verification code is 482910"))
        val result = useCase.classify("m1", "acc1")
        assertNotNull(result)
        assertEquals(ClassifierCategory.SECURITY, result!!.category)

        val stored = intelligence.getClassification("m1")
        assertNotNull(stored)
        assertEquals(MailCategory.SECURITY, stored!!.category)
        assertEquals(ClassificationSource.DETERMINISTIC, stored.source)
        assertEquals(DeterministicClassifier.VERSION, stored.version)
        assertEquals("acc1", stored.accountId)
        assertTrue(stored.explanation?.isNotBlank() == true)
    }

    @Test
    fun `confidence maps to documented floats`() {
        assertEquals(0.9f, Confidence.HIGH.toFloat())
        assertEquals(0.6f, Confidence.MEDIUM.toFloat())
        assertEquals(0.3f, Confidence.LOW.toFloat())
    }

    @Test
    fun `missing message returns null`() = runTest(testDispatcher) {
        assertNull(useCase.classify("nope", "acc1"))
    }

    // ---- Override safety (phase §30-31) ----

    @Test
    fun `user correction override is never overwritten`() = runTest(testDispatcher) {
        mail.seed(message("m1", subject = "Your verification code is 482910"))
        intelligence.setClassification(
            ClassificationRecord(
                messageId = "m1",
                accountId = "acc1",
                category = MailCategory.CAREER,
                confidence = 1.0f,
                source = ClassificationSource.USER_CORRECTION,
                version = 0,
                explanation = "User said so",
                classifiedAtEpochMs = 1L,
            ),
        )
        assertNull(useCase.classify("m1", "acc1"))
        // Original preserved untouched.
        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")!!.category)
    }

    @Test
    fun `user rule override is never overwritten`() = runTest(testDispatcher) {
        mail.seed(message("m1", subject = "20% off sale"))
        intelligence.setClassification(
            ClassificationRecord(
                messageId = "m1",
                accountId = "acc1",
                category = MailCategory.IMPORTANT,
                confidence = 1.0f,
                source = ClassificationSource.USER_RULE,
                version = 0,
                explanation = null,
                classifiedAtEpochMs = 1L,
            ),
        )
        assertNull(useCase.classify("m1", "acc1"))
        assertEquals(MailCategory.IMPORTANT, intelligence.getClassification("m1")!!.category)
    }

    // ---- Idempotency & versioning (phase §33-34) ----

    @Test
    fun `same version is not reclassified`() = runTest(testDispatcher) {
        mail.seed(message("m1", subject = "Your verification code is 482910"))
        assertNotNull(useCase.classify("m1", "acc1"))
        // Second run: already current → skipped (idempotent).
        assertNull(useCase.classify("m1", "acc1"))
    }

    @Test
    fun `older version is reclassified`() = runTest(testDispatcher) {
        mail.seed(message("m1", subject = "Your verification code is 482910"))
        intelligence.setClassification(
            ClassificationRecord(
                messageId = "m1",
                accountId = "acc1",
                category = MailCategory.UNCLASSIFIED,
                confidence = 0.3f,
                source = ClassificationSource.DETERMINISTIC,
                version = DeterministicClassifier.VERSION - 1,
                explanation = "old",
                classifiedAtEpochMs = 1L,
            ),
        )
        val result = useCase.classify("m1", "acc1")
        assertNotNull(result)
        assertEquals(ClassifierCategory.SECURITY, result!!.category)
        assertEquals(
            DeterministicClassifier.VERSION,
            intelligence.getClassification("m1")!!.version,
        )
    }

    @Test
    fun `force reclassifies current version`() = runTest(testDispatcher) {
        mail.seed(message("m1", subject = "Your verification code is 482910"))
        assertNotNull(useCase.classify("m1", "acc1"))
        assertNotNull(useCase.classify("m1", "acc1", force = true))
    }

    // ---- Account isolation (phase §36) ----

    @Test
    fun `never classifies another accounts message`() = runTest(testDispatcher) {
        mail.seed(message("m1", account = "acc2", subject = "Your verification code"))
        assertNull(useCase.classify("m1", "acc1"))
        assertNull(intelligence.getClassification("m1"))
    }

    // ---- Batch (phase §49) ----

    @Test
    fun `classifyNew processes unclassified messages`() = runTest(testDispatcher) {
        mail.seed(
            message("m1", subject = "Your verification code is 1"),
            message("m2", subject = "Your order has shipped"),
            message("m3", account = "acc2", subject = "Your verification code is 2"),
        )
        val n = mailboxUseCase.classifyNew("acc1")
        assertEquals(2, n)
        assertEquals(MailCategory.SECURITY, intelligence.getClassification("m1")!!.category)
        assertEquals(MailCategory.RECEIPTS_ORDERS, intelligence.getClassification("m2")!!.category)
        assertNull(intelligence.getClassification("m3"))
    }

    @Test
    fun `classifyNew is bounded by limit`() = runTest(testDispatcher) {
        mail.seed(*(1..10).map { message("m$it", subject = "Your verification code $it") }
            .toTypedArray())
        val n = mailboxUseCase.classifyNew("acc1", limit = 3)
        assertEquals(3, n)
    }

    @Test
    fun `category mapping covers all classifier categories`() {
        for (c in ClassifierCategory.values()) {
            // Must not throw; UNCLASSIFIED maps to UNCLASSIFIED.
            c.toMailCategory()
        }
        assertEquals(MailCategory.SECURITY, ClassifierCategory.SECURITY.toMailCategory())
        assertEquals(
            MailCategory.RECEIPTS_ORDERS,
            ClassifierCategory.RECEIPTS_ORDERS.toMailCategory(),
        )
        assertEquals(MailCategory.UNCLASSIFIED, ClassifierCategory.UNCLASSIFIED.toMailCategory())
    }

    // ---- Fakes ----

    private class FakeClassifyMailRepository :
        com.greninjaop.mailorganizer.data.repository.MailRepository {
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

        // ---- Phase 8 company intelligence support (minimal stubs) ----

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
            category: com.greninjaop.mailorganizer.data.local.MailCategory,
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
            category: com.greninjaop.mailorganizer.data.local.MailCategory,
        ): Map<String, Int> = emptyMap()

        override suspend fun companyCountsForLabel(
            accountId: String,
            label: String,
        ): Map<String, Int> = emptyMap()

        override suspend fun companyMessageCounts(
            accountId: String,
        ): Map<String, Int> = emptyMap()
    }

    private class FakeIntelligenceRepository : IntelligenceRepository {
        private val classifications = mutableMapOf<String, ClassificationRecord>()

        override suspend fun getClassifications(messageIds: List<String>): Map<String, ClassificationRecord> =
            classifications.filterKeys { it in messageIds }

        override suspend fun upsertSender(sender: SenderRecord) = Unit
        override fun observeTopSenders(
            accountId: String,
            limit: Int,
        ): Flow<List<SenderRecord>> = MutableStateFlow(emptyList())

        override suspend fun getSenderByEmail(
            accountId: String,
            normalizedEmail: String,
        ): SenderRecord? = null

        override suspend fun recordSenderMessage(
            accountId: String,
            emailAddress: String,
            normalizedEmail: String,
            displayName: String?,
            domain: String,
        ): SenderRecord = SenderRecord(
            senderId = "sender:$accountId:$normalizedEmail",
            accountId = accountId,
            emailAddress = emailAddress,
            normalizedEmail = normalizedEmail,
            displayName = displayName,
            domain = domain,
            firstSeenEpochMs = 0L,
            lastSeenEpochMs = 0L,
            messageCount = 1,
        )

        override suspend fun getCompanyByDomain(
            accountId: String,
            normalizedDomain: String,
        ): com.greninjaop.mailorganizer.data.local.CompanyRecord? = null

        override suspend fun upsertCompany(company: CompanyRecord) = Unit
        override fun observeCompanyFilterList(
            accountId: String,
            limit: Int,
        ): Flow<List<CompanyRecord>> = MutableStateFlow(emptyList())

        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) = Unit

        override suspend fun setClassification(record: ClassificationRecord) {
            classifications[record.messageId] = record
        }

        override suspend fun getClassification(messageId: String): ClassificationRecord? =
            classifications[messageId]

        override fun observeByCategory(
            accountId: String,
            category: MailCategory,
            limit: Int,
        ): Flow<List<ClassificationRecord>> =
            MutableStateFlow(
                classifications.values
                    .filter { it.accountId == accountId && it.category == category }
                    .take(limit),
            )

        override suspend fun categoryCounts(
            accountId: String,
        ): Map<MailCategory, Int> =
            classifications.values
                .filter { it.accountId == accountId }
                .groupingBy { it.category }
                .eachCount()

        override fun observeByPriority(
            accountId: String,
            priority: com.greninjaop.mailorganizer.data.local.Priority,
            limit: Int,
        ): Flow<List<PriorityRecord>> = MutableStateFlow(emptyList())

        override suspend fun setPriority(record: PriorityRecord) = Unit
        override suspend fun getPriority(messageId: String): PriorityRecord? = null
        override suspend fun getPriorities(messageIds: List<String>): Map<String, PriorityRecord> =
            emptyMap()
        override suspend fun addActionItem(item: ActionItemRecord): Long = 0L
        override fun observeOpenActionItems(
            accountId: String,
            limit: Int,
        ): Flow<List<ActionItemRecord>> = MutableStateFlow(emptyList())

        override suspend fun completeActionItem(id: Long) = Unit
        override suspend fun dismissActionItem(id: Long) = Unit
        override suspend fun addExtractedItem(item: ExtractedItemRecord): Long = 0L
        override fun observeOpenExtracted(
            accountId: String,
            type: ExtractedItemType,
            limit: Int,
        ): Flow<List<ExtractedItemRecord>> = MutableStateFlow(emptyList())
    }
}
