package com.greninjaop.mailorganizer.ui.settings

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 18 — Unit tests for [AccountsViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )
    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var prefs: FakeActiveAccountPreferences

    private class FakeActiveAccountPreferences : ActiveAccountPreferences {
        private val _selection = MutableStateFlow<AccountSelection>(AccountSelection.Unified)
        override val activeSelection: Flow<AccountSelection> = _selection.asStateFlow()
        override val activeAccountSelection: Flow<AccountSelection> = _selection.asStateFlow()

        override suspend fun setActiveSelection(selection: AccountSelection) {
            _selection.value = selection
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountRepo = FakeAccountRepository()
        prefs = FakeActiveAccountPreferences()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observes accounts and active selection`() = runTest(testDispatcher) {
        val acc1 = AccountRecord("acc-1", "alice@example.com", "Alice", 1000L, isEnabled = true)
        val acc2 = AccountRecord("acc-2", "bob@example.com", "Bob", 2000L, isEnabled = true)
        accountRepo.seed(acc1, acc2)

        val vm = AccountsViewModel(
            accountRepository = accountRepo,
            activeAccountPreferences = prefs,
            integrationManager = null,
            dispatchers = dispatchers,
        )

        backgroundScope.launch { vm.accounts.collect {} }
        advanceUntilIdle()

        assertEquals(2, vm.accounts.value.size)
        assertEquals(AccountSelection.Unified, vm.activeSelection.value)
    }

    @Test
    fun `switching account updates active preferences`() = runTest(testDispatcher) {
        val vm = AccountsViewModel(
            accountRepository = accountRepo,
            activeAccountPreferences = prefs,
            integrationManager = null,
            dispatchers = dispatchers,
        )

        // Select account 1
        vm.selectAccount("acc-1")
        advanceUntilIdle()
        assertEquals(AccountSelection.Single("acc-1"), prefs.activeSelection.first())

        // Select unified mode (null)
        vm.selectAccount(null)
        advanceUntilIdle()
        assertEquals(AccountSelection.Unified, prefs.activeSelection.first())
    }

    @Test
    fun `toggle account enabled changes enabled state`() = runTest(testDispatcher) {
        val acc = AccountRecord("acc-1", "alice@example.com", "Alice", 1000L, isEnabled = true)
        accountRepo.seed(acc)

        val vm = AccountsViewModel(
            accountRepository = accountRepo,
            activeAccountPreferences = prefs,
            integrationManager = null,
            dispatchers = dispatchers,
        )

        backgroundScope.launch { vm.accounts.collect {} }
        advanceUntilIdle()

        vm.toggleEnabled(acc)
        advanceUntilIdle()

        assertFalse(accountRepo.getById("acc-1")!!.isEnabled)
    }

    @Test
    fun `add local account persists account in repository`() = runTest(testDispatcher) {
        val vm = AccountsViewModel(
            accountRepository = accountRepo,
            activeAccountPreferences = prefs,
            integrationManager = null,
            dispatchers = dispatchers,
            clock = { 5000L },
        )

        vm.addLocalAccount("test@local.domain", "Local Test")
        advanceUntilIdle()

        val all = accountRepo.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("test@local.domain", all[0].emailAddress)
        assertEquals("Local Test", all[0].displayName)
        assertTrue(all[0].isEnabled)
    }

    @Test
    fun `disconnect account removes account and resets active selection if removed`() = runTest(testDispatcher) {
        val acc1 = AccountRecord("acc-1", "alice@example.com", "Alice", 1000L, isEnabled = true)
        val acc2 = AccountRecord("acc-2", "bob@example.com", "Bob", 2000L, isEnabled = true)
        accountRepo.seed(acc1, acc2)

        val vm = AccountsViewModel(
            accountRepository = accountRepo,
            activeAccountPreferences = prefs,
            integrationManager = null,
            dispatchers = dispatchers,
        )

        backgroundScope.launch { vm.accounts.collect {} }
        advanceUntilIdle()

        // Set active account to acc-1
        vm.selectAccount("acc-1")
        advanceUntilIdle()
        assertEquals(AccountSelection.Single("acc-1"), prefs.activeSelection.first())

        // Disconnect acc-1
        vm.disconnectAccount("acc-1")
        advanceUntilIdle()

        // acc-1 is deleted
        assertNull(accountRepo.getById("acc-1"))
        assertNotNull(accountRepo.getById("acc-2"))

        // Active selection reverts to Unified
        assertEquals(AccountSelection.Unified, prefs.activeSelection.first())
    }
}
