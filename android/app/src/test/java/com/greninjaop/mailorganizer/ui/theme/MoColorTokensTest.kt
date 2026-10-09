package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the spec-mandated color tokens (phase-01 §20). If the execution
 * plan changes a brand value, this test fails loudly on purpose.
 */
class MoColorTokensTest {

    @Test
    fun `light scheme uses the spec primary tokens`() {
        assertEquals(Color(0xFF5B5CE2), MoLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), MoLightColorScheme.onPrimary)
        assertEquals(Color(0xFFE8E8FF), MoLightColorScheme.primaryContainer)
    }

    @Test
    fun `light scheme uses the spec surface tokens`() {
        assertEquals(Color(0xFFF8F9FC), MoLightColorScheme.background)
        assertEquals(Color(0xFFFFFFFF), MoLightColorScheme.surface)
    }

    @Test
    fun `dark scheme uses the spec surface tokens`() {
        assertEquals(Color(0xFF101114), MoDarkColorScheme.background)
        assertEquals(Color(0xFF1A1B20), MoDarkColorScheme.surface)
        assertEquals(Color(0xFF222329), MoDarkColorScheme.surfaceContainerHigh)
        assertEquals(Color(0xFF222329), MoDarkElevatedSurface)
    }

    @Test
    fun `dark scheme keeps the spec primary`() {
        assertEquals(Color(0xFF5B5CE2), MoDarkColorScheme.primary)
    }

    @Test
    fun `light status colors match the spec semantic tokens`() {
        assertEquals(Color(0xFF16A34A), MoLightStatusColors.success)
        assertEquals(Color(0xFFD97706), MoLightStatusColors.warning)
        assertEquals(Color(0xFFDC2626), MoLightStatusColors.error)
        assertEquals(Color(0xFF2563EB), MoLightStatusColors.info)
        assertEquals(Color(0xFFDC2626), MoLightColorScheme.error)
    }
}
