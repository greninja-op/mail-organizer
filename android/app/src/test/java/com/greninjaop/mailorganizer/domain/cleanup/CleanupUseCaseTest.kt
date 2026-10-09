package com.greninjaop.mailorganizer.domain.cleanup

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateStatus
import com.greninjaop.mailorganizer.core.cleanup.CleanupCandidateType
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.SenderRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = AppDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
    )

    private val fakeMail = FakeMailRepository()
    private val fakeIntelligence = FakeIntelligenceRepository()

    private val cleanupUseCase = CleanupUseCase(
        mail = fakeMail,
        intelligence = fakeIntelligence,
        dispatchers = dispatchers,
    )

    @Test
    fun getCleanupCandidates_extractsCandidatesForAccount() = runTest(testDispatcher) {
        val accountId = "acc_test"
        val m1 = createMessage("m1", accountId, "newsletter@daily.com", "Daily News #10")
        val m2 = createMessage("m2", accountId, "security@bank.com", "Security verification code")
        fakeMail.addMessage(m1)
        fakeMail.addMessage(m2)

        fakeIntelligence.setClassification(
            ClassificationRecord(
                id = 1,
                messageId = "m1",
                accountId = accountId,
                category = MailCategory.NEWSLETTERS,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = 1,
                explanation = "Newsletter detected",
                overridden = false,
                classifiedAtEpochMs = 1000L,
            )
        )
        fakeIntelligence.setClassification(
            ClassificationRecord(
                id = 2,
                messageId = "m2",
                accountId = accountId,
                category = MailCategory.SECURITY,
                confidence = 0.95f,
                source = ClassificationSource.DETERMINISTIC,
                version = 1,
                explanation = "Security alert",
                overridden = false,
                classifiedAtEpochMs = 1000L,
            )
        )

        val candidates = cleanupUseCase.getCleanupCandidates(accountId)
        assertEquals(2, candidates.size)

        val newsletterCandidate = candidates.first { it.messageId == "m1" }
        assertEquals(CleanupCandidateType.NEWSLETTER, newsletterCandidate.candidateType)
        assertEquals(false, newsletterCandidate.isProtected)

        val securityCandidate = candidates.first { it.messageId == "m2" }
        assertEquals(true, securityCandidate.isProtected)
    }

    @Test
    fun getCleanupGroups_groupsCandidatesExcludingProtected() = runTest(testDispatcher) {
        val accountId = "acc_test"
        val m1 = createMessage("m1", accountId, "newsletter@daily.com", "Daily News #10")
        fakeMail.addMessage(m1)
        fakeIntelligence.setClassification(
            ClassificationRecord(
                id = 1,
                messageId = "m1",
                accountId = accountId,
                category = MailCategory.NEWSLETTERS,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = 1,
                explanation = "Newsletter detected",
                overridden = false,
                classifiedAtEpochMs = 1000L,
            )
        )

        val groups = cleanupUseCase.getCleanupGroups(accountId)
        assertEquals(1, groups.size)
        assertEquals("Newsletters", groups[0].title)
        assertEquals(1, groups[0].candidateCount)
    }

    @Test
    fun updateCandidateStatus_dismissesCandidate() = runTest(testDispatcher) {
        val accountId = "acc_test"
        val m1 = createMessage("m1", accountId, "newsletter@daily.com", "Daily News #10")
        fakeMail.addMessage(m1)
        fakeIntelligence.setClassification(
            ClassificationRecord(
                id = 1,
                messageId = "m1",
                accountId = accountId,
                category = MailCategory.NEWSLETTERS,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = 1,
                explanation = "Newsletter detected",
                overridden = false,
                classifiedAtEpochMs = 1000L,
            )
        )

        val candidatesBefore = cleanupUseCase.getCleanupCandidates(accountId)
        assertEquals(1, candidatesBefore.size)

        cleanupUseCase.updateCandidateStatus(candidatesBefore[0].candidateId, CleanupCandidateStatus.DISMISSED)

        val candidatesAfter = cleanupUseCase.getCleanupCandidates(accountId)
        assertEquals(0, candidatesAfter.size)
    }

    private fun createMessage(id: String, accountId: String, sender: String, subject: String): MessageRecord {
        return MessageRecord(
            messageId = id,
            gmailMessageId = "g_$id",
            threadId = "t_$id",
            accountId = accountId,
            fromAddress = sender,
            fromName = "Sender",
            toAddresses = listOf("user@example.com"),
            ccAddresses = emptyList(),
            subject = subject,
            snippet = "Snippet",
            bodyText = "Body text",
            bodyHtml = null,
            attachments = emptyList(),
            timestampEpochMs = 1000L,
            unread = false,
            starred = false,
            labels = emptyList(),
            sizeBytes = 100L,
            companyId = null,
        )
    }

    private class FakeMailRepository : MailRepository {
        private val messages = mutableMapOf<String, MessageRecord>()

        fun addMessage(message: MessageRecord) {
            messages[message.messageId] = message
        }

        override suspend fun saveThreadWithMessages(
            thread: com.greninjaop.mailorganizer.data.local.ThreadRecord,
            messages: List<MessageRecord>,
        ) {
            messages.forEach { this.messages[it.messageId] = it }
        }

        override fun observeThreads(accountId: String, limit: Int) = emptyFlow<List<com.greninjaop.mailorganizer.data.local.ThreadRecord>>()
        override fun observeMessages(threadId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override fun observeUnread(accountId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override fun observeStarred(accountId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override suspend fun searchByText(accountId: String, query: String, limit: Int) = emptyList<MessageRecord>()
        override suspend fun setRead(messageId: String, read: Boolean) {}
        override suspend fun setStarred(messageId: String, starred: Boolean) {}
        override suspend fun countByAccount(accountId: String) = messages.values.count { it.accountId == accountId }
        override suspend fun getMessagesByIds(messageIds: List<String>) = messageIds.mapNotNull { messages[it] }
        override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) = null
        override suspend fun updateThreadAggregates(threadId: String) {}
        override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) = emptySet<String>()
        override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) = emptyList<String>()
        override suspend fun getMessage(messageId: String) = messages[messageId]
        override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override suspend fun getUnextractedMessages(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override fun observeByLabel(accountId: String, label: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override suspend fun setMessageCompanyId(messageId: String, companyId: String?) {}
        override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) = emptyList<MessageRecord>()
        override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override fun observeMessagesByCompanyAndCategory(accountId: String, category: MailCategory, companyId: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override fun observeMessagesByCompanyAndLabel(accountId: String, companyId: String, label: String, limit: Int) = emptyFlow<List<MessageRecord>>()
        override suspend fun companyCountsForCategory(accountId: String, category: MailCategory) = emptyMap<String, Int>()
        override suspend fun companyCountsForLabel(accountId: String, label: String) = emptyMap<String, Int>()
        override suspend fun companyMessageCounts(accountId: String) = emptyMap<String, Int>()
        override suspend fun getMessageIdsByAccount(accountId: String, limit: Int) = messages.values.filter { it.accountId == accountId }.map { it.messageId }.take(limit)
        override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int) = emptyList<String>()
        override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int) = emptyList<String>()
        override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int) = emptyList<String>()
    }

    private class FakeIntelligenceRepository : IntelligenceRepository {
        private val classifications = mutableMapOf<String, ClassificationRecord>()
        private val priorities = mutableMapOf<String, PriorityRecord>()
        private val senders = mutableMapOf<String, SenderRecord>()

        override suspend fun upsertSender(sender: SenderRecord) {
            senders[sender.normalizedEmail] = sender
        }
        override fun observeTopSenders(accountId: String, limit: Int) = emptyFlow<List<SenderRecord>>()
        override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = senders[normalizedEmail]
        override suspend fun recordSenderMessage(accountId: String, emailAddress: String, normalizedEmail: String, displayName: String?, domain: String): SenderRecord {
            val record = SenderRecord(
                senderId = "s_$normalizedEmail",
                accountId = accountId,
                emailAddress = emailAddress,
                normalizedEmail = normalizedEmail,
                displayName = displayName,
                domain = domain,
                firstSeenEpochMs = 1000L,
                lastSeenEpochMs = 1000L,
                messageCount = (senders[normalizedEmail]?.messageCount ?: 0) + 1,
            )
            senders[normalizedEmail] = record
            return record
        }
        override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) {}
        override fun observeCompanyFilterList(accountId: String, limit: Int) = emptyFlow<List<com.greninjaop.mailorganizer.data.local.CompanyRecord>>()
        override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
        override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
        override suspend fun setClassification(record: ClassificationRecord) {
            classifications[record.messageId] = record
        }
        override suspend fun getClassification(messageId: String) = classifications[messageId]
        override suspend fun deleteClassification(messageId: String) {
            classifications.remove(messageId)
        }
        override suspend fun getClassifications(messageIds: List<String>) = messageIds.mapNotNull { classifications[it] }.associateBy { it.messageId }
        override fun observeByCategory(accountId: String, category: MailCategory, limit: Int) = emptyFlow<List<ClassificationRecord>>()
        override suspend fun categoryCounts(accountId: String) = emptyMap<MailCategory, Int>()
        override suspend fun setPriority(record: PriorityRecord) {
            priorities[record.messageId] = record
        }
        override suspend fun getPriority(messageId: String) = priorities[messageId]
        override suspend fun deletePriority(messageId: String) {
            priorities.remove(messageId)
        }
        override suspend fun getPriorities(messageIds: List<String>) = messageIds.mapNotNull { priorities[it] }.associateBy { it.messageId }
        override fun observeByPriority(accountId: String, priority: Priority, limit: Int) = emptyFlow<List<PriorityRecord>>()
        override suspend fun addActionItem(item: com.greninjaop.mailorganizer.data.local.ActionItemRecord) = 1L
        override fun observeOpenActionItems(accountId: String, limit: Int) = emptyFlow<List<com.greninjaop.mailorganizer.data.local.ActionItemRecord>>()
        override suspend fun completeActionItem(id: Long) {}
        override suspend fun dismissActionItem(id: Long) {}
        override suspend fun getActionItem(id: Long) = null
        override suspend fun getActionItemsByMessage(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
        override suspend fun getOpenActionItemsByThread(threadId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
        override suspend fun setActionItemStatus(id: Long, status: com.greninjaop.mailorganizer.core.actions.ActionStatus) {}
        override suspend fun deleteActionItems(ids: List<Long>) {}
        override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long) = 0
        override suspend fun addExtractedItem(item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord) = 1L
        override fun observeOpenExtracted(accountId: String, type: com.greninjaop.mailorganizer.data.local.ExtractedItemType, limit: Int) = emptyFlow<List<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>>()
        override suspend fun getExtractedItems(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>()
        override suspend fun deleteExtractedItems(ids: List<Long>) {}
    }
}
