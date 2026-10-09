package com.greninjaop.mailorganizer.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.SyncStatus
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import com.greninjaop.mailorganizer.data.repository.RoomAccountRepository
import com.greninjaop.mailorganizer.data.repository.RoomMailRepository
import com.greninjaop.mailorganizer.data.repository.RoomSyncStateRepository
import com.greninjaop.mailorganizer.data.repository.SyncStateRepository
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sync engine tests (Phase 4, phase §45–50): the coordinator is exercised
 * end to end over a real in-memory database against a deterministic fake
 * Gmail API. No real email data, no network, no credentials.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncCoordinatorTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var mail: MailRepository
    private lateinit var sync: SyncStateRepository
    private lateinit var fake: FakeGmailSyncApi

    private val accA = AccountId("acc-a")
    private val accB = AccountId("acc-b")

    private fun testPolicy() = SyncRetryPolicy(
        maxAttempts = 3,
        baseDelayMs = 1L,
        maxDelayMs = 10L,
        random = Random(42),
    )

    private fun coordinator(
        api: GmailSyncApi = fake,
        mailRepo: MailRepository = mail,
        config: SyncConfig = SyncConfig(pageSize = 10, maxMessagesPerRun = 100),
    ) = SyncCoordinator(
        api = api,
        mail = mailRepo,
        syncState = sync,
        dispatchers = AppDispatchers(),
        retryPolicy = testPolicy(),
        config = config,
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dispatchers = AppDispatchers()
        accounts = RoomAccountRepository(db.accountDao(), dispatchers)
        mail = RoomMailRepository(db, dispatchers)
        sync = RoomSyncStateRepository(db, dispatchers)
        fake = FakeGmailSyncApi()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedAccount(id: String) {
        accounts.upsert(AccountRecord(id, "$id@example.test", null, 1L))
    }

    private suspend fun messageCount(accountId: String) = mail.countByAccount(accountId)

    private suspend fun cursorFor(accountId: String) =
        sync.ensureForAccount(accountId).cursor

    // ---------- pagination (phase §46) ----------

    @Test
    fun `paginated initial sync imports every message exactly once`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(
                listOf(fakeRemoteMessage("m-1", "t-1"), fakeRemoteMessage("m-2", "t-1")),
                "tok-2",
            ),
            MessagePage(
                listOf(fakeRemoteMessage("m-3", "t-2"), fakeRemoteMessage("m-4", "t-2")),
                "tok-3",
            ),
            MessagePage(listOf(fakeRemoteMessage("m-5", "t-3")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-100")

        val outcome = coordinator().syncNow(accA)
        assertTrue(outcome is SyncOutcome.Success)
        val summary = (outcome as SyncOutcome.Success).summary
        assertEquals(5, summary.messagesProcessed)
        assertEquals(5, summary.inserted)
        assertEquals(0, summary.updated)
        assertTrue(summary.completedInitialSync)
        assertEquals(5, messageCount("acc-a"))

        // Cursor is now in incremental mode.
        assertEquals(SyncCursor.History("h-100"), SyncCursor.decode(cursorFor("acc-a")))
        // Thread aggregates are truthful.
        val thread = mail.getThreadByGmailId("acc-a", "t-1")!!
        assertEquals(2, thread.messageCount)
    }

    @Test
    fun `empty pages are skipped without losing pagination`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(emptyList(), "tok-2"), // empty page mid-pagination (§12)
            MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-1")

        val outcome = coordinator().syncNow(accA)
        assertTrue(outcome is SyncOutcome.Success)
        assertEquals(1, messageCount("acc-a"))
    }

    @Test
    fun `empty mailbox succeeds with zero messages`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages("acc-a", MessagePage(emptyList(), null))
        fake.scriptHistoryCursor("acc-a", "h-0")

        val outcome = coordinator().syncNow(accA)
        val summary = (outcome as SyncOutcome.Success).summary
        assertEquals(0, summary.messagesProcessed)
        assertEquals(0, messageCount("acc-a"))
    }

    // ---------- idempotency (phase §47) ----------

    @Test
    fun `repeated sync creates no duplicates`() = runTest {
        seedAccount("acc-a")
        val pages = listOf(
            MessagePage(
                listOf(fakeRemoteMessage("m-1", "t-1"), fakeRemoteMessage("m-2", "t-1")),
                null,
            ),
        )
        fake.scriptPages("acc-a", *pages.toTypedArray())
        fake.scriptHistoryCursor("acc-a", "h-1")
        // Incremental pass returns the same messages again.
        fake.scriptChanges(
            "acc-a",
            ChangePage(
                listOf(fakeRemoteMessage("m-1", "t-1"), fakeRemoteMessage("m-2", "t-1")),
                nextHistoryCursor = "h-2",
            ),
        )

        coordinator().syncNow(accA)
        assertEquals(2, messageCount("acc-a"))

        val second = coordinator().syncNow(accA) as SyncOutcome.Success
        assertEquals(2, messageCount("acc-a"))
        assertEquals(0, second.summary.inserted)
        assertEquals(2, second.summary.updated)
    }

    // ---------- account isolation (phase §48) ----------

    @Test
    fun `two accounts sync without cross-contamination`() = runTest {
        seedAccount("acc-a")
        seedAccount("acc-b")
        fake.scriptPages(
            "acc-a",
            MessagePage(
                listOf(fakeRemoteMessage("m-1", "t-1", from = "a1@example.test")),
                null,
            ),
        )
        fake.scriptPages(
            "acc-b",
            MessagePage(
                listOf(fakeRemoteMessage("m-1", "t-1", from = "b1@example.test")),
                null,
            ),
        )
        fake.scriptHistoryCursor("acc-a", "h-a")
        fake.scriptHistoryCursor("acc-b", "h-b")

        val c = coordinator()
        c.syncNow(accA)
        c.syncNow(accB)

        // Same Gmail ids in both accounts, but fully separated rows.
        assertEquals(1, messageCount("acc-a"))
        assertEquals(1, messageCount("acc-b"))
        val aMsg = db.messageDao().getById("acc-a:m-1")!!
        val bMsg = db.messageDao().getById("acc-b:m-1")!!
        assertEquals("a1@example.test", aMsg.fromAddress)
        assertEquals("b1@example.test", bMsg.fromAddress)
        assertEquals("acc-a", aMsg.accountId)
        assertEquals("acc-b", bMsg.accountId)
    }

    // ---------- failure recovery (phase §49) ----------

    @Test
    fun `database failure mid-sync does not advance cursor and retry recovers`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), "tok-2"),
            MessagePage(listOf(fakeRemoteMessage("m-2", "t-2")), "tok-3"),
            MessagePage(listOf(fakeRemoteMessage("m-3", "t-3")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-9")

        val failing = object : MailRepository by mail {
            var persistCalls = 0
            override suspend fun saveThreadWithMessages(
                thread: ThreadRecord,
                messages: List<MessageRecord>,
            ) {
                persistCalls++
                if (persistCalls == 2) throw android.database.sqlite.SQLiteException("boom")
                mail.saveThreadWithMessages(thread, messages)
            }
        }

        val failed = coordinator(mailRepo = failing).syncNow(accA)
        assertTrue(failed is SyncOutcome.Failed)
        assertTrue((failed as SyncOutcome.Failed).error is MoError.Database)

        // Page 1 committed and cursor advanced past it; page 2 NOT committed.
        assertEquals(1, messageCount("acc-a"))
        assertEquals(SyncCursor.Page("tok-2"), SyncCursor.decode(cursorFor("acc-a")))
        assertEquals(SyncStatus.FAILED, sync.ensureForAccount("acc-a").status)

        // Retry resumes at page 2 and completes; no duplicates.
        val recovered = coordinator().syncNow(accA) as SyncOutcome.Success
        assertEquals(3, messageCount("acc-a"))
        assertEquals(2, recovered.summary.inserted) // m-2, m-3
        assertTrue(recovered.summary.completedInitialSync)
    }

    @Test
    fun `transient network failure is retried then succeeds`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-1")
        fake.scriptFailures(
            "acc-a",
            null,
            SyncApiException.NetworkError(),
            SyncApiException.NetworkError(),
        )

        val outcome = coordinator().syncNow(accA)
        assertTrue(outcome is SyncOutcome.Success)
        assertEquals(1, messageCount("acc-a"))
        assertEquals(3, fake.fetchPageCount.get()) // 2 failures + 1 success
    }

    @Test
    fun `auth failure is not retried and marks auth error`() = runTest {
        seedAccount("acc-a")
        fake.scriptFailures("acc-a", null, SyncApiException.AuthExpired())

        val outcome = coordinator().syncNow(accA)
        assertTrue(outcome is SyncOutcome.Failed)
        val error = (outcome as SyncOutcome.Failed).error
        assertTrue(error is MoError.Authentication)
        assertEquals(1, fake.fetchPageCount.get()) // no blind retry
        val state = sync.ensureForAccount("acc-a")
        assertEquals(SyncStatus.FAILED, state.status)
        assertEquals("auth", state.errorCode)
        // Failed sync never wipes existing data (none here, but state is consistent).
        assertEquals(0, messageCount("acc-a"))
    }

    @Test
    fun `not-configured api surfaces invalid configuration`() = runTest {
        seedAccount("acc-a")
        val outcome = coordinator(api = DeferredGmailSyncApi()).syncNow(accA)
        assertTrue(outcome is SyncOutcome.Failed)
        assertTrue((outcome as SyncOutcome.Failed).error is MoError.InvalidConfiguration)
    }

    // ---------- cancellation (phase §50) ----------

    @Test
    fun `cancellation pauses sync and resume completes`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(
                listOf(fakeRemoteMessage("m-1", "t-1"), fakeRemoteMessage("m-2", "t-1")),
                "tok-2",
            ),
            MessagePage(listOf(fakeRemoteMessage("m-3", "t-2")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-5")
        fake.pageGate = CompletableDeferred()

        val c = coordinator()
        var outcome: SyncOutcome? = null
        val job = launch { outcome = c.syncNow(accA) }
        // Wait until the engine is actually inside the API call, then cancel.
        withTimeout(10_000) {
            while (fake.pageCalls.isEmpty()) kotlinx.coroutines.delay(10)
        }
        job.cancel()
        job.join()

        assertEquals(SyncOutcome.Cancelled, outcome)
        assertEquals(SyncStatus.PAUSED, sync.ensureForAccount("acc-a").status)
        assertEquals(SyncProgress.Idle, c.progress.value)
        // Nothing committed yet (page 1 never persisted).
        assertEquals(0, messageCount("acc-a"))

        // Resume works after cancellation.
        fake.pageGate = null
        val resumed = c.syncNow(accA) as SyncOutcome.Success
        assertEquals(3, messageCount("acc-a"))
        assertTrue(resumed.summary.completedInitialSync)
    }

    // ---------- concurrency (phase §29–30) ----------

    @Test
    fun `concurrent syncNow calls share one in-flight execution`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-1")
        val gate = CompletableDeferred<Unit>()
        fake.pageGate = gate

        val c = coordinator()
        val d1 = async { c.syncNow(accA) }
        withTimeout(10_000) {
            while (fake.pageCalls.isEmpty()) kotlinx.coroutines.delay(10)
        }
        val d2 = async { c.syncNow(accA) } // rapid second tap joins the first
        gate.complete(Unit)
        val first = d1.await()
        val second = d2.await()

        assertTrue(first is SyncOutcome.Success)
        assertTrue(second is SyncOutcome.Success)
        // First page fetched exactly once — no duplicate job.
        assertEquals(1, fake.pageCalls.count { it.pageToken == null })
        assertEquals(1, messageCount("acc-a"))
    }

    // ---------- incremental + history invalidation (phase §23–24) ----------

    @Test
    fun `incremental sync applies changes and deletions`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(
                listOf(
                    fakeRemoteMessage("m-1", "t-1"),
                    fakeRemoteMessage("m-2", "t-2"),
                ),
                null,
            ),
        )
        fake.scriptHistoryCursor("acc-a", "h-10")
        fake.scriptChanges(
            "acc-a",
            ChangePage(
                messages = listOf(fakeRemoteMessage("m-3", "t-3")),
                deletedRemoteIds = listOf("m-1"),
                nextHistoryCursor = "h-11",
            ),
        )

        val c = coordinator()
        c.syncNow(accA)
        assertEquals(2, messageCount("acc-a"))

        val inc = c.syncNow(accA) as SyncOutcome.Success
        assertEquals(1, inc.summary.inserted)
        assertEquals(1, inc.summary.deleted)
        assertEquals(2, messageCount("acc-a")) // m-2, m-3 remain
        assertEquals(null, db.messageDao().getById("acc-a:m-1"))
        assertEquals(SyncCursor.History("h-11"), SyncCursor.decode(cursorFor("acc-a")))
    }

    @Test
    fun `invalid history cursor triggers controlled re-baseline`() = runTest {
        seedAccount("acc-a")
        val initial = MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), null)
        fake.scriptPages("acc-a", initial)
        fake.scriptHistoryCursor("acc-a", "h-1")

        val c = coordinator()
        c.syncNow(accA)
        assertEquals(1, messageCount("acc-a"))

        // Server invalidates the cursor; next sync must re-baseline, not crash.
        fake.scriptHistoryInvalid("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-2")

        val outcome = c.syncNow(accA)
        assertTrue(outcome is SyncOutcome.Success)
        assertEquals(1, messageCount("acc-a")) // idempotent: still exactly one
        assertEquals(SyncCursor.History("h-2"), SyncCursor.decode(cursorFor("acc-a")))
    }

    // ---------- bounded initial sync (phase §11) ----------

    @Test
    fun `large mailbox sync is bounded per run and resumes`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(
                listOf(fakeRemoteMessage("m-1", "t-1"), fakeRemoteMessage("m-2", "t-2")),
                "tok-2",
            ),
            MessagePage(
                listOf(fakeRemoteMessage("m-3", "t-3"), fakeRemoteMessage("m-4", "t-4")),
                "tok-3",
            ),
            MessagePage(listOf(fakeRemoteMessage("m-5", "t-5")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-9")

        val c = coordinator(config = SyncConfig(pageSize = 10, maxMessagesPerRun = 3))
        val first = c.syncNow(accA) as SyncOutcome.Success
        // Stopped early at a page boundary with the cursor saved (not complete).
        assertEquals(false, first.summary.completedInitialSync)
        assertEquals(SyncCursor.Page("tok-3"), SyncCursor.decode(cursorFor("acc-a")))

        val second = c.syncNow(accA) as SyncOutcome.Success
        assertTrue(second.summary.completedInitialSync)
        assertEquals(5, messageCount("acc-a"))
    }

    // ---------- progress honesty (phase §31) ----------

    @Test
    fun `progress reports stages and counts, never fabricated percentages`() = runTest {
        seedAccount("acc-a")
        fake.scriptPages(
            "acc-a",
            MessagePage(listOf(fakeRemoteMessage("m-1", "t-1")), null),
        )
        fake.scriptHistoryCursor("acc-a", "h-1")

        val c = coordinator()
        val outcome = c.syncNow(accA)
        assertTrue(outcome is SyncOutcome.Success)
        val progress = c.progress.value
        assertTrue(progress is SyncProgress.Succeeded)
        assertEquals(1, (progress as SyncProgress.Succeeded).summary.messagesProcessed)
    }
}
