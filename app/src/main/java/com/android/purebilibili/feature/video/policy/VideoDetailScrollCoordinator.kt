package com.android.purebilibili.feature.video.policy

import kotlin.math.abs

internal data class VideoDetailScrollUpdate(
    val nextOffsetPx: Float,
    val consumedDeltaPx: Float
)

/**
 * 上滑折叠播放器（preScroll）。
 *
 * [layoutAlreadyCollapsed]：简介阈值折叠 / 评论页紧凑播放器已把布局压扁时必须为 true。
 * 此时若仍消费 available.y，相关推荐列表会先「空滑」一整段折叠距离，
 * 体感像上滑不跟手、甚至突然被顶回去。
 */
internal fun reduceVideoDetailPreScroll(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    inlinePortraitScrollEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    layoutAlreadyCollapsed: Boolean = false,
    minUpdateDeltaPx: Float = 0.75f
): VideoDetailScrollUpdate? {
    if (!inlinePortraitScrollEnabled || isPortraitFullscreen) return null
    if (layoutAlreadyCollapsed) return null
    if (deltaPx >= 0f) return null
    return reduceVideoDetailScrollOffset(
        currentOffsetPx = currentOffsetPx,
        deltaPx = deltaPx,
        minOffsetPx = minOffsetPx,
        minUpdateDeltaPx = minUpdateDeltaPx
    )
}

/**
 * 下滑展开播放器（postScroll）。
 * 布局已由阈值/评论页折叠时同样不消费，把剩余滚动留给列表。
 */
internal fun reduceVideoDetailPostScroll(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    inlinePortraitScrollEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    layoutAlreadyCollapsed: Boolean = false,
    minUpdateDeltaPx: Float = 0.75f
): VideoDetailScrollUpdate? {
    if (!inlinePortraitScrollEnabled || isPortraitFullscreen) return null
    if (layoutAlreadyCollapsed) return null
    if (deltaPx <= 0f) return null
    return reduceVideoDetailScrollOffset(
        currentOffsetPx = currentOffsetPx,
        deltaPx = deltaPx,
        minOffsetPx = minOffsetPx,
        minUpdateDeltaPx = minUpdateDeltaPx
    )
}

/** 阈值折叠或评论紧凑态下，手势折叠应让路给列表滚动。 */
internal fun shouldSkipGesturePlayerCollapseForLayout(
    compactForIntroScroll: Boolean,
    compactForCommentTab: Boolean,
): Boolean = compactForCommentTab

internal fun shouldTrackVideoDetailCollapseMotion(
    inlinePortraitScrollEnabled: Boolean,
    previousOffsetPx: Float,
    currentOffsetPx: Float,
    minMovementPx: Float = 0.75f,
): Boolean = inlinePortraitScrollEnabled &&
    previousOffsetPx.isFinite() &&
    currentOffsetPx.isFinite() &&
    abs(currentOffsetPx - previousOffsetPx) >= minMovementPx

internal fun resolveVideoDetailCollapseProgress(
    playerHeightOffsetPx: Float,
    collapseRangePx: Float,
    isPortraitFullscreen: Boolean
): Float {
    if (isPortraitFullscreen) return 0f
    if (collapseRangePx <= 0f) return 0f
    val effectiveOffset = playerHeightOffsetPx.coerceAtMost(0f)
    return (abs(effectiveOffset) / collapseRangePx).coerceIn(0f, 1f)
}

private fun reduceVideoDetailScrollOffset(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    maxOffsetPx: Float = 0f,
    minUpdateDeltaPx: Float
): VideoDetailScrollUpdate? {
    if (abs(deltaPx) < minUpdateDeltaPx) return null
    val nextOffset = (currentOffsetPx + deltaPx).coerceIn(minOffsetPx, maxOffsetPx)
    val consumedDelta = nextOffset - currentOffsetPx
    if (abs(consumedDelta) < minUpdateDeltaPx) return null
    return VideoDetailScrollUpdate(
        nextOffsetPx = nextOffset,
        consumedDeltaPx = consumedDelta
    )
}
