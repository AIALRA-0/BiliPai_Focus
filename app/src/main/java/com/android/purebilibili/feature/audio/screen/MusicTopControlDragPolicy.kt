package com.android.purebilibili.feature.audio.screen

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Immutable
internal data class MusicTopControlTransform(
    val scaleX: Float,
    val scaleY: Float,
    val translationX: Float,
    val translationY: Float,
)

/** AndroidLiquidGlass LiquidButton interaction preset, adapted without moving layout coordinates. */
internal fun resolveMusicTopControlTransform(
    dragX: Float,
    dragY: Float,
    maxDragPx: Float,
    widthPx: Float,
    heightPx: Float,
    expansionPx: Float,
): MusicTopControlTransform {
    val safeMaxDragPx = maxDragPx.coerceAtLeast(1f)
    val clampedX = dragX.coerceIn(-safeMaxDragPx, safeMaxDragPx)
    val clampedY = dragY.coerceIn(-safeMaxDragPx, safeMaxDragPx)
    val safeWidth = widthPx.coerceAtLeast(1f)
    val safeHeight = heightPx.coerceAtLeast(1f)
    val maxOffset = minOf(safeWidth, safeHeight)
    val maxDimension = maxOf(safeWidth, safeHeight)
    val dragProgress = maxOf(abs(clampedX), abs(clampedY)) / safeMaxDragPx
    val scale = 1f + expansionPx / safeHeight * dragProgress
    val maxDragScale = expansionPx / safeHeight
    val offsetAngle = atan2(clampedY, clampedX)
    return MusicTopControlTransform(
        scaleX = scale +
            maxDragScale * abs(cos(offsetAngle) * clampedX / maxDimension) *
            (safeWidth / safeHeight).coerceAtMost(1f),
        scaleY = scale +
            maxDragScale * abs(sin(offsetAngle) * clampedY / maxDimension) *
            (safeHeight / safeWidth).coerceAtMost(1f),
        translationX = maxOffset * tanh(0.05f * clampedX / maxOffset),
        translationY = maxOffset * tanh(0.05f * clampedY / maxOffset),
    )
}
