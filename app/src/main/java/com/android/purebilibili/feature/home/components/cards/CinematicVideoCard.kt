package com.android.purebilibili.feature.home.components.cards

import coil3.request.crossfade

import com.android.purebilibili.core.ui.MediaContrastPalette
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.videoCardTitleMaxLines
import com.android.purebilibili.core.ui.videoCardTitleOverflow
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.FeedTitleHierarchy
import com.android.purebilibili.core.ui.feedContentTypography
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.adaptiveCardHoverEffect
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalMiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO
import com.android.purebilibili.core.ui.transition.rememberNativeVideoCardSnapshotController
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoSharedCoverCacheKey
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.data.model.response.VideoItem
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.blur
import com.android.purebilibili.feature.home.LocalHomeScrollOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.PlayCircle
import com.android.purebilibili.feature.home.resolveHomeCardEnterAnimationEnabledAtMount
import kotlin.math.roundToInt

/**
 * 沉浸式视频卡片 (Cinematic Mode)
 * 全屏大图 + 底部文字遮罩，提供类似电影海报的沉浸体验
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CinematicVideoCard(
    video: VideoItem,
    index: Int,
    isFollowing: Boolean = false,
    animationEnabled: Boolean = true,
    motionTier: MotionTier = MotionTier.Normal,
    transitionEnabled: Boolean = false,
    sharedElementSourceRoute: String? = null,
    isReturningFromVideoDetail: Boolean = false,
    isQuickReturningFromVideoDetail: Boolean = false,
    scrollLiteModeEnabled: Boolean = false,
    isDataSaverActive: Boolean = false,
    preferLowQualityCover: Boolean = false,
    showUpBadge: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    onWatchLater: (() -> Unit)? = null,
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val contentTypography = feedContentTypography(FeedTitleHierarchy.Prominent)
    
    val cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.ProminentCard)
    val cardShape = AppShapes.container(ContainerLevel.ProminentCard)

    var showDismissMenu by remember { mutableStateOf(false) }

    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverUrl = remember(video.bvid, useLowQualityCover) {
        FormatUtils.resolveVideoCoverUrl(
            if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic,
            useLowQuality = useLowQualityCover
        )
    }

    // 记录位置
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val densityValue = density.density
    // 记录卡片位置（非 Compose State，避免滚动时触发高频重组）
    val cardBoundsRef = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val coverBoundsRef = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val nativeCardSnapshot = rememberNativeVideoCardSnapshotController(video.bvid)
    val localSharedElementSourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val effectiveSharedElementSourceRoute = remember(sharedElementSourceRoute, localSharedElementSourceRoute) {
        sharedElementSourceRoute ?: localSharedElementSourceRoute
    }
    val effectiveTransitionEnabled = transitionEnabled
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val transitionAdaptiveInfo = com.android.purebilibili.core.ui.transition
        .LocalVideoTransitionAdaptiveInfo.current
    val cardSharedTransitionMotionSpec = remember(
        effectiveSharedElementSourceRoute,
        effectiveTransitionEnabled,
        sharedTransitionSpeedSettings,
        transitionAdaptiveInfo,
    ) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = effectiveSharedElementSourceRoute,
            transitionEnabled = effectiveTransitionEnabled,
            speedSettings = sharedTransitionSpeedSettings,
            adaptiveInfo = transitionAdaptiveInfo,
        )
    }
    val coverCacheKey = remember(video.bvid, useLowQualityCover) {
        resolveVideoSharedCoverCacheKey(video.bvid, useLowQualityCover)
    }
    val triggerCardClick = {
        cardBoundsRef.value?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = densityValue,
                sourceCornerDp = cardCornerRadius.value.roundToInt(),
                coverBounds = coverBoundsRef.value,
                sourceLayout = com.android.purebilibili.core.ui.transition.VideoCardSourceLayout.STACKED,
                sourceChromeSnapshot = com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot(
                    title = video.title,
                    ownerName = video.owner.name,
                    ownerFaceUrl = video.owner.face,
                    viewText = FormatUtils.formatStat(video.stat.view.toLong()),
                    danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                    durationText = FormatUtils.formatDuration(video.duration),
                    infoPresentation = com.android.purebilibili.core.ui.transition
                        .resolveVideoCardSourceInfoPresentation(
                            publishTimeText = "",
                            showStatsInInfo = false,
                        ),
                    coverUrl = coverUrl,
                    coverCacheKey = coverCacheKey,
                ),
            )
            nativeCardSnapshot.capture()
        }
        onClick(video.bvid, 0)
    }
    
    // 共享元素
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = effectiveTransitionEnabled,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null
    )
    val isQuickReturnLimited = isReturningFromVideoDetail && isQuickReturningFromVideoDetail
    val useCardShellSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = effectiveSharedElementSourceRoute,
        transitionEnabled = coverSharedEnabled
    )
    val isSharedReturnTarget = remember(
        video.bvid,
        effectiveSharedElementSourceRoute,
        CardPositionManager.lastClickedVideoSourceKey,
    ) {
        isVideoCardSharedReturnTarget(
            bvid = video.bvid,
            sourceRoute = effectiveSharedElementSourceRoute,
            lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
        )
    }
    val coverCrossfadeEnabled = shouldEnableVideoCardCoverCrossfade(
        isScrollInProgress = false,
        isReturningFromDetail = isReturningFromVideoDetail,
        useCoverSharedBounds = useCardShellSharedBounds ||
            (LocalMiuixVideoCardTransitionState.current.enabled && isSharedReturnTarget),
        isSharedReturnTarget = isSharedReturnTarget,
    )
    val cardShellShape = cardShape
    val enterAnimationEnabledAtMount = remember(video.bvid) {
        resolveHomeCardEnterAnimationEnabledAtMount(
            baseAnimationEnabled = animationEnabled,
            isReturningFromDetail = isReturningFromVideoDetail,
            isSwitchingCategory = CardPositionManager.isSwitchingCategory,
            isScrollInProgress = scrollLiteModeEnabled
        )
    }
    val coordinateEnterWithTransition = remember(animationEnabled, transitionEnabled) {
        animationEnabled && transitionEnabled
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .adaptiveCardHoverEffect(shape = cardShellShape)
            .padding(bottom = AppSpacingTokens.ExtraLarge, start = AppSpacingTokens.Large, end = AppSpacingTokens.Large) // 增加间距
            .animateEnter(
                index = index,
                key = Unit,
                animationEnabled = enterAnimationEnabledAtMount,
                motionTier = motionTier,
                coordinateWithSharedTransition = coordinateEnterWithTransition
            )
            .onGloballyPositioned { coordinates ->
                cardBoundsRef.value = coordinates.boundsInRoot()
            }
    ) {
        // 卡片主体容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .videoCardShellSharedBoundsOrEmpty(
                    enabled = useCardShellSharedBounds,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    bvid = video.bvid,
                    sourceRoute = effectiveSharedElementSourceRoute,
                    motionSpec = cardSharedTransitionMotionSpec,
                    clipShape = cardShellShape
                )
                .clip(cardShape)
                .then(nativeCardSnapshot.modifier)
                .background(MediaContrastPalette.Scrim) // 纯黑底色
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                             if (onDismiss != null || onWatchLater != null) {
                                haptic(HapticType.HEAVY)
                                showDismissMenu = true
                             }
                        },
                        onTap = {
                            triggerCardClick()
                        }
                    )
                }
        ) {
            val coverModifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIDEO_SHARED_COVER_ASPECT_RATIO) // 统一共享比例
                .onGloballyPositioned { coordinates ->
                    coverBoundsRef.value = coordinates.boundsInRoot()
                }
                .videoCardShellReturnCoverAlpha(
                    enabled = useCardShellSharedBounds,
                    bvid = video.bvid,
                    sourceRoute = effectiveSharedElementSourceRoute,
                    isReturningFromDetail = isReturningFromVideoDetail,
                )
            
            Box(modifier = Modifier.clip(cardShape)) {
                 AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .placeholderMemoryCacheKey(coverCacheKey)
                        .crossfade(coverCrossfadeEnabled)
                        .memoryCacheKey(coverCacheKey)
                        .diskCacheKey(coverCacheKey)
                        .build(),
                    contentDescription = null,
                    modifier = coverModifier,
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(nativeCardSnapshot.coverOverlayModifier),
            ) {
            // 2. 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacingTokens.TripleExtraLarge * 4 - AppSpacingTokens.Medium)
                    .align(Alignment.BottomCenter)
                    .videoCardShellReturnCoverAlpha(
                        enabled = useCardShellSharedBounds,
                        bvid = video.bvid,
                        sourceRoute = effectiveSharedElementSourceRoute,
                        isReturningFromDetail = isReturningFromVideoDetail,
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MediaContrastPalette.Scrim.copy(alpha = 0.1f),
                                MediaContrastPalette.Scrim.copy(alpha = 0.5f),
                                MediaContrastPalette.Scrim.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // 3. 内容层
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacingTokens.Large)
                    .videoCardShellReturnChromeAlpha(
                        enabled = useCardShellSharedBounds,
                        bvid = video.bvid,
                        sourceRoute = effectiveSharedElementSourceRoute,
                        isReturningFromDetail = isReturningFromVideoDetail,
                        isQuickReturnFromDetail = isQuickReturningFromVideoDetail,
                    )
            ) {
                AppText(
                    text = video.title,
                    maxLines = videoCardTitleMaxLines(),
                    overflow = videoCardTitleOverflow(),
                    style = contentTypography.title.copy(color = MediaContrastPalette.Foreground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "视频标题: ${video.title}" }
                )

                Spacer(modifier = Modifier.height(AppSpacingTokens.Small))

                // 数据层 (一直显示)
                Row(
                    modifier = Modifier.padding(end = if (onDismiss != null || onWatchLater != null) {
                        AppChromeSizeTokens.MinimumTouchTarget
                    } else AppSpacingTokens.None),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
                ) {
                     UpBadgeName(
                         name = video.owner.name,
                         leadingContent = if (video.owner.face.isNotEmpty()) {
                             {
                                 AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(FormatUtils.fixImageUrl(video.owner.face))
                                        .size(64)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall)
                                        .clip(CircleShape)
                                        .background(MediaContrastPalette.Foreground.copy(alpha = 0.2f)),
                                    contentScale = ContentScale.Crop
                                 )
                             }
                         } else null,
                         nameStyle = contentTypography.author,
                         nameColor = MediaContrastPalette.Foreground.copy(alpha = 0.78f),
                         badgeTextColor = MediaContrastPalette.Foreground.copy(alpha = 0.92f),
                         badgeBorderColor = MediaContrastPalette.Foreground.copy(alpha = 0.45f),
                         showUpBadge = showUpBadge,
                         maxLines = Int.MAX_VALUE,
                         overflow = TextOverflow.Visible,
                         modifier = Modifier.weight(1f)
                     )
                     
                    HorizontalVideoStatRow(
                        playText = FormatUtils.formatStat(video.stat.view.toLong()),
                        danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                        contentColor = MediaContrastPalette.Foreground.copy(alpha = 0.8f),
                    )

                    // 时长
                    AppText(
                        text = FormatUtils.formatDuration(video.duration),
                        style = contentTypography.coverBadge.copy(
                            color = MediaContrastPalette.Foreground.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            }
        }
        
        // 更多操作按钮（右下角）
         val hasMenu = onDismiss != null || onWatchLater != null
         if (hasMenu) {
             Box(
                 modifier = Modifier
                     .align(Alignment.BottomEnd)
                     .padding(AppSpacingTokens.Medium)
                     .size(AppChromeSizeTokens.MinimumTouchTarget)
                     .semantics { contentDescription = "更多操作" }
                     .clickable { 
                         haptic(HapticType.LIGHT)
                         showDismissMenu = true 
                     },
                 contentAlignment = Alignment.Center
             ) {
                 Box(
                     modifier = Modifier
                         .size(AppSpacingTokens.ExtraLarge)
                         .background(MediaContrastPalette.Scrim.copy(alpha = 0.3f), CircleShape),
                     contentAlignment = Alignment.Center,
                 ) {
                      AppText(
                          text = "⋮",
                          color = MediaContrastPalette.Foreground,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(bottom = AppSpacingTokens.Micro)
                      )
                 }
             }
         }
    }


    // 长按菜单
    AppDropdownMenu(
        expanded = showDismissMenu,
        onDismissRequest = { showDismissMenu = false }
    ) {
        if (onWatchLater != null) {
            AppDropdownMenuItem(
                text = { AppText("🕐 稍后再看") },
                onClick = {
                    showDismissMenu = false
                    onWatchLater.invoke()
                }
            )
        }
        if (onDismiss != null) {
            AppDropdownMenuItem(
                text = { AppText("🚫 不感兴趣") },
                onClick = {
                    showDismissMenu = false
                    onDismiss.invoke()
                }
            )
        }
    }
}
