package com.android.purebilibili.feature.video.ui.components

import kotlin.math.abs
import kotlin.math.roundToInt

internal data class CircularGesturePercentMotionSpec(
    val durationMillis: Int,
    val blurRadiusDp: Float
)

internal fun resolveCircularGesturePercentMotion(
    previousPercent: Int,
    currentPercent: Int,
    elapsedMillis: Long
): CircularGesturePercentMotionSpec {
    val delta = abs(currentPercent.coerceIn(0, 100) - previousPercent.coerceIn(0, 100))
    if (delta == 0) return CircularGesturePercentMotionSpec(0, 0f)
    val speed = delta * 1000f / elapsedMillis.coerceAtLeast(1L)
    val fastFraction = ((speed - 20f) / 180f).coerceIn(0f, 1f)
    return CircularGesturePercentMotionSpec(
        durationMillis = (150f - 70f * fastFraction).roundToInt(),
        blurRadiusDp = 0.5f + fastFraction
    )
}

/** Input samples, rather than animated display values, determine gesture speed. */
internal class CircularGesturePercentMotionTracker {
    private var previousPercent: Int? = null
    private var previousTimeMillis = 0L

    fun update(percent: Int, nowMillis: Long): CircularGesturePercentMotionSpec? {
        val previous = previousPercent
        val elapsed = nowMillis - previousTimeMillis
        previousPercent = percent
        previousTimeMillis = nowMillis
        return previous?.let { resolveCircularGesturePercentMotion(it, percent, elapsed) }
    }
}
