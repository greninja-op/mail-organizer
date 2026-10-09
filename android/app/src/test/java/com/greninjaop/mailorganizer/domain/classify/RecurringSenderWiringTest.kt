package com.greninjaop.mailorganizer.domain.classify

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.classify.ClassifierCategory
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the Phase 8 recurring-sender wiring (Phase 8).
 *
 * The classifier's `isRecurringSender` flag must reflect the provider's
 * answer: a recurring correspondent with no bulk markers lands in
 * IMPORTANT via IMPORTANT_RECURRING_SENDER; without the provider the
 * same mail stays UNCLASSIFIED (Phase 7 behavior preserved).
 */
class RecurringSenderWiringTest {

    private val testDispatcher = StandardTestDispatcher()

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * No sender name on purpose: IMPORTANT_PERSONAL requires a named
     * sender, so these fixtures isolate the recurring-sender rule.
     */
    private fun message(from: String) = MessageRecord(
        messageId = "m1",
        gmailMessageId = null,
        threadId = "t1",
        accountId = "a1",
        fromAddress = from,
        fromName = null,
        subject = "quick question",
        snippet = null,
        bodyText = "Hey, do you have a minute to chat about the proposal?",
        timestampEpochMs = 1_800_000_000_000L,
    )

    /** Minimal MailRepository: only getMessage is exercised here. */
    private fun mailRepo(msg: MessageRecord): MailRepository =
        object : MailRepository {
            override suspend fun saveThreadWithMessages(
                thread: com.greninjaop.mailorganizer.data.local.ThreadRecord,
                messages: List<MessageRecord>,
            ) = Unit
            override fun observeThreads(accountId: String, limit: Int) =
                MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.ThreadRecord>())
            override fun observeMessages(threadId: String, limit: Int) =
                MutableStateFlow(emptyList<MessageRecord>())
            override fun observeUnread(accountId: String, limit: Int) =
                MutableStateFlow(emptyList<MessageRecord>())
            override fun observeStarred(accountId: String, limit: Int) =
                MutableStateFlow(emptyList<MessageRecord>())
            override suspend fun searchByText(accountId: String, query: String, limit: Int) =
                emptyList<MessageRecord>()
            override suspend fun setRead(messageId: String, read: Boolean) = Unit
            override suspend fun setStarred(messageId: String, starred: Boolean) = Unit
            override suspend fun countByAccount(accountId: String) = 0
            override suspend fun getMessagesByIds(messageIds: List<String>) =
                if ("m1" in messageIds) listOf(msg) else emptyList()
            override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) = null
            override suspend fun updateThreadAggregates(threadId: String) = Unit
            override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) =
                emptySet<String>()
            override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) =
                emptyList<String>()
            override suspend fun getMessage(messageId: String) =
                if (messageId == "m1") msg else null
            override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) =
                listOf(msg).take(limit)
            override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) =
                listOf(msg).take(limit)
            override fun observeByLabel(accountId: String, label: String, limit: Int) =
                MutableStateFlow(emptyList<MessageRecord>())
            override suspend fun setMessageCompanyId(messageId: String, companyId: String?) = Unit
            override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) =
                emptyList<MessageRecord>()
            override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int) =
                MutableStateFlow(emptyList<MessageRecord>())
            override fun observeMessagesByCompanyAndCategory(
                accountId: String,
                category: MailCategory,
                companyId: String,
                limit: Int,
            ) = MutableStateFlow(emptyList<MessageRecord>())
            override fun observeMessagesByCompanyAndLabel(
                accountId: String,
                companyId: String,
                label: String,
                limit: Int,
            ) = MutableStateFlow(emptyList<MessageRecord>())
            override suspend fun companyCountsForCategory(
                accountId: String,
                category: MailCategory,
            ) = emptyMap<String, Int>()
            override suspend fun companyCountsForLabel(accountId: String, label: String) =
                emptyMap<String, Int>()
        }

    /** Minimal IntelligenceRepository: captures the persisted record. */
    private class CapturingIntelligence : IntelligenceRepository {
        var last: ClassificationRecord? = null
        override suspend fun upsertSender(sender: com.greninjaop.mailorganizer.data.local.SenderRecord) = Unit
        override fun observeTopSenders(accountId: String, limit: Int) =
            MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>())
        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = null
        override suspend fun recordSenderMessage(
            accountId: String,
            emailAddress: String,
            normalizedEmail: String,
            displayName: String?,
            domain: String,
        ): com.greninjaop.mailorganizer.data.local.SenderRecord = throw UnsupportedOperationException()
        override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) = Unit
        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
        override fun observeCompanyFilterList(accountId: String, limit: Int) =
            MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.CompanyRecord>())
        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) = Unit
        override suspend fun setClassification(record: ClassificationRecord) {
            last = record
        }
        override suspend fun getClassification(messageId: String) = null
        override fun observeByCategory(accountId: String, category: MailCategory, limit: Int) =
            MutableStateFlow(emptyList<ClassificationRecord>())
        override suspend fun setPriority(record: com.greninjaop.mailorganizer.data.local.PriorityRecord) = Unit
        override suspend fun getPriority(messageId: String) = null
        override suspend fun getPriorities(messageIds: List<String>) =
            emptyMap<String, com.greninjaop.mailorganizer.data.local.PriorityRecord>()
        override suspend fun addActionItem(item: com.greninjaop.mailorganizer.data.local.ActionItemRecord) = 0L
        override fun observeOpenActionItems(accountId: String, limit: Int) =
            MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>())
        override suspend fun completeActionItem(id: Long) = Unit
        override suspend fun dismissActionItem(id: Long) = Unit
        override suspend fun addExtractedItem(item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord) = 0L
        override fun observeOpenExtracted(
            accountId: String,
            type: com.greninjaop.mailorganizer.data.local.ExtractedItemType,
            limit: Int,
        ) = MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>())
    }

    private fun useCase(
        msg: MessageRecord,
        provider: RecurringSenderProvider?,
    ): Pair<ClassifyMessageUseCase, CapturingIntelligence> {
        val intel = CapturingIntelligence()
        val uc = ClassifyMessageUseCase(
            mail = mailRepo(msg),
            intelligence = intel,
            dispatchers = dispatchers(),
            recurringSenderProvider = provider,
            clock = { 1_800_000_000_000L },
        )
        return uc to intel
    }

    @Test
    fun `recurring sender mail becomes important`() = runTest {
        val msg = message("sam@consulting.example")
        val (uc, intel) = useCase(
            msg,
            provider = object : RecurringSenderProvider {
                override suspend fun isRecurring(
                    accountId: String,
                    normalizedEmail: String,
                ) = true
            },
        )
        val result = uc.classify("m1", "a1", force = true)
        assertNotNull(result)
        assertEquals(ClassifierCategory.IMPORTANT, result!!.category)
        assertTrue(result.firingRuleIds.contains("IMPORTANT_RECURRING_SENDER"))
        assertEquals(MailCategory.IMPORTANT, intel.last?.category)
    }

    @Test
    fun `without provider the same mail stays unclassified`() = runTest {
        val msg = message("sam@consulting.example")
        val (uc, _) = useCase(msg, provider = null)
        val result = uc.classify("m1", "a1", force = true)
        assertNotNull(result)
        // Phase 7 behavior preserved: no fabricated importance.
        assertEquals(ClassifierCategory.UNCLASSIFIED, result!!.category)
    }

    @Test
    fun `recurring signal is vetoed by bulk markers`() = runTest {
        val msg = message("news@consulting.example").copy(
            subject = "Weekly digest — unsubscribe here",
            bodyText = "Our weekly digest. Click unsubscribe to opt out.",
        )
        val (uc, _) = useCase(
            msg,
            provider = object : RecurringSenderProvider {
                override suspend fun isRecurring(
                    accountId: String,
                    normalizedEmail: String,
                ) = true
            },
        )
        val result = uc.classify("m1", "a1", force = true)
        assertNotNull(result)
        // A recurring newsletter is still a newsletter — never IMPORTANT.
        assertTrue(result!!.category != ClassifierCategory.IMPORTANT)
    }

    @Test
    fun `provider receives normalized email`() = runTest {
        val msg = message("Sam@Consulting.Example")
        var seen = ""
        val (uc, _) = useCase(
            msg,
            provider = object : RecurringSenderProvider {
                override suspend fun isRecurring(
                    accountId: String,
                    normalizedEmail: String,
                ): Boolean {
                    seen = normalizedEmail
                    return false
                }
            },
        )
        uc.classify("m1", "a1", force = true)
        assertEquals("sam@consulting.example", seen)
    }
}
