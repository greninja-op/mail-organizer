package com.greninjaop.mailorganizer.ui.privacy

import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.domain.privacy.FakeActiveAccountPreferences
import com.greninjaop.mailorganizer.domain.privacy.PrivacyUseCase
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var activePrefs: FakeActiveAccountPreferences
    private lateinit var privacyUseCase: PrivacyUseCase
    private lateinit var viewModel: PrivacyViewModel

    private val account = AccountRecord(
        accountId = "acct-test",
        emailAddress = "user@test.org",
        displayName = "Test User",
        createdAtEpochMs = 1000L,
        connectionState = ConnectionState.CONNECTED,
    )

    @Before
    fun setUp() {
        accountRepo = FakeAccountRepository().apply { seed(account) }
        activePrefs = FakeActiveAccountPreferences()
        privacyUseCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = dispatchers,
        )
        viewModel = PrivacyViewModel(
            privacyUseCase = privacyUseCase,
            accountRepository = accountRepo,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun initialState_loadsInventoryAndScopes() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(10, state.dataInventory.size)
        assertEquals(4, state.scopes.size)
        assertEquals(1, state.accounts.size)
        assertNotNull(state.auditSnapshot)
    }

    @Test
    fun selectTab_updatesActiveTab() = runTest(testDispatcher) {
        viewModel.selectTab(PrivacyCenterTab.SECURITY_HARDENING)
        assertEquals(PrivacyCenterTab.SECURITY_HARDENING, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(PrivacyCenterTab.DATA_INVENTORY)
        assertEquals(PrivacyCenterTab.DATA_INVENTORY, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun requestDeleteAccount_showsDialog_confirmDisconnectsAccount() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.requestDeleteAccount(account)
        assertEquals(account, viewModel.uiState.value.showDeleteAccountDialogFor)

        viewModel.confirmDeleteAccount(account.accountId)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.showDeleteAccountDialogFor)
        val remaining = accountRepo.observeAll().first()
        assertTrue(remaining.isEmpty())
        assertNotNull(viewModel.uiState.value.statusMessage)
    }

    @Test
    fun clearAllData_dialogFlowAndPurge() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.requestClearAll()
        assertTrue(viewModel.uiState.value.showClearAllDialog)

        viewModel.confirmClearAll()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showClearAllDialog)
        assertNotNull(viewModel.uiState.value.statusMessage)
    }
}
