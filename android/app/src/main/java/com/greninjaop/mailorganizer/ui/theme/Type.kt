package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Centralized typography tokens — Phase 1 (source of truth: phase-01 §21).
 *
 * Inter is the preferred typeface per the design contract. It is NOT bundled
 * in Phase 1: the GitHub push path available in this environment cannot
 * verifiably transport binary font files, so bundling them would risk a
 * corrupt or incomplete repository. The type scale below therefore uses the
 * system sans-serif fallback, which the execution plan explicitly allows
 * ("with an appropriate system fallback").
 *
 * Decision (reversible): bundle Inter (OFL) in res/font in a later phase
 * once binary pushes are verified, or when building from a machine with
 * direct git access. See docs/development-status.md.
 *
 * Per-screen type customization must be justified and added here, not inline.
 */
private val MoFontFamily: FontFamily = FontFamily.Default

val MoTypography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp,
    ),
    // Large title
    headlineLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp,
    ),
    // Title
    titleLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    // Section title
    titleMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    // Secondary text
    bodyMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    // Caption / labels
    labelLarge = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MoFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
