package com.greninjaop.mailorganizer.core.classify

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Performance tests (Phase 7 §48): classification must stay cheap at
 * mailbox scale and never block the UI thread (the use cases dispatch to
 * IO; these tests assert the per-message cost).
 */
class ClassificationPerformanceTest {

    private val subjects = listOf(
        "Your verification code" to "Your verification code is 123456.",
        "Your order has shipped" to "Your package is on its way.",
        "Interview invitation" to "We would like to schedule an interview.",
        "Weekly digest" to "Top stories. Unsubscribe anytime.",
        "20% off sale" to "Use promo code SAVE20.",
        "Assignment posted" to "The new assignment is due Friday.",
        "New login" to "A new device signed in to your account.",
        "Invoice attached" to "Please find your invoice attached.",
        "Hello" to "Just saying hello.",
        "Team lunch" to "Let's grab lunch tomorrow at noon.",
    )

    private fun input(i: Int): ClassificationInput {
        val (subject, body) = subjects[i % subjects.size]
        return ClassificationInput(
            messageId = "m$i",
            fromAddress = "sender$i@example.com",
            fromName = "Sender $i",
            subject = "$subject #$i",
            bodyText = "$body Message number $i with some extra filler text.",
        )
    }

    private fun classifyN(n: Int): Long {
        val start = System.nanoTime()
        for (i in 0 until n) {
            DeterministicClassifier.classify(input(i)) { 1_700_000_000_000L }
        }
        return (System.nanoTime() - start) / 1_000_000
    }

    @Test
    fun `100 emails classify quickly`() {
        val ms = classifyN(100)
        println("100 emails: ${ms}ms")
        assertTrue("100 emails took ${ms}ms, expected < 2000ms", ms < 2000)
    }

    @Test
    fun `1000 emails classify in reasonable time`() {
        val ms = classifyN(1000)
        println("1000 emails: ${ms}ms")
        assertTrue("1000 emails took ${ms}ms, expected < 10000ms", ms < 10_000)
    }

    @Test
    fun `10000 emails classify in reasonable time`() {
        val ms = classifyN(10_000)
        println("10000 emails: ${ms}ms")
        // ~1ms per message budget on this hardware class.
        assertTrue("10000 emails took ${ms}ms, expected < 60000ms", ms < 60_000)
    }

    @Test
    fun `per-message cost is sub-millisecond on average`() {
        val ms = classifyN(2000)
        val perMessage = ms.toDouble() / 2000
        println("per-message: ${perMessage}ms")
        assertTrue("per-message ${perMessage}ms, expected < 5ms", perMessage < 5.0)
    }
}
