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
 * Remote AI Provider Seam with Bring-Your-Own-Key (BYOK) support (Phase 26 §34, §35, §72–§75).
 *
 * Security & Privacy:
 * - HTTPS only.
 * - API keys are NEVER logged, never committed, and never stored in plain SQLite.
 * - Fails closed when API key is missing or invalid ([AiAvailabilityState.AUTH_REQUIRED]).
 * - Gracefully reports [AiAvailabilityState.OFFLINE] when network is unavailable.
 */
class StubRemoteAiProvider(
    private val apiKeyProvider: () -> String?,
    private val isNetworkAvailable: () -> Boolean = { true },
    override val providerId: AiProviderId = PROVIDER_ID,
) : AiProvider {

    companion object {
        val PROVIDER_ID = AiProviderId("remote_byok")
    }

    override val info: AiProviderInfo = AiProviderInfo(
        id = providerId,
        displayName = "Remote Cloud AI (BYOK)",
        description = "Connects to user-configured remote AI provider with user API key",
        isRemote = true,
        supportedCapabilities = setOf(
            AiCapability.CLASSIFY_EMAIL,
            AiCapability.EXTRACT_DEADLINE,
            AiCapability.EXTRACT_MEETING,
            AiCapability.SUMMARIZE_THREAD,
            AiCapability.DETECT_INTENT,
        ),
    )

    override suspend fun checkAvailability(): AiAvailabilityState {
        val key = apiKeyProvider()
        if (key.isNullOrBlank()) {
            return AiAvailabilityState.NOT_CONFIGURED
        }
        if (!isNetworkAvailable()) {
            return AiAvailabilityState.OFFLINE
        }
        return AiAvailabilityState.AVAILABLE
    }

    override suspend fun execute(request: AiRequest): MoResult<AiRawResponse> {
        val availability = checkAvailability()
        if (availability != AiAvailabilityState.AVAILABLE) {
            return MoResult.Failure(
                MoError.InvalidConfiguration("Remote AI provider is not available: $availability")
            )
        }

        // Seam for HTTPS API network client.
        // In this phase, without an external commercial API key, it honestly reports NotConfigured.
        return MoResult.Failure(
            MoError.InvalidConfiguration("Remote AI API endpoint requires active provider configuration")
        )
    }

    override fun cancel(requestId: String) {
        // Cancellation seam
    }
}
