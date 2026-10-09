package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Centralized color tokens — Phase 1 (source of truth: phase-01 §20).
 *
 * Brand values are taken verbatim from the execution plan:
 *   Primary #5B5CE2 · Primary Container #E8E8FF · Light Bg #F8F9FC
 *   Dark Bg #101114 · Light Surface #FFFFFF · Dark Surface #1A1B20
 *   Dark Elevated #222329 · Success #16A34A · Warning #D97706
 *   Error #DC2626 · Info #2563EB
 *
 * Values marked "derived" below are not in the plan; they were chosen for
 * contrast/accessibility and are documented as such. Every screen must use
 * these tokens — no ad-hoc colors.
 *
 * Dynamic color (Android 12+) is preferred at runtime (see Theme.kt); these
 * are the controlled fallback palettes.
 */

// ---- Spec tokens (verbatim) ----
private val SpecPrimary = Color(0xFF5B5CE2)
private val SpecPrimaryContainer = Color(0xFFE8E8FF)
private val SpecLightBackground = Color(0xFFF8F9FC)
private val SpecDarkBackground = Color(0xFF101114)
private val SpecLightSurface = Color(0xFFFFFFFF)
private val SpecDarkSurface = Color(0xFF1A1B20)
private val SpecDarkElevatedSurface = Color(0xFF222329)
private val SpecSuccess = Color(0xFF16A34A)
private val SpecWarning = Color(0xFFD97706)
private val SpecError = Color(0xFFDC2626)
private val SpecInfo = Color(0xFF2563EB)

// ---- Derived companions (contrast-checked, not in the plan) ----
private val OnPrimaryContainerLight = Color(0xFF1A2151) // dark indigo on #E8E8FF
private val PrimaryContainerDark = Color(0xFF2E3370) // deep indigo for dark mode
private val OnLightBackground = Color(0xFF141519)
private val OnDarkBackground = Color(0xFFF4F5FA)
private val SurfaceVariantLight = Color(0xFFE9EAF2)
private val OnSurfaceVariantLight = Color(0xFF44464F)
private val SurfaceVariantDark = Color(0xFF2A2B33)
private val OnSurfaceVariantDark = Color(0xFFC6C7D0)
private val ErrorDark = Color(0xFFFFB4AB) // #DC2626 is unreadable on #101114
private val OnErrorDark = Color(0xFF690005)

val MoLightColorScheme = lightColorScheme(
    primary = SpecPrimary,
    onPrimary = Color.White,
    primaryContainer = SpecPrimaryContainer,
    onPrimaryContainer = OnPrimaryContainerLight,
    background = SpecLightBackground,
    onBackground = OnLightBackground,
    surface = SpecLightSurface,
    onSurface = OnLightBackground,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = SpecError,
    onError = Color.White,
)

val MoDarkColorScheme = darkColorScheme(
    primary = SpecPrimary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = SpecPrimaryContainer,
    background = SpecDarkBackground,
    onBackground = OnDarkBackground,
    surface = SpecDarkSurface,
    onSurface = OnDarkBackground,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    // Dark elevated surface (#222329) is exposed for cards/sheets that need
    // separation from the base dark surface; see MoDarkElevatedSurface.
    surfaceContainerHigh = SpecDarkElevatedSurface,
    error = ErrorDark,
    onError = OnErrorDark,
)

/** Dark elevated surface token (#222329) for cards/sheets on dark theme. */
val MoDarkElevatedSurface = SpecDarkElevatedSurface

/**
 * Semantic status colors. Light values are the verbatim spec tokens; dark
 * values are tonal equivalents derived for contrast on #101114.
 */
data class MoStatusColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val error: Color,
)

val MoLightStatusColors = MoStatusColors(
    success = SpecSuccess,
    warning = SpecWarning,
    info = SpecInfo,
    error = SpecError,
)

val MoDarkStatusColors = MoStatusColors(
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    info = Color(0xFF60A5FA),
    error = ErrorDark,
)

/**
 * Category accent colors (Phase 7 §53).
 *
 * Spec mapping: Action Required → red, Important → primary, Career → blue,
 * Education → purple, Receipts & Orders → green, Security → amber,
 * Notifications → blue/neutral, Newsletters → teal, Promotions → orange,
 * Low Value → gray. Spec tokens are reused verbatim where they match
 * (red/primary/blue/green/amber); the rest are derived companions chosen
 * for contrast on light (#F8F9FC) and dark (#101114) backgrounds and are
 * documented as derived, per the token rules above.
 *
 * Color is never the only category indicator — every usage pairs the
 * accent with a text label and a content description (see CategoryVisuals).
 */
data class MoCategoryColors(
    val actionRequired: Color,
    val important: Color,
    val career: Color,
    val education: Color,
    val receiptsOrders: Color,
    val security: Color,
    val notifications: Color,
    val newsletters: Color,
    val promotions: Color,
    val lowValue: Color,
    val unclassified: Color,
)

val MoLightCategoryColors = MoCategoryColors(
    actionRequired = SpecError,
    important = SpecPrimary,
    career = SpecInfo,
    education = Color(0xFF7C3AED),
    receiptsOrders = SpecSuccess,
    security = SpecWarning,
    notifications = Color(0xFF0284C7),
    newsletters = Color(0xFF0D9488),
    promotions = Color(0xFFEA580C),
    lowValue = Color(0xFF6B7280),
    unclassified = Color(0xFF6B7280),
)

val MoDarkCategoryColors = MoCategoryColors(
    actionRequired = ErrorDark,
    important = Color(0xFFA8ABFF),
    career = Color(0xFF60A5FA),
    education = Color(0xFFC4B5FD),
    receiptsOrders = Color(0xFF4ADE80),
    security = Color(0xFFFBBF24),
    notifications = Color(0xFF38BDF8),
    newsletters = Color(0xFF5EEAD4),
    promotions = Color(0xFFFB923C),
    lowValue = Color(0xFF9CA3AF),
    unclassified = Color(0xFF9CA3AF),
)
