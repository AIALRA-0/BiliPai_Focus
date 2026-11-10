package com.android.purebilibili.core.ui.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SkeletonSettingsStore

@Composable
fun rememberSkeletonBreathingEnabled(): Boolean {
    val context = LocalContext.current.applicationContext
    return remember(context) { SkeletonSettingsStore.breathingEnabled(context) }
        .collectAsStateWithLifecycle(initialValue = false).value
}

/** NaN 表示静态占位；只有开关开启且页面可见、允许动态效果时才创建动画。 */
@Composable
fun rememberSkeletonSweepState(
    animated: Boolean = true,
    initialPosition: Float = -0.5f,
    durationMillis: Int = 1000,
): State<Float> {
    val enabled = rememberSkeletonBreathingEnabled()
    val active = com.android.purebilibili.core.ui.rememberVisualWorkActive()
    val reduceMotion = com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion()
    if (!animated || !enabled || !active || reduceMotion) {
        return rememberUpdatedState(Float.NaN)
    }
    return rememberInfiniteTransition(label = "skeletonSweep").animateFloat(
        initialValue = initialPosition,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonSweepPosition",
    )
}

/**
 * 保留颜色 provider 的调用契约，同时携带绘制阶段才读取的扫光位置。
 * invoke 仅提供静态底色；骨架绘制器通过 skeletonBrush 获取渐变，普通固定色不启动动画。
 * MD3、Miuix 与玻璃路径共用绘制效果，各自的布局、材质背景和圆角保持原样。
 */
private class SkeletonFill(
    val base: Color,
    val highlight: Color,
    val position: () -> Float,
) : () -> Color {
    override fun invoke(): Color = base
}

internal fun skeletonFill(
    base: Color,
    highlight: Color,
    position: () -> Float,
): () -> Color = SkeletonFill(base, highlight, position)

internal fun (() -> Color).skeletonBrush(size: Size): Brush {
    val fill = this as? SkeletonFill ?: return SolidColor(invoke())
    val position = fill.position()
    if (!position.isFinite() || size.width <= 0f) return SolidColor(fill.base)
    val offset = size.width * position
    return Brush.linearGradient(
        0.1f to fill.base,
        0.3f to fill.highlight,
        0.5f to fill.highlight,
        0.7f to fill.base,
        start = Offset(offset, size.height * 0.35f),
        end = Offset(offset + size.width, size.height * 0.95f),
    )
}

internal fun Modifier.skeletonBackground(color: () -> Color): Modifier =
    drawBehind { drawRect(brush = color.skeletonBrush(size)) }
