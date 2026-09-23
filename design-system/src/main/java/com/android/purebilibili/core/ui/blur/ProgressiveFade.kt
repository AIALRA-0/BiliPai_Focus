package com.android.purebilibili.core.ui.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 移植自 Telegram/Nagram 的核心非线性渐变停靠点算法 (BlurredBackgroundWithFadeDrawable)。
 *
 * 普通线性渐变（LinearGradient）由于感知亮度变化不均匀，会产生明显的马赫带效应（Mach-Band Effect），
 * 视觉上有生硬的切面感。Telegram 采用 5 阶非线性缓动停靠点（0 -> 0x60 -> 0xB0 -> 0xE8 -> 0xFF），
 * 使透明度在靠近顶部时迅速收敛为纯色，在离开顶部时极其平滑柔和地消散。
 */
object ProgressiveFadeDefaults {
    const val STOP_TOP = 0.00f
    const val STOP_QUARTER = 0.25f
    const val STOP_HALF = 0.50f
    const val STOP_THREE_QUARTERS = 0.75f
    const val STOP_BOTTOM = 1.00f

    // Telegram 原生 Alpha 阶梯 (255 基准):
    // 0xFF (255 / 255 = 1.0f)
    // 0xE8 (232 / 255 ≈ 0.9098f)
    // 0xB0 (176 / 255 ≈ 0.6902f)
    // 0x60 (96 / 255 ≈ 0.3765f)
    // 0x00 (0 / 255 = 0.0f)
    val ALPHA_FACTORS = floatArrayOf(1.0f, 232f / 255f, 176f / 255f, 96f / 255f, 0.0f)

    /**
     * 生成非线性缓动颜色停靠点列表，依据 [baseColor] 的固有 Alpha 动态缩放。
     */
    fun createStops(baseColor: Color): List<Pair<Float, Color>> {
        val baseAlpha = baseColor.alpha
        return listOf(
            STOP_TOP to baseColor.copy(alpha = baseAlpha * ALPHA_FACTORS[0]),
            STOP_QUARTER to baseColor.copy(alpha = baseAlpha * ALPHA_FACTORS[1]),
            STOP_HALF to baseColor.copy(alpha = baseAlpha * ALPHA_FACTORS[2]),
            STOP_THREE_QUARTERS to baseColor.copy(alpha = baseAlpha * ALPHA_FACTORS[3]),
            STOP_BOTTOM to baseColor.copy(alpha = baseAlpha * ALPHA_FACTORS[4]),
        )
    }

    /**
     * 创建适用于顶部竖向渐隐的垂直渐变 Brush。
     */
    fun createVerticalBrush(
        baseColor: Color,
        startY: Float = 0f,
        endY: Float
    ): Brush {
        return Brush.verticalGradient(
            colorStops = createStops(baseColor).toTypedArray(),
            startY = startY,
            endY = endY
        )
    }
}

/**
 * 顶部纯色渐进消隐 Modifier。
 *
 * 在容器绘制完自身子内容（如滚动的列表 Item）后，在顶部 [fadeHeight] 区域覆盖一层 Telegram 同款的
 * 非线性渐进消隐遮罩，使向上滚动的内容自然平滑地“融化”进背景底色中。
 *
 * @param surfaceColor 渐变消融的目标底色（通常为背景底色）
 * @param fadeHeight 渐隐消融的竖向过渡高度
 * @param enabled 是否启用渐隐
 */
fun Modifier.topSolidProgressiveFade(
    surfaceColor: Color,
    fadeHeight: Dp = 80.dp,
    enabled: Boolean = true,
): Modifier {
    if (!enabled || surfaceColor.alpha <= 0.001f || fadeHeight <= 0.dp) return this
    return this.drawWithContent {
        drawContent()
        val fadePx = fadeHeight.toPx().coerceAtMost(size.height)
        if (fadePx > 0f) {
            val brush = ProgressiveFadeDefaults.createVerticalBrush(
                baseColor = surfaceColor,
                startY = 0f,
                endY = fadePx
            )
            drawRect(
                brush = brush,
                size = Size(width = size.width, height = fadePx)
            )
        }
    }
}

/**
 * 顶部纯色渐进消隐独立遮罩层 Composable。
 */
@Composable
fun TopSolidProgressiveFadeOverlay(
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    fadeHeight: Dp = 80.dp,
    enabled: Boolean = true,
) {
    if (!enabled || surfaceColor.alpha <= 0.001f || fadeHeight <= 0.dp) return
    val brush = remember(surfaceColor) {
        Brush.verticalGradient(
            colorStops = ProgressiveFadeDefaults.createStops(surfaceColor).toTypedArray()
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(fadeHeight)
            .background(brush)
    )
}
