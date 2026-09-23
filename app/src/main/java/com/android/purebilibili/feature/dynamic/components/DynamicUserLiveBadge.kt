package com.android.purebilibili.feature.dynamic.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.feature.dynamic.resolveDynamicUserLiveBadgeLabel
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun DynamicUserLiveBadge(
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DynamicUserLiveBadgeVisualSpec.HorizontalSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DynamicLivePulseIndicator(color = color)
        AppText(
            text = resolveDynamicUserLiveBadgeLabel(),
            color = color,
            fontSize = MaterialTheme.typography.labelSmall.fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun DynamicLivePulseIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberSystemReduceMotion()
    val progress = if (!reduceMotion) {
        val transition = rememberInfiniteTransition(label = "dynamicLivePulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = AppMotionTokens.livePulseSpec<Float>(),
            ),
            label = "dynamicLivePulseProgress",
        )
    } else {
        null
    }

    Canvas(
        modifier = modifier.size(
            width = DynamicUserLiveBadgeVisualSpec.IndicatorWidth,
            height = DynamicUserLiveBadgeVisualSpec.IndicatorHeight,
        )
    ) {
        drawDynamicLivePulseBars(
            progress = progress?.value,
            color = color,
        )
    }
}

private fun DrawScope.drawDynamicLivePulseBars(
    progress: Float?,
    color: Color,
) {
    val barWidth = size.width * 0.18f
    val gap = size.width * 0.14f
    val minHeight = size.height * 0.3f
    val availableHeight = size.height - minHeight
    val staticFractions = floatArrayOf(0.4f, 0.72f, 0.5f)

    repeat(3) { index ->
        val heightFraction = progress?.let { value ->
            val phase = value * 2f * PI.toFloat() + index * 2.1f
            0.5f + 0.5f * sin(phase)
        } ?: staticFractions[index]
        val barHeight = minHeight + availableHeight * heightFraction
        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = size.width * 0.09f + index * (barWidth + gap),
                y = size.height - barHeight,
            ),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}
