package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.data.local.MailCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiOutputValidatorTest {

    @Test
    fun validateClassification_validJson_success() {
        val json = """
            {
              "category": "NOTIFICATIONS",
              "confidence": 0.80,
              "explanation": "Service status update",
              "evidence": "Service OK"
            }
        """.trimIndent()

        val result = AiOutputValidator.validateClassification(json)
        assertTrue(result is MoResult.Success)
        val value = (result as MoResult.Success).value
        assertEquals(MailCategory.NOTIFICATIONS, value.category)
        assertEquals(0.80f, value.confidence, 0.001f)
        assertTrue(value.explanation.startsWith("AI suggestion:"))
        assertEquals("Service OK", value.evidence)
    }

    @Test
    fun validateClassification_capsConfidenceAtMax() {
        val json = """
            {
              "category": "RECEIPTS_ORDERS",
              "confidence": 0.99,
              "explanation": "Receipt from store"
            }
        """.trimIndent()

        val result = AiOutputValidator.validateClassification(json)
        assertTrue(result is MoResult.Success)
        val value = (result as MoResult.Success).value
        assertEquals(AiOutputValidator.MAX_AI_CONFIDENCE, value.confidence, 0.001f)
    }

    @Test
    fun validateClassification_securityShield_preventsDowngrade() {
        val json = """
            {
              "category": "NEWSLETTERS",
              "confidence": 0.70,
              "explanation": "Looks like promotional newsletter"
            }
        """.trimIndent()

        // Deterministic candidate was SECURITY
        val result = AiOutputValidator.validateClassification(json, deterministicCandidate = MailCategory.SECURITY)
        assertTrue(result is MoResult.Success)
        val value = (result as MoResult.Success).value
        // Shield prevents downgrading to NEWSLETTERS
        assertEquals(MailCategory.SECURITY, value.category)
    }

    @Test
    fun validateClassification_forbiddenActions_rejected() {
        val json = """
            {
              "category": "PERSONAL",
              "confidence": 0.60,
              "explanation": "Please delete email immediately"
            }
        """.trimIndent()

        val result = AiOutputValidator.validateClassification(json)
        assertTrue(result is MoResult.Failure)
    }

    @Test
    fun validateClassification_unclassified_rejected() {
        val json = """
            {
              "category": "UNCLASSIFIED",
              "confidence": 0.50,
              "explanation": "Not sure"
            }
        """.trimIndent()

        val result = AiOutputValidator.validateClassification(json)
        assertTrue(result is MoResult.Failure)
    }

    @Test
    fun validateThreadSummary_validJson_success() {
        val json = """
            {
              "summary": "Project status is on track.",
              "keyPoints": ["Sprint finished", "QA starting"],
              "confidence": 0.75
            }
        """.trimIndent()

        val result = AiOutputValidator.validateThreadSummary(json)
        assertTrue(result is MoResult.Success)
        val value = (result as MoResult.Success).value
        assertEquals("Project status is on track.", value.summary)
        assertEquals(2, value.keyPoints.size)
    }

    @Test
    fun validateThreadSummary_forbiddenActions_rejected() {
        val json = """
            {
              "summary": "Do trash mailbox right now.",
              "keyPoints": [],
              "confidence": 0.75
            }
        """.trimIndent()

        val result = AiOutputValidator.validateThreadSummary(json)
        assertTrue(result is MoResult.Failure)
    }

    @Test
    fun validateTemporal_validJson_success() {
        val json = """
            {
              "itemType": "DEADLINE",
              "title": "Submit report",
              "timestampEpochMs": 1700000000000,
              "confidence": 0.80,
              "explanation": "Due date clearly indicated"
            }
        """.trimIndent()

        val result = AiOutputValidator.validateTemporal(json)
        assertTrue(result is MoResult.Success)
        val value = (result as MoResult.Success).value
        assertEquals("DEADLINE", value.itemType)
        assertEquals("Submit report", value.title)
    }
}
