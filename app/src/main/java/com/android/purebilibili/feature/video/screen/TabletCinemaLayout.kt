package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.feature.video.usecase.seekPlayerFromUserAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TabletCommentPanelWidthPreset
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.util.ShareUtils
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.data.model.response.ViewPoint
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.note.VideoNoteEditorDocument
import com.android.purebilibili.feature.video.note.buildVideoNoteShareText
import com.android.purebilibili.feature.video.note.shouldShowVideoNoteCard
import com.android.purebilibili.feature.video.progress.PbpProgressData
import com.android.purebilibili.feature.video.ui.components.CommentSortFilterBar
import com.android.purebilibili.feature.video.ui.components.CollectionRow
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
import com.android.purebilibili.feature.video.ui.components.RelatedVideoItem
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.ui.components.VideoInlineSubReplyDetailContent
import com.android.purebilibili.feature.video.ui.components.rememberVideoCommentAppearance
import com.android.purebilibili.feature.video.ui.components.resolveReplyItemContentType
import com.android.purebilibili.feature.video.ui.components.shouldShowReplyTopAction
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.section.resolveDisplayBgmList
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import com.android.purebilibili.feature.video.ui.section.VideoTitleWithDesc
import com.android.purebilibili.feature.video.ui.section.VideoPlayerSection
import com.android.purebilibili.feature.video.ui.section.AiSummaryCard
import com.android.purebilibili.feature.video.ui.section.AiSummaryPromptCard
import com.android.purebilibili.feature.video.ui.section.VideoNoteCard
import com.android.purebilibili.feature.video.ui.section.VideoNoteDeleteConfirmDialog
import com.android.purebilibili.feature.video.ui.section.VideoNoteEditorSheet
import com.android.purebilibili.feature.video.ui.section.shouldShowAiSummaryEntry
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TabletCinemaLayout(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    commentState: CommentUiState,
    viewModel: PlayerViewModel,
    commentViewModel: VideoCommentViewModel,
    configuration: Configuration,
    isVerticalVideo: Boolean,
    sleepTimerMinutes: Int?,
    viewPoints: List<ViewPoint>,
    pbpProgressData: PbpProgressData?,
    bvid: String,
    coverUrl: String = "",
    onBack: () -> Unit,
    onUpClick: (Long) -> Unit,
    onBgmClick: (BgmInfo) -> Unit = {},
    onNavigateToAudioMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    isInPipMode: Boolean,
    onPipClick: () -> Unit,
    isPortraitFullscreen: Boolean = false,
    onHomeClick: () -> Unit,
    currentCodec: String = "hev1",
    onCodecChange: (String) -> Unit = {},
    currentSecondCodec: String = "avc1",
    onSecondCodecChange: (String) -> Unit = {},
    currentAudioQuality: Int = -1,
    onAudioQualityChange: (Int) -> Unit = {},
    transitionEnabled: Boolean = false,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    showRelatedVideosSection: Boolean = true,
    showUpBadge: Boolean = true,
    onSearchKeywordClick: (String) -> Unit = {},
    onOpenBilibiliLink: ((String) -> Unit)? = null,
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode =
        com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},
    forceCoverOnlyOnReturn: Boolean = false
) {
    val appContext = LocalContext.current
    val tabletCommentPanelWidthPreset by SettingsManager
        .getTabletCommentPanelWidthPreset(appContext)
        .collectAsStateWithLifecycle(initialValue = TabletCommentPanelWidthPreset.STANDARD
        )
    val commentMemberDecorationsEnabled by SettingsManager
        .getCommentMemberDecorationsEnabled(appContext)
        .collectAsStateWithLifecycle(initialValue = false
        )
    val policy = remember(configuration.screenWidthDp, tabletCommentPanelWidthPreset) {
        resolveTabletCinemaLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            commentWidthPreset = tabletCommentPanelWidthPreset
        )
    }
    val success = uiState as? PlayerUiState.Success
    val relatedVideos = remember(success?.related, showRelatedVideosSection) {
        if (showRelatedVideosSection) success?.related.orEmpty() else emptyList()
    }
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val initialCurtainState = remember(configuration.screenWidthDp) {
        resolveInitialCurtainState(configuration.screenWidthDp).name
    }
    var curtainStateName by rememberSaveable(bvid) { mutableStateOf(initialCurtainState) }
    val curtainState = remember(curtainStateName) {
        runCatching { TabletSideCurtainState.valueOf(curtainStateName) }
            .getOrDefault(resolveInitialCurtainState(configuration.screenWidthDp))
    }
    var selectedTab by rememberSaveable(bvid) { mutableIntStateOf(0) }
    val curtainPagerState = rememberPagerState(
        initialPage = selectedTab,
        pageCount = { 2 }
    )
    val curtainWidth by animateDpAsState(
        targetValue = resolveCurtainWidthDp(curtainState, policy).dp,
        animationSpec = tween(durationMillis = 240),
        label = "cinemaCurtainWidth"
    )

    LaunchedEffect(relatedVideos.size, commentState.replyCount, commentState.isRepliesLoading) {
        selectedTab = resolveCinemaSideCurtainSelectedTab(
            currentSelectedTab = selectedTab,
            replyCount = commentState.replyCount,
            isRepliesLoading = commentState.isRepliesLoading,
            hasRelatedVideos = relatedVideos.isNotEmpty()
        )
    }
    LaunchedEffect(selectedTab) {
        if (curtainPagerState.currentPage != selectedTab) {
            curtainPagerState.animateScrollToPage(selectedTab)
        }
    }
    LaunchedEffect(curtainPagerState.currentPage) {
        if (selectedTab != curtainPagerState.currentPage) {
            selectedTab = curtainPagerState.currentPage
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val padding = PaddingValues(
            top = max(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), policy.horizontalPaddingDp.dp),
            bottom = max(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(), policy.horizontalPaddingDp.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = policy.horizontalPaddingDp.dp)
                .padding(padding)
                .consumeWindowInsets(padding),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CinemaStagePlayer(
                    playerState = playerState,
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = onBack,
                    onHomeClick = onHomeClick,
                    bvid = bvid,
                    coverUrl = coverUrl,
                    onNavigateToAudioMode = onNavigateToAudioMode,
                    onToggleFullscreen = onToggleFullscreen,
                    isInPipMode = isInPipMode,
                    onPipClick = onPipClick,
                    sleepTimerMinutes = sleepTimerMinutes,
                    viewPoints = viewPoints,
                    pbpProgressData = pbpProgressData,
                    isVerticalVideo = isVerticalVideo,
                    isPortraitFullscreen = isPortraitFullscreen,
                    currentCodec = currentCodec,
                    onCodecChange = onCodecChange,
                    currentSecondCodec = currentSecondCodec,
                    onSecondCodecChange = onSecondCodecChange,
                    currentAudioQuality = currentAudioQuality,
                    onAudioQualityChange = onAudioQualityChange,
                    transitionEnabled = transitionEnabled,
                    currentPlayMode = currentPlayMode,
                    onPlayModeClick = onPlayModeClick,
                    onRelatedVideoClick = onRelatedVideoClick,
                    relatedVideos = relatedVideos,
                    playerMaxWidth = policy.playerMaxWidthDp.dp,
                    forceCoverOnlyOnReturn = forceCoverOnlyOnReturn
                )

                if (success != null) {
                    CinemaMetaPanel(
                        success = success,
                        downloadProgress = downloadProgress,
                        modifier = Modifier.weight(1f),
                        onFollowClick = { viewModel.toggleFollow() },
                        onUpClick = onUpClick,
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onLikeClick = { viewModel.toggleLike() },
                        onCoinClick = { viewModel.openCoinDialog() },
                        onTripleClick = { viewModel.doTripleAction() },
                        onDownloadClick = { viewModel.openDownloadDialog() },
                        onWatchLaterClick = { viewModel.toggleWatchLater() },
                        onOpenComments = {
                            selectedTab = 0
                            curtainStateName = TabletSideCurtainState.OPEN.name
                        },
                        onCollectionEpisodeClick = onRelatedVideoClick,
                        onPageSelect = { pageIndex -> viewModel.switchPage(pageIndex) },
                        onOpenBilibiliLink = onOpenBilibiliLink,
                        onBgmClick = onBgmClick,
                        onRelatedVideoClick = onRelatedVideoClick,
                        onRetryAiSummary = viewModel::retryAiSummary,
                        onCreateNoteDraftFromAiSummary = viewModel::createVideoNoteDraftFromAiSummary,
                        onOpenVideoNoteEditor = viewModel::openVideoNoteEditor,
                        onCloseVideoNoteEditor = viewModel::closeVideoNoteEditor,
                        onVideoNoteDocumentChange = viewModel::updateVideoNoteEditorDocument,
                        onInsertVideoNoteTimestamp = viewModel::insertCurrentPlaybackTimestampIntoNote,
                        onVideoNoteTimestampClick = viewModel::seekTo,
                        onSaveVideoNote = viewModel::saveVideoNote,
                        onDeleteVideoNote = viewModel::deleteVideoNote,
                        onRetryVideoNote = viewModel::retryVideoNote
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CupertinoActivityIndicator()
                        }
                    }
                }
            }

            CinemaSideCurtain(
                state = curtainState,
                width = curtainWidth,
                selectedTab = selectedTab,
                pagerState = curtainPagerState,
                onToggle = {
                    curtainStateName = when (curtainState) {
                        TabletSideCurtainState.OPEN -> TabletSideCurtainState.PEEK.name
                        TabletSideCurtainState.PEEK -> TabletSideCurtainState.OPEN.name
                        TabletSideCurtainState.HIDDEN -> TabletSideCurtainState.PEEK.name
                    }
                },
                onTabSelected = { tab ->
                    selectedTab = tab
                    curtainStateName = TabletSideCurtainState.OPEN.name
                },
                success = success,
                relatedVideos = relatedVideos,
                commentState = commentState,
                commentViewModel = commentViewModel,
                viewModel = viewModel,
                playerState = playerState,
                onUpClick = onUpClick,
                onRelatedVideoClick = onRelatedVideoClick,
                context = appContext,
                showUpBadge = showUpBadge,
                showIdentityDecorations = commentMemberDecorationsEnabled,
                onSearchKeywordClick = onSearchKeywordClick,
                onOpenBilibiliLink = onOpenBilibiliLink
            )
        }
    }
}

