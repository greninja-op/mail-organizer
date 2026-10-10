package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.data.local.MailCategory

/**
 * Strict schema validation, sanitization, and domain safety engine for AI output (Phase 26 §16–§19, §54, §60, §115).
 *
 * Principles:
 * - NEVER trust raw model output.
 * - Strict enum matching; invalid enum values fail closed.
 * - Confidence is capped at [MAX_AI_CONFIDENCE] (0.85) — an LLM cannot assert certainty.
 * - Explanations must remain advisory ("AI suggestion: ...").
 * - Action command attempts ("delete this", "send email") are strictly rejected.
 * - Security category protection: AI can never downgrade a deterministic SECURITY signal.
 */
object AiOutputValidator {

    const val MAX_AI_CONFIDENCE = 0.85f
    const val MIN_AI_CONFIDENCE = 0.20f

    private val FORBIDDEN_ACTION_KEYWORDS = listOf(
        "delete email",
        "delete message",
        "trash mailbox",
        "send email",
        "send message",
        "forward email",
        "format disk",
        "drop table",
        "exec(",
        "system(",
    )

    /**
     * Validates a raw JSON payload from a provider into an [AiClassificationOutput].
     *
     * @param rawJson the raw model response string.
     * @param deterministicCandidate the category proposed by deterministic rules, if any.
     */
    fun validateClassification(
        rawJson: String,
        deterministicCandidate: MailCategory? = null,
    ): MoResult<AiClassificationOutput> {
        val parsed = parseJsonObject(rawJson)
            ?: return MoResult.Failure(MoError.Parsing("Malformed JSON from AI provider"))

        val categoryStr = parsed["category"]?.trim()?.uppercase()
            ?: return MoResult.Failure(MoError.Parsing("Missing category field in AI response"))

        val category = try {
            MailCategory.valueOf(categoryStr)
        } catch (_: IllegalArgumentException) {
            return MoResult.Failure(MoError.Parsing("Unknown category '$categoryStr' in AI response"))
        }

        if (category == MailCategory.UNCLASSIFIED) {
            return MoResult.Failure(MoError.Parsing("AI returned UNCLASSIFIED; fallback has no recommendation"))
        }

        // Security Shield (§60, §115): AI cannot downgrade a deterministic security classification
        val effectiveCategory = if (deterministicCandidate == MailCategory.SECURITY && category != MailCategory.SECURITY) {
            MailCategory.SECURITY
        } else {
            category
        }

        val rawConfidence = parsed["confidence"]?.toFloatOrNull() ?: 0.5f
        val confidence = rawConfidence.coerceIn(MIN_AI_CONFIDENCE, MAX_AI_CONFIDENCE)

        val rawExplanation = parsed["explanation"]?.trim() ?: "AI assistance suggests ${effectiveCategory.name}"
        if (containsForbiddenActions(rawExplanation)) {
            return MoResult.Failure(MoError.Parsing("AI response contained prohibited action command"))
        }

        val formattedExplanation = if (rawExplanation.startsWith("AI", ignoreCase = true)) {
            rawExplanation
        } else {
            "AI suggestion: $rawExplanation"
        }

        val evidence = parsed["evidence"]?.trim()?.takeIf { it.isNotBlank() && it != "null" }
        if (evidence != null && containsForbiddenActions(evidence)) {
            return MoResult.Failure(MoError.Parsing("AI evidence contained prohibited action command"))
        }

        return MoResult.Success(
            AiClassificationOutput(
                category = effectiveCategory,
                confidence = confidence,
                explanation = formattedExplanation.take(200),
                evidence = evidence?.take(200),
            )
        )
    }

    /**
     * Validates a raw JSON payload into an [AiThreadSummaryOutput].
     */
    fun validateThreadSummary(rawJson: String): MoResult<AiThreadSummaryOutput> {
        val parsed = parseJsonObject(rawJson)
            ?: return MoResult.Failure(MoError.Parsing("Malformed JSON from AI provider"))

        val summary = parsed["summary"]?.trim()
            ?: return MoResult.Failure(MoError.Parsing("Missing summary field in AI response"))

        if (summary.isBlank() || containsForbiddenActions(summary)) {
            return MoResult.Failure(MoError.Parsing("Invalid or prohibited summary text"))
        }

        val rawConfidence = parsed["confidence"]?.toFloatOrNull() ?: 0.6f
        val confidence = rawConfidence.coerceIn(MIN_AI_CONFIDENCE, MAX_AI_CONFIDENCE)

        // Parse key points from comma-separated or simple array if present
        val keyPoints = parseStringList(parsed["keyPoints"])
            .filter { it.isNotBlank() && !containsForbiddenActions(it) }
            .take(5)
            .map { it.take(120) }

        return MoResult.Success(
            AiThreadSummaryOutput(
                summary = summary.take(400),
                keyPoints = keyPoints,
                confidence = confidence,
            )
        )
    }

