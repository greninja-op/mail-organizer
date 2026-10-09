package com.greninjaop.mailorganizer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountScopeTest {

    @Test(expected = IllegalArgumentException::class)
    fun `AccountId rejects blank value`() {
        AccountId("   ")
    }

    @Test
    fun `AccountId equality is value-based`() {
        assertEquals(AccountId("a1"), AccountId("a1"))
        assertNotEquals(AccountId("a1"), AccountId("a2"))
    }

    @Test
    fun `scopedKey namespaces keys per account`() {
        val a = AccountId("a1")
        val b = AccountId("b2")
        assertEquals("account_a1_theme", scopedKey(a, "theme"))
        assertNotEquals(scopedKey(a, "theme"), scopedKey(b, "theme"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scopedKey rejects blank name`() {
        scopedKey(AccountId("a1"), "")
    }
}
