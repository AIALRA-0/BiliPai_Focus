package com.android.purebilibili.feature.message

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.home.components.cards.VideoCardAmbientDrawSpec
import com.android.purebilibili.feature.home.components.cards.resolveVideoCardAmbientDrawSpec
import com.android.purebilibili.feature.home.components.cards.WallpaperPalette

private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.84f
private val MESSAGE_BUBBLE_MAX_WIDTH = 420.dp

internal fun resolveMessageBubbleMaxWidth(availableWidth: Dp): Dp {
    if (availableWidth <= 0.dp) return 0.dp
    return (availableWidth * MESSAGE_BUBBLE_WIDTH_FRACTION)
        .coerceAtMost(MESSAGE_BUBBLE_MAX_WIDTH)
}

internal fun resolveMessageGlassYFraction(
    positionY: Float,
    screenHeightPx: Float,
): Float {
    if (!positionY.isFinite() || !screenHeightPx.isFinite() || screenHeightPx <= 0f) {
        return 0.5f
    }
    return (positionY / screenHeightPx).coerceIn(0f, 1f)
}

internal fun resolveMessageBubbleFallbackContainerColor(
    isOwnMessage: Boolean,
    primary: Color,
    surfaceVariant: Color,
): Color = if (isOwnMessage) primary else surfaceVariant

internal fun resolveMessageBubbleFallbackContentColor(
    isOwnMessage: Boolean,
    onPrimary: Color,
    onSurfaceVariant: Color,
): Color = if (isOwnMessage) onPrimary else onSurfaceVariant

internal fun resolveMessageGlassDrawSpec(
    wallpaperPalette: WallpaperPalette?,
    yFraction: Float,
    isDarkTheme: Boolean,
    defaultContainerColor: Color,
    defaultBorderColor: Color,
    wallpaperVisible: Boolean,
    dynamicTintEnabled: Boolean,
): VideoCardAmbientDrawSpec = resolveVideoCardAmbientDrawSpec(
    wallpaperPalette = wallpaperPalette,
    yFraction = yFraction,
    coverTint = null,
    wallpaperTintEnabled = wallpaperVisible,
    isDarkTheme = isDarkTheme,
    defaultContainerColor = defaultContainerColor,
    defaultBorderColor = defaultBorderColor,
    frostedGlassEnabled = dynamicTintEnabled,
)
