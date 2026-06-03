package com.android.purebilibili.feature.screenshot

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.appScreenshotGestureDetector(
    enabled: Boolean,
    mode: AppScreenshotGestureMode,
    blocked: Boolean,
    onCaptureRequested: () -> Unit
): Modifier = composed {
    val density = LocalDensity.current
    val hotZoneSizePx = with(density) { 96.dp.toPx() }
    val threeFingerDistancePx = with(density) { 96.dp.toPx() }

    pointerInput(enabled, mode, blocked, hotZoneSizePx, threeFingerDistancePx) {
        awaitPointerEventScope {
            var hasTriggeredInCurrentGesture = false
            var twoFingerStartedAtMillis: Long? = null
            var threeFingerStartCentroid: Offset? = null

            while (true) {
                val event = awaitPointerEvent()
                val pressedChanges = event.changes.filter { it.pressed }
                if (pressedChanges.isEmpty()) {
                    hasTriggeredInCurrentGesture = false
                    twoFingerStartedAtMillis = null
                    threeFingerStartCentroid = null
                    continue
                }
                if (!enabled || blocked || mode == AppScreenshotGestureMode.DISABLED || hasTriggeredInCurrentGesture) {
                    continue
                }

                when (mode) {
                    AppScreenshotGestureMode.TOP_RIGHT_TWO_FINGER_LONG_PRESS -> {
                        val startedAt = twoFingerStartedAtMillis
                            ?: pressedChanges.minOf { it.uptimeMillis }.also { twoFingerStartedAtMillis = it }
                        val elapsedMillis = pressedChanges.maxOf { it.uptimeMillis } - startedAt
                        val shouldTrigger = shouldTriggerTopRightTwoFingerLongPress(
                            enabled = enabled,
                            blocked = blocked,
                            pointerCount = pressedChanges.size,
                            pointerPositions = pressedChanges.toAppScreenshotPointerPositions(),
                            elapsedMillis = elapsedMillis,
                            screenWidthPx = size.width.toFloat(),
                            hotZoneSizePx = hotZoneSizePx
                        )
                        if (shouldTrigger) {
                            hasTriggeredInCurrentGesture = true
                            onCaptureRequested()
                        }
                        if (pressedChanges.size != 2) {
                            twoFingerStartedAtMillis = null
                        }
                    }

                    AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN -> {
                        val currentCentroid = pressedChanges.centroid()
                        val startCentroid = threeFingerStartCentroid ?: currentCentroid.also {
                            threeFingerStartCentroid = it
                        }
                        val drag = currentCentroid - startCentroid
                        val shouldTrigger = shouldTriggerThreeFingerSwipeDown(
                            enabled = enabled,
                            blocked = blocked,
                            mode = mode,
                            pointerCount = pressedChanges.size,
                            totalDragX = drag.x,
                            totalDragY = drag.y,
                            triggerDistancePx = threeFingerDistancePx,
                            maxHorizontalToVerticalRatio = 0.6f
                        )
                        if (shouldTrigger) {
                            hasTriggeredInCurrentGesture = true
                            onCaptureRequested()
                        }
                        if (pressedChanges.size != 3) {
                            threeFingerStartCentroid = null
                        }
                    }

                    AppScreenshotGestureMode.DISABLED -> Unit
                }
            }
        }
    }
}

private fun List<PointerInputChange>.toAppScreenshotPointerPositions(): List<AppScreenshotPointerPosition> {
    return map { change ->
        AppScreenshotPointerPosition(
            x = change.position.x,
            y = change.position.y
        )
    }
}

private fun List<PointerInputChange>.centroid(): Offset {
    if (isEmpty()) return Offset.Zero
    val x = sumOf { it.position.x.toDouble() }.toFloat() / size
    val y = sumOf { it.position.y.toDouble() }.toFloat() / size
    return Offset(x, y)
}
