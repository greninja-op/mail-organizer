package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.data.local.MailCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelsTest {

    @Test
    fun aiCapability_enumMembers() {
        val capabilities = AiCapability.values()
        assertTrue(capabilities.contains(AiCapability.CLASSIFY_EMAIL))
        assertTrue(capabilities.contains(AiCapability.EXTRACT_DEADLINE))
        assertTrue(capabilities.contains(AiCapability.EXTRACT_MEETING))
        assertTrue(capabilities.contains(AiCapability.SUMMARIZE_THREAD))
        assertTrue(capabilities.contains(AiCapability.DETECT_INTENT))
    }

    @Test
    fun aiAvailabilityState_enumMembers() {
        val states = AiAvailabilityState.values()
        assertTrue(states.contains(AiAvailabilityState.DISABLED))
        assertTrue(states.contains(AiAvailabilityState.NOT_CONFIGURED))
        assertTrue(states.contains(AiAvailabilityState.AVAILABLE))
        assertTrue(states.contains(AiAvailabilityState.OFFLINE))
        assertTrue(states.contains(AiAvailabilityState.AUTH_REQUIRED))
        assertTrue(states.contains(AiAvailabilityState.RATE_LIMITED))
        assertTrue(states.contains(AiAvailabilityState.UNAVAILABLE))
        assertTrue(states.contains(AiAvailabilityState.ERROR))
    }

    @Test
    fun aiClassificationOutput_properties() {
        val output = AiClassificationOutput(
            category = MailCategory.RECEIPTS_ORDERS,
            confidence = 0.80f,
            explanation = "AI suggestion: Detected receipt keywords",
            evidence = "Order #1234",
        )
        assertEquals(MailCategory.RECEIPTS_ORDERS, output.category)
        assertEquals(0.80f, output.confidence, 0.001f)
        assertEquals("AI suggestion: Detected receipt keywords", output.explanation)
        assertEquals("Order #1234", output.evidence)
    }

    @Test
    fun aiTemporalOutput_properties() {
        val output = AiTemporalOutput(
            itemType = "DEADLINE",
            title = "Tax filing",
            timestampEpochMs = 1700000000000L,
            confidence = 0.75f,
            explanation = "AI suggestion: Due date mentioned",
        )
        assertEquals("DEADLINE", output.itemType)
        assertEquals("Tax filing", output.title)
        assertEquals(1700000000000L, output.timestampEpochMs)
    }

    @Test
    fun aiThreadSummaryOutput_properties() {
        val output = AiThreadSummaryOutput(
            summary = "Discussion about upcoming sprint goals.",
            keyPoints = listOf("Point 1", "Point 2"),
            confidence = 0.70f,
        )
        assertEquals("Discussion about upcoming sprint goals.", output.summary)
        assertEquals(2, output.keyPoints.size)
        assertEquals(0.70f, output.confidence, 0.001f)
    }
}
