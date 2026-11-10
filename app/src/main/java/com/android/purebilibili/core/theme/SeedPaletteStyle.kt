// Adapted from flex_seed_scheme 5.0.1, Copyright 2022-2025 Mike Rydstrom.
// BSD-3-Clause; full license: assets/licenses/flex_seed_scheme.LICENSE.
package com.android.purebilibili.core.theme

/**
 * Stable preference names; Standard preserves the previous color pipeline.
 * Extended styles preserve upstream accent and surface values. Inadequate foreground
 * pairs are retuned separately; the extended variants are not all official Material presets.
 */
enum class SeedPaletteStyle(val label: String, val isDynamic: Boolean = false) {
    Standard("Standard"),
    TonalSpot("TonalSpot", true),
    Fidelity("Fidelity", true),
    Monochrome("Monochrome", true),
    Neutral("Neutral", true),
    Vibrant("Vibrant", true),
    Expressive("Expressive", true),
    Content("Content", true),
    Rainbow("Rainbow", true),
    FruitSalad("FruitSalad", true),
    Material("Material", false),
    Material3Legacy("Material3Legacy", false),
    Soft("Soft", false),
    Vivid("Vivid", false),
    VividSurfaces("VividSurfaces", false),
    HighContrast("HighContrast", false),
    UltraContrast("UltraContrast", false),
    Jolly("Jolly", false),
    VividBackground("VividBackground", false),
    OneHue("OneHue", false),
    CandyPop("CandyPop", false),
    Chroma("Chroma", false),
    ;

    val enabled: Boolean get() = this != Standard

    companion object {
        fun fromStored(value: String?): SeedPaletteStyle =
            entries.firstOrNull { it.name == value } ?: Standard
    }
}
