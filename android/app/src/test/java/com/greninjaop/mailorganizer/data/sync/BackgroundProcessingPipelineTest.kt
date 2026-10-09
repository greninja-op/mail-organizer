package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.domain.actions.GenerateActionsUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMailboxUseCase
import com.greninjaop.mailorganizer.domain.priority.PrioritizeMessageUseCase
import com.greninjaop.mailorganizer.domain.temporal.ExtractMailboxUseCase
import com.greninjaop.mailorganizer.domain.temporal.ExtractTemporalUseCase
import com.greninjaop.mailorganizer.ui.mail.FakeIntelligenceRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 19 — Unit tests for [BackgroundProcessingPipeline].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundProcessingPipelineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)

    private lateinit var mailRepo: FakeMailRepository
    private lateinit var intelligenceRepo: FakeIntelligenceRepository

    private lateinit var companyIntelligence: CompanyIntelligenceUseCase
    private lateinit var classifyMailbox: ClassifyMailboxUseCase
    private lateinit var prioritizeMailbox: PrioritizeMailboxUseCase
    private lateinit var extractMailbox: ExtractMailboxUseCase
    private lateinit var generateActions: GenerateActionsUseCase

    @Before
    fun setUp() {
        mailRepo = FakeMailRepository()
        intelligenceRepo = FakeIntelligenceRepository()

        companyIntelligence = CompanyIntelligenceUseCase(
            mail = mailRepo,
            intelligence = intelligenceRepo,
            dispatchers = dispatchers,
            clock = { 1_000L },
        )
        val classifyMsg = ClassifyMessageUseCase(
            mail = mailRepo,
            intelligence = intelligenceRepo,
            dispatchers = dispatchers,
            clock = { 1_000L },
        )
        classifyMailbox = ClassifyMailboxUseCase(
            mail = mailRepo,
            classifyMessage = classifyMsg,
            dispatchers = dispatchers,
        )
        val prioritizeMsg = PrioritizeMessageUseCase(
            mail = mailRepo,
            intelligence = intelligenceRepo,
            dispatchers = dispatchers,
            clock = { 1_000L },
        )
        prioritizeMailbox = PrioritizeMailboxUseCase(
            mail = mailRepo,
            prioritizeMessage = prioritizeMsg,
            dispatchers = dispatchers,
        )
        val extractMsg = ExtractTemporalUseCase(
            mail = mailRepo,
            intelligence = intelligenceRepo,
            dispatchers = dispatchers,
            clock = { 1_000L },
        )
        extractMailbox = ExtractMailboxUseCase(
            mail = mailRepo,
            extractMessage = extractMsg,
            dispatchers = dispatchers,
        )
        generateActions = GenerateActionsUseCase(
            mail = mailRepo,
            intelligence = intelligenceRepo,
            dispatchers = dispatchers,
            clock = { 1_000L },
        )
    }

    @Test
    fun `pipeline runs all engines in sequence with account isolation`() = runTest(testDispatcher) {
        val accountId = "test-account"
        val otherAccount = "other-account"

        // Seed messages for both accounts
        val msg1 = MessageRecord(
            messageId = "m1",
            gmailMessageId = "gm1",
            threadId = "t1",
            accountId = accountId,
            fromAddress = "billing@aws.amazon.com",
            fromName = "AWS",
            toAddresses = listOf("user@domain.com"),
            subject = "Your AWS bill is ready for payment by Friday",
            snippet = "Please review your payment before the deadline",
            bodyText = "Please pay before Friday deadline",
            timestampEpochMs = 1_000L,
            unread = true,
        )
        val msgOther = MessageRecord(
            messageId = "m-other",
            gmailMessageId = "gm-other",
            threadId = "t-other",
            accountId = otherAccount,
            fromAddress = "news@tech.com",
            fromName = "Tech",
            toAddresses = listOf("other@domain.com"),
            subject = "Tech newsletter",
            snippet = "Weekly news",
            bodyText = "Weekly tech updates",
            timestampEpochMs = 1_000L,
            unread = true,
        )
        mailRepo.saveThreadWithMessages(
            com.greninjaop.mailorganizer.data.local.ThreadRecord(
                threadId = "t1",
                gmailThreadId = "gt1",
                accountId = accountId,
                subject = "Your AWS bill",
                latestMessageEpochMs = 1_000L,
            ),
            listOf(msg1),
        )
        mailRepo.saveThreadWithMessages(
            com.greninjaop.mailorganizer.data.local.ThreadRecord(
                threadId = "t-other",
                gmailThreadId = "gt-other",
                accountId = otherAccount,
                subject = "Tech news",
                latestMessageEpochMs = 1_000L,
            ),
            listOf(msgOther),
        )

        val pipeline = BackgroundProcessingPipeline(
            companyIntelligence = companyIntelligence,
            classifyMailbox = classifyMailbox,
            prioritizeMailbox = prioritizeMailbox,
            extractMailbox = extractMailbox,
            generateActions = generateActions,
            dispatchers = dispatchers,
            clock = { 2_000L },
        )

        val summary = pipeline.processAccount(accountId)

        assertEquals(accountId, summary.accountId)
        assertEquals(1, summary.attributedSenders)
        assertEquals(1, summary.classifiedMessages)
        assertEquals(1, summary.prioritizedMessages)

        // Verify other account was not touched (§43 Account Isolation)
        val otherClassified = intelligenceRepo.getClassification("m-other")
        assertTrue(otherClassified == null)
    }

    @Test
    fun `pipeline is idempotent and safe to re-run`() = runTest(testDispatcher) {
        val accountId = "acc-idempotent"
        val msg = MessageRecord(
            messageId = "m-idem",
            gmailMessageId = "gm-idem",
            threadId = "t-idem",
            accountId = accountId,
            fromAddress = "security@google.com",
            fromName = "Google",
            toAddresses = listOf("me@domain.com"),
            subject = "Security alert",
            snippet = "New sign-in",
            bodyText = "New sign-in detected",
            timestampEpochMs = 1_000L,
            unread = true,
        )
        mailRepo.saveThreadWithMessages(
            com.greninjaop.mailorganizer.data.local.ThreadRecord(
                threadId = "t-idem",
                gmailThreadId = "gt-idem",
                accountId = accountId,
                subject = "Security alert",
                latestMessageEpochMs = 1_000L,
            ),
            listOf(msg),
        )

        val pipeline = BackgroundProcessingPipeline(
            companyIntelligence = companyIntelligence,
            classifyMailbox = classifyMailbox,
            prioritizeMailbox = prioritizeMailbox,
            extractMailbox = extractMailbox,
            generateActions = generateActions,
            dispatchers = dispatchers,
            clock = { 2_000L },
        )

        val run1 = pipeline.processAccount(accountId)
        assertEquals(1, run1.classifiedMessages)

        // Second run on the same messages should process 0 new messages (§39)
        val run2 = pipeline.processAccount(accountId)
        assertEquals(0, run2.classifiedMessages)
    }
}
