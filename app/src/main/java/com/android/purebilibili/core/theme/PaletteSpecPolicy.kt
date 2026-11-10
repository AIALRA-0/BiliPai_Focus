package com.android.purebilibili.core.theme

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/** Matches the pinned MCU DynamicScheme variant/spec fallback rules. */
internal fun supportsMaterialSpec2025(style: PaletteStyle): Boolean = when (style) {
    PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Expressive -> true
    else -> false
}

internal fun resolveMaterialPaletteSpec(
    style: PaletteStyle,
    requested: ColorSpec.SpecVersion,
): ColorSpec.SpecVersion = when {
    // SPEC_2026 is native to CMF; the four modern variants still resolve to 2025.
    style is PaletteStyle.Cmf -> ColorSpec.SpecVersion.SPEC_2026
    !supportsMaterialSpec2025(style) -> ColorSpec.SpecVersion.SPEC_2021
    requested == ColorSpec.SpecVersion.SPEC_2026 -> ColorSpec.SpecVersion.SPEC_2025
    else -> requested
}

internal fun SeedPaletteStyle.material2025Style(): PaletteStyle? = when (this) {
    SeedPaletteStyle.TonalSpot -> PaletteStyle.TonalSpot
    SeedPaletteStyle.Neutral -> PaletteStyle.Neutral
    SeedPaletteStyle.Vibrant -> PaletteStyle.Vibrant
    SeedPaletteStyle.Expressive -> PaletteStyle.Expressive
    else -> null
}

internal fun resolveSeedPaletteSpec(
    style: SeedPaletteStyle,
    requested: ColorSpec.SpecVersion,
): ColorSpec.SpecVersion = style.material2025Style()?.let { resolveMaterialPaletteSpec(it, requested) }
    ?: ColorSpec.SpecVersion.SPEC_2021
