package com.greninjaop.mailorganizer.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for Room type converters (Phase 2).
 *
 * No Robolectric needed: converters are plain Kotlin.
 */
class MoConvertersTest {

    private val converters = MoConverters()

    @Test
    fun `string list round-trips including empty`() {
        val original = listOf("a@example.com", "b@example.com")
        assertEquals(original, converters.toStringList(converters.fromStringList(original)))
        assertEquals(emptyList<String>(), converters.toStringList(converters.fromStringList(emptyList())))
    }

    @Test
    fun `string list survives commas and semicolons in values`() {
        // The U+001F separator must not collide with common delimiters.
        val original = listOf("Doe, Jane", "a;b@c.com")
        assertEquals(original, converters.toStringList(converters.fromStringList(original)))
    }

    @Test
    fun `enums round-trip by name`() {
        assertEquals(MailCategory.CAREER, converters.toMailCategory(converters.fromMailCategory(MailCategory.CAREER)))
        assertEquals(Priority.CRITICAL, converters.toPriority(converters.fromPriority(Priority.CRITICAL)))
        assertEquals(ConnectionState.TOKEN_EXPIRED, converters.toConnectionState(converters.fromConnectionState(ConnectionState.TOKEN_EXPIRED)))
        assertEquals(ClassificationSource.USER_CORRECTION, converters.toClassificationSource(converters.fromClassificationSource(ClassificationSource.USER_CORRECTION)))
        assertEquals(ActionType.DEADLINE, converters.toActionType(converters.fromActionType(ActionType.DEADLINE)))
        assertEquals(SyncStatus.FAILED, converters.toSyncStatus(converters.fromSyncStatus(SyncStatus.FAILED)))
        assertEquals(RuleType.DOMAIN_TO_CATEGORY, converters.toRuleType(converters.fromRuleType(RuleType.DOMAIN_TO_CATEGORY)))
        assertEquals(CorrectionScope.COMPANY, converters.toCorrectionScope(converters.fromCorrectionScope(CorrectionScope.COMPANY)))
        assertEquals(CorrectionField.ACTION_REQUIRED, converters.toCorrectionField(converters.fromCorrectionField(CorrectionField.ACTION_REQUIRED)))
        assertEquals(ExtractedItemType.MEETING, converters.toExtractedItemType(converters.fromExtractedItemType(ExtractedItemType.MEETING)))
    }

    @Test
    fun `enum storage uses names not ordinals`() {
        // Guards against a future refactor switching to ordinal storage,
        // which would corrupt data on enum reordering.
        assertEquals("PROMOTIONS", converters.fromMailCategory(MailCategory.PROMOTIONS))
        assertTrue(converters.fromPriority(Priority.HIGH).isNotEmpty())
    }
}
