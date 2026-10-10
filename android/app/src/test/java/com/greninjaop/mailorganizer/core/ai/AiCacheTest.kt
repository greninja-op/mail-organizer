package com.greninjaop.mailorganizer.core.ai

import com.greninjaop.mailorganizer.data.local.MailCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AiCacheTest {

    @Test
    fun cache_putAndGet() {
        val cache = AiCache(maxEntries = 10)
        val result = AiResult(
            capability = AiCapability.CLASSIFY_EMAIL,
            providerId = AiProviderId("local-rules"),
            modelId = "local-heuristic",
            sourceId = "msg-1",
            accountId = "acc-1",
            output = AiClassificationOutput(
                category = MailCategory.NEWSLETTERS,
                confidence = 0.70f,
                explanation = "AI suggestion: newsletter digest",
            ),
            confidence = 0.70f,
            processingTimeMs = 5L,
            createdAtEpochMs = 1000L,
            version = 1,
        )

        cache.put(result)

        val cached = cache.get<AiClassificationOutput>(
            accountId = "acc-1",
            sourceId = "msg-1",
            capability = AiCapability.CLASSIFY_EMAIL,
            version = 1,
        )

        assertNotNull(cached)
        assertEquals(MailCategory.NEWSLETTERS, cached?.output?.category)
    }

    @Test
    fun cache_invalidateAccount_clearsOnlyThatAccount() {
        val cache = AiCache(maxEntries = 10)
        val res1 = AiResult(
            capability = AiCapability.CLASSIFY_EMAIL,
            providerId = AiProviderId("local-rules"),
            modelId = "m1",
            sourceId = "msg-1",
            accountId = "acc-1",
            output = AiClassificationOutput(MailCategory.NEWSLETTERS, 0.7f, ""),
            confidence = 0.7f,
            processingTimeMs = 5L,
            createdAtEpochMs = 1000L,
        )
        val res2 = AiResult(
            capability = AiCapability.CLASSIFY_EMAIL,
            providerId = AiProviderId("local-rules"),
            modelId = "m1",
            sourceId = "msg-2",
            accountId = "acc-2",
            output = AiClassificationOutput(MailCategory.NOTIFICATIONS, 0.7f, ""),
            confidence = 0.7f,
            processingTimeMs = 5L,
            createdAtEpochMs = 1000L,
        )

        cache.put(res1)
        cache.put(res2)

        cache.invalidateAccount("acc-1")

        assertNull(cache.get<AiClassificationOutput>("acc-1", "msg-1", AiCapability.CLASSIFY_EMAIL))
        assertNotNull(cache.get<AiClassificationOutput>("acc-2", "msg-2", AiCapability.CLASSIFY_EMAIL))
    }

    @Test
    fun cache_clear_clearsAll() {
        val cache = AiCache(maxEntries = 10)
        val res = AiResult(
            capability = AiCapability.CLASSIFY_EMAIL,
            providerId = AiProviderId("local-rules"),
            modelId = "m1",
            sourceId = "msg-1",
            accountId = "acc-1",
            output = AiClassificationOutput(MailCategory.NEWSLETTERS, 0.7f, ""),
            confidence = 0.7f,
            processingTimeMs = 5L,
            createdAtEpochMs = 1000L,
        )
        cache.put(res)
        assertEquals(1, cache.size)

        cache.clear()
        assertEquals(0, cache.size)
    }
}
