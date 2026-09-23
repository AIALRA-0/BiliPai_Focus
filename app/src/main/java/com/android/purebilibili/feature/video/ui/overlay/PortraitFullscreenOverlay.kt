package com.android.purebilibili.feature.video.ui.overlay
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.MoreVert
import com.android.purebilibili.feature.video.ui.components.PlaybackSpeed
import com.android.purebilibili.feature.video.ui.components.DolbyBadge
import com.android.purebilibili.feature.video.ui.components.HiResBadge
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio

import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppIconButtonDefaults
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoshotData
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.feature.video.ui.components.CollectionRow
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

internal fun shouldShowPortraitViewCount(viewCount: Int, compactMode: Boolean): Boolean {
    return viewCount > 0 && !compactMode
}

internal fun shouldShowPortraitTopMoreAction(): Boolean = false

internal fun resolvePortraitProgressTimeLabel(
    positionMs: Long,
    durationMs: Long
): String {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    val safePositionMs = if (safeDurationMs > 0L) {
        positionMs.coerceIn(0L, safeDurationMs)
    } else {
        positionMs.coerceAtLeast(0L)
    }
    return "${FormatUtils.formatDuration(safePositionMs)} / ${FormatUtils.formatDuration(safeDurationMs)}"
}

/**
 * 竖屏全屏覆盖层 (B站官方风格) - 重构版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitFullscreenOverlay(
    title: String,
    ugcSeason: UgcSeason? = null,
    currentBvid: String = "",
    currentCid: Long = 0L,
    authorName: String = "",
    authorFace: String = "",
    isPlaying: Boolean,
    progress: PlayerProgress,
    
    // 互动数据
    statView: Int = 0,
    statLike: Int = 0,
    statCoin: Int = 0,
    statDanmaku: Int = 0,
    statReply: Int = 0,
    statFavorite: Int = 0,
    statShare: Int = 0,
    
    // 互动状态
    isLiked: Boolean,
    isCoined: Boolean,
    isFavorited: Boolean,
    onLikeClick: () -> Unit,
    onLikeLongClick: () -> Unit = {},
    onCoinClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    
    // 关注状态 (Follow status)
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {},
    
    // [新增] 详情点击
    onDetailClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    
    // 控制状态
    currentSpeed: Float,
    currentQualityLabel: String,
    currentAudioQualityLabel: String,
    isHiResAudioSelected: Boolean,
    isDolbyAudioSelected: Boolean,
    currentRatio: VideoAspectRatio,
    danmakuEnabled: Boolean,
    isStatusBarHidden: Boolean,
    
    // 显示状态
    showControls: Boolean = true,
    commentExpansionProgress: Float = 0f,
    videoshotData: VideoshotData? = null,
    videoAspectRatio: Float? = null,
    isPlaybackRecovering: Boolean = false,
    
    // 回调
    onBack: () -> Unit,
    onHomeClick: () -> Unit = onBack,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    seekPositionMs: Long = progress.current,
    isSeekScrubbing: Boolean = false,
    onSeekDragStart: (Long) -> Unit = {},
    onSeekDragUpdate: (Long) -> Unit = {},
    onSeekDragCancel: () -> Unit = {},
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onAudioQualityClick: () -> Unit,
    onRatioClick: () -> Unit,
    showSubtitleChip: Boolean = false,
    subtitleEnabled: Boolean = false,
    onSubtitleClick: () -> Unit = {},
    onDanmakuToggle: () -> Unit,
    onDanmakuInputClick: () -> Unit,
    onToggleStatusBar: () -> Unit,
    onRotateToLandscape: () -> Unit,
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolvePortraitFullscreenOverlayLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val progressLayoutPolicy = remember(configuration.screenWidthDp) {
        resolvePortraitProgressBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val progressTimeLabel = remember(seekPositionMs, progress.current, progress.duration, isSeekScrubbing) {
        resolvePortraitProgressTimeLabel(
            positionMs = if (isSeekScrubbing) seekPositionMs else progress.current,
            durationMs = progress.duration
        )
    }
    val commentProgress = commentExpansionProgress.coerceIn(0f, 1f)
    val commentOverlayAlpha = (1f - commentProgress).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val commentOverlayOffsetPx = with(density) { 24.dp.toPx() } * commentProgress

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        
        // 控件层动画
        AnimatedVisibility(
            visible = showControls && commentOverlayAlpha > 0.001f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = commentOverlayAlpha
                            translationY = -commentOverlayOffsetPx
                        }
                ) {
                    PortraitReadableTextScrims(layoutPolicy = layoutPolicy)
                }
                
                // 1. 顶部栏 (返回 + 观看人数)
                PortraitTopControlBar(
                    layoutPolicy = layoutPolicy,
                    onBack = onBack,
                    onHomeClick = onHomeClick,
                    viewCount = statView,
                    onSearchClick = onSearchClick,
                    onMoreClick = onMoreClick,
                    modifier = Modifier.graphicsLayer {
                        alpha = commentOverlayAlpha
                        translationY = -commentOverlayOffsetPx
                    }
                )

                // 2. 右侧互动栏 (不再包含头像)
                PortraitInteractionBar(
                    isLiked = isLiked,
                    likeCount = statLike,
                    isCoined = isCoined,
                    coinCount = statCoin,
                    isFavorited = isFavorited,
                    favoriteCount = statFavorite,
                    commentCount = statReply.takeIf { it > 0 } ?: statDanmaku, // 优先用评论数，没有则用弹幕数代替展示
                    shareCount = statShare,
                    onLikeClick = onLikeClick,
                    onLikeLongClick = onLikeLongClick,
                    onCoinClick = onCoinClick,
                    onFavoriteClick = onFavoriteClick,
                    onCommentClick = onCommentClick,
                    onShareClick = onShareClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .graphicsLayer {
                            alpha = commentOverlayAlpha
                            translationX = commentOverlayOffsetPx
                        }
                )
                
                // 3. 底部区域 (信息 + 进度条 + 输入栏占位)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = commentOverlayAlpha
                            translationY = commentOverlayOffsetPx
                        }
                ) {
                    // 视频信息 (Video Info)
                    PortraitVideoInfo(
                        layoutPolicy = layoutPolicy,
                        authorName = authorName,
                        authorFace = authorFace,
                        title = title,
                        ugcSeason = ugcSeason,
                        currentBvid = currentBvid,
                        currentCid = currentCid,
                        isPlaying = isPlaying,
                        onCollectionClick = onDetailClick,
                        isFollowing = isFollowing,
                        onFollowClick = onFollowClick,
                        onTitleClick = onTitleClick,
                        onAuthorClick = onAuthorClick,
                        modifier = Modifier
                            .fillMaxWidth(layoutPolicy.infoWidthFraction)
                            .padding(horizontal = layoutPolicy.infoHorizontalPaddingDp.dp)
                            .padding(bottom = layoutPolicy.infoBottomPaddingDp.dp)
                    )

                    PortraitProgressControlStrip(
                        timeLabel = progressTimeLabel,
                        currentSpeed = currentSpeed,
                        currentQualityLabel = currentQualityLabel,
                        currentAudioQualityLabel = currentAudioQualityLabel,
                        isHiResAudioSelected = isHiResAudioSelected,
                        isDolbyAudioSelected = isDolbyAudioSelected,
                        currentRatioLabel = currentRatio.displayName,
                        showSubtitleChip = showSubtitleChip,
                        subtitleEnabled = subtitleEnabled,
                        onSpeedClick = onSpeedClick,
                        onQualityClick = onQualityClick,
                        onAudioQualityClick = onAudioQualityClick,
                        onRatioClick = onRatioClick,
                        onSubtitleClick = onSubtitleClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = progressLayoutPolicy.horizontalPaddingDp.dp)
                            .padding(bottom = 2.dp)
                    )
                    
                    // 底部进度条 (Progress Bar)
                    PortraitBottomContainer(
                        progress = if (progress.duration > 0) progress.current.toFloat() / progress.duration else 0f,
                        duration = progress.duration,
                        bufferProgress = if (progress.duration > 0L) {
                            progress.buffered.toFloat() / progress.duration.toFloat()
                        } else {
                            0f
                        },
                        seekPositionMs = seekPositionMs,
                        isSeekScrubbing = isSeekScrubbing,
                        onSeek = onSeek,
                        onSeekStart = onSeekStart,
                        onSeekDragStart = onSeekDragStart,
                        onSeekDragUpdate = onSeekDragUpdate,
                        onSeekDragCancel = onSeekDragCancel,
                        videoshotData = videoshotData,
                        videoAspectRatio = videoAspectRatio
                    )
                    
                    // 底部输入栏占位 (Input Bar Spacer)
                    // Input Bar height is usually around 50-60dp.
                    // Since Input Bar is an overlay at Alignment.BottomCenter in the outer Box (see below),
                    // we need to add a spacer here so the progress bar sits *above* the input bar, not behind it.
                    // Or, we render the Input Bar *here* in the Column?
                    // "PortraitBottomInputBar" logic:
                    // If we put it here, it will be stacked. 
                    // Let's verify where PortraitBottomInputBar is placed in the original code.
                    // Original: Modifier.align(Alignment.BottomCenter)
                    
                    // Let's add a Spacer. Assuming Input Bar height ~50dp + margins.
                    Spacer(
                        modifier = Modifier.height(
                            (layoutPolicy.bottomInputSpacerHeightDp + layoutPolicy.bottomInputLiftDp).dp
                        )
                    )
                }

                // 4. 底部输入栏 (Input Bar) - Keep strict bottom alignment (Overlay)
                PortraitBottomInputBar(
                    onInputClick = onDanmakuInputClick,
                    danmakuEnabled = danmakuEnabled,
                    onDanmakuToggle = onDanmakuToggle,
                    onRotateClick = onRotateToLandscape,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = layoutPolicy.bottomInputLiftDp.dp)
                        .graphicsLayer {
                            alpha = commentOverlayAlpha
                            translationY = commentOverlayOffsetPx
                        }
                )

                AnimatedVisibility(
                    visible = isPlaybackRecovering,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AppSurface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = AppShapes.container(ContainerLevel.Card)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppCircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AppText(
                                text = "正在恢复播放...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }

        // 控件隐藏时仍显示底部细进度条，方便随时感知播放进度
        if (!showControls) {
            PersistentBottomProgressBar(
                current = progress.current,
                duration = progress.duration,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PortraitReadableTextScrims(
    layoutPolicy: PortraitFullscreenOverlayLayoutPolicy
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 亮色视频会吞掉白色标题和顶部图标，只在文字覆盖区下方加渐变暗层。
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(layoutPolicy.topScrimHeightDp.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = layoutPolicy.topScrimStartAlpha),
                            1f to Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(layoutPolicy.bottomTextScrimHeightDp.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.42f to Color.Black.copy(alpha = layoutPolicy.bottomTextScrimEndAlpha * 0.46f),
                            1f to Color.Black.copy(alpha = layoutPolicy.bottomTextScrimEndAlpha)
                        )
                    )
                )
        )
    }
}

@Composable
private fun PortraitProgressControlStrip(
    timeLabel: String,
    currentSpeed: Float,
    currentQualityLabel: String,
    currentAudioQualityLabel: String,
    isHiResAudioSelected: Boolean,
    isDolbyAudioSelected: Boolean,
    currentRatioLabel: String,
    showSubtitleChip: Boolean = false,
    subtitleEnabled: Boolean = false,
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onAudioQualityClick: () -> Unit,
    onRatioClick: () -> Unit,
    onSubtitleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        // Keep the time readout closer to the progress bar on portrait video.
        modifier = modifier.height(46.dp).padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = timeLabel,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showSubtitleChip) {
            PortraitChromeChip(
                label = "字幕",
                highlighted = subtitleEnabled,
                onClick = onSubtitleClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        PortraitChromeChip(
            label = currentAudioQualityLabel,
            highlighted = false,
            showHiResBadge = isHiResAudioSelected,
            showDolbyBadge = isDolbyAudioSelected,
            onClick = onAudioQualityClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        PortraitChromeChip(
            label = currentQualityLabel,
            highlighted = false,
            onClick = onQualityClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        PortraitChromeChip(
            label = currentRatioLabel,
            highlighted = false,
            onClick = onRatioClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        PortraitChromeChip(
            label = PlaybackSpeed.formatSpeed(currentSpeed),
            highlighted = currentSpeed != 1.0f,
            onClick = onSpeedClick
        )
    }
}

@Composable
private fun PortraitChromeChip(
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showHiResBadge: Boolean = false,
    showDolbyBadge: Boolean = false
) {
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Pill),
        color = Color.White.copy(alpha = 0.14f),
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.White
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showHiResBadge) {
                Spacer(modifier = Modifier.width(4.dp))
                HiResBadge()
            }
            if (showDolbyBadge) {
                Spacer(modifier = Modifier.width(4.dp))
                DolbyBadge()
            }
        }
    }
}

/**
 * 顶部控制区
 */
