package com.greninjaop.mailorganizer.ui.actions

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import com.greninjaop.mailorganizer.ui.mail.FakeIntelligenceRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.Dispatchers
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

/** Actions destination tests (Phase 11) — view-only, no execution. */
class ActionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun viewModel(): ActionsViewModel = ActionsViewModel(
        accounts = accounts,
        mail = mail,
        intelligence = intelligence,
        dispatchers = dispatchers(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accounts = FakeAccountRepository()
        intelligence = FakeIntelligenceRepository()
        mail = FakeMailRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private suspend fun seedAccount(accountId: String = "a1") {
        accounts.upsert(
            AccountRecord(
                accountId = accountId,
                emailAddress = "$accountId@example.com",
                displayName = "Test",
                connectionState = ConnectionState.DISCONNECTED,
                createdAtEpochMs = 1L,
            ),
        )
    }

    private suspend fun saveMessage(
        id: String,
        category: MailCategory,
        explanation: String? = "needs reply",
    ) {
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = "t-$id",
                gmailThreadId = null,
                accountId = "a1",
                subject = "s",
                messageCount = 1,
                latestMessageId = id,
                latestMessageEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
            listOf(
                MessageRecord(
                    messageId = id,
                    gmailMessageId = "g-$id",
                    threadId = "t-$id",
                    accountId = "a1",
                    fromAddress = "sender@example.com",
                    fromName = null,
                    subject = "subject $id",
                    snippet = null,
                    bodyText = "body",
                    timestampEpochMs = 1_000L,
                ),
            ),
        )
        intelligence.setClassification(
            ClassificationRecord(
                messageId = id,
                accountId = "a1",
                category = category,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = 2,
                explanation = explanation,
                classifiedAtEpochMs = 1_000L,
            ),
        )
    }

    /** Collects the first non-Loading content. */
    private suspend fun ActionsViewModel.awaitContent(): ActionsContent {
        var content: ActionsContent = ActionsContent.Loading
        state.test {
            while (content is ActionsContent.Loading) {
                content = awaitItem().content
            }
            cancelAndIgnoreRemainingEvents()
        }
        return content
    }

    @Test
    fun `actions list shows only action-required mail`() = runTest(testDispatcher) {
        seedAccount()
        saveMessage("m1", MailCategory.ACTION_REQUIRED)
        saveMessage("m2", MailCategory.NEWSLETTERS)
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is ActionsContent.Loaded)
        val loaded = content as ActionsContent.Loaded
        assertEquals(1, loaded.items.size)
        assertEquals("m1", loaded.items[0].messageId)
        assertEquals("needs reply", loaded.items[0].explanation)
    }

    @Test
    fun `no action-required mail yields honest empty state`() = runTest(testDispatcher) {
        seedAccount()
        saveMessage("m1", MailCategory.NEWSLETTERS)
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is ActionsContent.Empty)
    }
}
