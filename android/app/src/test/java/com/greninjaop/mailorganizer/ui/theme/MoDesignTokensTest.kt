package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the spacing grid and shape tokens (phase-01 §22, §23).
 */
class MoDesignTokensTest {

    @Test
    fun `spacing follows the 4dp grid`() {
        val tokens = listOf(
            MoSpacing.xxs, MoSpacing.xs, MoSpacing.sm, MoSpacing.md,
            MoSpacing.lg, MoSpacing.xl, MoSpacing.xxl, MoSpacing.xxxl,
            MoSpacing.huge, MoSpacing.giant,
        )
        assertEquals(
            listOf(4.dp, 8.dp, 12.dp, 16.dp, 20.dp, 24.dp, 32.dp, 40.dp, 48.dp, 64.dp),
            tokens,
        )
        tokens.forEach { token ->
            assertTrue(
                "spacing token $token is off the 4dp grid",
                token.value % MoSpacing.grid.value == 0f,
            )
        }
    }

    @Test
    fun `shape radii match the spec tokens`() {
        assertEquals(8.dp, MoRadii.small)
        assertEquals(12.dp, MoRadii.medium)
        assertEquals(16.dp, MoRadii.large)
        assertEquals(20.dp, MoRadii.extraLarge)
    }

    @Test
    fun `material shapes are built from the radius tokens`() {
        assertEquals(RoundedCornerShape(8.dp), MoShapes.small)
        assertEquals(RoundedCornerShape(12.dp), MoShapes.medium)
        assertEquals(RoundedCornerShape(16.dp), MoShapes.large)
        assertEquals(RoundedCornerShape(20.dp), MoShapes.extraLarge)
    }
}
