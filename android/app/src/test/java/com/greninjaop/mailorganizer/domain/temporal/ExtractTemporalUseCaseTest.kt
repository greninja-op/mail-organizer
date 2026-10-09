package com.greninjaop.mailorganizer.domain.temporal

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.temporal.TemporalConfidence
import com.greninjaop.mailorganizer.core.temporal.TemporalItemType
import com.greninjaop.mailorganizer.core.temporal.TemporalStatus
import com.greninjaop.mailorganizer.core.temporal.TimezoneSource
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Temporal payload codec tests (Phase 13).
 *
 * The codec is total in both directions and versioned; malformed JSON
 * degrades to null, never throws.
 */
class TemporalPayloadJsonTest {

    private fun record() = ExtractedItemRecord(
        id = 1L,
        messageId = "m1",
        accountId = "a1",
        itemType = ExtractedItemType.DEADLINE,
        title = "Deadline",
        payload = null,
        dueDateEpochMs = 1_791_000_000_000L,
        detectedAtEpochMs = 1_790_000_000_000L,
        completed = false,
    )

    private fun temporal() = com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal(
        type = TemporalItemType.DEADLINE,
        title = "Deadline",
        startEpochMs = 1_791_000_000_000L,
        endEpochMs = null,
        isDateOnly = true,
        timezoneId = "UTC",
        timezoneSource = TimezoneSource.APP_FALLBACK,
        location = null,
        meetingUrl = null,
        status = TemporalStatus.UPCOMING,
        confidence = TemporalConfidence.HIGH,
        signals = listOf(
            com.greninjaop.mailorganizer.core.temporal.TemporalSignal(
                "deadline_vocabulary",
                "deadline language appears near the date",
            ),
        ),
        version = 1,
        extractedAtEpochMs = 1_790_000_000_000L,
        explanation = "Deadline detected because:\n• deadline language appears near the date",
    )

    @Test
    fun `round trip preserves all fields`() {
        val json = TemporalPayloadJson.encode(temporal())
        val decoded = TemporalPayloadJson.decode(json)
        assertNotNull(decoded)
        assertEquals(null, decoded!!.endEpochMs)
        assertEquals(true, decoded.isDateOnly)
        assertEquals("UTC", decoded.timezoneId)
        assertEquals(TimezoneSource.APP_FALLBACK, decoded.timezoneSource)
        assertEquals(TemporalStatus.UPCOMING, decoded.status)
        assertEquals(TemporalConfidence.HIGH, decoded.confidence)
        assertEquals(listOf("deadline_vocabulary"), decoded.signalNames)
        assertEquals(1, decoded.version)
        assertTrue(decoded.explanation.contains("deadline language"))
    }

    @Test
    fun `escapes survive round trip`() {
        val item = temporal().copy(
            location = "Room \"A\"\nBuilding 1",
            explanation = "line1\nline2 \"quoted\"",
        )
        val decoded = TemporalPayloadJson.decode(TemporalPayloadJson.encode(item))
        assertNotNull(decoded)
        assertEquals("Room \"A\"\nBuilding 1", decoded!!.location)
        assertTrue(decoded.explanation.contains("\"quoted\""))
    }

    @Test
    fun `malformed JSON decodes to null`() {
        assertNull(TemporalPayloadJson.decode("{not json"))
        assertNull(TemporalPayloadJson.decode(""))
        assertNull(TemporalPayloadJson.decode(null))
    }

    @Test
    fun `record round trip`() {
        val item = temporal()
        val stored = record().copy(payload = TemporalPayloadJson.encode(item))
        val back = stored.toExtractedTemporal()
        assertNotNull(back)
        assertEquals(TemporalItemType.DEADLINE, back!!.type)
        assertEquals(item.startEpochMs, back.startEpochMs)
        assertEquals(item.explanation, back.explanation)
    }

    @Test
    fun `type mapping covers all temporal types`() {
        TemporalItemType.entries.forEach { core ->
            val storage = core.toExtractedItemType()
            // Round-trip through storage must preserve the type.
            val back = storage.toTemporalItemType()
            assertEquals(core, back)
        }
    }
}

/**
 * ExtractTemporalUseCase tests (Phase 13).
 *
 * Verifies: extraction + persistence, idempotency, account isolation,
 * failure safety, and that user-completed rows survive re-extraction.
 */
class ExtractTemporalUseCaseTest {

    private val dispatchers = AppDispatchers()
    private val ref = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()

    private fun message(
        id: String = "m1",
        accountId: String = "a1",
        subject: String = "Assignment",
        body: String = "The final deadline is 18 October 2026.",
    ) = MessageRecord(
        messageId = id,
        threadId = "t1",
        accountId = accountId,
        fromAddress = "prof@university.edu",
        fromName = "Prof",
        subject = subject,
        snippet = null,
        bodyText = body,
        bodyHtml = null,
        timestampEpochMs = ref,
        unread = true,
        starred = false,
        labels = emptyList(),
        gmailMessageId = "g1",
    )

