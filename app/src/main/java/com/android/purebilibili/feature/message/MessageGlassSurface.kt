package com.android.purebilibili.feature.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.feature.home.components.cards.LocalHomeCardDynamicTintEnabled
import com.android.purebilibili.feature.home.components.cards.LocalWallpaperPalette
import com.android.purebilibili.feature.home.components.cards.VideoCardAdaptiveContentColors
import com.android.purebilibili.feature.home.components.cards.resolveVideoCardAdaptiveContentColors

@Composable
internal fun rememberMessageGlassContentColors(
    defaultOnSurface: Color,
    defaultOnSurfaceVariant: Color,
): VideoCardAdaptiveContentColors {
    val wallpaperPalette = LocalWallpaperPalette.current
    val dynamicTintEnabled = LocalHomeCardDynamicTintEnabled.current
    val wallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    val isDarkTheme = AppSurfaceTokens.chromeBackground().luminance() < 0.5f
    return remember(
        wallpaperPalette,
        dynamicTintEnabled,
        wallpaperVisible,
        isDarkTheme,
        defaultOnSurface,
        defaultOnSurfaceVariant,
    ) {
        resolveVideoCardAdaptiveContentColors(
            wallpaperPalette = wallpaperPalette,
            coverTint = null,
            wallpaperTintEnabled = wallpaperVisible,
            isDarkTheme = isDarkTheme,
            defaultOnSurface = defaultOnSurface,
            defaultOnSurfaceVariant = defaultOnSurfaceVariant,
            homeCardDynamicTintEnabled = dynamicTintEnabled,
        )
    }
}

@Composable
internal fun Modifier.messageGlassContainer(
    defaultContainerColor: Color,
    shape: Shape,
    drawGlassBorder: Boolean = true,
): Modifier {
    val wallpaperPalette = LocalWallpaperPalette.current
    val dynamicTintEnabled = LocalHomeCardDynamicTintEnabled.current
    val wallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    val isDarkTheme = AppSurfaceTokens.chromeBackground().luminance() < 0.5f
    if (!dynamicTintEnabled || !wallpaperVisible) {
        return background(defaultContainerColor)
    }
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val yFraction = remember { mutableFloatStateOf(0.5f) }
    val glassEdge = if (isDarkTheme) {
        Color.White.copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.40f)
    }
    val defaultBorder = glassEdge
    return onGloballyPositioned { coordinates ->
        val next = resolveMessageGlassYFraction(
            positionY = coordinates.positionInRoot().y,
            screenHeightPx = screenHeightPx,
        )
        if (yFraction.floatValue != next) {
            yFraction.floatValue = next
        }
    }
        .drawBehind {
            val spec = resolveMessageGlassDrawSpec(
                wallpaperPalette = wallpaperPalette,
                yFraction = yFraction.floatValue,
                isDarkTheme = isDarkTheme,
                defaultContainerColor = defaultContainerColor,
                defaultBorderColor = defaultBorder,
                wallpaperVisible = wallpaperVisible,
                dynamicTintEnabled = true,
            )
            drawRect(spec.containerColor)
        }
        .then(
            if (drawGlassBorder) {
                Modifier.border(width = 0.5.dp, color = glassEdge, shape = shape)
            } else {
                Modifier
            }
        )
}
