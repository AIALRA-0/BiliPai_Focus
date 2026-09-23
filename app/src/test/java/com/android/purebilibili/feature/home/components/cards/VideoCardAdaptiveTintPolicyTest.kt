package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.feature.home.HomeCardWallpaperSurfaceMode
import com.android.purebilibili.feature.home.resolveHomeCardWallpaperSurfaceMode
import androidx.palette.graphics.Palette
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoCardAdaptiveTintPolicyTest {

    @Test
    fun resolveCoverBottomSamplingRegion_targetsWholeCover() {
        val region = resolveCoverBottomSamplingRegion(width = 800, height = 600)
        assertEquals(0, region.left)
        assertEquals(0, region.top)
        assertEquals(800, region.right)
        assertEquals(600, region.bottom)
    }

    @Test
    fun resolveCoverBottomSamplingRegion_handlesEdgeDimensions() {
        val region = resolveCoverBottomSamplingRegion(width = 0, height = 0)
        assertEquals(0, region.left)
        assertEquals(0, region.top)
        assertEquals(1, region.right)
        assertEquals(1, region.bottom)
    }

    @Test
    fun interpolateWallpaperColor_interpolatesAccurately() {
        val palette = WallpaperPalette(
            topColor = Color(0xFF0000FF), // Pure Blue
            bottomColor = Color(0xFFFF0000) // Pure Red
        )

        val atTop = interpolateWallpaperColor(palette, yFraction = 0f)
        assertEquals(palette.topColor, atTop)

        val atBottom = interpolateWallpaperColor(palette, yFraction = 1f)
        assertEquals(palette.bottomColor, atBottom)

        // Clamping check
        val belowZero = interpolateWallpaperColor(palette, yFraction = -0.5f)
        assertEquals(palette.topColor, belowZero)

        val aboveOne = interpolateWallpaperColor(palette, yFraction = 1.5f)
        assertEquals(palette.bottomColor, aboveOne)
    }

    @Test
    fun interpolateWallpaperColor_multiStops_interpolatesContinuously() {
        val stops = listOf(
            Color(0xFFFF0000), // Red at 0.0
            Color(0xFF00FF00), // Green at 0.5
            Color(0xFF0000FF)  // Blue at 1.0
        )
        val palette = WallpaperPalette(
            topColor = stops.first(),
            bottomColor = stops.last(),
            stops = stops
        )

        val atZero = interpolateWallpaperColor(palette, 0f)
        assertEquals(Color(0xFFFF0000), atZero)

        val atHalf = interpolateWallpaperColor(palette, 0.5f)
        assertEquals(Color(0xFF00FF00), atHalf)

        val atOne = interpolateWallpaperColor(palette, 1f)
        assertEquals(Color(0xFF0000FF), atOne)
    }

    @Test
    fun resolveVideoCardAmbientDrawSpec_wallpaperTakesExclusivePrecedenceOverCoverTint() {
        val palette = WallpaperPalette(
            topColor = Color.Blue,
            bottomColor = Color.Red
        )
        val coverTint = Color.Green
        val defaultContainer = Color.Black
        val defaultBorder = Color.Gray

        val spec = resolveVideoCardAmbientDrawSpec(
            wallpaperPalette = palette,
            yFraction = 0.5f,
            coverTint = coverTint,
            wallpaperTintEnabled = true,
            isDarkTheme = true,
            defaultContainerColor = defaultContainer,
            defaultBorderColor = defaultBorder
        )

        assertEquals(0f, spec.coverGlowAlpha)
        // Container color comes from the interpolated wallpaper, not the cover.
        assertTrue(spec.containerColor != defaultContainer)
        assertTrue(spec.borderColor != coverTint)
    }

    @Test
    fun shouldUseCoverTintForCard_onlyUsesCoverWhenWallpaperIsDisabled() {
        assertTrue(shouldUseCoverTintForCard(false, Color.Green))
        assertEquals(false, shouldUseCoverTintForCard(true, Color.Green))
        assertEquals(false, shouldUseCoverTintForCard(false, null))
    }

    @Test
    fun representativeSwatch_prefersLargestUsefulRegionOverSmallBrightSubtitle() {
        val subtitleRed = Palette.Swatch(0xFFFF0000.toInt(), 12)
        val coverBlue = Palette.Swatch(0xFF336699.toInt(), 160)
        val whiteBackground = Palette.Swatch(0xFFFFFFFF.toInt(), 400)

        assertEquals(coverBlue, VideoCardCoverColorStore.resolveRepresentativeSwatch(
            listOf(subtitleRed, coverBlue, whiteBackground)
        ))
    }

    @Test
    fun representativeSwatch_keeps_grayscale_fallback_when_no_colorful_region_exists() {
        val black = Palette.Swatch(0xFF111111.toInt(), 120)
        val white = Palette.Swatch(0xFFF2F2F2.toInt(), 250)

        assertEquals(white, VideoCardCoverColorStore.resolveRepresentativeSwatch(listOf(black, white)))
    }

    @Test
    fun resolveVideoCardAmbientDrawSpec_fallsBackWhenDisabled() {
        val defaultContainer = Color(0xFF1E1E1E)
        val defaultBorder = Color(0xFF333333)

        val spec = resolveVideoCardAmbientDrawSpec(
            wallpaperPalette = null,
            yFraction = 0.5f,
            coverTint = null,
            wallpaperTintEnabled = false,
            isDarkTheme = true,
            defaultContainerColor = defaultContainer,
            defaultBorderColor = defaultBorder
        )

        assertEquals(defaultContainer, spec.containerColor)
        assertEquals(0f, spec.coverGlowAlpha)
        assertEquals(defaultBorder, spec.borderColor)
    }

    @Test
    fun resolveVideoCardAmbientDrawSpec_producesTranslucentFrostedGlassWhenWallpaperAbsent() {
        val defaultContainer = Color.White
        val defaultBorder = Color.LightGray

        val spec = resolveVideoCardAmbientDrawSpec(
            wallpaperPalette = null,
            yFraction = 0.5f,
            coverTint = null,
            wallpaperTintEnabled = true,
            isDarkTheme = false,
            defaultContainerColor = defaultContainer,
            defaultBorderColor = defaultBorder,
            frostedGlassEnabled = true
        )

        // Must be translucent (alpha < 1.0f), never opaque white
        assertTrue(spec.containerColor.alpha < 1.0f)
        assertTrue(spec.containerColor.alpha > 0.05f)
    }

    @Test
    fun resolveHomeCardWallpaperSurfaceMode_usesRealtimeFrostedForReadyStaticWallpaper() {
        assertEquals(
            HomeCardWallpaperSurfaceMode.REALTIME_FROSTED,
            resolveHomeCardWallpaperSurfaceMode(
                dynamicTintEnabled = true,
                wallpaperVisible = true,
                wallpaperIsStatic = true,
                backdropReady = true,
                blurEnabled = true,
                isDataSaverActive = false,
                lowBlurBudgetForced = false,
                sdkInt = 34,
            )
        )
    }

    @Test
    fun resolveHomeCardWallpaperSurfaceMode_fallsBackForAnimatedOrUnavailableWallpaper() {
        val animated = resolveHomeCardWallpaperSurfaceMode(
            dynamicTintEnabled = true,
            wallpaperVisible = true,
            wallpaperIsStatic = false,
            backdropReady = true,
            blurEnabled = true,
            isDataSaverActive = false,
            lowBlurBudgetForced = false,
            sdkInt = 34,
        )
        val notReady = resolveHomeCardWallpaperSurfaceMode(
            dynamicTintEnabled = true,
            wallpaperVisible = true,
            wallpaperIsStatic = true,
            backdropReady = false,
            blurEnabled = true,
            isDataSaverActive = false,
            lowBlurBudgetForced = false,
            sdkInt = 34,
        )
        assertEquals(HomeCardWallpaperSurfaceMode.LIGHTWEIGHT_TINT, animated)
        assertEquals(HomeCardWallpaperSurfaceMode.LIGHTWEIGHT_TINT, notReady)
    }

    @Test
    fun resolveHomeCardWallpaperSurfaceMode_disablesRealtimeForBudgetOrPlatform() {
        val dataSaver = resolveHomeCardWallpaperSurfaceMode(
            dynamicTintEnabled = true,
            wallpaperVisible = true,
            wallpaperIsStatic = true,
            backdropReady = true,
            blurEnabled = true,
            isDataSaverActive = true,
            lowBlurBudgetForced = false,
            sdkInt = 34,
        )
        val oldApi = resolveHomeCardWallpaperSurfaceMode(
            dynamicTintEnabled = true,
            wallpaperVisible = true,
            wallpaperIsStatic = true,
            backdropReady = true,
            blurEnabled = true,
            isDataSaverActive = false,
            lowBlurBudgetForced = false,
            sdkInt = 30,
        )
        assertEquals(HomeCardWallpaperSurfaceMode.LIGHTWEIGHT_TINT, dataSaver)
        assertEquals(HomeCardWallpaperSurfaceMode.LIGHTWEIGHT_TINT, oldApi)
    }

    @Test
    fun resolveHomeCardWallpaperSurfaceMode_usesStandardWhenDisabledOrNoWallpaper() {
        assertEquals(
            HomeCardWallpaperSurfaceMode.STANDARD,
            resolveHomeCardWallpaperSurfaceMode(
                dynamicTintEnabled = false,
                wallpaperVisible = true,
                wallpaperIsStatic = true,
                backdropReady = true,
                blurEnabled = true,
                isDataSaverActive = false,
                lowBlurBudgetForced = false,
                sdkInt = 34,
            )
        )
        assertEquals(
            HomeCardWallpaperSurfaceMode.STANDARD,
            resolveHomeCardWallpaperSurfaceMode(
                dynamicTintEnabled = true,
                wallpaperVisible = false,
                wallpaperIsStatic = true,
                backdropReady = true,
                blurEnabled = true,
                isDataSaverActive = false,
                lowBlurBudgetForced = false,
                sdkInt = 34,
            )
        )
    }

    @Test
    fun resolveVideoCardAdaptiveContentColors_usesWhiteTextForDarkCoverInLightMode() {
        val darkCover = Color(0xFF101828)
        val defaultOnSurface = Color(0xFF1D1B20)
        val defaultOnSurfaceVariant = Color(0xFF49454F)

        val colors = resolveVideoCardAdaptiveContentColors(
            wallpaperPalette = null,
            coverTint = darkCover,
            wallpaperTintEnabled = false,
            isDarkTheme = false,
            defaultOnSurface = defaultOnSurface,
            defaultOnSurfaceVariant = defaultOnSurfaceVariant,
            homeCardDynamicTintEnabled = true
        )

        assertEquals(Color.White, colors.titleColor)
        assertTrue(colors.isDarkSurface)
    }

    @Test
    fun resolveVideoCardAdaptiveContentColors_usesOnSurfaceForLightCoverInLightMode() {
        val lightCover = Color(0xFFE8F0FE)
        val defaultOnSurface = Color(0xFF1D1B20)
        val defaultOnSurfaceVariant = Color(0xFF49454F)

        val colors = resolveVideoCardAdaptiveContentColors(
            wallpaperPalette = null,
            coverTint = lightCover,
            wallpaperTintEnabled = false,
            isDarkTheme = false,
            defaultOnSurface = defaultOnSurface,
            defaultOnSurfaceVariant = defaultOnSurfaceVariant,
            homeCardDynamicTintEnabled = true
        )

        assertEquals(defaultOnSurface, colors.titleColor)
        assertEquals(defaultOnSurfaceVariant, colors.subtitleColor)
        assertEquals(false, colors.isDarkSurface)
    }

    @Test
    fun resolveVideoCardAdaptiveContentColors_usesWhiteTextForDarkWallpaperInLightMode() {
        val darkWallpaper = WallpaperPalette(
            topColor = Color(0xFF0F172A),
            bottomColor = Color(0xFF020617),
            dominantColor = Color(0xFF0F172A)
        )
        val defaultOnSurface = Color(0xFF1D1B20)
        val defaultOnSurfaceVariant = Color(0xFF49454F)

        val colors = resolveVideoCardAdaptiveContentColors(
            wallpaperPalette = darkWallpaper,
            coverTint = null,
            wallpaperTintEnabled = true,
            isDarkTheme = false,
            defaultOnSurface = defaultOnSurface,
            defaultOnSurfaceVariant = defaultOnSurfaceVariant,
            homeCardDynamicTintEnabled = true
        )

        assertEquals(Color.White, colors.titleColor)
        assertTrue(colors.isDarkSurface)
    }

    @Test
    fun resolveVideoCardAdaptiveContentColors_respectsDynamicTintDisabled() {
        val darkCover = Color(0xFF101828)
        val defaultOnSurface = Color(0xFF1D1B20)
        val defaultOnSurfaceVariant = Color(0xFF49454F)

        val colors = resolveVideoCardAdaptiveContentColors(
            wallpaperPalette = null,
            coverTint = darkCover,
            wallpaperTintEnabled = false,
            isDarkTheme = false,
            defaultOnSurface = defaultOnSurface,
            defaultOnSurfaceVariant = defaultOnSurfaceVariant,
            homeCardDynamicTintEnabled = false
        )

        assertEquals(defaultOnSurface, colors.titleColor)
        assertEquals(defaultOnSurfaceVariant, colors.subtitleColor)
        assertEquals(false, colors.isDarkSurface)
    }
}
