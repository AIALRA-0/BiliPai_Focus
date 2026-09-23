package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.feature.video.danmaku.AdvancedDanmakuData
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * 高级弹幕渲染层 (Compose 实现)
 * 
 * 负责渲染 Mode 7 (高级定位弹幕)。
 * 使用播放器统一视口映射作者坐标，并独立组合用户字号和视口缩放。
 * 
 * @param danmakuList 所有高级弹幕数据
 */
@Composable
fun AdvancedDanmakuOverlay(
    danmakuList: List<AdvancedDanmakuData>,
    player: androidx.media3.common.Player,
    viewport: com.android.purebilibili.feature.video.danmaku.DanmakuViewport,
    opacity: Float = 1f,
    fontScale: Float = 1f,
    fontWeight: Int = 5,
    modifier: Modifier = Modifier
) {
    // 使用 produceState 每一帧更新播放进度
    // 并处理暂停/播放状态
    val currentPosition by androidx.compose.runtime.produceState(
        initialValue = player.currentPosition,
        key1 = player,
        key2 = danmakuList
    ) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                value = player.currentPosition
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                value = player.currentPosition
            }
            override fun onPositionDiscontinuity(oldPosition: androidx.media3.common.Player.PositionInfo, newPosition: androidx.media3.common.Player.PositionInfo, reason: Int) {
                value = newPosition.positionMs
            }
        }
        player.addListener(listener)
        
        try {
            while (isActive) {
                val isPlaying = player.isPlaying
                val pos = player.currentPosition
                if (isPlaying) {
                    value = pos
                }
                if (!isPlaying) {
                    kotlinx.coroutines.delay(500)
                } else {
                    val hasActiveDanmaku = danmakuList.any { danmaku ->
                        pos >= danmaku.startTimeMs - 500 && pos <= danmaku.startTimeMs + danmaku.durationMs + 200
                    }
                    if (hasActiveDanmaku) {
                        // 约 60fps 更新保证动画平滑
                        kotlinx.coroutines.delay(16)
                    } else {
                        // 无活跃高级弹幕时降频到 250ms，大幅减少空转 CPU 消耗与无效重组
                        kotlinx.coroutines.delay(250)
                    }
                }
            }
        } finally {
            player.removeListener(listener)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        
        // 筛选当前时间应该显示的弹幕
        // 为了性能，只处理当前时间窗口内的弹幕
        val activeDanmakus by remember(danmakuList, currentPosition) {
            derivedStateOf {
                // 预留一些 buffer，避免刚好在边界闪烁
                danmakuList.filter { 
                    currentPosition >= it.startTimeMs - 100 && currentPosition <= it.startTimeMs + it.durationMs + 100
                }
            }
        }
        
        activeDanmakus.forEach { danmaku ->
            key(danmaku.id) {
                RenderSingleAdvancedDanmaku(
                    danmaku = danmaku,
                    currentPosition = currentPosition,
                    viewport = viewport,
                    opacity = opacity,
                    fontScale = fontScale,
                    fontWeight = fontWeight
                )
            }
        }
    }
}

@Composable
private fun RenderSingleAdvancedDanmaku(
    danmaku: AdvancedDanmakuData,
    currentPosition: Long,
    viewport: com.android.purebilibili.feature.video.danmaku.DanmakuViewport,
    opacity: Float,
    fontScale: Float,
    fontWeight: Int
) {
    // 如果是高能弹幕 (maxCount > 1)，需要动态计算显示的文字
    val displayText = if (danmaku.maxCount > 1) {
        // 计算积累阶段的进度
        val elapsed = currentPosition - danmaku.startTimeMs
        if (elapsed < danmaku.accumulationDurationMs) {
            // 增长阶段：根据时间比例计算当前数字
            // 至少显示 1
            val currentCount = (1 + (danmaku.maxCount - 1) * (elapsed.toFloat() / danmaku.accumulationDurationMs.toFloat())).toInt()
            "${danmaku.content} ×$currentCount"
        } else {
            // 积累完成，显示最大值
            "${danmaku.content} ×${danmaku.maxCount}"
        }
    } else {
        danmaku.content
    }

    // [完整 BAS] 位移动画进度（考虑 delay 与 translationDurationMs），并应用缓动曲线
    val rawTranslationProgress = danmaku.getTranslationProgress(currentPosition)
    val easedProgress = danmaku.easing.transform(rawTranslationProgress)

    // [完整 BAS] 位置：优先使用路径动画，否则起点->终点线性插值
    val position = if (danmaku.path.isNotEmpty()) {
        danmaku.getPathPointAt(easedProgress)
    } else {
        val currentX = danmaku.startX + (danmaku.endX - danmaku.startX) * easedProgress
        val currentY = danmaku.startY + (danmaku.endY - danmaku.startY) * easedProgress
        com.android.purebilibili.feature.video.danmaku.BasPathPoint(currentX, currentY)
    }

    // Normalized author coordinates are mapped once; the anchor is the text's top-left.
    val xPx = (position.x * viewport.widthPx).roundToInt()
    val yPx = (position.y * viewport.heightPx).roundToInt()

    // 颜色转换
    val color = Color(danmaku.color or 0xFF000000.toInt())

    // [完整 BAS] 透明度动画（alphaStart -> alphaEnd 按总时长插值）
    val currentAlpha = danmaku.getAlphaAt(currentPosition)

    // [视觉优化] 高能弹幕使用更强烈的动画效果
    val isAccumulating = currentPosition < danmaku.startTimeMs + danmaku.accumulationDurationMs

    // 心跳动画
    // 使用 infiniteTransition 或者简单的根据时间取余计算 scale
    val scale = if (danmaku.maxCount > 1 && isAccumulating) {
        // 简单的模拟心跳: 每 300ms 跳动一次
        val pulsePhase = (currentPosition % 300) / 300f
        // 1.0 -> 1.3 -> 1.0
        if (pulsePhase < 0.5f) {
            1.0f + 0.3f * (pulsePhase * 2)
        } else {
            1.3f - 0.3f * ((pulsePhase - 0.5f) * 2)
        }
    } else 1.0f

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx, yPx) }
            .alpha(currentAlpha * opacity.coerceIn(0f, 1f))
            .rotate(danmaku.rotateZ)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // 主文字
        AppText(
            text = displayText,
            color = color,
            fontSize = (danmaku.fontSize * fontScale.coerceIn(0.3f, 2f) * viewport.scale).sp,
            fontWeight = FontWeight(fontWeight.coerceIn(1, 9) * 100)
        )
    }
}