@Composable
private fun CinemaStagePlayer(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    bvid: String,
    coverUrl: String,
    onNavigateToAudioMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    isInPipMode: Boolean,
    onPipClick: () -> Unit,
    sleepTimerMinutes: Int?,
    viewPoints: List<ViewPoint>,
    pbpProgressData: PbpProgressData?,
    isVerticalVideo: Boolean,
    isPortraitFullscreen: Boolean,
    currentCodec: String,
    onCodecChange: (String) -> Unit,
    currentSecondCodec: String,
    onSecondCodecChange: (String) -> Unit,
    currentAudioQuality: Int,
    onAudioQualityChange: (Int) -> Unit,
    transitionEnabled: Boolean,
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode,
    onPlayModeClick: () -> Unit,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    relatedVideos: List<com.android.purebilibili.data.model.response.RelatedVideo>,
    playerMaxWidth: Dp,
    forceCoverOnlyOnReturn: Boolean
) {
    val context = LocalContext.current
    val success = uiState as? PlayerUiState.Success
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val playerContainerModifier = if (
        shouldEnableVideoCoverSharedTransition(
            transitionEnabled = transitionEnabled,
            hasSharedTransitionScope = sharedTransitionScope != null,
            hasAnimatedVisibilityScope = animatedVisibilityScope != null
        ) && !forceCoverOnlyOnReturn
    ) {
        with(requireNotNull(sharedTransitionScope)) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = com.android.purebilibili.core.ui.transition.videoCoverSharedElementKey(bvid)),
                animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                clipInOverlayDuringTransition = OverlayClip(
                    RoundedCornerShape(12.dp)
                )
            )
        }
    } else {
        Modifier
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val playerWidth = minOf(maxWidth, playerMaxWidth)
        val videoHeight = if (forceCoverOnlyOnReturn) {
            playerWidth / VIDEO_SHARED_COVER_ASPECT_RATIO
        } else {
            playerWidth * 9f / 16f
        }
        Surface(
            modifier = playerContainerModifier
                .align(Alignment.Center)
                .width(playerWidth)
                .height(videoHeight)
                .aspectRatio(playerWidth / videoHeight),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 4.dp
        ) {
            VideoPlayerSection(
                playerState = playerState,
                uiState = uiState,
                isFullscreen = false,
                isInPipMode = isInPipMode,
                onToggleFullscreen = onToggleFullscreen,
                onQualityChange = { qid -> viewModel.changeQuality(qid) },
                onBack = onBack,
                onHomeClick = onHomeClick,
                bvid = bvid,
                coverUrl = coverUrl,
                onDoubleTapLike = { viewModel.toggleLike() },
                onReloadVideo = { viewModel.reloadVideo() },
                currentCdnIndex = success?.currentCdnIndex ?: 0,
                cdnCount = success?.cdnCount ?: 1,
                cdnLineDiagnostics = success?.cdnLineDiagnostics.orEmpty(),
                isCdnProbing = success?.isCdnProbing ?: false,
                onSwitchCdn = { viewModel.switchCdn() },
                onSwitchCdnTo = { viewModel.switchCdnTo(it) },
                onProbeCdnCandidates = { viewModel.probeCurrentCdnCandidates() },
                isAudioOnly = false,
                onAudioOnlyToggle = {
                    viewModel.setAudioMode(true)
                    onNavigateToAudioMode()
                },
                sleepTimerMinutes = sleepTimerMinutes,
                onSleepTimerChange = { viewModel.setSleepTimer(it) },
                videoshotData = success?.videoshotData,
                viewPoints = viewPoints,
                pbpProgressData = pbpProgressData,
                isVerticalVideo = isVerticalVideo,
                onPortraitFullscreen = { playerState.setPortraitFullscreen(true) },
                isPortraitFullscreen = isPortraitFullscreen,
                onPipClick = onPipClick,
                currentCodec = currentCodec,
                onCodecChange = onCodecChange,
                currentSecondCodec = currentSecondCodec,
                onSecondCodecChange = onSecondCodecChange,
                currentAudioQuality = currentAudioQuality,
                onAudioQualityChange = onAudioQualityChange,
                onPlaybackSpeedChange = { viewModel.applyPlaybackSpeedFromUi(it) },
                onSaveCover = { viewModel.saveCover(context) },
                onDownloadAudio = { viewModel.downloadAudio(context) },
                currentPlayMode = currentPlayMode,
                onPlayModeClick = onPlayModeClick,
                onRelatedVideoClick = onRelatedVideoClick,
                relatedVideos = relatedVideos,
                forceCoverOnly = forceCoverOnlyOnReturn,
                ugcSeason = success?.info?.ugc_season,
                isFollowed = success?.isFollowing ?: false,
                isLiked = success?.isLiked ?: false,
                isCoined = success?.coinCount?.let { it > 0 } ?: false,
                isFavorited = success?.isFavorited ?: false,
                onToggleFollow = { viewModel.toggleFollow() },
                onToggleLike = { viewModel.toggleLike() },
                onDislike = { viewModel.markVideoNotInterested() },
                onCoin = { viewModel.showCoinDialog() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onTriple = { viewModel.doTripleAction() },
                onSubtitleTrackSelected = viewModel::selectSubtitleTrack
            )
        }
    }
}

