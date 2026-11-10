package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.LocalRuntimeShaderCache
import top.yukonga.miuix.kmp.blur.RuntimeShaderCache
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight

/** 仅玻璃播放器使用轨道高光；不增加背景采样，也不改变原生轨道尺寸和触摸区域。 */
@Composable
internal fun rememberPlayerProgressGlassHighlight(): PlayerProgressGlassHighlight? {
    if (!LocalPlayerControlGlassEnabled.current) return null
    val cache = LocalRuntimeShaderCache.current
    val density = LocalDensity.current
    return remember(cache, density) {
        PlayerProgressGlassHighlight(cache, with(density) { 0.65.dp.toPx() })
    }
}

internal class PlayerProgressGlassHighlight(
    private val cache: RuntimeShaderCache,
    private val widthPx: Float,
) {
    private val shape = RoundedCornerShape(50)
    private val highlight = Highlight(
        width = 0.65.dp,
        alpha = 0.7f,
        style = BloomStroke.GlassStrokeSmallDark.copy(innerBlurRadius = 0.6.dp),
    )
    private val paint = Paint().apply { blendMode = highlight.style.blendMode }
    private val stroke = Stroke(widthPx)
    private var fallbackHeight = -1f
    private var fallbackBrush: Brush? = null

    fun draw(scope: DrawScope, trackHeightPx: Float) = with(scope) {
        val height = trackHeightPx.coerceAtMost(size.height)
        if (height <= 0f || size.width <= 0f) return@with
        val top = (size.height - height) / 2f
        inset(left = 0f, top = top, right = 0f, bottom = top) {
            // 每次绘制重新设置共享着色器的尺寸，避免其他玻璃控件改写 uniform 后串用。
            val shader = with(highlight.style) {
                createShader(shape, widthPx.coerceAtMost(height / 2f), highlight.alpha, cache)
            }
            if (shader != null) {
                paint.shader = shader
                drawIntoCanvas { it.drawRect(0f, 0f, size.width, size.height, paint) }
                paint.shader = null
            } else {
                if (fallbackHeight != height) {
                    fallbackHeight = height
                    fallbackBrush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.5f),
                        0.5f to Color.White.copy(alpha = 0.08f),
                        1f to Color.White.copy(alpha = 0.22f),
                        startY = 0f,
                        endY = height,
                    )
                }
                val halfStroke = widthPx / 2f
                if (size.width > widthPx && height > widthPx) {
                    drawRoundRect(
                        brush = requireNotNull(fallbackBrush),
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - widthPx, height - widthPx),
                        cornerRadius = CornerRadius((height - widthPx) / 2f),
                        style = stroke,
                    )
                }
            }
        }
    }
}
