package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.ui.adaptive.AdaptiveWidthClass

enum class SplashWallpaperLayout {
    FULL_CROP,
    POSTER_CARD_BLUR_BG,
}

enum class ProfileWallpaperLayout {
    TOP_BANNER_BLUR_BG,
    POSTER_CARD_BLUR_BG,
}

@Suppress("UNUSED_PARAMETER") // 保留比例参数兼容调用方；壁纸裁剪由用户预览与对齐设置决定。
fun resolveSplashWallpaperLayout(
    widthClass: AdaptiveWidthClass,
    imageAspectRatio: Float? = null,
    screenAspectRatio: Float = 9f / 16f,
    compactAspectMismatchThreshold: Float = 0.08f,
): SplashWallpaperLayout {
    return when (widthClass) {
        // 长屏手机的壁纸不应因偏离 9:16 而缩成中央卡片。
        AdaptiveWidthClass.Compact,
        AdaptiveWidthClass.Medium,
        AdaptiveWidthClass.Expanded,
        AdaptiveWidthClass.Large,
        AdaptiveWidthClass.ExtraLarge -> SplashWallpaperLayout.FULL_CROP
    }
}

fun resolveProfileWallpaperLayout(widthClass: AdaptiveWidthClass): ProfileWallpaperLayout {
    return when (widthClass) {
        AdaptiveWidthClass.Compact -> ProfileWallpaperLayout.TOP_BANNER_BLUR_BG
        AdaptiveWidthClass.Medium,
        AdaptiveWidthClass.Expanded,
        AdaptiveWidthClass.Large,
        AdaptiveWidthClass.ExtraLarge -> ProfileWallpaperLayout.POSTER_CARD_BLUR_BG
    }
}
