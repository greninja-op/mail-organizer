package com.greninjaop.mailorganizer.data.ai

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.core.ai.AiProviderInfo
import com.greninjaop.mailorganizer.core.ai.AiRawResponse
import com.greninjaop.mailorganizer.core.ai.AiRequest

/**
 * Privacy-first on-device local fallback provider (Phase 26 §32, §33).
 *
 * Runs locally on-device without remote network calls.
 * Uses bounded lightweight local heuristics to resolve ambiguous edge cases.
 */
class LocalRuleAiProvider(
    override val providerId: AiProviderId = PROVIDER_ID,
) : AiProvider {

    companion object {
        val PROVIDER_ID = AiProviderId("local_heuristic")
    }

    override val info: AiProviderInfo = AiProviderInfo(
        id = providerId,
        displayName = "Local Heuristic Assistant",
        description = "Runs purely on-device without external data transmission",
        isRemote = false,
        supportedCapabilities = setOf(
            AiCapability.CLASSIFY_EMAIL,
            AiCapability.SUMMARIZE_THREAD,
            AiCapability.DETECT_INTENT,
        ),
    )

    override suspend fun checkAvailability(): AiAvailabilityState = AiAvailabilityState.AVAILABLE

    override suspend fun execute(request: AiRequest): MoResult<AiRawResponse> {
        val context = request.minimalContext
        val text = "${context.subject} ${context.snippet}".lowercase()

        val json = when (request.capability) {
            AiCapability.CLASSIFY_EMAIL -> {
                val proposedCategory = when {
                    text.contains("order") || text.contains("shipped") || text.contains("receipt") -> "RECEIPTS_ORDERS"
                    text.contains("interview") || text.contains("application") || text.contains("resume") -> "CAREER"
                    text.contains("course") || text.contains("assignment") || text.contains("grade") -> "EDUCATION"
                    text.contains("verify") || text.contains("password") || text.contains("security") -> "SECURITY"
                    text.contains("sale") || text.contains("discount") || text.contains("offer") -> "PROMOTIONS"
                    text.contains("newsletter") || text.contains("digest") || text.contains("edition") -> "NEWSLETTERS"
                    text.contains("alert") || text.contains("update") || text.contains("notification") -> "NOTIFICATIONS"
                    else -> "IMPORTANT"
                }
                """{
  "category": "$proposedCategory",
  "confidence": 0.65,
  "explanation": "Local analysis identified $proposedCategory markers in message text",
  "evidence": "${context.senderDomain}"
}"""
            }
            AiCapability.SUMMARIZE_THREAD -> {
                val subjectClean = context.subject.ifBlank { "Conversation" }
                """{
  "summary": "Thread regarding $subjectClean with sender domain ${context.senderDomain}.",
  "keyPoints": ["Recent activity in thread", "Latest status updated"],
  "confidence": 0.60
}"""
            }
            AiCapability.DETECT_INTENT -> {
                """{
  "intentDescription": "Advisory correspondence regarding ${context.subject}.",
  "confidence": 0.55,
  "explanation": "Local pattern evaluation"
}"""
            }
            else -> {
                return MoResult.Failure(MoError.InvalidConfiguration("Capability not supported by local provider"))
            }
        }

        return MoResult.Success(
            AiRawResponse(
                rawPayload = json,
                modelId = "local-heuristic-v1",
                processingTimeMs = 5L,
            )
        )
    }

    override fun cancel(requestId: String) {
        // Instant synchronous execution
    }
}