@Composable
private fun PortraitTopControlBar(
    layoutPolicy: PortraitFullscreenOverlayLayoutPolicy,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    viewCount: Int,
    onSearchClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = layoutPolicy.topHorizontalPaddingDp.dp,
                vertical = layoutPolicy.topVerticalPaddingDp.dp
            ),
    ) {
        // 左侧：返回 + 观看人数
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconButton(
                onClick = onBack,
                colors = AppIconButtonDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.size(layoutPolicy.topBackButtonSizeDp.dp)
            ) {
                AppIcon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.topBackIconSizeDp.dp)
                )
            }
            AppIconButton(
                onClick = onHomeClick,
                colors = AppIconButtonDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.size(layoutPolicy.topBackButtonSizeDp.dp)
            ) {
                AppIcon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = "主界面",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.topBackIconSizeDp.dp)
                )
            }
            Spacer(modifier = Modifier.width(layoutPolicy.topViewCountStartSpacingDp.dp))
            if (shouldShowPortraitViewCount(viewCount = viewCount, compactMode = layoutPolicy.compactMode)) {
                AppText(
                    text = "${FormatUtils.formatStat(viewCount.toLong())}播放",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = layoutPolicy.topViewCountFontSp.sp
                )
            }
        }

        // 右上角功能区
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layoutPolicy.topActionSpacingDp.dp)
        ) {
            AppIconButton(onClick = onSearchClick) {
                AppIcon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "搜索",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.topActionIconSizeDp.dp)
                )
            }
            if (shouldShowPortraitTopMoreAction()) {
                AppIconButton(onClick = onMoreClick) {
                    AppIcon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "菜单",
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.topActionIconSizeDp.dp)
                    )
                }
            }
        }
    }
}

