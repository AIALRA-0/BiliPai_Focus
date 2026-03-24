package com.android.purebilibili.feature.home

import com.android.purebilibili.core.theme.UiPreset
import kotlin.math.min
import kotlin.math.max
import kotlin.math.abs

internal fun resolvePullRefreshThresholdDp(): Float = 56f

enum class HomePullRefreshMotionStyle {
    IOS,
    MD3
}

internal fun resolveHomePullRefreshMotionStyle(uiPreset: UiPreset): HomePullRefreshMotionStyle {
    return if (uiPreset == UiPreset.MD3) {
        HomePullRefreshMotionStyle.MD3
    } else {
        HomePullRefreshMotionStyle.IOS
    }
}

internal fun resolveRequiredPullDistanceDp(
    thresholdDp: Float,
    dragMultiplier: Float
): Float {
    if (dragMultiplier <= 0f) return Float.POSITIVE_INFINITY
    return thresholdDp / dragMultiplier
}

internal fun shouldResetToTopOnRefreshStart(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
}

internal fun shouldResetToTopAfterIncrementalRefresh(
    currentCategory: HomeCategory,
    newItemsCount: Int?,
    isRefreshing: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if ((newItemsCount ?: 0) <= 0) return false
    if (isRefreshing) return false
    return shouldResetToTopOnRefreshStart(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
    )
}

internal fun shouldResetFollowToTopAfterRefreshCompletion(
    currentCategory: HomeCategory,
    resetKey: Long,
    handledKey: Long
): Boolean {
    if (currentCategory != HomeCategory.FOLLOW) return false
    if (resetKey <= 0L) return false
    return resetKey > handledKey
}

internal fun shouldCommitFollowRefreshPresentationAfterPullSettles(
    currentCategory: HomeCategory,
    hasPendingPresentation: Boolean,
    isRefreshing: Boolean,
    isStateAnimating: Boolean,
    distanceFraction: Float,
    contentOffsetFraction: Float = 0f,
    settledDistanceTolerance: Float = 0.01f
): Boolean {
    if (currentCategory != HomeCategory.FOLLOW) return false
    if (!hasPendingPresentation) return false
    if (isRefreshing || isStateAnimating) return false
    val tolerance = settledDistanceTolerance.coerceAtLeast(0f)
    if (distanceFraction > tolerance) return false
    return abs(contentOffsetFraction) <= tolerance
}

internal fun shouldDeferFollowRefreshPreviewWhilePullRefreshing(
    currentCategory: HomeCategory,
    isRefreshing: Boolean,
    hasPendingPresentation: Boolean
): Boolean {
    if (currentCategory != HomeCategory.FOLLOW) return false
    return isRefreshing || hasPendingPresentation
}

internal fun shouldShowReleaseToRefreshHint(
    progress: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean
): Boolean {
    if (isRefreshing) return false
    if (progress < 1f) return false
    return !isStateAnimating
}

internal fun resolvePullRefreshHintText(
    progress: Float,
    isRefreshing: Boolean,
    isStateAnimating: Boolean
): String {
    return when {
        isRefreshing -> "正在刷新..."
        shouldShowReleaseToRefreshHint(
            progress = progress,
            isRefreshing = isRefreshing,
            isStateAnimating = isStateAnimating
        ) -> "松手刷新"
        progress > 0f -> "下拉刷新..."
        else -> ""
    }
}

internal fun resolvePullIndicatorTranslationY(
    dragOffsetPx: Float,
    indicatorHeightPx: Float,
    minGapPx: Float,
    isRefreshing: Boolean
): Float {
    if (isRefreshing) return 0f
    if (dragOffsetPx <= 0f) return -indicatorHeightPx
    val centeredY = (dragOffsetPx / 2f) - (indicatorHeightPx / 2f)
    val maxAllowedY = dragOffsetPx - indicatorHeightPx - minGapPx
    return min(centeredY, maxAllowedY)
}

internal fun resolvePullContentOffsetFraction(
    distanceFraction: Float,
    isRefreshing: Boolean,
    motionStyle: HomePullRefreshMotionStyle = HomePullRefreshMotionStyle.IOS
): Float {
    if (motionStyle == HomePullRefreshMotionStyle.MD3) return 0f
    val clampedDistance = distanceFraction.coerceAtMost(2f).coerceAtLeast(0f)
    return clampedDistance * 0.5f
}
