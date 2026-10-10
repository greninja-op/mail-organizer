package com.greninjaop.mailorganizer.ui.settings

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCache
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.data.ai.AiProviderRegistry
import com.greninjaop.mailorganizer.data.ai.FakeTestAiProvider
import com.greninjaop.mailorganizer.data.prefs.FakeAiPreferences
import com.greninjaop.mailorganizer.domain.ai.AiFallbackUseCase
import com.greninjaop.mailorganizer.domain.ai.AiManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)
    private lateinit var preferences: FakeAiPreferences
    private lateinit var registry: AiProviderRegistry
    private lateinit var fakeProvider: FakeTestAiProvider
    private lateinit var aiManager: AiManager
    private lateinit var fallbackUseCase: AiFallbackUseCase
    private lateinit var viewModel: AiSettingsViewModel

    @Before
    fun setUp() {
        preferences = FakeAiPreferences()
        registry = AiProviderRegistry()
        fakeProvider = FakeTestAiProvider(
            providerId = AiProviderId("local-rules"),
            simulatedAvailability = AiAvailabilityState.AVAILABLE,
        )
        registry.register(fakeProvider)

        aiManager = AiManager(
            preferences = preferences,
            registry = registry,
            cache = AiCache(),
            dispatchers = dispatchers,
        )
        fallbackUseCase = AiFallbackUseCase(
            aiManager = aiManager,
            preferences = preferences,
            mailRepository = null,
            dispatchers = dispatchers,
        )

        viewModel = AiSettingsViewModel(
            preferences = preferences,
            registry = registry,
            aiFallbackUseCase = fallbackUseCase,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun defaultState_aiDisabled() = runTest {
        val state = viewModel.state.value
        assertFalse(state.isAiEnabled)
        assertFalse(state.hasUserConsented)
        assertEquals(AiAvailabilityState.DISABLED, state.providerAvailability)
    }

    @Test
    fun toggleAiEnabled_updatesState() = runTest {
        viewModel.toggleAiEnabled(true)
        assertTrue(preferences.isAiEnabled.first())
    }

    @Test
    fun toggleCapability_addsAndRemoves() = runTest {
        viewModel.toggleCapability(AiCapability.CLASSIFY_EMAIL, false)
        assertFalse(preferences.allowedCapabilities.first().contains(AiCapability.CLASSIFY_EMAIL))

        viewModel.toggleCapability(AiCapability.CLASSIFY_EMAIL, true)
        assertTrue(preferences.allowedCapabilities.first().contains(AiCapability.CLASSIFY_EMAIL))
    }

    @Test
    fun resetConfiguration_clearsAllConfiguration() = runTest {
        viewModel.toggleAiEnabled(true)
        viewModel.setUserConsent(true)
        viewModel.setApiKey("some-key")

        viewModel.resetConfiguration()

        assertFalse(preferences.isAiEnabled.first())
        assertFalse(preferences.hasUserConsented.first())
        assertEquals(null, preferences.apiKey.first())
    }
}
