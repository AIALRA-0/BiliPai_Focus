package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.math.abs

enum class SplashWallpaperLayout {
    FULL_CROP,
    POSTER_CARD_BLUR_BG
}

enum class ProfileWallpaperLayout {
    TOP_BANNER_BLUR_BG,
    POSTER_CARD_BLUR_BG
}

fun resolveSplashWallpaperLayout(
    widthSizeClass: WindowWidthSizeClass,
    imageAspectRatio: Float? = null,
    screenAspectRatio: Float = 9f / 16f,
    compactAspectMismatchThreshold: Float = 0.08f
): SplashWallpaperLayout {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            if (imageAspectRatio != null &&
                abs(imageAspectRatio - screenAspectRatio) > compactAspectMismatchThreshold
            ) {
                SplashWallpaperLayout.POSTER_CARD_BLUR_BG
            } else {
                SplashWallpaperLayout.FULL_CROP
            }
        }
        WindowWidthSizeClass.Medium,
        WindowWidthSizeClass.Expanded -> SplashWallpaperLayout.FULL_CROP
    }
}

fun resolveProfileWallpaperLayout(widthSizeClass: WindowWidthSizeClass): ProfileWallpaperLayout {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> ProfileWallpaperLayout.TOP_BANNER_BLUR_BG
        WindowWidthSizeClass.Medium,
        WindowWidthSizeClass.Expanded -> ProfileWallpaperLayout.POSTER_CARD_BLUR_BG
    }
}
