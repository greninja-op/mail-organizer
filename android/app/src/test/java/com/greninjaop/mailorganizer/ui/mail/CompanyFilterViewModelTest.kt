package com.greninjaop.mailorganizer.ui.mail

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.classify.DeterministicClassifier
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.sync.DeferredGmailSyncApi
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel tests for the Phase 8 company filter (Phase 8).
 *
 * Verifies: the filter row appears with honest global counts, selecting a
 * company narrows the destination to that company's mail, pinning reorders
 * the list, and the filter is hidden on non-grouping destinations.
 */
class CompanyFilterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository
    private lateinit var connectivity: FakeConnectivityObserver

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accounts = FakeAccountRepository()
        intelligence = FakeIntelligenceRepository()
        mail = FakeMailRepository()
        connectivity = FakeConnectivityObserver(true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): MailViewModel {
        val classifyMessage = ClassifyMessageUseCase(
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            clock = { 1_800_000_000_000L },
        )
        return MailViewModel(
            accounts = accounts,
            mail = mail,
            intelligence = intelligence,
            classifyMailbox = ClassifyMailboxUseCase(
                mail = mail,
                classifyMessage = classifyMessage,
                dispatchers = dispatchers(),
            ),
            companyIntelligence = CompanyIntelligenceUseCase(
                mail = mail,
                intelligence = intelligence,
                dispatchers = dispatchers(),
                clock = { 1_800_000_000_000L },
            ),
            syncCoordinator = SyncCoordinator(
                api = DeferredGmailSyncApi(),
                mail = mail,
                syncState = FakeSyncStateRepository(),
                dispatchers = dispatchers(),
            ),
            connectivity = connectivity,
            dispatchers = dispatchers(),
            samplePolicy = FakeSampleDataPolicy(false),
            seeder = SampleMailboxSeeder(accounts, mail, clock = { 1_800_000_000_000L }),
            clock = { 1_800_000_000_000L },
        )
    }

    private suspend fun seedAccount() {
        accounts.upsert(
            AccountRecord(
                accountId = "real-acct",
                emailAddress = "me@example.com",
                displayName = "Me",
                connectionState = ConnectionState.DISCONNECTED,
                createdAtEpochMs = 1L,
            ),
        )
    }

    private suspend fun savePromoMessage(id: String, from: String, subject: String) {
        val threadId = "t-$id"
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = null,
                accountId = "real-acct",
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
                    threadId = threadId,
                    accountId = "real-acct",
                    fromAddress = from,
                    fromName = null,
                    subject = subject,
                    snippet = null,
                    bodyText = "body",
                    timestampEpochMs = 1_000L,
                    labels = listOf("INBOX"),
                ),
            ),
        )
        intelligence.seedClassification(
            ClassificationRecord(
                messageId = id,
                accountId = "real-acct",
                category = MailCategory.PROMOTIONS,
                confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC,
                version = DeterministicClassifier.VERSION,
                explanation = null,
                classifiedAtEpochMs = 1_800_000_000_000L,
            ),
        )
    }

    /** Awaits the first filter state with entries (or returns the latest). */
    private suspend fun MailViewModel.awaitFilter(): CompanyFilterUiState {
        var last = CompanyFilterUiState()
        state.test {
            while (true) {
                val s = awaitItem()
                last = s.companyFilter
                if (last.visible && last.entries.isNotEmpty()) break
            }
            cancelAndIgnoreRemainingEvents()
        }
        return last
    }

    private suspend fun MailViewModel.awaitContent(): MailboxContent {
        var last: MailboxContent = MailboxContent.Loading
        state.test {
            while (true) {
                val s = awaitItem()
                last = s.content
                if (last !is MailboxContent.Loading) break
            }
            cancelAndIgnoreRemainingEvents()
        }
        return last
    }

    @Test
    fun `promotional shows company filter with honest counts`() =
        runTest(testDispatcher) {
            seedAccount()
            savePromoMessage("a1", "deals@amazon.com", "Clearance sale")
            savePromoMessage("a2", "offers@mail.amazon.com", "Deal of the day")
            savePromoMessage("f1", "sale@flipkart.com", "Big billion sale")
            mail.categoryByMessageId = mapOf(
                "a1" to MailCategory.PROMOTIONS,
                "a2" to MailCategory.PROMOTIONS,
                "f1" to MailCategory.PROMOTIONS,
            )
            val vm = viewModel()
            advanceUntilIdle()
            vm.setDestination(MailboxDestination.PROMOTIONAL)
            advanceUntilIdle()

            val filter = vm.awaitFilter()
            assertTrue(filter.visible)
            assertEquals(2, filter.entries.size)
            // Pinned-first, then by count: Amazon (2) before Flipkart (1).
            assertEquals("co:amazon.com", filter.entries[0].companyId)
            assertEquals("Amazon", filter.entries[0].displayName)
            assertEquals(2, filter.entries[0].messageCount)
            assertEquals("co:flipkart.com", filter.entries[1].companyId)
            assertEquals(1, filter.entries[1].messageCount)
            assertNull(filter.selectedCompanyId)
        }

    @Test
    fun `selecting a company narrows the list to its mail`() =
        runTest(testDispatcher) {
            seedAccount()
            savePromoMessage("a1", "deals@amazon.com", "Clearance sale")
            savePromoMessage("f1", "sale@flipkart.com", "Big billion sale")
            mail.categoryByMessageId = mapOf(
                "a1" to MailCategory.PROMOTIONS,
                "f1" to MailCategory.PROMOTIONS,
            )
            val vm = viewModel()
            advanceUntilIdle()
            vm.setDestination(MailboxDestination.PROMOTIONAL)
            advanceUntilIdle()
            vm.awaitFilter()

            vm.selectCompany("co:amazon.com")
            advanceUntilIdle()
            val content = vm.awaitContent()
            assertTrue("expected Messages, got $content", content is MailboxContent.Messages)
            val items = (content as MailboxContent.Messages).items
            assertEquals(listOf("a1"), items.map { it.messageId })

            // Clearing the selection restores the full list.
            vm.selectCompany(null)
            advanceUntilIdle()
            val cleared = vm.awaitContent()
            assertTrue(cleared is MailboxContent.Messages)
            assertEquals(2, (cleared as MailboxContent.Messages).items.size)
        }

    @Test
    fun `pinning moves the company to the top of the filter list`() =
        runTest(testDispatcher) {
            seedAccount()
            savePromoMessage("a1", "deals@amazon.com", "Clearance sale")
            savePromoMessage("a2", "offers@amazon.com", "Deal of the day")
            savePromoMessage("f1", "sale@flipkart.com", "Big billion sale")
            mail.categoryByMessageId = mapOf(
                "a1" to MailCategory.PROMOTIONS,
                "a2" to MailCategory.PROMOTIONS,
                "f1" to MailCategory.PROMOTIONS,
            )
            val vm = viewModel()
            advanceUntilIdle()
            vm.setDestination(MailboxDestination.PROMOTIONAL)
            advanceUntilIdle()
            vm.awaitFilter()

            vm.toggleCompanyPin("co:flipkart.com")
            advanceUntilIdle()
            val filter = vm.awaitFilter()
            // Pinned Flipkart now leads despite the lower count.
            assertEquals("co:flipkart.com", filter.entries[0].companyId)
            assertTrue(filter.entries[0].pinned)
            assertEquals("co:amazon.com", filter.entries[1].companyId)
            assertFalse(filter.entries[1].pinned)
        }

    @Test
    fun `filter is hidden on thread destinations and cleared on switch`() =
        runTest(testDispatcher) {
            seedAccount()
            savePromoMessage("a1", "deals@amazon.com", "Clearance sale")
            mail.categoryByMessageId = mapOf("a1" to MailCategory.PROMOTIONS)
            val vm = viewModel()
            advanceUntilIdle()

            // All Inbox: no company filter.
            vm.state.test {
                val s = awaitItem()
                assertFalse(s.companyFilter.visible)
                cancelAndIgnoreRemainingEvents()
            }

            // Select a company in Promotional, then switch away: cleared.
            vm.setDestination(MailboxDestination.PROMOTIONAL)
            advanceUntilIdle()
            vm.awaitFilter()
            vm.selectCompany("co:amazon.com")
            advanceUntilIdle()
            vm.setDestination(MailboxDestination.ALL_INBOX)
            advanceUntilIdle()
            vm.state.test {
                var s = awaitItem()
                // Destination and filter settle via different flows; await both.
                while (s.destination != MailboxDestination.ALL_INBOX ||
                    s.companyFilter.visible
                ) {
                    s = awaitItem()
                }
                assertFalse(s.companyFilter.visible)
                assertNull(s.companyFilter.selectedCompanyId)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
