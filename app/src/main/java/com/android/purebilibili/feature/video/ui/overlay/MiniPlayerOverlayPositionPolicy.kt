package com.android.purebilibili.feature.video.ui.overlay

import kotlin.math.abs

internal data class MiniPlayerOverlayOffset(
    val x: Float,
    val y: Float
)

internal data class MiniPlayerResizeBounds(
    val minWidthDp: Float,
    val maxWidthDp: Float
)

internal enum class MiniPlayerContentDragIntent {
    UNDECIDED,
    SEEK,
    MOVE
}

internal fun clampMiniPlayerOverlayOffset(
    offsetX: Float,
    offsetY: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    miniPlayerWidthPx: Float,
    miniPlayerHeightPx: Float,
    outerPaddingPx: Float,
    topInsetPx: Float,
    bottomInsetPx: Float
): MiniPlayerOverlayOffset {
    val minX = outerPaddingPx
    val maxX = (screenWidthPx - miniPlayerWidthPx - outerPaddingPx).coerceAtLeast(minX)
    val minY = outerPaddingPx + topInsetPx
    val maxY = (screenHeightPx - miniPlayerHeightPx - outerPaddingPx - bottomInsetPx).coerceAtLeast(minY)
    return MiniPlayerOverlayOffset(
        x = offsetX.coerceIn(minX, maxX),
        y = offsetY.coerceIn(minY, maxY)
    )
}

internal fun resolveMiniPlayerInitialOverlayOffset(
    cardLeftPx: Float?,
    entryFromLeft: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float,
    miniPlayerWidthPx: Float,
    miniPlayerHeightPx: Float,
    outerPaddingPx: Float,
    topInsetPx: Float,
    bottomInsetPx: Float
): MiniPlayerOverlayOffset {
    return clampMiniPlayerOverlayOffset(
        offsetX = cardLeftPx
            ?: if (entryFromLeft) {
                outerPaddingPx
            } else {
                screenWidthPx - miniPlayerWidthPx - outerPaddingPx
            },
        offsetY = resolveMiniPlayerDockedBottomOffsetY(
            screenHeightPx = screenHeightPx,
            miniPlayerHeightPx = miniPlayerHeightPx,
            outerPaddingPx = outerPaddingPx,
            bottomInsetPx = bottomInsetPx
        ),
        screenWidthPx = screenWidthPx,
        screenHeightPx = screenHeightPx,
        miniPlayerWidthPx = miniPlayerWidthPx,
        miniPlayerHeightPx = miniPlayerHeightPx,
        outerPaddingPx = outerPaddingPx,
        topInsetPx = topInsetPx,
        bottomInsetPx = bottomInsetPx
    )
}

internal fun resolveMiniPlayerOffsetAfterSizeChanged(
    offsetX: Float,
    offsetY: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    miniPlayerWidthPx: Float,
    miniPlayerHeightPx: Float,
    outerPaddingPx: Float,
    topInsetPx: Float,
    bottomInsetPx: Float
): MiniPlayerOverlayOffset {
    return clampMiniPlayerOverlayOffset(
        offsetX = offsetX,
        offsetY = offsetY,
        screenWidthPx = screenWidthPx,
        screenHeightPx = screenHeightPx,
        miniPlayerWidthPx = miniPlayerWidthPx,
        miniPlayerHeightPx = miniPlayerHeightPx,
        outerPaddingPx = outerPaddingPx,
        topInsetPx = topInsetPx,
        bottomInsetPx = bottomInsetPx
    )
}

internal fun resolveMiniPlayerDockedBottomOffsetY(
    screenHeightPx: Float,
    miniPlayerHeightPx: Float,
    outerPaddingPx: Float,
    bottomInsetPx: Float
): Float {
    return screenHeightPx - miniPlayerHeightPx - outerPaddingPx - bottomInsetPx
}

internal fun resolveMiniPlayerContentDragIntent(
    totalDragX: Float,
    totalDragY: Float,
    seekEnabled: Boolean,
    touchSlopPx: Float
): MiniPlayerContentDragIntent {
    val horizontal = abs(totalDragX)
    val vertical = abs(totalDragY)
    val dominant = maxOf(horizontal, vertical)
    if (dominant < touchSlopPx) return MiniPlayerContentDragIntent.UNDECIDED
    if (!seekEnabled) return MiniPlayerContentDragIntent.MOVE
    return if (horizontal > vertical * 1.35f) {
        MiniPlayerContentDragIntent.SEEK
    } else {
        MiniPlayerContentDragIntent.MOVE
    }
}

internal fun resolveMiniPlayerSeekTargetPosition(
    dragStartPositionMs: Long,
    dragDeltaPx: Float,
    miniPlayerWidthPx: Float,
    durationMs: Long
): Long {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    if (safeDurationMs <= 0L) return 0L
    val safeStartPositionMs = dragStartPositionMs.coerceIn(0L, safeDurationMs)
    if (miniPlayerWidthPx <= 0f) return safeStartPositionMs

    val seekDeltaMs = (dragDeltaPx / miniPlayerWidthPx * safeDurationMs).toLong()
    return (safeStartPositionMs + seekDeltaMs).coerceIn(0L, safeDurationMs)
}

internal fun resolveMiniPlayerResizeBounds(
    defaultWidthDp: Int,
    defaultHeightDp: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
    outerPaddingDp: Int,
    topInsetDp: Int,
    bottomInsetDp: Int
): MiniPlayerResizeBounds {
    val aspectRatio = defaultWidthDp.toFloat() / defaultHeightDp.coerceAtLeast(1)
    val availableWidth = (screenWidthDp - outerPaddingDp * 2).coerceAtLeast(defaultWidthDp)
    val availableHeight =
        (screenHeightDp - outerPaddingDp * 2 - topInsetDp - bottomInsetDp).coerceAtLeast(defaultHeightDp)
    val maxWidthByHeight = availableHeight * aspectRatio
    return MiniPlayerResizeBounds(
        minWidthDp = (defaultWidthDp * 0.75f).coerceAtLeast(168f),
        maxWidthDp = minOf(availableWidth.toFloat(), maxWidthByHeight, defaultWidthDp * 1.75f)
            .coerceAtLeast(defaultWidthDp.toFloat())
    )
}

internal fun resolveResizedMiniPlayerWidth(
    currentWidthPx: Float,
    dragDeltaX: Float,
    dragDeltaY: Float,
    aspectRatio: Float,
    minWidthPx: Float,
    maxWidthPx: Float
): Float {
    val safeAspectRatio = aspectRatio.coerceAtLeast(0.1f)
    val projectedWidthDelta = (dragDeltaX + dragDeltaY * safeAspectRatio) / 2f
    return (currentWidthPx + projectedWidthDelta).coerceIn(minWidthPx, maxWidthPx)
}
