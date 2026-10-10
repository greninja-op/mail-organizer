package com.greninjaop.mailorganizer.domain.privacy

import com.greninjaop.mailorganizer.core.privacy.DataSensitivity
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.ui.mail.FakeAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeActiveAccountPreferences : ActiveAccountPreferences {
    private val _selection = MutableStateFlow<AccountSelection>(AccountSelection.Unified)
    override val activeSelection: StateFlow<AccountSelection> = _selection.asStateFlow()

    override suspend fun setActiveSelection(selection: AccountSelection) {
        _selection.value = selection
    }
}

class PrivacyUseCaseTest {

    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var activePrefs: FakeActiveAccountPreferences

    private val account1 = AccountRecord(
        accountId = "acct-1",
        emailAddress = "alex@example.com",
        displayName = "Alex Rivera",
        createdAtEpochMs = 1000L,
        connectionState = ConnectionState.CONNECTED,
    )
    private val account2 = AccountRecord(
        accountId = "acct-2",
        emailAddress = "work@company.org",
        displayName = "Alex Work",
        createdAtEpochMs = 2000L,
        connectionState = ConnectionState.CONNECTED,
    )

    @Before
    fun setUp() {
        accountRepo = FakeAccountRepository().apply {
            seed(account1, account2)
        }
        activePrefs = FakeActiveAccountPreferences()
    }

    @Test
    fun getDataInventory_coversAllRequiredCategories() {
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        // PrivacyUseCase allows inspecting data inventory without full DB
        val categories = listOf(
            "Google OAuth Credentials",
            "Email Messages & Metadata",
            "Deterministic Classifications & Priorities",
            "Company & Sender Directory",
            "Full-Text Search Index",
            "Rules & User Corrections",
            "Action Cards & Proposed Tasks",
            "UI & Display Preferences",
            "Optional AI Fallback Context",
            "Advanced Automation Rules & Provenance History",
        )

        // Mockless validation of inventory items
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )

        val inventory = useCase.getDataInventory()
        assertEquals(10, inventory.size)
        categories.forEach { expectedCat ->
            assertTrue(
                "Missing category: $expectedCat",
                inventory.any { it.categoryName == expectedCat },
            )
        }

        // Verify credentials item specifically guarantees no plain database storage
        val credsItem = inventory.first { it.categoryName == "Google OAuth Credentials" }
        assertEquals(DataSensitivity.HIGHLY_SENSITIVE, credsItem.sensitivity)
        assertTrue(credsItem.leavesDevice)
        assertTrue(credsItem.destinationIfLeaves?.contains("google") == true)

        // Verify local message items do NOT leave device
        val messagesItem = inventory.first { it.categoryName == "Email Messages & Metadata" }
        assertFalse(messagesItem.leavesDevice)
    }

    @Test
    fun getScopeDisclosures_exposesAccurateGmailScopes() {
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )

        val scopes = useCase.getScopeDisclosures()
        assertTrue(scopes.any { it.scopeId == "gmail.readonly" && it.isGranted && !it.isWritePermission })
        assertTrue(scopes.any { it.scopeId == "gmail.modify" && !it.isGranted && it.isWritePermission })
    }

    @Test
    fun deleteAccountData_removesAccountAndResetsActiveSelectionIfMatching() = runTest {
        val testDispatchers = com.greninjaop.mailorganizer.core.AppDispatchers(
            io = kotlinx.coroutines.Dispatchers.Unconfined,
            default = kotlinx.coroutines.Dispatchers.Unconfined,
            main = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        val useCase = PrivacyUseCase(
            appDatabase = null,
            accountRepository = accountRepo,
            activeAccountPreferences = activePrefs,
            integrationManager = null,
            integrationStateRepository = null,
            searchIndexStore = null,
            dispatchers = testDispatchers,
        )

        activePrefs.setActiveSelection(AccountSelection.Single("acct-1"))
        useCase.deleteAccountData("acct-1")

        // Account is removed
        val remaining = accountRepo.observeAll().first()
        assertEquals(1, remaining.size)
        assertEquals("acct-2", remaining.first().accountId)

        // Active selection fallback to Unified
        assertEquals(AccountSelection.Unified, activePrefs.activeSelection.value)
    }
}
