package com.greninjaop.mailorganizer.domain.ai

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.ai.AiAvailabilityState
import com.greninjaop.mailorganizer.core.ai.AiCache
import com.greninjaop.mailorganizer.core.ai.AiCapability
import com.greninjaop.mailorganizer.core.ai.AiProviderId
import com.greninjaop.mailorganizer.data.ai.AiProviderRegistry
import com.greninjaop.mailorganizer.data.ai.FakeTestAiProvider
import com.greninjaop.mailorganizer.data.local.MailCategory
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.prefs.FakeAiPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = AppDispatchers(testDispatcher, testDispatcher, testDispatcher)
    private lateinit var preferences: FakeAiPreferences
    private lateinit var registry: AiProviderRegistry
    private lateinit var fakeProvider: FakeTestAiProvider
    private lateinit var aiManager: AiManager

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
    }

    @Test
    fun fallbackClassify_whenAiDisabled_returnsNull() = runTest {
        preferences.setAiEnabled(false)

        val message = testMessage()
        val result = aiManager.fallbackClassify(message, deterministicCategory = MailCategory.UNCLASSIFIED)
        assertNull(result)
    }

    @Test
    fun fallbackClassify_whenAiEnabledAndConsented_returnsValidatedResult() = runTest {
        preferences.setAiEnabled(true)
        preferences.setSelectedProviderId("fake-ai")
        preferences.setAllowedCapabilities(setOf(AiCapability.CLASSIFY_EMAIL))
        preferences.setUserConsented(true)

        fakeProvider.customResponsePayload = """{"category": "NEWSLETTERS", "confidence": 0.80, "explanation": "Weekly digest"}"""

        val message = testMessage()
        val result = aiManager.fallbackClassify(message, deterministicCategory = MailCategory.UNCLASSIFIED)
        assertNotNull(result)
        assertEquals(MailCategory.NEWSLETTERS, result?.output?.category)
        assertEquals(0.80f, result?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun fallbackClassify_cachedResultReturnedWithoutCallingProviderAgain() = runTest {
        preferences.setAiEnabled(true)
        preferences.setSelectedProviderId("fake-ai")
        preferences.setAllowedCapabilities(setOf(AiCapability.CLASSIFY_EMAIL))
        preferences.setUserConsented(true)

        fakeProvider.customResponsePayload = """{"category": "RECEIPTS_ORDERS", "confidence": 0.75, "explanation": "Invoice"}"""

        val message = testMessage()
        val first = aiManager.fallbackClassify(message, deterministicCategory = MailCategory.UNCLASSIFIED)
        assertNotNull(first)
        assertEquals(MailCategory.RECEIPTS_ORDERS, first?.output?.category)

        // Clear provider payload to prove provider is not called again
        fakeProvider.customResponsePayload = null

        val second = aiManager.fallbackClassify(message, deterministicCategory = MailCategory.UNCLASSIFIED)
        assertNotNull(second)
        assertEquals(MailCategory.RECEIPTS_ORDERS, second?.output?.category)
    }

    @Test
    fun summarizeThread_crossAccountDetectionRejects() = runTest {
        preferences.setAiEnabled(true)
        preferences.setSelectedProviderId("fake-ai")
        preferences.setAllowedCapabilities(setOf(AiCapability.SUMMARIZE_THREAD))
        preferences.setUserConsented(true)

        val m1 = testMessage(messageId = "m1", accountId = "acc1")
        val m2 = testMessage(messageId = "m2", accountId = "acc2")

        val result = aiManager.summarizeThread(listOf(m1, m2), accountId = "acc1", threadId = "t1")
        assertTrue(result is MoResult.Failure)
    }
}
