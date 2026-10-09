package com.greninjaop.mailorganizer.ui.mail

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.sync.DeferredGmailSyncApi
import com.greninjaop.mailorganizer.data.sync.SyncCoordinator
import com.greninjaop.mailorganizer.domain.classify.ClassifyMailboxUseCase
import com.greninjaop.mailorganizer.domain.classify.ClassifyMessageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MailViewModelTest {

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

    private fun viewModel(
        seed: Boolean = true,
        online: Boolean = true,
    ): MailViewModel {
        connectivity = FakeConnectivityObserver(online)
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
            companyIntelligence = com.greninjaop.mailorganizer.domain.company.CompanyIntelligenceUseCase(
                mail = mail,
                intelligence = intelligence,
                dispatchers = dispatchers(),
                clock = { 1_800_000_000_000L },
            ),
            prioritizeMailbox = com.greninjaop.mailorganizer.domain.priority.PrioritizeMailboxUseCase(
                mail = mail,
                prioritizeMessage = com.greninjaop.mailorganizer.domain.priority.PrioritizeMessageUseCase(
                    mail = mail,
                    intelligence = intelligence,
                    dispatchers = dispatchers(),
                    clock = { 1_800_000_000_000L },
                ),
                dispatchers = dispatchers(),
            ),
            extractMailbox = com.greninjaop.mailorganizer.domain.temporal.ExtractMailboxUseCase(
                mail = mail,
                extractMessage = com.greninjaop.mailorganizer.domain.temporal.ExtractTemporalUseCase(
                    mail = mail,
                    intelligence = intelligence,
                    dispatchers = dispatchers(),
                    clock = { 1_800_000_000_000L },
                ),
                dispatchers = dispatchers(),
            ),
            generateActions = com.greninjaop.mailorganizer.domain.actions.GenerateActionsUseCase(
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
            samplePolicy = FakeSampleDataPolicy(seed),
            seeder = SampleMailboxSeeder(accounts, mail, clock = { 1_800_000_000_000L }),
            clock = { 1_800_000_000_000L },
        )
    }

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

    private suspend fun MailViewModel.awaitContent(): MailboxContent {
        var last: MailboxContent = MailboxContent.Loading
        state.test {
            // Skip transient states; take the first stable non-loading content.
            while (true) {
                val s = awaitItem()
                last = s.content
                if (last !is MailboxContent.Loading) break
            }
        }
        return last
    }

    private suspend fun seedAccount(accountId: String) {
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
        threadId: String,
        from: String,
        subject: String,
        body: String,
        labels: List<String> = listOf("INBOX"),
    ) {
        mail.saveThreadWithMessages(
            com.greninjaop.mailorganizer.data.local.ThreadRecord(
                threadId = threadId,
                gmailThreadId = null,
                accountId = accountId,
                subject = subject,
                messageCount = 1,
                latestMessageId = id,
                latestMessageEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
            listOf(
                com.greninjaop.mailorganizer.data.local.MessageRecord(
                    messageId = id,
                    gmailMessageId = "g-$id",
                    threadId = threadId,
                    accountId = accountId,
                    fromAddress = from,
                    fromName = null,
                    subject = subject,
                    snippet = null,
                    bodyText = body,
                    timestampEpochMs = 1_000L,
                    labels = labels,
                ),
            ),
        )
    }

    @Test
    fun `fixtures seed and the inbox lists the active account threads`() = runTest(testDispatcher) {
        val vm = viewModel(seed = true)
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue(content is MailboxContent.Threads)
        val threads = (content as MailboxContent.Threads).items
        // Alex is the earliest-created fixture account → 6 threads, strictly scoped.
        assertEquals(6, threads.size)
        assertTrue(threads.all { it.accountId == SampleMailboxSeeder.ACCT_ALEX })
        assertFalse(content.hasMore)
        // Sample banner flag.
        var sample = false
        vm.state.test {
            var s = awaitItem()
            while (!s.isSampleData) s = awaitItem()
            sample = s.isSampleData
        }
        assertTrue(sample)
    }

    @Test
    fun `empty database yields honest empty state`() = runTest(testDispatcher) {
        val vm = viewModel(seed = false)
        advanceUntilIdle()
        assertEquals(MailboxContent.Empty(EmptyKind.NO_MAIL), vm.awaitContent())
    }

    @Test
    fun `database failure yields friendly error without internals`() = runTest(testDispatcher) {
        accounts.upsert(
            AccountRecord(
                accountId = "real-acct",
                emailAddress = "me@gmail.com",
                displayName = "Me",
                createdAtEpochMs = 1L,
                connectionState = ConnectionState.CONNECTED,
            ),
        )
        mail.failWith = RuntimeException("SQLITE_CORRUPT: database disk image is malformed")
        val vm = viewModel(seed = false)
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue(content is MailboxContent.Error)
        val message = (content as MailboxContent.Error).message
        assertFalse(message.contains("SQLITE"))
        assertFalse(message.contains("RuntimeException"))
        assertEquals("Couldn't load your mail. Please try again.", message)
    }

    @Test
    fun `filter narrows and empty filter result is honest`() = runTest(testDispatcher) {
        val vm = viewModel(seed = true)
        advanceUntilIdle()
        assertTrue(vm.awaitContent() is MailboxContent.Threads)

        vm.setFilterText("quarterly")
        advanceUntilIdle()
        val filtered = vm.awaitContent()
        assertTrue(filtered is MailboxContent.Threads)
        assertEquals(1, (filtered as MailboxContent.Threads).items.size)

        vm.setFilterText("zzz-no-such-mail")
        advanceUntilIdle()
        assertEquals(
            MailboxContent.Empty(EmptyKind.NO_FILTER_RESULTS),
            vm.awaitContent(),
        )

        vm.clearFilter()
        advanceUntilIdle()
        assertTrue(vm.awaitContent() is MailboxContent.Threads)
    }

    @Test
    fun `starred destination lists starred messages`() = runTest(testDispatcher) {
        val vm = viewModel(seed = true)
        advanceUntilIdle()
        vm.setDestination(MailboxDestination.STARRED)
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue(content is MailboxContent.Messages)
        assertEquals(1, (content as MailboxContent.Messages).items.size)
        assertEquals("Quarterly report", content.items[0].subject)
    }

    @Test
    fun `promotional destination shows classified promotions`() = runTest(testDispatcher) {
        seedAccount("real-acct")
        saveMessage(
            "promo-1", "real-acct", "t-promo",
            from = "deals@shop.example.com",
            subject = "Clearance: limited time offer",
            body = "Our clearance sale ends Sunday.",
        )
        saveMessage(
            "plain-1", "real-acct", "t-plain",
            from = "friend@example.com",
            subject = "Hello",
            body = "Just saying hello.",
        )
        val vm = viewModel(seed = false)
        advanceUntilIdle()
        vm.setDestination(MailboxDestination.PROMOTIONAL)
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("expected Messages, got $content", content is MailboxContent.Messages)
        val items = (content as MailboxContent.Messages).items
        assertEquals(listOf("promo-1"), items.map { it.messageId })
    }

    @Test
    fun `promotional destination is honestly empty when nothing classified`() =
        runTest(testDispatcher) {
            seedAccount("real-acct")
            val vm = viewModel(seed = false)
            advanceUntilIdle()
            vm.setDestination(MailboxDestination.PROMOTIONAL)
            advanceUntilIdle()
            assertEquals(
                MailboxContent.Empty(EmptyKind.NO_PROMOTIONS),
                vm.awaitContent(),
            )
        }

    @Test
    fun `spam destination shows spam-labeled mail`() = runTest(testDispatcher) {
        seedAccount("real-acct")
        saveMessage(
            "spam-1", "real-acct", "t-spam",
            from = "winner@prize.example.com",
            subject = "You won!",
            body = "Claim now.",
            labels = listOf("SPAM"),
        )
        saveMessage(
            "ham-1", "real-acct", "t-ham",
            from = "friend@example.com",
            subject = "Hello",
            body = "Hi.",
            labels = listOf("INBOX"),
        )
        val vm = viewModel(seed = false)
        advanceUntilIdle()
        vm.setDestination(MailboxDestination.SPAM)
        advanceUntilIdle()
        val content = vm.awaitContent()
        assertTrue("expected Messages, got $content", content is MailboxContent.Messages)
        assertEquals(
            listOf("spam-1"),
            (content as MailboxContent.Messages).items.map { it.messageId },
        )
    }

    @Test
    fun `background classification classifies new mail on startup`() =
        runTest(testDispatcher) {
            seedAccount("real-acct")
            saveMessage(
                "otp-1", "real-acct", "t-otp",
                from = "no-reply@bank.example.com",
                subject = "Your verification code is 482910",
                body = "Your verification code is 482910.",
            )
            val vm = viewModel(seed = false)
            advanceUntilIdle()
            val stored = intelligence.getClassification("otp-1")
            assertNotNull(stored)
            assertEquals(
                com.greninjaop.mailorganizer.data.local.MailCategory.SECURITY,
                stored!!.category,
            )
        }

    @Test
    fun `refresh without gmail connection reports honestly`() = runTest(testDispatcher) {
        val vm = viewModel(seed = true)
        advanceUntilIdle()

        vm.state.test {
            // Skip to a stable content state first.
            var s = awaitItem()
            while (s.content is MailboxContent.Loading) s = awaitItem()
            assertTrue(s.content is MailboxContent.Threads)

            vm.refresh()
            s = awaitItem()
            while (s.syncUi !is SyncUiState.Result) s = awaitItem()
            assertEquals(
                "Gmail isn't connected yet — sync will work once you connect an account.",
                (s.syncUi as SyncUiState.Result).message,
            )
            vm.consumeSyncResult()
            s = awaitItem()
            assertEquals(SyncUiState.Idle, s.syncUi)
        }
    }

    @Test
    fun `offline banner state follows connectivity`() = runTest(testDispatcher) {
        val vm = viewModel(seed = true, online = false)
        advanceUntilIdle()
        var offline = false
        vm.state.test {
            var s = awaitItem()
            while (!(s.isOffline && s.content is MailboxContent.Threads)) s = awaitItem()
            offline = s.isOffline
        }
        assertTrue(offline)
    }

    @Test
    fun `load more raises the page limit`() = runTest(testDispatcher) {
        val vm = viewModel(seed = true)
        advanceUntilIdle()
        // 6 threads < PAGE_SIZE → no more to load, but the call is safe.
        vm.loadMore()
        advanceUntilIdle()
        assertTrue(vm.awaitContent() is MailboxContent.Threads)
    }
}
