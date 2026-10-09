package com.greninjaop.mailorganizer.core.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for [RuleEngine] (Phase 12).
 *
 * No Android, no database — the engine is a pure function of
 * (normalized input, rules).
 */
class RuleEngineTest {

    private fun input(
        senderEmail: String = "recruiter@example.com",
        senderDomain: String = "example.com",
        companyId: String? = "co:example.com",
        subject: String = "Senior Android role",
        labelIds: List<String> = listOf("INBOX"),
        gmailCategory: String? = null,
        baseCategory: String? = "PROMOTIONS",
        basePriority: String? = "NORMAL",
        hasAttachment: Boolean = false,
        hasUnsubscribeMarker: Boolean = false,
    ) = RuleMatchInput(
        messageId = "m1",
        accountId = "a1",
        senderEmail = senderEmail,
        senderDomain = senderDomain,
        companyId = companyId,
        subject = subject,
        labelIds = labelIds,
        gmailCategory = gmailCategory,
        baseCategory = baseCategory,
        basePriority = basePriority,
        hasAttachment = hasAttachment,
        hasUnsubscribeMarker = hasUnsubscribeMarker,
    )

    private fun rule(
        id: Long,
        order: Int = 0,
        enabled: Boolean = true,
        conditions: List<RuleCondition> = listOf(
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
        ),
        actions: List<RuleAction> = listOf(
            RuleAction(RuleActionType.SET_CATEGORY, "CAREER"),
        ),
        name: String = "rule-$id",
    ) = EvaluableRule(
        id = id,
        name = name,
        enabled = enabled,
        order = order,
        conditions = conditions,
        actions = actions,
    )

    @Test
    fun `no rules means no match`() {
        val e = RuleEngine.evaluate(input(), emptyList())
        assertTrue(e.matchedRules.isEmpty())
        assertNull(e.effectiveCategory)
        assertNull(e.effectivePriority)
        assertTrue(e.conflicts.isEmpty())
    }

    @Test
    fun `disabled rule never matches`() {
        val e = RuleEngine.evaluate(input(), listOf(rule(1, enabled = false)))
        assertTrue(e.matchedRules.isEmpty())
        assertNull(e.effectiveCategory)
    }

