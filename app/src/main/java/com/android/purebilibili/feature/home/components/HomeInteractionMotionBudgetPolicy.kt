package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import com.android.purebilibili.core.ui.AppTopTabPresentation

import kotlin.math.roundToInt

internal const val HOME_HEADER_SECONDARY_BLUR_RESTORE_DELAY_MS = 120L

enum class HomeInteractionMotionBudget {
    FULL,
    REDUCED
}

internal data class TopTabScrollTarget(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffsetPx: Int
)

internal fun resolveHomeTopTabViewportSyncEnabled(
    currentTabHeightDp: Float,
    tabAlpha: Float,
    tabContentAlpha: Float,
    minVisibleHeightDp: Float = 1f,
    minVisibleAlpha: Float = 0.01f
): Boolean {
    return currentTabHeightDp > minVisibleHeightDp &&
        tabAlpha > minVisibleAlpha &&
        tabContentAlpha > minVisibleAlpha
}

internal fun resolveHomeInteractionMotionBudget(
    isPagerScrolling: Boolean,
    isProgrammaticPageSwitchInProgress: Boolean,
    isFeedScrolling: Boolean
): HomeInteractionMotionBudget {
    return if (isPagerScrolling || isProgrammaticPageSwitchInProgress || isFeedScrolling) {
        HomeInteractionMotionBudget.REDUCED
    } else {
        HomeInteractionMotionBudget.FULL
    }
}

internal fun shouldAnimateTopTabAutoScroll(
    selectedIndex: Int,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    budget: HomeInteractionMotionBudget
): Boolean {
    if (firstVisibleIndex > lastVisibleIndex) return true
    val isTargetOutsideViewport = selectedIndex < firstVisibleIndex || selectedIndex > lastVisibleIndex
    if (budget == HomeInteractionMotionBudget.REDUCED) {
        return isTargetOutsideViewport
    }
    return isTargetOutsideViewport
}

internal fun shouldSyncHomeTopTabViewport(
    pagerIsScrolling: Boolean,
    targetIsOutsideViewport: Boolean
): Boolean {
    return !pagerIsScrolling || targetIsOutsideViewport
}

internal fun resolveTopTabViewportAnchorIndex(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerIsScrolling: Boolean
): Int {
    if (!pagerIsScrolling) return pagerCurrentPage ?: selectedIndex
    return pagerTargetPage ?: pagerCurrentPage ?: selectedIndex
}

internal fun resolveTopTabPagerPosition(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerCurrentPageOffsetFraction: Float?,
    pagerIsScrolling: Boolean
): Float {
    if (!pagerIsScrolling) return (pagerCurrentPage ?: selectedIndex).toFloat()
    val currentPage = pagerCurrentPage ?: return selectedIndex.toFloat()
    val offsetFraction = pagerCurrentPageOffsetFraction ?: 0f
    // 手势打断程序动画时 targetPage 可能仍指向旧目标；只跟随实时 offset，
    // 才能让顶部指示器贴住屏幕中央的当前拖动位置。
    return currentPage + offsetFraction
}

internal fun resolveTopTabIndicatorRenderPosition(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerCurrentPageOffsetFraction: Float?,
    pagerIsScrolling: Boolean
): Float {
    return resolveTopTabPagerPosition(
        selectedIndex = selectedIndex,
        pagerCurrentPage = pagerCurrentPage,
        pagerTargetPage = pagerTargetPage,
        pagerCurrentPageOffsetFraction = pagerCurrentPageOffsetFraction,
        pagerIsScrolling = pagerIsScrolling
    )
}

internal fun resolveTopTabSelectedContentPosition(
    selectedIndex: Int,
    pagerCurrentPage: Int?,
    pagerTargetPage: Int?,
    pagerCurrentPageOffsetFraction: Float?,
    pagerIsScrolling: Boolean
): Float {
    return resolveTopTabPagerPosition(
        selectedIndex = selectedIndex,
        pagerCurrentPage = pagerCurrentPage,
        pagerTargetPage = pagerTargetPage,
        pagerCurrentPageOffsetFraction = pagerCurrentPageOffsetFraction,
        pagerIsScrolling = pagerIsScrolling
    )
}