    private class FakeMail(private val messages: Map<String, MessageRecord>) : MailRepository {
        override suspend fun saveThreadWithMessages(
            thread: com.greninjaop.mailorganizer.data.local.ThreadRecord,
            messages: List<MessageRecord>,
        ) = Unit
        override fun observeThreads(a: String, limit: Int) =
            MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.ThreadRecord>())
        override fun observeMessages(t: String, limit: Int) =
            MutableStateFlow(emptyList<MessageRecord>())
        override fun observeUnread(a: String, limit: Int) =
            MutableStateFlow(emptyList<MessageRecord>())
        override fun observeStarred(a: String, limit: Int) =
            MutableStateFlow(emptyList<MessageRecord>())
        override suspend fun searchByText(a: String, q: String, limit: Int) =
            emptyList<MessageRecord>()
        override suspend fun setRead(m: String, r: Boolean) = Unit
        override suspend fun setStarred(m: String, s: Boolean) = Unit
        override suspend fun countByAccount(a: String) = messages.size
        override suspend fun getMessage(messageId: String) = messages[messageId]
        override suspend fun getMessagesByIds(messageIds: List<String>) =
            messageIds.mapNotNull { messages[it] }
        override suspend fun getThreadByGmailId(a: String, g: String) = null
        override suspend fun updateThreadAggregates(t: String) = Unit
        override suspend fun existingGmailIds(a: String, g: List<String>) = emptySet<String>()
        override suspend fun deleteMessagesByGmailIds(a: String, g: List<String>) =
            emptyList<String>()
        override suspend fun getUnclassifiedMessages(a: String, l: Int) =
            emptyList<MessageRecord>()
        override suspend fun getUnprioritizedMessages(a: String, l: Int) =
            emptyList<MessageRecord>()
        override suspend fun getUnextractedMessages(a: String, l: Int) =
            emptyList<MessageRecord>()
        override fun observeByLabel(a: String, l: String, limit: Int) =
            MutableStateFlow(emptyList<MessageRecord>())
        override suspend fun setMessageCompanyId(m: String, c: String?) = Unit
        override suspend fun getMessagesWithoutCompany(a: String, l: Int) =
            emptyList<MessageRecord>()
        override fun observeMessagesByCompany(a: String, c: String, limit: Int) =
            MutableStateFlow(emptyList<MessageRecord>())
        override fun observeMessagesByCompanyAndCategory(
            a: String,
            c: com.greninjaop.mailorganizer.data.local.MailCategory,
            co: String,
            limit: Int,
        ) = MutableStateFlow(emptyList<MessageRecord>())
        override fun observeMessagesByCompanyAndLabel(
            a: String,
            co: String,
            l: String,
            limit: Int,
        ) = MutableStateFlow(emptyList<MessageRecord>())
        override suspend fun companyCountsForCategory(
            a: String,
            c: com.greninjaop.mailorganizer.data.local.MailCategory,
        ) = emptyMap<String, Int>()
        override suspend fun companyCountsForLabel(a: String, l: String) =
            emptyMap<String, Int>()
        override suspend fun companyMessageCounts(a: String) = emptyMap<String, Int>()
        override suspend fun getMessageIdsByAccount(a: String, l: Int) = emptyList<String>()
        override suspend fun getMessageIdsBySender(a: String, e: String, l: Int) =
            emptyList<String>()
        override suspend fun getMessageIdsByDomain(a: String, d: String, l: Int) =
            emptyList<String>()
        override suspend fun getMessageIdsByCompany(a: String, c: String, l: Int) =
            emptyList<String>()
    }

    private class FakeIntelligence : IntelligenceRepository {
        val items = mutableListOf<ExtractedItemRecord>()
        var nextId = 1L

        override suspend fun addExtractedItem(item: ExtractedItemRecord): Long {
            val id = nextId++
            items += item.copy(id = id)
            return id
        }

        override suspend fun getExtractedItems(messageId: String) =
            items.filter { it.messageId == messageId }

        override suspend fun deleteExtractedItems(ids: List<Long>) {
            items.removeIf { it.id in ids }
        }

        override fun observeOpenExtracted(
            accountId: String,
            type: ExtractedItemType,
            limit: Int,
        ): Flow<List<ExtractedItemRecord>> = MutableStateFlow(emptyList())

        // Unused surface — minimal stubs.
        override suspend fun upsertSender(s: com.greninjaop.mailorganizer.data.local.SenderRecord) = Unit
        override fun observeTopSenders(a: String, l: Int) = MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.SenderRecord>())
        override suspend fun getSenderByEmail(a: String, n: String) = null
        override suspend fun recordSenderMessage(a: String, e: String, n: String, d: String?, dom: String) =
            com.greninjaop.mailorganizer.data.local.SenderRecord("s", a, e, n, d, dom, 0L, 0L, 1)
        override suspend fun upsertCompany(c: com.greninjaop.mailorganizer.data.local.CompanyRecord) = Unit
        override fun observeCompanyFilterList(a: String, l: Int) = MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.CompanyRecord>())
        override suspend fun setCompanyPinned(c: String, p: Boolean) = Unit
        override suspend fun getCompanyByDomain(a: String, d: String) = null
        override suspend fun setClassification(r: com.greninjaop.mailorganizer.data.local.ClassificationRecord) = Unit
        override suspend fun getClassification(m: String) = null
        override suspend fun deleteClassification(m: String) = Unit
        override suspend fun getClassifications(ids: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.ClassificationRecord>()
        override fun observeByCategory(a: String, c: com.greninjaop.mailorganizer.data.local.MailCategory, l: Int) =
            MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.ClassificationRecord>())
        override suspend fun categoryCounts(a: String) = emptyMap<com.greninjaop.mailorganizer.data.local.MailCategory, Int>()
        override suspend fun setPriority(r: com.greninjaop.mailorganizer.data.local.PriorityRecord) = Unit
        override suspend fun getPriority(m: String) = null
        override suspend fun deletePriority(m: String) = Unit
        override suspend fun getPriorities(ids: List<String>) = emptyMap<String, com.greninjaop.mailorganizer.data.local.PriorityRecord>()
        override fun observeByPriority(a: String, p: com.greninjaop.mailorganizer.data.local.Priority, l: Int) =
            MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.PriorityRecord>())
        override suspend fun addActionItem(i: com.greninjaop.mailorganizer.data.local.ActionItemRecord) = 0L
        override fun observeOpenActionItems(a: String, l: Int) = MutableStateFlow(emptyList<com.greninjaop.mailorganizer.data.local.ActionItemRecord>())
        override suspend fun completeActionItem(id: Long) = Unit
        override suspend fun dismissActionItem(id: Long) = Unit

        override suspend fun getActionItem(id: Long): com.greninjaop.mailorganizer.data.local.ActionItemRecord? = null
        override suspend fun getActionItemsByMessage(messageId: String): List<com.greninjaop.mailorganizer.data.local.ActionItemRecord> = emptyList()
        override suspend fun getOpenActionItemsByThread(threadId: String): List<com.greninjaop.mailorganizer.data.local.ActionItemRecord> = emptyList()
        override suspend fun setActionItemStatus(id: Long, status: com.greninjaop.mailorganizer.core.actions.ActionStatus) = Unit
        override suspend fun deleteActionItems(ids: List<Long>) = Unit
        override suspend fun expireOverdueActionItems(accountId: String, cutoffEpochMs: Long): Int = 0
    }

    private fun useCase(
        messages: Map<String, MessageRecord>,
        intel: FakeIntelligence = FakeIntelligence(),
    ) = ExtractTemporalUseCase(
        mail = FakeMail(messages),
        intelligence = intel,
        dispatchers = dispatchers,
        clock = { 1_800_000_000_000L },
    ) to intel

    @Test
    fun `extracts and persists deadline`() = runTest {
        val (uc, intel) = useCase(mapOf("m1" to message()))
        val result = uc.extract("m1", "a1")
        assertNotNull(result)
        // Subject "Assignment" + "deadline" → SUBMISSION_DEADLINE by precedence.
        assertTrue(result!!.items.any { it.type == TemporalItemType.SUBMISSION_DEADLINE })
        assertEquals(1, intel.items.size)
        assertEquals(ExtractedItemType.SUBMISSION_DEADLINE, intel.items[0].itemType)
        assertEquals("a1", intel.items[0].accountId)
    }

    @Test
    fun `second run is idempotent`() = runTest {
        val (uc, intel) = useCase(mapOf("m1" to message()))
        uc.extract("m1", "a1")
        val second = uc.extract("m1", "a1")
        assertNull(second) // skipped — already current
        assertEquals(1, intel.items.size)
    }

    @Test
    fun `force re-extracts`() = runTest {
        val (uc, intel) = useCase(mapOf("m1" to message()))
        uc.extract("m1", "a1")
        val forced = uc.extract("m1", "a1", force = true)
        assertNotNull(forced)
        assertEquals(1, intel.items.size) // replaced, not duplicated
    }

    @Test
    fun `completed rows survive re-extraction`() = runTest {
        val (uc, intel) = useCase(mapOf("m1" to message()))
        uc.extract("m1", "a1")
        intel.items[0] = intel.items[0].copy(completed = true)
        uc.extract("m1", "a1", force = true)
        assertEquals(1, intel.items.size)
        assertTrue(intel.items[0].completed)
    }

    @Test
    fun `wrong account is rejected`() = runTest {
        val (uc, intel) = useCase(mapOf("m1" to message(accountId = "a1")))
        val result = uc.extract("m1", "a2")
        assertNull(result)
        assertTrue(intel.items.isEmpty())
    }

    @Test
    fun `missing message returns null`() = runTest {
        val (uc, _) = useCase(emptyMap())
        assertNull(uc.extract("nope", "a1"))
    }

    @Test
    fun `message with no temporal content extracts nothing but succeeds`() = runTest {
        val (uc, intel) = useCase(mapOf("m1" to message(body = "Hello, how are you?")))
        val result = uc.extract("m1", "a1")
        assertNotNull(result)
        assertTrue(result!!.items.isEmpty())
        assertTrue(intel.items.isEmpty())
    }
}
