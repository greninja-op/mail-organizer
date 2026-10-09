package com.greninjaop.mailorganizer.domain.company

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.ui.mail.FakeIntelligenceRepository
import com.greninjaop.mailorganizer.ui.mail.FakeMailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [CompanyIntelligenceUseCase] (Phase 8).
 *
 * All fixtures are synthetic — no real user mail.
 */
class CompanyIntelligenceUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mail: FakeMailRepository
    private lateinit var intelligence: FakeIntelligenceRepository
    private lateinit var useCase: CompanyIntelligenceUseCase

    private fun dispatchers() = AppDispatchers(
        io = testDispatcher,
        default = testDispatcher,
        main = testDispatcher,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mail = FakeMailRepository()
        intelligence = FakeIntelligenceRepository()
        useCase = CompanyIntelligenceUseCase(
            mail = mail,
            intelligence = intelligence,
            dispatchers = dispatchers(),
            clock = { 1_800_000_000_000L },
        )
    }

    private fun message(
        id: String,
        from: String,
        name: String? = null,
        accountId: String = "a1",
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = null,
        threadId = "t-$id",
        accountId = accountId,
        fromAddress = from,
        fromName = name,
        subject = "Hello",
        snippet = null,
        bodyText = "body",
        timestampEpochMs = 1_800_000_000_000L,
    )

    private suspend fun seed(vararg messages: MessageRecord) {
        for (m in messages) {
            mail.saveThreadWithMessages(
                ThreadRecord(
                    threadId = m.threadId,
                    gmailThreadId = null,
                    accountId = m.accountId,
                    subject = m.subject,
                    participantDisplayNames = listOf(m.fromAddress),
                    messageCount = 1,
                    unreadCount = 1,
                    latestMessageId = m.messageId,
                    latestMessageEpochMs = m.timestampEpochMs,
                    updatedAtEpochMs = m.timestampEpochMs,
                ),
                listOf(m),
            )
        }
    }

    // ---- Attribution ----

    @Test
    fun `processMessage attributes sender and company`() = runTest {
        val m = message("m1", "deals@mail.google.com", "Google Deals")
        seed(m)
        val attribution = useCase.processMessage(m)
        assertNotNull(attribution)
        assertEquals("co:google.com", attribution!!.company?.companyId)
        assertEquals("Google", attribution.company?.canonicalName)

        // Sender sighting recorded.
        val sender = intelligence.getSenderByEmail("a1", "deals@mail.google.com")
        assertNotNull(sender)
        assertEquals(1, sender!!.messageCount)

        // Message linked.
        assertEquals("co:google.com", mail.getMessage("m1")?.companyId)
    }

    @Test
    fun `subdomain variants merge into one company`() = runTest {
        seed(message("m1", "a@mail.google.com"), message("m2", "b@google.com"))
        useCase.processMessage(message("m1", "a@mail.google.com"))
        useCase.processMessage(message("m2", "b@google.com"))
        val company = intelligence.getCompanyByDomain("a1", "google.com")
        assertNotNull(company)
        // One company row, not two.
        assertEquals("co:google.com", company!!.companyId)
    }

    @Test
    fun `personal senders get sender tracking but no company`() = runTest {
        val m = message("m1", "jane@gmail.com", "Jane")
        seed(m)
        val attribution = useCase.processMessage(m)
        assertNotNull(attribution)
        assertNull(attribution!!.company)
        assertNotNull(intelligence.getSenderByEmail("a1", "jane@gmail.com"))
        assertNull(mail.getMessage("m1")?.companyId)
    }

    @Test
    fun `sender count increments across messages`() = runTest {
        val msgs = List(3) { i -> message("m$i", "boss@acme.com") }
        seed(*msgs.toTypedArray())
        msgs.forEach { useCase.processMessage(it) }
        val sender = intelligence.getSenderByEmail("a1", "boss@acme.com")
        assertEquals(3, sender!!.messageCount)
    }

    @Test
    fun `pinning survives re-detection`() = runTest {
        seed(message("m1", "a@acme.com"), message("m2", "b@mail.acme.com"))
        useCase.processMessage(message("m1", "a@acme.com"))
        val companyId = "co:acme.com"
        useCase.setCompanyPinned(companyId, true)
        // Re-process: detection must not clear the user's pin.
        useCase.processMessage(message("m2", "b@mail.acme.com"))
        val company = intelligence.getCompanyByDomain("a1", "acme.com")
        assertTrue(company!!.pinned)
    }

    @Test
    fun `cross-account messages are never attributed`() = runTest {
        // Seeded under a different account: the "a1" pass must skip it.
        seed(message("m1", "a@acme.com", accountId = "other"))
        val n = useCase.processNew("a1")
        assertEquals(0, n)
        assertNull(intelligence.getSenderByEmail("a1", "a@acme.com"))
        assertNull(mail.getMessage("m1")?.companyId)
    }

    @Test
    fun `processNew is bounded and incremental`() = runTest {
        seed(
            message("m1", "a@acme.com"),
            message("m2", "b@acme.com"),
            message("m3", "c@other.io"),
        )
        val n = useCase.processNew("a1", limit = 2)
        assertEquals(2, n)
        // One message left unattributed (limit honored).
        assertEquals(1, mail.getMessagesWithoutCompany("a1", 10).size)
        // Second pass picks up the remainder.
        assertEquals(1, useCase.processNew("a1", limit = 10))
    }

    // ---- Recurring-sender signal ----

    @Test
    fun `isRecurring reflects the documented threshold`() = runTest {
        repeat(2) { i -> useCase.processMessage(message("m$i", "pal@example.com")) }
        assertFalse(useCase.isRecurring("a1", "pal@example.com"))
        useCase.processMessage(message("m2", "pal@example.com"))
        assertTrue(useCase.isRecurring("a1", "pal@example.com"))
    }

    @Test
    fun `isRecurring is account-isolated`() = runTest {
        repeat(5) { i -> useCase.processMessage(message("m$i", "pal@example.com")) }
        assertTrue(useCase.isRecurring("a1", "pal@example.com"))
        assertFalse(useCase.isRecurring("a2", "pal@example.com"))
    }

    @Test
    fun `isRecurring is false for unknown senders`() = runTest {
        assertFalse(useCase.isRecurring("a1", "ghost@example.com"))
    }

    // ---- Totality ----

    @Test
    fun `blank sender degrades to null instead of throwing`() = runTest {
        assertNull(useCase.processMessage(message("m1", "   ")))
    }
}
