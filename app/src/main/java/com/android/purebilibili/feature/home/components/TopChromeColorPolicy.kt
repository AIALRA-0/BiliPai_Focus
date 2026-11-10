package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.ui.wallpaper.WallpaperLuminanceBand
import com.android.purebilibili.core.ui.wallpaper.resolveWallpaperProtectionAlpha
import kotlin.math.max
import kotlin.math.min

/** 顶部渐隐保留背景色；仅为当前前景文字补充局部对比度保护。 */
internal fun resolveTopChromeFadeColor(
    surfaceColor: Color,
    foregroundColor: Color,
    blurred: Boolean,
    backgroundColor: Color? = null,
    readabilityBand: WallpaperLuminanceBand? = null,
): Color {
    if (surfaceColor.alpha <= 0f) return surfaceColor
    val surface = surfaceColor.copy(alpha = 1f)
    val sampled = backgroundColor?.takeIf { it.alpha > 0f }
    val tinted = sampled?.let { lerp(surface, it.copy(alpha = 1f), 0.45f) } ?: surface
    val foregroundLuminance = foregroundColor.luminance()
    fun readable(color: Color): Boolean {
        val luminance = color.luminance()
        return (max(foregroundLuminance, luminance) + 0.05f) /
            (min(foregroundLuminance, luminance) + 0.05f) >= 4.5f
    }
    // 极亮或极暗的取色不能破坏主题原有的文字对比度。
    val fill = if (readable(tinted)) tinted else {
        var low = 0f
        var high = 1f
        repeat(12) {
            val fraction = (low + high) / 2f
            if (readable(lerp(tinted, surface, fraction))) high = fraction else low = fraction
        }
        lerp(tinted, surface, high)
    }
    val protection = resolveWallpaperProtectionAlpha(
        band = readabilityBand ?: WallpaperLuminanceBand(0f, 0.5f, 1f),
        surfaceColor = fill,
        foregroundColor = foregroundColor,
        blurred = blurred,
    )
    // 有模糊时让真实背景透色；单独渐隐仍保留消隐强度，但不再完全盖实。
    val minimum = if (blurred) 0.18f else 0.76f
    return fill.copy(alpha = max(minimum, protection) * surfaceColor.alpha)
}
