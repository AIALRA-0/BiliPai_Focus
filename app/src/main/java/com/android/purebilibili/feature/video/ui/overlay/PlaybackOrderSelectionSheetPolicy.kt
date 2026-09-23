package com.android.purebilibili.feature.video.ui.overlay

/**
 * 播放顺序弹窗布局：横屏/全屏避免 ModalBottomSheet 强制 Expanded 占大半屏，
 * 并让面板底色延伸到手势条后方（沉浸）。
 */
internal enum class PlaybackOrderSheetPresentation {
    BOTTOM_SHEET,
    COMPACT_OVERLAY,
}

internal data class PlaybackOrderSheetLayoutSpec(
    val presentation: PlaybackOrderSheetPresentation,
    val maxWidthDp: Int,
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val rowVerticalPaddingDp: Int,
    val titleVerticalPaddingDp: Int,
    val outerBottomPaddingDp: Int,
    val extendUnderNavigationBars: Boolean,
)

internal fun resolvePlaybackOrderSheetLayoutSpec(
    isLandscape: Boolean,
    isFullscreen: Boolean,
): PlaybackOrderSheetLayoutSpec {
    val compact = isLandscape || isFullscreen
    return if (compact) {
        PlaybackOrderSheetLayoutSpec(
            presentation = PlaybackOrderSheetPresentation.COMPACT_OVERLAY,
            maxWidthDp = 360,
            horizontalPaddingDp = 16,
            verticalPaddingDp = 8,
            rowVerticalPaddingDp = 10,
            titleVerticalPaddingDp = 4,
            outerBottomPaddingDp = 0,
            extendUnderNavigationBars = true,
        )
    } else {
        PlaybackOrderSheetLayoutSpec(
            presentation = PlaybackOrderSheetPresentation.BOTTOM_SHEET,
            maxWidthDp = Int.MAX_VALUE,
            horizontalPaddingDp = 20,
            verticalPaddingDp = 12,
            rowVerticalPaddingDp = 14,
            titleVerticalPaddingDp = 8,
            outerBottomPaddingDp = 0,
            extendUnderNavigationBars = true,
        )
    }
}
