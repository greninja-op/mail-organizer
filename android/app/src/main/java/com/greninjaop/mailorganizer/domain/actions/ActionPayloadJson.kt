package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.core.actions.ActionSignal

/**
 * Hand-rolled JSON codec for action-item payloads (Phase 14).
 *
 * Same rule as Phase 12's RuleJson and Phase 13's TemporalPayloadJson: no
 * kotlinx.serialization in the data path. The payload is a small,
 * versioned object; the codec is total in both directions and never throws.
 *
 * Stored fields: ver (generator version), signals (comma-joined stable
 * names), missing (comma-joined missing-info items), targetKey (dedup key
 * suffix), expl (redundant with the typed column — kept for audit).
 */
object ActionPayloadJson {

    data class Decoded(
        val version: Int,
        val signalNames: List<String>,
        val missingInfo: List<String>,
        val targetKey: String,
    )

    fun encode(
        version: Int,
        signals: List<ActionSignal>,
        missingInfo: List<String>,
        targetKey: String,
    ): String = buildString {
        append("{")
        field("ver", version.toString())
        field("signals", signals.joinToString(",") { it.name })
        field("missing", missingInfo.joinToString(","))
        field("targetKey", targetKey)
        append("}")
    }.replace(",}", "}")

    fun decode(json: String?): Decoded? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val map = mutableMapOf<String, String>()
            val body = json.trim().removePrefix("{").removeSuffix("}")
            var i = 0
            while (i < body.length) {
                require(body[i] == '"') { "key" }
                val ke = nextString(body, i + 1)
                i = ke.second
                require(body[i] == ':') { "colon" }
                require(body[i + 1] == '"') { "value" }
                val ve = nextString(body, i + 2)
                map[ke.first] = ve.first
                i = ve.second
                if (i < body.length && body[i] == ',') i++
            }
            Decoded(
                version = map["ver"]?.toIntOrNull() ?: 0,
                signalNames = map["signals"]?.split(",").orEmpty()
                    .filter { it.isNotBlank() },
                missingInfo = map["missing"]?.split(",").orEmpty()
                    .filter { it.isNotBlank() },
                targetKey = map["targetKey"].orEmpty(),
            )
        }.getOrNull()
    }

    private fun StringBuilder.field(key: String, value: String) {
        append("\"").append(key).append("\":\"")
        append(
            value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n"),
        )
        append("\",")
    }

    /** Returns (decoded string, index after closing quote). */
    private fun nextString(body: String, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var i = start
        while (i < body.length) {
            val c = body[i]
            if (c == '\\' && i + 1 < body.length) {
                when (body[i + 1]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    else -> sb.append(body[i + 1])
                }
                i += 2
            } else if (c == '"') {
                return sb.toString() to i + 1
            } else {
                sb.append(c)
                i++
            }
        }
        throw IllegalArgumentException("unterminated string")
    }
}
