package com.android.purebilibili.feature.video.player

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

import com.android.purebilibili.core.theme.resolveAdaptivePrimaryAccentColors
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.bouncyClickable
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo
import androidx.compose.foundation.isSystemInDarkTheme
import com.android.purebilibili.core.theme.ActionLikeDark
import com.android.purebilibili.core.theme.ActionCoinDark
import com.android.purebilibili.core.theme.ActionFavoriteDark
import com.android.purebilibili.core.theme.ActionShareDark
import com.android.purebilibili.core.theme.ActionCommentDark
import com.android.purebilibili.core.ui.components.UserUpBadge
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.rememberAppBookmarkIcon
import com.android.purebilibili.core.ui.rememberAppCoinIcon
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.feature.home.components.cards.VideoCardCoverDurationText

//  [重构] 视频标题区域 (官方B站样式：紧凑布局)
@Composable
fun VideoTitleSection(
    info: ViewInfo,
    onUpClick: (Long) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // 标题行 (可展开)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AppText(
                text = info.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            )
            Spacer(Modifier.width(4.dp))
            AppIcon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(Modifier.height(2.dp))
        
        // 统计行 (官方样式：播放量 • 弹幕 • 日期)
        AppText(
            text = "${FormatUtils.formatStat(info.stat.view.toLong())}  •  ${FormatUtils.formatStat(info.stat.danmaku.toLong())}弹幕  •  ${FormatUtils.formatPublishTime(info.pubdate)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}

//  [新增] 官方布局：标题 + 统计 + 描述 (紧凑排列)
@Composable
fun VideoTitleWithDesc(
    info: ViewInfo
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 4.dp)  //  紧凑布局：减小 vertical padding
    ) {
        // 标题行 (可展开)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AppText(
                text = info.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            )
            Spacer(Modifier.width(4.dp))
            AppIcon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(Modifier.height(2.dp))  //  紧凑布局
        
        // 统计行 (官方样式：播放量 • 弹幕 • 日期)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "${FormatUtils.formatStat(info.stat.view.toLong())}播放  •  ${FormatUtils.formatStat(info.stat.danmaku.toLong())}弹幕  •  ${FormatUtils.formatPublishTime(info.pubdate)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        
        //  描述（动态）- 紧接在统计后面
        if (info.desc.isNotBlank()) {
            Spacer(Modifier.height(4.dp))  //  紧凑布局
            AppText(
                text = info.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize()
            )
        }
    }
}

//  [重构] UP主信息区域 (官方B站样式：蓝色UP主标签)
@Composable
fun UpInfoSection(
    info: ViewInfo,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {},
    onUpClick: (Long) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onUpClick(info.owner.mid) }
            .padding(horizontal = 12.dp, vertical = 4.dp),  //  紧凑布局
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        com.android.purebilibili.feature.video.ui.section.OwnerDecoratedAvatar(
            faceUrl = info.owner.face,
            ownerMid = info.owner.mid,
            modifier = Modifier.size(36.dp),
            badgeSize = 12.dp,
            fallbackOfficialType = info.staff.firstOrNull { it.mid == info.owner.mid }?.official?.type,
            fallbackVipStatus = info.staff.firstOrNull { it.mid == info.owner.mid }?.vip?.status,
        )
        
        Spacer(Modifier.width(10.dp))
        
        // UP主名称行
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserUpBadge()
                Spacer(Modifier.width(4.dp))
                AppText(
                    text = info.owner.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // 关注按钮
        AppSurface(
            onClick = onFollowClick,
            color = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            shape = AppShapes.container(ContainerLevel.Card),
            modifier = Modifier.height(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                if (!isFollowing) {
                    AppIcon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                }
                AppText(
                    text = if (isFollowing) "已关注" else "关注",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }
        }
    }
}


//  2. 操作按钮行（官方B站样式：纯图标+数字，无圆形背景）
@Composable
fun ActionButtonsRow(
    info: ViewInfo,
    isFavorited: Boolean = false,
    isLiked: Boolean = false,
    coinCount: Int = 0,
    onFavoriteClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onTripleClick: () -> Unit = {},
    showTripleButton: Boolean = true  // [问题12] 控制三连按钮是否显示
) {
    val likeIcon = rememberAppLikeIcon()
    val likeFilledIcon = rememberAppLikeFilledIcon()
    val coinIcon = rememberAppCoinIcon()
    val favoriteIcon = rememberAppBookmarkIcon()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 2.dp),  //  紧凑布局
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //  点赞
        BiliActionButton(
            icon = if (isLiked) likeFilledIcon else likeIcon,
            text = FormatUtils.formatStat(info.stat.like.toLong()),
            isActive = isLiked,
            activeColor = MaterialTheme.colorScheme.primary,
            onClick = onLikeClick
        )

        // 🪙 投币
        BiliActionButton(
            icon = coinIcon,
            text = FormatUtils.formatStat(info.stat.coin.toLong()),
            isActive = coinCount > 0,
            activeColor = Color(0xFFFFB300),
            onClick = onCoinClick
        )

        //  收藏
        BiliActionButton(
            icon = favoriteIcon,
            text = FormatUtils.formatStat(info.stat.favorite.toLong()),
            isActive = isFavorited,
            activeColor = Color(0xFFFFC107),
            onClick = onFavoriteClick
        )

        //  [问题12] 仅在 showTripleButton 为 true 时显示三连按钮
        if (showTripleButton) {
            //  三连（👍图标）
            BiliActionButton(
                icon = likeFilledIcon,
                text = "三连",
                isActive = false,
                activeColor = Color(0xFFE91E63),
                onClick = onTripleClick
            )
        }
        
        //  [删除] 评论按钮已移除，因下方已有评论区入口
    }
}

