package com.greninjaop.mailorganizer.data.ai

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.core.ai.AiProviderInfo
import com.greninjaop.mailorganizer.core.ai.AiRawResponse
import com.greninjaop.mailorganizer.core.ai.AiRequest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Deterministic test and development AI provider (Phase 26 §143).
 *
 * Provides repeatable, synthetic AI responses for unit testing without
 * requiring remote internet access, third-party SDKs, or real API keys.
 */
class FakeTestAiProvider(
    override val providerId: AiProviderId = AiProviderId("fake_test"),
    var simulatedAvailability: AiAvailabilityState = AiAvailabilityState.AVAILABLE,
    var customResponsePayload: String? = null,
    var shouldFail: Boolean = false,
) : AiProvider {

    val cancelledRequests = mutableListOf<String>()
    val executedRequests = mutableListOf<AiRequest>()
    private val isCancelled = AtomicBoolean(false)

    override val info: AiProviderInfo = AiProviderInfo(
        id = providerId,
        displayName = "Synthetic Test Provider",
        description = "Deterministic local fixture provider for test verification",
        isRemote = false,
        supportedCapabilities = setOf(
            AiCapability.CLASSIFY_EMAIL,
            AiCapability.EXTRACT_DEADLINE,
            AiCapability.EXTRACT_MEETING,
            AiCapability.SUMMARIZE_THREAD,
            AiCapability.DETECT_INTENT,
        ),
    )

    override suspend fun checkAvailability(): AiAvailabilityState = simulatedAvailability

    override suspend fun execute(request: AiRequest): MoResult<AiRawResponse> {
        executedRequests.add(request)
        if (isCancelled.get()) {
            return MoResult.Failure(MoError.Network("Request cancelled"))
        }

        if (simulatedAvailability != AiAvailabilityState.AVAILABLE) {
            return MoResult.Failure(MoError.Network("Provider unavailable: $simulatedAvailability"))
        }

        if (shouldFail) {
            return MoResult.Failure(MoError.Network("Simulated provider failure"))
        }

        val custom = customResponsePayload
        if (custom != null) {
            return MoResult.Success(
                AiRawResponse(
                    rawPayload = custom,
                    modelId = "test-model-v1",
                    processingTimeMs = 15L,
                )
            )
        }

        val defaultPayload = when (request.capability) {
            AiCapability.CLASSIFY_EMAIL -> """{
  "category": "RECEIPTS_ORDERS",
  "confidence": 0.8,
  "explanation": "AI identified purchase receipt indicators in subject",
  "evidence": "order confirmation"
}"""
            AiCapability.SUMMARIZE_THREAD -> """{
  "summary": "The participants discussed project roadmap updates and aligned on milestones.",
  "keyPoints": ["Reviewed phase completion", "Confirmed deadlines"],
  "confidence": 0.75
}"""
            AiCapability.EXTRACT_DEADLINE,
            AiCapability.EXTRACT_MEETING -> """{
  "hasTemporalItem": true,
  "itemType": "DEADLINE",
  "title": "Quarterly Budget Submission",
  "timestampEpochMs": 1791600000000,
  "confidence": 0.82,
  "explanation": "Explicit deadline mentioned in text"
}"""
            AiCapability.DETECT_INTENT -> """{
  "intentDescription": "Sender is inquiring about meeting schedule confirmation.",
  "confidence": 0.70,
  "explanation": "Direct inquiry pattern detected"
}"""
        }

        return MoResult.Success(
            AiRawResponse(
                rawPayload = defaultPayload,
                modelId = "test-model-v1",
                processingTimeMs = 10L,
            )
        )
    }

    override fun cancel(requestId: String) {
        cancelledRequests.add(requestId)
        isCancelled.set(true)
    }

    fun reset() {
        executedRequests.clear()
        cancelledRequests.clear()
        isCancelled.set(false)
        shouldFail = false
        customResponsePayload = null
        simulatedAvailability = AiAvailabilityState.AVAILABLE
    }
}
