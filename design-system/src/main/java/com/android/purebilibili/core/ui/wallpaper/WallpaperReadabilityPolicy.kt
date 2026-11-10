package com.android.purebilibili.core.ui.wallpaper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Luminance quantiles come from visible pixels, independently of the decorative palette. */
@Immutable
data class WallpaperLuminanceBand(val low: Float, val mean: Float, val high: Float)

@Immutable
data class WallpaperReadabilityProfile(val bands: List<WallpaperLuminanceBand>) {
    val top: WallpaperLuminanceBand? get() = bands.firstOrNull()
    val bottom: WallpaperLuminanceBand? get() = bands.lastOrNull()
}

/** Find a local protective fill for the current text color, rather than dimming the full image. */
fun resolveWallpaperProtectionAlpha(
    band: WallpaperLuminanceBand,
    surfaceColor: Color,
    foregroundColor: Color,
    blurred: Boolean,
): Float {
    val foreground = foregroundColor.luminance()
    val worst = (if (foreground < 0.5f) band.low else band.high)
        .takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0.5f
    val gray = if (worst <= 0.0031308f) worst * 12.92f
        else 1.055f * worst.pow(1f / 2.4f) - 0.055f
    val background = Color(gray, gray, gray)
    fun contrast(alpha: Float): Float {
        val painted = surfaceColor.copy(alpha = alpha).compositeOver(background).luminance()
        return (max(foreground, painted) + 0.05f) / (min(foreground, painted) + 0.05f)
    }
    val minimum = if (blurred) 0.08f else 0.16f
    if (contrast(minimum) >= 4.5f) return minimum
    var low = minimum
    var high = 0.90f
    repeat(12) {
        val midpoint = (low + high) / 2f
        if (contrast(midpoint) >= 4.5f) high = midpoint else low = midpoint
    }
    return high
}

fun resolveWallpaperBodyTintAlpha(originalAlpha: Float): Float =
    ((originalAlpha.takeIf(Float::isFinite) ?: 0.28f) * 0.55f).coerceIn(0f, 0.28f)
