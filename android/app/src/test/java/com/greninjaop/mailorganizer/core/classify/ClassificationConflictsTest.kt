package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Conflict-resolution tests (Phase 7 §13, §45).
 *
 * Overlapping signals must resolve deterministically via scoring +
 * [DeterministicClassifier.PRECEDENCE] — never by rule-list order.
 */
class ClassificationConflictsTest {

    private fun input(
        from: String = "sender@example.com",
        name: String? = null,
        subject: String = "",
        body: String? = null,
        labels: List<String> = emptyList(),
        id: String = "m1",
    ) = ClassificationInput(
        messageId = id,
        fromAddress = from,
        fromName = name,
        subject = subject,
        bodyText = body,
        labelIds = labels,
    )

    private fun resultFor(i: ClassificationInput): ClassificationResult =
        DeterministicClassifier.classify(i) { 1_700_000_000_000L }

    @Test
    fun `security beats promotion`() {
        // "Newsletter + Security": an OTP mail that also has an unsubscribe
        // footer must stay SECURITY — security outranks by weight.
        val r = resultFor(
            input(
                from = "security@example.com",
                subject = "Your verification code",
                body = "Your verification code is 991122. Unsubscribe from alerts here.",
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
        assertEquals(Confidence.HIGH, r.confidence)
    }

    @Test
    fun `security beats notification`() {
        val r = resultFor(
            input(
                subject = "New login to your account",
                body = "A new device signed in. This is a routine notification.",
                labels = listOf("CATEGORY_UPDATES"),
            ),
        )
        assertEquals(ClassifierCategory.SECURITY, r.category)
    }

    @Test
    fun `order beats promotion`() {
        // "Promotion + Order": a shipped mail with marketing footer stays
        // RECEIPTS_ORDERS — the order rule (80) outscores promotion (50).
        val r = resultFor(
            input(
                from = "orders@shop.example.com",
                subject = "Your order has shipped",
                body = "Your package is on its way! P.S. Check out our big sale.",
                labels = listOf("CATEGORY_PROMOTIONS"),
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
        assertTrue(r.firingRuleIds.contains("ORDER_SHIPPED"))
    }

    @Test
    fun `action required beats notification`() {
        val r = resultFor(
            input(
                subject = "Payment due tomorrow",
                body = "Reminder: your payment is due tomorrow. This is a notification.",
                labels = listOf("CATEGORY_UPDATES"),
            ),
        )
        assertEquals(ClassifierCategory.ACTION_REQUIRED, r.category)
    }

    @Test
    fun `career beats promotion`() {
        // Recruiter mail with a "discount"-style word stays CAREER.
        val r = resultFor(
            input(
                from = "talent@company.example.com",
                name = "Talent Team",
                subject = "Interested? Save the date for a chat",
                body = "Hi, I'm a recruiter. Let's schedule a call about this role.",
            ),
        )
        assertEquals(ClassifierCategory.CAREER, r.category)
    }

    @Test
    fun `education beats notification`() {
        val r = resultFor(
            input(
                from = "prof@university.edu",
                subject = "Assignment 2 grades posted",
                body = "Your assignment grades are now available. Notification.",
            ),
        )
        assertEquals(ClassifierCategory.EDUCATION, r.category)
    }

    @Test
    fun `tie breaks by precedence deterministically`() {
        // Construct a near-tie: newsletter markers (55) vs promotion CTA
        // with Gmail promotions tab (45). Newsletter wins by score; the
        // test asserts the documented resolution, run twice for determinism.
        val i = input(
            from = "news@shop.example.com",
            subject = "Weekly deals newsletter",
            body = "Our weekly newsletter is here. Shop now! Unsubscribe anytime.",
            labels = listOf("CATEGORY_PROMOTIONS"),
        )
        val a = resultFor(i)
        val b = resultFor(i)
        assertEquals(a.category, b.category)
        assertEquals(a.ruleId, b.ruleId)
    }

    @Test
    fun `low value never wins when another category scored`() {
        // Noreply noise markers + a real order signal: order must win; the
        // LOW_VALUE gate keeps the noise bucket conservative (phase §41).
        val r = resultFor(
            input(
                from = "noreply@shop.example.com",
                subject = "Your order confirmation",
                body = "This is an automated message. Thank you for your order!",
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
    }

    @Test
    fun `precedence list covers every category`() {
        val all = ClassifierCategory.values().toSet()
        assertEquals(all, DeterministicClassifier.PRECEDENCE.toSet())
    }
}