    /**
     * Validates a raw JSON payload into an [AiTemporalOutput].
     */
    fun validateTemporal(rawJson: String): MoResult<AiTemporalOutput> {
        val parsed = parseJsonObject(rawJson)
            ?: return MoResult.Failure(MoError.Parsing("Malformed JSON from AI provider"))

        val itemType = parsed["itemType"]?.trim()?.uppercase()
            ?: return MoResult.Failure(MoError.Parsing("Missing itemType in AI temporal response"))

        if (itemType != "DEADLINE" && itemType != "MEETING") {
            return MoResult.Failure(MoError.Parsing("Invalid temporal itemType '$itemType'"))
        }

        val title = parsed["title"]?.trim()
            ?: return MoResult.Failure(MoError.Parsing("Missing title in AI temporal response"))

        if (title.isBlank() || containsForbiddenActions(title)) {
            return MoResult.Failure(MoError.Parsing("Prohibited or empty temporal title"))
        }

        val timestamp = parsed["timestampEpochMs"]?.toLongOrNull() ?: 0L
        val rawConfidence = parsed["confidence"]?.toFloatOrNull() ?: 0.5f
        val confidence = rawConfidence.coerceIn(MIN_AI_CONFIDENCE, MAX_AI_CONFIDENCE)

        val explanation = parsed["explanation"]?.trim() ?: "AI identified potential $itemType"
        if (containsForbiddenActions(explanation)) {
            return MoResult.Failure(MoError.Parsing("Prohibited action keyword in explanation"))
        }

        return MoResult.Success(
            AiTemporalOutput(
                itemType = itemType,
                title = title.take(60),
                timestampEpochMs = timestamp,
                confidence = confidence,
                explanation = "AI suggestion: $explanation".take(200),
            )
        )
    }

    private fun containsForbiddenActions(text: String): Boolean {
        val lower = text.lowercase()
        return FORBIDDEN_ACTION_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Lightweight, total JSON object parser for flat key-value pairs and simple arrays.
     */
    private fun parseJsonObject(json: String): Map<String, String>? {
        val trimmed = json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

        val result = mutableMapOf<String, String>()
        var i = 1
        val len = trimmed.length - 1

        while (i < len) {
            // Find key
            while (i < len && trimmed[i] != '"') i++
            if (i >= len) break
            i++ // skip opening quote
            val keyStart = i
            while (i < len && trimmed[i] != '"') {
                if (trimmed[i] == '\\') i++ // escape
                i++
            }
            if (i >= len) break
            val key = trimmed.substring(keyStart, i)
            i++ // skip closing quote

            // Find colon
            while (i < len && trimmed[i] != ':') i++
            if (i >= len) break
            i++ // skip colon

            // Skip whitespace
            while (i < len && trimmed[i].isWhitespace()) i++
            if (i >= len) break

            // Parse value
            val value: String
            if (trimmed[i] == '"') {
                i++ // skip opening quote
                val valStart = i
                while (i < len && trimmed[i] != '"') {
                    if (trimmed[i] == '\\') i++
                    i++
                }
                value = trimmed.substring(valStart, i)
                if (i < len) i++ // skip closing quote
            } else if (trimmed[i] == '[') {
                val arrayStart = i
                var bracketCount = 1
                i++
                while (i < len && bracketCount > 0) {
                    if (trimmed[i] == '[') bracketCount++
                    else if (trimmed[i] == ']') bracketCount--
                    i++
                }
                value = trimmed.substring(arrayStart, i)
            } else {
                // number or boolean or null
                val valStart = i
                while (i < len && trimmed[i] != ',' && trimmed[i] != '}') i++
                value = trimmed.substring(valStart, i).trim()
            }

            result[key] = value

            // Find next comma or end
            while (i < len && trimmed[i] != ',') i++
            if (i < len && trimmed[i] == ',') i++
        }

        return result
    }

    private fun parseStringList(rawArray: String?): List<String> {
        if (rawArray.isNullOrBlank()) return emptyList()
        val trimmed = rawArray.trim().removePrefix("[").removeSuffix("]").trim()
        if (trimmed.isEmpty()) return emptyList()

        val list = mutableListOf<String>()
        val regex = Regex(""""([^"\\]*(\\.[^"\\]*)*)"""")
        val matches = regex.findAll(trimmed)
        for (m in matches) {
            list.add(m.groupValues[1])
        }
        if (list.isEmpty()) {
            // fallback: split by comma
            return trimmed.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
        }
        return list
    }
}
