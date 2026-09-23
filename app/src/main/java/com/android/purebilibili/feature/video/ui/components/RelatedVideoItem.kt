package com.android.purebilibili.feature.video.ui.components

import coil3.request.crossfade

import android.widget.Toast
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TodayWatchDislikedVideoSnapshot
import com.android.purebilibili.core.store.TodayWatchFeedbackStore
import com.android.purebilibili.core.store.withDislikedVideoFeedback
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.LocalVideoTransitionAdaptiveInfo
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceCoverPresentation
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.rememberNativeVideoCardSnapshotController
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.ui.transition.withMeasuredCoverDecodeSize
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.data.model.response.RecommendationFeedbackLocalAction
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.BlockedUpRepository
import com.android.purebilibili.feature.home.HomeFeedCardLayout
import com.android.purebilibili.feature.home.components.cards.HorizontalVideoCardFrame
import com.android.purebilibili.feature.home.components.cards.HorizontalVideoStatRow
import com.android.purebilibili.feature.home.resolveHomeFeedCardLayout
import com.android.purebilibili.feature.video.ui.FollowBadgeTone
import com.android.purebilibili.feature.video.ui.resolveVideoFollowVisualPolicy
import com.android.purebilibili.navigation.VideoRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * PiliPlus horizontal cards use a 16:10 cover.
 */
internal const val RELATED_VIDEO_CARD_COVER_ASPECT_RATIO = 16f / 10f

internal const val RELATED_VIDEO_GRID_COLUMNS = 1

/**
 * Related Videos Header
 */
