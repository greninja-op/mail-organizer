package com.greninjaop.mailorganizer.core.actions

import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic generator tests (Phase 14).
 *
 * Every test pins behavior to fixed inputs — no randomness, no clock
 * dependence beyond the injected now.
 */
class ActionCandidateGeneratorTest {

    companion object {
        private const val NOW = 1_800_000_000_000L
        private const val DAY = 24L * 60 * 60 * 1000
    }

    private fun baseInput(
        messageId: String = "m1",
        category: MailCategory? = null,
        priority: Priority? = Priority.NORMAL,
        actionRequired: Boolean = false,
        temporal: List<TemporalRef> = emptyList(),
        subject: String = "Subject",
    ) = ActionInput(
        messageId = messageId,
        threadId = "t1",
        accountId = "a1",
        subject = subject,
        senderName = "Acme",
        senderAddress = "jobs@acme.com",
        timestampEpochMs = NOW - DAY,
        unread = true,
        category = category,
        categorySourceName = "DETERMINISTIC",
        priority = priority,
        actionRequired = actionRequired,
        temporalItems = temporal,
    )

    private fun temporal(
        id: Long = 7L,
        type: String = "INTERVIEW",
        status: String = "UPCOMING",
        confidence: String = "HIGH",
        start: Long = NOW + DAY,
        dateOnly: Boolean = false,
    ) = TemporalRef(
        itemId = id,
        itemTypeName = type,
        title = "Onsite interview",
        startEpochMs = start,
        endEpochMs = null,
        isDateOnly = dateOnly,
        timezoneId = "UTC",
        location = null,
        meetingUrl = null,
        statusName = status,
        confidenceName = confidence,
        signalNames = listOf("interview_vocabulary"),
    )

    @Test
    fun `interview produces meeting candidate with calendar proposal`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(temporal = listOf(temporal())),
            NOW,
        )
        assertEquals(1, out.size)
        val c = out.single()
        assertEquals(ActionType.MEETING, c.actionType)
        assertEquals(ExternalEffect.CALENDAR, c.externalEffect)
        assertEquals("temporal:7", c.targetKey)
        assertEquals("m1|MEETING|temporal:7", c.id)
        assertTrue(c.title.contains("Interview"))
        assertEquals(ActionSource.TEMPORAL_EXTRACTION, c.source)
        assertEquals(ActionConfidence.HIGH, c.confidence)
    }

    @Test
    fun `past temporal items never generate candidates`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(temporal = listOf(temporal(status = "PAST", start = NOW - DAY))),
            NOW,
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `bare date items never generate candidates`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(temporal = listOf(temporal(type = "DATE_ONLY"))),
            NOW,
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `deadline temporal items map to typed review cards`() {
        val cases = mapOf(
            "APPLICATION_DEADLINE" to ActionType.APPLICATION,
            "SUBMISSION_DEADLINE" to ActionType.APPLICATION,
            "PAYMENT_DEADLINE" to ActionType.PAYMENT,
            "REGISTRATION_DEADLINE" to ActionType.DEADLINE,
            "DEADLINE" to ActionType.DEADLINE,
            "REMINDER_DATE" to ActionType.REMINDER,
            "TRAVEL" to ActionType.TRAVEL,
        )
        for ((temporalType, expected) in cases) {
            val out = ActionCandidateGenerator.generate(
                baseInput(
                    messageId = "m-$temporalType",
                    temporal = listOf(temporal(type = temporalType)),
                ),
                NOW,
            )
            assertEquals("temporal type $temporalType", 1, out.size)
            assertEquals(expected, out.single().actionType)
            // Review-style cards propose nothing external.
            assertEquals(ExternalEffect.NONE, out.single().externalEffect)
        }
    }

    @Test
    fun `action-required category produces reply candidate`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(
                category = MailCategory.ACTION_REQUIRED,
                actionRequired = true,
            ),
            NOW,
        )
        assertEquals(1, out.size)
        val c = out.single()
        assertEquals(ActionType.REPLY_REQUIRED, c.actionType)
        // Replying is a Gmail write (Phase 22) — the suggestion stays internal.
        assertEquals(ExternalEffect.NONE, c.externalEffect)
        assertTrue(c.description.contains("nothing is sent automatically"))
    }

    @Test
    fun `user correction source is preserved on candidates`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(
                category = MailCategory.ACTION_REQUIRED,
                actionRequired = true,
            ).copy(categorySourceName = "USER_CORRECTION"),
            NOW,
        )
        assertEquals(ActionSource.USER_CORRECTION, out.single().source)
    }

    @Test
    fun `critical priority with no other signal produces review candidate`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(priority = Priority.CRITICAL),
            NOW,
        )
        assertEquals(1, out.size)
        assertEquals(ActionType.OTHER, out.single().actionType)
        assertEquals(ActionUrgency.CRITICAL, out.single().urgency)
    }

    @Test
    fun `ordinary mail produces no candidates`() {
        val out = ActionCandidateGenerator.generate(baseInput(), NOW)
        assertTrue(out.isEmpty())
    }

    @Test
    fun `urgency rises as the due date approaches`() {
        val soon = ActionCandidateGenerator.generate(
            baseInput(
                priority = Priority.LOW,
                temporal = listOf(temporal(start = NOW + 2 * 60 * 60 * 1000)),
            ),
            NOW,
        ).single()
        assertEquals(ActionUrgency.HIGH, soon.urgency)

        val far = ActionCandidateGenerator.generate(
            baseInput(
                priority = Priority.LOW,
                temporal = listOf(temporal(start = NOW + 30 * DAY)),
            ),
            NOW,
        ).single()
        assertEquals(ActionUrgency.LOW, far.urgency)
    }

    @Test
    fun `generation is deterministic`() {
        val input = baseInput(
            category = MailCategory.ACTION_REQUIRED,
            actionRequired = true,
            priority = Priority.HIGH,
            temporal = listOf(temporal(), temporal(id = 8L, type = "PAYMENT_DEADLINE")),
        )
        val a = ActionCandidateGenerator.generate(input, NOW)
        val b = ActionCandidateGenerator.generate(input, NOW)
        assertEquals(a, b)
    }

    @Test
    fun `blank identity produces nothing`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(messageId = "").copy(accountId = ""),
            NOW,
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `dedup ids are stable per message type and target`() {
        val out = ActionCandidateGenerator.generate(
            baseInput(temporal = listOf(temporal(id = 3L), temporal(id = 4L, type = "DEADLINE"))),
            NOW,
        )
        val ids = out.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("m1|") })
    }
}
