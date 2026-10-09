package com.greninjaop.mailorganizer.ui.companies

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.CompanyRecord
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

/** Companies destination tests (Phase 11). */
class CompaniesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun viewModel(): CompaniesViewModel = CompaniesViewModel(
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

    private suspend fun seedCompany(companyId: String, name: String) {
        intelligence.upsertCompany(
            CompanyRecord(
                companyId = companyId,
                accountId = "a1",
                canonicalName = name,
                normalizedDomain = "$name.example.com",
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        )
    }

    private suspend fun saveMessage(id: String, companyId: String?) {
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
                    companyId = companyId,
                ),
            ),
        )
        intelligence.setClassification(
            ClassificationRecord(
                messageId = id,
                accountId = "a1",
                category = MailCategory.PROMOTIONS,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = 2,
                explanation = "test",
                classifiedAtEpochMs = 1_000L,
            ),
        )
    }

    /** Collects the first non-Loading content. */
    private suspend fun CompaniesViewModel.awaitContent(): CompaniesContent {
        var content: CompaniesContent = CompaniesContent.Loading
        state.test {
            while (content is CompaniesContent.Loading) {
                content = awaitItem().content
            }
            cancelAndIgnoreRemainingEvents()
        }
        return content
    }

    /** Collects the first non-Loading detail content. */
    private suspend fun CompaniesViewModel.awaitDetail(): CompanyDetailContent {
        var content: CompanyDetailContent = CompanyDetailContent.Loading
        detail.test {
            while (content is CompanyDetailContent.Loading) {
                content = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }
        return content
    }

    @Test
    fun `company list shows real message counts`() = runTest(testDispatcher) {
        seedAccount()
        seedCompany("co:a.com", "A")
        seedCompany("co:b.com", "B")
        saveMessage("m1", "co:a.com")
        saveMessage("m2", "co:a.com")
        saveMessage("m3", null)
        val vm = viewModel()
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("got $content", content is CompaniesContent.Ready)
        val ready = content as CompaniesContent.Ready
        // Only companies with messages appear; mail without a company
        // has no row (unknown senders are never hidden — they just
        // have no company row).
        assertEquals(1, ready.companies.size)
        assertEquals("co:a.com", ready.companies[0].record.companyId)
        assertEquals(2, ready.companies[0].messageCount)
    }

    @Test
    fun `detail shows company messages with categories`() = runTest(testDispatcher) {
        seedAccount()
        seedCompany("co:a.com", "A")
        saveMessage("m1", "co:a.com")
        saveMessage("m2", "co:a.com")
        val vm = viewModel()
        vm.selectCompany("co:a.com")
        advanceUntilIdle()
        val content = vm.awaitDetail()
        assertTrue("got $content", content is CompanyDetailContent.Loaded)
        val loaded = content as CompanyDetailContent.Loaded
        assertEquals(2, loaded.messages.size)
        assertEquals(2, loaded.categoryCounts[MailCategory.PROMOTIONS])
    }

    @Test
    fun `unknown company id yields NotFound`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        vm.selectCompany("co:nope.com")
        advanceUntilIdle()
        val content = vm.awaitDetail()
        assertTrue("got $content", content is CompanyDetailContent.NotFound)
    }
}
