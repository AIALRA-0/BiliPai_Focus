package com.android.purebilibili.core.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Mobile playback track geometry, independent of theme, input, or composable layout. */
fun DrawScope.drawMediaProgressTrack(
    progressFraction: Float,
    bufferedFraction: Float,
    trackHeightPx: Float,
    activeColor: Color,
    bufferedColor: Color,
    inactiveColor: Color,
) {
    if (trackHeightPx <= 0f || size.width <= 0f) return
    val top = ((size.height - trackHeightPx) / 2f).coerceAtLeast(0f)
    val radius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
    fun drawTrack(width: Float, color: Color) {
        if (width <= 0f) return
        drawRoundRect(color, Offset(0f, top),
            Size(width.coerceAtLeast(trackHeightPx), trackHeightPx), radius)
    }
    drawTrack(size.width, inactiveColor)
    drawTrack(size.width * bufferedFraction.coerceIn(0f, 1f), bufferedColor)
    drawTrack(size.width * progressFraction.coerceIn(0f, 1f), activeColor)
}