@Composable
fun RelatedVideosHeader() {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "\u66f4\u591a\u63a8\u8350",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveRelatedVideoCardPressScaleTarget(
    isPressed: Boolean,
    transitionEnabled: Boolean
): Float = 1f

@Suppress("UNUSED_PARAMETER")
internal fun shouldEnableRelatedVideoCoverCrossfade(
    transitionEnabled: Boolean
): Boolean = false

@Suppress("UNUSED_PARAMETER")
internal fun shouldTriggerRelatedVideoPressHaptic(
    isPressed: Boolean,
    transitionEnabled: Boolean
): Boolean = false

internal fun resolveRelatedVideoSharedElementSourceRoute(sourceRoute: String?): String {
    return sourceRoute
        ?.substringBefore("?")
        ?.takeIf { it.isNotBlank() }
        ?: VideoRoute.base
}

internal fun chunkRelatedVideosForHomeStyleGrid(
    videos: List<RelatedVideo>,
): List<List<RelatedVideo>> {
    if (videos.isEmpty()) return emptyList()
    return videos.chunked(RELATED_VIDEO_GRID_COLUMNS)
}

@Composable
internal fun rememberRelatedVideoCardLayout(): HomeFeedCardLayout {
    val context = LocalContext.current
    val homeFeedCardStyle by SettingsManager
        .getHomeFeedCardStyle(context)
        .collectAsStateWithLifecycle(initialValue = HomeFeedCardStyle.BILIPAI)
    return remember(homeFeedCardStyle) {
        resolveHomeFeedCardLayout(homeFeedCardStyle)
    }
}

/**
 * 相关推荐单列横卡：点击时冻结来源标识、几何与 chrome，供整卡 Morph 及逐层返回。
 * 与首页视频卡一致，由一个 sharedBounds 容器承载封面、标题、UP 信息和统计内容。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RelatedVideoItem(
    video: RelatedVideo,
    isFollowed: Boolean = false,
    showUpBadge: Boolean = true,
    coverAspectRatio: Float = RELATED_VIDEO_CARD_COVER_ASPECT_RATIO,
    stacked: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val densityValue = density.density
    val sourceRoute = resolveRelatedVideoSharedElementSourceRoute(
        LocalVideoCardSharedElementSourceRoute.current
    )
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedTransitionEnabled = LocalSharedTransitionEnabled.current
    val sharedReady = sharedTransitionEnabled &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val transitionAdaptiveInfo = LocalVideoTransitionAdaptiveInfo.current
    val sharedTransitionMotionSpec = remember(
        sourceRoute,
        sharedTransitionEnabled,
        sharedTransitionSpeedSettings,
        transitionAdaptiveInfo,
    ) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRoute,
            transitionEnabled = sharedTransitionEnabled,
            speedSettings = sharedTransitionSpeedSettings,
            adaptiveInfo = transitionAdaptiveInfo,
        )
    }
    val useCardShellSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = sharedReady,
    )
    val cardCoordinatesRef = remember { object { var value: LayoutCoordinates? = null } }
    val coverCoordinatesRef = remember { object { var value: LayoutCoordinates? = null } }
    val nativeCardSnapshot = rememberNativeVideoCardSnapshotController(video.bvid)
    val cardShape = AppShapes.container(ContainerLevel.Card)
    val cardCornerRadiusDp = AppShapes.containerCornerDp(ContainerLevel.Card).value.roundToInt()
    val context = LocalContext.current
    // Keep the request stable and skip image crossfade during the Miuix page transition.
    val stationaryCoverUrl = remember(video.pic) {
        FormatUtils.resolveVideoCoverUrl(video.pic, useLowQuality = false)
    }
    val coverRequest = remember(stationaryCoverUrl) {
        ImageRequest.Builder(context)
            .data(stationaryCoverUrl)
            .crossfade(false)
            .memoryCacheKey(stationaryCoverUrl)
            .diskCacheKey(stationaryCoverUrl)
            .build()
    }
    val triggerRelatedVideoClick = {
        cardCoordinatesRef.value
            ?.takeIf { it.isAttached }
            ?.boundsInRoot()
            ?.let { bounds ->
                val sourceCoverBounds = coverCoordinatesRef.value
                    ?.takeIf { it.isAttached }
                    ?.boundsInRoot()
                CardPositionManager.recordVideoCardPosition(
                    bvid = video.bvid,
                    sourceRoute = sourceRoute,
                    bounds = bounds,
                    screenWidth = screenWidthPx,
                    screenHeight = screenHeightPx,
                    density = densityValue,
                    sourceCornerDp = cardCornerRadiusDp,
                    coverBounds = sourceCoverBounds,
                    sourceLayout = if (stacked) {
                        VideoCardSourceLayout.STACKED
                    } else {
                        VideoCardSourceLayout.SIDE_BY_SIDE
                    },
                    sourceChromeSnapshot = VideoCardSourceChromeSnapshot(
                        title = video.title,
                        ownerName = video.owner.name,
                        ownerFaceUrl = video.owner.face,
                        viewText = FormatUtils.formatStat(video.stat.view.toLong()),
                        danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                        durationText = FormatUtils.formatDuration(video.duration),
                        followed = isFollowed,
                        // Related horizontal card keeps play/danmaku in the info column.
                        infoPresentation = com.android.purebilibili.core.ui.transition
                            .resolveVideoCardSourceInfoPresentation(
                                publishTimeText = FormatUtils.formatPublishTime(video.pubdate),
                                showStatsInInfo = true,
                                showOverflowMenu = onMoreClick != null,
                            ),
                        coverPresentation = VideoCardSourceCoverPresentation(
                            showDurationOnCover = true,
                        ),
                        coverUrl = stationaryCoverUrl,
                        coverCacheKey = stationaryCoverUrl,
                    ).withMeasuredCoverDecodeSize(sourceCoverBounds),
                )
                nativeCardSnapshot.capture()
            }
        onClick()
        Unit
    }
    // 排版对齐首页单列卡片:标题用 feed 紧凑级,统计用 labelSmall。
    val contentTypography = com.android.purebilibili.core.ui.feedContentTypography(
        com.android.purebilibili.core.ui.FeedTitleHierarchy.Standard,
    )
    val moreHaptic = rememberHapticFeedback()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .onGloballyPositioned { coordinates ->
                cardCoordinatesRef.value = coordinates
            }
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                motionSpec = sharedTransitionMotionSpec,
                clipShape = cardShape,
                crossfadeSourceContent = true,
            )
            .clip(cardShape)
            .then(nativeCardSnapshot.modifier)
            .background(AppSurfaceTokens.cardContainer())
            .clickable(onClick = triggerRelatedVideoClick)
    ) {
        HorizontalVideoCardFrame(
            coverAspectRatio = coverAspectRatio,
            stacked = stacked,
            coverModifier = Modifier.onGloballyPositioned { coordinates ->
                coverCoordinatesRef.value = coordinates
            },
            coverOverlayModifier = nativeCardSnapshot.coverOverlayModifier,
            coverContent = {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            coverOverlayContent = {
                AppText(
                    text = FormatUtils.formatDuration(video.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            blurRadius = 4f,
                            offset = Offset(0f, 1f),
                        ),
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            },
            infoContent = {
                AppText(
                    text = video.title,
                    style = contentTypography.title,
                    maxLines = 2,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                    ) {
                        val publishTime = remember(video.pubdate) {
                            FormatUtils.formatPublishTime(video.pubdate)
                        }
                        if (publishTime.isNotBlank()) {
                            AppText(
                                text = publishTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 96.dp),
                            )
                        }
                        UpBadgeName(
                            name = video.owner.name,
                            inlineTrailingContent = if (isFollowed) {
                                {
                                    val followVisualPolicy = resolveVideoFollowVisualPolicy(
                                        isFollowing = true,
                                        darkTheme = true,
                                    )
                                    AppText(
                                        text = "已关注",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = when (followVisualPolicy.relatedBadgeTone) {
                                            FollowBadgeTone.PRIMARY -> MaterialTheme.colorScheme.primary
                                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            } else {
                                null
                            },
                            leadingContent = if (
                                com.android.purebilibili.core.ui.LocalUpBadgeVisibility.current.showAvatars &&
                                video.owner.face.isNotEmpty()
                            ) {
                                {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(FormatUtils.fixImageUrl(video.owner.face))
                                            .crossfade(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                    )
                                }
                            } else {
                                null
                            },
                            nameStyle = MaterialTheme.typography.labelMedium,
                            nameColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            badgeBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            showUpBadge = showUpBadge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalVideoStatRow(
                        playText = FormatUtils.formatStat(video.stat.view.toLong()),
                        danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                    )
                }
            },
            trailingContent = onMoreClick?.let { moreClick ->
                {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                moreHaptic(HapticType.LIGHT)
                                moreClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = "⋮",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
internal fun RelatedVideoGridRow(
    videos: List<RelatedVideo>,
    cardLayout: HomeFeedCardLayout,
    cardPresentation: RelatedVideoCardLayout? = null,
    followingMids: Set<Long> = emptySet(),
    showUpBadge: Boolean = true,
    onVideoClick: (RelatedVideo) -> Unit,
    onVideoHidden: ((RelatedVideo) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var actionVideo by remember { mutableStateOf<RelatedVideo?>(null) }
    var blockCreatorRequest by remember {
        mutableStateOf<RelatedVideoBlockRequest?>(null)
    }
    var isBlockingCreator by remember { mutableStateOf(false) }
    val blockedUpRepository = remember(context) { BlockedUpRepository(context) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = cardLayout.outerPaddingDp.dp, vertical = 2.dp)
    ) {
        val resolvedCardPresentation = cardPresentation
            ?: resolveRelatedVideoCardLayout(maxWidth.value.toInt())
        Row(modifier = Modifier.fillMaxWidth()) {
        videos.firstOrNull()?.let { video ->
            RelatedVideoItem(
                video = video,
                isFollowed = video.owner.mid in followingMids,
                showUpBadge = showUpBadge,
                coverAspectRatio = RELATED_VIDEO_CARD_COVER_ASPECT_RATIO,
                stacked = resolvedCardPresentation == RelatedVideoCardLayout.STACKED,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onVideoClick(video) },
                onMoreClick = { actionVideo = video }
            )
        }
        }
    }

    val pendingVideo = actionVideo
    if (pendingVideo != null) {
        RelatedVideoActionSheet(
            video = pendingVideo,
            onWatchLater = {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        ActionRepository.toggleWatchLater(pendingVideo.aid, true)
                    }
                    val message = result.fold(
                        onSuccess = { "已添加到稍后再看" },
                        onFailure = { error -> error.message ?: "添加失败" }
                    )
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onReasonSelected = { reason ->
                if (reason.localAction == RecommendationFeedbackLocalAction.CREATOR &&
                    pendingVideo.owner.mid > 0L
                ) {
                    blockCreatorRequest = RelatedVideoBlockRequest(pendingVideo, reason)
                } else {
                    scope.launch {
                        recordRelatedVideoFeedback(
                            context = context,
                            video = pendingVideo,
                            reason = reason
                        )
                        if (shouldRemoveRelatedVideoAfterFeedback(reason)) {
                            onVideoHidden?.invoke(pendingVideo)
                        }
                        Toast.makeText(
                            context,
                            resolveRelatedFeedbackToast(reason),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismissRequest = { actionVideo = null }
        )
    }

    val blockRequest = blockCreatorRequest
    if (blockRequest != null) {
        AppAlertDialog(
            onDismissRequest = {
                if (!isBlockingCreator) {
                    blockCreatorRequest = null
                }
            },
            title = { AppText("屏蔽 UP 主") },
            text = {
                AppText(
                    "确定要屏蔽 ${blockRequest.video.owner.name.ifBlank { "该 UP 主" }} 吗？\n" +
                        "屏蔽后将不再推荐该 UP 主的视频。"
                )
            },
            confirmButton = {
                AppDialogAction(
                    onClick = {
                        if (!isBlockingCreator) {
                            isBlockingCreator = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    blockedUpRepository.blockUpWithBilibiliSync(
                                        mid = blockRequest.video.owner.mid,
                                        name = blockRequest.video.owner.name,
                                        face = blockRequest.video.owner.face
                                    )
                                }
                                recordRelatedVideoFeedback(
                                    context = context,
                                    video = blockRequest.video,
                                    reason = blockRequest.reason
                                )
                                onVideoHidden?.invoke(blockRequest.video)
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                isBlockingCreator = false
                                blockCreatorRequest = null
                            }
                        }
                    }
                ) {
                    AppText("屏蔽", color = Color.Red)
                }
            },
            dismissButton = {
                AppDialogAction(
                    onClick = {
                        if (!isBlockingCreator) {
                            blockCreatorRequest?.let { request ->
                                blockCreatorRequest = null
                                scope.launch {
                                    recordRelatedVideoFeedback(
                                        context = context,
                                        video = request.video,
                                        reason = request.reason
                                    )
                                    onVideoHidden?.invoke(request.video)
                                    Toast.makeText(
                                        context,
                                        resolveRelatedFeedbackToast(request.reason),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                ) {
                    AppText("暂不屏蔽")
                }
            }
        )
    }
}

private data class RelatedVideoBlockRequest(
    val video: RelatedVideo,
    val reason: RecommendationFeedbackReason,
)

private suspend fun recordRelatedVideoFeedback(
    context: android.content.Context,
    video: RelatedVideo,
    reason: RecommendationFeedbackReason
) {
    withContext(Dispatchers.IO) {
        val includeCreator = reason.localAction == RecommendationFeedbackLocalAction.CREATOR &&
            video.owner.mid > 0L
        val keywords = when (reason.localAction) {
            RecommendationFeedbackLocalAction.SIMILAR_CONTENT ->
                extractRelatedFeedbackKeywords(video.title)
            else -> emptySet()
        }
        val snapshot = TodayWatchFeedbackStore.getSnapshot(context).withDislikedVideoFeedback(
            video = TodayWatchDislikedVideoSnapshot(
                bvid = video.bvid,
                title = video.title,
                creatorName = video.owner.name,
                creatorMid = video.owner.mid,
                dislikedAtMillis = System.currentTimeMillis()
            ),
            keywords = keywords,
            includeCreatorSignal = includeCreator
        )
        TodayWatchFeedbackStore.saveSnapshot(context, snapshot)
    }
}
