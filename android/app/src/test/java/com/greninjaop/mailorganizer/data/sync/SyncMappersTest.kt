package com.greninjaop.mailorganizer.data.sync

import com.greninjaop.mailorganizer.core.AccountId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Normalizer tests (Phase 4, phase §44): remote → local mapping rules. */
class SyncMappersTest {

    private val account = AccountId("acc-1")

    @Test
    fun `message maps with stable namespaced ids`() {
        val record = fakeRemoteMessage(id = "m-9", threadId = "t-3").toMessageRecord(account)
        assertEquals("acc-1:m-9", record.messageId)
        assertEquals("m-9", record.gmailMessageId)
        assertEquals("acc-1:t-3", record.threadId)
        assertEquals("acc-1", record.accountId)
    }

    @Test
    fun `same gmail id always maps to same local id (idempotency key)`() {
        val a = fakeRemoteMessage(id = "m-1").toMessageRecord(account)
        val b = fakeRemoteMessage(id = "m-1").toMessageRecord(account)
        assertEquals(a.messageId, b.messageId)
    }

    @Test
    fun `gmail ids are namespaced per account`() {
        val other = AccountId("acc-2")
        val a = fakeRemoteMessage(id = "m-1").toMessageRecord(account)
        val b = fakeRemoteMessage(id = "m-1").toMessageRecord(other)
        assertTrue(a.messageId != b.messageId)
        assertEquals("acc-2", b.accountId)
    }

    @Test
    fun `gmail metadata preserved`() {
        val remote = fakeRemoteMessage(
            id = "m-2",
            unread = false,
            labels = listOf("INBOX", "CATEGORY_PROMOTIONS", "STARRED"),
        ).copy(starred = true)
        val record = remote.toMessageRecord(account)
        assertEquals(false, record.unread)
        assertEquals(true, record.starred)
        assertEquals(listOf("INBOX", "CATEGORY_PROMOTIONS", "STARRED"), record.labels)
        assertEquals("snippet m-2", record.snippet)
    }

    @Test
    fun `thread record aggregates one page of messages`() {
        val records = listOf(
            fakeRemoteMessage(id = "m-1", threadId = "t-9", unread = true).copy(
                timestampEpochMs = 1_700_000_000_000L,
            ).toMessageRecord(account),
            fakeRemoteMessage(id = "m-2", threadId = "t-9", unread = false).copy(
                timestampEpochMs = 1_700_000_001_000L,
            ).toMessageRecord(account),
        )
        val thread = buildThreadRecord(account, "t-9", records, nowMs = 42L)
        assertEquals("acc-1:t-9", thread.threadId)
        assertEquals("t-9", thread.gmailThreadId)
        assertEquals("acc-1", thread.accountId)
        assertEquals(2, thread.messageCount)
        assertEquals(1, thread.unreadCount)
        assertEquals("acc-1:m-2", thread.latestMessageId)
        assertEquals(42L, thread.updatedAtEpochMs)
    }

    @Test
    fun `thread requires at least one message`() {
        try {
            buildThreadRecord(account, "t-9", emptyList(), 42L)
            error("should have thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `null body stays null (minimal representation)`() {
        val record = fakeRemoteMessage(id = "m-3").toMessageRecord(account)
        assertNull(record.bodyText)
    }
}
