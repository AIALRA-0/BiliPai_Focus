package com.android.purebilibili.feature.video.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.BilibiliUrlParser
import com.android.purebilibili.data.model.CommentFraudStatus
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.feature.video.screen.shouldOpenCommentUrlInApp
import com.android.purebilibili.feature.video.ui.pager.resolveVideoSubReplySheetMaxHeightFraction
import com.android.purebilibili.feature.video.ui.pager.resolveVideoSubReplySheetScrimAlpha
import com.android.purebilibili.feature.video.ui.pager.shouldOpenPortraitCommentReplyComposer
import com.android.purebilibili.feature.video.ui.pager.shouldOpenPortraitCommentThreadDetail
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import kotlinx.coroutines.launch

private const val MAIN_COMMENT_SHEET_HEIGHT_FRACTION = 0.60f
private const val MAIN_COMMENT_SHEET_SCRIM_ALPHA = 0.5f

internal enum class VideoCommentSheetHostContent {
    HIDDEN,
    MAIN_LIST,
    THREAD_DETAIL
}

internal fun resolveVideoCommentSheetHostContent(
    mainSheetVisible: Boolean,
    subReplyVisible: Boolean
): VideoCommentSheetHostContent {
    return when {
        subReplyVisible -> VideoCommentSheetHostContent.THREAD_DETAIL
        mainSheetVisible -> VideoCommentSheetHostContent.MAIN_LIST
        else -> VideoCommentSheetHostContent.HIDDEN
    }
}

internal fun resolveVideoCommentSheetHostHeightFraction(
    mainSheetVisible: Boolean,
    screenHeightPx: Int = 0,
    topReservedPx: Int = 0
): Float {
    return if (mainSheetVisible) {
        MAIN_COMMENT_SHEET_HEIGHT_FRACTION
    } else {
        resolveVideoSubReplySheetMaxHeightFraction(
            screenHeightPx = screenHeightPx,
            topReservedPx = topReservedPx
        )
    }
}

