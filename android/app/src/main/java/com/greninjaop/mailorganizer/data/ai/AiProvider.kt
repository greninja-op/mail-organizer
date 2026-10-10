package com.greninjaop.mailorganizer.data.ai

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.core.ai.AiProviderInfo
import com.greninjaop.mailorganizer.core.ai.AiRawResponse
import com.greninjaop.mailorganizer.core.ai.AiRequest

/**
 * Pluggable provider abstraction for AI assistance (Phase 26 §9, §10).
 *
 * Domain logic never couples directly to vendor SDKs or specific APIs.
 * Adapters translate provider-specific formats into internal structured models.
 */
interface AiProvider {

    /** Unique provider identifier. */
    val providerId: AiProviderId

    /** Metadata descriptor for UI presentation and capability queries. */
    val info: AiProviderInfo

    /**
     * Checks the live availability of this provider (network, credentials, status).
     */
    suspend fun checkAvailability(): AiAvailabilityState

    /**
     * Executes a structured AI request against the provider.
     */
    suspend fun execute(request: AiRequest): MoResult<AiRawResponse>

    /**
     * Cancels an in-flight request if supported (Phase 26 §147).
     */
    fun cancel(requestId: String)
}