/**
 * 底部视频信息 (重构：头像在左下角)
 */
@Composable
private fun PortraitVideoInfo(
    layoutPolicy: PortraitFullscreenOverlayLayoutPolicy,
    authorName: String,
    authorFace: String,
    title: String,
    ugcSeason: UgcSeason? = null,
    currentBvid: String = "",
    currentCid: Long = 0L,
    isPlaying: Boolean = false,
    onCollectionClick: () -> Unit = {},
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onTitleClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // 第一行：头像 + 名字 + 关注按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = layoutPolicy.authorRowBottomPaddingDp.dp)
                .clickable { onAuthorClick() }
        ) {
            // 头像
            if (authorFace.isNotEmpty()) {
                AsyncImage(
                    model = FormatUtils.fixImageUrl(authorFace),
                    contentDescription = authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(layoutPolicy.avatarSizeDp.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                Spacer(modifier = Modifier.width(layoutPolicy.avatarNameSpacingDp.dp))
            }
            
            // 名字（seed 未带 owner 时勿只渲染裸 `@`）
            AppText(
                text = com.android.purebilibili.feature.video.ui.pager.resolvePortraitAuthorLabel(authorName),
                color = Color.White,
                fontSize = layoutPolicy.authorNameFontSp.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.width(layoutPolicy.avatarNameSpacingDp.dp))
            
            // 关注按钮
            val isFollowed = isFollowing
            val buttonColor = if (isFollowed) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary
            val contentColor = if (isFollowed) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary
            val buttonText = if (isFollowed) "已关注" else "关注"
            val iconVisible = !isFollowed

            AppSurface(
                shape = RoundedCornerShape(layoutPolicy.followButtonCornerRadiusDp.dp),
                color = buttonColor,
                modifier = Modifier
                    .height(layoutPolicy.followButtonHeightDp.dp)
                    .clickable { onFollowClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = layoutPolicy.followButtonHorizontalPaddingDp.dp)
                ) {
                    if (iconVisible) {
                        AppIcon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(layoutPolicy.followIconSizeDp.dp)
                        )
                        Spacer(modifier = Modifier.width(layoutPolicy.followIconSpacingDp.dp))
                    }
                    AppText(
                        text = buttonText,
                        color = contentColor,
                        fontSize = layoutPolicy.followTextFontSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 第二行：标题
        AppText(
            text = title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = layoutPolicy.titleFontSp.sp,
            maxLines = 3,
            lineHeight = layoutPolicy.titleLineHeightSp.sp,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable { onTitleClick() }
        )
        if (ugcSeason != null && ugcSeason.id > 0L) {
            CollectionRow(
                    ugcSeason = ugcSeason,
                    currentBvid = currentBvid,
                    currentCid = currentCid,
                    isPlaying = isPlaying,
                    immersive = true,
                    onClick = onCollectionClick,
                    modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
