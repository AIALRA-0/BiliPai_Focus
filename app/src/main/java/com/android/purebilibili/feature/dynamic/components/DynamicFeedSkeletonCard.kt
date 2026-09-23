package com.android.purebilibili.feature.dynamic.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel

// 首屏骨架卡片数量（瀑布流两列时为 6 张）
internal const val DYNAMIC_FEED_SKELETON_ITEM_COUNT = 6

private const val DYNAMIC_SKELETON_PULSE_DURATION_MILLIS = 900

/**
 * 动态 feed 首屏骨架卡（shimmer 脉冲），对齐 BiliPai 的 DynamicCardSkeleton：
 * 头像 + 双行文字 + 正文条 + 封面块 + 底部操作占位。
 */
@Composable
internal fun rememberDynamicFeedSkeletonPulse(): Float {
    if (com.android.purebilibili.core.ui.skeleton.rememberSkeletonBreathingEnabled()) {
        return com.android.purebilibili.core.ui.skeleton.rememberGentleSkeletonPulse().value
    }
    val transition = rememberInfiniteTransition(label = "dynamicFeedSkeletonPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = DYNAMIC_SKELETON_PULSE_DURATION_MILLIS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dynamicFeedSkeletonPulseAlpha"
    )
    return pulse
}

@Composable
internal fun DynamicFeedSkeletonCard(
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val cardShape = AppShapes.container(ContainerLevel.Card)
    val textPlaceholderShape = AppShapes.container(ContainerLevel.Tag)
    val actionPlaceholderShape = AppShapes.container(ContainerLevel.Tag)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val blockColor = remember(pulse, surfaceVariant, onSurfaceVariant) {
        lerp(
            surfaceVariant,
            onSurfaceVariant.copy(alpha = 0.22f),
            pulse
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AppSpacingTokens.Medium)
    ) {
        // 作者行：头像 + 名称/时间
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(AppSpacingTokens.TripleExtraLarge)
                    .clip(CircleShape)
                    .background(blockColor)
            )
            Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
            Column {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(AppSpacingTokens.Small + AppSpacingTokens.Micro)
                        .clip(textPlaceholderShape)
                        .background(blockColor)
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(AppSpacingTokens.ExtraSmall)
                        .clip(textPlaceholderShape)
                        .background(blockColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        // 正文占位条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacingTokens.Small + AppSpacingTokens.Micro)
                .clip(textPlaceholderShape)
                .background(blockColor)
        )
        Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(AppSpacingTokens.Small + AppSpacingTokens.Micro)
                .clip(textPlaceholderShape)
                .background(blockColor)
        )

        Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        // 封面块（16:10，与视频卡一致）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(cardShape)
                .background(blockColor)
        )

        Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        // 底部操作占位
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppSpacingTokens.DoubleExtraLarge + AppSpacingTokens.Micro)
                        .clip(actionPlaceholderShape)
                        .background(blockColor)
                )
            }
        }
    }
}
