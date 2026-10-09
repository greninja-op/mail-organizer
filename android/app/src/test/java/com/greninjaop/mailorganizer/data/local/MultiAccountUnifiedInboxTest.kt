package com.greninjaop.mailorganizer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.search.SearchQuery
import com.greninjaop.mailorganizer.core.search.SearchResult
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences
import com.greninjaop.mailorganizer.data.repository.RoomAccountRepository
import com.greninjaop.mailorganizer.data.repository.RoomIntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.RoomMailRepository
import com.greninjaop.mailorganizer.data.repository.RoomSearchRepository
import com.greninjaop.mailorganizer.data.repository.SearchRepository
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import com.greninjaop.mailorganizer.ui.settings.AccountsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 18 — Multi-Account & Unified Inbox verification.
 *
 * Verifies:
 * 1. Database multi-account isolation and foreign-key cascade safety when disconnecting.
 * 2. Unified inbox queries combine threads and messages across enabled accounts only.
 * 3. Unified search queries across all enabled accounts with account context.
 * 4. ActiveAccountPreferences switching between Unified and Single account modes.
 * 5. AccountsViewModel supports local accounts, active selection, and clean disconnects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MultiAccountUnifiedInboxTest {

    private lateinit var db: AppDatabase
    private lateinit var mailRepo: RoomMailRepository
    private lateinit var accountRepo: RoomAccountRepository
    private lateinit var searchRepo: SearchRepository
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mailRepo = RoomMailRepository(db, dispatchers)
        accountRepo = RoomAccountRepository(db.accountDao(), dispatchers)
        searchRepo = RoomSearchRepository(
            db = db,
            mail = mailRepo,
            intelligence = RoomIntelligenceRepository(db, dispatchers),
            dispatchers = dispatchers,
            clock = { 1_000_000L },
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedTwoAccounts() {
        val acc1 = AccountRecord(
            accountId = "acc-1",
            emailAddress = "alice@example.com",
            displayName = "Alice Personal",
            createdAtEpochMs = 1000L,
            isEnabled = true,
        )
        val acc2 = AccountRecord(
            accountId = "acc-2",
            emailAddress = "work@company.com",
            displayName = "Alice Work",
            createdAtEpochMs = 2000L,
            isEnabled = true,
        )
        db.accountDao().upsert(acc1)
        db.accountDao().upsert(acc2)

        // Account 1 thread & messages
        val thread1 = ThreadRecord(
            threadId = "t-1",
            gmailThreadId = "gt-1",
            accountId = "acc-1",
            subject = "Personal Dinner",
            latestMessageEpochMs = 5000L,
            updatedAtEpochMs = 5000L,
        )
        val msg1 = MessageRecord(
            messageId = "m-1",
            gmailMessageId = "gm-1",
            threadId = "t-1",
            accountId = "acc-1",
            fromAddress = "friend@example.com",
            fromName = "Friend",
            subject = "Personal Dinner",
            snippet = "Are we still meeting tonight?",
            bodyText = "Are we still meeting tonight?",
            timestampEpochMs = 5000L,
            unread = true,
            starred = true,
        )
        db.threadDao().upsert(thread1)
        db.messageDao().upsert(msg1)

        // Account 2 thread & messages
        val thread2 = ThreadRecord(
            threadId = "t-2",
            gmailThreadId = "gt-2",
            accountId = "acc-2",
            subject = "Work Interview Announcement",
            latestMessageEpochMs = 8000L,
            updatedAtEpochMs = 8000L,
        )
        val msg2 = MessageRecord(
            messageId = "m-2",
            gmailMessageId = "gm-2",
            threadId = "t-2",
            accountId = "acc-2",
            fromAddress = "hr@company.com",
            fromName = "HR Recruiter",
            subject = "Work Interview Announcement",
            snippet = "Your interview is scheduled for Friday",
            bodyText = "Your interview is scheduled for Friday",
            timestampEpochMs = 8000L,
            unread = true,
            starred = false,
        )
        db.threadDao().upsert(thread2)
        db.messageDao().upsert(msg2)
    }

    @Test
    fun `unified threads query combines accounts newest first`() = runTest(testDispatcher) {
        seedTwoAccounts()

        db.threadDao().observeUnified(10).test {
            val list = awaitItem()
            assertEquals(2, list.size)
            // t-2 is 8000ms, t-1 is 5000ms -> t-2 should be first
            assertEquals("t-2", list[0].threadId)
            assertEquals("acc-2", list[0].accountId)
            assertEquals("t-1", list[1].threadId)
            assertEquals("acc-1", list[1].accountId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disabled account threads are excluded from unified query`() = runTest(testDispatcher) {
        seedTwoAccounts()

        // Disable acc-2
        db.accountDao().setEnabled("acc-2", false)

        db.threadDao().observeUnified(10).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("t-1", list[0].threadId)
            assertEquals("acc-1", list[0].accountId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unified unread and starred messages observe only enabled accounts`() = runTest(testDispatcher) {
        seedTwoAccounts()

        // Unread messages unified
        db.messageDao().observeUnifiedUnread(10).test {
            val unread = awaitItem()
            assertEquals(2, unread.size)
            assertEquals("m-2", unread[0].messageId)
            assertEquals("m-1", unread[1].messageId)
            cancelAndIgnoreRemainingEvents()
        }

        // Starred messages unified (only m-1 is starred)
        db.messageDao().observeUnifiedStarred(10).test {
            val starred = awaitItem()
            assertEquals(1, starred.size)
            assertEquals("m-1", starred[0].messageId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnecting account cascades its data leaving other account intact`() = runTest(testDispatcher) {
        seedTwoAccounts()

        // Verify pre-state
        assertNotNull(accountRepo.getById("acc-1"))
        assertNotNull(accountRepo.getById("acc-2"))
        assertEquals(1, db.threadDao().getByAccount("acc-1", 10).size)
        assertEquals(1, db.threadDao().getByAccount("acc-2", 10).size)

        // Delete acc-1 (simulating disconnect)
        accountRepo.deleteById("acc-1")

        // acc-1 data should be completely gone
        assertNull(accountRepo.getById("acc-1"))
        assertTrue(db.threadDao().getByAccount("acc-1", 10).isEmpty())
        assertNull(db.messageDao().getById("m-1"))

        // acc-2 data must remain completely intact
        assertNotNull(accountRepo.getById("acc-2"))
        val acc2Threads = db.threadDao().getByAccount("acc-2", 10)
        assertEquals(1, acc2Threads.size)
        assertEquals("t-2", acc2Threads[0].threadId)
        assertNotNull(db.messageDao().getById("m-2"))
    }

    @Test
    fun `account scoped search vs unified search`() = runTest(testDispatcher) {
        seedTwoAccounts()

        // Account-scoped search for acc-1
        val scopedOutcome1 = searchRepo.search(
            SearchQuery(rawText = "Interview", accountId = "acc-1"),
        )
        assertTrue(
            "acc-1 search should not find acc-2's interview email",
            scopedOutcome1.results.isEmpty(),
        )

        // Account-scoped search for acc-2
        val scopedOutcome2 = searchRepo.search(
            SearchQuery(rawText = "Interview", accountId = "acc-2"),
        )
        assertEquals(1, scopedOutcome2.results.size)
        val msgResult2 = scopedOutcome2.results[0] as SearchResult.Message
        assertEquals("m-2", msgResult2.record.messageId)
        assertEquals("acc-2", msgResult2.record.accountId)

        // Unified search (accountId = null) finds across both accounts
        val unifiedOutcome = searchRepo.search(
            SearchQuery(rawText = "Dinner", accountId = null),
        )
        assertEquals(1, unifiedOutcome.results.size)
        val unifiedMsg = unifiedOutcome.results[0] as SearchResult.Message
        assertEquals("m-1", unifiedMsg.record.messageId)
        assertEquals("acc-1", unifiedMsg.record.accountId)
    }

    @Test
    fun `active account preferences switching and view model interaction`() = runTest(testDispatcher) {
        seedTwoAccounts()

        val fakePrefs = FakeActiveAccountPreferences()
        assertEquals(AccountSelection.Unified, fakePrefs.activeAccountSelection.first())

        val vm = AccountsViewModel(
            accountRepository = accountRepo,
            activeAccountPreferences = fakePrefs,
            integrationManager = null,
            dispatchers = dispatchers,
        )

        // Switch to account 1
        vm.selectAccount("acc-1")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AccountSelection.Single("acc-1"), fakePrefs.activeAccountSelection.first())

        // Switch back to Unified
        vm.selectAccount(null)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AccountSelection.Unified, fakePrefs.activeAccountSelection.first())

        // Add local account
        vm.addLocalAccount("new@example.com", "New Account")
        testDispatcher.scheduler.advanceUntilIdle()
        val all = accountRepo.observeAll().first()
        assertEquals(3, all.size)
        assertTrue(all.any { it.emailAddress == "new@example.com" })
    }

    private class FakeActiveAccountPreferences : ActiveAccountPreferences {
        private val _selection = MutableStateFlow<AccountSelection>(AccountSelection.Unified)
        override val activeSelection: Flow<AccountSelection> = _selection.asStateFlow()
        override val activeAccountSelection: Flow<AccountSelection> = _selection.asStateFlow()

        override suspend fun setActiveSelection(selection: AccountSelection) {
            _selection.value = selection
        }
    }
}
