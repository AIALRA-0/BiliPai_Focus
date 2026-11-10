package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.android.purebilibili.testutil.readProjectSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import com.android.purebilibili.core.theme.iOSSystemGray6

class ThemeDynamicColorPolicyTest {

    @Test
    fun `all palette specs keep material and miuix roles accessible`() {
        val seeds = listOf(
            Color(0xFF007AFF),
            Color(0xFFFF5722),
            Color(0xFF34C759),
        )
        val modes = listOf(
            Triple(false, false, "light"),
            Triple(true, false, "dark"),
            Triple(true, true, "amoled"),
        )

        PaletteStyle.KnownStyles.forEach { style ->
            ColorSpec.SpecVersion.entries.forEach { spec ->
                seeds.forEach { seed ->
                    modes.forEach { (dark, amoled, mode) ->
                        val label = "$style/$spec/$mode/${seed.value}"
                        val scheme = createBiliPaiStyleColorScheme(
                            seedColor = seed,
                            darkTheme = dark,
                            amoledDarkTheme = amoled,
                            paletteStyle = style,
                            colorSpec = spec,
                        )

                        assertTextContrast(scheme.onBackground, scheme.background, "$label background")
                        assertTextContrast(scheme.onSurface, scheme.surface, "$label surface")
                        assertTextContrast(scheme.onSurfaceVariant, scheme.surfaceVariant, "$label surfaceVariant")
                        assertTextContrast(scheme.onPrimary, scheme.primary, "$label primary")
                        assertTextContrast(scheme.onPrimaryContainer, scheme.primaryContainer, "$label primaryContainer")
                        assertTextContrast(scheme.onSecondary, scheme.secondary, "$label secondary")
                        assertTextContrast(scheme.onSecondaryContainer, scheme.secondaryContainer, "$label secondaryContainer")
                        assertTextContrast(scheme.onTertiary, scheme.tertiary, "$label tertiary")
                        assertTextContrast(scheme.onTertiaryContainer, scheme.tertiaryContainer, "$label tertiaryContainer")
                        assertTextContrast(scheme.onError, scheme.error, "$label error")
                        assertTextContrast(scheme.onErrorContainer, scheme.errorContainer, "$label errorContainer")
                        assertTrue(
                            calculateContrastRatio(scheme.primary, scheme.surface) >= 3f,
                            "$label primary control is below 3:1",
                        )

                        val miuix = resolveMiuixColorsFromMaterialBridge(
                            bridge = createMiuixMaterialBridge(scheme),
                            darkTheme = dark,
                        )
                        assertTextContrast(miuix.onSurface, miuix.surface, "$label miuix surface")
                        assertTextContrast(miuix.onPrimary, miuix.primary, "$label miuix primary")
                        assertTextContrast(
                            miuix.onPrimaryContainer,
                            miuix.primaryContainer,
                            "$label miuix primaryContainer",
                        )
                        assertTrue(
                            calculateContrastRatio(miuix.onSecondary, miuix.secondary) >= 3f,
                            "$label miuix switch roles are below 3:1",
                        )
                        assertTrue(
                            calculateContrastRatio(miuix.sliderKeyPoint, miuix.surface) >= 3f,
                            "$label miuix slider is below 3:1",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `dynamic color keeps miuix bridge on explicit resolved colors`() {
        assertEquals(
            ColorSchemeMode.System,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.FOLLOW_SYSTEM,
                dynamicColorEnabled = true
            )
        )
        assertEquals(
            ColorSchemeMode.Light,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.LIGHT,
                dynamicColorEnabled = true
            )
        )
        assertEquals(
            ColorSchemeMode.Dark,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.DARK,
                dynamicColorEnabled = true
            )
        )
    }

    @Test
    fun `system wallpaper observer only runs when monet dynamic color is active`() {
        assertEquals(
            true,
            shouldObserveSystemWallpaperForDynamicColor(
                dynamicColorActive = true,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            false,
            shouldObserveSystemWallpaperForDynamicColor(
                dynamicColorActive = false,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            false,
            shouldObserveSystemWallpaperForDynamicColor(
                dynamicColorActive = true,
                sdkInt = android.os.Build.VERSION_CODES.R
            )
        )
    }

    @Test
    fun `wallpaper changes refresh AndroidX dynamic schemes immediately and on resume`() {
        val source = readProjectSource(
            "app/src/main/java/com/android/purebilibili/core/theme/Theme.kt"
        )
        val observer = source
            .substringAfter("private fun rememberSystemWallpaperRefreshToken(")
            .substringBefore("private const val SYSTEM_WALLPAPER_PALETTE_SETTLE_DELAY_MS")

        assertTrue(observer.contains("WallpaperManager.OnColorsChangedListener"))
        assertTrue(observer.contains("Intent.ACTION_WALLPAPER_CHANGED"))
        assertTrue(observer.contains("androidx.lifecycle.Lifecycle.Event.ON_RESUME"))
        assertTrue(observer.contains("wallpaperManager.removeOnColorsChangedListener(listener)"))
        assertTrue(observer.contains("lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)"))
    }

    @Test
    fun `resume fallback refresh is throttled so foreground bounces stay cheap`() {
        // 从未刷新过（例如首次进入前台）时必须刷新一次。
        assertTrue(
            shouldRefreshSystemWallpaperPaletteOnResume(lastRefreshAtMs = null, nowMs = 5_000L)
        )
        // 距上次刷新不足阈值：跳过。否则每次切回前台都会重算 dynamic color scheme，
        // 让整棵树重新取色，正好和用户回前台后的第一次切页抢主线程。
        assertFalse(
            shouldRefreshSystemWallpaperPaletteOnResume(
                lastRefreshAtMs = 1_000L,
                nowMs = 1_000L + SYSTEM_WALLPAPER_PALETTE_RESUME_MIN_INTERVAL_MS - 1
            )
        )
        assertTrue(
            shouldRefreshSystemWallpaperPaletteOnResume(
                lastRefreshAtMs = 1_000L,
                nowMs = 1_000L + SYSTEM_WALLPAPER_PALETTE_RESUME_MIN_INTERVAL_MS
            )
        )
    }

    @Test
    fun `md3 color source maps wallpaper to monet and custom to static seed`() {
        assertTrue(
            resolveMd3DynamicColorEnabled(
                source = Md3ColorSource.FOLLOW_WALLPAPER,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            false,
            resolveMd3DynamicColorEnabled(
                source = Md3ColorSource.CUSTOM,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            Color(0xFFFF5722),
            resolveMd3ThemeSeedColor(
                source = Md3ColorSource.CUSTOM,
                customColorHex = "#FF5722",
                themeColorIndex = 0
            )
        )
        assertEquals(
            Color(0xFF007AFF),
            resolveMd3ThemeSeedColor(
                source = Md3ColorSource.FOLLOW_WALLPAPER,
                customColorHex = "#FF5722",
                themeColorIndex = 0
            )
        )
    }

    @Test
    fun `wallpaper color scheme remains the exact AndroidX resolved scheme`() {
        val wallpaperScheme = lightColorScheme(
            primary = Color(0xFF246A73),
            secondary = Color(0xFF4F6367),
            tertiary = Color(0xFF526A92),
            surface = Color(0xFFF5FAF8)
        )

        val result = createBiliPaiStyleColorScheme(
            seedColor = Color.Red,
            darkTheme = false,
            amoledDarkTheme = true,
            paletteStyle = PaletteStyle.Expressive,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
            dynamicBaseScheme = wallpaperScheme
        )

        assertEquals(wallpaperScheme, result)
    }

    @Test
    fun `static color modes map to plain miuix color scheme modes`() {
        assertEquals(
            ColorSchemeMode.System,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.FOLLOW_SYSTEM,
                dynamicColorEnabled = false
            )
        )
        assertEquals(
            ColorSchemeMode.Light,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.LIGHT,
                dynamicColorEnabled = false
            )
        )
        assertEquals(
            ColorSchemeMode.Dark,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.DARK,
                dynamicColorEnabled = false
            )
        )
    }

    @Test
    fun `color style preference defaults to tonal spot and rejects invalid values`() {
        assertEquals(PaletteStyle.TonalSpot, resolvePaletteStylePreference(null))
        assertEquals(PaletteStyle.TonalSpot, resolvePaletteStylePreference("not-a-style"))
        assertEquals(PaletteStyle.Vibrant, resolvePaletteStylePreference(PaletteStyle.Vibrant.name))
    }

    @Test
    fun `color spec preference defaults to spec 2025 and rejects invalid values`() {
        assertEquals(ColorSpec.SpecVersion.SPEC_2025, resolveColorSpecPreference(null))
        assertEquals(ColorSpec.SpecVersion.SPEC_2025, resolveColorSpecPreference("not-a-spec"))
        assertEquals(
            ColorSpec.SpecVersion.SPEC_2021,
            resolveColorSpecPreference(ColorSpec.SpecVersion.SPEC_2021.name)
        )
    }

    @Test
    fun `color contrast preference defaults to material default and rejects invalid values`() {
        assertEquals(Contrast.Default, resolveColorContrastPreference(null))
        assertEquals(Contrast.Default, resolveColorContrastPreference("not-a-contrast"))
        assertEquals(Contrast.High, resolveColorContrastPreference(Contrast.High.name))
        assertEquals(Contrast.Reduced, resolveColorContrastPreference(Contrast.Reduced.name))
    }

    @Test
    fun `amoled overrides keep monet accents while forcing black surfaces`() {
        val monetScheme = darkColorScheme(
            primary = Color(0xFF84F2A4),
            secondary = Color(0xFF79D7FF),
            tertiary = Color(0xFFFFB3C1),
            background = Color(0xFF101414),
            surface = Color(0xFF161B1A),
            surfaceVariant = Color(0xFF29312E),
            surfaceContainer = Color(0xFF1E2523),
            outline = Color(0xFF6F7975),
            outlineVariant = Color(0xFF414946)
        )

        val result = applyAmoledSurfaceOverrides(monetScheme)

        assertEquals(monetScheme.primary, result.primary)
        assertEquals(monetScheme.secondary, result.secondary)
        assertEquals(monetScheme.tertiary, result.tertiary)
        assertEquals(Color.Black, result.background)
        assertEquals(Color.Black, result.surface)
        assertEquals(Color(0xFF050505), result.surfaceVariant)
        assertEquals(Color(0xFF090909), result.surfaceContainer)
        // 容器层五级 + Bright/Dim 也必须压进 AMOLED 暗部阶梯,避免纯黑底上突兀的亮灰层。
        assertEquals(Color.Black, result.surfaceContainerLowest)
        assertEquals(Color(0xFF050505), result.surfaceContainerLow)
        assertEquals(Color(0xFF121212), result.surfaceContainerHigh)
        assertEquals(Color(0xFF1A1A1A), result.surfaceContainerHighest)
        assertEquals(Color(0xFF0D0D0D), result.surfaceBright)
        assertEquals(Color.Black, result.surfaceDim)
    }

    @Test
    fun `custom seed md3 light scheme derives distinct secondary and tertiary roles from source color`() {
        val scheme = createBiliPaiStyleColorScheme(
            seedColor = Color(0xFF6750A4),
            darkTheme = false,
            amoledDarkTheme = false,
            // 生产默认值（Theme.kt）；签名新增必填参数后此处按默认行为补齐。
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )

        assertNotEquals(scheme.primary, scheme.secondary)
        assertNotEquals(scheme.primary, scheme.tertiary)
        assertNotEquals(scheme.primaryContainer, scheme.secondaryContainer)
        assertNotEquals(scheme.primaryContainer, scheme.tertiaryContainer)
        assertTrue(calculateContrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onTertiaryContainer, scheme.tertiaryContainer) >= 4.5f)
    }

    @Test
    fun `custom seed scheme does not stamp raw seed into surfaceTint`() {
        val brightSeed = Color(0xFF007AFF)

        val scheme = createBiliPaiStyleColorScheme(
            seedColor = brightSeed,
            darkTheme = false,
            amoledDarkTheme = false,
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )

        // tonal elevation 表面会混合 surfaceTint；未调和的原始种子 hex 一旦进入
        // surfaceTint，弹窗等表面就会被染成过饱和色，因此必须保持 materialkolor
        // 原生调和值。
        assertNotEquals(brightSeed, scheme.surfaceTint)
        assertNotEquals(scheme.primaryContainer, scheme.tertiaryContainer)
    }

    @Test
    fun `custom seed md3 surfaces should respond to different source colors instead of staying fixed`() {
        val blueScheme = createBiliPaiStyleColorScheme(
            seedColor = Color(0xFF007AFF),
            darkTheme = false,
            amoledDarkTheme = false,
            // 生产默认值（Theme.kt）；签名新增必填参数后此处按默认行为补齐。
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )
        val orangeScheme = createBiliPaiStyleColorScheme(
            seedColor = Color(0xFFFF5722),
            darkTheme = false,
            amoledDarkTheme = false,
            // 生产默认值（Theme.kt）；签名新增必填参数后此处按默认行为补齐。
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )

        assertNotEquals(blueScheme.background, orangeScheme.background)
        assertNotEquals(blueScheme.surfaceVariant, orangeScheme.surfaceVariant)
        assertNotEquals(blueScheme.outlineVariant, orangeScheme.outlineVariant)
    }

    @Test
    fun `custom seed md3 dark scheme keeps readable accents and source tinted surfaces`() {
        val scheme = createBiliPaiStyleColorScheme(
            seedColor = Color(0xFF34C759),
            darkTheme = true,
            amoledDarkTheme = false,
            // 生产默认值（Theme.kt）；签名新增必填参数后此处按默认行为补齐。
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )

        assertNotEquals(scheme.primary, scheme.secondary)
        assertNotEquals(scheme.primary, scheme.tertiary)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onSecondary, scheme.secondary) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onTertiary, scheme.tertiary) >= 4.5f)
        assertNotEquals(Color(0xFF121212), scheme.background)
        assertNotEquals(Color(0xFF1E1E1E), scheme.surface)
    }

