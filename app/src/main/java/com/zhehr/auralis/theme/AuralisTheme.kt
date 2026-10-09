package com.zhehr.auralis.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAuralisPalette = staticCompositionLocalOf { AuralisPalettes.Dark }

@Composable
fun AuralisTheme(mode: ThemeMode, accentOverride: Int? = null, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val base = paletteFor(mode, systemDark)
    val palette = if (accentOverride == null) base else {
        val a = Contrast.ensureOn(base.background, Color(accentOverride))
        base.copy(accent = a, onAccent = Contrast.onColor(a))
    }

    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent, onPrimary = palette.onAccent,
            background = palette.background, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh, onSurfaceVariant = palette.mutedText,
        )
    } else {
        lightColorScheme(
            primary = palette.accent, onPrimary = palette.onAccent,
            background = palette.background, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh, onSurfaceVariant = palette.mutedText,
        )
    }

    // Keep status/navigation bar icons readable when the chosen mode differs from the system setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !palette.isDark
            controller.isAppearanceLightNavigationBars = !palette.isDark
        }
    }

    CompositionLocalProvider(LocalAuralisPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = AuralisTypography, content = content)
    }
}
