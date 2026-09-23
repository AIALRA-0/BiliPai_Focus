package com.android.purebilibili.feature.home.components.cards

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppText

import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppSurface

import com.android.purebilibili.core.ui.MediaContrastPalette
/**
 * Shared Element Transition Imports
 */
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 *  iOS 风格直播间卡片
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LiveRoomCard(
    room: LiveRoom,
    index: Int,
    isDataSaverActive: Boolean = false,
    preferLowQualityCover: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    val cardShape = AppShapes.mediaCover()
    val tagShape = AppShapes.container(ContainerLevel.Tag)
    
    // Shared Element Transition Scopes
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    
    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverUrl = remember(room.roomid, useLowQualityCover) {
        FormatUtils.resolveVideoCoverUrl(
            room.cover.ifEmpty { room.keyframe.ifEmpty { room.userCover } },
            useLowQuality = useLowQualityCover
        )
    }
    val viewerCount = remember(room.online, room.watchedShow) { room.viewerCount() }
    val triggerCardClick = { onClick(room.roomid) }

    Column(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            //  iOS 点击动画
            .iOSTapEffect(
                scale = 0.97f,
                hapticEnabled = true
            ) {
                triggerCardClick()
            }
            .padding(bottom = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro)  //  减少间距
    ) {
        //  封面容器 - iOS 风格
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .shadow(
                    elevation = AppSpacingTokens.Micro,
                    shape = cardShape,
                    ambientColor = MediaContrastPalette.Scrim.copy(alpha = 0.08f),
                    spotColor = MediaContrastPalette.Scrim.copy(alpha = 0.12f),
                    clip = true // [Optimization] Combine shadow and clip
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = com.android.purebilibili.core.ui.transition.liveCoverSharedElementKey(room.roomid)),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
        ) {
            // 封面图 -  优化
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(150)
                    .memoryCacheKey("live_cover_${room.roomid}")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.DoubleExtraLarge)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MediaContrastPalette.Scrim.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // 🔴 直播标签 - 左上角
            AppSurface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacingTokens.Small),
                shape = tagShape,
                color = MaterialTheme.colorScheme.error
            ) {
                AppText(
                    text = "直播中",
                    color = MediaContrastPalette.Foreground,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro, vertical = AppSpacingTokens.Micro)
                )
            }
            
            // 分区标签 - 右上角
            if (room.areaName.isNotEmpty()) {
                AppSurface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppSpacingTokens.Small),
                    shape = tagShape,
                    color = MediaContrastPalette.Scrim.copy(alpha = 0.5f)
                ) {
                    AppText(
                        text = room.areaName,
                        color = MediaContrastPalette.Foreground,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro, vertical = AppSpacingTokens.Micro)
                    )
                }
            }
            
            // 观看人数 - 左下角
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "👁",
                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                )
                Spacer(modifier = Modifier.width(AppSpacingTokens.ExtraSmall))
                AppText(
                    text = FormatUtils.formatStat(viewerCount.toLong()),
                    color = MediaContrastPalette.Foreground.copy(0.95f),
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro))  //  减少间距
        
        // 标题
        AppText(
            text = room.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            ),
            modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall)
        )
        
        Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
        
        // 主播信息
        Row(
            modifier = Modifier.padding(horizontal = AppSpacingTokens.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主播头像
            if (room.face.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(room.face))
                        .crossfade(150)
                        .size(72, 72)
                        .memoryCacheKey("live_avatar_${room.uid}")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(AppSpacingTokens.Large + AppSpacingTokens.Micro)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro / 2))
            }
            
            AppText(
                text = room.uname,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
