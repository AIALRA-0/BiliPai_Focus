package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Retargets the official spring from its current value and velocity; idle motion requests no frames. */
class LiquidIndicatorVelocity(private val scope: CoroutineScope) {
    private val spec = LiquidIndicatorMotion.velocitySpec()
    private var animation by mutableStateOf(AnimationState(0f))
    private var worker: Job? = null
    private var target = 0f
    val value: Float get() = animation.value

    fun updateTarget(next: Float) {
        if (!next.isFinite() || next == target) return
        target = next
        val continuing = worker?.isActive == true
        worker?.cancel()
        val current = animation
        worker = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            current.animateTo(next, spec, sequentialAnimation = continuing)
            animation = AnimationState(next)
        }
    }

    /** Pointer input needs a synchronous reset, including cancellation of the previous spring. */
    fun snapTo(next: Float) {
        if (!next.isFinite()) return
        worker?.cancel()
        worker = null
        target = next
        animation = AnimationState(next)
    }

    fun freeze() = snapTo(value)
}
