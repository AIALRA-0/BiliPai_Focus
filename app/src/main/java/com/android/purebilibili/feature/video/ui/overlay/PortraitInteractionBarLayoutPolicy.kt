package com.android.purebilibili.feature.video.ui.overlay

data class PortraitInteractionBarLayoutPolicy(
    val endPaddingDp: Int,
    val bottomPaddingDp: Int,
    val itemSpacingDp: Int,
    val iconSizeDp: Int,
    val iconBackingSizeDp: Int,
    val iconBackingInnerPaddingDp: Int,
    val iconBackingAlpha: Float,
    val labelTopSpacingDp: Int,
    val labelFontSp: Int
)

fun resolvePortraitInteractionBarLayoutPolicy(
    widthDp: Int
): PortraitInteractionBarLayoutPolicy {
    if (widthDp >= 1600) {
        return PortraitInteractionBarLayoutPolicy(
            endPaddingDp = 18,
            bottomPaddingDp = 220,
            itemSpacingDp = 28,
            iconSizeDp = 40,
            iconBackingSizeDp = 52,
            iconBackingInnerPaddingDp = 6,
            iconBackingAlpha = 0.16f,
            labelTopSpacingDp = 4,
            labelFontSp = 15
        )
    }

    if (widthDp >= 840) {
        return PortraitInteractionBarLayoutPolicy(
            endPaddingDp = 12,
            bottomPaddingDp = 196,
            itemSpacingDp = 24,
            iconSizeDp = 35,
            iconBackingSizeDp = 46,
            iconBackingInnerPaddingDp = 5,
            iconBackingAlpha = 0.15f,
            labelTopSpacingDp = 3,
            labelFontSp = 13
        )
    }

    if (widthDp >= 600) {
        return PortraitInteractionBarLayoutPolicy(
            endPaddingDp = 10,
            bottomPaddingDp = 188,
            itemSpacingDp = 22,
            iconSizeDp = 32,
            iconBackingSizeDp = 42,
            iconBackingInnerPaddingDp = 5,
            iconBackingAlpha = 0.14f,
            labelTopSpacingDp = 2,
            labelFontSp = 12
        )
    }

    return PortraitInteractionBarLayoutPolicy(
        endPaddingDp = 8,
        bottomPaddingDp = 180,
        itemSpacingDp = 20,
        iconSizeDp = 29,
        iconBackingSizeDp = 38,
        iconBackingInnerPaddingDp = 4,
        iconBackingAlpha = 0.14f,
        labelTopSpacingDp = 2,
        labelFontSp = 12
    )
}
