package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun MusicWavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    wavy: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val amplitudePx by animateFloatAsState(
        targetValue = if (wavy) with(density) { MUSIC_WAVY_AMPLITUDE_DP.dp.toPx() } else 0f,
        label = "music-wavy-amplitude"
    )
    val infinite = rememberInfiniteTransition(label = "music-wavy-phase")
    val animatedPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "music-wavy-phase-value"
    )
    val phase = if (wavy) animatedPhase else 0f
    val wavelengthPx = with(density) { MUSIC_WAVY_WAVELENGTH_DP.dp.toPx() }
    val strokePx = with(density) { MUSIC_WAVY_STROKE_DP.dp.toPx() }
    val thumbRadiusPx = with(density) { MUSIC_WAVY_THUMB_DP.dp.toPx() / 2f }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics {
                setProgress {
                    onValueChange(
                        resolveMusicProgressValue(it, valueRange.start, valueRange.endInclusive)
                    )
                    onValueChangeFinished()
                    true
                }
                if (valueRange.endInclusive <= valueRange.start) disabled()
            }
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val fraction = if (size.width <= 0) 0f else (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(resolveMusicProgressValue(fraction, valueRange.start, valueRange.endInclusive))
                    onValueChangeFinished()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragEnd = onValueChangeFinished,
                    onDragCancel = onValueChangeFinished
                ) { change, _ ->
                    val fraction = if (size.width <= 0) 0f else (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(resolveMusicProgressValue(fraction, valueRange.start, valueRange.endInclusive))
                }
            }
    ) {
        val fraction = resolveMusicProgressFraction(value, valueRange.start, valueRange.endInclusive)
        val progressX = size.width * fraction
        val centerY = size.height / 2f
        val activePath = Path()
        val inactivePath = Path()
        val step = 2f
        var x = 0f
        var started = false
        while (x <= progressX) {
            val y = centerY + sin((x / wavelengthPx) * 2f * PI.toFloat() + phase) * amplitudePx
            if (!started) {
                activePath.moveTo(x, y)
                started = true
            } else {
                activePath.lineTo(x, y)
            }
            x += step
        }
        if (started) {
            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        inactivePath.moveTo(progressX, centerY)
        inactivePath.lineTo(size.width, centerY)
        drawPath(
            path = inactivePath,
            color = inactiveColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
        drawCircle(
            color = thumbColor,
            radius = thumbRadiusPx,
            center = Offset(progressX, centerY)
        )
    }
}