    @Test
    fun `sender email matches case-insensitively`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.SENDER_EMAIL, RuleOperator.EQUALS, "RECRUITER@EXAMPLE.COM"),
            ),
        )
        val e = RuleEngine.evaluate(input(senderEmail = "recruiter@example.com"), listOf(r))
        assertEquals(listOf(r), e.matchedRules)
        assertEquals("CAREER", e.effectiveCategory)
    }

    @Test
    fun `sender email mismatch does not match`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.SENDER_EMAIL, RuleOperator.EQUALS, "other@example.com"),
            ),
        )
        val e = RuleEngine.evaluate(input(), listOf(r))
        assertTrue(e.matchedRules.isEmpty())
    }

    @Test
    fun `subject contains matches case-insensitively`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, "android ROLE"),
            ),
        )
        val e = RuleEngine.evaluate(input(subject = "Senior Android Role at X"), listOf(r))
        assertEquals(1, e.matchedRules.size)
    }

    @Test
    fun `conditions use AND semantics`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
                RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, "invoice"),
            ),
        )
        // Subject does not contain "invoice" — no match despite domain hit.
        val e = RuleEngine.evaluate(input(), listOf(r))
        assertTrue(e.matchedRules.isEmpty())
        val e2 = RuleEngine.evaluate(input(subject = "Your invoice"), listOf(r))
        assertEquals(1, e2.matchedRules.size)
    }

    @Test
    fun `rule with no conditions never matches`() {
        val r = rule(1, conditions = emptyList())
        val e = RuleEngine.evaluate(input(), listOf(r))
        assertTrue(e.matchedRules.isEmpty())
    }

    @Test
    fun `lower order wins over higher order`() {
        val broad = rule(1, order = 10, actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "PROMOTIONS")))
        val specific = rule(2, order = 1, actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "CAREER")))
        val e = RuleEngine.evaluate(input(), listOf(broad, specific))
        assertEquals("CAREER", e.effectiveCategory)
        assertEquals(2L, e.categoryRule!!.id)
    }

    @Test
    fun `order tie breaks by lower id deterministically`() {
        val a = rule(5, order = 0, actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "CAREER")))
        val b = rule(3, order = 0, actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "EDUCATION")))
        // Pass in "wrong" order — result must not depend on input order.
        val e = RuleEngine.evaluate(input(), listOf(a, b))
        assertEquals("EDUCATION", e.effectiveCategory)
        assertEquals(3L, e.categoryRule!!.id)
    }

    @Test
    fun `different action types can come from different rules`() {
        val cat = rule(
            1, order = 5,
            actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "CAREER")),
        )
        val pri = rule(
            2, order = 1,
            actions = listOf(RuleAction(RuleActionType.SET_PRIORITY, "HIGH")),
        )
        val e = RuleEngine.evaluate(input(), listOf(cat, pri))
        assertEquals("CAREER", e.effectiveCategory)
        assertEquals("HIGH", e.effectivePriority)
        assertEquals(1L, e.categoryRule!!.id)
        assertEquals(2L, e.priorityRule!!.id)
    }

    @Test
    fun `first matching rule supplies the action when two rules set the same field`() {
        val first = rule(
            1, order = 1,
            actions = listOf(RuleAction(RuleActionType.SET_PRIORITY, "HIGH")),
        )
        val second = rule(
            2, order = 2,
            actions = listOf(RuleAction(RuleActionType.SET_PRIORITY, "CRITICAL")),
        )
        val e = RuleEngine.evaluate(input(), listOf(first, second))
        assertEquals("HIGH", e.effectivePriority)
        assertEquals(2, e.matchedRules.size)
    }

    @Test
    fun `conflicting rules are reported with the deterministic winner`() {
        val conds = listOf(
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
        )
        val a = rule(1, order = 2, conditions = conds,
            actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "CAREER")))
        val b = rule(2, order = 1, conditions = conds,
            actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "EDUCATION")))
        val e = RuleEngine.evaluate(input(), listOf(a, b))
        assertEquals(1, e.conflicts.size)
        val c = e.conflicts[0]
        assertEquals(RuleActionType.SET_CATEGORY, c.field)
        // A/B are in precedence order (b first: lower order), not input order.
        assertEquals(2L, c.ruleA.id)
        assertEquals(1L, c.ruleB.id)
        assertEquals("EDUCATION", c.valueA)
        assertEquals("CAREER", c.valueB)
        // Lower order wins.
        assertEquals(2L, c.winnerId)
        assertEquals("EDUCATION", e.effectiveCategory)
    }

    @Test
    fun `identical rules do not conflict`() {
        val conds = listOf(
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
        )
        val a = rule(1, conditions = conds,
            actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "CAREER")))
        val b = rule(2, conditions = conds,
            actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "CAREER")))
        val e = RuleEngine.evaluate(input(), listOf(a, b))
        assertTrue(e.conflicts.isEmpty())
    }

    @Test
    fun `boolean conditions match strictly`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.HAS_ATTACHMENT, RuleOperator.EQUALS, "true"),
            ),
            actions = listOf(RuleAction(RuleActionType.SET_PRIORITY, "HIGH")),
        )
        assertTrue(RuleEngine.evaluate(input(hasAttachment = true), listOf(r)).matchedRules.isNotEmpty())
        assertTrue(RuleEngine.evaluate(input(hasAttachment = false), listOf(r)).matchedRules.isEmpty())
        // Garbage boolean value degrades to no-match, never throws.
        val bad = rule(
            2,
            conditions = listOf(
                RuleCondition(RuleConditionField.HAS_ATTACHMENT, RuleOperator.EQUALS, "yes"),
            ),
        )
        assertTrue(RuleEngine.evaluate(input(hasAttachment = true), listOf(bad)).matchedRules.isEmpty())
    }

    @Test
    fun `base category and priority conditions work`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.BASE_CATEGORY, RuleOperator.EQUALS, "promotions"),
            ),
            actions = listOf(RuleAction(RuleActionType.SET_PRIORITY, "LOW")),
        )
        val e = RuleEngine.evaluate(input(baseCategory = "PROMOTIONS"), listOf(r))
        assertEquals("LOW", e.effectivePriority)
        val e2 = RuleEngine.evaluate(input(baseCategory = "CAREER"), listOf(r))
        assertTrue(e2.matchedRules.isEmpty())
    }

    @Test
    fun `gmail label condition matches label ids`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.GMAIL_LABEL, RuleOperator.EQUALS, "spam"),
            ),
            actions = listOf(RuleAction(RuleActionType.SET_CATEGORY, "PROMOTIONS")),
        )
        assertTrue(
            RuleEngine.evaluate(input(labelIds = listOf("INBOX", "SPAM")), listOf(r))
                .matchedRules.isNotEmpty(),
        )
        assertTrue(
            RuleEngine.evaluate(input(labelIds = listOf("INBOX")), listOf(r))
                .matchedRules.isEmpty(),
        )
    }

    @Test
    fun `company id condition matches`() {
        val r = rule(
            1,
            conditions = listOf(
                RuleCondition(RuleConditionField.COMPANY_ID, RuleOperator.EQUALS, "co:example.com"),
            ),
        )
        assertTrue(RuleEngine.evaluate(input(companyId = "co:example.com"), listOf(r)).matchedRules.isNotEmpty())
        assertTrue(RuleEngine.evaluate(input(companyId = null), listOf(r)).matchedRules.isEmpty())
    }

    @Test
    fun `oversized condition list never matches`() {
        val many = (1..(RuleCondition.MAX_CONDITIONS_PER_RULE + 1)).map {
            RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, "x")
        }
        val r = rule(1, conditions = many)
        assertTrue(RuleEngine.evaluate(input(subject = "x x x"), listOf(r)).matchedRules.isEmpty())
    }

    @Test
    fun `condition signature is order-independent`() {
        val a = listOf(
            RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, "invoice"),
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
        )
        val b = listOf(
            RuleCondition(RuleConditionField.SENDER_DOMAIN, RuleOperator.EQUALS, "example.com"),
            RuleCondition(RuleConditionField.SUBJECT, RuleOperator.CONTAINS, "invoice"),
        )
        assertEquals(RuleEngine.conditionSignature(a), RuleEngine.conditionSignature(b))
    }
}
