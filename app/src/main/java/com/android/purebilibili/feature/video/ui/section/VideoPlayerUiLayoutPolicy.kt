package com.android.purebilibili.feature.video.ui.section

data class VideoPlayerUiLayoutPolicy(
    val gestureOverlaySizeDp: Int,
    val gestureIconSizeDp: Int,
    val seekFeedbackSizeDp: Int,
    val gestureBoundaryPaddingDp: Int,
    val restoreButtonBottomOffsetDp: Int,
    val restoreButtonHorizontalPaddingDp: Int,
    val restoreButtonVerticalPaddingDp: Int,
    val restoreButtonIconSizeDp: Int,
    val longPressBadgeHorizontalPaddingDp: Int,
    val longPressBadgeVerticalPaddingDp: Int,
    val compactWindowHeight: Boolean = false,
    val seekCancelHintOffsetDp: Int = 96,
)

fun resolveVideoPlayerUiLayoutPolicy(
    widthDp: Int,
    windowHeightDp: Int = Int.MAX_VALUE,
): VideoPlayerUiLayoutPolicy {
    val widthPolicy = when {
        widthDp < 240 -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 96,
            gestureIconSizeDp = 36,
            seekFeedbackSizeDp = 84,
            gestureBoundaryPaddingDp = 16,
            restoreButtonBottomOffsetDp = 72,
            restoreButtonHorizontalPaddingDp = 12,
            restoreButtonVerticalPaddingDp = 6,
            restoreButtonIconSizeDp = 14,
            longPressBadgeHorizontalPaddingDp = 16,
            longPressBadgeVerticalPaddingDp = 10
        )
        widthDp >= 1600 -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 140,
            gestureIconSizeDp = 56,
            seekFeedbackSizeDp = 112,
            gestureBoundaryPaddingDp = 28,
            restoreButtonBottomOffsetDp = 116,
            restoreButtonHorizontalPaddingDp = 20,
            restoreButtonVerticalPaddingDp = 10,
            restoreButtonIconSizeDp = 18,
            longPressBadgeHorizontalPaddingDp = 24,
            longPressBadgeVerticalPaddingDp = 14
        )
        widthDp >= 840 -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 132,
            gestureIconSizeDp = 52,
            seekFeedbackSizeDp = 108,
            gestureBoundaryPaddingDp = 26,
            restoreButtonBottomOffsetDp = 112,
            restoreButtonHorizontalPaddingDp = 18,
            restoreButtonVerticalPaddingDp = 9,
            restoreButtonIconSizeDp = 17,
            longPressBadgeHorizontalPaddingDp = 22,
            longPressBadgeVerticalPaddingDp = 13
        )
        else -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 120,
            gestureIconSizeDp = 48,
            seekFeedbackSizeDp = 100,
            gestureBoundaryPaddingDp = 24,
            restoreButtonBottomOffsetDp = 100,
            restoreButtonHorizontalPaddingDp = 16,
            restoreButtonVerticalPaddingDp = 8,
            restoreButtonIconSizeDp = 16,
            longPressBadgeHorizontalPaddingDp = 20,
            longPressBadgeVerticalPaddingDp = 12
        )
    }
    if (windowHeightDp <= 0 || windowHeightDp >= 480) return widthPolicy
    val veryShort = windowHeightDp < 320
    // 矮窗口只收紧反馈与留白，不缩小原生按钮触摸目标；所有材质共用该几何策略。
    return widthPolicy.copy(
        compactWindowHeight = true,
        gestureOverlaySizeDp = minOf(widthPolicy.gestureOverlaySizeDp, if (veryShort) 80 else 96),
        gestureIconSizeDp = minOf(widthPolicy.gestureIconSizeDp, 36),
        seekFeedbackSizeDp = minOf(widthPolicy.seekFeedbackSizeDp, if (veryShort) 64 else 84),
        gestureBoundaryPaddingDp = minOf(widthPolicy.gestureBoundaryPaddingDp, if (veryShort) 12 else 16),
        restoreButtonBottomOffsetDp = minOf(widthPolicy.restoreButtonBottomOffsetDp, 72),
        seekCancelHintOffsetDp = if (veryShort) 32 else 48,
    )
}
