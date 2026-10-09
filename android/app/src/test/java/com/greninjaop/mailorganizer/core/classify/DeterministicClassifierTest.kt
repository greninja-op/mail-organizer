package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category coverage tests for [DeterministicClassifier] (Phase 7 §44).
 *
 * All fixtures are synthetic (phase §63) — no real user mail.
 */
class DeterministicClassifierTest {

    private fun input(
        from: String = "sender@example.com",
        name: String? = null,
        subject: String = "",
        body: String? = null,
        labels: List<String> = emptyList(),
        filenames: List<String> = emptyList(),
        id: String = "m1",
    ) = ClassificationInput(
        messageId = id,
        fromAddress = from,
        fromName = name,
        subject = subject,
        bodyText = body,
        labelIds = labels,
        attachmentFilenames = filenames,
    )

    private fun resultFor(i: ClassificationInput): ClassificationResult =
        DeterministicClassifier.classify(i) { 1_700_000_000_000L }

    // ---- Security (phase §44) ----

    @Test
    fun `otp mail is security with high confidence`() {
        val r = resultFor(
            input(
                from = "no-reply@accounts.example.com",
                subject = "Your verification code",
                body = "Your verification code is 482910. It expires in 10 minutes.",
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
        assertEquals(Confidence.HIGH, r.confidence)
        assertEquals("SECURITY_OTP", r.ruleId)
        assertEquals(DeterministicClassifier.VERSION, r.classifierVersion)
        assertTrue(r.explanation.isNotBlank())
        assertTrue(r.matchedSignals.isNotEmpty())
    }

    @Test
    fun `password reset mail is security`() {
        val r = resultFor(
            input(
                subject = "Reset your password",
                body = "We received a request to reset your password. Click the link.",
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
        assertEquals("SECURITY_PASSWORD_RESET", r.ruleId)
    }

    @Test
    fun `login alert mail is security`() {
        val r = resultFor(
            input(
                subject = "New login to your account",
                body = "A new device just signed in to your account.",
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
        assertEquals("SECURITY_LOGIN_ALERT", r.ruleId)
    }

    @Test
    fun `suspicious activity mail is security`() {
        val r = resultFor(
            input(
                subject = "Unusual activity detected",
                body = "We detected suspicious activity on your account.",
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
    }

    @Test
    fun `account verification mail is security`() {
        val r = resultFor(
            input(
                subject = "Please verify your email",
                body = "Click below to verify your account.",
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
        assertEquals("SECURITY_VERIFY_ACCOUNT", r.ruleId)
    }

    // ---- Career ----

    @Test
    fun `recruiter outreach is career`() {
        val r = resultFor(
            input(
                from = "jane@recruiting.example.com",
                name = "Jane Recruiter",
                subject = "Exciting opportunity for you",
                body = "Hi, I'm a recruiter at ExampleCo and your profile stood out.",
            ),
        )
        assertEquals(ClassifierCategory.CAREER, r.category)
        assertEquals("CAREER_RECRUITER", r.ruleId)
    }

    @Test
    fun `linkedin sender is career`() {
        val r = resultFor(
            input(
                from = "jobs@linkedin.com",
                subject = "3 new jobs for you",
            ),
        )
        assertEquals(ClassifierCategory.CAREER, r.category)
    }

    @Test
    fun `interview invitation is career`() {
        val r = resultFor(
            input(
                subject = "Interview invitation — Software Engineer",
                body = "We would like to schedule an interview with you next week.",
            ),
        )
        assertEquals(ClassifierCategory.CAREER, r.category)
        assertEquals("CAREER_INTERVIEW", r.ruleId)
    }

    @Test
    fun `application status is career`() {
        val r = resultFor(
            input(
                subject = "Your application status",
                body = "Thank you for applying. Your application has been received.",
            ),
        )
        assertEquals(ClassifierCategory.CAREER, r.category)
    }

    // ---- Education ----

    @Test
    fun `edu sender is education`() {
        val r = resultFor(
            input(
                from = "registrar@university.edu",
                subject = "Fall semester registration",
            ),
        )
        assertEquals(ClassifierCategory.EDUCATION, r.category)
        assertEquals("EDUCATION_INSTITUTION", r.ruleId)
    }

    @Test
    fun `assignment notice is education`() {
        val r = resultFor(
            input(
                subject = "Assignment 3 posted",
                body = "The new assignment is due Friday.",
            ),
        )
        assertEquals(ClassifierCategory.EDUCATION, r.category)
    }

    @Test
    fun `exam notice is education`() {
        val r = resultFor(
            input(
                subject = "Midterm exam schedule",
                body = "Your midterm exam is on Monday.",
            ),
        )
        assertEquals(ClassifierCategory.EDUCATION, r.category)
    }

    @Test
    fun `coursera sender is education`() {
        val r = resultFor(
            input(
                from = "noreply@coursera.org",
                subject = "Your course starts Monday",
            ),
        )
        assertEquals(ClassifierCategory.EDUCATION, r.category)
    }

    // ---- Receipts & Orders ----

    @Test
    fun `order confirmation is receipts and orders`() {
        val r = resultFor(
            input(
                from = "orders@shop.example.com",
                subject = "Your order confirmation #12345",
                body = "Thank you for your order! Your items will ship soon.",
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
        assertEquals("ORDER_CONFIRMATION", r.ruleId)
    }

    @Test
    fun `shipping notification is receipts and orders`() {
        val r = resultFor(
            input(
                subject = "Your package has shipped",
                body = "Your package is on its way. Tracking number: 1Z999.",
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
    }

    @Test
    fun `delivery confirmation is receipts and orders`() {
        val r = resultFor(
            input(
                subject = "Delivered: your package",
                body = "Your package has been delivered.",
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
    }

    @Test
    fun `invoice with attachment is receipts and orders`() {
        val r = resultFor(
            input(
                subject = "Invoice for March",
                body = "Please find your invoice attached.",
                filenames = listOf("invoice-march.pdf"),
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
        assertTrue(r.matchedSignals.any { it.name == "invoice_attachment" })
    }

    @Test
    fun `refund notice is receipts and orders`() {
        val r = resultFor(
            input(
                subject = "Your refund has been processed",
                body = "We have refunded $42.00 to your card.",
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
    }

    // ---- Newsletters ----

    @Test
    fun `newsletter with unsubscribe is newsletters`() {
        val r = resultFor(
            input(
                from = "news@digest.example.com",
                subject = "Your weekly digest",
                body = "Top stories this week. Unsubscribe anytime.",
            ),
        )
        assertEquals(ClassifierCategory.NEWSLETTERS, r.category)
    }

    // ---- Promotions ----

    @Test
    fun `discount mail is promotions`() {
        val r = resultFor(
            input(
                from = "deals@shop.example.com",
                subject = "20% off everything",
                body = "Use promo code SAVE20 at checkout.",
                labels = listOf("CATEGORY_PROMOTIONS"),
            ),
        )
        assertEquals(ClassifierCategory.PROMOTIONS, r.category)
    }

    @Test
    fun `sale mail is promotions`() {
        val r = resultFor(
            input(
                subject = "Clearance: limited time offer",
                body = "Our clearance sale ends Sunday.",
            ),
        )
        assertEquals(ClassifierCategory.PROMOTIONS, r.category)
    }

    // ---- Notifications ----

    @Test
    fun `social label mail is notifications`() {
        val r = resultFor(
            input(
                from = "notify@facebook.com",
                subject = "You have 3 new notifications",
                body = "Priya liked your photo.",
                labels = listOf("CATEGORY_SOCIAL"),
            ),
        )
        assertEquals(ClassifierCategory.NOTIFICATIONS, r.category)
        assertEquals("NOTIFICATION_SOCIAL", r.ruleId)
    }

    @Test
    fun `github notification is notifications`() {
        val r = resultFor(
            input(
                from = "notifications@github.com",
                subject = "[github] Build succeeded",
                labels = listOf("CATEGORY_UPDATES"),
            ),
        )
        assertEquals(ClassifierCategory.NOTIFICATIONS, r.category)
    }

    // ---- Action Required ----

    @Test
    fun `deadline mail is action required`() {
        val r = resultFor(
            input(
                subject = "Action required: documents due Friday",
                body = "Please submit your documents by Friday. This is a deadline.",
            ),
        )
        assertEquals(ClassifierCategory.ACTION_REQUIRED, r.category)
    }

    @Test
    fun `payment due mail is action required`() {
        val r = resultFor(
            input(
                subject = "Payment due: your bill",
                body = "Your payment is past due. Please pay by Monday.",
            ),
        )
        assertEquals(ClassifierCategory.ACTION_REQUIRED, r.category)
    }

    // ---- Important ----

    @Test
    fun `personal mail is important`() {
        val r = resultFor(
            input(
                from = "friend@example.com",
                name = "Alex Friend",
                subject = "Catching up",
                body = "Hey! Long time no see. Want to grab coffee?",
            ),
        )
        assertEquals(ClassifierCategory.IMPORTANT, r.category)
    }

    // ---- Low value / unclassified ----

    @Test
    fun `noreply noise is low value`() {
        val r = resultFor(
            input(
                from = "noreply@system.example.com",
                subject = "System notice",
                body = "This is an automated message. Do not reply.",
            ),
        )
        assertEquals(ClassifierCategory.LOW_VALUE, r.category)
        assertEquals(Confidence.LOW, r.confidence)
    }

    @Test
    fun `utterly generic mail stays unclassified`() {
        val r = resultFor(
            input(
                from = "mailer@example.com",
                subject = "Hello",
                body = "Just saying hello.",
            ),
        )
        // No rule fires: honest uncertainty, never a forced bucket.
        assertEquals(ClassifierCategory.UNCLASSIFIED, r.category)
        assertEquals(Confidence.LOW, r.confidence)
    }

    // ---- Result model ----

    @Test
    fun `result carries traceability fields`() {
        val r = resultFor(
            input(subject = "Your verification code is 123456"),
        )
        assertNotNull(r.ruleId)
        assertTrue(r.firingRuleIds.isNotEmpty())
        assertEquals(1_700_000_000_000L, r.classifiedAtEpochMs)
        assertEquals(DeterministicClassifier.VERSION, r.classifierVersion)
        assertTrue(r.explanation.contains("Security", ignoreCase = true))
    }

    @Test
    fun `same input classifies identically`() {
        val i = input(
            subject = "Interview invitation",
            body = "We would like to schedule an interview.",
        )
        val a = DeterministicClassifier.classify(i) { 1000L }
        val b = DeterministicClassifier.classify(i) { 2000L }
        assertEquals(a.category, b.category)
        assertEquals(a.confidence, b.confidence)
        assertEquals(a.ruleId, b.ruleId)
        assertEquals(a.matchedSignals, b.matchedSignals)
        // Only the injected timestamp differs.
        assertEquals(1000L, a.classifiedAtEpochMs)
        assertEquals(2000L, b.classifiedAtEpochMs)
    }
}
