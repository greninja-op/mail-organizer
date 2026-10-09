package com.greninjaop.mailorganizer.domain.temporal

import com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal
import com.greninjaop.mailorganizer.core.temporal.TemporalConfidence
import com.greninjaop.mailorganizer.core.temporal.TemporalItemType
import com.greninjaop.mailorganizer.core.temporal.TemporalStatus
import com.greninjaop.mailorganizer.core.temporal.TimezoneSource
import com.greninjaop.mailorganizer.data.local.ExtractedItemRecord
import com.greninjaop.mailorganizer.data.local.ExtractedItemType

/**
 * Hand-rolled JSON codec for temporal payloads (Phase 13).
 *
 * No kotlinx.serialization in the data path (same rule as Phase 12's
 * RuleJson): the payload is a small, versioned object; the codec is total
 * in both directions and never throws.
 *
 * Stored fields: end (epoch ms), dateOnly, tz, tzSource, loc, url, status,
 * conf, signals (comma-joined stable names), expl, ver.
 */
internal object TemporalPayloadJson {

    fun encode(item: ExtractedTemporal): String = buildString {
        append("{")
        field("end", item.endEpochMs?.toString() ?: "")
        field("dateOnly", if (item.isDateOnly) "1" else "0")
        field("tz", item.timezoneId)
        field("tzSource", item.timezoneSource.name)
        field("loc", item.location ?: "")
        field("url", item.meetingUrl ?: "")
        field("status", item.status.name)
        field("conf", item.confidence.name)
        field("signals", item.signals.joinToString(",") { it.name })
        field("expl", item.explanation)
        field("ver", item.version.toString())
        append("}")
    }.let {
        // Remove the trailing comma before the closing brace.
        it.replace(",}", "}")
    }

