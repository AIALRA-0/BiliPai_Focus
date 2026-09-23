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

internal data class PortraitCommentSheetVisibility(
    val commentSheetVisible: Boolean,
    val subReplyVisible: Boolean
)

internal fun resolvePortraitCommentSheetVisibility(
    active: Boolean,
    commentSheetVisible: Boolean,
    subReplyVisible: Boolean
): PortraitCommentSheetVisibility {
    return PortraitCommentSheetVisibility(
        commentSheetVisible = active && commentSheetVisible,
        subReplyVisible = active && subReplyVisible
    )
}

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

/**
 * 半屏展开后播放器目标缩放。
 *
 * - 默认：评论区固定 [PORTRAIT_COMMENT_EXPANDED_PLAYER_SCALE]
 * - [fitToAvailableBand]=true（UP 预览）：按「顶区剩余高度 / 视频视口高度」贴合，
 *   避免半屏过高时仍用 0.58 缩放导致上浮过大、画面比例失调。
 */
internal fun resolvePortraitOverlayExpandedPlayerScale(
    containerHeightPx: Int,
    containerWidthPx: Int,
    currentVideoAspect: Float,
    viewportVerticalOffsetPx: Float,
    fillContainer: Boolean,
    visibleHeightFraction: Float,
    fitToAvailableBand: Boolean,
    fallbackScale: Float = PORTRAIT_COMMENT_EXPANDED_PLAYER_SCALE,
): Float {
    if (!fitToAvailableBand || containerHeightPx <= 0) {
        return fallbackScale.coerceIn(0.32f, 1f)
    }
    val videoHeightPx = resolvePortraitCommentVideoHeightBeforeTransformPx(
        containerWidthPx = containerWidthPx,
        containerHeightPx = containerHeightPx,
        currentVideoAspect = currentVideoAspect,
        fillContainer = fillContainer,
    )
    val availableHeightPx =
        (visibleHeightFraction.coerceIn(0f, 1f) * containerHeightPx.toFloat())
            .coerceAtLeast(1f)
    if (videoHeightPx <= 1f) return fallbackScale.coerceIn(0.32f, 1f)
    // 略留顶边空隙，避免顶到状态栏图标；上限 1，下限防过小。
    val fit = (availableHeightPx * 0.92f) / videoHeightPx
    return fit.coerceIn(0.32f, 1f)
}

internal fun resolvePortraitCommentPlayerTransform(
    commentVisibilityProgress: Float,
    containerHeightPx: Int = 1,
    containerWidthPx: Int = 0,
    currentVideoAspect: Float = 0f,
    viewportVerticalOffsetPx: Float = 0f,
    fillContainer: Boolean = false,
    commentSheetHeightFraction: Float = MAIN_COMMENT_SHEET_HEIGHT_FRACTION,
    /** UP 预览：缩放贴合顶区可用高度，而不是固定 0.58。 */
    fitToAvailableBand: Boolean = false,
): PortraitCommentPlayerTransform {
    val progress = if (containerHeightPx > 0) {
        commentVisibilityProgress.coerceIn(0f, 1f)
    } else {
        0f
    }
    val sheetFraction = commentSheetHeightFraction.coerceIn(0f, 1f)
    val visibleHeightFraction = (1f - sheetFraction * progress).coerceIn(0f, 1f)
    val expandedScale = resolvePortraitOverlayExpandedPlayerScale(
        containerHeightPx = containerHeightPx,
        containerWidthPx = containerWidthPx,
        currentVideoAspect = currentVideoAspect,
        viewportVerticalOffsetPx = viewportVerticalOffsetPx,
        fillContainer = fillContainer,
        // 展开态按最终顶区比例算 fit（progress 插值用）
        visibleHeightFraction = (1f - sheetFraction).coerceIn(0f, 1f),
        fitToAvailableBand = fitToAvailableBand,
    )
    val scale = 1f - ((1f - expandedScale) * progress)
    val translationYPx = if (containerHeightPx > 0) {
        val visibleBottomPx = visibleHeightFraction * containerHeightPx.toFloat()
        val videoBottomPx = resolvePortraitCommentVideoBottomBeforeTransformPx(
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            currentVideoAspect = currentVideoAspect,
            viewportVerticalOffsetPx = viewportVerticalOffsetPx,
            fillContainer = fillContainer
        )
        // 目标：缩放后视频底边贴半屏顶；progress 插值回落时不额外乱跳。
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

private fun resolvePortraitCommentVideoHeightBeforeTransformPx(
    containerWidthPx: Int,
    containerHeightPx: Int,
    currentVideoAspect: Float,
    fillContainer: Boolean,
): Float {
    if (containerWidthPx <= 0 || containerHeightPx <= 0 || currentVideoAspect <= 0f) {
        return containerHeightPx.toFloat().coerceAtLeast(1f)
    }
    return resolvePortraitVideoViewportSize(
        containerWidth = containerWidthPx,
        containerHeight = containerHeightPx,
        currentVideoAspect = currentVideoAspect,
        fillContainer = fillContainer,
    ).height.toFloat().coerceAtLeast(1f)
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
    val reservedTopPx = topReservedPx.coerceAtLeast(0)
    if (shouldUseEmbeddedVideoSubReplyPresentation() && reservedTopPx == 0) {
        return MAIN_COMMENT_SHEET_HEIGHT_FRACTION
    }

    if (screenHeightPx <= 0) return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION
    if (reservedTopPx == 0) return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION

    val availableHeightPx = (screenHeightPx - reservedTopPx).coerceAtLeast(0)
    if (availableHeightPx == 0) return FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION

    return (availableHeightPx.toFloat() / screenHeightPx.toFloat())
        .coerceIn(0f, FULLSCREEN_VIDEO_SUB_REPLY_SHEET_HEIGHT_FRACTION)
}

internal fun resolveVideoSubReplySheetScrimAlpha(): Float = 0f
