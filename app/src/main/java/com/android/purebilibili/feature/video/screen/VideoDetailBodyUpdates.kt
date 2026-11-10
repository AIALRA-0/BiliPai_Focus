package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import com.android.purebilibili.feature.video.viewmodel.CommentUiState

/** 正文只接收评论标题投影与下载进度；列表须另外传原始评论句柄。 */
@Composable
internal fun VideoDetailBodyUpdates(
    comments: State<CommentUiState>,
    downloadProgress: State<Float>,
    content: @Composable (CommentUiState, Float) -> Unit,
) {
    // 正文壳只需要计数身份与排序；回复、分页和点赞集合只由评论页消费。
    val chrome = remember(comments) {
        derivedStateOf(structuralEqualityPolicy()) {
            val state = comments.value
            CommentUiState(
                replyCount = state.replyCount,
                replyCountSubject = state.replyCountSubject,
                replyCountIsAuthoritative = state.replyCountIsAuthoritative,
                sortMode = state.sortMode,
            )
        }
    }
    content(chrome.value, downloadProgress.value)
}

/** 独立的评论页重组边界；可选句柄兼容已有大屏与独立评论调用。 */
@Composable
internal fun VideoDetailCommentUpdates(
    comments: State<CommentUiState>?,
    content: @Composable (CommentUiState?) -> Unit,
) {
    content(comments?.value)
}