    @Test
    fun `custom seed dark scheme keeps theme identity via surfaceTint instead of raw primary`() {
        val selectedThemeColor = Color(0xFF007AFF)

        val scheme = createBiliPaiStyleColorScheme(
            seedColor = selectedThemeColor,
            darkTheme = true,
            amoledDarkTheme = false,
            // 生产默认值（Theme.kt）；签名新增必填参数后此处按默认行为补齐。
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )

        // 原始种子色不再强塞进 primary(避免亮色种子产生黑 onPrimary),
        // 品牌一致性由 surfaceTint 承载,控制色使用 HCT 映射后的可读角色。
        assertEquals(selectedThemeColor, scheme.surfaceTint)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.primary, scheme.surface) >= 3f)
    }

    @Test
    fun `ios light scheme keeps grouped list gray background and white cards`() {
        val scheme = createIosColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = false,
            amoledDarkTheme = false
        )

        assertEquals(iOSSystemGray6, scheme.background)
        assertEquals(Color.White, scheme.surface)
        assertEquals(Color(0xFF007AFF), scheme.primary)
    }

    @Test
    fun `ios dark scheme keeps ios neutral surfaces instead of md3 tinted neutrals`() {
        val iosScheme = createIosColorScheme(
            primaryColor = Color(0xFF34C759),
            darkTheme = true,
            amoledDarkTheme = false
        )
        val md3Scheme = createBiliPaiStyleColorScheme(
            seedColor = Color(0xFF34C759),
            darkTheme = true,
            amoledDarkTheme = false,
            // 生产默认值（Theme.kt）；签名新增必填参数后此处按默认行为补齐。
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        )

        assertNotEquals(md3Scheme.background, iosScheme.background)
        assertNotEquals(md3Scheme.surface, iosScheme.surface)
        assertEquals(Color(0xFF34C759), iosScheme.primary)
    }

    @Test
    fun `ios dynamic accent merge keeps ios surfaces while adopting monet accents`() {
        val base = createIosColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = false,
            amoledDarkTheme = false
        )
        val dynamicAccent = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFFFFBFE)
        )

        val merged = alignIosColorSchemeWithDynamicAccent(
            baseScheme = base,
            dynamicAccentScheme = dynamicAccent
        )

        assertEquals(base.background, merged.background)
        assertEquals(base.surface, merged.surface)
        assertEquals(dynamicAccent.primary, merged.primary)
        assertEquals(dynamicAccent.secondary, merged.secondary)
        assertEquals(dynamicAccent.tertiary, merged.tertiary)
    }

    @Test
    fun `ios amoled scheme forces black surfaces`() {
        val scheme = createIosColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = true,
            amoledDarkTheme = true
        )

        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertEquals(Color(0xFF007AFF), scheme.primary)
    }

    private fun assertTextContrast(foreground: Color, background: Color, label: String) {
        assertTrue(
            calculateContrastRatio(foreground, background) >= 4.5f,
            "$label text is below 4.5:1",
        )
    }
}
