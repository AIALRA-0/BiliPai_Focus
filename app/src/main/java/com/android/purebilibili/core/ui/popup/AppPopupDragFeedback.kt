package com.android.purebilibili.core.ui.popup

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

/** 确认弹窗使用 4dp 按压扩展和 3dp 位移上限；仅玻璃入口装配，不改全局控件反馈。 */
@Composable
internal fun rememberPopupDragFeedback(
    enabled: Boolean,
    closing: Boolean = false,
): Modifier {
    val latestClosing by rememberUpdatedState(closing)
    val density = LocalDensity.current.density
    val motion = remember(enabled, density) { PopupElasticMotion() }
    val scope = rememberCoroutineScope()
    val dragEnabled = com.android.purebilibili.core.ui.LocalAppThemeConfig.current.globalDragFeedbackEnabled
    DisposableEffect(motion, closing) {
        if (closing) motion.stop()
        onDispose { motion.stop() }
    }
    if (!enabled) return Modifier
    return Modifier
        .pointerInput(motion, density, dragEnabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (latestClosing) return@awaitEachGesture
                motion.isTracking = true
                motion.retarget(scope, true, Offset.Zero, reduced = false)
                try {
                    while (true) {
                        // 只旁观，不消费点击、文字选择与内部滚动。
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed || event.changes.count { it.pressed } > 1 || latestClosing) break
                        if (change.position != change.previousPosition && change.isConsumed) break
                        val raw = if (dragEnabled) change.position - Offset(
                            change.position.x.coerceIn(0f, size.width.toFloat()),
                            change.position.y.coerceIn(0f, size.height.toFloat()),
                        ) else Offset.Zero
                        motion.retarget(scope, true,
                            popupRubberBandPull(raw, minOf(size.width, size.height).toFloat(), maxPull = 3f * density), false)
                    }
                } finally {
                    motion.isTracking = false
                    // 窗口退出直接保留当前形变，交给退出转场继续。
                    if (latestClosing) motion.stop()
                    else motion.retarget(scope, false, Offset.Zero, reduced = false)
                }
            }
        }
        .graphicsLayer {
            val rest = Rect(Offset.Zero, size)
            val frame = motion.frame(AppGlassPopupFrame(rest, 0f, 1f, 1f, 0f), density, maxGrowthDp = 4f)
            if (!rest.isEmpty) {
                scaleX = frame.rect.width / rest.width
                scaleY = frame.rect.height / rest.height
                translationX = frame.rect.center.x - rest.center.x
                translationY = frame.rect.center.y - rest.center.y
            }
        }
}