@Composable
private fun CinemaMetaPanel(
    success: PlayerUiState.Success,
    downloadProgress: Float,
    modifier: Modifier = Modifier,
    onFollowClick: () -> Unit,
    onUpClick: (Long) -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onOpenComments: () -> Unit,
    onCollectionEpisodeClick: (String, android.os.Bundle?) -> Unit,
    onPageSelect: (Int) -> Unit,
    onOpenBilibiliLink: ((String) -> Unit)?,
    onBgmClick: (BgmInfo) -> Unit = {},
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit = { _, _ -> },
    onRetryAiSummary: () -> Unit,
    onCreateNoteDraftFromAiSummary: () -> Unit,
    onOpenVideoNoteEditor: () -> Unit,
    onCloseVideoNoteEditor: () -> Unit,
    onVideoNoteDocumentChange: (VideoNoteEditorDocument) -> Unit,
    onInsertVideoNoteTimestamp: () -> Unit,
    onVideoNoteTimestampClick: (Long) -> Unit,
    onSaveVideoNote: (VideoNoteEditorDocument) -> Unit,
    onDeleteVideoNote: () -> Unit,
    onRetryVideoNote: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val currentPageIndex = remember(success.info.cid, success.info.pages) {
        success.info.pages.indexOfFirst { it.cid == success.info.cid }.coerceAtLeast(0)
    }
    var showCollectionSheet by rememberSaveable(success.info.bvid) { mutableStateOf(false) }
    var confirmDeleteNote by rememberSaveable(success.info.bvid) { mutableStateOf(false) }
    val onShareVideoNote: (VideoNoteEditorDocument, Boolean) -> Unit = { document, isDraft ->
        ShareUtils.shareText(
            context = context,
            subject = document.title.ifBlank { success.info.title },
            text = buildVideoNoteShareText(
                videoTitle = success.info.title,
                bvid = success.info.bvid,
                document = document,
                isDraft = isDraft
            ),
            chooserTitle = "分享视频笔记"
        )
    }

    success.info.ugc_season?.let { season ->
        if (showCollectionSheet) {
            CollectionSheet(
                ugcSeason = season,
                currentBvid = success.info.bvid,
                currentCid = success.info.cid,
                onDismiss = { showCollectionSheet = false },
                onEpisodeClick = { episode ->
                    showCollectionSheet = false
                    val navOptions = buildVideoNavigationOptions(targetCid = episode.cid)
                    onCollectionEpisodeClick(episode.bvid, navOptions)
                }
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp),
        shape = RoundedCornerShape(24.dp),
        color = resolveCinemaMetaPanelContainerColor(
            isDarkTheme = isDarkTheme,
            surfaceColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val metaBlocks = remember(
            success.info.owner.mid,
            success.info.owner.name,
            success.info.ugc_season,
            success.info.pages.size
        ) {
            resolveCinemaMetaPanelBlocks(
                hasCollection = success.info.ugc_season != null,
                hasMultiplePages = success.info.pages.size > 1
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = metaBlocks,
                key = { it.name }
            ) { block ->
                when (block) {
                    CinemaMetaPanelBlock.ACTIONS -> {
                        if (success.info.owner.mid > 0L || success.info.owner.name.isNotBlank()) {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val isWide = maxWidth >= 600.dp

                                AnimatedContent(
                                    targetState = isWide,
                                    label = "ActionUpInfoTransition"
                                ) { targetIsWide ->
                                    if (targetIsWide) {
                                        // 宽度足够，横排
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CinemaMetaUpInfo(
                                                success = success,
                                                onFollowClick = onFollowClick,
                                                onUpClick = onUpClick,
                                                modifier = Modifier.weight(1f)
                                            )
                                            CinemaMetaActions(
                                                success = success,
                                                downloadProgress = downloadProgress,
                                                context = context,
                                                onFavoriteClick = onFavoriteClick,
                                                onLikeClick = onLikeClick,
                                                onCoinClick = onCoinClick,
                                                onTripleClick = onTripleClick,
                                                onDownloadClick = onDownloadClick,
                                                onWatchLaterClick = onWatchLaterClick,
                                                onOpenComments = onOpenComments,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        // 宽度不足，竖排
                                        Column(Modifier.fillMaxWidth()) {
                                            CinemaMetaActions(
                                                success = success,
                                                downloadProgress = downloadProgress,
                                                context = context,
                                                onFavoriteClick = onFavoriteClick,
                                                onLikeClick = onLikeClick,
                                                onCoinClick = onCoinClick,
                                                onTripleClick = onTripleClick,
                                                onDownloadClick = onDownloadClick,
                                                onWatchLaterClick = onWatchLaterClick,
                                                onOpenComments = onOpenComments,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            CinemaMetaUpInfo(
                                                success = success,
                                                onFollowClick = onFollowClick,
                                                onUpClick = onUpClick,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            CinemaMetaActions(
                                success = success,
                                downloadProgress = downloadProgress,
                                context = context,
                                onFavoriteClick = onFavoriteClick,
                                onLikeClick = onLikeClick,
                                onCoinClick = onCoinClick,
                                onTripleClick = onTripleClick,
                                onDownloadClick = onDownloadClick,
                                onWatchLaterClick = onWatchLaterClick,
                                onOpenComments = onOpenComments,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    CinemaMetaPanelBlock.INTRO -> {
                        CinemaVideoIntroSection(
                            success = success,
                            onOpenBilibiliLink = onOpenBilibiliLink,
                            onBgmClick = onBgmClick,
                            onRelatedVideoClick = onRelatedVideoClick,
                            onRetryAiSummary = onRetryAiSummary,
                            onCreateNoteDraftFromAiSummary = onCreateNoteDraftFromAiSummary,
                            onOpenVideoNoteEditor = onOpenVideoNoteEditor,
                            onRetryVideoNote = onRetryVideoNote,
                            onDeleteVideoNoteClick = { confirmDeleteNote = true },
                            onShareVideoNote = { document -> onShareVideoNote(document, false) },
                            onPublicVideoNoteClick = { _, url ->
                                if (url.isNotBlank()) onOpenBilibiliLink?.invoke(url)
                            }
                        )
                    }
                    CinemaMetaPanelBlock.COLLECTION -> {
                        success.info.ugc_season?.let { season ->
                            CollectionRow(
                                ugcSeason = season,
                                currentBvid = success.info.bvid,
                                currentCid = success.info.cid,
                                onClick = { showCollectionSheet = true }
                            )
                        }
                    }
                    CinemaMetaPanelBlock.PAGES -> {
                        if (success.info.pages.size > 1) {
                            PagesSelector(
                                pages = success.info.pages,
                                currentPageIndex = currentPageIndex,
                                onPageSelect = onPageSelect,
                                forceGridMode = true
                            )
                        }
                    }
                }
            }
        }
    }

    VideoNoteEditorSheet(
        noteState = success.videoNoteState,
        onDismiss = onCloseVideoNoteEditor,
        onDocumentChange = onVideoNoteDocumentChange,
        onInsertTimestamp = onInsertVideoNoteTimestamp,
        onTimestampClick = onVideoNoteTimestampClick,
        onShare = { document -> onShareVideoNote(document, success.videoNoteState.editorFromAiSummary) },
        onSave = onSaveVideoNote
    )

    VideoNoteDeleteConfirmDialog(
        visible = confirmDeleteNote,
        deleting = success.videoNoteState.deleting,
        onConfirm = {
            confirmDeleteNote = false
            onDeleteVideoNote()
        },
        onDismiss = { confirmDeleteNote = false }
    )
}

@Composable
private fun CinemaMetaActions(
    success: PlayerUiState.Success,
    downloadProgress: Float,
    context: android.content.Context,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActionButtonsRow(
        info = success.info,
        isFavorited = success.isFavorited,
        isLiked = success.isLiked,
        coinCount = success.coinCount,
        downloadProgress = downloadProgress,
        isInWatchLater = success.isInWatchLater,
        onFavoriteClick = onFavoriteClick,
        onLikeClick = onLikeClick,
        onCoinClick = onCoinClick,
        onTripleClick = onTripleClick,
        onDownloadClick = onDownloadClick,
        onWatchLaterClick = onWatchLaterClick,
        onCommentClick = onOpenComments,
        onShareClick = {
            ShareUtils.shareVideo(
                context,
                success.info.title,
                success.info.bvid
            )
        },
        modifier = modifier
    )
}

@Composable
private fun CinemaMetaUpInfo(
    success: PlayerUiState.Success,
    onFollowClick: () -> Unit,
    onUpClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    UpInfoSection(
        info = success.info,
        isFollowing = success.isFollowing,
        onFollowClick = onFollowClick,
        onUpClick = onUpClick,
        followerCount = success.ownerFollowerCount,
        videoCount = success.ownerVideoCount,
        modifier = modifier
    )
}

@Composable
private fun CinemaVideoIntroSection(
    success: PlayerUiState.Success,
    onBgmClick: (BgmInfo) -> Unit = {},
    onOpenBilibiliLink: ((String) -> Unit)? = null,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit = { _, _ -> },
    onRetryAiSummary: () -> Unit = {},
    onCreateNoteDraftFromAiSummary: () -> Unit = {},
    onOpenVideoNoteEditor: () -> Unit = {},
    onRetryVideoNote: () -> Unit = {},
    onDeleteVideoNoteClick: () -> Unit = {},
    onShareVideoNote: (VideoNoteEditorDocument) -> Unit = {},
    onPublicVideoNoteClick: (Long, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val videoAiSummaryEntryEnabled by com.android.purebilibili.core.store.SettingsManager
        .getVideoAiSummaryEntryEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true
        )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = resolveCinemaIntroCardContainerColor(
                isDarkTheme = isDarkTheme,
                surfaceContainerLowColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            VideoTitleWithDesc(
                info = success.info,
                videoTags = success.videoTags,
                onDescriptionUrlClick = onOpenBilibiliLink,
                bgmList = resolveDisplayBgmList(
                    bgmInfo = success.bgmInfo,
                    bgmInfoList = success.bgmInfoList
                ),
                onBgmClick = onBgmClick,
                onRelatedVideoClick = onRelatedVideoClick
            )
        }
        if (shouldShowAiSummaryEntry(
                aiSummary = success.aiSummary,
                isAiSummaryEntryEnabled = videoAiSummaryEntryEnabled
            )
        ) {
            AiSummaryCard(
                aiSummary = success.aiSummary,
                onCreateNoteDraftClick = onCreateNoteDraftFromAiSummary
            )
        } else if (videoAiSummaryEntryEnabled && success.aiSummaryPrompt != null) {
            AiSummaryPromptCard(
                promptState = success.aiSummaryPrompt,
                onActionClick = onRetryAiSummary
            )
        }
        val videoNoteEnabled by SettingsManager.getVideoNoteEnabled(context)
            .collectAsStateWithLifecycle(initialValue = true)
        val videoNoteDefaultCollapsed by SettingsManager.getVideoNoteDefaultCollapsed(context)
            .collectAsStateWithLifecycle(initialValue = false)
        if (shouldShowVideoNoteCard(videoNoteEnabled)) {
            VideoNoteCard(
                noteState = success.videoNoteState,
                isLoggedIn = success.isLoggedIn,
                onCreateOrEditClick = onOpenVideoNoteEditor,
                onRetryClick = onRetryVideoNote,
                onDeleteClick = onDeleteVideoNoteClick,
                onShareClick = onShareVideoNote,
                onPublicNoteClick = onPublicVideoNoteClick,
                defaultCollapsed = videoNoteDefaultCollapsed
            )
        }
    }
}

@Composable
private fun CinemaSideCurtain(
    state: TabletSideCurtainState,
    width: Dp,
    selectedTab: Int,
    pagerState: PagerState,
    onToggle: () -> Unit,
    onTabSelected: (Int) -> Unit,
    success: PlayerUiState.Success?,
    relatedVideos: List<com.android.purebilibili.data.model.response.RelatedVideo>,
    commentState: CommentUiState,
    commentViewModel: VideoCommentViewModel,
    viewModel: PlayerViewModel,
    playerState: VideoPlayerState,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    context: android.content.Context,
    showUpBadge: Boolean,
    showIdentityDecorations: Boolean,
    onSearchKeywordClick: (String) -> Unit,
    onOpenBilibiliLink: ((String) -> Unit)?
) {
    val subReplyState by commentViewModel.subReplyState.collectAsStateWithLifecycle()
    val transition = updateTransition(targetState = state, label = "SideCurtainAnimation")
    LaunchedEffect(subReplyState.visible) {
        if (subReplyState.visible) {
            onTabSelected(0)
        }
    }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            if (transition.currentState == TabletSideCurtainState.OPEN) {
                pagerState.animateScrollToPage(selectedTab)
            } else {    // 动画中或关闭状态停用动画，避免卡动画
                pagerState.scrollToPage(selectedTab)
            }
        }
    }
    transition.AnimatedContent(
        modifier = Modifier.fillMaxHeight()
    ) { targetState ->
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // 移除点击视觉反馈
                    ) { onToggle() },
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (targetState == TabletSideCurtainState.OPEN) {
                            Icons.Outlined.KeyboardDoubleArrowRight
                        } else {
                            Icons.Outlined.KeyboardDoubleArrowLeft
                        },
                        contentDescription = "toggle curtain",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (targetState != TabletSideCurtainState.HIDDEN) {
                Surface(
                    modifier = Modifier
                        .width(width)
                        .fillMaxHeight()
                        .animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    if (targetState == TabletSideCurtainState.PEEK) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { onTabSelected(0) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "comments"
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            IconButton(onClick = { onTabSelected(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                    contentDescription = "related videos"
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PrimaryTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == 0,
                                    onClick = {
                                        onTabSelected(0)
                                    },
                                    text = {
                                        Text(
                                            text = "评论 ${if (commentState.replyCount > 0) "(${commentState.replyCount})" else ""}"
                                        )
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 1,
                                    onClick = {
                                        onTabSelected(1)
                                    },
                                    text = { Text("相关推荐") }
                                )
                            }

                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = true,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when {
                                    success == null -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CupertinoActivityIndicator()
                                        }
                                    }

                                    page == 0 -> {
                                        CinemaCommentsPane(
                                            success = success,
                                            commentState = commentState,
                                            subReplyState = subReplyState,
                                            commentViewModel = commentViewModel,
                                            viewModel = viewModel,
                                            playerState = playerState,
                                            onUpClick = onUpClick,
                                            context = context,
                                            onRelatedVideoClick = onRelatedVideoClick,
                                            showIdentityDecorations = showIdentityDecorations,
                                            onSearchKeywordClick = onSearchKeywordClick,
                                            onOpenBilibiliLink = onOpenBilibiliLink
                                        )
                                    }

                                    else -> {
                                        CinemaRelatedPane(
                                            success = success,
                                            relatedVideos = relatedVideos,
                                            onRelatedVideoClick = onRelatedVideoClick,
                                            context = context,
                                            showUpBadge = showUpBadge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinemaCommentsPane(
    success: PlayerUiState.Success,
    commentState: CommentUiState,
    subReplyState: SubReplyUiState,
    commentViewModel: VideoCommentViewModel,
    viewModel: PlayerViewModel,
    playerState: VideoPlayerState,
    onUpClick: (Long) -> Unit,
    context: android.content.Context,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    showIdentityDecorations: Boolean,
    onSearchKeywordClick: (String) -> Unit,
    onOpenBilibiliLink: ((String) -> Unit)?
) {
    val commentAppearance = rememberVideoCommentAppearance()
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val openCommentUrl: (String) -> Unit = openCommentUrl@{ rawUrl ->
        val url = rawUrl.trim()
        if (url.isEmpty()) return@openCommentUrl
        if (onOpenBilibiliLink != null) {
            onOpenBilibiliLink(url)
            return@openCommentUrl
        }
        when (val target = resolveCommentUrlNavigationTarget(url)) {
            is CommentUrlNavigationTarget.Video -> {
                onRelatedVideoClick(target.videoId, null)
                return@openCommentUrl
            }

            is CommentUrlNavigationTarget.Search -> {
                onSearchKeywordClick(target.keyword)
                return@openCommentUrl
            }

            is CommentUrlNavigationTarget.Space -> {
                onUpClick(target.mid)
                return@openCommentUrl
            }

            null -> Unit
        }
        runCatching { uriHandler.openUri(url) }
    }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewInitialIndex by remember { mutableIntStateOf(0) }
    var sourceRect by remember { mutableStateOf<Rect?>(null) }
    var previewTextContent by remember { mutableStateOf<ImagePreviewTextContent?>(null) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3 && !commentState.isRepliesLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            commentViewModel.loadComments()
        }
    }

    if (showImagePreview && previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages,
            initialIndex = previewInitialIndex,
            sourceRect = sourceRect,
            textContent = previewTextContent,
            onDismiss = {
                showImagePreview = false
                previewTextContent = null
            }
        )
    }

    if (subReplyState.visible && subReplyState.rootReply != null) {
        VideoInlineSubReplyDetailContent(
            state = subReplyState,
            commentState = commentState,
            emoteMap = success.emoteMap,
            maxTimestampMs = success.videoDurationMs.takeIf { it > 0L },
            onLoadMore = { commentViewModel.loadMoreSubReplies() },
            onDismiss = { commentViewModel.closeSubReply() },
            onRootCommentClick = { viewModel.openRootCommentComposer() },
            onTimestampClick = { positionMs ->
                seekPlayerFromUserAction(playerState.player, positionMs)
            },
            onImagePreview = { images, index, rect, textContent ->
                previewImages = images
                previewInitialIndex = index
                sourceRect = rect
                previewTextContent = textContent
                showImagePreview = true
            },
            onReplyClick = { reply ->
                viewModel.setReplyingTo(reply)
                viewModel.showCommentInputDialog()
            },
            onConversationClick = commentViewModel::openSubReplyConversation,
            onConversationBack = commentViewModel::closeSubReplyConversation,
            onDissolveStart = { rpid -> commentViewModel.startSubDissolve(rpid) },
            onDeleteComment = { rpid -> commentViewModel.deleteSubComment(rpid) },
            onCommentLike = commentViewModel::likeComment,
            onReportComment = commentViewModel::reportComment,
            onUrlClick = openCommentUrl,
            showIdentityDecorations = showIdentityDecorations,
            onAvatarClick = { mid -> mid.toLongOrNull()?.let(onUpClick) ?: Unit }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 74.dp)
        ) {
            item {
                CommentSortFilterBar(
                    count = commentState.replyCount,
                    sortMode = commentState.sortMode,
                    onSortModeChange = { mode ->
                        commentViewModel.setSortMode(mode)
                        scope.launch {
                            SettingsManager.setCommentDefaultSortMode(context, mode.apiMode)
                        }
                    },
                    upOnly = commentState.upOnlyFilter,
                    onUpOnlyToggle = { commentViewModel.toggleUpOnly() }
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = commentAppearance.composerHintBackgroundColor,
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        viewModel.openRootCommentComposer()
                    }
                ) {
                    Text(
                        text = "写评论，直接和 UP 主交流",
                        color = commentAppearance.secondaryTextColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            items(
                items = commentState.replies,
                key = { "curtain_reply_${it.rpid}" },
                contentType = { resolveReplyItemContentType(it) }
            ) { reply ->
                ReplyItemView(
                    item = reply,
                    upMid = success.info.owner.mid,
                    showUpFlag = commentState.showUpFlag,
                    showIdentityDecorations = showIdentityDecorations,
                    isPinned = reply.rpid in commentState.pinnedReplyIds,
                    emoteMap = success.emoteMap,
                    onClick = {},
                    onSubClick = { commentViewModel.openSubReply(it) },
                    onTimestampClick = { positionMs ->
                        seekPlayerFromUserAction(playerState.player, positionMs)
                    },
                    maxTimestampMs = success.videoDurationMs.takeIf { it > 0L },
                    onImagePreview = { images, index, rect, textContent ->
                        previewImages = images
                        previewInitialIndex = index
                        sourceRect = rect
                        previewTextContent = textContent
                        showImagePreview = true
                    },
                    onLikeClick = { commentViewModel.likeComment(reply.rpid) },
                    isLiked = reply.action == 1 || reply.rpid in commentState.likedComments,
                    onReplyClick = {
                        viewModel.setReplyingTo(reply)
                        viewModel.showCommentInputDialog()
                    },
                    onReportClick = { reason -> commentViewModel.reportComment(reply.rpid, reason) },
                    canToggleTop = shouldShowReplyTopAction(
                        currentMid = commentState.currentMid,
                        upMid = success.info.owner.mid,
                        item = reply
                    ),
                    onToggleTopClick = { commentViewModel.toggleTopComment(reply) },
                    onDeleteClick = if (
                        commentState.currentMid > 0 && reply.mid == commentState.currentMid
                    ) {
                        { commentViewModel.startDissolve(reply.rpid) }
                    } else {
                        null
                    },
                    onUrlClick = openCommentUrl,
                    onAvatarClick = { mid ->
                        mid.toLongOrNull()?.let(onUpClick)
                    }
                )
            }
            if (commentState.isRepliesLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CupertinoActivityIndicator()
                    }
                }
            }
            if (commentState.replies.isEmpty() && !commentState.isRepliesLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "还没有评论，先看看相关推荐",
                            style = MaterialTheme.typography.bodyMedium,
                            color = commentAppearance.secondaryTextColor
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { commentViewModel.toggleUpOnly() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            containerColor = if (commentState.upOnlyFilter) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (commentState.upOnlyFilter) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
            shape = CircleShape
        ) {
            Text(
                text = "UP",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
    }
}

@Composable
private fun CinemaRelatedPane(
    success: PlayerUiState.Success,
    relatedVideos: List<com.android.purebilibili.data.model.response.RelatedVideo>,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    context: android.content.Context,
    showUpBadge: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(
            items = relatedVideos,
            key = { index, item ->
                resolveIndexedVideoLazyKey(
                    namespace = "cinema_related",
                    index = index,
                    bvid = item.bvid,
                    aid = item.aid,
                    cid = item.cid
                )
            }
        ) { _, video ->
            RelatedVideoItem(
                video = video,
                isFollowed = video.owner.mid in success.followingMids,
                showUpBadge = showUpBadge,
                onClick = {
                    val activity = (context as? Activity)
                        ?: (context as? ContextWrapper)?.baseContext as? Activity
                    val options = activity?.let {
                        android.app.ActivityOptions.makeSceneTransitionAnimation(it).toBundle()
                    }
                    val navOptions = android.os.Bundle(options ?: android.os.Bundle.EMPTY)
                    if (video.cid > 0L) {
                        navOptions.putLong(VIDEO_NAV_TARGET_CID_KEY, video.cid)
                    }
                    onRelatedVideoClick(video.bvid, navOptions)
                }
            )
        }
        if (relatedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂时没有推荐视频",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
