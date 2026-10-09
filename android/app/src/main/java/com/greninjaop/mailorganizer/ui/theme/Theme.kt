package com.greninjaop.mailorganizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * App theme. Dynamic color on Android 12+, controlled Mail Organizer fallback
 * palettes below that (per design.md). [themeMode] may be a plain value or a
 * [Flow] collected by the caller — the overloads keep call sites simple.
 */
@Composable
fun MailOrganizerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MoDarkColorScheme
        else -> MoLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoTypography,
        shapes = MoShapes,
        content = content,
    )
}

@Composable
fun MailOrganizerTheme(
    themeMode: Flow<ThemeMode>,
    content: @Composable () -> Unit,
) {
    val mode: State<ThemeMode> = produceState(initialValue = ThemeMode.SYSTEM, themeMode) {
        themeMode.collect { value = it }
    }
    MailOrganizerTheme(themeMode = mode.value, content = content)
}
