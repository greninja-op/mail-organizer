package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Adversarial tests (Phase 7 §46): misleading subjects must follow the
 * documented rules, not single-word reflexes.
 */
class ClassificationAdversarialTest {

    private fun input(
        from: String = "sender@example.com",
        name: String? = null,
        subject: String = "",
        body: String? = null,
        labels: List<String> = emptyList(),
    ) = ClassificationInput(
        messageId = "m1",
        fromAddress = from,
        fromName = name,
        subject = subject,
        bodyText = body,
        labelIds = labels,
    )

    private fun resultFor(i: ClassificationInput): ClassificationResult =
        DeterministicClassifier.classify(i) { 1_700_000_000_000L }

    @Test
    fun `lone security word does not trigger security`() {
        // "Your account security sale is here!" — single-word "security"
        // and "sale" must not fire the multi-word security phrases.
        val r = resultFor(
            input(
                from = "deals@shop.example.com",
                subject = "Your account security sale is here!",
                body = "Keep your account safe with our security sale.",
            ),
        )
        assertNotEquals(
            "single-word 'security' must not classify as SECURITY",
            ClassifierCategory.SECURITY,
            r.category,
        )
    }

    @Test
    fun `interview tips newsletter is newsletters not career`() {
        // "Interview" alone (no scheduling context) + newsletter format.
        val r = resultFor(
            input(
                from = "news@careerblog.example.com",
                subject = "Interview tips newsletter",
                body = "This week's newsletter: top 10 interview tips. Unsubscribe anytime.",
            ),
        )
        assertEquals(ClassifierCategory.NEWSLETTERS, r.category)
    }

    @Test
    fun `lone sale inside unrelated sentence is not promotion`() {
        // "sale" as a bare word is not in any promotion phrase list —
        // multi-word phrases are required (phase §21).
        val r = resultFor(
            input(
                from = "friend@example.com",
                name = "Sam",
                subject = "Weekend plans",
                body = "For sale: my old bike. Let me know if interested!",
            ),
        )
        assertNotEquals(ClassifierCategory.PROMOTIONS, r.category)
    }

    @Test
    fun `fake invoice phish still classifies by content not trust`() {
        // The classifier is content-based, not a phishing detector: an
        // invoice-shaped mail is RECEIPTS_ORDERS. Documented limitation —
        // phishing detection is not Phase 7 scope.
        val r = resultFor(
            input(
                from = "billing@totally-legit.example.com",
                subject = "Invoice #999 due now",
                body = "Please pay this invoice immediately.",
            ),
        )
        assertEquals(ClassifierCategory.RECEIPTS_ORDERS, r.category)
    }

    @Test
    fun `unsubscribe alone does not make a newsletter`() {
        // Legit service mail with an unsubscribe footer but no newsletter
        // markers must not become NEWSLETTERS (phase §25).
        val r = resultFor(
            input(
                from = "alerts@bank.example.com",
                subject = "Your statement is ready",
                body = "Your monthly statement is available. Unsubscribe from " +
                    "marketing emails here.",
            ),
        )
        assertNotEquals(ClassifierCategory.NEWSLETTERS, r.category)
    }

    @Test
    fun `gmail promotions label alone does not decide`() {
        // Phase §23: the Gmail label needs corroborating marketing wording.
        val r = resultFor(
            input(
                from = "hello@startup.example.com",
                subject = "Thanks for signing up",
                body = "Welcome aboard! Here's how to get started.",
                labels = listOf("CATEGORY_PROMOTIONS"),
            ),
        )
        assertNotEquals(
            "bare CATEGORY_PROMOTIONS must not force PROMOTIONS",
            ClassifierCategory.PROMOTIONS,
            r.category,
        )
    }

    @Test
    fun `keyword stuffing does not escalate confidence`() {
        val r = resultFor(
            input(
                subject = "sale sale sale sale",
                body = "sale ".repeat(200),
            ),
        )
        // Bare "sale" matches no multi-word phrase; must not be HIGH.
        assertNotEquals(Confidence.HIGH, r.confidence)
    }
}
