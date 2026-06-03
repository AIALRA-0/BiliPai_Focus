package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.store.HomeWallpaperEffectMode
import com.android.purebilibili.core.ui.AppSurfaceTokens

data class HomeGlassChromeStyle(
    val containerAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float,
    val shadowAlpha: Float
)

data class HomeGlassPillStyle(
    val containerAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float,
    val contentAlpha: Float
)

data class HomeGlassResolvedColors(
    val containerColor: Color,
    val borderColor: Color,
    val highlightColor: Color
)

enum class HomeRefreshTipSurfaceStyle {
    GLASS,
    PLAIN
}

data class HomeRefreshTipAppearance(
    val surfaceStyle: HomeRefreshTipSurfaceStyle,
    val borderWidthDp: Float,
    val tonalElevationDp: Float,
    val shadowElevationDp: Float
)

data class HomeWallpaperBackdropAppearance(
    val visible: Boolean,
    val baseBackgroundAlpha: Float,
    val detailAlpha: Float,
    val scrimAlpha: Float,
    val bottomScrimAlpha: Float,
    val blurRadiusDp: Float
)

data class HomeCardInfoSurfaceAppearance(
    val useTintedSurface: Boolean,
    val containerAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float
)

internal fun resolveHomeGlassChromeStyle(
    glassEnabled: Boolean,
    blurEnabled: Boolean
): HomeGlassChromeStyle {
    return when {
        !blurEnabled -> HomeGlassChromeStyle(
            containerAlpha = 0.88f,
            borderAlpha = 0.10f,
            highlightAlpha = 0.08f,
            shadowAlpha = 0.08f
        )

        glassEnabled -> HomeGlassChromeStyle(
            containerAlpha = 0.16f,
            borderAlpha = 0.18f,
            highlightAlpha = 0.22f,
            shadowAlpha = 0.14f
        )

        else -> HomeGlassChromeStyle(
            containerAlpha = 0.72f,
            borderAlpha = 0.12f,
            highlightAlpha = 0.10f,
            shadowAlpha = 0.10f
        )
    }
}

internal fun resolveHomeGlassPillStyle(
    glassEnabled: Boolean,
    blurEnabled: Boolean,
    emphasized: Boolean
): HomeGlassPillStyle {
    return when {
        !blurEnabled -> HomeGlassPillStyle(
            containerAlpha = if (emphasized) 0.92f else 0.88f,
            borderAlpha = 0.12f,
            highlightAlpha = if (emphasized) 0.12f else 0.08f,
            contentAlpha = 0.96f
        )

        glassEnabled -> HomeGlassPillStyle(
            containerAlpha = if (emphasized) 0.28f else 0.24f,
            borderAlpha = 0.16f,
            highlightAlpha = if (emphasized) 0.20f else 0.16f,
            contentAlpha = 1f
        )

        else -> HomeGlassPillStyle(
            containerAlpha = if (emphasized) 0.64f else 0.58f,
            borderAlpha = 0.14f,
            highlightAlpha = if (emphasized) 0.12f else 0.08f,
            contentAlpha = 0.98f
        )
    }
}

internal fun resolveHomeRefreshTipAppearance(
    liquidGlassEnabled: Boolean,
    blurEnabled: Boolean
): HomeRefreshTipAppearance {
    return if (!liquidGlassEnabled && !blurEnabled) {
        HomeRefreshTipAppearance(
            surfaceStyle = HomeRefreshTipSurfaceStyle.PLAIN,
            borderWidthDp = 0f,
            tonalElevationDp = 1f,
            shadowElevationDp = 1f
        )
    } else {
        HomeRefreshTipAppearance(
            surfaceStyle = HomeRefreshTipSurfaceStyle.GLASS,
            borderWidthDp = 0.8f,
            tonalElevationDp = 0f,
            shadowElevationDp = 0f
        )
    }
}

internal fun resolveHomeWallpaperBackdropAppearance(
    hasWallpaper: Boolean,
    effectMode: HomeWallpaperEffectMode = HomeWallpaperEffectMode.SOFT_BLUR,
    isDarkTheme: Boolean,
    isDataSaverActive: Boolean
): HomeWallpaperBackdropAppearance {
    if (!hasWallpaper || effectMode == HomeWallpaperEffectMode.OFF) {
        return HomeWallpaperBackdropAppearance(
            visible = false,
            baseBackgroundAlpha = 1f,
            detailAlpha = 0f,
            scrimAlpha = 0f,
            bottomScrimAlpha = 0f,
            blurRadiusDp = 0f
        )
    }

    return when {
        effectMode == HomeWallpaperEffectMode.ORIGINAL -> HomeWallpaperBackdropAppearance(
            visible = true,
            baseBackgroundAlpha = if (isDarkTheme) 0.24f else 0.14f,
            detailAlpha = 0f,
            scrimAlpha = if (isDarkTheme) 0.16f else 0.04f,
            bottomScrimAlpha = if (isDarkTheme) 0.22f else 0.12f,
            blurRadiusDp = 0f
        )

        effectMode == HomeWallpaperEffectMode.STRONG_BLUR -> HomeWallpaperBackdropAppearance(
            visible = true,
            baseBackgroundAlpha = if (isDarkTheme) 0.50f else 0.34f,
            detailAlpha = 0.05f,
            scrimAlpha = if (isDarkTheme) 0.28f else 0.12f,
            bottomScrimAlpha = if (isDarkTheme) 0.40f else 0.26f,
            blurRadiusDp = 60f
        )

        isDataSaverActive -> HomeWallpaperBackdropAppearance(
            visible = true,
            baseBackgroundAlpha = if (isDarkTheme) 0.48f else 0.34f,
            detailAlpha = 0.16f,
            scrimAlpha = if (isDarkTheme) 0.20f else 0.08f,
            bottomScrimAlpha = if (isDarkTheme) 0.28f else 0.18f,
            blurRadiusDp = 18f
        )

        isDarkTheme -> HomeWallpaperBackdropAppearance(
            visible = true,
            baseBackgroundAlpha = 0.34f,
            detailAlpha = 0.24f,
            scrimAlpha = 0.24f,
            bottomScrimAlpha = 0.34f,
            blurRadiusDp = 24f
        )

        else -> HomeWallpaperBackdropAppearance(
            visible = true,
            baseBackgroundAlpha = 0.22f,
            detailAlpha = 0.32f,
            scrimAlpha = 0.06f,
            bottomScrimAlpha = 0.18f,
            blurRadiusDp = 22f
        )
    }
}

