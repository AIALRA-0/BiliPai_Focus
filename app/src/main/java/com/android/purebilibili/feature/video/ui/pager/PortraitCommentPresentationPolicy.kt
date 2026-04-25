package com.android.purebilibili.feature.video.ui.pager

internal fun shouldUseEmbeddedVideoSubReplyPresentation(): Boolean = true

private const val FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION = 1f

internal fun shouldShowDetachedVideoSubReplySheet(
    useEmbeddedPresentation: Boolean
): Boolean = !useEmbeddedPresentation

internal fun shouldOpenPortraitCommentReplyComposer(): Boolean = true

internal fun shouldOpenPortraitCommentThreadDetail(
    useEmbeddedPresentation: Boolean
): Boolean = true

internal fun resolvePortraitCommentHostMainSheetVisible(
    commentSheetVisible: Boolean,
    subReplyVisible: Boolean
): Boolean = commentSheetVisible || subReplyVisible

private const val PORTRAIT_COMMENT_SHEET_PLAYER_SCALE = 0.58f

internal fun resolvePortraitCommentExpandedPlayerScale(
    commentSheetVisible: Boolean
): Float {
    return resolvePortraitCommentExpandedPlayerScale(
        commentVisibilityProgress = if (commentSheetVisible) 1f else 0f
    )
}

internal fun resolvePortraitCommentExpandedPlayerScale(
    commentVisibilityProgress: Float
): Float {
    val clampedProgress = commentVisibilityProgress.coerceIn(0f, 1f)
    return 1f - ((1f - PORTRAIT_COMMENT_SHEET_PLAYER_SCALE) * clampedProgress)
}

internal fun resolvePortraitCommentVisibilityProgress(
    sheetOffsetPx: Float,
    sheetHeightPx: Float
): Float {
    if (sheetHeightPx <= 0f) return 1f
    return (1f - (sheetOffsetPx.coerceAtLeast(0f) / sheetHeightPx)).coerceIn(0f, 1f)
}

internal fun shouldDismissPortraitCommentSheetByDrag(
    sheetOffsetPx: Float,
    sheetHeightPx: Float,
    dismissThresholdFraction: Float = 0.22f
): Boolean {
    if (sheetHeightPx <= 0f) return false
    return sheetOffsetPx >= sheetHeightPx * dismissThresholdFraction.coerceAtLeast(0f)
}

internal fun resolveVideoSubReplySheetMaxHeightFraction(
    screenHeightPx: Int = 0,
    topReservedPx: Int = 0
): Float {
    if (screenHeightPx <= 0) return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION

    val reservedTopPx = topReservedPx.coerceAtLeast(0)
    if (reservedTopPx == 0) return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION

    val availableHeightPx = (screenHeightPx - reservedTopPx).coerceAtLeast(0)
    if (availableHeightPx == 0) return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION

    return (availableHeightPx.toFloat() / screenHeightPx.toFloat())
        .coerceIn(0f, FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION)
}

internal fun resolveVideoSubReplySheetScrimAlpha(): Float = 0f
