package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized shape tokens — Phase 1 (source of truth: phase-01 §23).
 *
 * Standard radii: 8 / 12 / 16 / 20 dp, plus pill. Use [MoShapes] via
 * MaterialTheme; reach for [MoRadii] only when a component needs an
 * explicit radius value.
 */
object MoRadii {
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 20.dp

    /** Pill shape for chips, account indicators, and similar components. */
    val pill: Shape = CircleShape
}

val MoShapes = Shapes(
    extraSmall = RoundedCornerShape(MoRadii.small),
    small = RoundedCornerShape(MoRadii.small),
    medium = RoundedCornerShape(MoRadii.medium),
    large = RoundedCornerShape(MoRadii.large),
    extraLarge = RoundedCornerShape(MoRadii.extraLarge),
)
