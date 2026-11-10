package com.android.purebilibili.feature.video.ui.overlay.glass

import com.android.purebilibili.feature.video.ui.overlay.*

data class PortraitTopBarLayoutPolicy(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val leftSectionSpacingDp: Int,
    val rightSectionSpacingDp: Int,
    val buttonSizeDp: Int,
    val iconSizeDp: Int,
    val chipFontSp: Int,
    val chipHorizontalPaddingDp: Int,
    val chipVerticalPaddingDp: Int,
    val onlineCountFontSp: Int
)

/**
 * 竖屏内联顶栏是否加状态栏 padding。
 * 状态栏可见时必须避让；「播放页隐藏状态栏」或全屏藏栏后为 false。
 */
fun shouldApplyStatusBarPaddingToPortraitTopBar(
    statusBarVisible: Boolean,
): Boolean = statusBarVisible

fun resolvePortraitTopBarLayoutPolicy(
    widthDp: Int,
    compact: Boolean = false
): PortraitTopBarLayoutPolicy {
    if (widthDp >= 1400) {
        return PortraitTopBarLayoutPolicy(
            horizontalPaddingDp = 24,
            verticalPaddingDp = if (compact) 8 else 12,
            leftSectionSpacingDp = if (compact) 7 else 14,
            rightSectionSpacingDp = if (compact) 5 else 10,
            buttonSizeDp = 52,
            iconSizeDp = 28,
            chipFontSp = 15,
            chipHorizontalPaddingDp = 14,
            chipVerticalPaddingDp = 8,
            onlineCountFontSp = 14
        )
    }

    if (widthDp >= 840) {
        return PortraitTopBarLayoutPolicy(
            horizontalPaddingDp = 16,
            verticalPaddingDp = if (compact) 6 else 10,
            leftSectionSpacingDp = if (compact) 5 else 10,
            rightSectionSpacingDp = if (compact) 4 else 8,
            buttonSizeDp = 48,
            iconSizeDp = 24,
            chipFontSp = 13,
            chipHorizontalPaddingDp = 10,
            chipVerticalPaddingDp = 6,
            onlineCountFontSp = 12
        )
    }

    if (widthDp >= 600) {
        return PortraitTopBarLayoutPolicy(
            horizontalPaddingDp = 12,
            verticalPaddingDp = if (compact) 5 else 9,
            leftSectionSpacingDp = if (compact) 4 else 9,
            rightSectionSpacingDp = if (compact) 3 else 6,
            buttonSizeDp = 44,
            iconSizeDp = 22,
            chipFontSp = 12,
            chipHorizontalPaddingDp = 9,
            chipVerticalPaddingDp = 5,
            onlineCountFontSp = 11
        )
    }

    return PortraitTopBarLayoutPolicy(
        horizontalPaddingDp = 8,
        verticalPaddingDp = if (compact) 4 else 8,
        leftSectionSpacingDp = if (compact) 3 else 8,
        rightSectionSpacingDp = if (compact) 2 else 4,
        // Match the inline bottom controls so circular buttons and the action dock
        // retain enough height on phone video and bangumi detail pages.
        buttonSizeDp = 40,
        iconSizeDp = 22,
        chipFontSp = 11,
        chipHorizontalPaddingDp = 8,
        chipVerticalPaddingDp = 4,
        onlineCountFontSp = 11
    )
}
