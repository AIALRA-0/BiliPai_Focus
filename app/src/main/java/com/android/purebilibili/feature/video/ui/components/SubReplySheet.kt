package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.CommentWindowNavigation
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.feature.video.viewmodel.SubReplySortMode
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubReplySheet(
    state: SubReplyUiState,
    showUpFlag: Boolean = false,
    emoteMap: Map<String, String>,
    onDismiss: () -> Unit,
    onRootCommentClick: (() -> Unit)? = null,
    onLoadMore: () -> Unit,
    onSortModeChange: (SubReplySortMode) -> Unit,
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
    onCommentHate: ((Long) -> Unit)? = null,
    hatedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    showIdentityDecorations: Boolean = true,
    onAvatarClick: ((String) -> Unit)? = null,
    onCoveredBlurProgressChange: ((Float) -> Unit)? = null,
) {
    if (state.visible && state.rootReply != null) {
        val rootReply = state.rootReply
        val backState = rememberNavigationEventState(NavigationEventInfo.None)
        val transition = backState.transitionState as? NavigationEventTransitionState.InProgress
        val backProgress by animateFloatAsState(
            targetValue = transition?.latestEvent?.progress ?: 0f,
            animationSpec = if (transition != null) snap() else tween(180),
            label = "standalone_comment_predictive_back",
        )
        val threadDrag = rememberCommentThreadDrag(
            visible = state.visible,
            rootReplyId = rootReply.rpid,
            onDismiss = onDismiss,
        )
        val coveredBlurProgress = resolveCommentThreadCoveredBlurProgress(
            maxOf(backProgress, threadDrag.revealProgress)
        )
        SideEffect {
            onCoveredBlurProgressChange?.invoke(coveredBlurProgress)
        }
        DisposableEffect(Unit) {
            onDispose {
                onCoveredBlurProgressChange?.invoke(0f)
            }
        }
        com.android.purebilibili.core.ui.AppModalBottomSheet(
            onDismissRequest = onDismiss,
            dismissOnBackPress = false,
            modifier = Modifier.fillMaxHeight(maxHeightFraction)
                .then(threadDrag.containerModifier)
                .graphicsLayer {
                    translationY = threadDrag.offsetPx.value +
                        resolveCommentThreadPredictiveBackOffsetY(backProgress, size.height)
                },
            scrimColor = Color.Black.copy(alpha = scrimAlpha * (1f - maxOf(backProgress, threadDrag.revealProgress)))
        ) {
            CommentWindowNavigation {
                NavigationBackHandler(
                    state = backState,
                    isBackEnabled = true,
                    onBackCompleted = onDismiss,
                )
                SubReplyDetailContent(
                    headerDragModifier = threadDrag.headerModifier,
                    rootReply = rootReply,
                    subReplies = state.items,
                    sortMode = state.sortMode,
                    error = state.error,
                    onSortModeChange = onSortModeChange,
                    remoteReplyCount = state.totalCount,
                    isLoading = state.isLoading,
                    isEnd = state.isEnd,
                    emoteMap = emoteMap,
                    onLoadMore = onLoadMore,
                    onDismiss = onDismiss,
                    onRootCommentClick = onRootCommentClick,
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
                    onCommentHate = onCommentHate,
                    hatedComments = hatedComments,
                    onUrlClick = onUrlClick,
                    showIdentityDecorations = showIdentityDecorations,
                    onAvatarClick = onAvatarClick,
                    targetReplyId = state.targetReplyId
                )
            }
        }
    }
}
