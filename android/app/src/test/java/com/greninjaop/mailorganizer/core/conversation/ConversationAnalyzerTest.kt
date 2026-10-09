package com.greninjaop.mailorganizer.core.conversation

import com.greninjaop.mailorganizer.core.classify.Confidence
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationAnalyzerTest {

    private val userEmail = "alex.rivera@example.com"
    private val accountId = "acct-personal"
    private val threadId = "thread-101"
    private val baseTime = 1_700_000_000_000L // Reference time

    private val analyzer = ConversationAnalyzer(clock = { baseTime })

    private fun createMessage(
        id: String,
        fromAddress: String,
        fromName: String? = null,
        subject: String = "Test Conversation",
        bodyText: String = "Hello there",
        timestamp: Long,
    ) = MessageRecord(
        messageId = id,
        gmailMessageId = "gm-$id",
        threadId = threadId,
        accountId = accountId,
        fromAddress = fromAddress,
        fromName = fromName,
        subject = subject,
        snippet = bodyText.take(50),
        bodyText = bodyText,
        timestampEpochMs = timestamp,
        unread = false,
    )

    @Test
    fun emptyMessages_returnsNoAction() {
        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = emptyList(),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.NO_ACTION, result.state)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun otherPartyAsksDirectQuestion_returnsAwaitingUserReply() {
        val msg = createMessage(
            id = "m1",
            fromAddress = "recruiter@acme.com",
            fromName = "Acme Recruiting",
            subject = "Interview Schedule",
            bodyText = "Hi Alex, please let me know which time slot works for you.",
            timestamp = baseTime - 3600_000L, // 1 hour ago
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(msg),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.AWAITING_USER_REPLY, result.state)
        assertTrue(result.explanation.summary.contains("awaiting your reply", ignoreCase = true))
        assertTrue(result.explanation.reasons.any { it.contains("Acme Recruiting") })
        assertEquals("m1", result.lastMessageId)
        assertFalse(result.isFollowUpCandidate)
    }

    @Test
    fun userRepliesAfterOtherPartyQuestion_transitionsToAwaitingOtherPartyOrRecentlyReplied() {
        val m1 = createMessage(
            id = "m1",
            fromAddress = "recruiter@acme.com",
            fromName = "Acme Recruiting",
            subject = "Interview Schedule",
            bodyText = "Can you please send your updated resume?",
            timestamp = baseTime - (48 * 3600_000L), // 2 days ago
        )

        val m2 = createMessage(
            id = "m2",
            fromAddress = userEmail,
            fromName = "Alex Rivera",
            subject = "Re: Interview Schedule",
            bodyText = "Attached is my latest resume. Looking forward to speaking with the team.",
            timestamp = baseTime - (4 * 3600_000L), // 4 hours ago (recent reply)
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(m1, m2),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        // User replied 4h ago: state is RECENTLY_REPLIED or AWAITING_OTHER_PARTY
        assertTrue(
            result.state == ConversationState.RECENTLY_REPLIED ||
                result.state == ConversationState.AWAITING_OTHER_PARTY
        )
        assertEquals("m2", result.lastMessageId)
        assertEquals(m2.timestampEpochMs, result.lastUserMessageEpochMs)
    }

    @Test
    fun userSentMessageOver3DaysAgoWithQuestion_becomesFollowUpCandidate() {
        val m1 = createMessage(
            id = "m1",
            fromAddress = userEmail,
            fromName = "Alex Rivera",
            subject = "Contract review",
            bodyText = "Hi Bob, can you please review the attached contract and let me know?",
            timestamp = baseTime - (4 * 24 * 3600_000L), // 4 days ago
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(m1),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.AWAITING_OTHER_PARTY, result.state)
        assertTrue(result.isFollowUpCandidate)
        assertNotNull(result.followUpReason)
        assertTrue(result.followUpReason!!.contains("3 days ago"))
    }

    @Test
    fun pendingQuestionOver7DaysAgo_becomesStaleConversation() {
        val m1 = createMessage(
            id = "m1",
            fromAddress = "partner@vendor.org",
            fromName = "Vendor Partner",
            subject = "Inquiry",
            bodyText = "Please let me know if you are interested in this demo.",
            timestamp = baseTime - (10 * 24 * 3600_000L), // 10 days ago
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(m1),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.STALE_CONVERSATION, result.state)
        assertTrue(result.explanation.summary.contains("7 days", ignoreCase = true))
    }

    @Test
    fun resolutionPhraseInLatestMessage_returnsResolved() {
        val m1 = createMessage(
            id = "m1",
            fromAddress = "support@company.com",
            fromName = "Support Desk",
            subject = "Ticket #1234",
            bodyText = "Can you confirm if your issue persists?",
            timestamp = baseTime - 7200_000L,
        )
        val m2 = createMessage(
            id = "m2",
            fromAddress = "support@company.com",
            fromName = "Support Desk",
            subject = "Re: Ticket #1234",
            bodyText = "Thank you, this is all set and the issue has been resolved. Case closed.",
            timestamp = baseTime - 3600_000L,
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(m1, m2),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.RESOLVED, result.state)
        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.explanation.summary.contains("resolved", ignoreCase = true))
    }

    @Test
    fun newsletterOrNotificationCategory_returnsNoAction() {
        val msg = createMessage(
            id = "m1",
            fromAddress = "digest@techweekly.com",
            fromName = "Tech Weekly",
            subject = "Are you ready for AI? Weekly edition",
            bodyText = "Read about latest tech trends. What do you think about our newsletter?",
            timestamp = baseTime - 3600_000L,
        )

        val classification = ClassificationRecord(
            messageId = "m1",
            accountId = accountId,
            category = MailCategory.NEWSLETTERS,
            confidence = 0.9f,
            source = ClassificationSource.DETERMINISTIC,
            version = 1,
            explanation = "Classified as Newsletters",
            classifiedAtEpochMs = baseTime,
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(msg),
            classificationsByMessageId = mapOf("m1" to classification),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.NO_ACTION, result.state)
        assertTrue(result.explanation.summary.contains("automated", ignoreCase = true) ||
            result.explanation.summary.contains("informational", ignoreCase = true))
    }

    @Test
    fun automatedNoReplySender_returnsNoActionEvenWithQuestionMarks() {
        val msg = createMessage(
            id = "m1",
            fromAddress = "no-reply@service.com",
            fromName = "Service Notifications",
            subject = "Security Notice",
            bodyText = "Did you log in from an unknown device? If not, check your account settings.",
            timestamp = baseTime - 1800_000L,
        )

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(msg),
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(ConversationState.NO_ACTION, result.state)
        assertEquals(Confidence.HIGH, result.confidence)
    }

    @Test
    fun multiAccountIsolation_directionDependsOnAccountOwner() {
        val userAcct1 = "work@company.com"
        val userAcct2 = "personal@gmail.com"

        val msg = createMessage(
            id = "m1",
            fromAddress = userAcct1,
            fromName = "Company User",
            subject = "Status report",
            bodyText = "Could you please send the report?",
            timestamp = baseTime - 1000L,
        )

        // For Account 1, this message is USER sent -> AWAITING_OTHER_PARTY
        val inputAcct1 = ConversationAnalysisInput(
            accountId = "acct-1",
            userEmailAddress = userAcct1,
            threadId = "t1",
            messages = listOf(msg),
            nowEpochMs = baseTime,
        )
        val result1 = analyzer.analyze(inputAcct1)
        assertEquals(ConversationState.AWAITING_OTHER_PARTY, result1.state)

        // For Account 2, this same sender is an OTHER_PARTY -> AWAITING_USER_REPLY
        val inputAcct2 = ConversationAnalysisInput(
            accountId = "acct-2",
            userEmailAddress = userAcct2,
            threadId = "t2",
            messages = listOf(msg),
            nowEpochMs = baseTime,
        )
        val result2 = analyzer.analyze(inputAcct2)
        assertEquals(ConversationState.AWAITING_USER_REPLY, result2.state)
    }

    @Test
    fun timelinePreservesParticipantOrderAndRoles() {
        val m1 = createMessage("m1", "client@corp.com", "Client Corp", "Project X", "Can we meet Monday?", baseTime - 7000L)
        val m2 = createMessage("m2", userEmail, "Alex", "Re: Project X", "Yes, 10am works.", baseTime - 3000L)
        val m3 = createMessage("m3", "client@corp.com", "Client Corp", "Re: Project X", "Perfect, see you then.", baseTime - 1000L)

        val input = ConversationAnalysisInput(
            accountId = accountId,
            userEmailAddress = userEmail,
            threadId = threadId,
            messages = listOf(m2, m1, m3), // pass in out of order
            nowEpochMs = baseTime,
        )

        val result = analyzer.analyze(input)
        assertEquals(3, result.timeline.size)
        // Timeline sorted chronologically
        assertEquals("m1", result.timeline[0].messageId)
        assertEquals(ParticipantRole.OTHER_PARTY, result.timeline[0].role)

        assertEquals("m2", result.timeline[1].messageId)
        assertEquals(ParticipantRole.USER, result.timeline[1].role)

        assertEquals("m3", result.timeline[2].messageId)
        assertEquals(ParticipantRole.OTHER_PARTY, result.timeline[2].role)
        assertTrue(result.timeline[2].isLatest)

        assertEquals(ConversationState.RESOLVED, result.state)
    }
}
