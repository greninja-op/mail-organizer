package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.core.email.EmailAddress
import com.greninjaop.mailorganizer.core.email.EmailMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 5 mapper tests: parsed [EmailMessage] → [MessageRecord]. */
class EmailMappersTest {

    private val account = AccountId("acc-1")

    private fun emailMessage() = EmailMessage(
        gmailId = "m-7",
        gmailThreadId = "t-2",
        from = EmailAddress("Jane", "jane@example.test"),
        to = listOf(EmailAddress(null, "me@example.test")),
        cc = listOf(EmailAddress("Boss", "boss@example.test")),
        subject = "Subject!",
        dateEpochMs = 1_700_000_000_000L,
        snippet = "snip",
        bodyText = "plain",
        bodyHtml = "<p>html</p>",
        attachments = listOf(AttachmentMeta("a.pdf", "application/pdf", 42L, "att-1")),
        labelIds = listOf("INBOX", "UNREAD", "STARRED"),
        sizeBytes = 999L,
    )

    @Test
    fun `parsed message maps with stable namespaced ids`() {
        val record = emailMessage().toMessageRecord(account)
        assertEquals("acc-1:m-7", record.messageId)
        assertEquals("m-7", record.gmailMessageId)
        assertEquals("acc-1:t-2", record.threadId)
        assertEquals("acc-1", record.accountId)
    }

    @Test
    fun `all parsed fields transfer to the record`() {
        val record = emailMessage().toMessageRecord(account)
        assertEquals("jane@example.test", record.fromAddress)
        assertEquals("Jane", record.fromName)
        assertEquals(listOf("me@example.test"), record.toAddresses)
        assertEquals(listOf("boss@example.test"), record.ccAddresses)
        assertEquals("Subject!", record.subject)
        assertEquals("snip", record.snippet)
        assertEquals("plain", record.bodyText)
        assertEquals("<p>html</p>", record.bodyHtml)
        assertEquals(1_700_000_000_000L, record.timestampEpochMs)
        assertEquals(999L, record.sizeBytes)
        assertEquals(listOf("INBOX", "UNREAD", "STARRED"), record.labels)
    }

    @Test
    fun `attachments survive the mapping`() {
        val record = emailMessage().toMessageRecord(account)
        assertEquals(1, record.attachments.size)
        assertEquals("a.pdf", record.attachments[0].filename)
        assertEquals("att-1", record.attachments[0].attachmentId)
    }

    @Test
    fun `gmail unread and starred labels map to flags`() {
        val record = emailMessage().toMessageRecord(account)
        assertTrue(record.unread)
        assertTrue(record.starred)

        val read = emailMessage().copy(labelIds = listOf("INBOX")).toMessageRecord(account)
        assertFalse(read.unread)
        assertFalse(read.starred)
    }

    @Test
    fun `ids are namespaced per account`() {
        val other = AccountId("acc-2")
        val a = emailMessage().toMessageRecord(account)
        val b = emailMessage().toMessageRecord(other)
        assertTrue(a.messageId != b.messageId)
        assertEquals("acc-2", b.accountId)
    }

    @Test
    fun `same gmail id maps to same local id (idempotent with sync engine)`() {
        val a = emailMessage().toMessageRecord(account)
        val b = emailMessage().toMessageRecord(account)
        assertEquals(a.messageId, b.messageId)
        assertEquals(a.threadId, b.threadId)
    }

    @Test
    fun `missing from degrades to empty address`() {
        val record = emailMessage().copy(from = null).toMessageRecord(account)
        assertEquals("", record.fromAddress)
        assertEquals(null, record.fromName)
    }
}
