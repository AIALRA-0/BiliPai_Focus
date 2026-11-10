package com.android.purebilibili.feature.nearby

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.transition.BlurRenderEffectCache
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val NEARBY_FLIGHT_DURATION_MS = 320

/** 同一进度控制飞行、模糊和边缘起伏；首尾无抖动，不创建常驻时钟。 */
internal fun nearbyFlightEnvelope(progress: Float): Float =
    sin(PI.toFloat() * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)

@Composable
internal fun Modifier.nearbyFlightBlur(distanceProvider: () -> Float, enabled: Boolean): Modifier {
    val density = LocalDensity.current
    // 只创建有限档位，图层更新不反复分配模糊效果，不在组合阶段读取逐帧进度。
    val effects = remember(density, enabled) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) emptyList() else List<RenderEffect?>(13) { step ->
            BlurRenderEffectCache().resolve(with(density) { step.dp.toPx() })
        }
    }
    return if (effects.isEmpty()) this else graphicsLayer {
        val step = (distanceProvider().coerceIn(0f, 1f) * 12f).roundToInt()
        renderEffect = effects[step]
    }
}

/** 在卡片外侧绘制小幅移动的柔光轮廓，内容本身不抖动。 */
internal fun Modifier.nearbyLivingEdge(
    progressProvider: () -> Float,
    accent: Color,
    highlight: Color,
    glassEnabled: Boolean,
): Modifier = drawWithCache {
    val inset = 12.dp.toPx()
    val width = (size.width - inset * 2f).coerceAtLeast(1f)
    val height = (size.height - inset * 2f).coerceAtLeast(1f)
    val radius = minOf(20.dp.toPx(), width / 2f, height / 2f)
    val horizontal = (width - radius * 2f).coerceAtLeast(0f)
    val vertical = (height - radius * 2f).coerceAtLeast(0f)
    val quarter = PI.toFloat() * radius / 2f
    val lengths = floatArrayOf(horizontal, quarter, vertical, quarter, horizontal, quarter, vertical, quarter)
    val perimeter = lengths.sum()
    val path = Path()
    val amplitude = 1.8.dp.toPx()
    val fineStroke = Stroke(width = 1.dp.toPx())
    val softStroke = Stroke(width = 5.dp.toPx())
    val outerStroke = Stroke(width = 10.dp.toPx())
    // 玻璃仅增强反光的透明度；关闭玻璃后的主题色、轮廓几何与运动独立保留。
    val reflectionAlpha = if (glassEnabled) 0.66f else 0.34f
    onDrawBehind {
        val progress = progressProvider().coerceIn(0f, 1f)
        val envelope = nearbyFlightEnvelope(progress)
        if (envelope > 0.001f) {
            path.reset()
            for (index in 0..128) {
                val fraction = index / 128f
                var remaining = fraction * perimeter
                var segment = 0
                while (segment < 7 && remaining > lengths[segment]) {
                    remaining -= lengths[segment]
                    segment++
                }
                val t = if (lengths[segment] > 0f) (remaining / lengths[segment]).coerceIn(0f, 1f) else 0f
                val angle = when (segment) {
                    1 -> -PI.toFloat() / 2f + t * PI.toFloat() / 2f
                    3 -> t * PI.toFloat() / 2f
                    5 -> PI.toFloat() / 2f + t * PI.toFloat() / 2f
                    7 -> PI.toFloat() + t * PI.toFloat() / 2f
                    else -> 0f
                }
                val normalX = when (segment) { 0, 4 -> 0f; 2 -> 1f; 6 -> -1f; else -> cos(angle) }
                val normalY = when (segment) { 0 -> -1f; 2, 6 -> 0f; 4 -> 1f; else -> sin(angle) }
                val x = when (segment) {
                    0 -> radius + horizontal * t
                    2 -> width
                    4 -> width - radius - horizontal * t
                    6 -> 0f
                    1, 3 -> width - radius + radius * cos(angle)
                    else -> radius + radius * cos(angle)
                }
                val y = when (segment) {
                    0 -> 0f
                    2 -> radius + vertical * t
                    4 -> height
                    6 -> height - radius - vertical * t
                    1, 7 -> radius + radius * sin(angle)
                    else -> height - radius + radius * sin(angle)
                }
                val wave = (sin(fraction * PI.toFloat() * 12f - progress * PI.toFloat() * 4f) * 0.7f +
                    sin(fraction * PI.toFloat() * 20f + progress * PI.toFloat() * 2f) * 0.3f) * amplitude * envelope
                val px = inset + x + normalX * wave
                val py = inset + y + normalY * wave
                if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, accent.copy(alpha = 0.07f * envelope), style = outerStroke)
            drawPath(path, accent.copy(alpha = 0.16f * envelope), style = softStroke)
            drawPath(path, highlight.copy(alpha = reflectionAlpha * envelope), style = fineStroke)
        }
    }
}
