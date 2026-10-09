package com.zhehr.auralis.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Persisted by name. Older names (SYSTEM, DARK, AMOLED, LIGHT) keep working. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    AMOLED("AMOLED"),
    LIGHT("Light"),
    OBSIDIAN("Obsidian"),
    AMBER("Amber"),
    CRIMSON("Crimson"),
    ELECTRIC_BLUE("Electric Blue"),
    VIOLET("Violet"),
    EMERALD("Emerald"),
    ICE("Ice"),
    CYBER("Cyber"),
    MIDNIGHT("Midnight"),
}

enum class MotionLevel(val label: String, val blurb: String) {
    MINIMAL("Minimal", "No decorative animation. Instant transitions."),
    BALANCED("Balanced", "Short, meaningful motion. The default."),
    CINEMATIC("Cinematic", "Longer fades and a slower intro."),
}

/**
 * The app's own colour tokens. Material's ColorScheme is derived from this, so themes,
 * custom accents and (later) wallpapers only have to produce a palette.
 */
@Immutable
data class AuralisPalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val mutedText: Color,
    val isDark: Boolean,
)

private fun dark(bg: Long, surface: Long, high: Long, accent: Long) = AuralisPalette(
    background = Color(bg), surface = Color(surface), surfaceHigh = Color(high),
    accent = Color(accent), onAccent = Color(0xFF0B0D12),
    text = Color(0xFFF2F1EE), mutedText = Color(0xFF9AA0B2), isDark = true,
)

object AuralisPalettes {
    val Dark = dark(0xFF0B0D12, 0xFF141824, 0xFF1D2232, 0xFFFFB454)
    val Amoled = dark(0xFF000000, 0xFF0C0E14, 0xFF161A26, 0xFFFFB454)
    val Light = AuralisPalette(
        background = Color(0xFFF7F5F0), surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFECE8DF),
        accent = Color(0xFFB35A00), onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF15161A), mutedText = Color(0xFF5C6070), isDark = false,
    )
    val Obsidian = dark(0xFF0A0A0C, 0xFF141417, 0xFF1E1E23, 0xFFE8E6E1)
    val Amber = dark(0xFF100C08, 0xFF1A140D, 0xFF261D12, 0xFFFFB454)
    val Crimson = dark(0xFF0E0A0B, 0xFF191114, 0xFF241A1E, 0xFFFF4D5E)
    val ElectricBlue = dark(0xFF080C14, 0xFF0F1626, 0xFF172036, 0xFF3D9BFF)
    val Violet = dark(0xFF0C0A14, 0xFF151126, 0xFF201A38, 0xFFA78BFA)
    val Emerald = dark(0xFF08100D, 0xFF0F1B16, 0xFF16281F, 0xFF34D399)
    val Cyber = dark(0xFF07070D, 0xFF10101E, 0xFF1A1A30, 0xFF00E5FF)
    val Midnight = dark(0xFF070B16, 0xFF0E1426, 0xFF16203A, 0xFF8AB4FF)
    val Ice = AuralisPalette(
        background = Color(0xFFEEF4FA), surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFDCE8F4),
        accent = Color(0xFF0B6FB8), onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF10151C), mutedText = Color(0xFF51606F), isDark = false,
    )
}

fun paletteFor(mode: ThemeMode, systemDark: Boolean): AuralisPalette = when (mode) {
    ThemeMode.SYSTEM -> if (systemDark) AuralisPalettes.Dark else AuralisPalettes.Light
    ThemeMode.DARK -> AuralisPalettes.Dark
    ThemeMode.AMOLED -> AuralisPalettes.Amoled
    ThemeMode.LIGHT -> AuralisPalettes.Light
    ThemeMode.OBSIDIAN -> AuralisPalettes.Obsidian
    ThemeMode.AMBER -> AuralisPalettes.Amber
    ThemeMode.CRIMSON -> AuralisPalettes.Crimson
    ThemeMode.ELECTRIC_BLUE -> AuralisPalettes.ElectricBlue
    ThemeMode.VIOLET -> AuralisPalettes.Violet
    ThemeMode.EMERALD -> AuralisPalettes.Emerald
    ThemeMode.ICE -> AuralisPalettes.Ice
    ThemeMode.CYBER -> AuralisPalettes.Cyber
    ThemeMode.MIDNIGHT -> AuralisPalettes.Midnight
}
