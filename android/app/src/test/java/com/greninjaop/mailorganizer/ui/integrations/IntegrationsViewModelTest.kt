package com.greninjaop.mailorganizer.ui.integrations

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.data.integrations.CalendarIntegrationAdapter
import com.greninjaop.mailorganizer.data.integrations.GmailIntegrationAdapter
import com.greninjaop.mailorganizer.data.integrations.TasksIntegrationAdapter
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.IntegrationStateRecord
import com.greninjaop.mailorganizer.data.repository.IntegrationStateRepository
import com.greninjaop.mailorganizer.data.sync.DeferredGmailSyncApi
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * IntegrationsViewModel tests (Phase 17).
 *
 * Real manager + real adapters + fake accounts/state — the honest
 * statuses are asserted end-to-end at the UI boundary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accounts: FakeAccountRepository
    private lateinit var states: FakeStateRepository

    private class FakeStateRepository : IntegrationStateRepository {
        val rows = mutableListOf<IntegrationStateRecord>()
        override suspend fun upsert(record: IntegrationStateRecord) {
            rows.removeIf {
                it.integrationId == record.integrationId && it.accountId == record.accountId
            }
            rows.add(record)
        }
        override suspend fun getForAccount(accountId: String?) =
            rows.filter { it.accountId == accountId }
        override suspend fun clear(integrationId: String, accountId: String?) {
            rows.removeIf { it.integrationId == integrationId && it.accountId == accountId }
        }
        override suspend fun clearForAccount(accountId: String?) {
            rows.removeIf { it.accountId == accountId }
        }
    }

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private fun manager() = IntegrationManager(
        adapters = listOf(
            GmailIntegrationAdapter(DeferredGmailSyncApi(), accounts),
            CalendarIntegrationAdapter(),
            TasksIntegrationAdapter(),
        ),
        states = states,
        dispatchers = dispatchers(),
        clock = { 0L },
    )

    private fun viewModel() = IntegrationsViewModel(
        manager = manager(),
        accounts = accounts,
        dispatchers = dispatchers(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        accounts = FakeAccountRepository()
        states = FakeStateRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private suspend fun seedAccount() {
        accounts.upsert(
            AccountRecord(
                accountId = "a1",
                emailAddress = "a1@example.com",
                displayName = "Test",
                connectionState = ConnectionState.DISCONNECTED,
                createdAtEpochMs = 1L,
            ),
        )
    }

    @Test
    fun `loads all three integrations with honest statuses`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        vm.uiState.test {
            val state = awaitItem()
            // Eagerly-started flow may emit Loading first; skip to Loaded.
            val loaded = if (state is IntegrationsUiState.Loaded) state
            else awaitItem() as IntegrationsUiState.Loaded
            assertEquals(3, loaded.snapshots.size)
            assertEquals("a1@example.com", loaded.accountEmail)
            val byId = loaded.snapshots.associateBy { it.id }
            // Gmail: local-only account → AUTH_REQUIRED, never CONNECTED.
            assertEquals(
                IntegrationStatus.AUTH_REQUIRED,
                byId[IntegrationId.GMAIL]?.status,
            )
            // Calendar/Tasks: deferred → UNAVAILABLE.
            assertEquals(
                IntegrationStatus.UNAVAILABLE,
                byId[IntegrationId.CALENDAR]?.status,
            )
            assertEquals(
                IntegrationStatus.UNAVAILABLE,
                byId[IntegrationId.TASKS]?.status,
            )
            // Nothing is ever reported connected.
            assertTrue(loaded.snapshots.none { it.status == IntegrationStatus.CONNECTED })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gmail connect emits honest phase 3 message`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        vm.events.test {
            vm.onConnect(IntegrationId.GMAIL)
            val event = awaitItem()
            assertTrue(event is IntegrationsEvent.Message)
            assertTrue(
                "was: ${(event as IntegrationsEvent.Message).text}",
                event.text.contains("Phase 3"),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `calendar connect emits honest not-available message`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        vm.events.test {
            vm.onConnect(IntegrationId.CALENDAR)
            val event = awaitItem()
            assertTrue(event is IntegrationsEvent.Message)
            assertTrue(
                "was: ${(event as IntegrationsEvent.Message).text}",
                event.text.contains("Phase 15"),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `snapshotFor returns null for unknown id`() = runTest(testDispatcher) {
        seedAccount()
        val vm = viewModel()
        assertEquals(null, vm.snapshotFor(IntegrationId("nope")))
    }
}
