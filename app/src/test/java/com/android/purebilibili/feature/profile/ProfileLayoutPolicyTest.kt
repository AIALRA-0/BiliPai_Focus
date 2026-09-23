package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileLayoutPolicyTest {

    @Test
    fun heroHeight_usesScreenFractionAndClamps() {
        val tokens = resolveProfileLayoutTokens()
        val heroHeight = resolveProfileHeroHeightDp(screenHeightDp = 800)

        assertEquals(tokens.heroMinHeightDp.toFloat(), heroHeight, 0.001f)
        assertTrue(heroHeight in tokens.heroMinHeightDp.toFloat()..tokens.heroMaxHeightDp.toFloat())
    }

    @Test
    fun heroHeight_clampsOnVerySmallScreens() {
        assertEquals(336f, resolveProfileHeroHeightDp(screenHeightDp = 500))
    }

    @Test
    fun heroHeight_clampsOnVeryTallScreens() {
        assertEquals(360f, resolveProfileHeroHeightDp(screenHeightDp = 1200))
    }

    @Test
    fun topBannerHeight_delegatesToHeroHeightPolicy() {
        assertEquals(
            resolveProfileHeroHeightDp(
                screenHeightDp = referenceProfileScreenHeightDp(WindowWidthSizeClass.Compact),
                widthSizeClass = WindowWidthSizeClass.Compact
            ),
            resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun tabletWallpaper_reservesLargerVisibleBannerBeforeSplitContent() {
        assertEquals(
            224,
            resolveProfileTabletWallpaperRevealHeightDp(
                useSplitLayout = true,
                hasWallpaper = true
            )
        )
        assertEquals(
            0,
            resolveProfileTabletWallpaperRevealHeightDp(
                useSplitLayout = true,
                hasWallpaper = false
            )
        )
        assertEquals(
            0,
            resolveProfileTabletWallpaperRevealHeightDp(
                useSplitLayout = false,
                hasWallpaper = true
            )
        )
    }

    @Test
    fun cardTokens_useUnifiedPosterDimensions() {
        val tokens = resolveProfileCardTokens()

        assertEquals(148, tokens.widthDp)
        assertEquals(56, tokens.metadataHeightDp)
        assertTrue(resolveProfileCardHeightDp(tokens) > resolveProfileCardCoverHeightDp(tokens))
    }
}