internal fun resolveVideoCommentSheetHostScrimAlpha(
    mainSheetVisible: Boolean
): Float {
    return if (mainSheetVisible) {
        MAIN_COMMENT_SHEET_SCRIM_ALPHA
    } else {
        resolveVideoSubReplySheetScrimAlpha()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun VideoCommentSheetHost(
    mainSheetVisible: Boolean,
    onDismiss: () -> Unit,
    commentViewModel: VideoCommentViewModel,
    aid: Long,
    upMid: Long = 0,
    expectedReplyCount: Int = 0,
    emoteMap: Map<String, String> = emptyMap(),
    onRootCommentClick: () -> Unit = {},
    onReplyClick: (ReplyItem) -> Unit = {},
    onUserClick: (Long) -> Unit,
    screenHeightPx: Int = 0,
    topReservedPx: Int = 0,
    onTimestampClick: ((Long) -> Unit)? = null,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val commentState by commentViewModel.commentState.collectAsState()
    val subReplyState by commentViewModel.subReplyState.collectAsState()
    val defaultSortMode by com.android.purebilibili.core.store.SettingsManager
        .getCommentDefaultSortMode(context)
        .collectAsState(
            initial = com.android.purebilibili.core.store.SettingsManager.getCommentDefaultSortModeSync(context)
        )
    val preferredSortMode = remember(defaultSortMode) {
        CommentSortMode.fromApiMode(defaultSortMode)
    }
    val hostContent = resolveVideoCommentSheetHostContent(
        mainSheetVisible = mainSheetVisible,
        subReplyVisible = subReplyState.visible
    )
    val hostVisible = hostContent != VideoCommentSheetHostContent.HIDDEN
    val sheetHeightFraction = resolveVideoCommentSheetHostHeightFraction(
        mainSheetVisible = mainSheetVisible,
        screenHeightPx = screenHeightPx,
        topReservedPx = topReservedPx
    )
    val scrimAlpha = resolveVideoCommentSheetHostScrimAlpha(mainSheetVisible = mainSheetVisible)
    val appearance = rememberVideoCommentAppearance()

    var fallbackPreviewVisible by remember { mutableStateOf(false) }
    var fallbackPreviewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var fallbackPreviewIndex by remember { mutableIntStateOf(0) }
    var fallbackPreviewSourceRect by remember { mutableStateOf<Rect?>(null) }
    var fallbackPreviewTextContent by remember { mutableStateOf<ImagePreviewTextContent?>(null) }

    val previewCallback: (List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit =
        onImagePreview ?: { images, index, rect, textContent ->
            fallbackPreviewImages = images
            fallbackPreviewIndex = index
            fallbackPreviewSourceRect = rect
            fallbackPreviewTextContent = textContent
            fallbackPreviewVisible = true
        }

    if (fallbackPreviewVisible && fallbackPreviewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = fallbackPreviewImages,
            initialIndex = fallbackPreviewIndex,
            sourceRect = fallbackPreviewSourceRect,
            textContent = fallbackPreviewTextContent,
            onDismiss = {
                fallbackPreviewVisible = false
                fallbackPreviewTextContent = null
            }
        )
    }

    BackHandler(enabled = hostVisible) {
        if (subReplyState.visible) {
            commentViewModel.closeSubReply()
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(aid, mainSheetVisible, preferredSortMode, upMid, expectedReplyCount) {
        if (mainSheetVisible) {
            commentViewModel.init(
                aid = aid,
                upMid = upMid,
                preferredSortMode = preferredSortMode,
                expectedReplyCount = expectedReplyCount
            )
        }
    }

    var fraudDialogStatus by remember { mutableStateOf<CommentFraudStatus?>(null) }
    LaunchedEffect(Unit) {
        commentViewModel.fraudEvent.collect { status ->
            if (status != CommentFraudStatus.NORMAL) {
                fraudDialogStatus = status
            }
        }
    }

    fraudDialogStatus?.let { status ->
        CommentFraudResultDialog(
            status = status,
            onDismiss = {
                fraudDialogStatus = null
                commentViewModel.dismissFraudResult()
            },
            onDeleteComment = if (status == CommentFraudStatus.SHADOW_BANNED) {
                {
                    val rpid = commentViewModel.commentState.value.fraudDetectRpid
                    if (rpid > 0) {
                        commentViewModel.startDissolve(rpid)
                    }
                }
            } else null
        )
    }

    val openCommentUrl: (String) -> Unit = openCommentUrl@{ rawUrl ->
        val url = rawUrl.trim()
        if (url.isEmpty()) return@openCommentUrl

        val parsedResult = BilibiliUrlParser.parse(url)
        if (parsedResult.bvid != null && shouldOpenCommentUrlInApp(url)) {
            val inAppIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            ).setPackage(context.packageName)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            val launchedInApp = runCatching { context.startActivity(inAppIntent) }.isSuccess
            if (launchedInApp) return@openCommentUrl
        }

        if (shouldOpenCommentUrlInApp(url)) {
            val inAppIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            ).setPackage(context.packageName)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            val launchedInApp = runCatching { context.startActivity(inAppIntent) }.isSuccess
            if (launchedInApp) return@openCommentUrl
        }

        runCatching { uriHandler.openUri(url) }
    }

    AnimatedVisibility(
        visible = hostVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = hostVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(sheetHeightFraction)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    color = appearance.panelColor
                ) {
                    when (hostContent) {
                        VideoCommentSheetHostContent.MAIN_LIST -> {
                            VideoCommentMainList(
                                viewModel = commentViewModel,
                                onRootCommentClick = onRootCommentClick,
                                onReplyClick = onReplyClick,
                                onUserClick = onUserClick,
                                onCommentUrlClick = openCommentUrl,
                                onTimestampClick = onTimestampClick,
                                onImagePreview = previewCallback
                            )
                        }

                        VideoCommentSheetHostContent.THREAD_DETAIL -> {
                            val rootReply = subReplyState.rootReply
                            if (rootReply != null) {
                                SubReplyDetailContent(
                                    rootReply = rootReply,
                                    subReplies = subReplyState.items,
                                    isLoading = subReplyState.isLoading,
                                    isEnd = subReplyState.isEnd,
                                    emoteMap = emoteMap,
                                    onLoadMore = { commentViewModel.loadMoreSubReplies() },
                                    onDismiss = { commentViewModel.closeSubReply() },
                                    onRootCommentClick = onRootCommentClick,
                                    onTimestampClick = onTimestampClick,
                                    upMid = subReplyState.upMid,
                                    showUpFlag = commentState.showUpFlag,
                                    onImagePreview = previewCallback,
                                    onReplyClick = onReplyClick,
                                    dissolvingIds = subReplyState.dissolvingIds,
                                    currentMid = commentState.currentMid,
                                    onDissolveStart = { rpid -> commentViewModel.startSubDissolve(rpid) },
                                    onDeleteComment = { rpid -> commentViewModel.deleteSubComment(rpid) },
                                    onCommentLike = commentViewModel::likeComment,
                                    likedComments = commentState.likedComments,
                                    onUrlClick = openCommentUrl,
                                    onAvatarClick = { mid ->
                                        mid.toLongOrNull()?.let(onUserClick)
                                    }
                                )
                            }
                        }

                        VideoCommentSheetHostContent.HIDDEN -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCommentMainList(
    viewModel: VideoCommentViewModel,
    onRootCommentClick: () -> Unit,
    onReplyClick: (ReplyItem) -> Unit,
    onUserClick: (Long) -> Unit,
    onCommentUrlClick: (String) -> Unit,
    onTimestampClick: ((Long) -> Unit)?,
    onImagePreview: (List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit
) {
    val state by viewModel.commentState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appearance = rememberVideoCommentAppearance()

    Column(modifier = Modifier.fillMaxSize()) {
        CommentSortFilterBar(
            count = state.replyCount,
            sortMode = state.sortMode,
            onSortModeChange = { mode ->
                viewModel.setSortMode(mode)
                scope.launch {
                    com.android.purebilibili.core.store.SettingsManager
                        .setCommentDefaultSortMode(context, mode.apiMode)
                }
            },
            upOnly = state.upOnlyFilter,
            onUpOnlyToggle = { viewModel.toggleUpOnly() }
        )

        CommentFraudDetectingBanner(isDetecting = state.isDetectingFraud)

        if (state.isRepliesLoading && state.replies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = appearance.composerHintBackgroundColor,
                        shape = RoundedCornerShape(16.dp),
                        onClick = onRootCommentClick
                    ) {
                        Text(
                            text = "说点什么，直接评论 UP 主和大家",
                            color = appearance.secondaryTextColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                items(
                    items = state.replies,
                    key = { it.rpid },
                    contentType = { resolveReplyItemContentType(it) }
                ) { reply ->
                    ReplyItemView(
                        item = reply,
                        upMid = state.upMid,
                        showUpFlag = state.showUpFlag,
                        isPinned = false,
                        onClick = {},
                        onSubClick = { parentReply ->
                            if (shouldOpenPortraitCommentThreadDetail(useEmbeddedPresentation = true)) {
                                viewModel.openSubReply(parentReply)
                            }
                        },
                        onTimestampClick = onTimestampClick,
                        onImagePreview = onImagePreview,
                        onLikeClick = { viewModel.likeComment(reply.rpid) },
                        onReplyClick = {
                            if (shouldOpenPortraitCommentReplyComposer()) {
                                onReplyClick(reply)
                            }
                        },
                        onUrlClick = onCommentUrlClick,
                        onAvatarClick = { mid -> mid.toLongOrNull()?.let(onUserClick) ?: Unit }
                    )
                }

                item {
                    if (!state.isRepliesEnd) {
                        LaunchedEffect(Unit) {
                            viewModel.loadComments()
                        }
                        LoadingFooter()
                    } else {
                        NoMoreFooter()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp)
    }
}

@Composable
private fun NoMoreFooter() {
    val appearance = rememberVideoCommentAppearance()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "没有更多了",
            color = appearance.secondaryTextColor,
            fontWeight = FontWeight.Normal
        )
    }
}
