package com.android.purebilibili.feature.home.components.cards

import android.os.Build
import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppTextButton

import com.android.purebilibili.core.ui.MediaContrastPalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.store.HomeWallpaperEffectMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.HomeCoverReturnPrefetchEntry
import com.android.purebilibili.core.util.HomeCoverReturnPrefetchRegistry
import com.android.purebilibili.core.ui.transition.VideoCardSourceChromeSnapshot
import com.android.purebilibili.core.ui.transition.VideoCardSourceCoverPresentation
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.store.HomeCardBadgeEffectMode
import com.android.purebilibili.core.store.HomeCardInfoGlassMode
import com.android.purebilibili.core.store.HomeDurationStyle
import com.android.purebilibili.core.ui.LocalWallpaperHazeState
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.tween
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.videoCardTitleMaxLines
import com.android.purebilibili.core.ui.videoCardTitleOverflow
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.feedContentTypography
import com.android.purebilibili.feature.home.LocalHomeWallpaperBackdrop
import com.android.purebilibili.feature.home.LocalHomeWallpaperBackdropReady
import com.android.purebilibili.feature.home.LocalHomeWallpaperIsStatic
import com.android.purebilibili.feature.home.HomeCoverRequestSpec
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import com.android.purebilibili.core.ui.ContainerLevel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.adaptiveCardHoverEffect
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.components.resolveUpStatsText
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalMiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.captureNativeCoverOverlayLayer
import com.android.purebilibili.core.ui.transition.captureNativeVideoCardImage
import com.android.purebilibili.core.ui.transition.recordNativeVideoCardLayer
import com.android.purebilibili.core.ui.transition.rememberNativeVideoCardLayer
import com.android.purebilibili.core.ui.transition.LocalClickToPlayEnabled
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionVisualSpec
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoSharedCoverCacheKey
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionOwnership
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionPlaybackIntent
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionVisualSpec
import com.android.purebilibili.core.store.SettingsManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedBoundsResizeMode
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.ui.transition.videoCoverSharedElementKey
import com.android.purebilibili.core.ui.transition.videoSharedElementBoundsTransformSpec
import com.android.purebilibili.feature.home.resolveHomeCardEnterAnimationEnabledAtMount
import com.android.purebilibili.feature.home.resolveHomeCardInfoSurfaceAppearance
import com.android.purebilibili.feature.home.resolveHomeCardWallpaperSurfaceMode
import com.android.purebilibili.feature.home.HomeCardWallpaperSurfaceMode
import com.android.purebilibili.feature.home.HomeGlassPillStyle
import com.android.purebilibili.feature.home.HomeGlassResolvedColors
import com.android.purebilibili.feature.home.resolveHomeGlassCoverPillBaseColor
import com.android.purebilibili.feature.home.resolveHomeGlassPillStyle
import com.android.purebilibili.feature.video.controller.PlaybackProgressManager
import com.android.purebilibili.feature.video.ui.section.resolveCompactPublishTimeRowText
//  [预览播放] 相关引用已移除

import kotlin.math.roundToInt

internal fun shouldOpenLongPressMenu(
    hasPreviewAction: Boolean,
    hasMenuAction: Boolean
): Boolean = !hasPreviewAction && hasMenuAction

internal fun resolveVideoCardMenuOffset(
    rootBoundsInRoot: androidx.compose.ui.geometry.Rect?,
    anchorBoundsInRoot: androidx.compose.ui.geometry.Rect?,
    density: Float,
    pressOffsetInAnchorPx: Offset? = null
): DpOffset {
    if (rootBoundsInRoot == null || anchorBoundsInRoot == null || density <= 0f) {
        return DpOffset.Zero
    }

    val anchorPointInRoot = if (pressOffsetInAnchorPx != null) {
        Offset(
            x = anchorBoundsInRoot.left + pressOffsetInAnchorPx.x,
            y = anchorBoundsInRoot.top + pressOffsetInAnchorPx.y
        )
    } else {
        Offset(
            x = anchorBoundsInRoot.left,
            y = anchorBoundsInRoot.bottom
        )
    }

    val localX = (anchorPointInRoot.x - rootBoundsInRoot.left).coerceAtLeast(0f)
    val localY = (anchorPointInRoot.y - rootBoundsInRoot.top).coerceAtLeast(0f)
    return DpOffset(
        x = (localX / density).dp,
        y = (localY / density).dp
    )
}

internal fun resolveVideoCardCoverCacheKey(
    video: VideoItem,
    useLowQualityCover: Boolean,
    requestSpec: HomeCoverRequestSpec? = null,
): String {
    val normalizedIdentity = video.bvid.trim().ifEmpty {
        video.pic.trim().ifEmpty {
            "fallback_${video.id.coerceAtLeast(0L)}_${video.cid.coerceAtLeast(0L)}_${video.title.hashCode()}"
        }
    }
    val baseKey = resolveVideoSharedCoverCacheKey(normalizedIdentity, useLowQualityCover)
    return requestSpec?.let { "${baseKey}_${it.cacheKeySuffix}" } ?: baseKey
}

private data class VideoCardTexts(
    val durationText: String,
    val primaryStatText: String,
    val secondaryStatText: String?,
    val durationBadgeMinWidth: androidx.compose.ui.unit.Dp
)

private data class VideoCardPillColors(
    val cover: HomeGlassResolvedColors,
    val emphasizedCover: HomeGlassResolvedColors,
    val inline: HomeGlassResolvedColors
)

private data class VideoCardSharedTransitionSpecs(
    val motion: VideoSharedTransitionMotionSpec,
    val visual: VideoSharedTransitionVisualSpec
)

private data class VideoCardScreenMetrics(
    val widthPx: Float,
    val heightPx: Float,
    val density: Float
)

internal data class HomeVideoCardMetadataColors(
    val upNameColor: Color,
    val upMetaColor: Color,
    val upBadgeTextColor: Color,
    val upBadgeBackgroundColor: Color,
    val publishTimeColor: Color
)

internal fun resolveHomeVideoCardMetadataColors(
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
): HomeVideoCardMetadataColors {
    return HomeVideoCardMetadataColors(
        // 作者信息降为次级语义色，让标题保持卡片内的第一视觉层级。
        upNameColor = onSurfaceVariantColor,
        upMetaColor = onSurfaceColor.copy(alpha = 0.82f),
        upBadgeTextColor = onSurfaceColor.copy(alpha = 0.68f),
        upBadgeBackgroundColor = onSurfaceColor.copy(alpha = 0.10f),
        publishTimeColor = onSurfaceColor.copy(alpha = 0.72f)
    )
}

private fun resolveVideoCardMetadataModifier(
    hasTrailingCardAction: Boolean
): Modifier {
    return Modifier
        .fillMaxWidth()
        .then(
            if (hasTrailingCardAction) {
                // 右下角"⋮"/取消收藏按钮的视觉图标只占约 20dp，预留 24dp 即可
                // 防遮挡，把更多宽度让给 UP 名称/日期，避免名称提前折叠省略。
                Modifier.padding(end = AppSpacingTokens.ExtraLarge)
            } else {
                Modifier
            }
        )
}

private fun resolveVideoCardMetadataRowModifier(): Modifier = Modifier.fillMaxWidth()