internal fun resolveTopTabFollowScrollTarget(
    indicatorPosition: Float,
    itemWidthPx: Float,
    itemCount: Int,
    viewportWidthPx: Float,
    currentFirstVisibleItemIndex: Int,
    currentFirstVisibleItemScrollOffsetPx: Int,
    maxScrollPx: Float,
    edgeBufferPx: Float
): TopTabScrollTarget {
    val currentTarget = TopTabScrollTarget(
        firstVisibleItemIndex = currentFirstVisibleItemIndex.coerceAtLeast(0),
        firstVisibleItemScrollOffsetPx = currentFirstVisibleItemScrollOffsetPx.coerceAtLeast(0)
    )
    if (itemWidthPx <= 0f || itemCount <= 0 || viewportWidthPx <= 0f || maxScrollPx <= 0f) {
        return currentTarget
    }

    val clampedPosition = indicatorPosition.coerceIn(0f, (itemCount - 1).toFloat())
    val selectedItemIndex = clampedPosition.roundToInt().coerceIn(0, itemCount - 1)
    val usableViewportWidthPx = (viewportWidthPx - edgeBufferPx.coerceAtLeast(0f) * 2f)
        .coerceAtLeast(itemWidthPx)
    val visibleSlots = (usableViewportWidthPx / itemWidthPx).toInt().coerceAtLeast(1)
    val centerSlotIndex = (visibleSlots / 2).coerceAtLeast(0)
    val maxFirstVisibleByCount = (itemCount - visibleSlots).coerceAtLeast(0)
    val maxFirstVisibleByScroll = (maxScrollPx / itemWidthPx).toInt().coerceAtLeast(0)
    val maxFirstVisibleIndex = minOf(maxFirstVisibleByCount, maxFirstVisibleByScroll)
    val targetIndex = (selectedItemIndex - centerSlotIndex)
        .coerceIn(0, maxFirstVisibleIndex)

    return TopTabScrollTarget(
        firstVisibleItemIndex = targetIndex,
        firstVisibleItemScrollOffsetPx = 0
    )
}

internal fun resolveMd3TopTabViewportPosition(
    visibleIndices: List<Int>,
    absolutePagerPosition: Float
): Float {
    if (visibleIndices.isEmpty()) return 0f
    if (visibleIndices.size == 1) return 0f

    val firstIndex = visibleIndices.first().toFloat()
    val lastIndex = visibleIndices.last().toFloat()
    if (absolutePagerPosition <= firstIndex) return 0f
    if (absolutePagerPosition >= lastIndex) return visibleIndices.lastIndex.toFloat()

    visibleIndices.zipWithNext().forEachIndexed { slotIndex, (start, end) ->
        val startFloat = start.toFloat()
        val endFloat = end.toFloat()
        if (absolutePagerPosition in startFloat..endFloat) {
            val span = (endFloat - startFloat).coerceAtLeast(0.0001f)
            val fraction = (absolutePagerPosition - startFloat) / span
            return slotIndex + fraction
        }
    }

    return visibleIndices.indexOfLast { it.toFloat() <= absolutePagerPosition }
        .coerceAtLeast(0)
        .toFloat()
}

internal fun resolveMd3TopTabIndicatorTranslationPx(
    absolutePagerPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    indicatorWidthPx: Float,
    contentPaddingPx: Float = 0f
): Float {
    if (itemWidthPx <= 0f || indicatorWidthPx <= 0f) return contentPaddingPx
    val indicatorCenterPx = contentPaddingPx + (absolutePagerPosition * itemWidthPx) + (itemWidthPx / 2f)
    return indicatorCenterPx - rowScrollOffsetPx - (indicatorWidthPx / 2f)
}

internal data class Md3TopTabUnderlineBounds(
    val translationXPx: Float,
    val widthPx: Float,
)

/**
 * PiliPlus-style elastic underline: while the pager crosses a tab boundary the underline
 * temporarily spans the old and new tab centers, then contracts back to its resting width.
 */
internal fun resolveMd3TopTabUnderlineBounds(
    absolutePagerPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    indicatorWidthPx: Float,
    contentPaddingPx: Float = 0f,
): Md3TopTabUnderlineBounds {
    val restingLeft = resolveMd3TopTabIndicatorTranslationPx(
        absolutePagerPosition = absolutePagerPosition,
        itemWidthPx = itemWidthPx,
        rowScrollOffsetPx = rowScrollOffsetPx,
        indicatorWidthPx = indicatorWidthPx,
        contentPaddingPx = contentPaddingPx,
    )
    if (itemWidthPx <= 0f || indicatorWidthPx <= 0f) {
        return Md3TopTabUnderlineBounds(restingLeft, indicatorWidthPx.coerceAtLeast(0f))
    }

    val transitionFraction = (
        absolutePagerPosition - kotlin.math.floor(absolutePagerPosition)
    ).coerceIn(0f, 1f)
    // The leading edge gets ahead while the trailing edge catches up. Traversing the same
    // geometry backwards automatically swaps their roles, so swipe direction needs no state.
    val trailingEdgeProgress = 1f -
        kotlin.math.cos(transitionFraction * Math.PI.toFloat() / 2f)
    val leadingEdgeProgress =
        kotlin.math.sin(transitionFraction * Math.PI.toFloat() / 2f)
    val startTranslation = resolveMd3TopTabIndicatorTranslationPx(
        absolutePagerPosition = kotlin.math.floor(absolutePagerPosition),
        itemWidthPx = itemWidthPx,
        rowScrollOffsetPx = rowScrollOffsetPx,
        indicatorWidthPx = indicatorWidthPx,
        contentPaddingPx = contentPaddingPx,
    )
    val stretchedWidth = indicatorWidthPx +
        itemWidthPx * (leadingEdgeProgress - trailingEdgeProgress)
    return Md3TopTabUnderlineBounds(
        translationXPx = startTranslation + itemWidthPx * trailingEdgeProgress,
        widthPx = stretchedWidth,
    )
}

