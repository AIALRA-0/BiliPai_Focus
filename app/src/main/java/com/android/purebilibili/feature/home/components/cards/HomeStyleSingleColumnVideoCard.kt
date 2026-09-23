package com.android.purebilibili.feature.home.components.cards

import coil3.request.crossfade

import com.android.purebilibili.core.ui.MediaContrastPalette

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.FeedTitleHierarchy
import com.android.purebilibili.core.ui.feedContentTypography
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.videoCardTitleMaxLines
import com.android.purebilibili.core.ui.videoCardTitleOverflow
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceCoverPresentation
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.rememberNativeVideoCardSnapshotController
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.adaptive.adaptiveCardHoverEffect
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.ui.transition.withMeasuredCoverDecodeSize
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.home.resolveHomeCoverRequestSpec
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeStyleSingleColumnVideoCard(
    video: VideoItem,
    sourceRoute: String,
    @Suppress("UNUSED_PARAMETER") coverAspectRatio: Float,
    transitionEnabled: Boolean,
    sharedTransitionEnabled: Boolean = transitionEnabled,
    isFollowing: Boolean = false,
    showUpBadge: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    highlightedTitle: androidx.compose.ui.text.AnnotatedString? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val isLongPressEnabled = com.android.purebilibili.core.ui.LocalVideoCardLongPressEnabled.current
    val contentTypography = feedContentTypography(FeedTitleHierarchy.Standard)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val densityValue = density.density
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val latestOnClick = rememberUpdatedState(onClick)
    val clickScope = rememberCoroutineScope()
    var forceSharedTransitionForClick by remember { mutableStateOf(false) }
    val effectiveTransitionEnabled = transitionEnabled || forceSharedTransitionForClick
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedReady = effectiveTransitionEnabled &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val motionSettings = LocalVideoSharedTransitionSpeedSettings.current
    val transitionAdaptiveInfo = com.android.purebilibili.core.ui.transition
        .LocalVideoTransitionAdaptiveInfo.current
    val motionSpec = remember(
        sourceRoute,
        effectiveTransitionEnabled,
        motionSettings,
        transitionAdaptiveInfo,
    ) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRoute,
            transitionEnabled = effectiveTransitionEnabled,
            speedSettings = motionSettings,
            adaptiveInfo = transitionAdaptiveInfo,
        )
    }
    val cardBounds = remember { object { var value: Rect? = null } }
    val coverBounds = remember { object { var value: Rect? = null } }
    val nativeCardSnapshot = rememberNativeVideoCardSnapshotController(video.bvid)
    val cardShape = AppShapes.container(ContainerLevel.Card)
    val cardCornerDp = AppShapes.containerCornerDp(ContainerLevel.Card)
    val useCardShellSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = sharedReady,
    )
    val coverRequestSpec = remember(densityValue) {
        resolveHomeCoverRequestSpec(
            cardWidthDp = HORIZONTAL_VIDEO_CARD_COVER_WIDTH_DP.toFloat(),
            density = densityValue,
            useLowQualityCover = false,
        )
    }
    val stationaryCoverUrl = remember(video.pic, coverRequestSpec) {
        coverRequestSpec.resolveUrl(video.pic)
    }
    val coverRequest = remember(stationaryCoverUrl, coverRequestSpec) {
        ImageRequest.Builder(context)
            .data(stationaryCoverUrl)
            .size(coverRequestSpec.widthPx, coverRequestSpec.heightPx)
            .crossfade(false)
            .memoryCacheKey(stationaryCoverUrl)
            .diskCacheKey(stationaryCoverUrl)
            .build()
    }
    val triggerClick = {
        cardBounds.value?.let { bounds ->
            val sourceCoverBounds = coverBounds.value
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = density.density,
                sourceCornerDp = cardCornerDp.value.roundToInt(),
                coverBounds = sourceCoverBounds,
                sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
                sourceChromeSnapshot = VideoCardSourceChromeSnapshot(
                    title = video.title,
                    ownerName = video.owner.name,
                    ownerFaceUrl = video.owner.face,
                    viewText = FormatUtils.formatStat(video.stat.view.toLong()),
                    danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                    durationText = FormatUtils.formatDuration(video.duration),
                    followed = isFollowing,
                    // Single-column paints play/danmaku in the info column (not on cover only).
                    infoPresentation = com.android.purebilibili.core.ui.transition
                        .resolveVideoCardSourceInfoPresentation(
                            publishTimeText = "",
                            showStatsInInfo = true,
                            showOverflowMenu = onMoreClick != null || trailingContent != null,
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
        if (sharedTransitionEnabled && !transitionEnabled) {
            forceSharedTransitionForClick = true
            clickScope.launch {
                withFrameNanos { }
                withFrameNanos { }
                latestOnClick.value()
            }
        } else {
            latestOnClick.value()
        }
        Unit
    }
    val moreHaptic = rememberHapticFeedback()
    HorizontalVideoCardFrame(
        modifier = modifier
            .adaptiveCardHoverEffect(shape = cardShape)
            .onGloballyPositioned { coordinates ->
                cardBounds.value = coordinates.boundsInRoot()
            }
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                motionSpec = motionSpec,
                clipShape = cardShape,
                crossfadeSourceContent = true,
            )
            .clip(cardShape)
            .then(nativeCardSnapshot.modifier)
            .background(AppSurfaceTokens.cardContainer())
            .combinedClickable(
                onClick = triggerClick,
                onLongClick = if (isLongPressEnabled) onLongClick else null,
            ),
        coverModifier = Modifier.onGloballyPositioned { coordinates ->
            coverBounds.value = coordinates.boundsInRoot()
        },
        coverOverlayModifier = nativeCardSnapshot.coverOverlayModifier,
        coverContent = {
            AsyncImage(
                model = coverRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        coverOverlayContent = {
            AppText(
                text = FormatUtils.formatDuration(video.duration),
                color = MediaContrastPalette.Foreground,
                style = contentTypography.coverBadge.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(
                        color = MediaContrastPalette.Scrim.copy(alpha = 0.64f),
                        blurRadius = 4f,
                        offset = Offset(0f, 1f),
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro),
            )
        },
        infoContent = {
            AppText(
                text = highlightedTitle ?: androidx.compose.ui.text.AnnotatedString(video.title),
                modifier = Modifier.fillMaxWidth(),
                style = contentTypography.title,
                maxLines = videoCardTitleMaxLines(),
                overflow = videoCardTitleOverflow(),
                color = MaterialTheme.colorScheme.onSurface,
            )

            UpBadgeName(
                name = video.owner.name,
                inlineTrailingContent = if (isFollowing) {
                    {
                        AppText(
                            text = "已关注",
                            style = contentTypography.coverBadge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
                nameStyle = contentTypography.author,
                nameColor = MaterialTheme.colorScheme.onSurfaceVariant,
                badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                badgeBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                showUpBadge = showUpBadge,
                // The info column is a single horizontal-card row; keep the owner on one
                // measured line so a long name cannot grow past the cover bounds.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalVideoStatRow(
                playText = FormatUtils.formatStat(video.stat.view.toLong()),
                danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
            )
        },
        trailingContent = when {
            trailingContent != null -> {
                {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = AppSpacingTokens.Small,
                                bottom = AppSpacingTokens.ExtraSmall,
                            ),
                    ) { trailingContent() }
                }
            }
            onMoreClick != null -> {
                {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = AppSpacingTokens.Micro, bottom = AppSpacingTokens.Micro)
                            .size(AppChromeSizeTokens.MinimumTouchTarget)
                            .clip(CircleShape)
                            .semantics { contentDescription = "更多操作" }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                moreHaptic(HapticType.LIGHT)
                                onMoreClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = "⋮",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            else -> null
        },
    )
}
