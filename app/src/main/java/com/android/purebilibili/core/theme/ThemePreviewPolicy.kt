package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import com.android.purebilibili.core.store.ThemeRoleOverrides
import com.android.purebilibili.core.store.WallpaperPaletteOverrides
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/** 复用主题宿主读取的壁纸色板，避免预览回退预设色或重新读取动画中的颜色。 */
internal data class ThemeWallpaperPreviewSchemes(
    val light: ColorScheme? = null,
    val dark: ColorScheme? = null,
)

internal val LocalThemeWallpaperPreviewSchemes = staticCompositionLocalOf { ThemeWallpaperPreviewSchemes() }

internal data class ThemePreviewConfiguration(
    val uiStyle: AppUiStyle = AppUiStyle.MATERIAL3,
    val source: Md3ColorSource = Md3ColorSource.CUSTOM,
    val liquidGlassEnabled: Boolean = false,
    val amoledDarkTheme: Boolean = false,
    val roleOverrides: ThemeRoleOverrides = ThemeRoleOverrides(),
    val wallpaperOverrides: WallpaperPaletteOverrides = WallpaperPaletteOverrides(),
    val syncControlAccentToSeed: Boolean = false,
    val wallpaperSchemes: ThemeWallpaperPreviewSchemes = ThemeWallpaperPreviewSchemes(),
)

/** Matches saving a candidate: generation, saved edits, glass selection, wallpaper edits,
 * native bridge and foreground correction. No persisted settings are written by preview. */
internal fun resolveThemePreviewPalette(
    seed: Color,
    darkTheme: Boolean,
    style: PaletteStyle,
    spec: ColorSpec.SpecVersion,
    contrast: Contrast,
    seedStyle: SeedPaletteStyle,
    configuration: ThemePreviewConfiguration,
): AppThemePalette {
    val base = createBiliPaiStyleColorScheme(
        seed, darkTheme, configuration.amoledDarkTheme, style, spec,
        uiStyle = configuration.uiStyle, colorContrast = contrast, seedPaletteStyle = seedStyle,
        dynamicBaseScheme = if (configuration.source == Md3ColorSource.FOLLOW_WALLPAPER) {
            if (darkTheme) configuration.wallpaperSchemes.dark else configuration.wallpaperSchemes.light
        } else null,
    )
    val overrides = resolveEffectiveThemeRoleOverrides(configuration.source, configuration.roleOverrides)
        .let { if (seedStyle.enabled) it.copy(enabled = false) else it }
        .let { if (configuration.syncControlAccentToSeed) syncThemeRoleControlAccent(it, formatMd3CustomColorHex(seed)) else it }
    val configured = resolveConfiguredMaterialScheme(
        base, overrides, darkTheme, style, spec, contrast, seedStyle,
        configuration.source, seed, configuration.liquidGlassEnabled, configuration.uiStyle,
    )
    return resolveAppThemeEndpointPalette(
        configured, configuration.uiStyle, configuration.source, darkTheme,
        amoledDarkTheme = configuration.amoledDarkTheme,
        wallpaperOverrides = configuration.wallpaperOverrides,
        customSeed = seed, roleOverrides = overrides,
        liquidGlassEnabled = configuration.liquidGlassEnabled,
        preserveGeneratedPalette = seedStyle.enabled,
    )
}