//  官方B站样式操作按钮 - 纯图标+数字，无圆形背景
@Composable
private fun BiliActionButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    // 按压动画
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    
    // 激活状态脉冲动画
    var shouldPulse by remember { mutableStateOf(false) }
    val pulseScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shouldPulse) 1.2f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.4f,
            stiffness = 400f
        ),
        label = "pulseScale",
        finishedListener = { shouldPulse = false }
    )
    
    LaunchedEffect(isActive) {
        if (isActive) shouldPulse = true
    }
    
    val iconColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale * pulseScale
                scaleY = scale * pulseScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        AppText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1
        )
    }
}

//  优化版 ActionButton - 带按压动画和彩色图标
@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, //  新增颜色参数
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    
    //  按压动画状态
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "pressScale"
    )
    
    //  心跳脉冲动画 - 当 isActive 变为 true 时触发
    var shouldPulse by remember { mutableStateOf(false) }
    val pulseScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shouldPulse) 1.3f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.35f,  // 较低的阻尼创造弹性效果
            stiffness = 300f
        ),
        label = "pulseScale",
        finishedListener = { shouldPulse = false }  // 动画结束后重置
    )
    
    // 监听 isActive 变化
    LaunchedEffect(isActive) {
        if (isActive) {
            shouldPulse = true
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .width(56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        //  图标容器 - 使用彩色背景，深色模式下提高透明度
        Box(
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    //  脉冲缩放应用到图标容器
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(iconColor.copy(alpha = if (isDark) 0.15f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AppText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

//  3. 简介区域（优化样式）
@Composable
fun DescriptionSection(desc: String) {
    var expanded by remember { mutableStateOf(false) }

    if (desc.isBlank()) return

    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            AppText(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (desc.length > 100 || desc.lines().size > 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = if (expanded) "收起" else "展开更多",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    AppIcon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

//  4. 推荐视频列表头部
@Composable
fun RelatedVideosHeader() {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "更多推荐",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

//  5. 推荐视频单项（iOS 风格优化）
@Composable
fun RelatedVideoItem(video: RelatedVideo, onClick: () -> Unit) {
    //  iOS 风格按压动画
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(horizontal = 16.dp, vertical = 6.dp)  //  紧凑布局
        ) {
            // 视频封面
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(94.dp)
                    .clip(AppShapes.container(ContainerLevel.Card))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(video.pic))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                VideoCardCoverDurationText(
                    text = FormatUtils.formatDuration(video.duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
                
                //  播放量遮罩
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
                
                // 播放量标签
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    AppText(
                        text = FormatUtils.formatStat(video.stat.view.toLong()),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 视频信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(94.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题
                AppText(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // UP主信息行 + 播放量/弹幕  [优化] 新增统计信息
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserUpBadge()
                        Spacer(modifier = Modifier.width(4.dp))
                        AppText(
                            text = video.owner.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    //  [新增] 播放量 · 弹幕数
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        AppText(
                            text = FormatUtils.formatStat(video.stat.view.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AppText(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AppText(
                            text = "${FormatUtils.formatStat(video.stat.danmaku.toLong())}弹幕",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}



//  [新增] 视频分P选择器 (支持展开/收起)
@Composable
fun PagesSelector(
    pages: List<com.android.purebilibili.data.model.response.Page>,
    currentPageIndex: Int,
    onPageSelect: (Int) -> Unit
) {
    //  展开/收起状态
    var isExpanded by remember { mutableStateOf(false) }
    val selectedColors = resolveAdaptivePrimaryAccentColors(MaterialTheme.colorScheme)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 8.dp)
    ) {
        // 标题行 + 展开按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    text = "选集",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                AppText(
                    text = "(${pages.size}P)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            //  展开/收起按钮
            Row(
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = if (isExpanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
                AppIcon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isExpanded) {
            //  展开状态：垂直网格布局
            val columns = 3  // 每行3个
            val chunkedPages = pages.chunked(columns)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunkedPages.forEachIndexed { rowIndex, rowPages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPages.forEachIndexed { colIndex, page ->
                            val actualIndex = rowIndex * columns + colIndex
                            val isSelected = actualIndex == currentPageIndex
                            
                            AppSurface(
                                onClick = { onPageSelect(actualIndex) },
                                color = if (isSelected) selectedColors.backgroundColor else MaterialTheme.colorScheme.surfaceVariant,
                                shape = AppShapes.container(ContainerLevel.Chip),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    AppText(
                                        text = "P${page.page}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) selectedColors.contentColor else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AppText(
                                        text = page.part.ifEmpty { "第${page.page}P" },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) selectedColors.contentColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        // 填充空位
                        repeat(columns - rowPages.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            //  收起状态：横向滚动列表
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pages.size, key = { index -> pages[index].cid }) { index ->
                    val page = pages[index]
                    val isSelected = index == currentPageIndex
                    
                    AppSurface(
                        onClick = { onPageSelect(index) },
                        color = if (isSelected) selectedColors.backgroundColor else MaterialTheme.colorScheme.surfaceVariant,
                        shape = AppShapes.container(ContainerLevel.Chip),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            AppText(
                                text = "P${page.page}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) selectedColors.contentColor else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AppText(
                                text = page.part.ifEmpty { "第${page.page}P" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) selectedColors.contentColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
