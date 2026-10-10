package com.greninjaop.mailorganizer.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.greninjaop.mailorganizer.core.email.AttachmentMeta
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

        // ---- Open through Room: triggers MIGRATION_1_2 .. MIGRATION_9_10 + schema validation ----
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6,
                Migrations.MIGRATION_6_7,
                Migrations.MIGRATION_7_8,
                Migrations.MIGRATION_8_9,
                Migrations.MIGRATION_9_10,
            )
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

    @Test
    fun `v2 to v3 adds parser columns and preserves message data`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "migration-v2-v3-test.db")
        if (dbFile.exists()) dbFile.delete()

        // ---- Build a v2 database by running MIGRATION_1_2 on genuine v1 schema ----
        val raw = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        raw.execSQL(
            "CREATE TABLE accounts (" +
                "accountId TEXT NOT NULL PRIMARY KEY, " +
                "emailAddress TEXT NOT NULL, " +
                "displayName TEXT, " +
                "createdAtEpochMs INTEGER NOT NULL)",
        )
        applyMigration(Migrations.MIGRATION_1_2, raw)

        raw.execSQL(
            "INSERT INTO accounts (accountId, emailAddress, displayName, createdAtEpochMs) VALUES " +
                "('acc-1', 'a@example.test', NULL, 1)",
        )
        raw.execSQL(
            "INSERT INTO threads (threadId, gmailThreadId, accountId, subject, participantDisplayNames, messageCount, unreadCount, latestMessageId, latestMessageEpochMs, updatedAtEpochMs) VALUES " +
                "('thr-1', 'gt-1', 'acc-1', 'subj', 'Jane', 1, 1, 'msg-1', 5, 5)",
        )
        raw.execSQL(
            "INSERT INTO messages (messageId, gmailMessageId, threadId, accountId, fromAddress, fromName, toAddresses, ccAddresses, subject, snippet, bodyText, timestampEpochMs, unread, starred, labels, sizeBytes) VALUES (" +
                "'msg-1', 'm-1', 'thr-1', 'acc-1', 'j@example.test', 'Jane', " +
                "'me@example.test', '', 'Hello', 'snip', 'body', " +
                "1700000000000, 1, 0, 'INBOX', 100)",
        )
        raw.version = 2
        raw.close()

        // ---- Open through Room: triggers MIGRATION_2_3 .. MIGRATION_9_10 + schema validation ----
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6,
                Migrations.MIGRATION_6_7,
                Migrations.MIGRATION_7_8,
                Migrations.MIGRATION_8_9,
                Migrations.MIGRATION_9_10,
            )
            .allowMainThreadQueries()
            .build()

        try {
            // Existing row preserved; new columns default to null/empty.
            val msg = db.messageDao().getById("msg-1")!!
            assertEquals("Hello", msg.subject)
            assertEquals("body", msg.bodyText)
            assertEquals("j@example.test", msg.fromAddress)
            assertNull(msg.bodyHtml)
            assertEquals(emptyList<AttachmentMeta>(), msg.attachments)

            // New columns are writable through the current entity.
            db.messageDao().upsert(
                msg.copy(
                    bodyHtml = "<p>hi</p>",
                    attachments = listOf(
                        AttachmentMeta("a.pdf", "application/pdf", 10L, "att-1"),
                    ),
                ),
            )
            val updated = db.messageDao().getById("msg-1")!!
            assertEquals("<p>hi</p>", updated.bodyHtml)
            assertEquals(1, updated.attachments.size)
            assertEquals("a.pdf", updated.attachments[0].filename)
            assertEquals("att-1", updated.attachments[0].attachmentId)
        } finally {
            db.close()
            dbFile.delete()
        }
    }

    @Test
    fun `v3 to v4 adds company link and preserves message data`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "migration-v3-v4-test.db")
        if (dbFile.exists()) dbFile.delete()

        // ---- Build a v3 database by running MIGRATION_1_2 + MIGRATION_2_3 on genuine v1 schema ----
        val raw = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        raw.execSQL(
            "CREATE TABLE accounts (" +
                "accountId TEXT NOT NULL PRIMARY KEY, " +
                "emailAddress TEXT NOT NULL, " +
                "displayName TEXT, " +
                "createdAtEpochMs INTEGER NOT NULL)",
        )
        applyMigration(Migrations.MIGRATION_1_2, raw)
        applyMigration(Migrations.MIGRATION_2_3, raw)

        raw.execSQL(
            "INSERT INTO accounts (accountId, emailAddress, displayName, createdAtEpochMs) VALUES " +
                "('acc-1', 'a@example.test', NULL, 1)",
        )
        raw.execSQL(
            "INSERT INTO threads (threadId, gmailThreadId, accountId, subject, participantDisplayNames, messageCount, unreadCount, latestMessageId, latestMessageEpochMs, updatedAtEpochMs) VALUES " +
                "('thr-1', 'gt-1', 'acc-1', 'subj', 'Jane', 1, 1, 'msg-1', 5, 5)",
        )
        raw.execSQL(
            "INSERT INTO messages (messageId, gmailMessageId, threadId, accountId, fromAddress, fromName, toAddresses, ccAddresses, subject, snippet, bodyText, bodyHtml, attachments, timestampEpochMs, unread, starred, labels, sizeBytes) VALUES (" +
                "'msg-1', 'm-1', 'thr-1', 'acc-1', 'j@example.test', 'Jane', " +
                "'me@example.test', '', 'Hello', 'snip', 'body', " +
                "NULL, '', 1700000000000, 1, 0, 'INBOX', 100)",
        )
        raw.version = 3
        raw.close()

        // ---- Open through Room: triggers MIGRATION_3_4 .. MIGRATION_9_10 + schema validation ----
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
                Migrations.MIGRATION_5_6,
                Migrations.MIGRATION_6_7,
                Migrations.MIGRATION_7_8,
                Migrations.MIGRATION_8_9,
                Migrations.MIGRATION_9_10,
            )
            .allowMainThreadQueries()
            .build()

        try {
            // Existing row preserved; companyId defaults to null
            // ("not yet processed by company intelligence").
            val msg = db.messageDao().getById("msg-1")!!
            assertEquals("Hello", msg.subject)
            assertEquals("j@example.test", msg.fromAddress)
            assertNull(msg.companyId)

            // The link is writable through the current entity.
            db.messageDao().setCompanyId("msg-1", "co:example.test")
            assertEquals("co:example.test", db.messageDao().getById("msg-1")!!.companyId)

            // Clearing restores null.
            db.messageDao().setCompanyId("msg-1", null)
            assertNull(db.messageDao().getById("msg-1")!!.companyId)
        } finally {
            db.close()
            dbFile.delete()
        }
    }

    private fun applyMigration(migration: Migration, raw: SQLiteDatabase) {
        val proxy = java.lang.reflect.Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") {
                if (args == null || args.size == 1) {
                    raw.execSQL(args?.get(0) as String)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    raw.execSQL(args[0] as String, args[1] as Array<out Any>)
                }
                null
            } else {
                null
            }
        } as SupportSQLiteDatabase
        migration.migrate(proxy)
    }
}
