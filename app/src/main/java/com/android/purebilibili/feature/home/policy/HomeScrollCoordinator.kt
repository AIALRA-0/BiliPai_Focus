package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.core.store.CommonListHeaderCollapseMode
import com.android.purebilibili.core.store.HomeBarHideType
import com.android.purebilibili.core.store.HomeHeaderCollapseMode
import com.android.purebilibili.feature.home.resolveNextHomeGlobalScrollOffset
import kotlin.math.abs
import kotlin.math.round

internal enum class BottomBarVisibilityIntent {
    SHOW,
    HIDE
}

internal data class HomeScrollUpdate(
    val headerOffsetPx: Float,
    val bottomBarVisibilityIntent: BottomBarVisibilityIntent?,
    val globalScrollOffset: Float?,
    val shouldAnimateHeader: Boolean = false
)

internal data class HomeHeaderSettleTransition(
    val targetOffsetPx: Float,
    val shouldAnimate: Boolean
)

internal fun resolveHomeRecommendationHeaderCollapseMode(
    homeHeaderCollapseMode: HomeHeaderCollapseMode
): HomeHeaderCollapseMode {
    return homeHeaderCollapseMode
}

internal fun quantizeHomeHeaderOffset(
    offsetPx: Float,
    stepPx: Float
): Float {
    if (stepPx <= 0f) return offsetPx
    return round(offsetPx / stepPx) * stepPx
}

internal fun resolveHomeEmbeddedPageTopPaddingPx(
    expandedTopPaddingPx: Float,
    headerOffsetPx: Float,
    collapsedTabInsetPx: Float,
    minimumTopPaddingPx: Float,
): Float = (
    expandedTopPaddingPx + headerOffsetPx - collapsedTabInsetPx
).coerceAtLeast(minimumTopPaddingPx)

internal fun shouldHandleHomeVerticalPreScroll(
    deltaX: Float,
    deltaY: Float,
    minimumVerticalDeltaPx: Float = 0.5f
): Boolean {
    val absoluteDeltaY = abs(deltaY)
    if (absoluteDeltaY < minimumVerticalDeltaPx) return false
    return absoluteDeltaY >= abs(deltaX)
}

internal fun resolveHomeHeaderSettleTransition(
    currentHeaderOffsetPx: Float,
    targetHeaderOffsetPx: Float,
    animationThresholdPx: Float = 0.5f
): HomeHeaderSettleTransition {
    return HomeHeaderSettleTransition(
        targetOffsetPx = targetHeaderOffsetPx,
        shouldAnimate = abs(currentHeaderOffsetPx - targetHeaderOffsetPx) > animationThresholdPx
    )
}

internal fun resolveHomeHeaderReleaseTarget(
    maxHeaderCollapsePx: Float,
    canRevealHeader: Boolean,
    collapseMode: CommonListHeaderCollapseMode = CommonListHeaderCollapseMode.SHOW_AT_TOP_ONLY,
): Float {
    if (
        maxHeaderCollapsePx <= 0f ||
            canRevealHeader ||
            collapseMode == CommonListHeaderCollapseMode.SHOW_ON_REVERSE_SCROLL
    ) {
        return 0f
    }
    return -maxHeaderCollapsePx
}

internal fun resolveHomeHeaderTransitionRunning(
    isFeedScrolling: Boolean,
    isPagerScrolling: Boolean,
    isHeaderSettleAnimating: Boolean
): Boolean {
    return isFeedScrolling || isPagerScrolling || isHeaderSettleAnimating
}

internal fun resolveHomeHeaderListIndex(
    displayedEntryIsSubscription: Boolean,
    categoryFirstVisibleIndex: Int,
    subscriptionFirstVisibleIndex: Int,
): Int {
    return if (displayedEntryIsSubscription) {
        subscriptionFirstVisibleIndex
    } else {
        categoryFirstVisibleIndex
    }
}

internal fun canRevealHomeHeaderForList(
    firstVisibleItemIndex: Int,
    listMissing: Boolean,
): Boolean {
    return listMissing || firstVisibleItemIndex == 0
}

/** 只在列表真正贴顶时，把手势或惯性的下拉位移先分配给尚未展开的顶栏。 */
internal fun resolveHomeHeaderRevealConsumedY(
    currentHeaderOffsetPx: Float,
    deltaY: Float,
    listAtTop: Boolean,
    collapseEnabled: Boolean,
): Float = if (collapseEnabled && listAtTop && deltaY > 0f) {
    minOf(deltaY, (-currentHeaderOffsetPx).coerceAtLeast(0f))
} else 0f

