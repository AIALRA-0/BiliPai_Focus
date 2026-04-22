package com.android.purebilibili.feature.live

import androidx.compose.ui.graphics.Color

internal data class LivePiliPlusHomeMetrics(
    val safeSpaceDp: Int,
    val cardSpaceDp: Int,
    val cardRadiusDp: Int,
    val coverAspectRatio: Float,
    val followAvatarSizeDp: Int,
    val followItemExtentDp: Int
)

internal data class LivePiliPlusChipColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color
)

internal data class LivePiliPlusChatBubbleTokens(
    val cornerRadiusDp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val fontSizeSp: Int,
    val backgroundAlpha: Float,
    val nameAlpha: Float
)

internal data class LivePiliPlusRoomColorTokens(
    val baseBackgroundColor: Color,
    val backdropImageAlpha: Float,
    val inputContainerAlpha: Float,
    val inputOverlayColor: Color,
    val inputContentColor: Color
)

internal fun resolveLivePiliPlusHomeMetrics(): LivePiliPlusHomeMetrics {
    return LivePiliPlusHomeMetrics(
        safeSpaceDp = 12,
        cardSpaceDp = 8,
        cardRadiusDp = 10,
        coverAspectRatio = 16f / 10f,
        followAvatarSizeDp = 45,
        followItemExtentDp = 70
    )
}

internal fun resolveLivePiliPlusGridColumns(
    widthDp: Int,
    isExpandedScreen: Boolean
): Int {
    if (!isExpandedScreen) return 2
    return (widthDp / 240).coerceIn(2, 5)
}

internal fun resolveLivePiliPlusChipColors(
    selectedContainer: Color,
    selectedContent: Color,
    unselectedContent: Color
): LivePiliPlusChipColors {
    return LivePiliPlusChipColors(
        selectedContainerColor = selectedContainer,
        selectedContentColor = selectedContent,
        unselectedContainerColor = Color.Transparent,
        unselectedContentColor = unselectedContent
    )
}

internal fun resolveLivePiliPlusChatBubbleTokens(
    isOverlay: Boolean,
    isDark: Boolean
): LivePiliPlusChatBubbleTokens {
    val backgroundAlpha = when {
        isOverlay -> 0.56f
        else -> 0.08f
    }
    val nameAlpha = if (isOverlay) 0.90f else 0.60f
    return LivePiliPlusChatBubbleTokens(
        cornerRadiusDp = 14,
        horizontalPaddingDp = 10,
        verticalPaddingDp = 4,
        fontSizeSp = 14,
        backgroundAlpha = backgroundAlpha,
        nameAlpha = nameAlpha
    )
}

internal fun shouldRenderLiveDanmaku(
    text: String,
    emoticonUrl: String?
): Boolean {
    return text.isNotBlank() || !emoticonUrl.isNullOrBlank()
}

internal fun resolveLivePiliPlusRoomColorTokens(
    inputOverlayColor: Color = Color.White,
    inputContentColor: Color = Color(0xFFEEEEEE)
): LivePiliPlusRoomColorTokens {
    return LivePiliPlusRoomColorTokens(
        baseBackgroundColor = Color.Black,
        backdropImageAlpha = 0.60f,
        inputContainerAlpha = 0.10f,
        inputOverlayColor = inputOverlayColor,
        inputContentColor = inputContentColor
    )
}
