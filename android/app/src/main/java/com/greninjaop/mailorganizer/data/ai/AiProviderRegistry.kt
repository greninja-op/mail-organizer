package com.greninjaop.mailorganizer.data.ai

import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.core.ai.AiProviderInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry and discovery point for AI providers (Phase 26 §9, §77).
 */
class AiProviderRegistry {

    private val providers = ConcurrentHashMap<AiProviderId, AiProvider>()

    /**
     * Registers a provider instance.
     */
    fun register(provider: AiProvider) {
        providers[provider.providerId] = provider
    }

    /**
     * Unregisters a provider.
     */
    fun unregister(providerId: AiProviderId) {
        providers.remove(providerId)
    }

    /**
     * Retrieves a registered provider by its ID.
     */
    fun get(providerId: AiProviderId): AiProvider? = providers[providerId]

    /**
     * Lists all registered providers.
     */
    fun getAllProviders(): List<AiProvider> = providers.values.toList()

    /**
     * Lists metadata info for all registered providers.
     */
    fun getAllProviderInfos(): List<AiProviderInfo> = providers.values.map { it.info }

    /**
     * Finds providers supporting a specific capability.
     */
    fun findSupporting(capability: AiCapability): List<AiProvider> =
        providers.values.filter { it.info.supportedCapabilities.contains(capability) }
}
