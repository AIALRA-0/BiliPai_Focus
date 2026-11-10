package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import kotlinx.coroutines.CancellationException

/** Click-time UI position retained while a related-detail entry covers its parent. */
internal data class VideoDetailParentUiSnapshot(
    val selectedTabIndex: Int,
    val introItemIndex: Int,
    val introItemOffsetPx: Int,
    val commentItemIndex: Int,
    val commentItemOffsetPx: Int,
)

internal fun captureVideoDetailParentUiSnapshot(
    selectedTabIndex: Int,
    introListState: LazyListState,
    commentListState: LazyListState,
): VideoDetailParentUiSnapshot = VideoDetailParentUiSnapshot(
    selectedTabIndex = selectedTabIndex.coerceAtLeast(0),
    introItemIndex = introListState.firstVisibleItemIndex,
    introItemOffsetPx = introListState.firstVisibleItemScrollOffset,
    commentItemIndex = commentListState.firstVisibleItemIndex,
    commentItemOffsetPx = commentListState.firstVisibleItemScrollOffset,
)

internal suspend fun restoreVideoDetailParentUiSnapshot(
    snapshot: VideoDetailParentUiSnapshot,
    introListState: LazyListState,
    commentListState: LazyListState,
    pagerState: PagerState,
) {
    val targetPage = snapshot.selectedTabIndex.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
    if (pagerState.isScrollInProgress || pagerState.currentPage != targetPage ||
        pagerState.currentPageOffsetFraction != 0f
    ) {
        try {
            pagerState.scrollToPage(targetPage)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IllegalArgumentException) {
            // 页面数量变化时保留当前页。
        }
    }
    restoreVideoDetailListPosition(
        listState = introListState,
        itemIndex = snapshot.introItemIndex,
        itemOffsetPx = snapshot.introItemOffsetPx,
    )
    restoreVideoDetailListPosition(
        listState = commentListState,
        itemIndex = snapshot.commentItemIndex,
        itemOffsetPx = snapshot.commentItemOffsetPx,
    )
}

private suspend fun restoreVideoDetailListPosition(
    listState: LazyListState,
    itemIndex: Int,
    itemOffsetPx: Int,
) {
    val totalItems = listState.layoutInfo.totalItemsCount
    if (totalItems <= 0) return
    val safeIndex = itemIndex.coerceIn(0, totalItems - 1)
    val safeOffset = if (safeIndex == itemIndex) itemOffsetPx.coerceAtLeast(0) else 0
    if (!listState.isScrollInProgress && listState.firstVisibleItemIndex == safeIndex &&
        listState.firstVisibleItemScrollOffset == safeOffset
    ) return
    try {
        listState.scrollToItem(
            index = safeIndex,
            scrollOffset = safeOffset,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: IllegalArgumentException) {
        // 列表数据变化时保留当前位置。
    }
}

/** Nested back navigation restores the parent's HELD session rather than global IDLE. */
internal fun canReleaseRelatedVideoNavigation(
    phase: VideoCardTransitionBackgroundPhase,
    gestureInProgress: Boolean,
    gestureRestoreInProgress: Boolean,
): Boolean = !gestureInProgress && !gestureRestoreInProgress &&
    (phase == VideoCardTransitionBackgroundPhase.IDLE ||
        phase == VideoCardTransitionBackgroundPhase.HELD)
