package com.android.purebilibili.feature.home.components.cards

internal data class VideoCardDurationBadgeVisualStyle(
    val backgroundAlpha: Float,
    val compactMinWidthDp: Float,
    val extendedMinWidthDp: Float
)

internal fun resolveVideoCardDurationBadgeVisualStyle(): VideoCardDurationBadgeVisualStyle {
    return VideoCardDurationBadgeVisualStyle(
        backgroundAlpha = 0.54f,
        compactMinWidthDp = 40f,
        extendedMinWidthDp = 52f
    )
}

internal fun resolveVideoCardDurationBadgeMinWidthDp(
    durationText: String,
    style: VideoCardDurationBadgeVisualStyle = resolveVideoCardDurationBadgeVisualStyle()
): Float {
    return if (durationText.length >= 7) {
        style.extendedMinWidthDp
    } else {
        style.compactMinWidthDp
    }
}
