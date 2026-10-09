package com.greninjaop.mailorganizer.ui.categories

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

/** Categories destination tests (Phase 11). */
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun viewModel(): CategoriesViewModel = CategoriesViewModel(
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

    private suspend fun saveClassified(
        id: String,
        accountId: String,
        category: MailCategory,
    ) {
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = "t-$id",
                gmailThreadId = null,
                accountId = accountId,
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
                    accountId = accountId,
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
                accountId = accountId,
                category = category,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = 2,
                explanation = "test",
                classifiedAtEpochMs = 1_000L,
            ),
        )
    }

    /** Collects the first non-Loading content. */
    private suspend fun CategoriesViewModel.awaitContent(): CategoriesContent {
        var content: CategoriesContent = CategoriesContent.Loading
        state.test {
            while (content is CategoriesContent.Loading) {
                content = awaitItem().content
            }
            cancelAndIgnoreRemainingEvents()
        }
        return content
    }

    @Test
    fun `category list shows real counts`() = runTest(testDispatcher) {
        seedAccount()
        saveClassified("m1", "a1", MailCategory.CAREER)
        saveClassified("m2", "a1", MailCategory.CAREER)
        saveClassified("m3", "a1", MailCategory.SECURITY)
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is CategoriesContent.Ready)
        val ready = content as CategoriesContent.Ready
        assertEquals(2, ready.counts[MailCategory.CAREER])
        assertEquals(1, ready.counts[MailCategory.SECURITY])
    }

    @Test
    fun `empty categories yield Empty not fake rows`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is CategoriesContent.Empty)
    }

    @Test
    fun `detail shows messages for the selected category`() = runTest(testDispatcher) {
        seedAccount()
        saveClassified("m1", "a1", MailCategory.CAREER)
        saveClassified("m2", "a1", MailCategory.NEWSLETTERS)
        val vm = viewModel()
        vm.selectCategory(MailCategory.CAREER)
        advanceUntilIdle()
        vm.detail.test {
            var content: CategoryDetailContent = awaitItem()
            while (content is CategoryDetailContent.Loading) {
                content = awaitItem()
            }
            assertTrue("got $content", content is CategoryDetailContent.Loaded)
            val loaded = content as CategoryDetailContent.Loaded
            assertEquals(1, loaded.messages.size)
            assertEquals("m1", loaded.messages[0].messageId)
        }
    }
}
