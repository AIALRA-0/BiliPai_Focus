package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.stopScroll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Releases mutations retained across fullscreen without resetting list offsets or the page. */
internal suspend fun stopVideoDetailInlineScrolls(
    introListState: ScrollableState,
    commentListState: ScrollableState,
    pagerState: ScrollableState,
): Unit = coroutineScope {
    // Default priority cannot interrupt a drag or tab animation running at UserInput priority.
    // Skip idle states: an unopened LazyListState/PagerState may still be awaiting its first layout.
    // This is input lifecycle recovery for every material; native layout and motion stay unchanged.
    if (introListState.isScrollInProgress) {
        launch { introListState.stopScroll(MutatePriority.PreventUserInput) }
    }
    if (commentListState.isScrollInProgress) {
        launch { commentListState.stopScroll(MutatePriority.PreventUserInput) }
    }
    if (pagerState.isScrollInProgress) {
        launch { pagerState.stopScroll(MutatePriority.PreventUserInput) }
    }
}
