package com.greninjaop.mailorganizer.domain.actions

import com.greninjaop.mailorganizer.core.actions.ActionSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ActionPayloadJson round-trip tests (Phase 14). */
class ActionPayloadJsonTest {

    @Test
    fun `round trip preserves fields`() {
        val json = ActionPayloadJson.encode(
            version = 1,
            signals = listOf(
                ActionSignal("interview_detected", "detail"),
                ActionSignal("upcoming", "detail"),
            ),
            missingInfo = listOf("start time"),
            targetKey = "temporal:7",
        )
        val decoded = ActionPayloadJson.decode(json)!!
        assertEquals(1, decoded.version)
        assertEquals(listOf("interview_detected", "upcoming"), decoded.signalNames)
        assertEquals(listOf("start time"), decoded.missingInfo)
        assertEquals("temporal:7", decoded.targetKey)
    }

    @Test
    fun `malformed json decodes to null`() {
        assertNull(ActionPayloadJson.decode(null))
        assertNull(ActionPayloadJson.decode(""))
        assertNull(ActionPayloadJson.decode("{oops"))
    }

    @Test
    fun `escapes survive round trip`() {
        val json = ActionPayloadJson.encode(
            version = 1,
            signals = listOf(ActionSignal("a\"b\\c", "d")),
            missingInfo = emptyList(),
            targetKey = "k",
        )
        val decoded = ActionPayloadJson.decode(json)!!
        assertEquals(listOf("a\"b\\c"), decoded.signalNames)
    }
}
