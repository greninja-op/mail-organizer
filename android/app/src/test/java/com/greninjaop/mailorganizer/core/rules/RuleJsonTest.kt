package com.greninjaop.mailorganizer.core.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the hand-rolled rule JSON codecs (Phase 12). */
class RuleJsonTest {

    @Test
    fun `conditions round-trip`() {
        val conditions = listOf(
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
            RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, "say \"hi\" \\ now"),
        )
        val decoded = RuleJson.decodeConditions(RuleJson.encodeConditions(conditions))
        assertEquals(conditions, decoded)
    }

    @Test
    fun `actions round-trip`() {
        val actions = listOf(
            RuleAction(RuleActionType.SET_CATEGORY, "CAREER"),
            RuleAction(RuleActionType.SET_PRIORITY, "HIGH"),
        )
        val decoded = RuleJson.decodeActions(RuleJson.encodeActions(actions))
        assertEquals(actions, decoded)
    }

    @Test
    fun `empty lists encode and decode`() {
        assertEquals("[]", RuleJson.encodeConditions(emptyList()))
        assertEquals("[]", RuleJson.encodeActions(emptyList()))
        assertTrue(RuleJson.decodeConditions("").isEmpty())
        assertTrue(RuleJson.decodeConditions("[]").isEmpty())
        assertTrue(RuleJson.decodeActions("").isEmpty())
    }

    @Test
    fun `malformed json degrades to empty list, never throws`() {
        assertTrue(RuleJson.decodeConditions("{bad json").isEmpty())
        assertTrue(RuleJson.decodeConditions("[{\"field\":1}]").isEmpty())
        assertTrue(RuleJson.decodeActions("null").isEmpty())
        assertTrue(RuleJson.decodeActions("[{\"type\":\"SET_CATEGORY\"}]").isEmpty())
    }

    @Test
    fun `unknown enums are dropped, known ones kept`() {
        val json = "[" +
            "{\"field\":\"SENDER_DOMAIN\",\"operator\":\"EQUALS\",\"value\":\"a.com\"}," +
            "{\"field\":\"NOPE\",\"operator\":\"EQUALS\",\"value\":\"b.com\"}" +
            "]"
        val decoded = RuleJson.decodeConditions(json)
        assertEquals(1, decoded.size)
        assertEquals("a.com", decoded[0].value)
    }

    @Test
    fun `oversized condition values are rejected at construction`() {
        val tooLong = "x".repeat(RuleCondition.MAX_CONDITION_VALUE_CHARS + 1)
        var threw = false
        try {
            RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, tooLong)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }
}
