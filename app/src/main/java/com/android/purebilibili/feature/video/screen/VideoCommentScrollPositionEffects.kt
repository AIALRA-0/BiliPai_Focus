package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import kotlinx.coroutines.flow.distinctUntilChanged

/** 排序是两个详情列表的共同内容身份；隐藏的模式也不能继续保留旧排序的锚点。 */
@Composable
internal fun VideoCommentScrollPositionEffects(
    commentState: State<CommentUiState>,
    inlineList: LazyListState,
    landscapeList: LazyListState,
) {
    LaunchedEffect(commentState, inlineList, landscapeList) {
        var previousSort = commentState.value.sortMode
        snapshotFlow { commentState.value.sortMode }.distinctUntilChanged().collect { sort ->
            if (sort != previousSort) {
                inlineList.requestScrollToItem(0)
                landscapeList.requestScrollToItem(0)
                previousSort = sort
            }
        }
    }
}
