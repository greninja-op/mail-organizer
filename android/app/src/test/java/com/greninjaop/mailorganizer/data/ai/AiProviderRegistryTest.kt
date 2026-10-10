package com.greninjaop.mailorganizer.data.ai

import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderRegistryTest {

    @Test
    fun registry_registerAndRetrieve() {
        val registry = AiProviderRegistry()
        val provider = FakeTestAiProvider(
            providerId = AiProviderId("test-provider"),
        )

        registry.register(provider)

        val retrieved = registry.get(AiProviderId("test-provider"))
        assertNotNull(retrieved)
        assertEquals("Synthetic Test Provider", retrieved?.info?.displayName)
    }

    @Test
    fun registry_unregister() {
        val registry = AiProviderRegistry()
        val provider = FakeTestAiProvider(providerId = AiProviderId("test-provider"))

        registry.register(provider)
        registry.unregister(AiProviderId("test-provider"))

        assertNull(registry.get(AiProviderId("test-provider")))
    }

    @Test
    fun registry_getAllProviderInfos() = runBlocking {
        val registry = AiProviderRegistry()
        registry.register(LocalRuleAiProvider())
        registry.register(
            StubRemoteAiProvider(
                apiKeyProvider = { null },
                isNetworkAvailable = { true },
            )
        )

        val infos = registry.getAllProviderInfos()
        assertEquals(2, infos.size)
        assertTrue(infos.any { it.id == LocalRuleAiProvider.PROVIDER_ID })
        assertTrue(infos.any { it.id == StubRemoteAiProvider.PROVIDER_ID })
    }

    @Test
    fun stubRemoteAiProvider_notConfiguredWithoutApiKey() = runBlocking {
        val stub = StubRemoteAiProvider(
            apiKeyProvider = { null },
            isNetworkAvailable = { true },
        )
        val state = stub.checkAvailability()
        assertEquals(AiAvailabilityState.NOT_CONFIGURED, state)
    }

    @Test
    fun stubRemoteAiProvider_offlineWhenNoNetwork() = runBlocking {
        val stub = StubRemoteAiProvider(
            apiKeyProvider = { "valid-key" },
            isNetworkAvailable = { false },
        )
        val state = stub.checkAvailability()
        assertEquals(AiAvailabilityState.OFFLINE, state)
    }

    @Test
    fun stubRemoteAiProvider_availableWhenKeyAndNetworkPresent() = runBlocking {
        val stub = StubRemoteAiProvider(
            apiKeyProvider = { "valid-key" },
            isNetworkAvailable = { true },
        )
        val state = stub.checkAvailability()
        assertEquals(AiAvailabilityState.AVAILABLE, state)
    }
}
