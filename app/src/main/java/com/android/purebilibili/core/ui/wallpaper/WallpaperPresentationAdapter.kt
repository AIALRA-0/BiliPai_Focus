package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.ui.adaptive.toAdaptiveWidthClass
import com.android.purebilibili.core.ui.wallpaper.resolveProfileWallpaperLayout as resolveSharedProfileWallpaperLayout
import com.android.purebilibili.core.ui.wallpaper.resolveSplashWallpaperLayout as resolveSharedSplashWallpaperLayout
import com.android.purebilibili.core.util.WindowWidthSizeClass

fun resolveSplashWallpaperLayout(
    widthSizeClass: WindowWidthSizeClass,
    imageAspectRatio: Float? = null,
    screenAspectRatio: Float = 9f / 16f,
    compactAspectMismatchThreshold: Float = 0.08f,
): SplashWallpaperLayout = resolveSharedSplashWallpaperLayout(
    widthClass = widthSizeClass.toAdaptiveWidthClass(),
    imageAspectRatio = imageAspectRatio,
    screenAspectRatio = screenAspectRatio,
    compactAspectMismatchThreshold = compactAspectMismatchThreshold,
)

fun resolveProfileWallpaperLayout(
    widthSizeClass: WindowWidthSizeClass,
): ProfileWallpaperLayout = resolveSharedProfileWallpaperLayout(
    widthClass = widthSizeClass.toAdaptiveWidthClass(),
)
