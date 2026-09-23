// Used exclusively by FloatingBottomBar so home dock interactions match that component, not the
// design-system DampedDragAnimationState used by top tabs / segmented controls.
package com.android.purebilibili.feature.home.components.miuix

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class DampedDragTrackingMode {
    SPRING,
    DIRECT,
}

internal fun normalizeFloatingDockDragVelocity(
    slotVelocity: Float,
    valueRange: ClosedRange<Float>,
): Float = slotVelocity / (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)

/**
 * Floating dock damped-drag kernel: spring-followed value, press/scale springs, velocity
 * deformation sampling, and [modifier] driven by [inspectDragGestures] + [canDrag].
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    pressedScale: Float,
    private val trackingMode: DampedDragTrackingMode = DampedDragTrackingMode.SPRING,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec =
        spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    // Motion tuning copied from HyperIsland's LiquidGlassNavigationBar.
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    // Pointer events may arrive again before the coroutine launched by updateValue starts.
    // Keep the requested target synchronous so every drag delta accumulates from the latest one.
    private var requestedValue = initialValue.coerceIn(valueRange)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()
    private var valueTrackingJob: Job? = null
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    private var velocityJob: Job? = null

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = requestedValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    var isDragging by mutableStateOf(false)
        private set

    var pressedScale: Float = pressedScale

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        var gestureAccepted = false
        inspectDragGestures(
            onDragStart = { down ->
                // Decide ownership from the initial down and keep it for the whole gesture.
                // A predictive-back swipe may enter the dock after starting in the system edge
                // band; it must never be adopted halfway through by the liquid indicator.
                gestureAccepted = canDrag(down.position)
                if (gestureAccepted) {
                    isDragging = true
                    onDragStarted(down.position)
                    press()
                }
            },
            onDragEnd = {
                if (gestureAccepted) {
                    // Settle first so pager-follow observers cannot snap to the stale page
                    // between isDragging flipping false and the drag target being recorded.
                    onDragStopped()
                    isDragging = false
                    release()
                }
                gestureAccepted = false
            },
            onDragCancel = {
                if (gestureAccepted) {
                    onDragStopped()
                    isDragging = false
                    release()
                }
                gestureAccepted = false
            }
        ) { change, dragAmount ->
            if (!gestureAccepted) return@inspectDragGestures
            // Once the indicator owns this pointer, keep following it even if the moving
            // hit box translates out from under the finger. Dropping deltas here is what
            // made a second drag lose real-time tracking after search resized the dock.
            if (dragAmount != Offset.Zero) {
                change.consume()
            }
            onDrag(size, dragAmount)
        }
    }

    val longPressModifier: Modifier = Modifier.pointerInput(Unit) {
        var gestureAccepted = false
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                gestureAccepted = canDrag(position)
                if (gestureAccepted) {
                    isDragging = true
                    onDragStarted(position)
                    press()
                }
            },
            onDragEnd = {
                if (gestureAccepted) {
                    onDragStopped()
                    isDragging = false
                    release()
                }
                gestureAccepted = false
            },
            onDragCancel = {
                if (gestureAccepted) {
                    onDragStopped()
                    isDragging = false
                    release()
                }
                gestureAccepted = false
            },
            onDrag = { change, dragAmount ->
                if (!gestureAccepted) return@detectDragGesturesAfterLongPress
                if (dragAmount != Offset.Zero) change.consume()
                onDrag(size, dragAmount)
            },
        )
    }

    fun press() {
        velocityTracker.resetTracking()
        releaseJob?.cancel()
        pressJob?.cancel()
        pressJob = animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        releaseJob?.cancel()
        velocityJob?.cancel()
        releaseJob = animationScope.launch {
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = ((valueRange.endInclusive - valueRange.start) * 0.025f)
                    .coerceAtLeast(visibilityThreshold.coerceAtLeast(0.001f))
                snapshotFlow { abs(valueAnimation.value - targetValue) }
                    .filter { it <= threshold }
                    .first()
            }
            // 照搬 HyperIsland LiquidGlassNavigationBar 的 release()：直接接管正在跑的
            // 放大动画，不等待它结束。原先这里的 pressJob?.join() 会让缩放先跑到峰值停住、
            // 之后才开始缩回（放大 → 停顿 → 缩小），破坏连贯性。
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
            // 速度形变是非对称的（scaleX 除以 1-v、scaleY 乘以 1-v），不归零就会留下
            // 椭圆残影。参考项目靠 animateToValue 里的归零，这里补上 release 路径。
            launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
        }
    }

    fun snapTo(value: Float) {
        val next = value.coerceIn(valueRange)
        requestedValue = next
        launchValueTracking { valueAnimation.snapTo(next) }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        requestedValue = targetValue
        if (trackingMode == DampedDragTrackingMode.DIRECT || isDragging) {
            launchValueTracking {
                valueAnimation.snapTo(targetValue)
                updateVelocity()
            }
            return
        }
        launchValueTracking {
            valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(
        value: Float,
        animatePress: Boolean = true,
    ) {
        val targetValue = value.coerceIn(valueRange)
        requestedValue = targetValue
        launchValueTracking {
            mutatorMutex.mutate {
                if (animatePress) press()
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    velocityJob?.cancel()
                    velocityJob = launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                if (animatePress) release()
            }
        }
    }

    /**
     * Installs the newest position mutation before the caller returns. Pointer updates can arrive
     * faster than the dispatcher gets a chance to run a normally launched coroutine; cancelling a
     * still-pending job on every event leaves [valueAnimation] frozen while [requestedValue] moves.
     * Undispatched start closes that scheduling gap while cancellation still guarantees latest-wins.
     */
    private fun launchValueTracking(block: suspend CoroutineScope.() -> Unit) {
        valueTrackingJob?.cancel()
        valueTrackingJob = animationScope.launch(
            start = CoroutineStart.UNDISPATCHED,
            block = block,
        )
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            SystemClock.uptimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity = normalizeFloatingDockDragVelocity(
            slotVelocity = velocityTracker.calculateVelocity().x,
            valueRange = valueRange,
        )
        velocityJob?.cancel()
        velocityJob = animationScope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) {
            velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)
        }
    }
}
