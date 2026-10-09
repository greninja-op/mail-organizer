package com.greninjaop.mailorganizer.domain.conversation

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.conversation.ConversationState
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import com.greninjaop.mailorganizer.ui.mail.FakeIntelligenceRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationIntelligenceUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private val now = 1_700_000_000_000L
    private lateinit var mailRepo: FakeMailRepository
    private lateinit var intelligenceRepo: FakeIntelligenceRepository
    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var useCase: ConversationIntelligenceUseCase

    private val testAccount = AccountRecord(
        accountId = "acct-1",
        emailAddress = "alex@example.com",
        displayName = "Alex Rivera",
        createdAtEpochMs = now,
        connectionState = ConnectionState.CONNECTED,
    )

    @Before
    fun setUp() {
        mailRepo = FakeMailRepository()
        intelligenceRepo = FakeIntelligenceRepository()
        accountRepo = FakeAccountRepository().apply { seed(testAccount) }
        useCase = ConversationIntelligenceUseCase(
            mail = mailRepo,
            intelligence = intelligenceRepo,
            accounts = accountRepo,
            dispatchers = dispatchers,
            clock = { now },
        )
    }

    @Test
    fun analyzeThread_emptyThread_returnsNull() = runTest(testDispatcher) {
        val result = useCase.analyzeThread("non-existent-thread", nowEpochMs = now)
        assertNull(result)
    }

    @Test
    fun analyzeThread_withAwaitingReplyMessage_returnsAwaitingUserReply() = runTest(testDispatcher) {
        val threadId = "thread-1"
        val msg = MessageRecord(
            messageId = "m-1",
            gmailMessageId = "gm-1",
            threadId = threadId,
            accountId = "acct-1",
            fromAddress = "sarah@partner.org",
            fromName = "Sarah Chen",
            subject = "Project roadmap review",
            snippet = "Please let me know if Monday works.",
            bodyText = "Hi Alex, please let me know if Monday 2pm works for our kickoff call.",
            timestampEpochMs = now - 3600_000L,
            unread = true,
        )

        mailRepo.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = "gt-1",
                accountId = "acct-1",
                subject = "Project roadmap review",
                latestMessageId = "m-1",
                latestMessageEpochMs = msg.timestampEpochMs,
                updatedAtEpochMs = now,
            ),
            listOf(msg),
        )

        val result = useCase.analyzeThread(threadId, nowEpochMs = now)
        assertNotNull(result)
        assertEquals(ConversationState.AWAITING_USER_REPLY, result?.state)
        assertEquals("m-1", result?.lastMessageId)
    }

    @Test
    fun observeThreadConversation_emitsReactiveResults() = runTest(testDispatcher) {
        val threadId = "thread-2"
        val msg1 = MessageRecord(
            messageId = "m-1",
            gmailMessageId = "gm-1",
            threadId = threadId,
            accountId = "acct-1",
            fromAddress = "sarah@partner.org",
            fromName = "Sarah Chen",
            subject = "Status",
            snippet = "Could you please send the notes?",
            bodyText = "Could you please send the meeting notes?",
            timestampEpochMs = now - 7200_000L,
        )

        mailRepo.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = "gt-2",
                accountId = "acct-1",
                subject = "Status",
                latestMessageId = "m-1",
                latestMessageEpochMs = msg1.timestampEpochMs,
                updatedAtEpochMs = now,
            ),
            listOf(msg1),
        )

        useCase.observeThreadConversation(threadId, testAccount.emailAddress, testAccount.accountId).test {
            val initial = awaitItem()
            assertEquals(ConversationState.AWAITING_USER_REPLY, initial.state)

            // User replies
            val msg2 = MessageRecord(
                messageId = "m-2",
                gmailMessageId = "gm-2",
                threadId = threadId,
                accountId = "acct-1",
                fromAddress = testAccount.emailAddress,
                fromName = testAccount.displayName,
                subject = "Re: Status",
                snippet = "Notes are attached.",
                bodyText = "Notes are attached, thank you.",
                timestampEpochMs = now - 1000L,
            )

            mailRepo.saveThreadWithMessages(
                ThreadRecord(
                    threadId = threadId,
                    gmailThreadId = "gt-2",
                    accountId = "acct-1",
                    subject = "Status",
                    latestMessageId = "m-2",
                    latestMessageEpochMs = msg2.timestampEpochMs,
                    updatedAtEpochMs = now,
                ),
                listOf(msg1, msg2),
            )

            val updated = awaitItem()
            // After user reply, it should no longer be AWAITING_USER_REPLY
            assert(updated.state != ConversationState.AWAITING_USER_REPLY)
            assertEquals("m-2", updated.lastMessageId)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
