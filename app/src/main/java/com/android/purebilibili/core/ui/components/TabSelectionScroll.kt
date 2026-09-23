package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

internal fun resolveTabSelectionLeadingSpacePx(itemWidthPx: Float, viewportWidthPx: Float): Int =
    ((viewportWidthPx - itemWidthPx).coerceAtLeast(0f) / 2f).roundToInt()

internal fun resolveTabSelectionScrollOffsetPx(
    selectedIndex: Int,
    itemWidthPx: Float,
    viewportWidthPx: Float,
    maxScrollPx: Int,
    contentPaddingPx: Float = 0f,
): Int {
    if (selectedIndex <= 0 || itemWidthPx <= 0f || viewportWidthPx <= 0f) return 0
    return (contentPaddingPx + selectedIndex * itemWidthPx -
        resolveTabSelectionLeadingSpacePx(itemWidthPx, viewportWidthPx))
        .roundToInt()
        .coerceIn(0, maxScrollPx.coerceAtLeast(0))
}

/** Keeps a continuously moving indicator inside the visible rail during a long drag. */
internal fun resolveScrollableTabIndicatorFollowDeltaPx(
    indicatorPosition: Float,
    itemWidthPx: Float,
    viewportWidthPx: Float,
    currentScrollPx: Float,
    contentPaddingPx: Float = 0f,
    edgePaddingPx: Float = 0f,
): Float {
    if (!indicatorPosition.isFinite() || !currentScrollPx.isFinite() ||
        itemWidthPx <= 0f || viewportWidthPx <= 0f
    ) {
        return 0f
    }
    val indicatorLeftPx = contentPaddingPx + indicatorPosition * itemWidthPx - currentScrollPx
    val indicatorRightPx = indicatorLeftPx + itemWidthPx
    return when {
        indicatorLeftPx < edgePaddingPx -> indicatorLeftPx - edgePaddingPx
        indicatorRightPx > viewportWidthPx - edgePaddingPx ->
            indicatorRightPx - (viewportWidthPx - edgePaddingPx)
        else -> 0f
    }
}

/** Selection and viewport changes move the rail; manual scrolling does not re-trigger it. */
@Composable
internal fun KeepScrollableTabSelectionVisible(
    scrollState: ScrollState,
    selectedIndex: Int,
    itemWidthPx: Float,
    viewportWidthPx: Float,
    contentPaddingPx: Float = 0f,
) {
    val animate = LocalAppThemeConfig.current.uiEntranceAnimationEnabled
    LaunchedEffect(scrollState, selectedIndex, itemWidthPx, viewportWidthPx, contentPaddingPx, animate) {
        // maxValue is unknown before the scroll container is measured. Also follow resizes
        // without restarting this effect on every animation frame or fighting a manual swipe.
        snapshotFlow { scrollState.maxValue }
            .filter { it != Int.MAX_VALUE }
            .collectLatest { maxScrollPx ->
                val target = resolveTabSelectionScrollOffsetPx(
                    selectedIndex, itemWidthPx, viewportWidthPx, maxScrollPx, contentPaddingPx,
                )
                if (animate) scrollState.animateScrollTo(target) else scrollState.scrollTo(target)
            }
    }
}

/** Home's equal-width LazyRow uses the same centered selection, clamped by LazyListState. */
@Composable
internal fun KeepLazyTabSelectionVisible(
    listState: LazyListState,
    selectedIndex: Int,
) {
    val animate = LocalAppThemeConfig.current.uiEntranceAnimationEnabled
    LaunchedEffect(listState, selectedIndex, animate) {
        snapshotFlow {
            val info = listState.layoutInfo
            // Observe geometry, not offsets: dragging the row must remain under user control.
            TabRailGeometry(
                viewportWidthPx = info.viewportSize.width,
                itemWidthPx = info.visibleItemsInfo.firstOrNull()?.size ?: 0,
                contentPaddingPx = info.beforeContentPadding,
                itemCount = info.totalItemsCount,
            )
        }.filter { it.viewportWidthPx > 0 && it.itemWidthPx > 0 && it.itemCount > 0 }
            .collectLatest { geometry ->
                if (!listState.canScrollBackward && !listState.canScrollForward) return@collectLatest
                val target = selectedIndex.coerceIn(0, geometry.itemCount - 1)
                val offset = geometry.contentPaddingPx - resolveTabSelectionLeadingSpacePx(
                    geometry.itemWidthPx.toFloat(), geometry.viewportWidthPx.toFloat(),
                )
                if (animate) {
                    listState.animateScrollToItem(target, scrollOffset = offset)
                } else {
                    listState.scrollToItem(target, scrollOffset = offset)
                }
            }
    }
}

private data class TabRailGeometry(
    val viewportWidthPx: Int,
    val itemWidthPx: Int,
    val contentPaddingPx: Int,
    val itemCount: Int,
)
