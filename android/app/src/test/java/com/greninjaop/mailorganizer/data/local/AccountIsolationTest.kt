package com.greninjaop.mailorganizer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MANDATORY security test (phase §37): data belonging to Account A must
 * never appear in Account B queries — across every account-owned table.
 * This is a security requirement, not merely a correctness test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountIsolationTest {

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

    private suspend fun seedTwoAccountsWithMail() {
        for (account in listOf("acc-a", "acc-b")) {
            db.accountDao().upsert(AccountRecord(account, "$account@example.test", null, 1L))
            val threadId = "thr-$account"
            db.threadDao().upsert(
                ThreadRecord(
                    threadId = threadId, gmailThreadId = "g-$threadId", accountId = account,
                    subject = "Subject $account", latestMessageEpochMs = 1L, updatedAtEpochMs = 1L,
                ),
            )
            db.messageDao().upsert(
                MessageRecord(
                    messageId = "msg-$account", gmailMessageId = "gm-$account",
                    threadId = threadId, accountId = account,
                    fromAddress = "sender@example.test", fromName = null,
                    subject = "Hello $account", snippet = null, bodyText = null,
                    timestampEpochMs = 1L,
                ),
            )
            db.senderDao().upsert(
                SenderRecord(
                    senderId = "snd-$account", accountId = account,
                    emailAddress = "sender@example.test", normalizedEmail = "sender@example.test",
                    displayName = null, domain = "example.test",
                    firstSeenEpochMs = 1L, lastSeenEpochMs = 1L,
                ),
            )
            db.companyDao().upsert(
                CompanyRecord(
                    companyId = "co-$account", accountId = account,
                    canonicalName = "Example $account", normalizedDomain = "example.test",
                    createdAtEpochMs = 1L, updatedAtEpochMs = 1L,
                ),
            )
            db.classificationDao().setClassification(
                ClassificationRecord(
                    messageId = "msg-$account", accountId = account,
                    category = MailCategory.IMPORTANT, confidence = 1f,
                    source = ClassificationSource.DETERMINISTIC, version = 1,
                    explanation = null, classifiedAtEpochMs = 1L,
                ),
            )
        }
    }

    @Test
    fun `account A queries never return account B data`() = runTest {
        seedTwoAccountsWithMail()

        val threadsA = db.threadDao().getByAccount("acc-a", 10)
        assertEquals(listOf("thr-acc-a"), threadsA.map { it.threadId })

        val hitsA = db.messageDao().searchByText("acc-a", "Hello acc-a", 10)
        assertEquals(listOf("msg-acc-a"), hitsA.map { it.messageId })
        val leakB = db.messageDao().searchByText("acc-a", "Hello acc-b", 10)
        assertTrue("account B message leaked into account A query", leakB.isEmpty())

        val senderA = db.senderDao().getByEmail("acc-a", "sender@example.test")
        assertEquals("snd-acc-a", senderA?.senderId)

        val classA = db.classificationDao().getByMessage("msg-acc-a")
        assertEquals("acc-a", classA?.accountId)
    }

    @Test
    fun `deleting account A cascades and leaves account B intact`() = runTest {
        seedTwoAccountsWithMail()

        db.accountDao().deleteById("acc-a")

        assertEquals(null, db.accountDao().getById("acc-a"))
        assertEquals(0, db.threadDao().getByAccount("acc-a", 10).size)
        assertEquals(0, db.messageDao().countByAccount("acc-a"))
        assertEquals(null, db.classificationDao().getByMessage("msg-acc-a"))

        // Account B untouched.
        assertEquals("acc-b@example.test", db.accountDao().getById("acc-b")?.emailAddress)
        assertEquals(1, db.threadDao().getByAccount("acc-b", 10).size)
        assertEquals(1, db.messageDao().countByAccount("acc-b"))
    }
}
