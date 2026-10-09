package com.greninjaop.mailorganizer.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration test: v1 (Phase 0 accounts seed) -> v2 (Phase 2 mailbox schema).
 *
 * Builds a genuine v1 database with raw SQLite, then opens it through Room
 * with [Migrations.MIGRATION_1_2]. Room validates the migrated schema
 * against the expected v2 schema and throws on any mismatch — so this test
 * proves both data preservation and schema exactness.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    @Test
    fun `v1 to v2 preserves accounts and creates mailbox schema`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "migration-v1-v2-test.db")
        if (dbFile.exists()) dbFile.delete()

        // ---- Build a v1 database exactly as Phase 0 defined it ----
        val raw = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        raw.execSQL(
            "CREATE TABLE accounts (" +
                "accountId TEXT NOT NULL PRIMARY KEY, " +
                "emailAddress TEXT NOT NULL, " +
                "displayName TEXT, " +
                "createdAtEpochMs INTEGER NOT NULL)",
        )
        raw.execSQL(
            "INSERT INTO accounts VALUES ('acc-1', 'a@example.test', 'Account A', 123)",
        )
        raw.execSQL("INSERT INTO accounts VALUES ('acc-2', 'b@example.test', NULL, 456)")
        raw.version = 1
        raw.close()

        // ---- Open through Room: triggers MIGRATION_1_2 + schema validation ----
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(Migrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            // Old rows preserved with safe migration defaults.
            val acc1 = db.accountDao().getById("acc-1")!!
            assertEquals("a@example.test", acc1.emailAddress)
            assertEquals("Account A", acc1.displayName)
            assertEquals(123L, acc1.createdAtEpochMs)
            assertNull(acc1.googleAccountId)
            assertEquals(AccountRecord.PROVIDER_GOOGLE, acc1.provider)
            assertEquals(ConnectionState.DISCONNECTED, acc1.connectionState)
            assertNull(acc1.lastSyncEpochMs)
            assertTrue(acc1.isEnabled)

            val acc2 = db.accountDao().getById("acc-2")!!
            assertEquals("b@example.test", acc2.emailAddress)
            assertNull(acc2.displayName)

            // New tables exist and are writable (FK chain works post-migration).
            db.threadDao().upsert(
                ThreadRecord(
                    threadId = "thr-1", gmailThreadId = null, accountId = "acc-1",
                    subject = "post-migration", latestMessageEpochMs = 9L, updatedAtEpochMs = 9L,
                ),
            )
            db.messageDao().upsert(
                MessageRecord(
                    messageId = "msg-1", gmailMessageId = null, threadId = "thr-1",
                    accountId = "acc-1", fromAddress = "x@example.test", fromName = null,
                    subject = "post-migration", snippet = null, bodyText = null,
                    timestampEpochMs = 9L,
                ),
            )
            assertEquals("post-migration", db.messageDao().getById("msg-1")?.subject)
            assertEquals(1, db.messageDao().countByAccount("acc-1"))

            // New tables start empty for migrated accounts.
            assertEquals(0, db.messageDao().countByAccount("acc-2"))
        } finally {
            db.close()
            dbFile.delete()
        }
    }
}
