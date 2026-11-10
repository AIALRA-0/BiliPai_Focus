package com.android.purebilibili.feature.video.ui.overlay

data class VideoPlayerOverlayVisualPolicy(
    val topScrimHeightDp: Int,
    val bottomScrimHeightDp: Int,
    val lockButtonEndPaddingDp: Int,
    val lockButtonSizeDp: Int,
    val lockButtonCornerRadiusDp: Int,
    val lockIconSizeDp: Int,
    val statsTopPaddingDp: Int,
    val statsEndPaddingDp: Int,
    val statsHorizontalPaddingDp: Int,
    val statsVerticalPaddingDp: Int,
    val statsCornerRadiusDp: Int,
    val statsFontSp: Int,
    val qualitySwitchCornerRadiusDp: Int,
    val qualitySwitchOuterPaddingDp: Int,
    val qualitySwitchContentHorizontalPaddingDp: Int,
    val qualitySwitchContentVerticalPaddingDp: Int,
    val qualitySwitchContentSpacingDp: Int,
    val qualitySwitchMessageFontSp: Int,
    val interactionIconSizeDp: Int,
    val interactionLabelFontSp: Int,
    val interactionLabelTopSpacingDp: Int,
    val tripleActionSpacingDp: Int,
    val tripleRingExtraSizeDp: Int
)

fun resolveVideoPlayerOverlayVisualPolicy(
    widthDp: Int,
    compact: Boolean = false
): VideoPlayerOverlayVisualPolicy {
    if (widthDp >= 1600) {
        return VideoPlayerOverlayVisualPolicy(
            topScrimHeightDp = if (compact) 150 else 200,
            bottomScrimHeightDp = if (compact) 210 else 280,
            lockButtonEndPaddingDp = 40,
            lockButtonSizeDp = 64,
            lockButtonCornerRadiusDp = 16,
            lockIconSizeDp = 30,
            statsTopPaddingDp = 104,
            statsEndPaddingDp = 36,
            statsHorizontalPaddingDp = 12,
            statsVerticalPaddingDp = 6,
            statsCornerRadiusDp = 6,
            statsFontSp = 14,
            qualitySwitchCornerRadiusDp = 16,
            qualitySwitchOuterPaddingDp = 32,
            qualitySwitchContentHorizontalPaddingDp = 32,
            qualitySwitchContentVerticalPaddingDp = 20,
            qualitySwitchContentSpacingDp = 16,
            qualitySwitchMessageFontSp = 16,
            interactionIconSizeDp = 28,
            interactionLabelFontSp = 12,
            interactionLabelTopSpacingDp = 6,
            tripleActionSpacingDp = 20,
            tripleRingExtraSizeDp = 16
        )
    }

    if (widthDp >= 840) {
        return VideoPlayerOverlayVisualPolicy(
            topScrimHeightDp = if (compact) 126 else 168,
            bottomScrimHeightDp = if (compact) 180 else 240,
            lockButtonEndPaddingDp = 30,
            lockButtonSizeDp = 56,
            lockButtonCornerRadiusDp = 14,
            lockIconSizeDp = 26,
            statsTopPaddingDp = 92,
            statsEndPaddingDp = 28,
            statsHorizontalPaddingDp = 10,
            statsVerticalPaddingDp = 5,
            statsCornerRadiusDp = 5,
            statsFontSp = 13,
            qualitySwitchCornerRadiusDp = 14,
            qualitySwitchOuterPaddingDp = 28,
            qualitySwitchContentHorizontalPaddingDp = 28,
            qualitySwitchContentVerticalPaddingDp = 18,
            qualitySwitchContentSpacingDp = 14,
            qualitySwitchMessageFontSp = 15,
            interactionIconSizeDp = 26,
            interactionLabelFontSp = 11,
            interactionLabelTopSpacingDp = 5,
            tripleActionSpacingDp = 18,
            tripleRingExtraSizeDp = 14
        )
    }

    if (widthDp >= 600) {
        return VideoPlayerOverlayVisualPolicy(
            topScrimHeightDp = if (compact) 114 else 152,
            bottomScrimHeightDp = if (compact) 165 else 220,
            lockButtonEndPaddingDp = 26,
            lockButtonSizeDp = 52,
            lockButtonCornerRadiusDp = 13,
            lockIconSizeDp = 25,
            statsTopPaddingDp = 86,
            statsEndPaddingDp = 26,
            statsHorizontalPaddingDp = 9,
            statsVerticalPaddingDp = 4,
            statsCornerRadiusDp = 4,
            statsFontSp = 12,
            qualitySwitchCornerRadiusDp = 13,
            qualitySwitchOuterPaddingDp = 26,
            qualitySwitchContentHorizontalPaddingDp = 26,
            qualitySwitchContentVerticalPaddingDp = 17,
            qualitySwitchContentSpacingDp = 13,
            qualitySwitchMessageFontSp = 14,
            interactionIconSizeDp = 25,
            interactionLabelFontSp = 10,
            interactionLabelTopSpacingDp = 4,
            tripleActionSpacingDp = 17,
            tripleRingExtraSizeDp = 13
        )
    }

    return VideoPlayerOverlayVisualPolicy(
        topScrimHeightDp = if (compact) 105 else 140,
        bottomScrimHeightDp = if (compact) 150 else 200,
        lockButtonEndPaddingDp = 24,
        lockButtonSizeDp = 48,
        lockButtonCornerRadiusDp = 12,
        lockIconSizeDp = 24,
        statsTopPaddingDp = 80,
        statsEndPaddingDp = 24,
        statsHorizontalPaddingDp = 8,
        statsVerticalPaddingDp = 4,
        statsCornerRadiusDp = 4,
        statsFontSp = 12,
        qualitySwitchCornerRadiusDp = 12,
        qualitySwitchOuterPaddingDp = 24,
        qualitySwitchContentHorizontalPaddingDp = 24,
        qualitySwitchContentVerticalPaddingDp = 16,
        qualitySwitchContentSpacingDp = 12,
        qualitySwitchMessageFontSp = 14,
        interactionIconSizeDp = 24,
        interactionLabelFontSp = 10,
        interactionLabelTopSpacingDp = 4,
        tripleActionSpacingDp = 16,
        tripleRingExtraSizeDp = 12
    )
}
