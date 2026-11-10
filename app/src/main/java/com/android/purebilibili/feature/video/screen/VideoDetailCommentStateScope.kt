package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState

/** Read full comment payloads only after the comment surface is mounted. */
@Composable
internal fun VideoDetailCommentStateScope(
    comments: State<CommentUiState>,
    thread: State<SubReplyUiState>,
    content: @Composable (CommentUiState, SubReplyUiState) -> Unit,
) {
    content(comments.value, thread.value)
}