internal fun shouldExpandHomeHeaderForSettledPage(
    currentHeaderOffsetPx: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (currentHeaderOffsetPx >= 0f) return false
    if (firstVisibleItemIndex != 0) return false
    return firstVisibleItemScrollOffset == 0
}

internal fun resolveHomeHeaderOffsetForSettledPage(
    currentHeaderOffsetPx: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    maxHeaderCollapsePx: Float
): Float {
    if (maxHeaderCollapsePx <= 0f) return 0f
    return if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
        // 目标页在顶部时保留当前折叠态，避免切到「热门」等未滚动分区时把已折叠顶栏强行展开。
        // 回到顶部后的展开仍由列表上滑 + canRevealHeader 驱动。
        currentHeaderOffsetPx.coerceIn(-maxHeaderCollapsePx, 0f)
    } else {
        -maxHeaderCollapsePx
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun reduceHomePreScroll(
    currentHeaderOffsetPx: Float,
    deltaY: Float,
    minHeaderOffsetPx: Float,
    canRevealHeader: Boolean,
    collapseMode: CommonListHeaderCollapseMode = CommonListHeaderCollapseMode.SHOW_AT_TOP_ONLY,
    isHeaderCollapseEnabled: Boolean,
    isBottomBarAutoHideEnabled: Boolean,
    useSideNavigation: Boolean,
    liquidGlassEnabled: Boolean,
    currentGlobalScrollOffset: Float,
    bottomBarVisibilityThresholdPx: Float = 10f,
    hideType: HomeBarHideType = HomeBarHideType.SYNC,
    instantDirectionThresholdPx: Float = 0.5f,
    isHeaderRevealLocked: Boolean = false,
    headerConsumedY: Float = 0f,
    listAtTop: Boolean = canRevealHeader,
    headerDeltaY: Float = deltaY,
): HomeScrollUpdate {
    // 首页允许浏览中反向恢复；通用的仅回顶模式继续保留，回顶锁定期强制展开。
    val nextHeaderOffset = when {
        !isHeaderCollapseEnabled -> 0f
        isHeaderRevealLocked -> 0f
        collapseMode == CommonListHeaderCollapseMode.SHOW_AT_TOP_ONLY && headerDeltaY > 0f && !listAtTop ->
            minHeaderOffsetPx
        collapseMode == CommonListHeaderCollapseMode.SHOW_AT_TOP_ONLY && !canRevealHeader ->
            minHeaderOffsetPx
        headerConsumedY > 0f -> (currentHeaderOffsetPx + headerConsumedY).coerceIn(minHeaderOffsetPx, 0f)
        headerDeltaY == 0f -> currentHeaderOffsetPx.coerceIn(minHeaderOffsetPx, 0f)
        hideType == HomeBarHideType.INSTANT && headerDeltaY <= -instantDirectionThresholdPx -> minHeaderOffsetPx
        hideType == HomeBarHideType.INSTANT && headerDeltaY >= instantDirectionThresholdPx -> 0f
        hideType == HomeBarHideType.INSTANT -> currentHeaderOffsetPx.coerceIn(minHeaderOffsetPx, 0f)
        else -> (currentHeaderOffsetPx + headerDeltaY).coerceIn(minHeaderOffsetPx, 0f)
    }
    val shouldAnimateHeader = hideType == HomeBarHideType.INSTANT &&
        !isHeaderRevealLocked && headerConsumedY == 0f && abs(nextHeaderOffset - currentHeaderOffsetPx) > 0.5f

    val nextBottomBarIntent = when {
        !isBottomBarAutoHideEnabled || useSideNavigation -> null
        deltaY <= -bottomBarVisibilityThresholdPx -> BottomBarVisibilityIntent.HIDE
        deltaY >= bottomBarVisibilityThresholdPx -> BottomBarVisibilityIntent.SHOW
        else -> null
    }

    return HomeScrollUpdate(
        headerOffsetPx = nextHeaderOffset,
        bottomBarVisibilityIntent = nextBottomBarIntent,
        // 被顶栏展开消费的位移不再计入全局滚动偏移，避免贴顶下拉时玻璃效果多算。
        globalScrollOffset = resolveNextHomeGlobalScrollOffset(
            currentOffset = currentGlobalScrollOffset,
            scrollDeltaY = deltaY - headerConsumedY,
            liquidGlassEnabled = liquidGlassEnabled
        ),
        shouldAnimateHeader = shouldAnimateHeader
    )
}
