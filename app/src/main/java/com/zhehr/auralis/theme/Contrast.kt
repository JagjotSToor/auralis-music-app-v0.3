package com.zhehr.auralis.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

/** WCAG 2.x contrast helpers. Every palette (preset, custom accent, artwork-generated) passes through these. */
object Contrast {
    const val AA_NORMAL = 4.5f
    const val AA_LARGE = 3.0f

    fun ratio(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    fun meetsAA(foreground: Color, background: Color, largeText: Boolean = false): Boolean =
        ratio(foreground, background) >= if (largeText) AA_LARGE else AA_NORMAL

    /** Returns [preferred] if it is readable on [background], otherwise black or white, whichever reads better. */
    fun readableOn(background: Color, preferred: Color): Color {
        if (meetsAA(preferred, background)) return preferred
        return onColor(background)
    }

    /** Black or white, whichever has more contrast on [bg]. */
    fun onColor(bg: Color): Color = if (ratio(Color.White, bg) >= ratio(Color.Black, bg)) Color.White else Color.Black

    /** Nudges [color] toward white (dark bg) or black (light bg) until it reaches [min] contrast on [bg]. */
    fun ensureOn(bg: Color, color: Color, min: Float = AA_NORMAL): Color {
        var c = color.toArgb()
        val target = if (bg.luminance() < 0.5f) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        var tries = 0
        while (ratio(Color(c), bg) < min && tries < 12) {
            c = ColorUtils.blendARGB(c, target, 0.12f)
            tries++
        }
        return Color(c)
    }
}
