package com.android.purebilibili.feature.settings

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate

/** 配色预览的城市与声波插画，随主题角色色变化，不依赖网络图片或材质开关。 */
internal fun DrawScope.drawThemePreviewCoverArt(
    rect: Rect,
    coverVariant: Int,
    highlight: Color,
    ridge: Color,
) {
    clipRect(rect.left, rect.top, rect.right, rect.bottom) {
        drawRect(
            brush = Brush.linearGradient(
                listOf(highlight.copy(alpha = 0.28f), ridge.copy(alpha = 0.14f)),
                rect.topLeft,
                rect.bottomRight,
            ),
            topLeft = rect.topLeft,
            size = rect.size,
        )
        val w = rect.width
        val h = rect.height
        translate(rect.left, rect.top) {
            if (coverVariant == 0) {
                drawCircle(ridge.copy(alpha = 0.6f), w * 0.22f, Offset(w * 0.8f, h * 0.24f))
                val heights = listOf(0.38f, 0.65f, 0.48f, 0.8f, 0.52f, 0.34f)
                heights.forEachIndexed { index, fraction ->
                    val height = h * fraction
                    val left = w * (0.06f + index * 0.16f)
                    drawRoundRect(
                        highlight.copy(alpha = 0.5f + index * 0.07f),
                        Offset(left, h - height),
                        Size(w * 0.12f, height),
                        CornerRadius(w * 0.025f),
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.4f),
                        Offset(left + w * 0.035f, h - height + h * 0.09f),
                        Offset(left + w * 0.035f, h - h * 0.08f),
                        w * 0.014f,
                    )
                }
            } else {
                drawCircle(highlight.copy(alpha = 0.12f), h * 0.7f, Offset(w * 0.5f, h * 0.5f))
                val heights = listOf(0.18f, 0.36f, 0.56f, 0.8f, 0.48f, 0.66f, 0.32f, 0.16f)
                heights.forEachIndexed { index, fraction ->
                    drawRoundRect(
                        if (index % 3 == 0) ridge else highlight,
                        Offset(w * (0.13f + index * 0.095f), h * (1f - fraction) / 2f),
                        Size(w * 0.045f, h * fraction),
                        CornerRadius(w * 0.024f),
                    )
                }
            }
        }
    }
}