@Composable
private fun VideoCardOwnerMetadata(
    video: VideoItem,
    isFollowing: Boolean,
    showUpBadge: Boolean,
    showUpAvatar: Boolean,
    upFollowerCount: Int?,
    upVideoCount: Int?,
    infoBadgeStyle: HomeVideoBadgeStyle,
    inlinePillColors: HomeGlassResolvedColors,
    metadataColors: HomeVideoCardMetadataColors,
    onUpClick: ((Long) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val contentTypography = feedContentTypography()
    val upClickMid = video.owner.mid.takeIf { it > 0L && onUpClick != null }
    val ownerModifier = modifier
        .fillMaxWidth()
        .then(
            if (upClickMid != null) {
                Modifier.clickable { onUpClick?.invoke(upClickMid) }
            } else {
                Modifier
            }
        )

    UpBadgeName(
        name = video.owner.name,
        metaText = resolveUpStatsText(
            followerCount = upFollowerCount,
            videoCount = upVideoCount
        ),
        inlineTrailingContent = if (isFollowing) {
            {
                if (infoBadgeStyle == HomeVideoBadgeStyle.GLASS) {
                    AppSurface(
                        modifier = Modifier.wrapContentSize(),
                        shape = AppShapes.container(ContainerLevel.Pill),
                        color = inlinePillColors.containerColor,
                        border = BorderStroke(
                            AppSpacingTokens.Micro * 0.4f,
                            inlinePillColors.borderColor
                        )
                    ) {
                        AppText(
                            text = "已关注",
                            style = contentTypography.coverBadge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                horizontal = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro,
                                vertical = AppSpacingTokens.Micro
                            )
                        )
                    }
                } else {
                    AppText(
                        text = "已关注",
                        style = contentTypography.coverBadge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            null
        },
        leadingContent = if (showUpAvatar && video.owner.face.isNotEmpty()) {
            {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.owner.face))
                        .crossfade(100)
                        .size(32, 32)
                        .memoryCacheKey("avatar_${video.owner.face.hashCode()}")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(AppSpacingTokens.Medium + AppSpacingTokens.Micro)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            null
        },
        nameStyle = contentTypography.author,
        nameColor = metadataColors.upNameColor,
        metaColor = metadataColors.upMetaColor,
        badgeTextColor = metadataColors.upBadgeTextColor,
        badgeBackgroundColor = metadataColors.upBadgeBackgroundColor,
        // 名称占满剩余宽度，省略号顶到卡片内边距附近；右侧留 6dp 空隙不与
        // 后续元素贴死，长昵称也不会被提前折叠到卡片一半宽度。
        nameEndPadding = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro,
        // 未关注时不再渲染空的尾部槽位，避免继续占用作者名的可用宽度；
        // 已关注时由真实的尾部内容自行占位。
        reserveTrailingSlot = false,
        trailingSlotMinWidth = AppSpacingTokens.None,
        trailingSlotMinHeight = AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall,
        showUpBadge = showUpBadge,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Visible,
        metaMaxLines = Int.MAX_VALUE,
        metaOverflow = TextOverflow.Visible,
        modifier = ownerModifier
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun VideoCardDurationPublishRow(
    durationText: String,
    publishTimeText: String,
    emphasizePublishTime: Boolean,
    publishTimeColor: Color,
    topSpacing: Dp,
) {
    if (durationText.isBlank() && publishTimeText.isBlank()) return
    val contentTypography = feedContentTypography()

    Spacer(modifier = Modifier.height(topSpacing))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        itemVerticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Micro)
    ) {
        if (publishTimeText.isNotBlank()) {
            VideoCardPublishTime(
                text = publishTimeText,
                emphasized = emphasizePublishTime,
                color = publishTimeColor,
                modifier = Modifier.wrapContentWidth(),
            )
        }

        if (durationText.isNotBlank()) {
            AppText(
                text = durationText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = contentTypography.statistic.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
private fun VideoCardPublishTime(
    text: String,
    emphasized: Boolean,
    color: Color,
    modifier: Modifier,
) {
    val contentTypography = feedContentTypography()
    if (emphasized) {
        AppSurface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shape = AppShapes.container(ContainerLevel.Pill),
            modifier = modifier
        ) {
            AppText(
                text = text,
                style = contentTypography.statistic.copy(fontWeight = FontWeight.Medium),
                color = color.copy(alpha = 0.92f),
                softWrap = true,
                overflow = TextOverflow.Visible,
                modifier = Modifier.padding(horizontal = AppSpacingTokens.Small, vertical = AppSpacingTokens.ExtraSmall - AppSpacingTokens.Micro / 2)
            )
        }
    } else {
        AppText(
            text = text,
            style = contentTypography.statistic,
            color = color,
            softWrap = true,
            overflow = TextOverflow.Visible,
            modifier = modifier
        )
    }
}

private fun resolveVideoCardPillColors(
    glassEnabled: Boolean,
    blurEnabled: Boolean,
    inlineBaseColor: Color
): VideoCardPillColors {
    val coverBaseColor = resolveHomeGlassCoverPillBaseColor()
    return VideoCardPillColors(
        cover = resolveVideoCardPillColors(
            style = resolveHomeGlassPillStyle(
                glassEnabled = glassEnabled,
                blurEnabled = blurEnabled,
                emphasized = false
            ),
            baseColor = coverBaseColor
        ),
        emphasizedCover = resolveVideoCardPillColors(
            style = resolveHomeGlassPillStyle(
                glassEnabled = glassEnabled,
                blurEnabled = blurEnabled,
                emphasized = true
            ),
            baseColor = coverBaseColor
        ),
        inline = resolveVideoCardPillColors(
            style = resolveHomeGlassPillStyle(
                glassEnabled = glassEnabled,
                blurEnabled = blurEnabled,
                emphasized = false
            ),
            baseColor = inlineBaseColor
        )
    )
}

private fun resolveVideoCardPillColors(
    style: HomeGlassPillStyle,
    baseColor: Color
): HomeGlassResolvedColors {
    return HomeGlassResolvedColors(
        containerColor = baseColor.copy(alpha = style.containerAlpha),
        borderColor = MediaContrastPalette.Foreground.copy(alpha = style.borderAlpha),
        highlightColor = MediaContrastPalette.Foreground.copy(alpha = style.highlightAlpha)
    )
}

/**
 *  官方 B 站风格视频卡片
 * 采用与 Bilibili 官方 App 一致的设计：
 * - 封面比例由首页卡片样式统一配置
 * - 左下角：播放量 + 弹幕数
 * - 右下角：时长
 * - 标题：2行
 * - 底部：「已关注」标签 + UP主名称
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ElegantVideoCard(
    video: VideoItem,
    index: Int,
    refreshKey: Long = 0L,
    isFollowing: Boolean = false,  //  是否已关注该 UP 主
    animationEnabled: Boolean = true,   //  卡片进场动画开关
    motionTier: MotionTier = MotionTier.Normal,
    transitionEnabled: Boolean = false, //  卡片过渡动画开关
    sharedElementSourceRoute: String? = null,
    isReturningFromVideoDetail: Boolean = false,
    isQuickReturningFromVideoDetail: Boolean = false,
    scrollLiteModeEnabled: Boolean = false,
    showPublishTime: Boolean = false,   //  是否显示发布时间（搜索结果用）
    isDataSaverActive: Boolean = false, // 🚀 [性能优化] 从父级传入，避免每个卡片重复计算
    preferLowQualityCover: Boolean = false,
    coverRequestSpec: HomeCoverRequestSpec? = null,
    glassEnabled: Boolean = true,
    blurEnabled: Boolean = true,
    compactStatsOnCover: Boolean = false, // 播放/弹幕默认位于信息区
    showCoverGlassBadges: Boolean = false,
    showInfoGlassBadges: Boolean = false,
    badgeEffectMode: HomeCardBadgeEffectMode = HomeCardBadgeEffectMode.OFF,
    infoGlassMode: HomeCardInfoGlassMode = HomeCardInfoGlassMode.OFF,
    wallpaperTintEnabled: Boolean = false,
    wallpaperEffectMode: HomeWallpaperEffectMode = HomeWallpaperEffectMode.SOFT_BLUR,
    showUpBadge: Boolean? = null,
    showUpAvatar: Boolean? = null,
    homeDurationStyle: HomeDurationStyle = HomeDurationStyle.OUTSIDE_COVER,
    // 默认跟官方双列 4:3；首页会传入 resolveHomeFeedCardLayout 的比例覆盖
    coverAspectRatio: Float = 4f / 3f,
    compactMetadata: Boolean = true,
    titleMinLines: Int = 2,
    titleMaxLines: Int = 2,
    highlightedTitle: AnnotatedString? = null,
    showOnlineCount: Boolean = false,
    upFollowerCount: Int? = null,
    upVideoCount: Int? = null,
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onWatchLater: (() -> Unit)? = null,  //  [新增] 稍后再看回调
    onUnfavorite: (() -> Unit)? = null,  //  [新增] 取消收藏回调
    dismissMenuText: String = "\uD83D\uDEAB 不感兴趣", //  [新增] 自定义长按菜单删除文案
    onLongClick: ((VideoItem) -> Unit)? = null, // [Feature] Long Press Preview
    onUpClick: ((Long) -> Unit)? = null,
    singleColumn: Boolean = false,
    cardWidthDp: Float? = null,
    modifier: Modifier = Modifier,
    onClick: (String, Long) -> Unit
) {
    val isLongPressEnabled = com.android.purebilibili.core.ui.LocalVideoCardLongPressEnabled.current
    if (singleColumn) {
        val actions = buildList {
            onUpClick?.let { visit ->
                add(com.android.purebilibili.core.ui.components.AppWindowAction(
                    label = "访问UP主", onClick = { visit(video.owner.mid) },
                ))
            }
            onWatchLater?.let {
                add(com.android.purebilibili.core.ui.components.AppWindowAction(label = "加入稍后再看", onClick = it))
            }
            onUnfavorite?.let {
                add(com.android.purebilibili.core.ui.components.AppWindowAction(label = "取消收藏", onClick = it))
            }
            onDismiss?.let {
                add(com.android.purebilibili.core.ui.components.AppWindowAction(label = dismissMenuText, onClick = it))
            }
        }
        HomeStyleSingleColumnVideoCard(
            video = video,
            sourceRoute = sharedElementSourceRoute
                ?: com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute.current.orEmpty(),
            coverAspectRatio = coverAspectRatio,
            transitionEnabled = transitionEnabled,
            isFollowing = isFollowing,
            showUpBadge = showUpBadge ?: com.android.purebilibili.core.ui.LocalUpBadgeVisibility.current.showBadges,
            modifier = modifier,
            highlightedTitle = highlightedTitle,
            onClick = { onClick(video.bvid, video.cid) },
            onLongClick = if (isLongPressEnabled) onLongClick?.let { callback -> { callback(video) } } else null,
            trailingContent = if (actions.isEmpty()) null else ({
                com.android.purebilibili.core.ui.components.AppWindowActionMenu(groups = listOf(actions)) {
                    AppIcon(Icons.Outlined.MoreVert, contentDescription = "更多操作")
                }
            }),
        )
        return
    }
    val haptic = rememberHapticFeedback()
    val contentTypography = feedContentTypography()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val playbackProgressManager = remember(context) {
        PlaybackProgressManager.getInstance(context)
    }
    
    val cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.Card)
    val durationBadgeStyle = remember { resolveVideoCardDurationBadgeVisualStyle() }
    val cardTexts = remember(video.duration, video.stat.view, video.stat.reply, video.stat.danmaku, video.progress) {
        val durationText = FormatUtils.formatDuration(video.duration)
        val primaryStatText = if (video.stat.view > 0) {
            FormatUtils.formatStat(video.stat.view.toLong())
        } else {
            FormatUtils.formatProgress(video.progress, video.duration)
        }
        val commentCount = video.stat.reply.takeIf { it > 0 } ?: video.stat.danmaku
        val secondaryStatText = commentCount.takeIf { it > 0 }?.let { FormatUtils.formatStat(it.toLong()) }
        val durationBadgeMinWidth = resolveVideoCardDurationBadgeMinWidthDp(
            durationText = durationText,
            style = durationBadgeStyle
        ).dp
        VideoCardTexts(durationText, primaryStatText, secondaryStatText, durationBadgeMinWidth)
    }
    val durationText = cardTexts.durationText
    val primaryStatText = cardTexts.primaryStatText
    val secondaryStatText = cardTexts.secondaryStatText
    val durationBadgeMinWidth = cardTexts.durationBadgeMinWidth
    // 时长作为统计行 pill（闹钟图标 + 文本）时的最小宽度预算，供封面统计行自适应让位。
    val durationStatMinWidthDp = remember(durationText) {
        resolveVideoCardDurationStatMinWidthDp(durationText)
    }
    val showDurationOnCover = homeDurationStyle == HomeDurationStyle.OVERLAY_TEXT_ONLY
    val coverOverlayTextShadow = remember { resolveVideoCardCoverOverlayTextShadow() }
    val coverOverlayTextStyle = remember(coverOverlayTextShadow) {
        TextStyle(shadow = coverOverlayTextShadow)
    }
    val showDurationOutside = homeDurationStyle == HomeDurationStyle.OUTSIDE_COVER
    val inlinePillBaseColor = AppSurfaceTokens.cardContainer()
    val wallpaperHazeState = LocalWallpaperHazeState.current
    val homeWallpaperBackdrop = LocalHomeWallpaperBackdrop.current
    val homeWallpaperBackdropReady = LocalHomeWallpaperBackdropReady.current
    val homeWallpaperIsStatic = LocalHomeWallpaperIsStatic.current
    val badgeEffectVisual = remember(badgeEffectMode, wallpaperHazeState != null) {
        resolveHomeCardBadgeEffectVisual(
            mode = badgeEffectMode,
            scrollLiteModeEnabled = false,
            hasHazeState = wallpaperHazeState != null
        )
    }
    val effectiveGlassEnabled = badgeEffectVisual.glassEnabled && glassEnabled
    val effectiveBlurEnabled = badgeEffectVisual.blurEnabled && blurEnabled
    val pillColors = remember(effectiveGlassEnabled, effectiveBlurEnabled, inlinePillBaseColor) {
        resolveVideoCardPillColors(
            glassEnabled = effectiveGlassEnabled,
            blurEnabled = effectiveBlurEnabled,
            inlineBaseColor = inlinePillBaseColor
        )
    }
    val coverPillColors = pillColors.cover
    val inlinePillColors = pillColors.inline
    val isDarkCardTheme = AppSurfaceTokens.chromeBackground().luminance() < 0.5f
    val wallpaperPalette = LocalWallpaperPalette.current
    val homeCardDynamicTintEnabled = LocalHomeCardDynamicTintEnabled.current
    val lowBlurBudgetForced = isLowBlurBudgetForced()
    val homeWallpaperSurfaceMode = remember(
        homeCardDynamicTintEnabled,
        wallpaperTintEnabled,
        homeWallpaperIsStatic,
        homeWallpaperBackdropReady,
        blurEnabled,
        isDataSaverActive,
        lowBlurBudgetForced,
        Build.VERSION.SDK_INT,
    ) {
        resolveHomeCardWallpaperSurfaceMode(
            dynamicTintEnabled = homeCardDynamicTintEnabled,
            wallpaperVisible = wallpaperTintEnabled,
            wallpaperIsStatic = homeWallpaperIsStatic,
            backdropReady = homeWallpaperBackdropReady,
            blurEnabled = blurEnabled,
            isDataSaverActive = isDataSaverActive,
            lowBlurBudgetForced = lowBlurBudgetForced,
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
    val useRealtimeWallpaperBackdrop =
        homeWallpaperSurfaceMode == HomeCardWallpaperSurfaceMode.REALTIME_FROSTED
    val cardYFraction = remember { mutableFloatStateOf(0.5f) }
    val infoLayoutCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val homeScrollTickProvider = LocalHomeScrollTickProvider.current
    val infoSurfaceAppearance = remember(
        wallpaperTintEnabled,
        wallpaperEffectMode,
        isDarkCardTheme,
        isDataSaverActive,
        infoGlassMode,
        wallpaperHazeState != null,
        blurEnabled
    ) {
        resolveHomeCardInfoSurfaceAppearance(
            wallpaperTintEnabled = wallpaperTintEnabled,
            wallpaperEffectMode = wallpaperEffectMode,
            isDarkTheme = isDarkCardTheme,
            isDataSaverActive = isDataSaverActive,
            infoGlassMode = infoGlassMode,
            hasWallpaperHazeState = wallpaperHazeState != null,
            hasLayerBackdrop = false,
            blurEnabled = blurEnabled
        )
    }
    // The global card switch is authoritative. Older per-card glass settings must not
    // re-enable frosted surfaces after the user turns the shared setting off.
    val shouldUseFrostedGlass = homeCardDynamicTintEnabled
    val scrollLitePolicy = remember(compactStatsOnCover) {
        resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = compactStatsOnCover
        )
    }
    val badgeStylePolicy = remember(badgeEffectVisual, showCoverGlassBadges, showInfoGlassBadges) {
        HomeVideoGlassBadgeStylePolicy(
            coverStyle = if (showCoverGlassBadges) {
                badgeEffectVisual.coverStyle
            } else {
                HomeVideoBadgeStyle.PLAIN
            },
            infoStyle = if (showInfoGlassBadges) {
                badgeEffectVisual.infoStyle
            } else {
                HomeVideoBadgeStyle.PLAIN
            }
        )
    }
    val historyProgressState = remember(video.bvid, video.cid, video.view_at, video.duration, video.progress, refreshKey) {
        val localPositionMs = if (video.bvid.isNotBlank()) {
            playbackProgressManager.getCachedPosition(video.bvid, video.cid)
        } else {
            0L
        }
        resolveVideoCardHistoryProgressState(
            viewAt = video.view_at,
            durationSec = video.duration,
            progressSec = video.progress,
            localPositionMs = localPositionMs
        )
    }
    val showHistoryProgressBar = historyProgressState.showProgressBar
    val historyProgressFraction = historyProgressState.progressFraction
    val historyProgressBarColor = resolveVideoCardHistoryProgressBarColor(
        themePrimary = MaterialTheme.colorScheme.primary
    )
    val coverOverlayBottomLayout = remember(scrollLitePolicy.showHistoryProgressBar, showHistoryProgressBar) {
        resolveVideoCardCoverOverlayBottomLayout(
            showHistoryProgressBar = scrollLitePolicy.showHistoryProgressBar && showHistoryProgressBar
        )
    }
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    //  [新增] 确认对话框状态
    var showUnfavoriteDialog by remember { mutableStateOf(false) }
    
    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverCacheKey: String
    val coverUrl: String
    val premiumBadgeLabel: String?
    // Cover identity does not depend on playback progress or live statistics. Keep unrelated
    // VideoItem updates from rebuilding the cover URL/cache key on the UI thread.
    remember(video.pic, video.rights, useLowQualityCover, coverRequestSpec) {
        Triple(
            resolveVideoCardCoverCacheKey(
                video = video,
                useLowQualityCover = useLowQualityCover,
                requestSpec = coverRequestSpec,
            ),
            coverRequestSpec?.resolveUrl(video.pic) ?: FormatUtils.resolveVideoCoverUrl(
                video.pic,
                useLowQuality = useLowQualityCover,
            ),
            resolveVideoPremiumBadgeLabel(video.rights)
        )
    }.let { (cache, url, badge) ->
        coverCacheKey = cache
        coverUrl = url
        premiumBadgeLabel = badge
    }
    var coverTint by remember(coverCacheKey) {
        mutableStateOf(VideoCardCoverColorStore.getCachedColor(coverCacheKey))
    }
    val activeCoverCacheKey by rememberUpdatedState(coverCacheKey)
    val animatedCoverTint by animateColorAsState(
        targetValue = coverTint ?: Color.Transparent,
        animationSpec = tween(durationMillis = 350),
        label = "video_card_cover_tint"
    )
    val defaultOnSurface = MaterialTheme.colorScheme.onSurface
    val defaultOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val adaptiveContentColors = remember(
        wallpaperPalette,
        animatedCoverTint,
        wallpaperTintEnabled,
        isDarkCardTheme,
        defaultOnSurface,
        defaultOnSurfaceVariant,
        homeCardDynamicTintEnabled
    ) {
        resolveVideoCardAdaptiveContentColors(
            wallpaperPalette = wallpaperPalette,
            coverTint = if (animatedCoverTint.alpha > 0f) animatedCoverTint else null,
            wallpaperTintEnabled = wallpaperTintEnabled,
            isDarkTheme = isDarkCardTheme,
            defaultOnSurface = defaultOnSurface,
            defaultOnSurfaceVariant = defaultOnSurfaceVariant,
            homeCardDynamicTintEnabled = homeCardDynamicTintEnabled
        )
    }
    // 返回预热：组合即可见，上报 (bvid, url, cacheKey)，供详情返回时按同一 cacheKey
    // prefetch，避免首页 scene 重建后封面重新解码造成落位闪变。
    SideEffect {
        HomeCoverReturnPrefetchRegistry.onCardVisible(
            HomeCoverReturnPrefetchEntry(
                bvid = video.bvid.trim(),
                url = coverUrl,
                cacheKey = coverCacheKey,
            )
        )
    }
    val onlineCount = rememberVideoCardOnlineCount(
        video = video,
        showOnlineCount = showOnlineCount
    )
    val publishTimeRowText: String
    val emphasizePublishTime: Boolean
    remember(showPublishTime, video.pubdate) {
        if (!showPublishTime) {
            "" to false
        } else {
            resolveCompactPublishTimeRowText(pubdate = video.pubdate) to false
        }
    }.let { (text, emphasize) ->
        publishTimeRowText = text
        emphasizePublishTime = emphasize
    }
    
    //  判断是否为竖屏视频（通过封面图 URL 中的尺寸信息或默认不显示）
    // B站封面 URL 通常包含尺寸信息，如 width=X&height=Y
    // 简单方案：暂不显示竖屏标签（因推荐API不提供视频尺寸信息）

    //  获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenMetrics = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density
    ) {
        VideoCardScreenMetrics(
            widthPx = with(density) { configuration.screenWidthDp.dp.toPx() },
            heightPx = with(density) { configuration.screenHeightDp.dp.toPx() },
            density = density.density
        )
    }
    val resolvedCardWidthDp = remember(cardWidthDp, screenMetrics.widthPx, screenMetrics.density) {
        cardWidthDp ?: ((screenMetrics.widthPx / screenMetrics.density - (AppSpacingTokens.Medium.value * 2f)) / 2f)
    }
    val compactStatsAvailableWidthDp = remember(resolvedCardWidthDp) {
        (resolvedCardWidthDp - (AppSpacingTokens.Small.value * 2f)).coerceAtLeast(0f)
    }
    
    //  记录卡片位置（非 Compose State，避免滚动时触发高频重组）
    //  [性能优化] 存储 LayoutCoordinates 引用而非 Rect，boundsInRoot() 仅在交互时惰性计算，
    //  避免滚动期间每帧 4 次坐标树遍历开销。
    val cardCoordsRef = remember { object { var value: LayoutCoordinates? = null } }
    val sharedSourceInstanceId = rememberSaveable(video.bvid) {
        CardPositionManager.newVideoCardSourceInstanceId()
    }
    val coverCoordsRef = remember { object { var value: LayoutCoordinates? = null } }
    val titleCoordsRef = remember { object { var value: LayoutCoordinates? = null } }
    val menuButtonCoordsRef = remember { object { var value: LayoutCoordinates? = null } }
    val localSharedElementSourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val effectiveSharedElementSourceRoute = remember(sharedElementSourceRoute, localSharedElementSourceRoute) {
        sharedElementSourceRoute ?: localSharedElementSourceRoute
    }

    val openDismissMenu: (LayoutCoordinates?, Offset?) -> Unit = { anchorCoords, pressOffset ->
        menuOffset = resolveVideoCardMenuOffset(
            rootBoundsInRoot = cardCoordsRef.value?.takeIf { it.isAttached }?.boundsInRoot(),
            anchorBoundsInRoot = anchorCoords?.takeIf { it.isAttached }?.boundsInRoot(),
            density = screenMetrics.density,
            pressOffsetInAnchorPx = pressOffset
        )
        showDismissMenu = true
    }
    val hasOverflowMenu = onDismiss != null || onWatchLater != null
    val hasTrailingCardAction = onUnfavorite != null || hasOverflowMenu
    val nativeCardLayer = rememberNativeVideoCardLayer()
    val nativeCoverOverlayLayer = rememberNativeVideoCardLayer()
    val freezeNativeCardLayer = remember(video.bvid) { mutableStateOf(false) }
    
    val triggerCardClick = {
        cardCoordsRef.value?.takeIf { it.isAttached }?.boundsInRoot()?.let { bounds ->
            val sourceCoverBounds = coverCoordsRef.value
                ?.takeIf { it.isAttached }
                ?.boundsInRoot()
            val frozenCompactStatsLayout = resolveVideoCardCompactCoverStatsLayout(
                availableWidthDp = sourceCoverBounds?.let { coverBounds ->
                    (
                        coverBounds.width / screenMetrics.density -
                            (AppSpacingTokens.Small.value * 2f)
                        ).coerceAtLeast(0f)
                } ?: Float.MAX_VALUE,
                primaryStatText = primaryStatText,
                secondaryStatText = secondaryStatText,
                hasOnlineCount = onlineCount.isNotEmpty(),
                durationBadgeMinWidthDp = if (showDurationOnCover) {
                    durationBadgeMinWidth.value
                } else {
                    0f
                },
                durationStatMinWidthDp = if (showDurationOutside) {
                    durationStatMinWidthDp
                } else {
                    0f
                },
            )
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                bounds = bounds,
                screenWidth = screenMetrics.widthPx,
                screenHeight = screenMetrics.heightPx,
                density = screenMetrics.density,
                sourceCornerDp = cardCornerRadius.value.roundToInt(),
                coverBounds = sourceCoverBounds,
                // Dual-column home cards are cover-over-meta; freeze chrome so return text
                // survives Loading and does not wait for destination ViewInfo.
                sourceLayout = VideoCardSourceLayout.STACKED,
                sourceChromeSnapshot = VideoCardSourceChromeSnapshot(
                    title = video.title,
                    ownerName = video.owner.name,
                    ownerFaceUrl = video.owner.face,
                    viewText = if (video.stat.view > 0) {
                        FormatUtils.formatStat(video.stat.view.toLong())
                    } else {
                        primaryStatText
                    },
                    danmakuText = secondaryStatText
                        ?: FormatUtils.formatStat(video.stat.danmaku.toLong()),
                    durationText = durationText,
                    followed = video.isFollowed,
                    // Remember what the list info column actually paints (not cover badges).
                    infoPresentation = com.android.purebilibili.core.ui.transition
                        .resolveVideoCardSourceInfoPresentation(
                            publishTimeText = publishTimeRowText,
                            showStatsInInfo = scrollLitePolicy.showSecondaryStatsRow,
                            showDurationInInfo = showDurationOutside,
                            useTintedInfoSurface = shouldUseFrostedGlass,
                            showOverflowMenu = hasOverflowMenu,
                        ),
                    coverPresentation = VideoCardSourceCoverPresentation(
                        showGradientMask = scrollLitePolicy.showCoverGradientMask,
                        showStatsOnCover = scrollLitePolicy.showCompactStatsOnCover,
                        showSecondaryStatOnCover = scrollLitePolicy.showCompactStatsOnCover &&
                            frozenCompactStatsLayout.showSecondaryStat,
                        showOnlineCountOnCover = scrollLitePolicy.showCompactStatsOnCover &&
                            frozenCompactStatsLayout.showOnlineCount,
                        showDurationOnCover = showDurationOnCover,
                        showDurationAsStat = scrollLitePolicy.showCompactStatsOnCover &&
                            showDurationOutside,
                        useGlassStats = badgeStylePolicy.coverStyle == HomeVideoBadgeStyle.GLASS,
                        onlineCountText = onlineCount,
                        premiumBadgeText = premiumBadgeLabel.orEmpty(),
                        showHistoryProgressBar = scrollLitePolicy.showHistoryProgressBar &&
                            showHistoryProgressBar,
                        historyProgressFraction = historyProgressFraction,
                    ),
                    // Exact stationary list cover request (URL + key + Coil size).
                    coverUrl = coverUrl,
                    coverCacheKey = coverCacheKey,
                    coverDecodeWidthPx = coverRequestSpec?.widthPx ?: 0,
                    coverDecodeHeightPx = coverRequestSpec?.heightPx ?: 0,
                ),
                sourceInstanceId = sharedSourceInstanceId,
            )
            // The mounted draw modifier reads this latch directly, so the layer freezes
            // before OPENING can hide the stationary title and statistics.
            freezeNativeCardLayer.value = true
            captureNativeVideoCardImage(nativeCardLayer)
            captureNativeCoverOverlayLayer(nativeCoverOverlayLayer)
        }
        onClick(video.bvid, video.cid)
    }
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
    val adaptiveHoverShape = AppShapes.container(ContainerLevel.Card)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .adaptiveCardHoverEffect(shape = adaptiveHoverShape)
            // 进场动画：挂载门控已含滚动/返回/切分类；与过渡并存时仅淡入不改几何
            .animateEnter(
                index = index,
                key = Unit,
                animationEnabled = enterAnimationEnabledAtMount,
                motionTier = motionTier,
                coordinateWithSharedTransition = coordinateEnterWithTransition
            )
            //  [新增] 记录卡片位置（仅存引用，boundsInRoot() 在交互时惰性计算）
            .onGloballyPositioned { coordinates ->
                cardCoordsRef.value = coordinates
            }
    ) {
        //  尝试获取共享元素作用域。首页点击视频时，由卡片主容器承载整体放大/回收。
        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
        val transitionAdaptiveInfo = com.android.purebilibili.core.ui.transition
            .LocalVideoTransitionAdaptiveInfo.current
        val effectiveTransitionEnabled = transitionEnabled
        val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
            transitionEnabled = effectiveTransitionEnabled && LocalSharedTransitionEnabled.current,
            hasSharedTransitionScope = sharedTransitionScope != null,
            hasAnimatedVisibilityScope = animatedVisibilityScope != null
        )
        val isQuickReturnLimited = isReturningFromVideoDetail && isQuickReturningFromVideoDetail
        val sharedTransitionOwnership = resolveVideoSharedTransitionOwnership(
            sourceRoute = effectiveSharedElementSourceRoute,
            coverSharedEnabled = coverSharedEnabled,
            isQuickReturnLimited = isQuickReturnLimited
        )
        val autoPlayOnOpenEnabled = LocalClickToPlayEnabled.current
        val videoSharedPlaybackIntent = remember(autoPlayOnOpenEnabled) {
            resolveVideoSharedTransitionPlaybackIntent(
                clickToPlayEnabled = autoPlayOnOpenEnabled
            )
        }
        val homeSharedTransitionSpecs = remember(
            effectiveSharedElementSourceRoute,
            effectiveTransitionEnabled,
            cardCornerRadius,
            sharedTransitionSpeedSettings,
            videoSharedPlaybackIntent,
            transitionAdaptiveInfo,
        ) {
            VideoCardSharedTransitionSpecs(
                motion = resolveVideoCardSharedTransitionMotionSpec(
                    sourceRoute = effectiveSharedElementSourceRoute,
                    transitionEnabled = effectiveTransitionEnabled,
                    speedSettings = sharedTransitionSpeedSettings,
                    adaptiveInfo = transitionAdaptiveInfo,
                ),
                visual = resolveVideoSharedTransitionVisualSpec(
                    sourceRoute = effectiveSharedElementSourceRoute,
                    sourceCornerDp = cardCornerRadius.value.roundToInt(),
                    playbackIntent = videoSharedPlaybackIntent,
                    adaptiveInfo = transitionAdaptiveInfo,
                )
            )
        }
        val homeSharedTransitionMotionSpec = homeSharedTransitionSpecs.motion
        val homeSharedTransitionVisualSpec = homeSharedTransitionSpecs.visual
        val routeMatchesSharedReturnTarget = remember(
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
        val sharedSourceOwnershipAllowed = isVideoCardSharedSourceInstanceOwner(
            sourceInstanceId = sharedSourceInstanceId,
            lastClickedSourceInstanceId = CardPositionManager.lastClickedVideoSourceInstanceId,
        )
        val isCoverSharedReturnTarget = routeMatchesSharedReturnTarget && sharedSourceOwnershipAllowed
        val useCardShellSharedBounds = sharedTransitionOwnership.useCardContainerSharedBounds &&
            sharedSourceOwnershipAllowed
        val miuixCardPixelOwnership =
            LocalMiuixVideoCardTransitionState.current.enabled && isCoverSharedReturnTarget
        val coverCrossfadeEnabled = shouldEnableVideoCardCoverCrossfade(
            isScrollInProgress = false,
            isReturningFromDetail = isReturningFromVideoDetail,
            useCoverSharedBounds = useCardShellSharedBounds || miuixCardPixelOwnership,
            isSharedReturnTarget = isCoverSharedReturnTarget
        )
        // lastClicked 生命周期内钉住点击时的封面源，避免返回途中换 URL/质量触发重解码闪烁。
        val pinnedSharedReturnCover = remember(isCoverSharedReturnTarget) {
            if (shouldPinVideoCardCoverForSharedReturn(isCoverSharedReturnTarget)) {
                coverUrl to coverCacheKey
            } else {
                null
            }
        }
        val requestCoverUrl = pinnedSharedReturnCover?.first ?: coverUrl
        val requestCoverCacheKey = pinnedSharedReturnCover?.second ?: coverCacheKey
        val cardShellShape = AppShapes.container(ContainerLevel.Card)
        // sharedBounds 与卡片底色拆开：
        // - 外层 Box 量尺寸 + 画 surface（只在源布局层，不进 overlay）
        // - 内层 Column 挂 sharedBounds（封面/标题等，无 solid fill）
        // 若把 cardContainer 画进 sharedBounds，预测返回时会盖住详情壳实时视频 → 大黑块。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .recordNativeVideoCardLayer(
                    layer = nativeCardLayer,
                    freezeProvider = {
                        freezeNativeCardLayer.value &&
                            CardPositionManager.isNativeVideoCardLayerCurrentOwner(nativeCardLayer)
                    },
                    bvid = video.bvid,
                    sourceRoute = effectiveSharedElementSourceRoute,
                    enabled = effectiveTransitionEnabled,
                ),
        ) {
            val cardShellBaseColor = if (shouldUseFrostedGlass) {
                Color.Transparent
            } else {
                AppSurfaceTokens.cardContainer()
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(cardShellShape)
                    .background(cardShellBaseColor)
                    .videoCardShellReturnCoverAlpha(
                        enabled = useCardShellSharedBounds,
                        bvid = video.bvid,
                        sourceRoute = effectiveSharedElementSourceRoute,
                        isReturningFromDetail = isReturningFromVideoDetail,
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .videoCardShellSharedBoundsOrEmpty(
                        enabled = useCardShellSharedBounds,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        bvid = video.bvid,
                        sourceRoute = effectiveSharedElementSourceRoute,
                        motionSpec = homeSharedTransitionMotionSpec,
                        clipShape = cardShellShape
                    )
                    .clip(cardShellShape)
            ) {
        //  [性能优化] 封面圆角形状缓存（避免重组时重复创建）
        val coverShape = remember(cardCornerRadius) {
            AppShapes.topRounded(cardCornerRadius)
        }

        val coverSharedBoundsEnabled = shouldEnableVideoCoverSharedTransition(
            transitionEnabled = sharedTransitionOwnership.useCoverSharedBounds,
            hasSharedTransitionScope = sharedTransitionScope != null,
            hasAnimatedVisibilityScope = animatedVisibilityScope != null,
        ) && !useCardShellSharedBounds && sharedSourceOwnershipAllowed
        val coverSharedBoundsModifier = if (coverSharedBoundsEnabled) {
            with(requireNotNull(sharedTransitionScope)) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = videoCoverSharedElementKey(
                            bvid = video.bvid,
                            sourceRoute = effectiveSharedElementSourceRoute,
                        )
                    ),
                    animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                    boundsTransform = { initialBounds, targetBounds ->
                        videoSharedElementBoundsTransformSpec(
                            motion = homeSharedTransitionMotionSpec,
                            initialBounds = initialBounds,
                            targetBounds = targetBounds,
                        )
                    },
                    // 禁止默认 Center：非首页预测返回中间态会往屏幕中心飞。
                    resizeMode = resolveVideoCardSharedBoundsResizeMode(),
                    clipInOverlayDuringTransition = OverlayClip(coverShape),
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = coverSharedBoundsModifier
                .fillMaxWidth()
                .videoCardShellReturnCoverAlpha(
                    enabled = useCardShellSharedBounds,
                    bvid = video.bvid,
                    sourceRoute = effectiveSharedElementSourceRoute,
                    isReturningFromDetail = isReturningFromVideoDetail,
                )
                .testTag("home_video_cover")
                .aspectRatio(coverAspectRatio)
                .clip(coverShape)
                .onGloballyPositioned { coordinates ->
                    coverCoordsRef.value = coordinates
                    val rootTop = coordinates.boundsInRoot().top
                    if (screenMetrics.heightPx > 0f) {
                        cardYFraction.floatValue = (rootTop / screenMetrics.heightPx).coerceIn(0f, 1f)
                    }
                }
                .background(MaterialTheme.colorScheme.surfaceVariant)
                //  [交互优化] 封面区域：点击跳转
                .pointerInput(isLongPressEnabled, onLongClick, onDismiss, onWatchLater, onUnfavorite) {
                    val hasPreviewAction = isLongPressEnabled && onLongClick != null
                    val hasLongPressMenu = isLongPressEnabled && (onDismiss != null || onWatchLater != null || onUnfavorite != null)
                    detectTapGestures(
                        onLongPress = if (isLongPressEnabled && (hasPreviewAction || hasLongPressMenu)) {
                            { pressOffset ->
                                if (hasPreviewAction) {
                                    haptic(HapticType.HEAVY)
                                    onLongClick(video)
                                } else if (shouldOpenLongPressMenu(hasPreviewAction, hasLongPressMenu)) {
                                    haptic(HapticType.HEAVY)
                                    if (onUnfavorite != null && onDismiss == null && onWatchLater == null) {
                                        showUnfavoriteDialog = true
                                    } else {
                                        openDismissMenu(coverCoordsRef.value, pressOffset)
                                    }
                                }
                            }
                        } else null,
                        onTap = {
                            triggerCardClick()
                        }
                    )
                }
        ) {
            // crossfade 必须进 remember key：若 clearReturning 后才打开 crossfade，
            // 会新建 ImageRequest 导致 Coil 再跑一次淡入闪烁（快速返回尤其明显）。
            val coverImageRequest = remember(
                requestCoverUrl,
                requestCoverCacheKey,
                coverCrossfadeEnabled,
            ) {
                ImageRequest.Builder(context)
                    .data(requestCoverUrl)
                    .apply {
                        coverRequestSpec?.let { size(it.widthPx, it.heightPx) }
                    }
                    .placeholderMemoryCacheKey(requestCoverCacheKey)
                    .crossfade(coverCrossfadeEnabled)
                    .memoryCacheKey(requestCoverCacheKey)
                    .diskCacheKey(requestCoverCacheKey)
                    .build()
            }
            AsyncImage(
                model = coverImageRequest,
                contentDescription = null,
                onSuccess = { state ->
                    if (homeCardDynamicTintEnabled && coverTint == null) {
                        val bitmap = (state.result.image as? coil3.BitmapImage)?.bitmap
                        if (bitmap != null) {
                            VideoCardCoverColorStore.extractColorAsync(
                                cacheKey = requestCoverCacheKey,
                                bitmap = bitmap,
                                scope = scope
                            ) { extracted ->
                                if (activeCoverCacheKey == requestCoverCacheKey) {
                                    coverTint = extracted
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize(),
                // 官方粉版：居中 Crop；16:9 框配 16:9 投稿封面时基本不裁
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .recordNativeVideoCardLayer(
                        layer = nativeCoverOverlayLayer,
                        freezeProvider = {
                            freezeNativeCardLayer.value &&
                                CardPositionManager.isNativeCoverOverlayLayerCurrentOwner(
                                    nativeCoverOverlayLayer
                                )
                        },
                        bvid = video.bvid,
                        sourceRoute = effectiveSharedElementSourceRoute,
                        enabled = effectiveTransitionEnabled,
                    ),
            ) {
            if (premiumBadgeLabel != null) {
                HomeVideoBadgePill(
                    style = badgeStylePolicy.coverStyle,
                    useRealtimeHaze = badgeEffectVisual.useRealtimeHaze,
                    shape = AppShapes.container(ContainerLevel.Tag),
                    containerColor = BiliPink.copy(alpha = if (badgeStylePolicy.coverStyle == HomeVideoBadgeStyle.GLASS) 0.78f else 1f),
                    borderColor = MediaContrastPalette.Foreground.copy(alpha = 0.24f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppSpacingTokens.Small)
                ) {
                    AppText(
                        text = premiumBadgeLabel,
                        color = MediaContrastPalette.Foreground,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            //  底部渐变遮罩

            if (scrollLitePolicy.showCoverGradientMask) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MediaContrastPalette.Scrim.copy(alpha = 0.3f),
                                    MediaContrastPalette.Scrim.copy(alpha = 0.78f),
                                )
                            )
                        )
                )
            }

            if (scrollLitePolicy.showHistoryProgressBar && showHistoryProgressBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(coverOverlayBottomLayout.historyProgressBarHeightDp.dp)
                        .background(MediaContrastPalette.Foreground.copy(alpha = 0.24f))
                )
                if (historyProgressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(historyProgressFraction)
                            .height(coverOverlayBottomLayout.historyProgressBarHeightDp.dp)
                            .background(historyProgressBarColor)
                    )
                }
            }

            if (scrollLitePolicy.showCompactStatsOnCover) {
                val compactStatsLayout = remember(
                    compactStatsAvailableWidthDp,
                    primaryStatText,
                    secondaryStatText,
                    onlineCount,
                    showDurationOnCover,
                    showDurationOutside,
                    durationBadgeMinWidth,
                    durationStatMinWidthDp
                ) {
                    resolveVideoCardCompactCoverStatsLayout(
                        availableWidthDp = compactStatsAvailableWidthDp,
                        primaryStatText = primaryStatText,
                        secondaryStatText = secondaryStatText,
                        hasOnlineCount = onlineCount.isNotEmpty(),
                        durationBadgeMinWidthDp = if (showDurationOnCover) {
                            durationBadgeMinWidth.value
                        } else {
                            0f
                        },
                        durationStatMinWidthDp = if (showDurationOutside) {
                            durationStatMinWidthDp
                        } else {
                            0f
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            start = AppSpacingTokens.Small,
                            top = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro,
                            end = AppSpacingTokens.Small,
                            bottom = coverOverlayBottomLayout.compactStatsBottomPaddingDp.dp
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = compactStatsLayout.statsEndPaddingDp.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro)
                    ) {
                        val compactViewsModifier = Modifier.widthIn(min = compactStatsLayout.primaryMinWidthDp.dp)
                        HomeVideoBadgePill(
                            modifier = compactViewsModifier,
                            style = badgeStylePolicy.coverStyle,
                            useRealtimeHaze = badgeEffectVisual.useRealtimeHaze,
                            shape = AppShapes.container(ContainerLevel.Pill),
                            containerColor = coverPillColors.containerColor,
                            borderColor = coverPillColors.borderColor
                        ) {
                            AppIcon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                                tint = MediaContrastPalette.Foreground.copy(alpha = 0.94f)
                            )
                            AppText(
                                text = primaryStatText,
                                color = MediaContrastPalette.Foreground.copy(alpha = 0.94f),
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                fontWeight = FontWeight.Medium,
                                style = coverOverlayTextStyle,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        }

                        if (compactStatsLayout.showSecondaryStat && secondaryStatText != null) {
                            val compactDanmakuModifier = Modifier.widthIn(min = compactStatsLayout.secondaryMinWidthDp.dp)
                            HomeVideoBadgePill(
                                modifier = compactDanmakuModifier,
                                style = badgeStylePolicy.coverStyle,
                                useRealtimeHaze = badgeEffectVisual.useRealtimeHaze,
                                shape = AppShapes.container(ContainerLevel.Pill),
                                containerColor = coverPillColors.containerColor,
                                borderColor = coverPillColors.borderColor
                            ) {
                                AppIcon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                                    tint = MediaContrastPalette.Foreground.copy(alpha = 0.90f)
                                )
                                AppText(
                                    text = secondaryStatText,
                                    color = MediaContrastPalette.Foreground.copy(alpha = 0.90f),
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    fontWeight = FontWeight.Medium,
                                    style = coverOverlayTextStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }

                        if (compactStatsLayout.showOnlineCount) {
                            HomeVideoBadgePill(
                                modifier = Modifier.weight(1f, fill = false),
                                style = badgeStylePolicy.coverStyle,
                                useRealtimeHaze = badgeEffectVisual.useRealtimeHaze,
                                shape = AppShapes.container(ContainerLevel.Pill),
                                containerColor = coverPillColors.containerColor,
                                borderColor = coverPillColors.borderColor
                            ) {
                                AppIcon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                                    tint = MediaContrastPalette.Foreground.copy(alpha = 0.90f)
                                )
                                AppText(
                                    text = onlineCount,
                                    color = MediaContrastPalette.Foreground.copy(alpha = 0.90f),
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    fontWeight = FontWeight.Medium,
                                    style = coverOverlayTextStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }

                        // 时长随统计行显示（OUTSIDE_COVER）：闹钟图标 + 时长，与其他统计 pill 同结构。
                        if (showDurationOutside) {
                            HomeVideoBadgePill(
                                style = badgeStylePolicy.coverStyle,
                                useRealtimeHaze = badgeEffectVisual.useRealtimeHaze,
                                shape = AppShapes.container(ContainerLevel.Pill),
                                containerColor = coverPillColors.containerColor,
                                borderColor = coverPillColors.borderColor
                            ) {
                                AppIcon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                                    tint = MediaContrastPalette.Foreground.copy(alpha = 0.90f)
                                )
                                AppText(
                                    text = durationText,
                                    color = MediaContrastPalette.Foreground.copy(alpha = 0.90f),
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    fontWeight = FontWeight.Medium,
                                    style = coverOverlayTextStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }

                    //  时长标签 (与播放量/评论数同行对齐)
                    if (showDurationOnCover) {
                        AppText(
                            text = durationText,
                            color = MediaContrastPalette.Foreground,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = FontWeight.Medium,
                            style = coverOverlayTextStyle,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            } else {
                //  非贴封面模式时，时长标签仍独立显示在右下角
                if (showDurationOnCover) {
                    AppText(
                        text = durationText,
                        color = MediaContrastPalette.Foreground,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Medium,
                        style = coverOverlayTextStyle,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                                bottom = coverOverlayBottomLayout.floatingDurationBottomPaddingDp.dp
                            )
                    )
                }
            }
            }

        }
        
        val infoSurfaceShape = remember(cardCornerRadius) {
            AppShapes.bottomRounded(cardCornerRadius)
        }
        val infoContainerModifier = if (shouldUseFrostedGlass) {
            // Wallpaper-only Haze for realtime blur (never main content HazeState).
            val hazeModifier = if (
                infoSurfaceAppearance.useRealtimeHaze && wallpaperHazeState != null
            ) {
                Modifier.unifiedBlur(
                    hazeState = wallpaperHazeState,
                    shape = infoSurfaceShape,
                    surfaceType = BlurSurfaceType.BOTTOM_BAR,
                    isScrolling = false,
                    isTransitionRunning = false,
                    forceLowBudget = false
                )
            } else {
                Modifier
            }
            // Wallpaper-only Miuix blur. The source is attached to the sibling wallpaper layer,
            // so this surface never samples its own cover, text, or the feed content.
            val wallpaperBlurRadiusPx = with(density) { 24.dp.toPx() }
            val wallpaperBackdropModifier = if (
                useRealtimeWallpaperBackdrop &&
                homeWallpaperBackdrop != null
            ) {
                Modifier.drawBackdrop(
                    backdrop = homeWallpaperBackdrop,
                    shape = { infoSurfaceShape },
                    effects = {
                        blur(wallpaperBlurRadiusPx, wallpaperBlurRadiusPx)
                    }
                )
            } else {
                Modifier
            }
            val baseContainerColor = AppSurfaceTokens.cardContainer()
            val baseBorderColor = MediaContrastPalette.Foreground
            Modifier
                .fillMaxWidth()
                .clip(infoSurfaceShape)
                .then(hazeModifier)
                .then(wallpaperBackdropModifier)
                .onPlaced { coordinates ->
                    infoLayoutCoordinates.value = coordinates
                }
                .drawBehind {
                    if (homeCardDynamicTintEnabled && !useRealtimeWallpaperBackdrop) {
                        // 1. 在 Draw 阶段按需读取滚动 tick，零重组实现 120fps 实时刷新跟随
                        homeScrollTickProvider?.invoke()

                        // 2. 动态获取当前帧卡片底部组件在窗口根布局的物理 Y 坐标
                        val coords = infoLayoutCoordinates.value
                        val currentY = if (coords != null && coords.isAttached) {
                            coords.positionInRoot().y
                        } else null

                        val yFrac = if (currentY != null && screenMetrics.heightPx > 0f) {
                            (currentY / screenMetrics.heightPx).coerceIn(0f, 1f)
                        } else {
                            cardYFraction.floatValue
                        }
                        val drawSpec = resolveVideoCardAmbientDrawSpec(
                            wallpaperPalette = wallpaperPalette,
                            yFraction = yFrac,
                            coverTint = if (animatedCoverTint.alpha > 0f) animatedCoverTint else null,
                            wallpaperTintEnabled = wallpaperTintEnabled,
                            isDarkTheme = isDarkCardTheme,
                            defaultContainerColor = baseContainerColor,
                            defaultBorderColor = baseBorderColor,
                            isDataSaverActive = isDataSaverActive,
                            frostedGlassEnabled = true
                        )
                        drawRect(color = drawSpec.containerColor)
                        if (drawSpec.coverGlowAlpha > 0f && animatedCoverTint.alpha > 0f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        animatedCoverTint.copy(alpha = drawSpec.coverGlowAlpha),
                                        animatedCoverTint.copy(alpha = drawSpec.coverGlowAlpha * 0.35f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.85f
                                )
                            )
                        }
                    } else {
                        val realtimeAlpha = if (isDarkCardTheme) 0.24f else 0.16f
                        drawRect(
                            color = baseContainerColor.copy(
                                alpha = if (useRealtimeWallpaperBackdrop) {
                                    realtimeAlpha
                                } else {
                                    infoSurfaceAppearance.containerAlpha
                                }
                            )
                        )
                    }
                }
                .border(
                    width = 0.5.dp,
                    color = if (homeCardDynamicTintEnabled && !useRealtimeWallpaperBackdrop) {
                        val borderAlpha = if (isDarkCardTheme) 0.30f else 0.45f
                        val baseBorder = Color.White.copy(alpha = borderAlpha)
                        if (
                            shouldUseCoverTintForCard(wallpaperTintEnabled, coverTint) &&
                            animatedCoverTint.alpha > 0f
                        ) {
                            val borderBlend = if (isDarkCardTheme) 0.50f else 0.40f
                            androidx.compose.ui.graphics.lerp(baseBorder, animatedCoverTint, borderBlend).copy(alpha = borderAlpha)
                        } else {
                            baseBorder
                        }
                    } else {
                        baseBorderColor.copy(
                            alpha = if (useRealtimeWallpaperBackdrop) {
                                if (isDarkCardTheme) 0.14f else 0.22f
                            } else {
                                infoSurfaceAppearance.borderAlpha
                            }
                        )
                    },
                    shape = infoSurfaceShape
                )
                .padding(
                    horizontal = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                    vertical = if (compactMetadata) AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro else AppSpacingTokens.Small
                )
        } else {
            val ambientCoverGlowModifier = if (
                homeCardDynamicTintEnabled &&
                shouldUseCoverTintForCard(wallpaperTintEnabled, coverTint) &&
                animatedCoverTint.alpha > 0f
            ) {
                Modifier.drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedCoverTint.copy(alpha = if (isDarkCardTheme) 0.16f else 0.10f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.75f
                        )
                    )
                }
            } else {
                Modifier
            }
            Modifier
                .fillMaxWidth()
                .then(ambientCoverGlowModifier)
                .padding(
                    start = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                    top = AppSpacingTokens.None,
                    end = AppSpacingTokens.Small + AppSpacingTokens.Micro,
                    bottom = if (compactMetadata) {
                        AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro
                    } else {
                        AppSpacingTokens.Small
                    }
                )
        }

        Box(
            modifier = infoContainerModifier.videoCardShellReturnChromeAlpha(
                enabled = useCardShellSharedBounds,
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                isReturningFromDetail = isReturningFromVideoDetail,
                isQuickReturnFromDetail = isQuickReturningFromVideoDetail,
            )
        ) {
        Column {
        if (!shouldUseFrostedGlass) {
            Spacer(modifier = Modifier.height(if (compactMetadata) AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro else AppSpacingTokens.Small))
        }

        AppText(
            text = highlightedTitle ?: AnnotatedString(video.title),
            maxLines = videoCardTitleMaxLines(titleMaxLines),
            minLines = titleMinLines,
            overflow = videoCardTitleOverflow(),
            style = contentTypography.title.copy(
                color = adaptiveContentColors.titleColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "视频标题: ${video.title}" }
                .onGloballyPositioned { coordinates ->
                    titleCoordsRef.value = coordinates
                }
                //  [交互优化] 标题区域：长按弹出菜单，点击跳转
                .pointerInput(isLongPressEnabled, onLongClick, onDismiss, onWatchLater, onUnfavorite) {
                    val hasPreviewAction = isLongPressEnabled && onLongClick != null
                    val hasLongPressMenu = isLongPressEnabled && (onDismiss != null || onWatchLater != null || onUnfavorite != null)
                    detectTapGestures(
                        onLongPress = if (isLongPressEnabled && (hasPreviewAction || hasLongPressMenu)) {
                            { pressOffset ->
                                if (hasPreviewAction) {
                                    haptic(HapticType.HEAVY)
                                    onLongClick(video)
                                } else if (shouldOpenLongPressMenu(hasPreviewAction, hasLongPressMenu)) {
                                    haptic(HapticType.HEAVY)
                                    if (onUnfavorite != null && onDismiss == null && onWatchLater == null) {
                                        showUnfavoriteDialog = true
                                    } else {
                                        openDismissMenu(titleCoordsRef.value, pressOffset)
                                    }
                                }
                            }
                        } else null,
                        onTap = {
                            triggerCardClick()
                        }
                    )
                }
        )
        
        Spacer(modifier = Modifier.height(if (compactMetadata) AppSpacingTokens.ExtraSmall else AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro))

        Column(
            modifier = resolveVideoCardMetadataModifier(hasTrailingCardAction)
        ) {
        val metadataColors = resolveHomeVideoCardMetadataColors(
            onSurfaceColor = adaptiveContentColors.titleColor,
            onSurfaceVariantColor = adaptiveContentColors.subtitleColor,
        )

        val resolvedUpBadgeVisibility = com.android.purebilibili.core.ui.LocalUpBadgeVisibility.current
        if (scrollLitePolicy.showSecondaryStatsRow) {
            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)
            ) {
                HorizontalVideoStatRow(
                    playText = primaryStatText,
                    danmakuText = secondaryStatText.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (onlineCount.isNotEmpty()) {
                    HomeVideoBadgePill(
                        style = badgeStylePolicy.infoStyle,
                        useRealtimeHaze = badgeEffectVisual.useRealtimeHaze,
                        shape = AppShapes.container(ContainerLevel.Pill),
                        containerColor = inlinePillColors.containerColor,
                        borderColor = inlinePillColors.borderColor
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(AppSpacingTokens.Medium),
                            tint = adaptiveContentColors.subtitleColor
                        )
                        AppText(
                            text = onlineCount,
                            color = adaptiveContentColors.subtitleColor,
                            style = contentTypography.statistic.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

        VideoCardOwnerMetadata(
            video = video,
            isFollowing = isFollowing,
            showUpBadge = showUpBadge ?: resolvedUpBadgeVisibility.showBadges,
            showUpAvatar = showUpAvatar ?: resolvedUpBadgeVisibility.showAvatars,
            upFollowerCount = upFollowerCount,
            upVideoCount = upVideoCount,
            infoBadgeStyle = badgeStylePolicy.infoStyle,
            inlinePillColors = inlinePillColors,
            metadataColors = metadataColors,
            onUpClick = onUpClick,
            modifier = resolveVideoCardMetadataRowModifier()
        )

        VideoCardDurationPublishRow(
            durationText = durationText.takeIf { showDurationOutside }.orEmpty(),
            publishTimeText = publishTimeRowText,
            emphasizePublishTime = emphasizePublishTime,
            publishTimeColor = metadataColors.publishTimeColor,
            topSpacing = if (compactMetadata) {
                AppSpacingTokens.ExtraSmall
            } else {
                AppSpacingTokens.Small - AppSpacingTokens.Micro
            },
        )

        }
        }

        if (hasTrailingCardAction) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onUnfavorite != null) {
                    Box(
                        modifier = Modifier
                            .size(AppChromeSizeTokens.MinimumTouchTarget)
                            .clickable {
                                haptic(HapticType.MEDIUM)
                                showUnfavoriteDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "取消收藏",
                            modifier = Modifier.size(AppSpacingTokens.Large),
                            tint = adaptiveContentColors.subtitleColor.copy(alpha = 0.7f)
                        )
                    }
                }

                if (hasOverflowMenu) {
                    Box(
                        modifier = Modifier
                            .size(AppChromeSizeTokens.MinimumTouchTarget)
                            .semantics { contentDescription = "更多操作" }
                            .onGloballyPositioned { coordinates ->
                                menuButtonCoordsRef.value = coordinates
                            }
                            .clickable {
                                haptic(HapticType.LIGHT)
                                openDismissMenu(menuButtonCoordsRef.value, null)
                            },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AppText(
                            text = "⋮",
                            color = adaptiveContentColors.subtitleColor,
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                end = AppSpacingTokens.ExtraSmall,
                                bottom = AppSpacingTokens.ExtraSmall
                            )
                        )
                    }
                }
            }
        }

            }
        }
    }
        
        // 菜单需要挂在一个本地小锚点上，避免 DropdownMenu 在整张卡片根节点右侧 fallback 时反向偏移。
        //
        // 这里保留非 lambda 版 offset（lint 的 UseOfNonLambdaOffsetOverload 会报）：
        // menuOffset 只在每次长按时变一次，不是逐帧动画值，lambda 版没有实际收益；
        // 而改写形式会打断 VideoCardLongPressPolicyTest 对上面那次回归修复的字面守卫。
        // 为零收益去动别人的回归守卫不划算。
        @Suppress("UseOfNonLambdaOffsetOverload")
        Box(
            modifier = Modifier
                .offset(x = menuOffset.x, y = menuOffset.y)
                .size(AppSpacingTokens.Micro / 2)
        ) {
            AppDropdownMenu(
                expanded = showDismissMenu,
                onDismissRequest = { showDismissMenu = false },
                offset = DpOffset.Zero
            ) {
                // 稍后再看
                if (onWatchLater != null) {
                    AppDropdownMenuItem(
                        text = {
                            AppText(
                                "🕐 稍后再看",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showDismissMenu = false
                            onWatchLater.invoke()
                        }
                    )
                }

                // 取消收藏 (仅在收藏页显示)
                if (onUnfavorite != null) {
                     AppDropdownMenuItem(
                        text = {
                            AppText(
                                "💔 取消收藏",
                                color = MaterialTheme.colorScheme.error  // 使用错误色强调删除操作
                            )
                        },
                        onClick = {
                            showDismissMenu = false
                            // onUnfavorite.invoke() -> 改为弹窗确认
                            showUnfavoriteDialog = true
                        }
                    )
                }

                // 不感兴趣 (放第一位，方便操作) -> 改回下方
                if (onDismiss != null) {
                    AppDropdownMenuItem(
                        text = {
                            AppText(
                                dismissMenuText,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showDismissMenu = false
                            onDismiss.invoke()
                        }
                    )
                }
            }
        }
    }
    
    
    if (showUnfavoriteDialog) {
        AppAlertDialog(
            onDismissRequest = { showUnfavoriteDialog = false },
            title = { AppText("取消收藏") },
            text = { AppText("确定要将此视频从收藏夹中移除吗？") },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        showUnfavoriteDialog = false
                        onUnfavorite?.invoke()
                    }
                ) {
                    AppText("移除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showUnfavoriteDialog = false }) {
                    AppText("取消")
                }
            }
        )
    }

}

@Composable
internal fun HomeVideoBadgePill(
    style: HomeVideoBadgeStyle,
    shape: Shape,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    /** When true, sample [LocalMainHazeState] like the bottom bar (realtime blur). */
    useRealtimeHaze: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    if (style == HomeVideoBadgeStyle.GLASS) {
        // Wallpaper-only HazeState (sibling source), never main content HazeState —
        // badges live inside the main hazeSource and would SO the render tree otherwise.
        val hazeState = LocalWallpaperHazeState.current
        // Match bottom bar: keep blur on while scrolling (isScrolling=false → full visual path).
        val glassModifier = if (useRealtimeHaze && hazeState != null) {
            modifier.unifiedBlur(
                hazeState = hazeState,
                shape = shape,
                surfaceType = BlurSurfaceType.BOTTOM_BAR,
                // Bottom bar intentionally does not zero blur on feed scroll.
                isScrolling = false,
                isTransitionRunning = false,
                forceLowBudget = false
            )
        } else {
            modifier
        }
        // Slightly clearer fill when realtime haze already provides frosted backdrop.
        val surfaceColor = if (useRealtimeHaze && hazeState != null) {
            containerColor.copy(alpha = (containerColor.alpha * 0.55f).coerceIn(0.08f, 0.45f))
        } else {
            containerColor
        }
        AppSurface(
            modifier = glassModifier,
            shape = shape,
            color = surfaceColor,
            border = BorderStroke(AppSpacingTokens.Micro * 0.4f, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro, vertical = AppSpacingTokens.ExtraSmall - AppSpacingTokens.Micro / 2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Micro),
                content = content
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Micro),
            content = content
        )
    }
}

/**
 * 简化版视频网格项 (用于搜索结果等)
 * 注意: onClick 只接收 bvid，不接收 cid
 */
@Composable
fun VideoGridItem(video: VideoItem, index: Int, onLongClick: ((VideoItem) -> Unit)? = null, onClick: (String) -> Unit) {
    ElegantVideoCard(video, index, onLongClick = onLongClick) { bvid, _ -> onClick(bvid) }
}
