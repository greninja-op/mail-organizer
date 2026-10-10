package com.greninjaop.mailorganizer.ui.mail

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
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

class ThreadViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun message(
        id: String,
        threadId: String,
        subject: String,
        timestamp: Long,
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = null,
        threadId = threadId,
        accountId = "acct-1",
        fromAddress = "a@example.com",
        fromName = "A",
        subject = subject,
        snippet = null,
        bodyText = "body $id",
        timestampEpochMs = timestamp,
        unread = false,
    )

    private suspend fun seedThread(threadId: String, vararg stamps: Long) {
        val messages = stamps.mapIndexed { i, ts ->
            message("m$i", threadId, "Subject", ts)
        }
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = null,
                accountId = "acct-1",
                subject = "Subject",
                messageCount = messages.size,
                latestMessageId = messages.maxByOrNull { it.timestampEpochMs }?.messageId,
                latestMessageEpochMs = messages.maxOf { it.timestampEpochMs },
                updatedAtEpochMs = 0L,
            ),
            messages,
        )
    }

    private suspend fun ReceiveTurbine<ThreadDetailState>.awaitContent(): ThreadDetailState.Content {
        while (true) {
            when (val s = awaitItem()) {
                is ThreadDetailState.Content -> return s
                else -> Unit
            }
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mail = FakeMailRepository()
        intelligence = FakeIntelligenceRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `messages are oldest first and newest starts expanded`() = runTest(testDispatcher) {
        // Inserted newest-first to prove ordering is by timestamp, not insert order.
        seedThread("t1", 3000L, 1000L, 2000L)
        val vm = ThreadViewModel("t1", mail, intelligence, dispatchers())
        advanceUntilIdle()

        vm.state.test {
            val content = awaitContent()
            assertEquals(listOf(1000L, 2000L, 3000L), content.messages.map { it.timestampEpochMs })
            assertEquals(setOf("m0"), content.expandedIds) // m0 has ts 3000 (newest)
        }
    }

    @Test
    fun `toggle expands and collapses`() = runTest(testDispatcher) {
        seedThread("t1", 1000L, 2000L) // m0 oldest, m1 newest (expanded by default)
        val vm = ThreadViewModel("t1", mail, intelligence, dispatchers())
        advanceUntilIdle()

        // Establish the default: newest (m1) expanded.
        vm.state.test {
            assertEquals(setOf("m1"), awaitContent().expandedIds)
        }

        vm.toggleExpanded("m1") // collapse the default-expanded newest
        advanceUntilIdle()
        vm.state.test {
            assertEquals(emptySet<String>(), awaitContent().expandedIds)
        }

        vm.toggleExpanded("m0") // expand the oldest
        advanceUntilIdle()
        vm.state.test {
            assertEquals(setOf("m0"), awaitContent().expandedIds)
        }
    }

    @Test
    fun `unknown thread is empty, not an error`() = runTest(testDispatcher) {
        val vm = ThreadViewModel("nope", mail, intelligence, dispatchers())
        advanceUntilIdle()
        vm.state.test {
            while (true) {
                if (awaitItem() is ThreadDetailState.Empty) break
            }
        }
    }

    @Test
    fun `ensureExpanded opens a deep-linked message`() = runTest(testDispatcher) {
        seedThread("t1", 1000L, 2000L)
        val vm = ThreadViewModel("t1", mail, intelligence, dispatchers())
        advanceUntilIdle()
        vm.ensureExpanded("m1")
        advanceUntilIdle()
        vm.state.test {
            val content = awaitContent()
            assertTrue(content.expandedIds.contains("m1"))
        }
    }

    @Test
    fun `message classifications are loaded with the thread`() = runTest(testDispatcher) {
        seedThread("t1", 1000L, 2000L)
        intelligence.seedClassification(
            com.greninjaop.mailorganizer.data.local.ClassificationRecord(
                messageId = "m0",
                accountId = "acct-1",
                category = com.greninjaop.mailorganizer.data.local.MailCategory.SECURITY,
                confidence = 0.9f,
                source = com.greninjaop.mailorganizer.data.local.ClassificationSource.DETERMINISTIC,
                version = 1,
                explanation = "Classified as Security [SECURITY_OTP].",
                classifiedAtEpochMs = 1L,
            ),
        )
        val vm = ThreadViewModel("t1", mail, intelligence, dispatchers())
        advanceUntilIdle()
        vm.state.test {
            val content = awaitContent()
            assertEquals(
                com.greninjaop.mailorganizer.data.local.MailCategory.SECURITY,
                content.classifications["m0"]?.category,
            )
            // Unclassified messages map to null, not a crash.
            assertTrue(content.classifications.containsKey("m1"))
        }
    }

    @Test
    fun `subject comes from the thread messages`() = runTest(testDispatcher) {
        seedThread("t1", 1000L)
        val vm = ThreadViewModel("t1", mail, intelligence, dispatchers())
        advanceUntilIdle()
        vm.subject.test {
            // Initial "" then the real subject once messages arrive.
            var last = awaitItem()
            while (last.isEmpty()) last = awaitItem()
            assertEquals("Subject", last)
        }
    }

    @Test
    fun `conversation intelligence is attached to thread state when present`() = runTest(testDispatcher) {
        val account = com.greninjaop.mailorganizer.data.local.AccountRecord(
            accountId = "acct-1",
            emailAddress = "alex@example.com",
            displayName = "Alex",
            createdAtEpochMs = 1000L,
        )
        val accountRepo = FakeAccountRepository().apply { seed(account) }
        val convUseCase = com.greninjaop.mailorganizer.domain.conversation.ConversationIntelligenceUseCase(
            mail = mail,
            intelligence = intelligence,
            accounts = accountRepo,
            dispatchers = dispatchers(),
        )
        seedThread("t1", 1000L)
        val vm = ThreadViewModel(
            threadId = "t1",
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            conversationIntelligence = convUseCase,
        )
        advanceUntilIdle()
        vm.state.test {
            val content = awaitContent()
            org.junit.Assert.assertNotNull(content.conversation)
            assertEquals("acct-1", content.conversation?.accountId)
            assertEquals("t1", content.conversation?.threadId)
        }
    }

    @Test
    fun `requestThreadSummary transitions to Unavailable when aiFallback is absent`() = runTest(testDispatcher) {
        seedThread("t1", 1000L)
        val vm = ThreadViewModel(
            threadId = "t1",
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            aiFallback = null,
        )
        advanceUntilIdle()

        vm.requestThreadSummary()
        assertEquals(ThreadSummaryUiState.Unavailable, vm.threadSummary.value)
    }

    @Test
    fun `requestThreadSummary transitions to Content on successful AI response`() = runTest(testDispatcher) {
        seedThread("t1", 1000L)
        val prefs = com.greninjaop.mailorganizer.data.prefs.FakeAiPreferences(
            initialEnabled = true,
            initialProviderId = "fake_test",
        )
        val fakeProvider = com.greninjaop.mailorganizer.data.ai.FakeTestAiProvider()
        val registry = com.greninjaop.mailorganizer.data.ai.AiProviderRegistry().apply {
            register(fakeProvider)
        }
        val aiManager = com.greninjaop.mailorganizer.domain.ai.AiManager(
            preferences = prefs,
            registry = registry,
            dispatchers = dispatchers(),
        )
        val aiFallbackUseCase = com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase(
            aiManager = aiManager,
            preferences = prefs,
            mailRepository = mail,
            dispatchers = dispatchers(),
        )

        val vm = ThreadViewModel(
            threadId = "t1",
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            aiFallback = aiFallbackUseCase,
        )
        advanceUntilIdle()

        assertEquals(ThreadSummaryUiState.Idle, vm.threadSummary.value)
        vm.requestThreadSummary()
        advanceUntilIdle()

        val summary = vm.threadSummary.value
        assertTrue("Expected Content state but was $summary", summary is ThreadSummaryUiState.Content)
        val content = summary as ThreadSummaryUiState.Content
        assertTrue(content.summary.isNotBlank())
        assertTrue(content.keyPoints.isNotEmpty())
    }

    @Test
    fun `requestThreadSummary transitions to Error on provider failure`() = runTest(testDispatcher) {
        seedThread("t1", 1000L)
        val prefs = com.greninjaop.mailorganizer.data.prefs.FakeAiPreferences(
            initialEnabled = true,
            initialProviderId = "fake_test",
        )
        val fakeProvider = com.greninjaop.mailorganizer.data.ai.FakeTestAiProvider(
            shouldFail = true,
        )
        val registry = com.greninjaop.mailorganizer.data.ai.AiProviderRegistry().apply {
            register(fakeProvider)
        }
        val aiManager = com.greninjaop.mailorganizer.domain.ai.AiManager(
            preferences = prefs,
            registry = registry,
            dispatchers = dispatchers(),
        )
        val aiFallbackUseCase = com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase(
            aiManager = aiManager,
            preferences = prefs,
            mailRepository = mail,
            dispatchers = dispatchers(),
        )

        val vm = ThreadViewModel(
            threadId = "t1",
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            aiFallback = aiFallbackUseCase,
        )
        advanceUntilIdle()

        vm.requestThreadSummary()
        advanceUntilIdle()

        val summary = vm.threadSummary.value
        assertTrue("Expected Error state but was $summary", summary is ThreadSummaryUiState.Error)
    }
}
