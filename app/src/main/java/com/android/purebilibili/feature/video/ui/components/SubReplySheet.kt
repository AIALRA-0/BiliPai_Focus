package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubReplySheet(
    state: SubReplyUiState,
    showUpFlag: Boolean = false,
    emoteMap: Map<String, String>,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    maxHeightFraction: Float = 1f,
    scrimAlpha: Float = 0.32f,
    onTimestampClick: ((Long) -> Unit)? = null,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)? = null,
    onReplyClick: ((ReplyItem) -> Unit)? = null,
    // [新增] 删除评论相关
    currentMid: Long = 0,
    onDissolveStart: ((Long) -> Unit)? = null,
    onDeleteComment: ((Long) -> Unit)? = null,
    // [新增] 点赞
    onCommentLike: ((Long) -> Unit)? = null,
    likedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null
) {
    if (state.visible && state.rootReply != null) {
        val rootReply = state.rootReply
        com.android.purebilibili.core.ui.IOSModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxHeight(maxHeightFraction),
            scrimColor = Color.Black.copy(alpha = scrimAlpha)
        ) {
            SubReplyDetailContent(
                rootReply = rootReply,
                subReplies = state.items,
                isLoading = state.isLoading,
                isEnd = state.isEnd,
                emoteMap = emoteMap,
                onLoadMore = onLoadMore,
                onTimestampClick = onTimestampClick,
                upMid = state.upMid,
                showUpFlag = showUpFlag,
                onImagePreview = onImagePreview,
                onReplyClick = onReplyClick,
                // [新增] 消散动画相关
                dissolvingIds = state.dissolvingIds,
                currentMid = currentMid,
                onDissolveStart = onDissolveStart,
                onDeleteComment = onDeleteComment,
                onCommentLike = onCommentLike,
                likedComments = likedComments,
                onUrlClick = onUrlClick,
                onAvatarClick = onAvatarClick
            )
        }
    }
}
