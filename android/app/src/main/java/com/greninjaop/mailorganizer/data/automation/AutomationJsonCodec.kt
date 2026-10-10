package com.greninjaop.mailorganizer.data.automation

import com.greninjaop.mailorganizer.core.automation.AutomationAction
import com.greninjaop.mailorganizer.core.automation.AutomationActionType
import com.greninjaop.mailorganizer.core.automation.AutomationCondition
import com.greninjaop.mailorganizer.core.automation.AutomationConditionField
import com.greninjaop.mailorganizer.core.automation.AutomationConditionGroup
import com.greninjaop.mailorganizer.core.automation.AutomationOperator
import com.greninjaop.mailorganizer.core.automation.ConditionGroupLogic

/**
 * Hand-rolled, zero-dependency JSON codecs for Automation models (Phase 27).
 * Pure Kotlin, zero reflection, resilient to malformed input, safe from Android mock stubs.
 */
object AutomationJsonCodec {

    fun conditionGroupToJson(group: AutomationConditionGroup): String {
        val sb = StringBuilder("{")
        sb.append("\"logic\":").append(jsonString(group.logic.name)).append(',')
        sb.append("\"conditions\":[")
        group.conditions.forEachIndexed { index, c ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"field\":").append(jsonString(c.field.name)).append(',')
            sb.append("\"operator\":").append(jsonString(c.operator.name)).append(',')
            sb.append("\"value\":").append(jsonString(c.value))
            sb.append('}')
        }
        sb.append("]}")
        return sb.toString()
    }

    fun conditionGroupFromJson(jsonString: String?): AutomationConditionGroup {
        if (jsonString.isNullOrBlank()) return AutomationConditionGroup()
        return try {
            val parser = SimpleJsonParser(jsonString)
            val root = parser.parseValue() as? Map<*, *> ?: return AutomationConditionGroup()
            val logicStr = root["logic"] as? String ?: ConditionGroupLogic.ALL.name
            val logic = runCatching { ConditionGroupLogic.valueOf(logicStr) }.getOrDefault(ConditionGroupLogic.ALL)
            val condsList = root["conditions"] as? List<*> ?: emptyList<Any>()
            val conditions = condsList.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val fieldStr = map["field"] as? String ?: return@mapNotNull null
                val field = runCatching { AutomationConditionField.valueOf(fieldStr) }.getOrNull() ?: return@mapNotNull null
                val opStr = map["operator"] as? String ?: AutomationOperator.EQUALS.name
                val operator = runCatching { AutomationOperator.valueOf(opStr) }.getOrDefault(AutomationOperator.EQUALS)
                val value = map["value"] as? String ?: ""
                AutomationCondition(field, operator, value)
            }
            AutomationConditionGroup(logic, conditions)
        } catch (_: Exception) {
            AutomationConditionGroup()
        }
    }

    fun actionsToJson(actions: List<AutomationAction>): String {
        if (actions.isEmpty()) return "[]"
        val sb = StringBuilder("[")
        actions.forEachIndexed { index, a ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"type\":").append(jsonString(a.type.name))
            if (a.parameter != null) {
                sb.append(',').append("\"parameter\":").append(jsonString(a.parameter))
            }
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    fun actionsFromJson(jsonString: String?): List<AutomationAction> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val parser = SimpleJsonParser(jsonString)
            val list = parser.parseValue() as? List<*> ?: return emptyList()
            list.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val typeStr = map["type"] as? String ?: return@mapNotNull null
                val type = runCatching { AutomationActionType.valueOf(typeStr) }.getOrNull() ?: return@mapNotNull null
                val param = map["parameter"] as? String
                AutomationAction(type, param)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun jsonString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class SimpleJsonParser(private val s: String) {
        private var i = 0

        fun parseValue(): Any? {
            skipWs()
            if (i >= s.length) return null
            return when (peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                'n' -> parseNull()
                't', 'f' -> parseBoolean()
                else -> parseNumberOrRaw()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = mutableMapOf<String, Any?>()
            skipWs()
            if (peek() == '}') {
                i++
                return map
            }
            while (true) {
                val key = parseString()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWs()
                when (peek()) {
                    ',' -> { i++; skipWs() }
                    '}' -> { i++; return map }
                    else -> throw IllegalArgumentException("bad object")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWs()
            if (peek() == ']') {
                i++
                return list
            }
            while (true) {
                val value = parseValue()
                list.add(value)
                skipWs()
                when (peek()) {
                    ',' -> { i++; skipWs() }
                    ']' -> { i++; return list }
                    else -> throw IllegalArgumentException("bad array")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) throw IllegalArgumentException("bad escape")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw IllegalArgumentException("bad unicode")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw IllegalArgumentException("unterminated string")
        }

        private fun parseNull(): Any? {
            if (s.startsWith("null", i)) {
                i += 4
                return null
            }
            throw IllegalArgumentException("bad null")
        }

        private fun parseBoolean(): Boolean {
            if (s.startsWith("true", i)) {
                i += 4
                return true
            }
            if (s.startsWith("false", i)) {
                i += 5
                return false
            }
            throw IllegalArgumentException("bad boolean")
        }

        private fun parseNumberOrRaw(): String {
            val start = i
            while (i < s.length && !s[i].isWhitespace() && s[i] != ',' && s[i] != '}' && s[i] != ']') {
                i++
            }
            return s.substring(start, i)
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun peek(): Char {
            skipWs()
            if (i >= s.length) throw IllegalArgumentException("unexpected end")
            return s[i]
        }

        private fun expect(c: Char) {
            skipWs()
            if (i >= s.length || s[i] != c) throw IllegalArgumentException("expected $c")
            i++
        }
    }
}
