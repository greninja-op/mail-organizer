package com.greninjaop.mailorganizer.data.automation

import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationActionType
import com.greninjaop.mailorganizer.core.automation.AutomationCondition
import com.greninjaop.mailorganizer.core.automation.AutomationConditionField
import com.greninjaop.mailorganizer.core.automation.AutomationConditionGroup
import com.greninjaop.mailorganizer.core.automation.AutomationOperator
import com.greninjaop.mailorganizer.core.automation.ConditionGroupLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AutomationJsonCodec (Phase 27).
 */
class AutomationJsonCodecTest {

    @Test
    fun conditionGroup_roundTrip() {
        val group = AutomationConditionGroup(
            logic = ConditionGroupLogic.ANY,
            conditions = listOf(
                AutomationCondition(AutomationConditionField.SENDER_DOMAIN, AutomationOperator.EQUALS, "amazon.com"),
                AutomationCondition(AutomationConditionField.SUBJECT, AutomationOperator.CONTAINS, "Order \"Confirmation\" #123"),
                AutomationCondition(AutomationConditionField.IS_ACTION_REQUIRED, AutomationOperator.EQUALS, "true"),
            ),
        )

        val json = AutomationJsonCodec.conditionGroupToJson(group)
        val decoded = AutomationJsonCodec.conditionGroupFromJson(json)

        assertEquals(group.logic, decoded.logic)
        assertEquals(group.conditions.size, decoded.conditions.size)
        assertEquals(group.conditions[0], decoded.conditions[0])
        assertEquals(group.conditions[1], decoded.conditions[1])
        assertEquals(group.conditions[2], decoded.conditions[2])
    }

    @Test
    fun actions_roundTrip() {
        val actions = listOf(
            AutomationAction(AutomationActionType.SET_CATEGORY, "FINANCE"),
            AutomationAction(AutomationActionType.MARK_AS_READ),
            AutomationAction(AutomationActionType.CREATE_LOCAL_REMINDER, "Follow up next week"),
        )

        val json = AutomationJsonCodec.actionsToJson(actions)
        val decoded = AutomationJsonCodec.actionsFromJson(json)

        assertEquals(actions, decoded)
    }

    @Test
    fun emptyCollections_encodeAndDecodeCleanly() {
        val emptyGroup = AutomationConditionGroup()
        val groupJson = AutomationJsonCodec.conditionGroupToJson(emptyGroup)
        val decodedGroup = AutomationJsonCodec.conditionGroupFromJson(groupJson)
        assertEquals(ConditionGroupLogic.ALL, decodedGroup.logic)
        assertTrue(decodedGroup.conditions.isEmpty())

        val emptyActions = emptyList<AutomationAction>()
        val actionsJson = AutomationJsonCodec.actionsToJson(emptyActions)
        val decodedActions = AutomationJsonCodec.actionsFromJson(actionsJson)
        assertTrue(decodedActions.isEmpty())
    }

    @Test
    fun malformedJson_degradesSafely() {
        val badGroup = AutomationJsonCodec.conditionGroupFromJson("{invalid json[}")
        assertEquals(ConditionGroupLogic.ALL, badGroup.logic)
        assertTrue(badGroup.conditions.isEmpty())

        val badActions = AutomationJsonCodec.actionsFromJson("[broken json")
        assertTrue(badActions.isEmpty())

        val nullActions = AutomationJsonCodec.actionsFromJson(null)
        assertTrue(nullActions.isEmpty())
    }
}