    private fun StringBuilder.field(key: String, value: String) {
        append("\"").append(key).append("\":\"")
        append(value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n"))
        append("\",")
    }

    /** Decoded payload; null when the JSON is malformed or version-mismatched. */
    data class Decoded(
        val endEpochMs: Long?,
        val isDateOnly: Boolean,
        val timezoneId: String,
        val timezoneSource: TimezoneSource,
        val location: String?,
        val meetingUrl: String?,
        val status: TemporalStatus,
        val confidence: TemporalConfidence,
        val signalNames: List<String>,
        val explanation: String,
        val version: Int,
    )

    fun decode(json: String?): Decoded? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val map = mutableMapOf<String, String>()
            val body = json.trim().removePrefix("{").removeSuffix("}")
            var i = 0
            while (i < body.length) {
                // "key":"value" with \" and \\ escapes.
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
                endEpochMs = map["end"]?.takeIf { it.isNotEmpty() }?.toLong(),
                isDateOnly = map["dateOnly"] == "1",
                timezoneId = map["tz"].orEmpty(),
                timezoneSource = map["tzSource"]?.let { TimezoneSource.valueOf(it) }
                    ?: TimezoneSource.UNKNOWN,
                location = map["loc"]?.takeIf { it.isNotEmpty() },
                meetingUrl = map["url"]?.takeIf { it.isNotEmpty() },
                status = map["status"]?.let { TemporalStatus.valueOf(it) }
                    ?: TemporalStatus.UNKNOWN,
                confidence = map["conf"]?.let { TemporalConfidence.valueOf(it) }
                    ?: TemporalConfidence.UNKNOWN,
                signalNames = map["signals"]?.split(",")
                    ?.filter { it.isNotEmpty() }.orEmpty(),
                explanation = map["expl"].orEmpty(),
                version = map["ver"]?.toIntOrNull() ?: 0,
            )
        }.getOrNull()
    }

    private fun nextString(s: String, from: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    else -> sb.append(s[i + 1])
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

/**
 * Core temporal type → storage enum. Lives in domain (not core) so
 * `core.temporal` keeps zero data-layer dependencies — the same boundary
 * pattern as Phase 7's `toMailCategory` and Phase 9's `toPriority`.
 */
fun TemporalItemType.toExtractedItemType(): ExtractedItemType = when (this) {
    TemporalItemType.DEADLINE -> ExtractedItemType.DEADLINE
    TemporalItemType.MEETING -> ExtractedItemType.MEETING
    TemporalItemType.APPOINTMENT -> ExtractedItemType.APPOINTMENT
    TemporalItemType.EVENT -> ExtractedItemType.EVENT
    TemporalItemType.INTERVIEW -> ExtractedItemType.INTERVIEW
    TemporalItemType.SUBMISSION_DEADLINE -> ExtractedItemType.SUBMISSION_DEADLINE
    TemporalItemType.APPLICATION_DEADLINE -> ExtractedItemType.APPLICATION_DEADLINE
    TemporalItemType.PAYMENT_DEADLINE -> ExtractedItemType.PAYMENT_DEADLINE
    TemporalItemType.REGISTRATION_DEADLINE -> ExtractedItemType.REGISTRATION_DEADLINE
    TemporalItemType.REMINDER_DATE -> ExtractedItemType.REMINDER_DATE
    TemporalItemType.DATE_ONLY -> ExtractedItemType.DATE_ONLY
    TemporalItemType.TIME_ONLY -> ExtractedItemType.TIME_ONLY
    TemporalItemType.DATE_TIME -> ExtractedItemType.DATE_TIME
    TemporalItemType.DATE_RANGE -> ExtractedItemType.DATE_RANGE
}

/** Rebuilds a domain item from its stored record + payload. */
fun ExtractedItemRecord.toExtractedTemporal(): ExtractedTemporal? {
    val payload = TemporalPayloadJson.decode(payload) ?: return null
    val type = itemType.toTemporalItemType() ?: return null
    return ExtractedTemporal(
        type = type,
        title = title,
        startEpochMs = dueDateEpochMs ?: return null,
        endEpochMs = payload.endEpochMs,
        isDateOnly = payload.isDateOnly,
        timezoneId = payload.timezoneId,
        timezoneSource = payload.timezoneSource,
        location = payload.location,
        meetingUrl = payload.meetingUrl,
        status = payload.status,
        confidence = payload.confidence,
        signals = payload.signalNames.map {
            com.greninjaop.mailorganizer.core.temporal.TemporalSignal(it, it)
        },
        version = payload.version,
        extractedAtEpochMs = detectedAtEpochMs,
        explanation = payload.explanation,
    )
}

fun ExtractedItemType.toTemporalItemType(): TemporalItemType? = when (this) {
    ExtractedItemType.MEETING -> TemporalItemType.MEETING
    ExtractedItemType.DEADLINE -> TemporalItemType.DEADLINE
    ExtractedItemType.PAYMENT -> TemporalItemType.PAYMENT_DEADLINE
    ExtractedItemType.APPOINTMENT -> TemporalItemType.APPOINTMENT
    ExtractedItemType.APPLICATION -> TemporalItemType.APPLICATION_DEADLINE
    ExtractedItemType.REMINDER -> TemporalItemType.REMINDER_DATE
    ExtractedItemType.EVENT -> TemporalItemType.EVENT
    ExtractedItemType.INTERVIEW -> TemporalItemType.INTERVIEW
    ExtractedItemType.SUBMISSION_DEADLINE -> TemporalItemType.SUBMISSION_DEADLINE
    ExtractedItemType.APPLICATION_DEADLINE -> TemporalItemType.APPLICATION_DEADLINE
    ExtractedItemType.PAYMENT_DEADLINE -> TemporalItemType.PAYMENT_DEADLINE
    ExtractedItemType.REGISTRATION_DEADLINE -> TemporalItemType.REGISTRATION_DEADLINE
    ExtractedItemType.REMINDER_DATE -> TemporalItemType.REMINDER_DATE
    ExtractedItemType.DATE_ONLY -> TemporalItemType.DATE_ONLY
    ExtractedItemType.TIME_ONLY -> TemporalItemType.TIME_ONLY
    ExtractedItemType.DATE_TIME -> TemporalItemType.DATE_TIME
    ExtractedItemType.DATE_RANGE -> TemporalItemType.DATE_RANGE
    // Legacy generic types with no temporal equivalent.
    ExtractedItemType.TRAVEL, ExtractedItemType.REPLY_REQUIRED -> null
}
