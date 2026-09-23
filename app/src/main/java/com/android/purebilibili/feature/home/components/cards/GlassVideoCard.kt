// 文件路径: feature/home/components/cards/GlassVideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppSurface

import com.android.purebilibili.core.ui.MediaContrastPalette
import com.android.purebilibili.core.ui.videoCardTitleMaxLines
import com.android.purebilibili.core.ui.videoCardTitleOverflow

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.iOSCardTapEffect
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.adaptiveCardHoverEffect
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.feedContentTypography
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalMiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO
import com.android.purebilibili.core.ui.transition.rememberNativeVideoCardSnapshotController
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoSharedCoverCacheKey
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.feature.home.resolveHomeCardEnterAnimationEnabledAtMount
import kotlin.math.roundToInt
import com.android.purebilibili.feature.home.rememberHomeGlassPillColors
import com.android.purebilibili.feature.home.resolveHomeGlassCoverPillBaseColor
import com.android.purebilibili.feature.home.HomeVisualPalette

/**
 *  玻璃拟态卡片 - Vision Pro 风格 (性能优化版)
 * 
 * 特点：
 * - 彩虹渐变边框
 * - 无封面阴影，滚动时保持平面稳定
 * - 悬浮播放按钮
 * 
 *  性能优化：移除了昂贵的 blur() 和阴影层
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassVideoCard(
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
    showCoverGlassBadges: Boolean = false,
    showInfoGlassBadges: Boolean = false,
    showUpBadge: Boolean = true,
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val contentTypography = feedContentTypography()
    
    val cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.ProminentCard)
    val cardShape = AppShapes.container(ContainerLevel.ProminentCard)
    val coverShape = AppShapes.container(ContainerLevel.Dialog)
    val verticalBadgeShape = AppShapes.container(ContainerLevel.Chip)
    val durationText = remember(video.duration) { FormatUtils.formatDuration(video.duration) }
    val coverPillColors = rememberHomeGlassPillColors(
        glassEnabled = true,
        blurEnabled = true,
        emphasized = false,
        baseColor = resolveHomeGlassCoverPillBaseColor()
    )
    val emphasizedCoverPillColors = rememberHomeGlassPillColors(
        glassEnabled = true,
        blurEnabled = true,
        emphasized = true,
        baseColor = resolveHomeGlassCoverPillBaseColor()
    )
    val inlinePillColors = rememberHomeGlassPillColors(
        glassEnabled = true,
        blurEnabled = true,
        emphasized = false,
        baseColor = AppSurfaceTokens.cardContainer()
    )
    val badgeStylePolicy = remember(showCoverGlassBadges, showInfoGlassBadges) {
        resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = showCoverGlassBadges,
            showInfoGlassBadges = showInfoGlassBadges
        )
    }
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    
    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverUrl = remember(video.bvid, useLowQualityCover) {
        FormatUtils.resolveVideoCoverUrl(
            if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic,
            useLowQuality = useLowQualityCover
        )
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    //  玻璃背景色 - 使用系统主题色自动适配
    val glassBackground = AppSurfaceTokens.cardContainer().copy(alpha = 0.92f)
    
    //  获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    //  记录卡片位置（非 Compose State，避免滚动时触发高频重组）
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
                    // Glass card paints play/danmaku on cover; info is title/UP style.
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
    
    //  尝试获取共享元素作用域
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = effectiveTransitionEnabled &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
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
    // 🌈 彩虹渐变边框色
    val rainbowColors = HomeVisualPalette.GlassSpectrum
    
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
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                motionSpec = cardSharedTransitionMotionSpec,
                clipShape = cardShellShape
            )
            .padding(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro)
            .animateEnter(
                index = index,
                key = Unit,
                animationEnabled = enterAnimationEnabledAtMount,
                motionTier = motionTier,
                coordinateWithSharedTransition = coordinateEnterWithTransition
            )
            //  [新增] 记录卡片位置
            .onGloballyPositioned { coordinates ->
                cardBoundsRef.value = coordinates.boundsInRoot()
            }
    ) {
        //  [性能优化] 移除 blur() 层，改用静态渐变色
        // 原：blur(radius = AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall) 成本很高
        // 新：单层轻量阴影
        
        //  玻璃卡片主体
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .then(nativeCardSnapshot.modifier)
                // 彩虹渐变边框
                .border(
                    width = AppSpacingTokens.Micro * 0.75f,
                    brush = Brush.sweepGradient(
                        colors = rainbowColors.map { it.copy(alpha = 0.6f) }
                    ),
                    shape = cardShape
                )
                // 单层轻量阴影
                .background(glassBackground)
                //  [新增] 长按手势检测
                .pointerInput(onDismiss) {
                    if (onDismiss != null) {
                        detectTapGestures(
                            onLongPress = {
                                haptic(HapticType.HEAVY)
                                showDismissMenu = true
                            },
                            onTap = {
                                triggerCardClick()
                            }
                        )
                    }
                }
                .then(
                    if (onDismiss == null) {
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                //  封面区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(VIDEO_SHARED_COVER_ASPECT_RATIO)
                        .onGloballyPositioned { coordinates ->
                            coverBoundsRef.value = coordinates.boundsInRoot()
                        }
                        .padding(AppSpacingTokens.Small + AppSpacingTokens.Micro)
                ) {
                    // 封面图片 - 圆角内嵌
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .videoCardShellReturnCoverAlpha(
                                enabled = useCardShellSharedBounds,
                                bvid = video.bvid,
                                sourceRoute = effectiveSharedElementSourceRoute,
                                isReturningFromDetail = isReturningFromVideoDetail,
                            )
                            .clip(coverShape)
                    ) {
                        // 由 AsyncImage 根据卡片布局约束选择解码尺寸。
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .placeholderMemoryCacheKey(coverCacheKey)
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
                        //  底部渐变遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Medium)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MediaContrastPalette.Scrim.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                        
                        VideoCardCoverDurationText(
                            text = durationText,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                        )
                        
                        //  [新增] 竖屏标签 - 左上角显示
                        if (video.isVertical && badgeStylePolicy.coverStyle == HomeVideoBadgeStyle.GLASS) {
                            AppSurface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                                color = HomeVisualPalette.VerticalVideoAccent.copy(alpha = 0.82f),
                                border = BorderStroke(AppSpacingTokens.Micro * 0.4f, coverPillColors.borderColor),
                                shape = verticalBadgeShape
                            ) {
                                AppText(
                                    text = "竖屏",
                                    color = MediaContrastPalette.Foreground,
                                    style = contentTypography.coverBadge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro, vertical = AppSpacingTokens.ExtraSmall - AppSpacingTokens.Micro / 2)
                                )
                            }
                        } else if (video.isVertical) {
                            AppSurface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                                color = HomeVisualPalette.VerticalVideoAccent.copy(alpha = 0.82f),
                                shape = verticalBadgeShape
                            ) {
                                AppText(
                                    text = "竖屏",
                                    color = MediaContrastPalette.Foreground,
                                    style = contentTypography.coverBadge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro, vertical = AppSpacingTokens.ExtraSmall - AppSpacingTokens.Micro / 2)
                                )
                            }
                        }
                        }
                    }
                }
                
                //  信息区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacingTokens.Medium + AppSpacingTokens.Micro)
                        .padding(bottom = AppSpacingTokens.Medium + AppSpacingTokens.Micro)
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
                        color = onSurface,
                        style = contentTypography.title,
                        maxLines = videoCardTitleMaxLines(),
                        overflow = videoCardTitleOverflow()
                    )
                    
                    Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
                    
                    // 数据行
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UpBadgeName(
                            name = video.owner.name,
                            leadingContent = if (video.owner.face.isNotBlank()) {
                                {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(FormatUtils.fixImageUrl(video.owner.face))
                                            .size(72, 72)
                                            .crossfade(100)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(AppSpacingTokens.Medium + AppSpacingTokens.Micro)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else null,
                            nameStyle = contentTypography.author,
                            nameColor = onSurfaceVariant,
                            badgeTextColor = onSurfaceVariant.copy(alpha = 0.85f),
                            badgeBackgroundColor = onSurfaceVariant.copy(alpha = 0.12f),
                            showUpBadge = showUpBadge,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                        
                        // 播放量与弹幕统一使用相关推荐统计组件。
                        if (video.stat.view > 0) {
                            HorizontalVideoStatRow(
                                playText = FormatUtils.formatStat(video.stat.view.toLong()),
                                danmakuText = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                            )
                        }
                    }
                }
            }
            
            //  顶部高光线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacingTokens.Micro)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MediaContrastPalette.Foreground.copy(alpha = 0.6f),
                                MediaContrastPalette.Foreground.copy(alpha = 0.8f),
                                MediaContrastPalette.Foreground.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
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
