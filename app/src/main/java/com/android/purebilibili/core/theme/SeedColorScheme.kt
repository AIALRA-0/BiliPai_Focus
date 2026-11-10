// Adapted from flex_seed_scheme 5.0.1, Copyright 2022-2025 Mike Rydstrom.
// BSD-3-Clause; full license: assets/licenses/flex_seed_scheme.LICENSE.
// MCU uses MaterialKolor 6.0.0-beta01 (Apache-2.0); see assets/licenses/material_color_utilities.LICENSE.
package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.Variant
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.hct.Cam16
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Single-seed native port of the 5.0.1 preset API. All 21 presets use their own
 * tone/chroma rules. Foreground accessibility is applied after the raw palette and
 * pure-black adjustments; accents and surfaces stay intact. Standard keeps the old pipeline.
 * The four supported MCU styles use the complete native 2025 path when selected;
 * other variants retain their legacy preset rules.
 */
internal fun createSeedColorScheme(
    seedColor: Color,
    isDark: Boolean,
    style: SeedPaletteStyle,
    pureBlack: Boolean = false,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
): ColorScheme = enforceSeedTextContrast(createRawSeedColorScheme(seedColor, isDark, style, pureBlack, colorSpec))

/** Reference palette before application-level foreground accessibility adjustments. */
internal fun createRawSeedColorScheme(
    seedColor: Color,
    isDark: Boolean,
    style: SeedPaletteStyle,
    pureBlack: Boolean = false,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
): ColorScheme {
    require(style.enabled)
    // 2025 has different palette generation AND role constraints. Use its complete
    // native implementation; the legacy tone solver must never process a 2025 scheme.
    if (resolveSeedPaletteSpec(style, colorSpec) == ColorSpec.SpecVersion.SPEC_2025) {
        val modern = dynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = requireNotNull(style.material2025Style()),
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            contrastLevel = 0.0,
        )
        return if (isDark && pureBlack) darkenSeedColorScheme(modern) else modern
    }
    val scheme = if (style.isDynamic) {
        val hct = Hct.fromInt(seedColor.toArgb())
        val dynamicScheme = when (style) {
            SeedPaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Neutral -> SchemeNeutral(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Expressive -> SchemeExpressive(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Content -> SchemeContent(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            SeedPaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
            else -> error("Expected a dynamic palette style")
        }
        seedDynamicColorScheme(dynamicScheme)
    } else {
        fixedSeedColorScheme(seedColor, isDark, SeedTones.forStyle(style, isDark))
    }
    return if (isDark && pureBlack) darkenSeedColorScheme(scheme) else scheme
}

private fun fixedSeedColorScheme(seed: Color, isDark: Boolean, tones: SeedTones): ColorScheme {
    val hct = Hct.fromInt(seed.toArgb())
    val cam = Cam16.fromInt(seed.toArgb())
    val hue = if (tones.useCam16) cam.hue else hct.hue
    val chroma = if (tones.useCam16) cam.chroma else hct.chroma
    fun color(group: String, tone: Int): Color {
        val (paletteHue, paletteChroma) = when (group) {
            "primary" -> hue to max(tones.primaryMinChroma ?: 48.0, tones.primaryChroma ?: chroma)
            "secondary" -> hue to max(tones.secondaryMinChroma ?: 0.0, tones.secondaryChroma ?: chroma)
            "tertiary" -> ((hue + (tones.tertiaryHueRotation ?: 60.0)) % 360.0 + 360.0) % 360.0 to
                max(tones.tertiaryMinChroma ?: 0.0, tones.tertiaryChroma ?: chroma)
            "neutral" -> hue to max(tones.neutralMinChroma ?: 0.0, tones.neutralChroma ?: chroma)
            "neutralVariant" -> hue to max(tones.neutralVariantMinChroma ?: 0.0, tones.neutralVariantChroma ?: chroma)
            "error" -> 25.0 to max(tones.errorMinChroma ?: 0.0, tones.errorChroma ?: 84.0)
            else -> error("Unknown tonal palette")
        }
        // Direct HCT evaluation is deliberate: newer TonalPalette.tone(99) has a
        // yellow averaging special case which is absent from the reference tables.
        return Color(Hct.from(paletteHue, paletteChroma, tone.toDouble()).toInt())
    }
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = color("primary", tones.primaryTone),
        onPrimary = color("primary", tones.onPrimaryTone),
        primaryContainer = color("primary", tones.primaryContainerTone),
        onPrimaryContainer = color("primary", tones.onPrimaryContainerTone),
        primaryFixed = color("primary", tones.primaryFixedTone),
        primaryFixedDim = color("primary", tones.primaryFixedDimTone),
        onPrimaryFixed = color("primary", tones.onPrimaryFixedTone),
        onPrimaryFixedVariant = color("primary", tones.onPrimaryFixedVariantTone),
        secondary = color("secondary", tones.secondaryTone),
        onSecondary = color("secondary", tones.onSecondaryTone),
        secondaryContainer = color("secondary", tones.secondaryContainerTone),
        onSecondaryContainer = color("secondary", tones.onSecondaryContainerTone),
        secondaryFixed = color("secondary", tones.secondaryFixedTone),
        secondaryFixedDim = color("secondary", tones.secondaryFixedDimTone),
        onSecondaryFixed = color("secondary", tones.onSecondaryFixedTone),
        onSecondaryFixedVariant = color("secondary", tones.onSecondaryFixedVariantTone),
        tertiary = color("tertiary", tones.tertiaryTone),
        onTertiary = color("tertiary", tones.onTertiaryTone),
        tertiaryContainer = color("tertiary", tones.tertiaryContainerTone),
        onTertiaryContainer = color("tertiary", tones.onTertiaryContainerTone),
        tertiaryFixed = color("tertiary", tones.tertiaryFixedTone),
        tertiaryFixedDim = color("tertiary", tones.tertiaryFixedDimTone),
        onTertiaryFixed = color("tertiary", tones.onTertiaryFixedTone),
        onTertiaryFixedVariant = color("tertiary", tones.onTertiaryFixedVariantTone),
        error = color("error", tones.errorTone),
        onError = color("error", tones.onErrorTone),
        errorContainer = color("error", tones.errorContainerTone),
        onErrorContainer = color("error", tones.onErrorContainerTone),
        surface = color("neutral", tones.surfaceTone),
        surfaceDim = color("neutral", tones.surfaceDimTone),
        surfaceBright = color("neutral", tones.surfaceBrightTone),
        surfaceContainerLowest = color("neutral", tones.surfaceContainerLowestTone),
        surfaceContainerLow = color("neutral", tones.surfaceContainerLowTone),
        surfaceContainer = color("neutral", tones.surfaceContainerTone),
        surfaceContainerHigh = color("neutral", tones.surfaceContainerHighTone),
        surfaceContainerHighest = color("neutral", tones.surfaceContainerHighestTone),
        onSurface = color("neutral", tones.onSurfaceTone),
        onSurfaceVariant = color("neutralVariant", tones.onSurfaceVariantTone),
        outline = color("neutralVariant", tones.outlineTone),
        outlineVariant = color("neutralVariant", tones.outlineVariantTone),
        scrim = color("neutral", tones.scrimTone),
        inverseSurface = color("neutral", tones.inverseSurfaceTone),
        inverseOnSurface = color("neutral", tones.onInverseSurfaceTone),
        inversePrimary = color("primary", tones.inversePrimaryTone),
        surfaceTint = color("primary", tones.surfaceTintTone),
        background = color("neutral", tones.surfaceTone),
        onBackground = color("neutral", tones.onSurfaceTone),
        surfaceVariant = color("neutral", tones.surfaceContainerHighestTone),
    )
}

/** Preserve the legacy preset tones without depending on internal MCU ColorSpec classes. */
private fun legacySeedRole(role: DynamicColor, legacySurface: Boolean): DynamicColor {
    val container = role.name in setOf(
        "on_primary_container", "on_secondary_container", "on_tertiary_container", "on_error_container",
    )
    if (!container && !(legacySurface && role.name == "surface")) return role
    return DynamicColor(
        name = role.name,
        palette = role.palette,
        isBackground = role.isBackground,
        chromaMultiplier = role.chromaMultiplier,
        background = role.background,
        tone = { scheme ->
            val fidelity = scheme.variant == Variant.FIDELITY || scheme.variant == Variant.CONTENT
            when {
                role.name == "surface" -> if (scheme.isDark) 10.0 else 99.0
                role.name != "on_error_container" && (fidelity ||
                    (role.name == "on_primary_container" && scheme.variant == Variant.MONOCHROME)) -> role.tone(scheme)
                else -> if (scheme.isDark) 90.0 else 10.0
            }
        },
        secondBackground = role.secondBackground,
        contrastCurve = role.contrastCurve,
        toneDeltaPair = role.toneDeltaPair,
        opacity = role.opacity,
    )
}

/** Shared role mapping for seeded MCU presets and system core palettes. */
internal fun seedDynamicColorScheme(
    scheme: DynamicScheme,
    legacySurface: Boolean = false,
    resolve: ((DynamicColor, Double) -> Color)? = null,
): ColorScheme {
    val rules = MaterialDynamicColors()
    val resolver = SeedToneResolver(scheme)
    fun color(original: DynamicColor): Color {
        val role = legacySeedRole(original, legacySurface)
        val tone = resolver.tone(role)
        val palette = role.palette(scheme)
        return resolve?.invoke(role, tone) ?: Color(Hct.from(palette.hue, palette.chroma, tone).toInt())
    }
    val base = if (scheme.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = color(rules.primary),
        onPrimary = color(rules.onPrimary),
        primaryContainer = color(rules.primaryContainer),
        onPrimaryContainer = color(rules.onPrimaryContainer),
        primaryFixed = color(rules.primaryFixed),
        primaryFixedDim = color(rules.primaryFixedDim),
        onPrimaryFixed = color(rules.onPrimaryFixed),
        onPrimaryFixedVariant = color(rules.onPrimaryFixedVariant),
        secondary = color(rules.secondary),
        onSecondary = color(rules.onSecondary),
        secondaryContainer = color(rules.secondaryContainer),
        onSecondaryContainer = color(rules.onSecondaryContainer),
        secondaryFixed = color(rules.secondaryFixed),
        secondaryFixedDim = color(rules.secondaryFixedDim),
        onSecondaryFixed = color(rules.onSecondaryFixed),
        onSecondaryFixedVariant = color(rules.onSecondaryFixedVariant),
        tertiary = color(rules.tertiary),
        onTertiary = color(rules.onTertiary),
        tertiaryContainer = color(rules.tertiaryContainer),
        onTertiaryContainer = color(rules.onTertiaryContainer),
        tertiaryFixed = color(rules.tertiaryFixed),
        tertiaryFixedDim = color(rules.tertiaryFixedDim),
        onTertiaryFixed = color(rules.onTertiaryFixed),
        onTertiaryFixedVariant = color(rules.onTertiaryFixedVariant),
        error = color(rules.error),
        onError = color(rules.onError),
        errorContainer = color(rules.errorContainer),
        onErrorContainer = color(rules.onErrorContainer),
        surface = color(rules.surface),
        surfaceDim = color(rules.surfaceDim),
        surfaceBright = color(rules.surfaceBright),
        surfaceContainerLowest = color(rules.surfaceContainerLowest),
        surfaceContainerLow = color(rules.surfaceContainerLow),
        surfaceContainer = color(rules.surfaceContainer),
        surfaceContainerHigh = color(rules.surfaceContainerHigh),
        surfaceContainerHighest = color(rules.surfaceContainerHighest),
        onSurface = color(rules.onSurface),
        onSurfaceVariant = color(rules.onSurfaceVariant),
        outline = color(rules.outline),
        outlineVariant = color(rules.outlineVariant),
        scrim = color(rules.scrim),
        inverseSurface = color(rules.inverseSurface),
        inverseOnSurface = color(rules.inverseOnSurface),
        inversePrimary = color(rules.inversePrimary),
        surfaceTint = color(rules.primary),
        background = color(rules.surface),
        onBackground = color(rules.onSurface),
        surfaceVariant = color(rules.surfaceContainerHighest),
    )
}

/** Reference pure-black role adjustments, interpolated in encoded sRGB like Color.lerp. */
internal fun darkenSeedColorScheme(scheme: ColorScheme): ColorScheme {
    fun Color.darken(amount: Double = 0.5): Color {
        val argb = toArgb()
        fun channel(shift: Int): Int = (((argb ushr shift) and 255) * (1.0 - amount)).roundToInt()
        return Color((argb and -0x1000000) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0))
    }
    return scheme.copy(
        primary = scheme.primary.darken(0.1),
        onPrimary = scheme.onPrimary.darken(0.1),
        primaryContainer = scheme.primaryContainer.darken(0.1),
        onPrimaryContainer = scheme.onPrimaryContainer.darken(0.1),
        inversePrimary = scheme.inversePrimary.darken(0.1),
        secondary = scheme.secondary.darken(0.05),
        onSecondary = scheme.onSecondary.darken(0.05),
        secondaryContainer = scheme.secondaryContainer.darken(0.05),
        onSecondaryContainer = scheme.onSecondaryContainer.darken(0.05),
        error = scheme.error.darken(0.05),
        background = Color.Black,
        surface = Color.Black,
        onSurface = scheme.onSurface.darken(0.15),
        onBackground = scheme.onSurface.darken(0.15),
        surfaceTint = scheme.surfaceTint.darken(),
        inverseSurface = scheme.inverseSurface.darken(),
        inverseOnSurface = scheme.inverseOnSurface.darken(),
        surfaceContainer = scheme.surfaceContainer.darken(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.darken(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.darken(0.4),
        surfaceVariant = scheme.surfaceContainerHighest.darken(0.4),
    )
}
