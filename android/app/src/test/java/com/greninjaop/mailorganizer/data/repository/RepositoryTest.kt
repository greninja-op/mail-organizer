package com.greninjaop.mailorganizer.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.AppDatabase
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.CorrectionField
import com.greninjaop.mailorganizer.data.local.CorrectionScope
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.SyncStatus
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.local.UserCorrectionRecord
import com.greninjaop.mailorganizer.data.local.UserRuleRecord
import com.greninjaop.mailorganizer.data.local.RuleType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Repository-layer tests (Phase 2): proves the UI-facing interfaces work
 * end to end over a real (in-memory) database, including the transactional
 * thread+message save the Phase 4 sync engine will reuse.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var mail: MailRepository
    private lateinit var intelligence: IntelligenceRepository
    private lateinit var rules: RuleRepository
    private lateinit var sync: SyncStateRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dispatchers = AppDispatchers()
        accounts = RoomAccountRepository(db.accountDao(), dispatchers)
        mail = RoomMailRepository(db, dispatchers)
        intelligence = RoomIntelligenceRepository(db, dispatchers)
        rules = RoomRuleRepository(db, dispatchers)
        sync = RoomSyncStateRepository(db, dispatchers)
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedAccount(id: String = "acc-1") {
        accounts.upsert(AccountRecord(id, "$id@example.test", null, 1L))
    }

    @Test
    fun `account lifecycle through repository`() = runTest {
        seedAccount()
        accounts.updateConnectionState("acc-1", ConnectionState.CONNECTED)
        accounts.recordSync("acc-1", 42L)

        val loaded = accounts.getById("acc-1")!!
        assertEquals(ConnectionState.CONNECTED, loaded.connectionState)
        assertEquals(42L, loaded.lastSyncEpochMs)

        accounts.setEnabled("acc-1", false)
        accounts.observeEnabled().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `thread with messages saves atomically`() = runTest {
        seedAccount()
        val thread = ThreadRecord(
            threadId = "thr-1", gmailThreadId = "g1", accountId = "acc-1",
            subject = "Atomic", latestMessageEpochMs = 5L, updatedAtEpochMs = 5L,
        )
        val messages = listOf(
            MessageRecord(
                messageId = "m1", gmailMessageId = "gm1", threadId = "thr-1",
                accountId = "acc-1", fromAddress = "a@example.test", fromName = null,
                subject = "Atomic", snippet = null, bodyText = null, timestampEpochMs = 5L,
            ),
            MessageRecord(
                messageId = "m2", gmailMessageId = "gm2", threadId = "thr-1",
                accountId = "acc-1", fromAddress = "a@example.test", fromName = null,
                subject = "Atomic", snippet = null, bodyText = null, timestampEpochMs = 6L,
            ),
        )
        mail.saveThreadWithMessages(thread, messages)

        mail.observeMessages("thr-1", 10).test {
            assertEquals(listOf("m1", "m2"), awaitItem().map { it.messageId })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, mail.countByAccount("acc-1"))
    }

    @Test
    fun `intelligence write and read through repository`() = runTest {
        seedAccount()
        val thread = ThreadRecord(
            threadId = "thr-1", gmailThreadId = "g1", accountId = "acc-1",
            subject = "Intel", latestMessageEpochMs = 5L, updatedAtEpochMs = 5L,
        )
        mail.saveThreadWithMessages(
            thread,
            listOf(
                MessageRecord(
                    messageId = "m1", gmailMessageId = "gm1", threadId = "thr-1",
                    accountId = "acc-1", fromAddress = "a@example.test", fromName = null,
                    subject = "Intel", snippet = null, bodyText = null, timestampEpochMs = 5L,
                ),
            ),
        )

        val repo = intelligence as RoomIntelligenceRepository
        repo.classifyDeterministic("m1", "acc-1", MailCategory.CAREER, 0.95f, 1, "test")
        repo.prioritize("m1", "acc-1", Priority.HIGH, false, "test", 1)
        val actionId = repo.detectAction(
            "m1", "acc-1", ActionType.REPLY_REQUIRED, 0.9f, "question", null,
        )

        assertEquals(MailCategory.CAREER, intelligence.getClassification("m1")?.category)
        assertEquals(Priority.HIGH, intelligence.getPriority("m1")?.priority)
        intelligence.observeOpenActionItems("acc-1", 10).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        intelligence.completeActionItem(actionId)
        intelligence.observeOpenActionItems("acc-1", 10).test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        intelligence.addExtractedItem(
            ExtractedItemRecord(
                messageId = "m1", accountId = "acc-1",
                itemType = ExtractedItemType.DEADLINE, title = "File taxes",
                payload = null, dueDateEpochMs = 999L, detectedAtEpochMs = 5L,
            ),
        )
        intelligence.observeOpenExtracted("acc-1", ExtractedItemType.DEADLINE, 10).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rules and corrections through repository`() = runTest {
        seedAccount()
        val ruleId = rules.addRule(
            UserRuleRecord(
                accountId = "acc-1", ruleType = RuleType.DOMAIN_TO_CATEGORY,
                matcher = "example.test", targetCategory = MailCategory.NEWSLETTERS,
                targetPriority = null, targetActionRequired = null,
                createdAtEpochMs = 1L, updatedAtEpochMs = 1L,
            ),
        )
        rules.observeEnabledRules("acc-1").test {
            assertEquals(listOf(ruleId), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        rules.recordCorrection(
            UserCorrectionRecord(
                accountId = "acc-1", scope = CorrectionScope.DOMAIN,
                scopeKey = "example.test", field = CorrectionField.CATEGORY,
                value = "CAREER", createdAtEpochMs = 2L,
            ),
        )
        val correction = rules.getCorrection(
            "acc-1", CorrectionScope.DOMAIN, "example.test", CorrectionField.CATEGORY,
        )
        assertNotNull(correction)
        assertEquals("CAREER", correction!!.value)
    }

    @Test
    fun `sync state lifecycle through repository`() = runTest {
        seedAccount()
        val initial = sync.ensureForAccount("acc-1")
        assertEquals(SyncStatus.NEVER_SYNCED, initial.status)

        sync.markAttempt("acc-1", SyncStatus.RUNNING)
        sync.observe("acc-1").test {
            assertEquals(SyncStatus.RUNNING, awaitItem()?.status)
            cancelAndIgnoreRemainingEvents()
        }

        sync.markSuccess("acc-1", "cursor-123")
        sync.observe("acc-1").test {
            val state = awaitItem()!!
            assertEquals(SyncStatus.IDLE, state.status)
            assertEquals("cursor-123", state.cursor)
            assertNotNull(state.lastSuccessfulSyncEpochMs)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
