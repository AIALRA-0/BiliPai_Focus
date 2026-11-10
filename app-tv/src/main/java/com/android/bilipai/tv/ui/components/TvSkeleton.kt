package com.android.bilipai.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.android.bilipai.tv.ui.LocalTvReduceMotion
import com.android.bilipai.tv.ui.LocalTvSimpleEffects
import com.android.bilipai.tv.ui.TvUiTokens
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel

/**
 * 视频卡片形骨架占位：封面块 + 两行标题条，脉动呼吸提示加载中。
 * 减少动画或精简效果时静态呈现，不播放脉动。纯占位，不参与焦点。
 */
@Composable
internal fun TvSkeletonCard(modifier: Modifier = Modifier) {
    val reduce = LocalTvReduceMotion.current || LocalTvSimpleEffects.current
    val alpha = if (reduce) {
        0.55f
    } else {
        rememberInfiniteTransition(label = "tv-skeleton").animateFloat(
            initialValue = 0.35f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
            label = "tv-skeleton-alpha",
        ).value
    }
    val shape = TvUiTokens.shape(ContainerLevel.Card)
    val blockColor = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).graphicsLayer { this.alpha = alpha }
            .background(blockColor, shape))
        Box(Modifier.fillMaxWidth(0.92f).height(14.dp).graphicsLayer { this.alpha = alpha }
            .background(blockColor, shape))
        Box(Modifier.fillMaxWidth(0.55f).height(12.dp).graphicsLayer { this.alpha = alpha }
            .background(blockColor, shape))
    }
}

/**
 * 首屏加载骨架网格：列数算法与 [com.android.bilipai.tv.ui.TvVideoGrid] 一致，铺满可用空间的两行占位。
 */
@Composable
internal fun TvSkeletonGrid(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val availableWidth = (maxWidth - TvUiTokens.gridPadding * 2).value
        val columns = ((availableWidth + TvUiTokens.cardGap.value) /
            (TvUiTokens.minimumCardWidth.value * LocalDensity.current.fontScale.coerceAtLeast(1f) + TvUiTokens.cardGap.value))
            .toInt().coerceIn(1, 6)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = TvUiTokens.gridPadding),
            verticalArrangement = Arrangement.spacedBy(TvUiTokens.cardGap),
        ) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(TvUiTokens.cardGap)) {
                    repeat(columns) {
                        TvSkeletonCard(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
