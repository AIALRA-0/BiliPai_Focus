package com.android.purebilibili.core.ui.components

import android.view.ViewConfiguration as AndroidViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** 只连接玻璃控件与同窗菜单；隐藏源控件不会中断已按下的指针。 */
class AppSingleChoicePopupPress {
    interface Listener {
        fun onMove(positionInWindow: Offset)
        fun onRelease(positionInWindow: Offset)
        fun onCancel()
    }

    private var listener: Listener? = null
    private var finished = false
    private var moved = false

    fun attach(listener: Listener?) {
        this.listener = if (finished) null else listener
    }

    fun move(positionInWindow: Offset) {
        if (finished) return
        moved = true
        listener?.onMove(positionInWindow)
    }

    fun release(positionInWindow: Offset) {
        if (finished) return
        finished = true
        val target = listener
        listener = null
        if (moved) target?.onRelease(positionInWindow) else target?.onCancel()
    }

    fun cancel() {
        if (finished) return
        finished = true
        val target = listener
        listener = null
        target?.onCancel()
    }
}

@Composable
internal fun rememberChoicePopupPressModifier(
    enabled: Boolean,
    toWindow: (Offset) -> Offset,
    onOpen: (AppSingleChoicePopupPress) -> Unit,
    consumeInInitial: Boolean = false,
): Modifier {
    val latestOpen by rememberUpdatedState(appValueChangeWithFeedback(onValueChange = onOpen))
    val latestToWindow by rememberUpdatedState(toWindow)
    return if (!enabled) Modifier else Modifier.pointerInput(consumeInInitial) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var extraPointer = false
            val stillHeld = withTimeoutOrNull(AndroidViewConfiguration.getTapTimeout().toLong()) {
                var waiting = true
                while (waiting) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    extraPointer = event.changes.any { it.id != down.id && it.pressed }
                    waiting = change != null && change.pressed &&
                        !(change.isConsumed && change.position != change.previousPosition) &&
                        !extraPointer &&
                        (change.position - down.position).getDistance() <= viewConfiguration.touchSlop
                }
                false
            }
            // 快点按由原行点击处理；超出阈值的早期拖动留给设置列表滚动。
            if (stillHeld != null) {
                if (extraPointer) {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
                return@awaitEachGesture
            }
            val press = AppSingleChoicePopupPress()
            latestOpen(press)
            var moved = false
            try {
                while (true) {
                    // 公共按钮的父级观察器须先于子按钮消费抬手，避免长按后再执行一次短按。
                    val pass = if (consumeInInitial) PointerEventPass.Initial else PointerEventPass.Main
                    val event = awaitPointerEvent(pass)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || change.isConsumed) break
                    if (event.changes.any { it.id != down.id && it.pressed }) {
                        press.cancel()
                        event.changes.forEach { it.consume() }
                        var held = event.changes.any { it.pressed }
                        while (held) {
                            val remaining = awaitPointerEvent(pass)
                            remaining.changes.forEach { it.consume() }
                            held = remaining.changes.any { it.pressed }
                        }
                        break
                    }
                    val position = latestToWindow(change.position)
                    moved = moved || (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    change.consume()
                    if (!change.pressed) {
                        press.release(position)
                        break
                    }
                    if (moved) press.move(position)
                }
            } finally {
                press.cancel()
            }
        }
    }
}
