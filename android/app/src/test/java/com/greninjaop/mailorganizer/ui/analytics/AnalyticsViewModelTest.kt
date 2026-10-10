package com.greninjaop.mailorganizer.ui.analytics

import com.greninjaop.mailorganizer.core.analytics.AnalyticsDateRange
import com.greninjaop.mailorganizer.core.analytics.AnalyticsSnapshot
import com.greninjaop.mailorganizer.core.analytics.UNIFIED_ACCOUNT_ID
import com.greninjaop.mailorganizer.core.analytics.InboxHealthMetrics
import com.greninjaop.mailorganizer.core.analytics.NoiseMetrics
import com.greninjaop.mailorganizer.core.analytics.OrganizationMetrics
import com.greninjaop.mailorganizer.core.analytics.PriorityMetrics
import com.greninjaop.mailorganizer.core.analytics.RuleCorrectionMetrics
import com.greninjaop.mailorganizer.core.analytics.SourceMetrics
import com.greninjaop.mailorganizer.core.analytics.TimeMetrics
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.domain.analytics.AnalyticsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val account = AccountRecord(
        accountId = "acc_test",
        emailAddress = "user@example.com",
        displayName = "User",
        createdAtEpochMs = 1700000000000L,
        isEnabled = true,
    )

    private val fakeAccountRepo = object : AccountRepository {
        override suspend fun upsert(account: AccountRecord) {}
        override suspend fun getById(accountId: String): AccountRecord? = account
        override fun observeAll(): Flow<List<AccountRecord>> = flowOf(listOf(account))
        override fun observeEnabled(): Flow<List<AccountRecord>> = flowOf(listOf(account))
        override suspend fun updateConnectionState(accountId: String, state: com.greninjaop.mailorganizer.data.local.ConnectionState) {}
        override suspend fun recordSync(accountId: String, syncEpochMs: Long) {}
        override suspend fun setEnabled(accountId: String, enabled: Boolean) {}
        override suspend fun deleteById(accountId: String) {}
    }

    private val activeSelectionFlow = MutableStateFlow<AccountSelection>(AccountSelection.Single("acc_test"))
    private val fakePrefs = object : ActiveAccountPreferences {
        override val activeSelection: Flow<AccountSelection> get() = activeSelectionFlow
        override suspend fun setActiveSelection(selection: AccountSelection) { activeSelectionFlow.value = selection }
    }

    private val dummySnapshot = AnalyticsSnapshot(
        accountId = "acc_test",
        isUnified = false,
        accountEmail = "user@example.com",
        dateRange = AnalyticsDateRange.LAST_7_DAYS,
        inboxHealth = InboxHealthMetrics(10, 2, 1, 1, 0, 2),
        organization = OrganizationMetrics(emptyMap(), 8, 2, 0, 0, 0.8f),
        priority = PriorityMetrics(emptyMap()),
        sources = SourceMetrics(emptyList(), emptyList(), emptyList(), 5),
        noise = NoiseMetrics(1, 1, 0, 0, 0, emptyList(), 0.2f),
        time = TimeMetrics(emptyList(), 0, 0, 0, 0, 0, null),
        ruleCorrections = RuleCorrectionMetrics(0, 0, 0, emptyList()),
        generatedInsights = emptyList(),
        lastSyncEpochMs = 1000L,
        generatedAtEpochMs = 2000L,
        hasEnoughData = true,
    )

    private val fakeUseCase = object : AnalyticsUseCase(
        mail = object : com.greninjaop.mailorganizer.data.repository.MailRepository {
            override suspend fun saveThreadWithMessages(thread: com.greninjaop.mailorganizer.data.local.ThreadRecord, messages: List<com.greninjaop.mailorganizer.data.local.MessageRecord>) {}
            override fun observeThreads(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ThreadRecord>())
            override fun observeMessages(threadId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override fun observeUnread(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override fun observeStarred(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override suspend fun searchByText(accountId: String, query: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
            override suspend fun setRead(messageId: String, read: Boolean) {}
            override suspend fun setStarred(messageId: String, starred: Boolean) {}
            override suspend fun countByAccount(accountId: String) = 0
            override suspend fun getMessagesByIds(messageIds: List<String>) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
            override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) = null
            override suspend fun updateThreadAggregates(threadId: String) {}
            override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) = emptySet<String>()
            override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) = emptyList<String>()
            override suspend fun getMessage(messageId: String) = null
            override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
            override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
            override suspend fun getUnextractedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
            override fun observeByLabel(accountId: String, label: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override suspend fun setMessageCompanyId(messageId: String, companyId: String?) {}
            override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
            override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override fun observeMessagesByCompanyAndCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, companyId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override fun observeMessagesByCompanyAndLabel(accountId: String, companyId: String, label: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
            override suspend fun companyCountsForCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory) = emptyMap<String, Int>()
            override suspend fun companyCountsForLabel(accountId: String, label: String) = emptyMap<String, Int>()
            override suspend fun companyMessageCounts(accountId: String) = emptyMap<String, Int>()
            override suspend fun getMessageIdsByAccount(accountId: String, limit: Int) = emptyList<String>()
            override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int) = emptyList<String>()
            override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int) = emptyList<String>()
            override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int) = emptyList<String>()
        },
        intelligence = object : com.greninjaop.mailorganizer.data.repository.IntelligenceRepository {
            override suspend fun upsertSender(sender: com.greninjaop.mailorganizer.data.local.SenderRecord) {}
            override fun observeTopSenders(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>())
            override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = null
            override suspend fun recordSenderMessage(accountId: String, emailAddress: String, normalizedEmail: String, displayName: String?, domain: String) = com.greninjaop.mailorganizer.data.local.SenderRecord("s1", accountId, emailAddress, normalizedEmail, displayName, domain, 0L, 0L, 1)
            override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) {}
            override fun observeCompanyFilterList(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.CompanyRecord>())
            override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
            override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
            override suspend fun setClassification(record: com.greninjaop.mailorganizer.data.local.ClassificationRecord) {}
            override suspend fun deleteClassification(messageId: String) {}
            override suspend fun getClassification(messageId: String) = null
            override suspend fun getClassifications(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.ClassificationRecord>()
            override fun observeByCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ClassificationRecord>())
            override suspend fun categoryCounts(accountId: String) = emptyMap<com.greninjaop.mailorganizer.data.local.MailCategory, Int>()
            override suspend fun setPriority(record: com.greninjaop.mailorganizer.data.local.PriorityRecord) {}
            override suspend fun deletePriority(messageId: String) {}
            override suspend fun getPriority(messageId: String) = null
            override suspend fun getPriorities(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.PriorityRecord>()
            override fun observeByPriority(accountId: String, priority: com.greninjaop.mailorganizer.data.local.Priority, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.PriorityRecord>())
            override suspend fun addActionItem(item: com.greninjaop.mailorganizer.data.local.ActionItemRecord) = 1L
            override fun observeOpenActionItems(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>())
            override suspend fun completeActionItem(id: Long) {}
            override suspend fun dismissActionItem(id: Long) {}
            override suspend fun getActionItem(id: Long) = null
            override suspend fun getActionItemsByMessage(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
            override suspend fun getOpenActionItemsByThread(threadId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
            override suspend fun setActionItemStatus(id: Long, status: com.greninjaop.mailorganizer.core.actions.ActionStatus) {}
            override suspend fun deleteActionItems(ids: List<Long>) {}
            override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long) = 0
            override suspend fun addExtractedItem(item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord) = 1L
            override fun observeOpenExtracted(accountId: String, type: com.greninjaop.mailorganizer.data.local.ExtractedItemType, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>())
            override suspend fun getExtractedItems(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>()
            override suspend fun deleteExtractedItems(ids: List<Long>) {}
        },
        accounts = fakeAccountRepo,
        syncState = object : com.greninjaop.mailorganizer.data.repository.SyncStateRepository {
            override suspend fun ensureForAccount(accountId: String) = com.greninjaop.mailorganizer.data.local.SyncStateRecord(accountId = accountId)
            override fun observe(accountId: String) = flowOf(null)
            override suspend fun markAttempt(accountId: String, status: com.greninjaop.mailorganizer.data.local.SyncStatus, errorCode: String?) {}
            override suspend fun markSuccess(accountId: String, cursor: String?) {}
            override suspend fun updateCursor(accountId: String, cursor: String) {}
        },
        ruleRepository = object : com.greninjaop.mailorganizer.data.repository.RuleRepository {
            override suspend fun addRule(rule: com.greninjaop.mailorganizer.data.local.UserRuleRecord) = 1L
            override suspend fun deleteRule(id: Long) {}
            override fun observeEnabledRules(accountId: String) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.UserRuleRecord>())
            override suspend fun getEnabledRules(accountId: String) = emptyList<com.greninjaop.mailorganizer.data.local.UserRuleRecord>()
            override fun observeAllRules() = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.UserRuleRecord>())
            override suspend fun recordCorrection(correction: com.greninjaop.mailorganizer.data.local.UserCorrectionRecord) {}
            override suspend fun getCorrection(accountId: String, scope: com.greninjaop.mailorganizer.data.local.CorrectionScope, scopeKey: String, field: com.greninjaop.mailorganizer.data.local.CorrectionField) = null
            override suspend fun updateRule(rule: com.greninjaop.mailorganizer.data.local.UserRuleRecord) {}
            override suspend fun getRule(id: Long) = null
            override suspend fun setRuleEnabled(id: Long, enabled: Boolean) {}
            override suspend fun getAllRules(accountId: String) = emptyList<com.greninjaop.mailorganizer.data.local.UserRuleRecord>()
            override suspend fun getCorrections(accountId: String, lookups: List<com.greninjaop.mailorganizer.data.repository.CorrectionLookup>) = emptyMap<com.greninjaop.mailorganizer.data.repository.CorrectionLookup, com.greninjaop.mailorganizer.data.local.UserCorrectionRecord>()
            override suspend fun deleteCorrection(accountId: String, scope: com.greninjaop.mailorganizer.data.local.CorrectionScope, scopeKey: String, field: com.greninjaop.mailorganizer.data.local.CorrectionField) {}
        },
        conversationUseCase = com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase(
            mail = object : com.greninjaop.mailorganizer.data.repository.MailRepository {
                override suspend fun saveThreadWithMessages(thread: com.greninjaop.mailorganizer.data.local.ThreadRecord, messages: List<com.greninjaop.mailorganizer.data.local.MessageRecord>) {}
                override fun observeThreads(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ThreadRecord>())
                override fun observeMessages(threadId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeUnread(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeStarred(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override suspend fun searchByText(accountId: String, query: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun setRead(messageId: String, read: Boolean) {}
                override suspend fun setStarred(messageId: String, starred: Boolean) {}
                override suspend fun countByAccount(accountId: String) = 0
                override suspend fun getMessagesByIds(messageIds: List<String>) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) = null
                override suspend fun updateThreadAggregates(threadId: String) {}
                override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) = emptySet<String>()
                override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) = emptyList<String>()
                override suspend fun getMessage(messageId: String) = null
                override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun getUnextractedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override fun observeByLabel(accountId: String, label: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override suspend fun setMessageCompanyId(messageId: String, companyId: String?) {}
                override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeMessagesByCompanyAndCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, companyId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeMessagesByCompanyAndLabel(accountId: String, companyId: String, label: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override suspend fun companyCountsForCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory) = emptyMap<String, Int>()
                override suspend fun companyCountsForLabel(accountId: String, label: String) = emptyMap<String, Int>()
                override suspend fun companyMessageCounts(accountId: String) = emptyMap<String, Int>()
                override suspend fun getMessageIdsByAccount(accountId: String, limit: Int) = emptyList<String>()
                override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int) = emptyList<String>()
                override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int) = emptyList<String>()
                override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int) = emptyList<String>()
            },
            intelligence = object : com.greninjaop.mailorganizer.data.repository.IntelligenceRepository {
                override suspend fun upsertSender(sender: com.greninjaop.mailorganizer.data.local.SenderRecord) {}
                override fun observeTopSenders(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>())
                override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = null
                override suspend fun recordSenderMessage(accountId: String, emailAddress: String, normalizedEmail: String, displayName: String?, domain: String) = com.greninjaop.mailorganizer.data.local.SenderRecord("s1", accountId, emailAddress, normalizedEmail, displayName, domain, 0L, 0L, 1)
                override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) {}
                override fun observeCompanyFilterList(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.CompanyRecord>())
                override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
                override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
                override suspend fun setClassification(record: com.greninjaop.mailorganizer.data.local.ClassificationRecord) {}
                override suspend fun deleteClassification(messageId: String) {}
                override suspend fun getClassification(messageId: String) = null
                override suspend fun getClassifications(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.ClassificationRecord>()
                override fun observeByCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ClassificationRecord>())
                override suspend fun categoryCounts(accountId: String) = emptyMap<com.greninjaop.mailorganizer.data.local.MailCategory, Int>()
                override suspend fun setPriority(record: com.greninjaop.mailorganizer.data.local.PriorityRecord) {}
                override suspend fun deletePriority(messageId: String) {}
                override suspend fun getPriority(messageId: String) = null
                override suspend fun getPriorities(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.PriorityRecord>()
                override fun observeByPriority(accountId: String, priority: com.greninjaop.mailorganizer.data.local.Priority, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.PriorityRecord>())
                override suspend fun addActionItem(item: com.greninjaop.mailorganizer.data.local.ActionItemRecord) = 1L
                override fun observeOpenActionItems(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>())
                override suspend fun completeActionItem(id: Long) {}
                override suspend fun dismissActionItem(id: Long) {}
                override suspend fun getActionItem(id: Long) = null
                override suspend fun getActionItemsByMessage(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
                override suspend fun getOpenActionItemsByThread(threadId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
                override suspend fun setActionItemStatus(id: Long, status: com.greninjaop.mailorganizer.core.actions.ActionStatus) {}
                override suspend fun deleteActionItems(ids: List<Long>) {}
                override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long) = 0
                override suspend fun addExtractedItem(item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord) = 1L
                override fun observeOpenExtracted(accountId: String, type: com.greninjaop.mailorganizer.data.local.ExtractedItemType, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>())
                override suspend fun getExtractedItems(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>()
                override suspend fun deleteExtractedItems(ids: List<Long>) {}
            },
            accounts = fakeAccountRepo,
            dispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(testDispatcher, testDispatcher, testDispatcher),
        ),
        cleanupUseCase = com.greninjaop.mailorganizer.domain.cleanup.CleanupUseCase(
            mail = object : com.greninjaop.mailorganizer.data.repository.MailRepository {
                override suspend fun saveThreadWithMessages(thread: com.greninjaop.mailorganizer.data.local.ThreadRecord, messages: List<com.greninjaop.mailorganizer.data.local.MessageRecord>) {}
                override fun observeThreads(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ThreadRecord>())
                override fun observeMessages(threadId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeUnread(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeStarred(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override suspend fun searchByText(accountId: String, query: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun setRead(messageId: String, read: Boolean) {}
                override suspend fun setStarred(messageId: String, starred: Boolean) {}
                override suspend fun countByAccount(accountId: String) = 0
                override suspend fun getMessagesByIds(messageIds: List<String>) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun getThreadByGmailId(accountId: String, gmailThreadId: String) = null
                override suspend fun updateThreadAggregates(threadId: String) {}
                override suspend fun existingGmailIds(accountId: String, gmailIds: List<String>) = emptySet<String>()
                override suspend fun deleteMessagesByGmailIds(accountId: String, gmailIds: List<String>) = emptyList<String>()
                override suspend fun getMessage(messageId: String) = null
                override suspend fun getUnclassifiedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun getUnprioritizedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override suspend fun getUnextractedMessages(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override fun observeByLabel(accountId: String, label: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override suspend fun setMessageCompanyId(messageId: String, companyId: String?) {}
                override suspend fun getMessagesWithoutCompany(accountId: String, limit: Int) = emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>()
                override fun observeMessagesByCompany(accountId: String, companyId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeMessagesByCompanyAndCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, companyId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override fun observeMessagesByCompanyAndLabel(accountId: String, companyId: String, label: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.MessageRecord>())
                override suspend fun companyCountsForCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory) = emptyMap<String, Int>()
                override suspend fun companyCountsForLabel(accountId: String, label: String) = emptyMap<String, Int>()
                override suspend fun companyMessageCounts(accountId: String) = emptyMap<String, Int>()
                override suspend fun getMessageIdsByAccount(accountId: String, limit: Int) = emptyList<String>()
                override suspend fun getMessageIdsBySender(accountId: String, email: String, limit: Int) = emptyList<String>()
                override suspend fun getMessageIdsByDomain(accountId: String, domain: String, limit: Int) = emptyList<String>()
                override suspend fun getMessageIdsByCompany(accountId: String, companyId: String, limit: Int) = emptyList<String>()
            },
            intelligence = object : com.greninjaop.mailorganizer.data.repository.IntelligenceRepository {
                override suspend fun upsertSender(sender: com.greninjaop.mailorganizer.data.local.SenderRecord) {}
                override fun observeTopSenders(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>())
                override suspend fun getSenderByEmail(accountId: String, normalizedEmail: String) = null
                override suspend fun recordSenderMessage(accountId: String, emailAddress: String, normalizedEmail: String, displayName: String?, domain: String) = com.greninjaop.mailorganizer.data.local.SenderRecord("s1", accountId, emailAddress, normalizedEmail, displayName, domain, 0L, 0L, 1)
                override suspend fun upsertCompany(company: com.greninjaop.mailorganizer.data.local.CompanyRecord) {}
                override fun observeCompanyFilterList(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.CompanyRecord>())
                override suspend fun setCompanyPinned(companyId: String, pinned: Boolean) {}
                override suspend fun getCompanyByDomain(accountId: String, normalizedDomain: String) = null
                override suspend fun setClassification(record: com.greninjaop.mailorganizer.data.local.ClassificationRecord) {}
                override suspend fun deleteClassification(messageId: String) {}
                override suspend fun getClassification(messageId: String) = null
                override suspend fun getClassifications(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.ClassificationRecord>()
                override fun observeByCategory(accountId: String, category: com.greninjaop.mailorganizer.data.local.MailCategory, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ClassificationRecord>())
                override suspend fun categoryCounts(accountId: String) = emptyMap<com.greninjaop.mailorganizer.data.local.MailCategory, Int>()
                override suspend fun setPriority(record: com.greninjaop.mailorganizer.data.local.PriorityRecord) {}
                override suspend fun deletePriority(messageId: String) {}
                override suspend fun getPriority(messageId: String) = null
                override suspend fun getPriorities(messageIds: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.PriorityRecord>()
                override fun observeByPriority(accountId: String, priority: com.greninjaop.mailorganizer.data.local.Priority, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.PriorityRecord>())
                override suspend fun addActionItem(item: com.greninjaop.mailorganizer.data.local.ActionItemRecord) = 1L
                override fun observeOpenActionItems(accountId: String, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>())
                override suspend fun completeActionItem(id: Long) {}
                override suspend fun dismissActionItem(id: Long) {}
                override suspend fun getActionItem(id: Long) = null
                override suspend fun getActionItemsByMessage(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
                override suspend fun getOpenActionItemsByThread(threadId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>()
                override suspend fun setActionItemStatus(id: Long, status: com.greninjaop.mailorganizer.core.actions.ActionStatus) {}
                override suspend fun deleteActionItems(ids: List<Long>) {}
                override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long) = 0
                override suspend fun addExtractedItem(item: com.greninjaop.mailorganizer.data.local.ExtractedItemRecord) = 1L
                override fun observeOpenExtracted(accountId: String, type: com.greninjaop.mailorganizer.data.local.ExtractedItemType, limit: Int) = flowOf(emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>())
                override suspend fun getExtractedItems(messageId: String) = emptyList<com.greninjaop.mailorganizer.data.local.ExtractedItemRecord>()
                override suspend fun deleteExtractedItems(ids: List<Long>) {}
            },
            dispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(testDispatcher, testDispatcher, testDispatcher),
        ),
        activeAccountPreferences = fakePrefs,
        dispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(testDispatcher, testDispatcher, testDispatcher),
    ) {
        override suspend fun getAnalyticsSnapshot(
            targetAccountId: String?,
            dateRange: AnalyticsDateRange,
        ): AnalyticsSnapshot {
            return dummySnapshot.copy(
                accountId = targetAccountId ?: "acc_test",
                dateRange = dateRange,
            )
        }
    }

    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AnalyticsViewModel(
            analyticsUseCase = fakeUseCase,
            accountRepository = fakeAccountRepo,
            activeAccountPreferences = fakePrefs,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads snapshot for active account and default range`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Success", state is AnalyticsUiState.Success)
        val snapshot = (state as AnalyticsUiState.Success).snapshot
        assertEquals("acc_test", snapshot.accountId)
        assertEquals(AnalyticsDateRange.LAST_7_DAYS, viewModel.selectedRange.value)
    }

    @Test
    fun `setDateRange triggers reload with new date range`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.setDateRange(AnalyticsDateRange.LAST_30_DAYS)
        advanceUntilIdle()

        assertEquals(AnalyticsDateRange.LAST_30_DAYS, viewModel.selectedRange.value)
        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertEquals(AnalyticsDateRange.LAST_30_DAYS, state.snapshot.dateRange)
    }

    @Test
    fun `account switch updates currentAccount and reloads analytics`() = runTest(testDispatcher) {
        advanceUntilIdle()

        activeSelectionFlow.value = AccountSelection.Unified
        advanceUntilIdle()

        val account = viewModel.currentAccount.value
        assertEquals(UNIFIED_ACCOUNT_ID, account?.accountId)

        val state = viewModel.uiState.value as AnalyticsUiState.Success
        assertEquals(UNIFIED_ACCOUNT_ID, state.snapshot.accountId)
    }
}
