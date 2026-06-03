package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO

/**
 *  骨架屏组件 - iOS 风格加载占位
 */

private val LocalVideoSkeletonPulse = staticCompositionLocalOf { 0.5f }

// 基础 Shimmer 容器
@Composable
fun ShimmerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pulse = rememberVideoSkeletonPulse()
    CompositionLocalProvider(LocalVideoSkeletonPulse provides pulse) {
        Box(modifier = modifier) {
            content()
        }
    }
}

// 骨架方块
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    SkeletonBlock(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(cornerRadius)
    )
}

// 圆形骨架 (头像)
@Composable
fun SkeletonCircle(size: Dp = 48.dp) {
    SkeletonBlock(modifier = Modifier.size(size), shape = CircleShape)
}

/**
 *  视频详情页内容骨架屏（不包含播放器区域）
 */
@Composable
fun VideoDetailSkeleton() {
    ShimmerContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            VideoDetailTabBarSkeleton()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                VideoDetailUpInfoSkeleton()
                VideoDetailTitleInfoSkeleton()
                VideoDetailActionButtonsSkeleton()
            }
            VideoDetailRelatedHeaderSkeleton()
            repeat(3) {
                RelatedVideoItemSkeleton()
            }
        }
    }
}

@Composable
private fun VideoDetailTabBarSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(
            modifier = Modifier
                .weight(1f)
                .padding(start = 0.dp, top = 5.dp, end = 8.dp, bottom = 5.dp),
            height = 36.dp,
            cornerRadius = 18.dp
        )
        SkeletonBox(
            modifier = Modifier.width(54.dp),
            height = 28.dp,
            cornerRadius = 14.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        SkeletonBox(
            modifier = Modifier.width(64.dp),
            height = 28.dp,
            cornerRadius = 14.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        SkeletonBox(
            modifier = Modifier.size(28.dp),
            height = 28.dp,
            cornerRadius = 14.dp
        )
    }
}

@Composable
private fun VideoDetailUpInfoSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(modifier = Modifier.width(116.dp), height = 16.dp, cornerRadius = 8.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(modifier = Modifier.width(88.dp), height = 12.dp, cornerRadius = 6.dp)
        }
        SkeletonBox(modifier = Modifier.width(76.dp), height = 36.dp, cornerRadius = 18.dp)
    }
}

@Composable
private fun VideoDetailTitleInfoSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SkeletonBox(
                modifier = Modifier.weight(1f),
                height = 18.dp,
                cornerRadius = 9.dp
            )
            Spacer(Modifier.width(8.dp))
            SkeletonBox(
                modifier = Modifier.size(20.dp),
                height = 20.dp,
                cornerRadius = 10.dp
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(modifier = Modifier.width(56.dp), height = 11.dp, cornerRadius = 6.dp)
            Spacer(Modifier.width(10.dp))
            SkeletonBox(modifier = Modifier.width(54.dp), height = 11.dp, cornerRadius = 6.dp)
            Spacer(Modifier.width(10.dp))
            SkeletonBox(modifier = Modifier.width(72.dp), height = 11.dp, cornerRadius = 6.dp)
            Spacer(Modifier.width(10.dp))
            SkeletonBox(modifier = Modifier.width(84.dp), height = 11.dp, cornerRadius = 6.dp)
        }
        Spacer(Modifier.height(6.dp))
        SkeletonBox(modifier = Modifier.width(104.dp), height = 24.dp, cornerRadius = 10.dp)
    }
}

@Composable
private fun VideoDetailActionButtonsSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SkeletonBlock(modifier = Modifier.size(24.dp), shape = CircleShape)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(modifier = Modifier.width(42.dp), height = 11.dp, cornerRadius = 6.dp)
            }
        }
    }
}

@Composable
private fun VideoDetailRelatedHeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(modifier = Modifier.width(84.dp), height = 20.dp, cornerRadius = 10.dp)
    }
}

@Composable
private fun RelatedVideoItemSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(5.dp)
        ) {
            val relatedCoverWidth = 130.dp
            val relatedCoverHeight = relatedCoverWidth / VIDEO_SHARED_COVER_ASPECT_RATIO
            SkeletonBlock(
                modifier = Modifier
                    .width(relatedCoverWidth)
                    .height(relatedCoverHeight),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = relatedCoverHeight)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 16.dp, cornerRadius = 8.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.82f), height = 16.dp, cornerRadius = 8.dp)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBlock(modifier = Modifier.size(16.dp), shape = CircleShape)
                            Spacer(modifier = Modifier.width(6.dp))
                            SkeletonBox(modifier = Modifier.width(84.dp), height = 14.dp, cornerRadius = 7.dp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(modifier = Modifier.width(52.dp), height = 13.dp, cornerRadius = 7.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            SkeletonBox(modifier = Modifier.width(52.dp), height = 13.dp, cornerRadius = 7.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(rememberVideoSkeletonBlockColor())
    )
}

@Composable
private fun rememberVideoSkeletonPulse(): Float {
    val transition = rememberInfiniteTransition(label = "videoSkeletonPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = VIDEO_SKELETON_PULSE_DURATION_MILLIS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "videoSkeletonPulseAlpha"
    )
    return pulse
}

@Composable
private fun rememberVideoSkeletonBlockColor(): Color {
    val pulse = LocalVideoSkeletonPulse.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val alpha = if (isDarkTheme) {
        VIDEO_SKELETON_DARK_MIN_ALPHA +
            (VIDEO_SKELETON_DARK_MAX_ALPHA - VIDEO_SKELETON_DARK_MIN_ALPHA) * pulse
    } else {
        VIDEO_SKELETON_LIGHT_MIN_ALPHA +
            (VIDEO_SKELETON_LIGHT_MAX_ALPHA - VIDEO_SKELETON_LIGHT_MIN_ALPHA) * pulse
    }
    return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
}

private const val VIDEO_SKELETON_PULSE_DURATION_MILLIS = 2_000
private const val VIDEO_SKELETON_LIGHT_MIN_ALPHA = 0.06f
private const val VIDEO_SKELETON_LIGHT_MAX_ALPHA = 0.11f
private const val VIDEO_SKELETON_DARK_MIN_ALPHA = 0.10f
private const val VIDEO_SKELETON_DARK_MAX_ALPHA = 0.16f
