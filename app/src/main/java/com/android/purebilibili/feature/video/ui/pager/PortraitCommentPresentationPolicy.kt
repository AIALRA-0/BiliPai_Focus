package com.android.purebilibili.feature.video.ui.pager

internal fun shouldUseEmbeddedVideoSubReplyPresentation(): Boolean = true

private const val FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION = 1f
private const val MAIN_COMMENT_SHEET_HEIGHT_FRACTION = 0.60f
private const val PORTRAIT_COMMENT_EXPANDED_PLAYER_SCALE = 0.58f

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

internal data class PortraitCommentPlayerTransform(
    val progress: Float,
    val scale: Float,
    val translationYPx: Float,
    val visibleHeightFraction: Float,
    val overlayAlpha: Float,
    val playerGesturesEnabled: Boolean
)

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
    return resolvePortraitCommentPlayerTransform(
        commentVisibilityProgress = commentVisibilityProgress
    ).scale
}

internal fun resolvePortraitCommentPlayerTransform(
    commentVisibilityProgress: Float,
    containerHeightPx: Int = 1,
    containerWidthPx: Int = 0,
    currentVideoAspect: Float = 0f,
    viewportVerticalOffsetPx: Float = 0f,
    fillContainer: Boolean = false,
    commentSheetHeightFraction: Float = MAIN_COMMENT_SHEET_HEIGHT_FRACTION
): PortraitCommentPlayerTransform {
    val progress = if (containerHeightPx > 0) {
        commentVisibilityProgress.coerceIn(0f, 1f)
    } else {
        0f
    }
    val sheetFraction = commentSheetHeightFraction.coerceIn(0f, 1f)
    val visibleHeightFraction = (1f - sheetFraction * progress).coerceIn(0f, 1f)
    val scale = 1f - ((1f - PORTRAIT_COMMENT_EXPANDED_PLAYER_SCALE) * progress)
    val translationYPx = if (containerHeightPx > 0) {
        val visibleBottomPx = visibleHeightFraction * containerHeightPx.toFloat()
        val videoBottomPx = resolvePortraitCommentVideoBottomBeforeTransformPx(
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            currentVideoAspect = currentVideoAspect,
            viewportVerticalOffsetPx = viewportVerticalOffsetPx,
            fillContainer = fillContainer
        )
        val targetTranslationPx = visibleBottomPx - videoBottomPx * scale
        val collapsedTranslationPx = containerHeightPx.toFloat() - videoBottomPx
        targetTranslationPx - collapsedTranslationPx * (1f - progress)
    } else {
        0f
    }

    return PortraitCommentPlayerTransform(
        progress = progress,
        scale = scale,
        translationYPx = translationYPx,
        visibleHeightFraction = visibleHeightFraction,
        overlayAlpha = (1f - progress).coerceIn(0f, 1f),
        playerGesturesEnabled = progress <= 0.001f
    )
}

private fun resolvePortraitCommentVideoBottomBeforeTransformPx(
    containerWidthPx: Int,
    containerHeightPx: Int,
    currentVideoAspect: Float,
    viewportVerticalOffsetPx: Float,
    fillContainer: Boolean
): Float {
    if (containerWidthPx <= 0 || containerHeightPx <= 0 || currentVideoAspect <= 0f) {
        return containerHeightPx.toFloat()
    }
    // 评论展开对齐的是用户看到的视频底边；横屏视频只占整层中部，不能按整层底边计算。
    val viewportSize = resolvePortraitVideoViewportSize(
        containerWidth = containerWidthPx,
        containerHeight = containerHeightPx,
        currentVideoAspect = currentVideoAspect,
        fillContainer = fillContainer
    )
    return containerHeightPx / 2f + viewportVerticalOffsetPx + viewportSize.height / 2f
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
