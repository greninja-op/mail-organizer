package com.greninjaop.mailorganizer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration test for the Room foundation: proves the KSP-generated
 * implementation works and that account rows are isolated per account id.
 * Runs on the JVM via Robolectric (no emulator needed).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AccountDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.accountDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `upsert and getById round-trips an account`() = runTest {
        val record = AccountRecord(
            accountId = "acc-1",
            emailAddress = "a@example.com",
            displayName = "Account A",
            createdAtEpochMs = 1L,
        )
        dao.upsert(record)
        assertEquals(record, dao.getById("acc-1"))
    }

    @Test
    fun `accounts are isolated per account id`() = runTest {
        dao.upsert(AccountRecord("acc-1", "a@example.com", null, 1L))
        dao.upsert(AccountRecord("acc-2", "b@example.com", null, 2L))

        assertEquals("a@example.com", dao.getById("acc-1")?.emailAddress)
        assertEquals("b@example.com", dao.getById("acc-2")?.emailAddress)

        dao.deleteById("acc-1")
        assertNull(dao.getById("acc-1"))
        assertNotNull(dao.getById("acc-2"))
    }
}
