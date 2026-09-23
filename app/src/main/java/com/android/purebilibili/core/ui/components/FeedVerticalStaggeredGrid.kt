package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/**
 * Keeps the feed's chrome inset in item space instead of the staggered grid's before-padding
 * region. Uneven lanes can otherwise retain different anchors inside that region when reversing
 * a slow scroll. A full-line inset gives all lanes the same origin and still scrolls under chrome.
 *
 * The inset is a real grid item: callers must use grid indices (including this item) for scroll
 * restoration, and item keys when mapping a visible item back to feed data.
 */
@Composable
internal fun FeedVerticalStaggeredGrid(
    columns: StaggeredGridCells,
    state: LazyStaggeredGridState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalItemSpacing: Dp = 0.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(0.dp),
    // Opt in only for incremental feeds whose content is cards plus a single old-content divider.
    prependItemKeys: List<String> = emptyList(),
    prependDividerIndex: Int = -1,
    content: LazyStaggeredGridScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val topInset = contentPadding.calculateTopPadding()
    // Small decorative padding needs no header; do not add an extra gap when inset < spacing.
    val hasInsetItem = topInset > verticalItemSpacing && topInset > 0.dp
    val viewportStartOffset = with(LocalDensity.current) { topInset.roundToPx() }
    val prependAnchor = remember(state, prependItemKeys, prependDividerIndex, hasInsetItem, viewportStartOffset) {
        // Capture the last rendered card position before the new item provider is measured.
        // Do not subscribe composition to frame-rate layout changes.
        Snapshot.withoutReadObservation {
            val layout = state.layoutInfo
            resolveFeedPrependScrollTarget(
                itemKeys = prependItemKeys,
                dividerIndex = prependDividerIndex,
                leadingItemCount = if (hasInsetItem) 1 else 0,
                visibleItems = layout.visibleItemsInfo.map {
                    FeedVisibleAnchor(it.key, it.index, it.offset.y, it.size.height)
                },
                viewportStartOffset = viewportStartOffset,
            )?.let { layout to it }
        }
    }
    LaunchedEffect(state, prependAnchor) {
        val (previousLayout, target) = prependAnchor ?: return@LaunchedEffect
        // Wait for the new indices to exist. Scrolling against the old provider can anchor to
        // an unrelated card at the target index, especially when only one item was prepended.
        snapshotFlow { state.layoutInfo }.first { it !== previousLayout }
        state.scrollToItem(target.index, target.scrollOffset)
    }
    LazyVerticalStaggeredGrid(
        columns = columns,
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(direction),
            top = if (hasInsetItem) 0.dp else topInset,
            end = contentPadding.calculateEndPadding(direction),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalItemSpacing = verticalItemSpacing,
        horizontalArrangement = horizontalArrangement,
    ) {
        if (hasInsetItem) {
            item(
                key = "feed_chrome_top_inset",
                contentType = "feed_chrome_top_inset",
                span = StaggeredGridItemSpan.FullLine,
            ) {
                Spacer(Modifier.height(topInset - verticalItemSpacing))
            }
        }
        content()
    }
}
