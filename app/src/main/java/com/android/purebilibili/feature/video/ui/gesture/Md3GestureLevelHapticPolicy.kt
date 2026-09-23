package com.android.purebilibili.feature.video.ui.gesture

import com.android.purebilibili.feature.video.ui.components.shouldTriggerGesturePercentHaptic
import kotlin.math.abs

internal enum class GestureLevelHapticFeedback { Tick, Boundary }

/** Gesture-local feedback history, updated only from the host's effect. */
internal class Md3GestureLevelHapticPolicy {
    private var previousPercent: Int? = null
    private var lastFeedbackPercent: Int? = null
    private var lastFeedbackTimeMillis: Long? = null
    private var latchedBoundary: Int? = null

    fun update(percent: Int, active: Boolean, nowMillis: Long): GestureLevelHapticFeedback? {
        if (!active) {
            previousPercent = null
            lastFeedbackPercent = null
            lastFeedbackTimeMillis = null
            latchedBoundary = null
            return null
        }
        val current = percent.coerceIn(0, 100)
        val previous = previousPercent
        previousPercent = current
        if (previous == null) {
            latchedBoundary = current.takeIf { it == 0 || it == 100 }
            return null
        }
        // Small reversals at a limit must not repeatedly replay its confirmation.
        latchedBoundary?.let { if (abs(current - it) >= 3) latchedBoundary = null }
        if (current == previous) return null

        val feedback = if (current == 0 || current == 100) {
            if (latchedBoundary == current) return null
            latchedBoundary = current
            // A newly reached limit takes priority over the ordinary tick cooldown.
            GestureLevelHapticFeedback.Boundary
        } else {
            if (!shouldTriggerGesturePercentHaptic(previous, current, stepPercent = 5)) return null
            if (lastFeedbackPercent?.let { abs(current - it) < 5 } == true) return null
            if (lastFeedbackTimeMillis?.let { nowMillis - it < 70L } == true) return null
            GestureLevelHapticFeedback.Tick
        }
        lastFeedbackPercent = current
        lastFeedbackTimeMillis = nowMillis
        return feedback
    }
}
