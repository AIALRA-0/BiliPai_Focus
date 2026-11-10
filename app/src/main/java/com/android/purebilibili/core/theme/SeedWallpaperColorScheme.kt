// Adapted from flex_seed_scheme 5.0.1 and Material Color Utilities (Apache-2.0).
package com.android.purebilibili.core.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.Variant

/** Rebuild from Android's five core palettes, keeping the supplied tone colors. */
@RequiresApi(Build.VERSION_CODES.S)
internal fun readSeedWallpaperColorScheme(context: Context, isDark: Boolean): ColorScheme {
    val tones = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100)
    val caches = mutableMapOf<TonalPalette, Map<Int, Int>>()
    fun palette(name: String): TonalPalette? {
        val colors = linkedMapOf<Int, Int>()
        for (tone in tones) {
            val shade = 1000 - tone * 10
            val id = context.resources.getIdentifier("system_${name}_$shade", "color", "android")
            if (id == 0) return null
            colors[tone] = context.getColor(id)
        }
        var hue = 0.0
        var chroma = 0.0
        for (argb in colors.values) {
            val hct = Hct.fromInt(argb)
            if (hct.tone <= 98.0 && hct.chroma > chroma) {
                hue = hct.hue
                chroma = hct.chroma
            }
        }
        return TonalPalette.fromHueAndChroma(hue, chroma).also { caches[it] = colors }
    }
    fun fallback(): ColorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val primary = palette("accent1") ?: return fallback()
    val secondary = palette("accent2") ?: return fallback()
    val tertiary = palette("accent3") ?: return fallback()
    val neutral = palette("neutral1") ?: return fallback()
    val neutralVariant = palette("neutral2") ?: return fallback()
    val scheme = DynamicScheme(
        sourceColorHct = primary.keyColor,
        variant = Variant.TONAL_SPOT,
        isDark = isDark,
        contrastLevel = 0.0,
        primaryPalette = primary,
        secondaryPalette = secondary,
        tertiaryPalette = tertiary,
        neutralPalette = neutral,
        neutralVariantPalette = neutralVariant,
        errorPalette = TonalPalette.fromHueAndChroma(25.0, 84.0),
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
    )
    return seedDynamicColorScheme(scheme, legacySurface = true) { role, tone ->
        val palette = role.palette(scheme)
        val cached = if (tone == tone.toInt().toDouble()) caches[palette]?.get(tone.toInt()) else null
        Color(cached ?: Hct.from(palette.hue, palette.chroma, tone).toInt())
    }
}
