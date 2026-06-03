package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback

private const val PortraitTripleProgressDurationMillis = 900

internal fun shouldStartPortraitTriplePress(longPressConfirmed: Boolean): Boolean {
    return longPressConfirmed
}

internal fun shouldCancelPortraitTriplePressOnRelease(
    isTriplePressing: Boolean,
    tripleCompleted: Boolean
): Boolean {
    return isTriplePressing && !tripleCompleted
}

/**
 * 竖屏播放器右侧互动栏 (Refined Style)
 *
 * 移除了头像，仅保留操作按钮：点赞、评论、收藏、分享
 */
@Composable
fun PortraitInteractionBar(
    isLiked: Boolean,
    likeCount: Int,
    isFavorited: Boolean,
    favoriteCount: Int,
    commentCount: Int,
    shareCount: Int,
    onLikeClick: () -> Unit,
    onLikeLongClick: () -> Unit = {},
    onFavoriteClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolvePortraitInteractionBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val haptic = rememberHapticFeedback()
    var isTriplePressing by remember { mutableStateOf(false) }
    var tripleCompleted by remember { mutableStateOf(false) }
    var tripleProgress by remember { mutableFloatStateOf(0f) }
    val animatedTripleProgress by animateFloatAsState(
        targetValue = if (isTriplePressing) 1f else 0f,
        animationSpec = if (isTriplePressing) {
            tween(durationMillis = PortraitTripleProgressDurationMillis, easing = LinearEasing)
        } else {
            tween(durationMillis = 180, easing = FastOutSlowInEasing)
        },
        label = "portraitTripleProgress",
        finishedListener = { progress ->
            tripleProgress = progress
            if (progress >= 1f && isTriplePressing && !tripleCompleted) {
                tripleCompleted = true
                haptic(HapticType.MEDIUM)
                onLikeLongClick()
                isTriplePressing = false
            }
        }
    )

    LaunchedEffect(animatedTripleProgress) {
        tripleProgress = animatedTripleProgress
    }

    LaunchedEffect(isTriplePressing) {
        if (isTriplePressing) {
            haptic(HapticType.LIGHT)
        }
    }

    Column(
        modifier = modifier
            .padding(
                end = layoutPolicy.endPaddingDp.dp,
                bottom = layoutPolicy.bottomPaddingDp.dp
            ),
        verticalArrangement = Arrangement.spacedBy(layoutPolicy.itemSpacingDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // 点赞
        InteractionButton(
            icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
            countText = if (likeCount > 0) FormatUtils.formatStat(likeCount.toLong()) else "点赞",
            isActive = isLiked,
            activeColor = BiliPink,
            layoutPolicy = layoutPolicy,
            progress = tripleProgress,
            onClick = onLikeClick,
            onLongClick = {
                tripleCompleted = false
                isTriplePressing = shouldStartPortraitTriplePress(
                    longPressConfirmed = true
                )
            },
            onPressRelease = {
                if (
                    shouldCancelPortraitTriplePressOnRelease(
                        isTriplePressing = isTriplePressing,
                        tripleCompleted = tripleCompleted
                    )
                ) {
                    isTriplePressing = false
                }
            }
        )
        
        // 评论
        InteractionButton(
            icon = Icons.AutoMirrored.Rounded.Comment,
            countText = if (commentCount > 0) FormatUtils.formatStat(commentCount.toLong()) else "评论",
            isActive = false,
            layoutPolicy = layoutPolicy,
            onClick = onCommentClick
        )
        
        // 收藏
        InteractionButton(
            icon = if (isFavorited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            countText = if (favoriteCount > 0) FormatUtils.formatStat(favoriteCount.toLong()) else "收藏",
            isActive = isFavorited,
            activeColor = BiliPink,
            layoutPolicy = layoutPolicy,
            onClick = onFavoriteClick
        )
        
        // 分享
        InteractionButton(
            icon = Icons.Rounded.Share,
            countText = if (shareCount > 0) FormatUtils.formatStat(shareCount.toLong()) else "分享",
            isActive = false,
            layoutPolicy = layoutPolicy,
            onClick = onShareClick
        )
    }
}

/**
 * 互动按钮组件 (带数字)
 */
@Composable
private fun InteractionButton(
    icon: ImageVector,
    countText: String,
    isActive: Boolean,
    activeColor: Color = BiliPink,
    layoutPolicy: PortraitInteractionBarLayoutPolicy,
    progress: Float = 0f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPressRelease: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onLongClick != null) {
            Modifier.pointerInput(onClick, onLongClick, onPressRelease) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                    onPress = {
                        tryAwaitRelease()
                        onPressRelease?.invoke()
                    }
                )
            }
        } else {
            Modifier.clickable(
                indication = null,
                interactionSource = interactionSource
            ) { onClick() }
        }
    ) {
        Box(
            modifier = Modifier
                .size(layoutPolicy.iconBackingSizeDp.dp)
                .background(
                    color = Color.Black.copy(alpha = layoutPolicy.iconBackingAlpha),
                    shape = CircleShape
                )
                .padding(layoutPolicy.iconBackingInnerPaddingDp.dp),
            contentAlignment = Alignment.Center
        ) {
            if (progress > 0f) {
                Canvas(modifier = Modifier.size(layoutPolicy.iconBackingSizeDp.dp)) {
                    val stroke = 3.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f
                    )
                    drawArc(
                        color = activeColor.copy(alpha = 0.22f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color.White,
                modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
            )
        }
        Spacer(modifier = Modifier.height(layoutPolicy.labelTopSpacingDp.dp))
        Text(
            text = countText,
            color = Color.White,
            fontSize = layoutPolicy.labelFontSp.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 8f
                )
            )
        )
    }
}
