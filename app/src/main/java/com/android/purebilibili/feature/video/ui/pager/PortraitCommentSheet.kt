package com.android.purebilibili.feature.video.ui.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.video.ui.components.VideoCommentSheetHost
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel

@Composable
fun PortraitCommentSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onVisibilityProgressChange: (Float) -> Unit = {},
    commentViewModel: VideoCommentViewModel,
    aid: Long,
    upMid: Long = 0,
    expectedReplyCount: Int = 0,
    emoteMap: Map<String, String> = emptyMap(),
    maxTimestampMs: Long? = null,
    onRootCommentClick: () -> Unit = {},
    onReplyClick: (ReplyItem) -> Unit = {},
    onUserClick: (Long) -> Unit
) {
    val subReplyState by commentViewModel.subReplyState.collectAsState()
    val hostMainSheetVisible = resolvePortraitCommentHostMainSheetVisible(
        commentSheetVisible = visible,
        subReplyVisible = subReplyState.visible
    )

    VideoCommentSheetHost(
        mainSheetVisible = hostMainSheetVisible,
        onDismiss = onDismiss,
        onMainSheetVisibilityProgressChange = onVisibilityProgressChange,
        commentViewModel = commentViewModel,
        aid = aid,
        upMid = upMid,
        expectedReplyCount = expectedReplyCount,
        emoteMap = emoteMap,
        maxTimestampMs = maxTimestampMs,
        onRootCommentClick = onRootCommentClick,
        onReplyClick = onReplyClick,
        onUserClick = onUserClick
    )
}