internal fun resolveHomeWallpaperUri(
    homeWallpaperUri: String?,
    splashWallpaperUri: String?
): String {
    val dedicatedHomeUri = homeWallpaperUri?.trim().orEmpty()
    if (dedicatedHomeUri.isNotEmpty()) return dedicatedHomeUri
    return splashWallpaperUri?.trim().orEmpty()
}

internal fun resolveHomeCardInfoSurfaceAppearance(
    wallpaperTintEnabled: Boolean,
    wallpaperEffectMode: HomeWallpaperEffectMode = HomeWallpaperEffectMode.SOFT_BLUR,
    isDarkTheme: Boolean,
    isDataSaverActive: Boolean
): HomeCardInfoSurfaceAppearance {
    if (!wallpaperTintEnabled || wallpaperEffectMode == HomeWallpaperEffectMode.OFF) {
        return HomeCardInfoSurfaceAppearance(
            useTintedSurface = false,
            containerAlpha = 1f,
            borderAlpha = 0f,
            highlightAlpha = 0f
        )
    }

    return HomeCardInfoSurfaceAppearance(
        useTintedSurface = true,
        containerAlpha = when {
            wallpaperEffectMode == HomeWallpaperEffectMode.ORIGINAL && isDarkTheme -> 0.26f
            wallpaperEffectMode == HomeWallpaperEffectMode.ORIGINAL -> 0.12f
            wallpaperEffectMode == HomeWallpaperEffectMode.STRONG_BLUR && isDarkTheme -> 0.50f
            wallpaperEffectMode == HomeWallpaperEffectMode.STRONG_BLUR -> 0.32f
            isDataSaverActive -> if (isDarkTheme) 0.56f else 0.36f
            isDarkTheme -> 0.36f
            else -> 0.16f
        },
        borderAlpha = when {
            wallpaperEffectMode == HomeWallpaperEffectMode.ORIGINAL && isDarkTheme -> 0.18f
            wallpaperEffectMode == HomeWallpaperEffectMode.ORIGINAL -> 0.22f
            isDarkTheme -> 0.12f
            else -> 0.14f
        },
        highlightAlpha = if (isDarkTheme) 0.04f else 0.06f
    )
}

internal fun resolveHomeGlassCoverPillBaseColor(): Color {
    // Cover badges sit directly on top of unpredictable thumbnails, so keep the
    // glass tint dark to preserve white text contrast in history/favorites/etc.
    return Color.Black
}

@Composable
internal fun rememberHomeGlassChromeColors(
    glassEnabled: Boolean,
    blurEnabled: Boolean,
    baseColor: Color = AppSurfaceTokens.cardContainer()
): HomeGlassResolvedColors {
    val style = remember(glassEnabled, blurEnabled) {
        resolveHomeGlassChromeStyle(
            glassEnabled = glassEnabled,
            blurEnabled = blurEnabled
        )
    }
    return remember(style, baseColor) {
        HomeGlassResolvedColors(
            containerColor = baseColor.copy(alpha = style.containerAlpha),
            borderColor = Color.White.copy(alpha = style.borderAlpha),
            highlightColor = Color.White.copy(alpha = style.highlightAlpha)
        )
    }
}

@Composable
internal fun rememberHomeGlassPillColors(
    glassEnabled: Boolean,
    blurEnabled: Boolean,
    emphasized: Boolean,
    baseColor: Color
): HomeGlassResolvedColors {
    val style = remember(glassEnabled, blurEnabled, emphasized) {
        resolveHomeGlassPillStyle(
            glassEnabled = glassEnabled,
            blurEnabled = blurEnabled,
            emphasized = emphasized
        )
    }
    return remember(style, baseColor) {
        HomeGlassResolvedColors(
            containerColor = baseColor.copy(alpha = style.containerAlpha),
            borderColor = Color.White.copy(alpha = style.borderAlpha),
            highlightColor = Color.White.copy(alpha = style.highlightAlpha)
        )
    }
}
