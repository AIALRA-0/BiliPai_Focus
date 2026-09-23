package com.android.purebilibili.feature.video.screen

import android.content.Context
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackBlurFrame
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.shouldAllowRuntimeShaderBackedHazeEffect
import com.android.purebilibili.core.ui.rememberAppChevronUpIcon
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.feature.video.share.VideoSharePayload
import com.android.purebilibili.feature.video.share.buildVideoSharePayload
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.ui.components.BottomInputBar
import com.android.purebilibili.feature.video.ui.components.resolveBottomInputBarContentBottomPadding
import com.android.purebilibili.feature.video.ui.components.shouldUseFloatingLiquidBottomInputBar
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale
import com.android.purebilibili.feature.video.usecase.seekPlayerFromUserAction
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.withEngagementUiState
import com.android.purebilibili.feature.video.player.PlaylistItem
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.ui.blur.hazeEffectCompat
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun VideoDetailPhoneSuccessContentLayer(
    success: VideoPlaybackUiState.Success,
    introListState: LazyListState,
    commentListState: LazyListState,
    videoContentPagerState: PagerState,
    commentState: CommentUiState,
    engagementState: VideoEngagementUiState,
    androidNativeLiquidGlassEnabled: Boolean,
    commentMemberDecorationsEnabled: Boolean,
    playbackActions: VideoDetailPlaybackActions,
    engagementActions: VideoDetailEngagementActions,
    commentActions: VideoDetailCommentActions,
    context: Context,
    sortPreferenceScope: CoroutineScope,
    playerState: VideoPlayerState,
    motionSpec: VideoDetailMotionSpec,
    hazeState: HazeState,
    isTransitionFinished: Boolean,
    isLeaving: Boolean,
    rootTransitionOwnsContentAlpha: Boolean,
    keepContentVisibleAfterBackPreview: Boolean = false,
    shouldShowExternalPlaylistQueueBar: Boolean,
    selectedVideoContentTabIndex: Int,
    useTabletLayout: Boolean,
    isFullscreenMode: Boolean,
    isPortraitFullscreen: Boolean,
    showCommentInput: Boolean,
    isCommentThreadVisible: Boolean,
    showFavoriteFolderDialog: Boolean,
    downloadProgress: Float,
    danmakuEnabledForDetail: Boolean,
    isQuickReturnLimitedForSharedElements: Boolean,
    transitionEnabled: Boolean,
    sourceRouteForSharedElement: String?,
    isPlayerCollapsed: Boolean,
    onBgmClick: (BgmInfo) -> Unit,
    homeUpBadgesVisible: Boolean,
    isVideoPlaying: Boolean,
    onSelectedTabChange: (Int) -> Unit,
    onIntroScrollThresholdChange: (Boolean) -> Unit,
    openFavoriteFolders: (VideoFavoriteEntryPoint) -> Unit,
    navigateToUserSpaceFromVideo: (Long) -> Unit,
    navigateToRelatedVideo: (String, android.os.Bundle?) -> Unit,
    openCommentUrl: (String) -> Unit,
    onSearchKeywordClick: (String) -> Unit,
    onOpenBilibiliLink: ((String) -> Unit)?,
    onShareVideo: (VideoSharePayload) -> Unit,
    externalPlaylistQueueTitle: String,
    playlistItems: List<PlaylistItem>,
    onShowExternalPlaylistQueueSheet: () -> Unit,
    commentThreadCoveredBlurProgress: Float = 0f,
) {
    val engagementSuccess = success.withEngagementUiState(engagementState)
    val danmakuManager = rememberDanmakuManager(success.info.bvid)
    // Android 16 ART 曾拒绝校验 VideoDetailScreen 中捕获过多状态的匿名 Compose lambda。
    // 保持这个成功态为命名边界，避免 R8/Compose 再生成单个超大内容块。
    key(success.info.bvid) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val detailContentRevealEnter = fadeIn(
                    tween(
                        motionSpec.contentRevealFadeDurationMillis,
                        easing = com.android.purebilibili.core.ui.motion.AppMotionEasing.Continuity
                    )
                )
                val detailContentExitFade = fadeOut(
                    tween(
                        durationMillis = 180,
                        delayMillis = 60,
                        easing = com.android.purebilibili.core.ui.motion.AppMotionEasing.Continuity
                    )
                )
                AnimatedVisibility(
                    visible = shouldShowVideoDetailContent(
                        isTransitionFinished = isTransitionFinished,
                        isLeaving = isLeaving,
                        rootTransitionOwnsContentAlpha = rootTransitionOwnsContentAlpha,
                        keepContentVisibleAfterBackPreview = keepContentVisibleAfterBackPreview,
                    ),
                    enter = if (rootTransitionOwnsContentAlpha) {
                        EnterTransition.None
                    } else {
                        detailContentRevealEnter
                    },
                    exit = if (rootTransitionOwnsContentAlpha) {
                        ExitTransition.None
                    } else {
                        detailContentExitFade
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val floatingLiquidBottomInputBar = shouldUseFloatingLiquidBottomInputBar(
                            androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
                        )
                        // Capture scrolling detail content only; BottomInputBar stays outside
                        // the source so neither blur nor backdrop samples the bar itself.
                        val bottomInputBarBackdrop = if (floatingLiquidBottomInputBar) {
                            rememberLayerBackdrop()
                        } else {
                            null
                        }
                        val showExternalPlaylistQueueBarOnCurrentTab =
                            shouldShowExternalPlaylistQueueBarOnContentTab(
                                queueAvailable = shouldShowExternalPlaylistQueueBar,
                                selectedTabIndex = selectedVideoContentTabIndex
                            )
                        val showFrozenCommentBar = shouldShowVideoDetailBottomInteractionBar(
                            useTabletLayout = useTabletLayout,
                            selectedTabIndex = selectedVideoContentTabIndex,
                            isFullscreenMode = isFullscreenMode,
                            isPortraitFullscreen = isPortraitFullscreen,
                            isCommentInputVisible = showCommentInput,
                            isCommentThreadVisible = isCommentThreadVisible,
                            isFavoriteFolderDialogVisible = showFavoriteFolderDialog,
                            isExternalPlaylistQueueBarVisible = showExternalPlaylistQueueBarOnCurrentTab
                        )
                        val showInteractionActions = shouldShowVideoDetailActionButtons()
                        val videoContentBottomPadding = resolveBottomInputBarContentBottomPadding(
                            showBar = showFrozenCommentBar,
                            floatingLiquidGlass = floatingLiquidBottomInputBar,
                            showActionButtonsFallback = showInteractionActions
                        )
                        val currentPageIndex = success.info.pages
                            .indexOfFirst { it.cid == success.info.cid }
                            .coerceAtLeast(0)

                        val coveredBlurProgress = if (isCommentThreadVisible) commentThreadCoveredBlurProgress else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (floatingLiquidBottomInputBar && bottomInputBarBackdrop != null) {
                                        Modifier.layerBackdrop(bottomInputBarBackdrop)
                                    } else {
                                        Modifier
                                    }
                                )
                                .hazeSourceCompat(hazeState)
                                .graphicsLayer {
                                    renderEffect = null
                                    if (coveredBlurProgress > 0f &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                    ) {
                                        val blurFrame = resolvePredictiveBackBlurFrame(
                                            progress = coveredBlurProgress,
                                        )
                                        renderEffect = if (blurFrame.blurRadiusPx > 0.5f) {
                                            AndroidRenderEffect.createBlurEffect(
                                                blurFrame.blurRadiusPx,
                                                blurFrame.blurRadiusPx,
                                                Shader.TileMode.CLAMP,
                                            ).asComposeRenderEffect()
                                        } else {
                                            null
                                        }
                                    }
                                }
                        ) {
                            VideoContentSection(
                                data = VideoContentData(
                                    info = engagementSuccess.info,
                                    introListState = introListState,
                                    commentListState = commentListState,
                                    pagerState = videoContentPagerState,
                                    relatedVideos = success.related,
                                    replies = commentState.replies,
                                    replyCount = commentState.replyCount,
                                    emoteMap = success.emoteMap,
                                    followingMids = engagementState.followingMids,
                                    videoTags = success.videoTags,
                                    bgmInfo = success.bgmInfo,
                                    bgmInfoList = success.bgmInfoList,
                                ),
                                engagementState = VideoContentEngagementState(
                                    isLoggedIn = success.isLoggedIn,
                                    isFollowing = engagementState.isFollowing,
                                    isFavorited = engagementState.isFavorited,
                                    isLiked = engagementState.isLiked,
                                    coinCount = engagementState.coinCount,
                                    currentPageIndex = currentPageIndex,
                                    downloadProgress = downloadProgress,
                                    isInWatchLater = engagementState.isInWatchLater,
                                ),
                                commentState = VideoContentCommentState(
                                    isRepliesLoading = commentState.isRepliesLoading,
                                    isRepliesEnd = commentState.isRepliesEnd,
                                    sortMode = commentState.sortMode,
                                    currentMid = commentState.currentMid,
                                    showUpFlag = commentState.showUpFlag,
                                    showIdentityDecorations = commentMemberDecorationsEnabled,
                                    dissolvingIds = commentState.dissolvingIds,
                                    likedComments = commentState.likedComments,
                                    hatedComments = commentState.hatedComments,
                                ),
                                noteState = VideoContentNoteState(
                                    aiSummary = success.aiSummary,
                                    aiSummaryPrompt = success.aiSummaryPrompt,
                                    videoNoteState = success.videoNoteState,
                                ),
                                presentationState = VideoContentPresentationState(
                                    danmakuEnabled = danmakuEnabledForDetail,
                                    transitionEnabled = transitionEnabled,
                                    isQuickReturnLimitedForSharedElements = isQuickReturnLimitedForSharedElements,
                                    sourceRouteForSharedElement = sourceRouteForSharedElement,
                                    isPlayerCollapsed = isPlayerCollapsed,
                                    onlineCount = success.onlineCount,
                                    showOnlineCount = true,
                                    ownerFollowerCount = success.ownerFollowerCount,
                                    ownerVideoCount = success.ownerVideoCount,
                                    showUpBadge = homeUpBadgesVisible,
                                    showInteractionActions = showInteractionActions,
                                    isVideoPlaying = isVideoPlaying,
                                    bottomContentPadding = videoContentBottomPadding,
                                ),
                                primaryActions = VideoContentPrimaryActions(
                                    onFollowClick = engagementActions.toggleFollow,
                                    onFavoriteClick = { openFavoriteFolders(VideoFavoriteEntryPoint.DetailActionRow) },
                                    onLikeClick = engagementActions.toggleLike,
                                    onCoinClick = engagementActions.openCoinDialog,
                                    onTripleClick = engagementActions.doTripleAction,
                                    onPageSelect = playbackActions.switchPage,
                                    onUpClick = navigateToUserSpaceFromVideo,
                                    onRelatedVideoClick = navigateToRelatedVideo,
                                    onDownloadClick = playbackActions.openDownloadDialog,
                                    onWatchLaterClick = engagementActions.toggleWatchLater,
                                    onShareClick = {
                                        onShareVideo(buildVideoSharePayload(
                                            title = success.info.title,
                                            bvid = success.info.bvid,
                                            coverUrl = success.info.pic,
                                        ))
                                    },
                                    onTimestampClick = { positionMs -> seekPlayerFromUserAction(playerState.player, positionMs) },
                                    onDanmakuSendClick = {
                                        android.util.Log.d("VideoDetailScreen", "Danmaku send clicked")
                                        playbackActions.showDanmakuSendDialog()
                                    },
                                    onDanmakuToggle = {
                                        val newValue = !danmakuEnabledForDetail
                                        danmakuManager.isEnabled = newValue
                                        if (!newValue) danmakuManager.clear()
                                        sortPreferenceScope.launch {
                                            com.android.purebilibili.core.store.SettingsManager.setDanmakuEnabled(
                                                context,
                                                newValue,
                                                com.android.purebilibili.core.store.DanmakuSettingsScope.PORTRAIT,
                                            )
                                        }
                                    },
                                    onFavoriteLongClick = playbackActions.showFavoriteFolderDialog,
                                    onBgmClick = onBgmClick,
                                ),
                                commentActions = VideoContentCommentActions(
                                    onSortModeChange = { mode ->
                                        commentActions.setSortMode(mode)
                                        sortPreferenceScope.launch {
                                            com.android.purebilibili.core.store.SettingsManager.setCommentDefaultSortMode(context, mode.apiMode)
                                        }
                                    },
                                    onSubReplyClick = commentActions.openSubReply,
                                    onCommentReplyClick = playbackActions.replyTo,
                                    onLoadMoreReplies = commentActions.loadComments,
                                    onDeleteComment = commentActions.deleteComment,
                                    onDissolveStart = commentActions.startDissolve,
                                    onCommentLike = commentActions.likeComment,
                                    onCommentHate = commentActions.hateComment,
                                    onCommentUrlClick = openCommentUrl,
                                    onDescriptionUrlClick = onOpenBilibiliLink,
                                    onSearchKeywordClick = onSearchKeywordClick,
                                    onReportComment = commentActions.reportComment,
                                    onToggleTopComment = commentActions.toggleTopComment,
                                    onCheckCommentFraud = commentActions.checkCommentFraud,
                                ),
                                noteActions = VideoContentNoteActions(
                                    onRetryAiSummary = playbackActions.retryAiSummary,
                                    onCreateNoteDraftFromAiSummary = playbackActions.createVideoNoteDraftFromAiSummary,
                                    onOpenVideoNoteEditor = playbackActions.openVideoNoteEditor,
                                    onCloseVideoNoteEditor = playbackActions.closeVideoNoteEditor,
                                    onVideoNoteDocumentChange = playbackActions.updateVideoNoteEditorDocument,
                                    onInsertVideoNoteTimestamp = playbackActions.insertCurrentPlaybackTimestampIntoNote,
                                    onVideoNoteTimestampClick = playbackActions.seekTo,
                                    onSaveVideoNote = playbackActions.saveVideoNote,
                                    onDeleteVideoNote = playbackActions.deleteVideoNote,
                                    onRetryVideoNote = playbackActions.retryVideoNote,
                                    onPublicVideoNoteClick = { _, url -> if (url.isNotBlank()) onOpenBilibiliLink?.invoke(url) },
                                ),
                                uiActions = VideoContentUiActions(
                                    onSelectedTabChange = onSelectedTabChange,
                                    onIntroScrollThresholdChange = onIntroScrollThresholdChange,
                                    onCommentScrollStateChange = { _, _ -> },
                                ),
                            )
                        }

                        if (showFrozenCommentBar) {
                            BottomInputBar(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                isLiked = engagementState.isLiked,
                                isFavorited = engagementState.isFavorited,
                                isCoined = engagementState.coinCount > 0,
                                onLikeClick = engagementActions.toggleLike,
                                onFavoriteClick = {
                                    openFavoriteFolders(VideoFavoriteEntryPoint.BottomInputBar)
                                },
                                onCoinClick = engagementActions.openCoinDialog,
                                onShareClick = {
                                    onShareVideo(
                                        buildVideoSharePayload(
                                            title = success.info.title,
                                            bvid = success.info.bvid,
                                            coverUrl = success.info.pic
                                        )
                                    )
                                },
                                onCommentClick = {
                                    android.util.Log.d("VideoDetailScreen", "Comment input clicked")
                                    playbackActions.openRootCommentComposer()
                                },
                                backdrop = if (floatingLiquidBottomInputBar) {
                                    bottomInputBarBackdrop
                                } else {
                                    null
                                },
                                hazeState = hazeState,
                                isScrollInProgressProvider = {
                                    introListState.isScrollInProgress ||
                                        commentListState.isScrollInProgress ||
                                        videoContentPagerState.isScrollInProgress
                                },
                                scrollPositionProvider = {
                                    val state = if (videoContentPagerState.currentPage == 0) {
                                        introListState
                                    } else {
                                        commentListState
                                    }
                                    state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
                                },
                            )
                        }

                        if (showExternalPlaylistQueueBarOnCurrentTab) {
                            ExternalPlaylistQueueCollapsedBar(
                                title = externalPlaylistQueueTitle,
                                videoCount = playlistItems.size,
                                onClick = onShowExternalPlaylistQueueSheet,
                                hazeState = hazeState,
                                liquidGlassEnabled = floatingLiquidBottomInputBar,
                                backdrop = if (floatingLiquidBottomInputBar) bottomInputBarBackdrop else null,
                                isScrollInProgressProvider = {
                                    introListState.isScrollInProgress ||
                                        commentListState.isScrollInProgress ||
                                        videoContentPagerState.isScrollInProgress
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = 8.dp,
                                        bottom = 24.dp,
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalPlaylistQueueCollapsedBar(
    title: String,
    videoCount: Int,
    onClick: () -> Unit,
    hazeState: HazeState,
    liquidGlassEnabled: Boolean,
    backdrop: top.yukonga.miuix.kmp.blur.Backdrop?,
    isScrollInProgressProvider: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val shape = AppShapes.borderedContainer(ContainerLevel.Dialog)
    if (liquidGlassEnabled) {
        BottomBarMatchedReusableLiquidDock(
            shape = shape,
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp),
            backdrop = backdrop,
            reuseEnabled = true,
            drawShellLens = true,
            shellLensIntensity = resolveFloatingDockGeometryScale(64f),
            isScrollInProgressProvider = isScrollInProgressProvider,
        ) {
            ExternalPlaylistQueueCollapsedBarContent(
                title = title,
                videoCount = videoCount,
                onClick = onClick,
            )
        }
        return
    }
    val useHazeEffect = shouldAllowRuntimeShaderBackedHazeEffect(Build.VERSION.SDK_INT)
    AppSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            // 先 clip 再 haze：否则模糊层按矩形绘制，左/右端会露出直角（与展开 sheet 一致）。
            .clip(shape)
            .then(
                if (useHazeEffect) {
                    Modifier.hazeEffectCompat(
                        state = hazeState,
                        style = HazeMaterials.ultraThin()
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        shape = shape,
        color = AppSurfaceTokens.cardContainer().copy(alpha = 0.74f),
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        ExternalPlaylistQueueCollapsedBarContent(
            title = title,
            videoCount = videoCount,
            onClick = onClick,
        )
    }
}

@Composable
private fun ExternalPlaylistQueueCollapsedBarContent(
    title: String,
    videoCount: Int,
    onClick: () -> Unit,
) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            AppText(
                text = "${videoCount}个视频",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            AppIcon(
                imageVector = rememberAppChevronUpIcon(),
                contentDescription = "展开${title}队列",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
}
