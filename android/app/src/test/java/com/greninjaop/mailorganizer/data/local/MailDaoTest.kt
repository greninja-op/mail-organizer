package com.greninjaop.mailorganizer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for threads, messages, senders, companies and the
 * intelligence tables (Phase 2). Runs on the JVM via Robolectric.
 *
 * Synthetic data only — never real personal email (phase §46).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MailDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedAccount(id: String = "acc-1") {
        db.accountDao().upsert(AccountRecord(id, "$id@example.test", null, 1L))
    }

    private fun thread(id: String = "thr-1", accountId: String = "acc-1") = ThreadRecord(
        threadId = id,
        gmailThreadId = "gmail-$id",
        accountId = accountId,
        subject = "Test thread",
        participantDisplayNames = listOf("Example Sender"),
        latestMessageEpochMs = 100L,
        updatedAtEpochMs = 100L,
    )

    private fun message(
        id: String = "msg-1",
        threadId: String = "thr-1",
        accountId: String = "acc-1",
        unread: Boolean = true,
        starred: Boolean = false,
        subject: String = "Test Message",
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = "gmail-$id",
        threadId = threadId,
        accountId = accountId,
        fromAddress = "notifications@example.test",
        fromName = "Example Sender",
        toAddresses = listOf("$accountId@example.test"),
        subject = subject,
        snippet = "snippet",
        bodyText = "body",
        timestampEpochMs = 100L,
        unread = unread,
        starred = starred,
    )

    @Test
    fun `thread and message insert and read back`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message())

        val loaded = db.messageDao().getById("msg-1")
        assertNotNull(loaded)
        assertEquals("Test Message", loaded!!.subject)
        assertEquals("thr-1", loaded.threadId)
    }

    @Test
    fun `thread message relationship is enforced`() = runTest {
        seedAccount()
        // Message referencing a missing thread must fail (FK constraint).
        var failed = false
        try {
            db.messageDao().upsert(message(threadId = "no-such-thread"))
        } catch (e: Exception) {
            failed = true
        }
        assertTrue("expected FK violation for orphan message", failed)
    }

    @Test
    fun `unread and starred filters work`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message("m1", unread = true))
        db.messageDao().upsert(message("m2", unread = false, starred = true))

        db.messageDao().observeUnreadByAccount("acc-1", 10).test {
            assertEquals(listOf("m1"), awaitItem().map { it.messageId })
            cancelAndIgnoreRemainingEvents()
        }
        db.messageDao().observeStarredByAccount("acc-1", 10).test {
            assertEquals(listOf("m2"), awaitItem().map { it.messageId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mark read and star update state`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message())

        db.messageDao().setUnread("msg-1", false)
        db.messageDao().setStarred("msg-1", true)

        val loaded = db.messageDao().getById("msg-1")!!
        assertEquals(false, loaded.unread)
        assertEquals(true, loaded.starred)
    }

    @Test
    fun `text search finds subject matches`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message("m1", subject = "Quarterly invoice"))
        db.messageDao().upsert(message("m2", subject = "Lunch plans"))

        val hits = db.messageDao().searchByText("acc-1", "invoice", 10)
        assertEquals(1, hits.size)
        assertEquals("m1", hits[0].messageId)
    }

    @Test
    fun `classification set replaces previous row`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message())

        val dao = db.classificationDao()
        dao.setClassification(
            ClassificationRecord(
                messageId = "msg-1", accountId = "acc-1",
                category = MailCategory.NEWSLETTERS, confidence = 0.9f,
                source = ClassificationSource.DETERMINISTIC, version = 1,
                explanation = "bulk sender", classifiedAtEpochMs = 2L,
            ),
        )
        dao.setClassification(
            ClassificationRecord(
                messageId = "msg-1", accountId = "acc-1",
                category = MailCategory.CAREER, confidence = 0.99f,
                source = ClassificationSource.USER_CORRECTION, version = 1,
                explanation = "user override", overridden = true, classifiedAtEpochMs = 3L,
            ),
        )

        val current = dao.getByMessage("msg-1")!!
        assertEquals(MailCategory.CAREER, current.category)
        assertEquals(ClassificationSource.USER_CORRECTION, current.source)
    }

    @Test
    fun `priority set replaces previous row`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message())

        val dao = db.priorityDao()
        dao.setPriority(
            PriorityRecord(
                messageId = "msg-1", accountId = "acc-1", priority = Priority.NORMAL,
                reason = "computed", version = 1, updatedAtEpochMs = 2L,
            ),
        )
        dao.setPriority(
            PriorityRecord(
                messageId = "msg-1", accountId = "acc-1", priority = Priority.CRITICAL,
                manualOverride = true, reason = "user", version = 1, updatedAtEpochMs = 3L,
            ),
        )

        val current = dao.getByMessage("msg-1")!!
        assertEquals(Priority.CRITICAL, current.priority)
        assertTrue(current.manualOverride)
    }

    @Test
    fun `company pinning orders filter list with pinned first`() = runTest {
        seedAccount()
        val dao = db.companyDao()
        dao.upsert(
            CompanyRecord("c1", "acc-1", "Zeta Corp", "zeta.test", emptyList(), null, false, 1L, 1L),
        )
        dao.upsert(
            CompanyRecord("c2", "acc-1", "Alpha Inc", "alpha.test", emptyList(), null, false, 1L, 1L),
        )
        dao.setPinned("c1", true)

        dao.observeFilterList("acc-1", 10).test {
            // Pinned first despite alphabetical order.
            assertEquals(listOf("Zeta Corp", "Alpha Inc"), awaitItem().map { it.canonicalName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `action items complete and dismiss`() = runTest {
        seedAccount()
        db.threadDao().upsert(thread())
        db.messageDao().upsert(message())

        val dao = db.actionItemDao()
        val id = dao.insert(
            ActionItemRecord(
                messageId = "msg-1", accountId = "acc-1",
                actionType = ActionType.REPLY_REQUIRED, confidence = 0.8f,
                explanation = "question asked", dueDateEpochMs = null, detectedAtEpochMs = 5L,
            ),
        )
        dao.observeOpenByAccount("acc-1", 10).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        dao.markCompleted(id)
        dao.observeOpenByAccount("acc-1", 10).test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `user correction latest wins`() = runTest {
        seedAccount()
        val dao = db.userCorrectionDao()
        dao.upsertCorrection(
            UserCorrectionRecord(
                accountId = "acc-1", scope = CorrectionScope.SENDER,
                scopeKey = "boss@example.test", field = CorrectionField.CATEGORY,
                value = "NEWSLETTERS", createdAtEpochMs = 1L,
            ),
        )
        dao.upsertCorrection(
            UserCorrectionRecord(
                accountId = "acc-1", scope = CorrectionScope.SENDER,
                scopeKey = "boss@example.test", field = CorrectionField.CATEGORY,
                value = "CAREER", createdAtEpochMs = 2L,
            ),
        )

        val current = dao.get("acc-1", CorrectionScope.SENDER, "boss@example.test", CorrectionField.CATEGORY)!!
        assertEquals("CAREER", current.value)
    }

    @Test
    fun `sync state upsert and observe`() = runTest {
        seedAccount()
        val dao = db.syncStateDao()
        dao.upsert(SyncStateRecord(accountId = "acc-1"))
        dao.updateAttempt("acc-1", SyncStatus.RUNNING, 10L, null)

        dao.observeByAccount("acc-1").test {
            val state = awaitItem()!!
            assertEquals(SyncStatus.RUNNING, state.status)
            assertEquals(10L, state.lastAttemptEpochMs)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
