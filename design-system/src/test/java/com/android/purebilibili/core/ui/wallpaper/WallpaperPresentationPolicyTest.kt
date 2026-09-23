package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.ui.adaptive.AdaptiveWidthClass
import kotlin.test.Test
import kotlin.test.assertEquals

class WallpaperPresentationPolicyTest {

    @Test
    fun compactSplashKeepsFullCropForTallWallpapers() {
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(AdaptiveWidthClass.Compact),
        )
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(
                widthClass = AdaptiveWidthClass.Compact,
                imageAspectRatio = 0.42f,
            ),
        )
    }

    @Test
    fun tabletSplashUsesFullCrop() {
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(AdaptiveWidthClass.Medium),
        )
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(AdaptiveWidthClass.Expanded),
        )
    }

    @Test
    fun profileLayoutSeparatesCompactAndTabletWidths() {
        assertEquals(
            ProfileWallpaperLayout.TOP_BANNER_BLUR_BG,
            resolveProfileWallpaperLayout(AdaptiveWidthClass.Compact),
        )
        assertEquals(
            ProfileWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveProfileWallpaperLayout(AdaptiveWidthClass.Medium),
        )
    }
}
