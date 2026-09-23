package com.android.purebilibili.feature.video.ui.overlay

data class PortraitProgressBarLayoutPolicy(
    val horizontalPaddingDp: Int,
    val bottomPaddingDp: Int,
    val touchAreaHeightDp: Int,
    val idleTrackHeightDp: Int,
    val draggingTrackHeightDp: Int,
    val draggingThumbSizeDp: Int,
    val trackCornerRadiusDp: Int,
    val previewBubbleOffsetYDp: Int,
    val compactPortraitPreviewOffsetYDp: Int,
    val compactLandscapePreviewOffsetYDp: Int,
    val bubbleOffsetYDp: Int,
    val bubbleFontSp: Int,
    val bubbleHorizontalPaddingDp: Int,
    val bubbleVerticalPaddingDp: Int,
    val bubbleCornerRadiusDp: Int
)

fun resolvePortraitProgressBarLayoutPolicy(
    widthDp: Int
): PortraitProgressBarLayoutPolicy {
    if (widthDp >= 1600) {
        return PortraitProgressBarLayoutPolicy(
            horizontalPaddingDp = 16,
            bottomPaddingDp = 16,
            touchAreaHeightDp = 64,
            idleTrackHeightDp = 2,
            draggingTrackHeightDp = 5,
            draggingThumbSizeDp = 16,
            trackCornerRadiusDp = 6,
            previewBubbleOffsetYDp = -108,
            compactPortraitPreviewOffsetYDp = -304,
            compactLandscapePreviewOffsetYDp = -150,
            bubbleOffsetYDp = -52,
            bubbleFontSp = 22,
            bubbleHorizontalPaddingDp = 16,
            bubbleVerticalPaddingDp = 8,
            bubbleCornerRadiusDp = 10
        )
    }

    if (widthDp >= 840) {
        return PortraitProgressBarLayoutPolicy(
            horizontalPaddingDp = 12,
            bottomPaddingDp = 14,
            touchAreaHeightDp = 56,
            idleTrackHeightDp = 2,
            draggingTrackHeightDp = 5,
            draggingThumbSizeDp = 14,
            trackCornerRadiusDp = 5,
            previewBubbleOffsetYDp = -104,
            compactPortraitPreviewOffsetYDp = -294,
            compactLandscapePreviewOffsetYDp = -144,
            bubbleOffsetYDp = -46,
            bubbleFontSp = 20,
            bubbleHorizontalPaddingDp = 14,
            bubbleVerticalPaddingDp = 7,
            bubbleCornerRadiusDp = 9
        )
    }

    if (widthDp >= 600) {
        return PortraitProgressBarLayoutPolicy(
            horizontalPaddingDp = 10,
            bottomPaddingDp = 13,
            touchAreaHeightDp = 52,
            idleTrackHeightDp = 2,
            draggingTrackHeightDp = 5,
            draggingThumbSizeDp = 13,
            trackCornerRadiusDp = 5,
            previewBubbleOffsetYDp = -100,
            compactPortraitPreviewOffsetYDp = -282,
            compactLandscapePreviewOffsetYDp = -136,
            bubbleOffsetYDp = -43,
            bubbleFontSp = 19,
            bubbleHorizontalPaddingDp = 13,
            bubbleVerticalPaddingDp = 6,
            bubbleCornerRadiusDp = 8
        )
    }

    return PortraitProgressBarLayoutPolicy(
        horizontalPaddingDp = 8,
        bottomPaddingDp = 12,
        touchAreaHeightDp = 48,
        idleTrackHeightDp = 2,
        draggingTrackHeightDp = 5,
        draggingThumbSizeDp = 12,
        trackCornerRadiusDp = 4,
        previewBubbleOffsetYDp = -96,
        compactPortraitPreviewOffsetYDp = -250,
        compactLandscapePreviewOffsetYDp = -124,
        bubbleOffsetYDp = -40,
        bubbleFontSp = 18,
        bubbleHorizontalPaddingDp = 12,
        bubbleVerticalPaddingDp = 6,
        bubbleCornerRadiusDp = 8
    )
}
