// 文件路径: feature/home/components/cards/StoryVideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.MediaContrastPalette
import com.android.purebilibili.core.ui.FeedTitleHierarchy
import com.android.purebilibili.core.ui.feedContentTypography

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.iOSCardTapEffect
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween

import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.videoCardTitleMaxLines
import com.android.purebilibili.core.ui.videoCardTitleOverflow
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.store.HomeDurationStyle
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.adaptiveCardHoverEffect
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.resolveUpStatsText
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalMiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.rememberNativeVideoCardSnapshotController
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.feature.home.HomeCoverRequestSpec
import com.android.purebilibili.feature.home.resolveHomeCardEnterAnimationEnabledAtMount
import com.android.purebilibili.feature.video.ui.section.resolveCompactPublishTimeRowText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import kotlin.math.roundToInt

/**
 *  故事卡片 - 影院海报风格
 * 
 * 特点：
 * - 封面比例由首页卡片样式统一配置
 * - 大圆角 (24dp)
 * - 标题叠加在封面底部
 * - 沉浸电影感
 */
@Composable
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
internal fun StoryVideoCard(
    video: VideoItem,
    index: Int = 0,  //  [新增] 索引用于动画延迟
    animationEnabled: Boolean = true,  //  卡片动画开关
    motionTier: MotionTier = MotionTier.Normal,
    transitionEnabled: Boolean = false, //  卡片过渡动画开关
    sharedElementSourceRoute: String? = null,
    isReturningFromVideoDetail: Boolean = false,
    isQuickReturningFromVideoDetail: Boolean = false,
    scrollLiteModeEnabled: Boolean = false,
    isDataSaverActive: Boolean = false,
    preferLowQualityCover: Boolean = false,
    coverRequestSpec: HomeCoverRequestSpec? = null,
    showCoverGlassBadges: Boolean = false,
    showInfoGlassBadges: Boolean = false,
    showUpBadge: Boolean? = null,
    showUpAvatar: Boolean? = null,
    homeDurationStyle: HomeDurationStyle = HomeDurationStyle.OUTSIDE_COVER,
    coverAspectRatio: Float = 4f / 3f,
    cardHorizontalPadding: Dp = AppSpacingTokens.None,
    compactMetadata: Boolean = true,
    titleMinLines: Int = 1,
    titleMaxLines: Int = 2,
    showOnlineCount: Boolean = false,
    showPublishTime: Boolean = false,
    upFollowerCount: Int? = null,
    upVideoCount: Int? = null,
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onUpClick: ((Long) -> Unit)? = null,
    onLongClick: ((VideoItem) -> Unit)? = null, // [修复] 长按预览回调
    onClick: (String, Long) -> Unit
) {
    val contentTypography = feedContentTypography(
        titleHierarchy = if (compactMetadata) {
            FeedTitleHierarchy.Compact
        } else {
            FeedTitleHierarchy.Standard
        },
    )
    val haptic = rememberHapticFeedback()
    
    val isLongPressEnabled = com.android.purebilibili.core.ui.LocalVideoCardLongPressEnabled.current
    val cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.ProminentCard)
    val cardShape = AppShapes.container(ContainerLevel.ProminentCard)
    val coverShape = cardShape
    val durationText = remember(video.duration) { FormatUtils.formatDuration(video.duration) }
    val showDurationOnCover = homeDurationStyle == HomeDurationStyle.OVERLAY_TEXT_ONLY
    val coverOverlayTextStyle = remember {
        TextStyle(shadow = resolveVideoCardCoverOverlayTextShadow())
    }
    val showDurationOutside = homeDurationStyle == HomeDurationStyle.OUTSIDE_COVER
    val scrollLitePolicy = remember {
        resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false
        )
    }
    val badgeStylePolicy = remember(showCoverGlassBadges, showInfoGlassBadges) {
        resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = showCoverGlassBadges,
            showInfoGlassBadges = showInfoGlassBadges
        )
    }
    val premiumBadgeLabel = remember(video.rights) {
        resolveVideoPremiumBadgeLabel(video.rights)
    }
    val onlineCount = rememberVideoCardOnlineCount(
        video = video,
        showOnlineCount = showOnlineCount
    )
    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverUrl = remember(video.bvid, video.pic, useLowQualityCover, coverRequestSpec) {
        coverRequestSpec?.resolveUrl(video.pic) ?: FormatUtils.resolveVideoCoverUrl(
            video.pic,
            useLowQuality = useLowQualityCover,
        )
    }
    val coverCacheKey = remember(video.bvid, video.pic, coverRequestSpec) {
        "story_${video.bvid.ifBlank { video.pic }}_${coverRequestSpec?.cacheKeySuffix ?: "default"}"
    }
    val publishTimeRowText = remember(showPublishTime, video.pubdate) {
        if (!showPublishTime) {
            ""
        } else {
            resolveCompactPublishTimeRowText(pubdate = video.pubdate)
        }
    }
    val emphasizePublishTime = false
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    
    //  获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val localSharedElementSourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val effectiveSharedElementSourceRoute = remember(sharedElementSourceRoute, localSharedElementSourceRoute) {
        sharedElementSourceRoute ?: localSharedElementSourceRoute
    }
    
    //  记录卡片位置
    var cardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var coverBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val nativeCardSnapshot = rememberNativeVideoCardSnapshotController(video.bvid)
    val triggerCardClick = {
        cardBounds?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                isSingleColumn = !transitionEnabled,
                sourceCornerDp = cardCornerRadius.value.roundToInt(),
                coverBounds = coverBounds,
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
                    coverDecodeWidthPx = coverRequestSpec?.widthPx ?: 0,
                    coverDecodeHeightPx = coverRequestSpec?.heightPx ?: 0,
                ),
            )
            nativeCardSnapshot.capture()
        }
        onClick(video.bvid, 0)
    }
    
    //  尝试获取共享元素作用域
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val effectiveTransitionEnabled = transitionEnabled
    val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = effectiveTransitionEnabled && LocalSharedTransitionEnabled.current,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .adaptiveCardHoverEffect(shape = cardShellShape)
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                motionSpec = cardSharedTransitionMotionSpec,
                clipShape = cardShellShape
            )
            .padding(horizontal = cardHorizontalPadding, vertical = AppSpacingTokens.Small)
            .animateEnter(
                index = index,
                key = Unit,
                animationEnabled = enterAnimationEnabledAtMount,
                motionTier = motionTier,
                coordinateWithSharedTransition = coordinateEnterWithTransition
            )
            //  [新增] 记录卡片位置
            .then(nativeCardSnapshot.modifier)
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInRoot()
            }
            .pointerInput(isLongPressEnabled, onDismiss, onLongClick) {
                 val hasLongPressAction = isLongPressEnabled && (onDismiss != null || onLongClick != null)
                 if (hasLongPressAction) {
                     detectTapGestures(
                         onLongPress = {
                             if (onLongClick != null) {
                                 haptic(HapticType.HEAVY)
                                 onLongClick(video)
                             } else if (onDismiss != null) {
                                 haptic(HapticType.HEAVY)
                                 showDismissMenu = true
                             }
                         },
                         onTap = {
                             triggerCardClick()
                         }
                     )
                 }
            }
            .then(
                 if (!isLongPressEnabled || (onDismiss == null && onLongClick == null)) {
                     Modifier.iOSCardTapEffect(
                         pressScale = 1f,
                         pressTranslationY = 0f,
                         hapticEnabled = true
                     ) {
                         triggerCardClick()
                     }
                 } else Modifier
            )
    ) {
        // 卡片容器 (封面)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .videoCardShellReturnCoverAlpha(
                    enabled = useCardShellSharedBounds,
                    bvid = video.bvid,
                    sourceRoute = effectiveSharedElementSourceRoute,
                    isReturningFromDetail = isReturningFromVideoDetail,
                )
                .testTag("home_story_video_cover")
                .aspectRatio(coverAspectRatio)
                .onGloballyPositioned { coordinates ->
                    coverBounds = coordinates.boundsInRoot()
                }
                .clip(coverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant) // 封面占位色
        ) {
            // 封面比例由首页卡片样式统一配置。
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .apply {
                        coverRequestSpec?.let { size(it.widthPx, it.heightPx) }
                    }
                    .crossfade(coverCrossfadeEnabled)
                    .memoryCacheKey(coverCacheKey)
                    .diskCacheKey(coverCacheKey)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(nativeCardSnapshot.coverOverlayModifier),
            ) {
            if (premiumBadgeLabel != null) {
                HomeVideoBadgePill(
                    style = badgeStylePolicy.coverStyle,
                    shape = AppShapes.container(ContainerLevel.Chip),
                    containerColor = BiliPink.copy(alpha = 0.82f),
                    borderColor = MediaContrastPalette.Foreground.copy(alpha = 0.24f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppSpacingTokens.Small)
                ) {
                    AppText(
                        text = premiumBadgeLabel,
                        color = MediaContrastPalette.Foreground,
                        style = contentTypography.coverBadge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
            
            //  时长标签 (保留在封面上)
            if (showDurationOnCover) {
                AppText(
                    text = durationText,
                    color = MediaContrastPalette.Foreground,
                    style = contentTypography.coverBadge
                        .copy(fontWeight = FontWeight.Medium)
                        .merge(coverOverlayTextStyle),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AppSpacingTokens.Small + AppSpacingTokens.Micro)
                )
            }
            }
        }
        
        Column(
            modifier = Modifier.videoCardShellReturnChromeAlpha(
                enabled = useCardShellSharedBounds,
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                isReturningFromDetail = isReturningFromVideoDetail,
                isQuickReturnFromDetail = isQuickReturningFromVideoDetail,
            )
        ) {
        Spacer(modifier = Modifier.height(if (compactMetadata) AppSpacingTokens.Small else AppSpacingTokens.Medium))
        
        AppText(
            text = video.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = contentTypography.title,
            minLines = titleMinLines,
            maxLines = videoCardTitleMaxLines(titleMaxLines),
            overflow = videoCardTitleOverflow(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(if (compactMetadata) AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro else AppSpacingTokens.Small))
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (scrollLitePolicy.showSecondaryStatsRow && video.stat.view > 0) {
                FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HORIZONTAL_VIDEO_STAT_ROW_SPACING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(HORIZONTAL_VIDEO_STAT_WRAP_SPACING_DP.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalVideoStatRow(
                        playText = FormatUtils.formatStat(video.stat.view.toLong()),
                        danmakuText = if (video.stat.danmaku > 0) {
                            FormatUtils.formatStat(video.stat.danmaku.toLong())
                        } else {
                            ""
                        },
                    )
                    if (publishTimeRowText.isNotBlank()) {
                        AppText(
                            text = publishTimeRowText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            style = contentTypography.statistic,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
            }

            val upClickMid = video.owner.mid.takeIf { it > 0L && onUpClick != null }
            val upNameModifier = if (upClickMid != null) {
                Modifier.fillMaxWidth().clickable { onUpClick?.invoke(upClickMid) }
            } else {
                Modifier.fillMaxWidth()
            }

            UpBadgeName(
                name = video.owner.name,
                metaText = resolveUpStatsText(
                    followerCount = upFollowerCount,
                    videoCount = upVideoCount
                ),
                leadingContent = if ((showUpAvatar ?: com.android.purebilibili.core.ui.LocalUpBadgeVisibility.current.showAvatars) && video.owner.face.isNotEmpty()) {
                    {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(video.owner.face))
                                .size(96, 96)
                                .crossfade(100)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(AppSpacingTokens.ExtraLarge)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else null,
                nameStyle = contentTypography.author.copy(fontWeight = FontWeight.Medium),
                nameColor = MaterialTheme.colorScheme.onSurfaceVariant,
                metaColor = MaterialTheme.colorScheme.primary,
                badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                badgeBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                showUpBadge = showUpBadge,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible,
                metaMaxLines = Int.MAX_VALUE,
                metaOverflow = TextOverflow.Visible,
                modifier = upNameModifier
            )

            if (!scrollLitePolicy.showSecondaryStatsRow) {
                VideoCardDurationPublishRow(
                    durationText = "",
                    publishTimeText = publishTimeRowText,
                    emphasizePublishTime = emphasizePublishTime,
                    publishTimeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    topSpacing = if (compactMetadata) AppSpacingTokens.ExtraSmall else AppSpacingTokens.Small
                )
            }

        }
        }
    }
    
    //  [新增] 长按删除菜单
    AppDropdownMenu(
        expanded = showDismissMenu,
        onDismissRequest = { showDismissMenu = false }
    ) {
        AppDropdownMenuItem(
            text = { 
                AppText(
                    "🚫 不感兴趣",
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            onClick = {
                showDismissMenu = false
                onDismiss?.invoke()
            }
        )
    }
}
