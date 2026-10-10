package com.greninjaop.mailorganizer.domain.ai

import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCache
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.data.ai.AiProviderRegistry
import com.greninjaop.mailorganizer.data.ai.FakeTestAiProvider
import com.greninjaop.mailorganizer.data.local.ClassificationSource
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.prefs.FakeAiPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiFallbackUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)
    private lateinit var preferences: FakeAiPreferences
    private lateinit var registry: AiProviderRegistry
    private lateinit var fakeProvider: FakeTestAiProvider
    private lateinit var aiManager: AiManager
    private lateinit var useCase: AiFallbackUseCase

    private fun testMessage(
        messageId: String = "m1",
        accountId: String = "acc1",
        threadId: String = "t1",
        fromAddress: String = "news@example.com",
        subject: String = "Weekly Newsletter",
        snippet: String = "Latest news updates",
    ) = MessageRecord(
        messageId = messageId,
        gmailMessageId = "gm-$messageId",
        threadId = threadId,
        accountId = accountId,
        fromAddress = fromAddress,
        fromName = "News",
        toAddresses = listOf("me@example.com"),
        subject = subject,
        snippet = snippet,
        bodyText = snippet,
        timestampEpochMs = 1000L,
    )

    @Before
    fun setUp() {
        preferences = FakeAiPreferences()
        registry = AiProviderRegistry()
        fakeProvider = FakeTestAiProvider(
            providerId = AiProviderId("fake-ai"),
            simulatedAvailability = AiAvailabilityState.AVAILABLE,
        )
        registry.register(fakeProvider)

        aiManager = AiManager(
            preferences = preferences,
            registry = registry,
            cache = AiCache(),
            dispatchers = dispatchers,
        )
        useCase = AiFallbackUseCase(
            aiManager = aiManager,
            preferences = preferences,
            mailRepository = null,
            dispatchers = dispatchers,
            clock = { 1000L },
        )
    }

    @Test
    fun classifyWithFallback_skipsWhenDeterministicAlreadyDecided() = runTest {
        preferences.setAiEnabled(true)
        preferences.setSelectedProviderId("fake-ai")
        preferences.setAllowedCapabilities(setOf(AiCapability.CLASSIFY_EMAIL))
        preferences.setUserConsented(true)

        val message = testMessage()

        // Deterministic engine already identified this as PROMOTIONS
        val result = useCase.classifyWithFallback(message, deterministicCategory = MailCategory.PROMOTIONS)

        // Must be null: AI never replaces deterministic decisions
        assertNull(result)
    }

    @Test
    fun classifyWithFallback_runsWhenDeterministicIsUnclassified() = runTest {
        preferences.setAiEnabled(true)
        preferences.setSelectedProviderId("fake-ai")
        preferences.setAllowedCapabilities(setOf(AiCapability.CLASSIFY_EMAIL))
        preferences.setUserConsented(true)

        fakeProvider.customResponsePayload = """{"category": "NEWSLETTERS", "confidence": 0.70, "explanation": "Monthly newsletter"}"""

        val message = testMessage()

        val record = useCase.classifyWithFallback(message, deterministicCategory = MailCategory.UNCLASSIFIED)

        assertNotNull(record)
        assertEquals(MailCategory.NEWSLETTERS, record?.category)
        assertEquals(ClassificationSource.OPTIONAL_AI, record?.source)
        assertEquals(0.70f, record?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun disconnectProvider_resetsAiConfigurationAndClearsCache() = runTest {
        preferences.setAiEnabled(true)
        preferences.setSelectedProviderId("fake-ai")
        preferences.setUserConsented(true)
        preferences.setApiKey("secret-key")

        useCase.disconnectProvider()

        assertEquals(false, preferences.isAiEnabled.first())
        assertEquals(false, preferences.hasUserConsented.first())
        assertNull(preferences.apiKey.first())
        assertEquals(0, aiManager.aiCache.size)
    }
}
