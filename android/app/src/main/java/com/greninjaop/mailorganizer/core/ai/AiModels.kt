package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.Priority

/**
 * Capability-based AI classification (Phase 26 §11).
 * AI providers advertise only the capabilities they support.
 */
enum class AiCapability {
    /** Fallback classification for ambiguous or unclassified emails. */
    CLASSIFY_EMAIL,

    /** Complex deadline extraction when deterministic regexes find ambiguity. */
    EXTRACT_DEADLINE,

    /** Complex meeting extraction when deterministic parsing finds ambiguity. */
    EXTRACT_MEETING,

    /** On-demand, user-initiated thread summarization. */
    SUMMARIZE_THREAD,

    /** Advisory conversation intent resolution for ambiguous threads. */
    DETECT_INTENT,
}

/**
 * Availability state model for AI subsystem and providers (Phase 26 §8).
 */
enum class AiAvailabilityState {
    /** AI is disabled by user setting (default state). */
    DISABLED,

    /** AI is enabled but no provider or credentials are configured. */
    NOT_CONFIGURED,

    /** Provider is configured, connected, and ready for requests. */
    AVAILABLE,

    /** Device is offline; fallback to deterministic intelligence. */
    OFFLINE,

    /** Authentication or API key is missing or invalid. */
    AUTH_REQUIRED,

    /** Provider rate limits exceeded; temporary backoff. */
    RATE_LIMITED,

    /** Provider is temporarily unreachable or service down. */
    UNAVAILABLE,

    /** Terminal or unrecognized provider error. */
    ERROR,
}

/**
 * Identifier for an AI provider (Phase 26 §10).
 */
@JvmInline
value class AiProviderId(val value: String) {
    override fun toString(): String = value
}

/**
 * Metadata descriptor for an AI provider (Phase 26 §10).
 */
data class AiProviderInfo(
    val id: AiProviderId,
    val displayName: String,
    val description: String,
    val isRemote: Boolean,
    val supportedCapabilities: Set<AiCapability>,
)

/**
 * Data-minimized context for an AI fallback request (Phase 26 §24, §25, §26, §40).
 * Strips all OAuth tokens, full recipient addresses, attachments, and raw HTML.
 */
data class AiMinimalContext(
    val subject: String,
    val senderDomain: String,
    val snippet: String,
    val timestampEpochMs: Long,
)

/**
 * Structured internal request for AI fallback (Phase 26 §40).
 * Strictly account-scoped and capability-specific.
 */
data class AiRequest(
    val capability: AiCapability,
    val accountId: String,
    val sourceMessageIds: List<String>,
    val sourceThreadId: String?,
    val minimalContext: AiMinimalContext,
    val schemaVersion: Int = 1,
    val privacyPolicyAccepted: Boolean = true,
)

/**
 * Marker interface for structured, validated AI outputs (Phase 26 §17).
 */
sealed interface AiStructuredOutput

/**
 * Structured output for email classification fallback (Phase 26 §17).
 */
data class AiClassificationOutput(
    val category: MailCategory,
    val confidence: Float,
    val explanation: String,
    val evidence: String? = null,
) : AiStructuredOutput

/**
 * Structured output for temporal item extraction fallback (Phase 26 §17, §64, §65).
 */
data class AiTemporalOutput(
    val itemType: String,
    val title: String,
    val timestampEpochMs: Long,
    val confidence: Float,
    val explanation: String,
) : AiStructuredOutput

/**
 * Structured output for on-demand thread summarization (Phase 26 §57).
 */
data class AiThreadSummaryOutput(
    val summary: String,
    val keyPoints: List<String>,
    val confidence: Float,
) : AiStructuredOutput

/**
 * Structured output for conversation intent detection fallback (Phase 26 §66).
 */
data class AiIntentOutput(
    val intentDescription: String,
    val confidence: Float,
    val explanation: String,
) : AiStructuredOutput

/**
 * Structured, validated internal AI result with provenance (Phase 26 §41, §102).
 */
data class AiResult<out T : AiStructuredOutput>(
    val capability: AiCapability,
    val providerId: AiProviderId,
    val modelId: String,
    val sourceId: String,
    val accountId: String,
    val output: T,
    val confidence: Float,
    val processingTimeMs: Long,
    val createdAtEpochMs: Long,
    val version: Int = 1,
)

/**
 * Raw response envelope from an AI provider before schema validation (Phase 26 §16).
 */
data class AiRawResponse(
    val rawPayload: String,
    val modelId: String,
    val processingTimeMs: Long,
    val metadata: Map<String, String> = emptyMap(),
)
