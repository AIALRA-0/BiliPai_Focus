package com.android.purebilibili.feature.home.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.ProgressiveBlur

/** 非玻璃渐进顶栏使用轻量连续遮罩，分类区域柔和加强，尾部平滑归零。 */
internal fun homeTopReadabilityStops(
    contentBottom: Float,
    tabTop: Float,
    tabBottom: Float,
    fadeBottom: Float,
    transition: Float,
    baseAlpha: Float,
    tabVisibility: Float,
): List<Pair<Float, Float>> {
    val end = fadeBottom.coerceAtLeast(1f)
    val contentEnd = contentBottom.coerceIn(0f, end)
    val top = tabTop.coerceIn(0f, contentEnd)
    val bottom = tabBottom.coerceIn(top, contentEnd)
    val edge = transition.coerceAtLeast(1f)
    val base = baseAlpha.coerceIn(0f, 1f)
    val peak = maxOf(base, 0.32f * tabVisibility.coerceIn(0f, 1f))
    val hasTabs = bottom > top && tabVisibility > 0f
    val tabCenter = (top + bottom) / 2f
    val tabFadeStart = (top - edge).coerceAtLeast(0f)
    val tabFadeEnd = if (bottom >= contentEnd - 1f) end else (bottom + edge).coerceAtMost(end)
    fun smoothWeight(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    // 增加曲线采样，避免线性渐变在分类上下沿形成明显折线和整条深色平台。
    val positions = (listOf(
        0f, tabFadeStart, top, tabCenter, bottom,
        (bottom + edge).coerceAtMost(end), contentEnd,
        contentEnd + (end - contentEnd) * 0.5f, end,
    ) + (0..32).map { end * it / 32f }).distinct().sorted()
    return positions.map { y ->
        val baseWeight = if (y <= contentEnd) 1f else {
            smoothWeight((end - y) / (end - contentEnd).coerceAtLeast(1f))
        }
        val tabWeight = when {
            !hasTabs -> 0f
            y <= tabCenter -> smoothWeight((y - tabFadeStart) / (tabCenter - tabFadeStart).coerceAtLeast(1f))
            else -> smoothWeight((tabFadeEnd - y) / (tabFadeEnd - tabCenter).coerceAtLeast(1f))
        }
        y / end to (base + (peak - base) * tabWeight) * baseWeight
    }
}

internal fun Modifier.homeTopReadabilityProtection(
    color: Color,
    contentBottom: Dp,
    tabTop: Dp,
    tabBottom: Dp,
    baseAlpha: Float,
    tabVisibility: Float,
): Modifier = drawWithCache {
    val stops = homeTopReadabilityStops(
        contentBottom = contentBottom.toPx(),
        tabTop = tabTop.toPx(),
        tabBottom = tabBottom.toPx(),
        fadeBottom = size.height,
        transition = 16.dp.toPx(),
        baseAlpha = baseAlpha,
        tabVisibility = tabVisibility,
    )
    val brush = Brush.verticalGradient(
        colorStops = stops.map { (position, alpha) -> position to color.copy(alpha = alpha) }.toTypedArray(),
        endY = size.height,
    )
    onDrawBehind { drawRect(brush) }
}

/** 仅供首页非玻璃连续顶栏使用，连续衰减并在控件底部之前归零。 */
internal fun homeTopReadabilityBlurGradient(
    gradient: ProgressiveBlur,
    contentBottomFraction: Float,
): ProgressiveBlur {
    // 分类文字由独立保护层保证对比度，不能把满强度模糊平台推到分类栏底部。
    // 保留较短的用户覆盖范围；绘制层额外的渐隐高度不应把模糊带进首排封面。
    val end = minOf(gradient.endFraction, contentBottomFraction).coerceIn(0.01f, 1f)
    val start = gradient.startFraction.coerceIn(0f, end - 0.01f)
    return gradient.copy(startFraction = start, endFraction = end)
}