internal fun resolveIosTopTabCapsuleTranslationPx(
    absolutePagerPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    contentPaddingPx: Float = 0f
): Float {
    if (itemWidthPx <= 0f) return contentPaddingPx
    return contentPaddingPx + absolutePagerPosition.coerceAtLeast(0f) * itemWidthPx - rowScrollOffsetPx
}

internal fun resolveIosTopTabCapsuleTargetTranslationPx(
    measuredSelectedItemLeftPx: Float?,
    absolutePagerPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    contentPaddingPx: Float = 0f,
    followPagerPosition: Boolean = false
): Float {
    val measuredLeft = measuredSelectedItemLeftPx
    if (!followPagerPosition && measuredLeft != null && !measuredLeft.isNaN()) {
        return measuredLeft
    }
    return resolveIosTopTabCapsuleTranslationPx(
        absolutePagerPosition = absolutePagerPosition,
        itemWidthPx = itemWidthPx,
        rowScrollOffsetPx = rowScrollOffsetPx,
        contentPaddingPx = contentPaddingPx
    )
}

internal fun shouldAnimateIosTopTabCapsule(
    pagerIsDragging: Boolean,
    pagerIsScrolling: Boolean
): Boolean {
    return !pagerIsDragging && !pagerIsScrolling
}

internal fun shouldDrawLightweightTopTabItemContainer(
    presentation: AppTopTabPresentation,
    skinPlainStyle: Boolean,
    hasSkinStickerIcon: Boolean
): Boolean {
    // Skin stickers keep per-item chrome. BiliPai moving indicator owns selection for
    // MOVING_CAPSULE + TONAL_CAPSULE (Miuix) — never double-draw secondaryContainer pills.
    if (skinPlainStyle || hasSkinStickerIcon) return true
    return when (presentation) {
        AppTopTabPresentation.MOVING_CAPSULE,
        AppTopTabPresentation.TONAL_CAPSULE -> false
        AppTopTabPresentation.MATERIAL_UNDERLINE -> true
    }
}

internal fun shouldUseLightweightTopTabItemClickIndication(
    presentation: AppTopTabPresentation,
    skinPlainStyle: Boolean,
    usesCapsuleIndicator: Boolean
): Boolean {
    if (skinPlainStyle) return true
    if (usesCapsuleIndicator) return false
    return presentation == AppTopTabPresentation.MATERIAL_UNDERLINE
}

internal const val MD3_TOP_TAB_INDICATOR_DURATION_MILLIS = 300

internal val Md3TopTabIndicatorFlutterEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

internal val Md3TopTabIndicatorDecelerate = Easing { fraction ->
    kotlin.math.sin(Md3TopTabIndicatorFlutterEase.transform(fraction) * Math.PI.toFloat() / 2f)
}

internal val Md3TopTabIndicatorAccelerate = Easing { fraction ->
    1f - kotlin.math.cos(Md3TopTabIndicatorFlutterEase.transform(fraction) * Math.PI.toFloat() / 2f)
}

internal fun shouldAnimateMd3TopTabUnderline(
    pagerIsDragging: Boolean,
    topTabIndicatorOwnsPosition: Boolean
): Boolean {
    return !pagerIsDragging && !topTabIndicatorOwnsPosition
}

internal data class Md3TopTabTargetBounds(
    val leftPx: Float,
    val rightPx: Float,
)

internal fun resolveMd3TopTabTargetBounds(
    targetIndex: Int,
    itemWidthPx: Float,
    indicatorWidthPx: Float,
    contentPaddingPx: Float = 0f,
): Md3TopTabTargetBounds {
    if (itemWidthPx <= 0f || indicatorWidthPx <= 0f) {
        return Md3TopTabTargetBounds(contentPaddingPx, contentPaddingPx)
    }
    val centerPx = contentPaddingPx + (targetIndex * itemWidthPx) + (itemWidthPx / 2f)
    val halfW = indicatorWidthPx / 2f
    return Md3TopTabTargetBounds(
        leftPx = centerPx - halfW,
        rightPx = centerPx + halfW,
    )
}

internal fun resolveMd3TopTabUnderlineTapBounds(
    animatedLeftPx: Float,
    animatedRightPx: Float,
    rowScrollOffsetPx: Float,
): Md3TopTabUnderlineBounds {
    return Md3TopTabUnderlineBounds(
        translationXPx = animatedLeftPx - rowScrollOffsetPx,
        widthPx = (animatedRightPx - animatedLeftPx).coerceAtLeast(0f),
    )
}

internal fun resolveMd3TopTabTapContentPosition(
    animatedLeftPx: Float,
    animatedRightPx: Float,
    itemWidthPx: Float,
    contentPaddingPx: Float,
    fallbackIndex: Int,
    categoryCount: Int,
): Float {
    if (itemWidthPx <= 0f || categoryCount <= 0) return fallbackIndex.toFloat()
    val centerPx = (animatedLeftPx + animatedRightPx) / 2f
    val position = (centerPx - contentPaddingPx - itemWidthPx / 2f) / itemWidthPx
    return position.coerceIn(0f, (categoryCount - 1).toFloat())
}
