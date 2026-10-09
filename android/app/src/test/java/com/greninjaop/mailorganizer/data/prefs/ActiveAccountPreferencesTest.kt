package com.greninjaop.mailorganizer.data.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — Unit tests for [ActiveAccountPreferences] and [AccountSelection].
 */
class ActiveAccountPreferencesTest {

    private class TestActiveAccountPreferences : ActiveAccountPreferences {
        private val _selection = MutableStateFlow<AccountSelection>(AccountSelection.Unified)
        override val activeSelection = _selection.asStateFlow()
        override val activeAccountSelection = _selection.asStateFlow()

        override suspend fun setActiveSelection(selection: AccountSelection) {
            _selection.value = selection
        }
    }

    @Test
    fun `account selection model equality and semantics`() {
        assertEquals(AccountSelection.Unified, AccountSelection.Unified)
        assertEquals(AccountSelection.Single("acc-1"), AccountSelection.Single("acc-1"))
        assertFalse(AccountSelection.Single("acc-1") == AccountSelection.Single("acc-2"))
        assertFalse(AccountSelection.Unified == AccountSelection.Single("acc-1"))
    }

    @Test
    fun `preferences default to unified mode`() = runTest {
        val prefs = TestActiveAccountPreferences()
        assertEquals(AccountSelection.Unified, prefs.activeSelection.first())
        assertEquals(AccountSelection.Unified, prefs.activeAccountSelection.first())
    }

    @Test
    fun `switching account updates selection correctly`() = runTest {
        val prefs = TestActiveAccountPreferences()

        // Switch to specific account
        prefs.setActiveAccount("work@company.com")
        assertEquals(AccountSelection.Single("work@company.com"), prefs.activeSelection.first())

        // Switch back to Unified via null
        prefs.setActiveAccount(null)
        assertEquals(AccountSelection.Unified, prefs.activeSelection.first())

        // Switch via explicit setActiveSelection
        prefs.setActiveSelection(AccountSelection.Single("personal@gmail.com"))
        assertEquals(AccountSelection.Single("personal@gmail.com"), prefs.activeSelection.first())

        prefs.setActiveSelection(AccountSelection.Unified)
        assertEquals(AccountSelection.Unified, prefs.activeSelection.first())
    }
}
