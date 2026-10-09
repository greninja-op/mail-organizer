package com.greninjaop.mailorganizer.ui.home

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.CompanyRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import com.greninjaop.mailorganizer.ui.mail.FakeConnectivityObserver
import com.greninjaop.mailorganizer.ui.mail.FakeIntelligenceRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import com.greninjaop.mailorganizer.ui.mail.FakeSampleDataPolicy
import com.greninjaop.mailorganizer.ui.mail.SampleMailboxSeeder
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

/**
 * Home dashboard tests (Phase 11).
 *
 * Verifies the information hierarchy assembles from real local data:
 * attention → high priority → recent → categories → companies. Nothing
 * is fabricated — empty data yields honest empty states.
 */
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun viewModel(): HomeViewModel = HomeViewModel(
        accounts = accounts,
        mail = mail,
        intelligence = intelligence,
        connectivity = FakeConnectivityObserver(true),
        dispatchers = dispatchers(),
        samplePolicy = FakeSampleDataPolicy(false),
        seeder = SampleMailboxSeeder(accounts, mail, clock = { 1_800_000_000_000L }),
        clock = { 1_800_000_000_000L },
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
        accountId: String,
        subject: String,
        category: MailCategory? = null,
        priority: Priority? = null,
        companyId: String? = null,
    ) {
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = "t-$id",
                gmailThreadId = null,
                accountId = accountId,
                subject = subject,
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
                    fromName = "Sender",
                    subject = subject,
                    snippet = null,
                    bodyText = "body",
                    timestampEpochMs = 1_000L,
                    companyId = companyId,
                ),
            ),
        )
        if (category != null) {
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
        if (priority != null) {
            intelligence.setPriority(
                PriorityRecord(
                    messageId = id,
                    accountId = accountId,
                    priority = priority,
                    reason = "test",
                    version = 1,
                    updatedAtEpochMs = 1_000L,
                ),
            )
        }
    }


    /** Collects the first non-Loading content. */
    private suspend fun HomeViewModel.awaitContent(): HomeContent {
        var content: HomeContent = HomeContent.Loading
        state.test {
            while (content is HomeContent.Loading) {
                content = awaitItem().content
            }
            cancelAndIgnoreRemainingEvents()
        }
        return content
    }

    private suspend fun HomeViewModel.awaitLoaded(): HomeContent.Loaded {
        var last: HomeContent = HomeContent.Loading
        state.test {
            while (true) {
                val s = awaitItem()
                last = s.content
                if (last !is HomeContent.Loading) break
            }
        }
        assertTrue("expected Loaded, got $last", last is HomeContent.Loaded)
        return last as HomeContent.Loaded
    }

    @Test
    fun `no account yields NoAccount`() = runTest(testDispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is HomeContent.NoAccount)
    }

    @Test
    fun `account with no mail yields Empty`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is HomeContent.Empty)
    }

    @Test
    fun `attention section shows action-required mail`() = runTest(testDispatcher) {
        seedAccount()
        saveMessage("m1", "a1", "Pay invoice", category = MailCategory.ACTION_REQUIRED)
        saveMessage("m2", "a1", "Hello", category = MailCategory.NEWSLETTERS)
        val vm = viewModel()
        advanceUntilIdle()
        val loaded = vm.awaitLoaded()
        assertEquals(1, loaded.attention.size)
        assertEquals("m1", loaded.attention[0].messageId)
    }

    @Test
    fun `high priority section shows high and critical`() = runTest(testDispatcher) {
        seedAccount()
        saveMessage("m1", "a1", "Urgent", priority = Priority.CRITICAL)
        saveMessage("m2", "a1", "Important", priority = Priority.HIGH)
        saveMessage("m3", "a1", "Normal", priority = Priority.NORMAL)
        val vm = viewModel()
        advanceUntilIdle()
        val loaded = vm.awaitLoaded()
        assertEquals(2, loaded.highPriority.size)
        assertTrue(loaded.highPriority.none { it.messageId == "m3" })
    }

    @Test
    fun `category counts come from real data`() = runTest(testDispatcher) {
        seedAccount()
        saveMessage("m1", "a1", "Job", category = MailCategory.CAREER)
        saveMessage("m2", "a1", "Job 2", category = MailCategory.CAREER)
        saveMessage("m3", "a1", "News", category = MailCategory.NEWSLETTERS)
        val vm = viewModel()
        advanceUntilIdle()
        val loaded = vm.awaitLoaded()
        assertEquals(2, loaded.categoryCounts[MailCategory.CAREER])
        assertEquals(1, loaded.categoryCounts[MailCategory.NEWSLETTERS])
        assertEquals(null, loaded.categoryCounts[MailCategory.SECURITY])
    }

    @Test
    fun `companies section shows real counts`() = runTest(testDispatcher) {
        seedAccount()
        intelligence.upsertCompany(
            CompanyRecord(
                companyId = "co:example.com",
                accountId = "a1",
                canonicalName = "Example",
                normalizedDomain = "example.com",
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        )
        saveMessage("m1", "a1", "Hi", companyId = "co:example.com")
        val vm = viewModel()
        advanceUntilIdle()
        val loaded = vm.awaitLoaded()
        assertEquals(1, loaded.companies.size)
        assertEquals(1, loaded.companies[0].messageCount)
    }

    @Test
    fun `failure degrades to Error not fake data`() = runTest(testDispatcher) {
        seedAccount()
        mail.failWith = RuntimeException("boom")
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is HomeContent.Error)
    }
}
