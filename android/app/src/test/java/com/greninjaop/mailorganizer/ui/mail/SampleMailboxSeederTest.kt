package com.greninjaop.mailorganizer.ui.mail

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleMailboxSeederTest {

    private fun seeder(
        accounts: FakeAccountRepository = FakeAccountRepository(),
        mail: FakeMailRepository = FakeMailRepository(),
        clock: () -> Long = { 1_800_000_000_000L },
    ) = Triple(SampleMailboxSeeder(accounts, mail, clock), accounts, mail)

    @Test
    fun `seeds two fixture accounts and seven threads`() = runTest {
        val (s, accounts, mail) = seeder()
        assertTrue(s.seedIfEmpty())
        assertEquals(2, accounts.observeAllForTest().size)
        assertEquals(7, mail.threadCount())
        assertEquals(11, mail.messageCount())
    }

    @Test
    fun `all fixture ids carry the fixture prefix`() = runTest {
        val (s, accounts, mail) = seeder()
        s.seedIfEmpty()
        assertTrue(
            accounts.observeAllForTest()
                .all { it.accountId.startsWith(SampleMailboxSeeder.FIXTURE_ID_PREFIX) },
        )
        assertTrue(
            mail.allMessages().all {
                it.messageId.startsWith(SampleMailboxSeeder.FIXTURE_ID_PREFIX) &&
                    it.threadId.startsWith(SampleMailboxSeeder.FIXTURE_ID_PREFIX)
            },
        )
    }

    @Test
    fun `fixture rows were never synced`() = runTest {
        val (s, _, mail) = seeder()
        s.seedIfEmpty()
        assertTrue(mail.allMessages().all { it.gmailMessageId == null })
    }

    @Test
    fun `second seed is a no-op`() = runTest {
        val (s, _, mail) = seeder()
        assertTrue(s.seedIfEmpty())
        assertFalse(s.seedIfEmpty())
        assertEquals(7, mail.threadCount())
    }

    @Test
    fun `never touches a database that already has accounts`() = runTest {
        val accounts = FakeAccountRepository()
        val mail = FakeMailRepository()
        accounts.upsert(
            com.greninjaop.mailorganizer.data.local.AccountRecord(
                accountId = "real-acct",
                emailAddress = "me@gmail.com",
                displayName = "Me",
                createdAtEpochMs = 1L,
            ),
        )
        val (s) = seeder(accounts, mail)
        assertFalse(s.seedIfEmpty())
        assertEquals(0, mail.threadCount())
        assertEquals(1, accounts.observeAllForTest().size)
    }

    @Test
    fun `thread aggregates are correct`() = runTest {
        val (s, _, mail) = seeder()
        s.seedIfEmpty()
        // The 4-message Malayalam thread: 4 messages, 1 unread.
        val t4 = mail.allMessages().filter { it.threadId.endsWith("thread-t4") }
        assertEquals(4, t4.size)
        assertEquals(1, t4.count { it.unread })
    }

    @Test
    fun `accounts are isolated`() = runTest {
        val (s, _, mail) = seeder()
        s.seedIfEmpty()
        val acct2 = mail.allMessages().filter {
            it.accountId == SampleMailboxSeeder.ACCT_PRIYA
        }
        assertEquals(2, acct2.size)
        assertTrue(acct2.all { it.threadId.endsWith("thread-t7") })
        // No cross-account leakage in the other direction either.
        val acct1Threads = mail.allMessages()
            .filter { it.accountId == SampleMailboxSeeder.ACCT_ALEX }
            .map { it.threadId }.toSet()
        assertEquals(6, acct1Threads.size)
    }

    @Test
    fun `fixture html was sanitized and javascript link is inert`() = runTest {
        val (s, _, mail) = seeder()
        s.seedIfEmpty()
        val html = mail.allMessages()
            .first { it.messageId.endsWith("msg-m3") }
            .bodyHtml!!
        // Sanitizer ran at seed time: no script, no javascript: href.
        assertFalse(html.contains("<script"))
        assertFalse(html.lowercase().contains("javascript:"))
        // …and the renderer keeps it that way.
        val links = SafeHtmlRenderer.render(html)
            .filterIsInstance<BodyBlock.Paragraph>()
            .flatMap { it.runs }.mapNotNull { it.link }
        assertTrue(links.all { it.startsWith("https://") })
        assertTrue(links.any { it == "https://example.com/digest" })
    }

    @Test
    fun `starred fixture exists for the starred destination`() = runTest {
        val (s, _, mail) = seeder()
        s.seedIfEmpty()
        assertEquals(1, mail.allMessages().count { it.starred })
    }

    // Small helper: collect the accounts flow.
    private suspend fun FakeAccountRepository.observeAllForTest() =
        observeAll().first()
}
