package com.android.purebilibili.feature.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class ProfileHeroChrome(
    val textColor: Color,
    val secondaryTextColor: Color,
    val scrimTopAlpha: Float,
    val scrimBottomAlpha: Float,
    val avatarBorderColor: Color,
    val actionButtonContentColor: Color,
    val actionButtonBorderAlpha: Float,
    val metaChipContainerColor: Color,
    val metaChipBorderColor: Color,
    val useLightStatusBarIcons: Boolean
)

data class ProfileContentChrome(
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val onSurfaceVariantColor: Color,
    val primaryColor: Color,
    val sheetGradientTopColor: Color,
    val sheetGradientBottomColor: Color,
    val cardContainerColor: Color,
    val cardMetadataColor: Color,
    val cardBorderColor: Color,
    val cardShadowElevationDp: Int,
    val sheetShadowElevationDp: Int,
    val sheetBorderColor: Color
)

data class ProfileHeroFallbackGradient(
    val topColor: Color,
    val bottomColor: Color
)

fun resolveProfileHeroChrome(
    hasWallpaper: Boolean,
    isDarkTheme: Boolean,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
): ProfileHeroChrome {
    if (hasWallpaper) {
        return ProfileHeroChrome(
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.72f),
            scrimTopAlpha = 0f,
            scrimBottomAlpha = 0f,
            avatarBorderColor = Color.White.copy(alpha = 0.88f),
            actionButtonContentColor = Color.White,
            actionButtonBorderAlpha = 0.42f,
            metaChipContainerColor = Color.Black.copy(alpha = 0.22f),
            metaChipBorderColor = Color.White.copy(alpha = 0.22f),
            useLightStatusBarIcons = false
        )
    }
    return ProfileHeroChrome(
        textColor = onSurfaceColor,
        secondaryTextColor = onSurfaceVariantColor,
        scrimTopAlpha = 0f,
        scrimBottomAlpha = 0f,
        avatarBorderColor = onSurfaceColor.copy(alpha = 0.16f),
        actionButtonContentColor = onSurfaceColor,
        actionButtonBorderAlpha = 0.28f,
        metaChipContainerColor = onSurfaceColor.copy(alpha = 0.06f),
        metaChipBorderColor = onSurfaceColor.copy(alpha = 0.12f),
        useLightStatusBarIcons = !isDarkTheme
    )
}

fun resolveProfileContentChrome(
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color,
    surfaceContainerLowColor: Color,
    surfaceContainerHighColor: Color,
    surfaceContainerHighestColor: Color,
    outlineVariantColor: Color,
    isDarkTheme: Boolean
): ProfileContentChrome {
    return ProfileContentChrome(
        surfaceColor = surfaceColor,
        onSurfaceColor = onSurfaceColor,
        onSurfaceVariantColor = onSurfaceVariantColor,
        primaryColor = primaryColor,
        sheetGradientTopColor = surfaceContainerLowColor,
        sheetGradientBottomColor = surfaceColor,
        cardContainerColor = surfaceContainerHighColor,
        cardMetadataColor = surfaceContainerHighestColor,
        cardBorderColor = outlineVariantColor.copy(alpha = if (isDarkTheme) 0.28f else 0.22f),
        cardShadowElevationDp = if (isDarkTheme) 0 else 1,
        sheetShadowElevationDp = if (isDarkTheme) 0 else 2,
        sheetBorderColor = onSurfaceColor.copy(alpha = if (isDarkTheme) 0.08f else 0.04f)
    )
}

fun resolveProfileHeroFallbackGradient(
    hasWallpaper: Boolean,
    isDarkTheme: Boolean,
    surfaceColor: Color,
    surfaceVariantColor: Color,
    primaryContainerColor: Color
): ProfileHeroFallbackGradient? {
    if (hasWallpaper) return null
    return if (isDarkTheme) {
        ProfileHeroFallbackGradient(
            topColor = surfaceVariantColor.copy(alpha = 0.92f),
            bottomColor = surfaceColor
        )
    } else {
        ProfileHeroFallbackGradient(
            topColor = primaryContainerColor.copy(alpha = 0.55f),
            bottomColor = surfaceColor
        )
    }
}

fun resolveProfileContentUsesOpaqueSurface(hasWallpaper: Boolean): Boolean = true

fun resolveProfileHeroUsesWallpaperBackground(hasWallpaper: Boolean): Boolean = hasWallpaper