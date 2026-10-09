package com.greninjaop.mailorganizer.core.rules

/**
 * Minimal JSON codecs for rule conditions/actions (Phase 12).
 *
 * Rules are stored as JSON string columns (see `MoConverters` rationale:
 * conditions/actions are read-with-rule metadata, never query predicates).
 *
 * Deliberately dependency-free (no kotlinx.serialization in the catalog),
 * following Phase 5's `AttachmentMetaJson` pattern: fixed flat shapes, a
 * small hand-rolled codec, total in both directions — malformed input
 * decodes to an empty list rather than throwing (a corrupt rule row must
 * never crash rule evaluation).
 *
 * Wire shapes:
 * `[{"field":"SENDER_DOMAIN","operator":"EQUALS","value":"example.com"}]`
 * `[{"type":"SET_CATEGORY","value":"CAREER"}]`
 */
object RuleJson {

    // ---- Conditions ----

    fun encodeConditions(conditions: List<RuleCondition>): String {
        if (conditions.isEmpty()) return "[]"
        val sb = StringBuilder("[")
        conditions.forEachIndexed { index, c ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"field\":").append(jsonString(c.field.name)).append(',')
            sb.append("\"operator\":").append(jsonString(c.operator.name)).append(',')
            sb.append("\"value\":").append(jsonString(c.value))
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    fun decodeConditions(json: String): List<RuleCondition> {
        if (json.isBlank()) return emptyList()
        return try {
            Parser(json).parseArray().mapNotNull { obj ->
                val field = obj["field"]?.let {
                    runCatching { RuleConditionField.valueOf(it) }.getOrNull()
                } ?: return@mapNotNull null
                val operator = obj["operator"]?.let {
                    runCatching { RuleOperator.valueOf(it) }.getOrNull()
                } ?: return@mapNotNull null
                val value = obj["value"] ?: return@mapNotNull null
                runCatching { RuleCondition(field, operator, value) }.getOrNull()
            }.take(RuleCondition.MAX_CONDITIONS_PER_RULE)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---- Actions ----

    fun encodeActions(actions: List<RuleAction>): String {
        if (actions.isEmpty()) return "[]"
        val sb = StringBuilder("[")
        actions.forEachIndexed { index, a ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"type\":").append(jsonString(a.type.name)).append(',')
            sb.append("\"value\":").append(jsonString(a.value))
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    fun decodeActions(json: String): List<RuleAction> {
        if (json.isBlank()) return emptyList()
        return try {
            Parser(json).parseArray().mapNotNull { obj ->
                val type = obj["type"]?.let {
                    runCatching { RuleActionType.valueOf(it) }.getOrNull()
                } ?: return@mapNotNull null
                val value = obj["value"]?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                RuleAction(type, value)
            }.take(RuleAction.MAX_ACTIONS_PER_RULE)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---- Shared string escaping ----

    private fun jsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') {
                    sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /**
     * Minimal parser for arrays of flat string-valued objects.
     * Only what the rule shapes need — not a general JSON parser.
     */
    private class Parser(private val s: String) {
        private var i = 0

        fun parseArray(): List<Map<String, String>> {
            skipWs()
            expect('[')
            val out = mutableListOf<Map<String, String>>()
            skipWs()
            if (peek() == ']') {
                i++
                return out
            }
            while (true) {
                out += parseObject()
                skipWs()
                when (peek()) {
                    ',' -> { i++; skipWs() }
                    ']' -> { i++; return out }
                    else -> throw IllegalArgumentException("bad array")
                }
            }
        }

        private fun parseObject(): Map<String, String> {
            skipWs()
            expect('{')
            val map = mutableMapOf<String, String>()
            skipWs()
            if (peek() == '}') {
                i++
                return map
            }
            while (true) {
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                map[key] = parseString()
                skipWs()
                when (peek()) {
                    ',' -> { i++; skipWs() }
                    '}' -> { i++; return map }
                    else -> throw IllegalArgumentException("bad object")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) throw IllegalArgumentException("unterminated string")
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
                            else -> throw IllegalArgumentException("bad escape")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun peek(): Char {
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
