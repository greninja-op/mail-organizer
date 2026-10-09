package com.greninjaop.mailorganizer.core.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class ParticipantClassifierTest {

    private val userEmail = "alex.rivera@example.com"

    @Test
    fun `user email exact match is classified as USER`() {
        val role = ParticipantClassifier.classifyRole(
            fromAddress = "alex.rivera@example.com",
            userEmail = userEmail,
        )
        assertEquals(ParticipantRole.USER, role)
    }

    @Test
    fun `user email with name and formatting is classified as USER`() {
        val role = ParticipantClassifier.classifyRole(
            fromAddress = "Alex Rivera <Alex.Rivera@Example.Com>",
            userEmail = userEmail,
        )
        assertEquals(ParticipantRole.USER, role)
    }

    @Test
    fun `counterparty human email is classified as OTHER_PARTY`() {
        val role = ParticipantClassifier.classifyRole(
            fromAddress = "sarah.chen@partner.com",
            userEmail = userEmail,
        )
        assertEquals(ParticipantRole.OTHER_PARTY, role)
    }

    @Test
    fun `counterparty with name is classified as OTHER_PARTY`() {
        val role = ParticipantClassifier.classifyRole(
            fromAddress = "Dr. Robert Smith <rsmith@hospital.org>",
            userEmail = userEmail,
        )
        assertEquals(ParticipantRole.OTHER_PARTY, role)
    }

    @Test
    fun `automated no-reply addresses are classified as AUTOMATED_NO_REPLY`() {
        val noReplyEmails = listOf(
            "no-reply@service.com",
            "noreply@updates.com",
            "do-not-reply@bank.com",
            "donotreply@portal.org",
            "notifications@slack.com",
            "notification@app.com",
            "alerts@security.net",
            "mailer-daemon@mx.google.com",
            "bounce@marketing.io",
            "system@gitlab.com",
            "automated@aws.amazon.com",
        )

        for (email in noReplyEmails) {
            val role = ParticipantClassifier.classifyRole(
                fromAddress = email,
                userEmail = userEmail,
            )
            assertEquals("Expected $email to be AUTOMATED_NO_REPLY", ParticipantRole.AUTOMATED_NO_REPLY, role)
        }
    }

    @Test
    fun `null or empty user email falls back safely`() {
        val role = ParticipantClassifier.classifyRole(
            fromAddress = "sarah@example.com",
            userEmail = null,
        )
        assertEquals(ParticipantRole.OTHER_PARTY, role)
    }

    @Test
    fun `displayName extraction strips email angle brackets`() {
        val name = ParticipantClassifier.displayName("Sarah Chen <sarah@chen.org>")
        assertEquals("Sarah Chen", name)
    }

    @Test
    fun `displayName falls back to local part if only email provided`() {
        val name = ParticipantClassifier.displayName("sarah.chen@example.com")
        assertEquals("sarah.chen", name)
    }
}
