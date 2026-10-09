package com.greninjaop.mailorganizer.ui.actions

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionStatus
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ActionItemRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.domain.actions.ActionExecutorRegistry
import com.greninjaop.mailorganizer.domain.actions.ReviewActionUseCase
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

/**
 * Actions destination tests (Phase 14).
 *
 * The screen is now backed by the action engine's cards — not the raw
 * ACTION_REQUIRED classification view (Phase 11's view-only seam, now
 * filled). Covers: cards render ordered by urgency, dismiss intent,
 * confirm flows (internal → open thread; external with no executor →
 * confirmation dialog → honest not-connected message).
 */
class ActionsViewModelTest {

    companion object {
        private const val NOW = 1_800_000_000_000L
        private const val ACCOUNT = "a1"
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository
    private lateinit var review: ReviewActionUseCase

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun viewModel() = ActionsViewModel(
        accounts = accounts,
        mail = mail,
        intelligence = intelligence,
        review = review,
        dispatchers = dispatchers(),
        clock = { NOW },
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accounts = FakeAccountRepository()
        mail = FakeMailRepository()
        intelligence = FakeIntelligenceRepository()
        review = ReviewActionUseCase(
            intelligence = intelligence,
            executors = ActionExecutorRegistry(),
            dispatchers = dispatchers(),
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private suspend fun seedAccount() {
        accounts.upsert(
            AccountRecord(
                accountId = ACCOUNT,
                emailAddress = "user@example.com",
                displayName = "Test",
                connectionState = ConnectionState.DISCONNECTED,
                createdAtEpochMs = 1L,
            ),
        )
    }

    private suspend fun seedMessage(messageId: String, threadId: String = "t1") {
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = "g-$threadId",
                accountId = ACCOUNT,
                subject = "Interview confirmation",
                latestMessageEpochMs = NOW,
            ),
            listOf(
                MessageRecord(
                    messageId = messageId,
                    gmailMessageId = "g-$messageId",
                    threadId = threadId,
                    accountId = ACCOUNT,
                    fromAddress = "jobs@acme.com",
                    fromName = "Acme",
                    subject = "Interview confirmation",
                    snippet = null,
                    bodyText = "body",
                    timestampEpochMs = NOW,
                ),
            ),
        )
    }

    private fun cardRow(
        id: Long,
        messageId: String,
        urgency: ActionUrgency,
        externalEffect: ExternalEffect = ExternalEffect.NONE,
    ) = ActionItemRecord(
        id = id,
        messageId = messageId,
        accountId = ACCOUNT,
        threadId = "t1",
        actionType = ActionType.MEETING,
        title = "Interview",
        urgency = urgency,
        source = ActionSource.TEMPORAL_EXTRACTION,
        status = ActionStatus.SUGGESTED,
        confidence = 1.0f,
        explanation = "why",
        externalEffect = externalEffect,
        dueDateEpochMs = NOW + 86_400_000L,
        detectedAtEpochMs = NOW,
    )

    @Test
    fun `cards render ordered by urgency`() = runTest(testDispatcher) {
        seedAccount()
        seedMessage("m1")
        seedMessage("m2")
        intelligence.seedActionItems(
            listOf(
                cardRow(1L, "m1", ActionUrgency.LOW),
                cardRow(2L, "m2", ActionUrgency.CRITICAL),
            ),
        )

        val vm = viewModel()
        vm.state.test {
            var content: ActionsContent = ActionsContent.Loading
            while (content is ActionsContent.Loading) {
                content = awaitItem().content
            }
            val loaded = content as ActionsContent.Loaded
            assertEquals(2, loaded.cards.size)
            // CRITICAL first.
            assertEquals(2L, loaded.cards[0].id)
            assertEquals("Acme", loaded.cards[0].senderName)
            assertEquals("Interview confirmation", loaded.cards[0].subject)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty when no cards`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        vm.state.test {
            var content: ActionsContent = ActionsContent.Loading
            while (content is ActionsContent.Loading) {
                content = awaitItem().content
            }
            assertTrue(content is ActionsContent.Empty)
            assertEquals(true, (content as ActionsContent.Empty).hasAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `act on internal suggestion confirms and opens thread`() = runTest(testDispatcher) {
        seedAccount()
        seedMessage("m1")
        intelligence.seedActionItems(
            listOf(cardRow(1L, "m1", ActionUrgency.HIGH, ExternalEffect.NONE)),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            val card = currentCard(vm)
            vm.onAct(card)
            val event = awaitItem()
            assertTrue(event is ActionsEvent.OpenThread)
            assertEquals("t1", (event as ActionsEvent.OpenThread).threadId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `act on external proposal requests confirmation dialog`() = runTest(testDispatcher) {
        seedAccount()
        seedMessage("m1")
        intelligence.seedActionItems(
            listOf(cardRow(1L, "m1", ActionUrgency.HIGH, ExternalEffect.CALENDAR)),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            val card = currentCard(vm)
            assertTrue(card.requiresConfirmation)
            vm.onAct(card)
            val event = awaitItem()
            assertTrue(event is ActionsEvent.ConfirmAction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming external proposal reports not-connected honestly`() =
        runTest(testDispatcher) {
            seedAccount()
            seedMessage("m1")
            intelligence.seedActionItems(
                listOf(cardRow(1L, "m1", ActionUrgency.HIGH, ExternalEffect.CALENDAR)),
            )
            val vm = viewModel()
            advanceUntilIdle()

            vm.events.test {
                val card = currentCard(vm)
                vm.onConfirmDialogResult(card, confirmed = true)
                val event = awaitItem()
                assertTrue(event is ActionsEvent.Message)
                val text = (event as ActionsEvent.Message).text
                assertTrue("honest copy, got: $text", text.contains("isn't connected yet"))
                assertTrue(
                    "no fake success, got: $text",
                    text.contains("nothing was created") || !text.contains("created"),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dismiss removes the card and notifies`() = runTest(testDispatcher) {
        seedAccount()
        seedMessage("m1")
        intelligence.seedActionItems(listOf(cardRow(1L, "m1", ActionUrgency.HIGH)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.events.test {
            val card = currentCard(vm)
            vm.onDismiss(card)
            val event = awaitItem()
            assertTrue(event is ActionsEvent.Message)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(ActionStatus.DISMISSED, intelligence.getActionItem(1L)!!.status)
    }

    private suspend fun currentCard(vm: ActionsViewModel): ActionCardUi {
        var content: ActionsContent = ActionsContent.Loading
        vm.state.test {
            while (content is ActionsContent.Loading) {
                content = awaitItem().content
            }
            cancelAndIgnoreRemainingEvents()
        }
        return (content as ActionsContent.Loaded).cards.single()
    }
}
