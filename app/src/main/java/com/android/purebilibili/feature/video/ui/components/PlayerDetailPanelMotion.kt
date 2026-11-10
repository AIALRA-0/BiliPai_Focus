package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** 入场、主动关闭、侧拖和系统返回共用位移进度，退出完成后才移除 Popup。 */
@Composable
internal fun PlayerDetailPanelMotion(
    sidePanel: Boolean,
    dragEnabled: Boolean,
    distancePx: Float,
    closeProgress: MutableFloatState,
    onDismiss: () -> Unit,
    onDismissAction: (() -> Unit) -> Unit,
    content: @Composable (requestDismiss: () -> Unit) -> Unit,
) {
    val motionEnabled = LocalComponentMotionEnabled.current && !rememberSystemReduceMotion()
    val latestDismiss = rememberUpdatedState(onDismiss)
    val scope = rememberCoroutineScope()
    val animation = remember(closeProgress) { arrayOfNulls<Job>(1) }
    var closing by remember(closeProgress) { mutableStateOf(false) }
    var trackingBack by remember(closeProgress) { mutableStateOf(false) }
    val backStart = remember(closeProgress) { floatArrayOf(0f) }
    fun settle(target: Float, dismiss: Boolean = false) {
        animation[0]?.cancel()
        animation[0] = scope.launch {
            val remaining = abs(target - closeProgress.floatValue).coerceIn(0f, 1f)
            if (motionEnabled && remaining > 0.001f) {
                animate(closeProgress.floatValue, target,
                    animationSpec = tween(
                        ((if (dismiss) 180 else 220) * remaining).roundToInt().coerceAtLeast(1),
                        easing = AppMotionEasing.M1Standard,
                    ),
                ) { value, _ -> closeProgress.floatValue = value }
            }
            closeProgress.floatValue = target
            if (dismiss) latestDismiss.value()
        }
    }
    val requestDismiss: () -> Unit = {
        if (!closing) {
            closing = true
            trackingBack = false
            settle(1f, dismiss = true)
        }
    }
    SideEffect { onDismissAction(requestDismiss) }
    LaunchedEffect(closeProgress, motionEnabled) { if (!closing) settle(0f) }
    DisposableEffect(closeProgress) { onDispose { animation[0]?.cancel() } }
    PlayerPanelPopupBackHandler(
        predictive = LocalPredictiveBackGestureEnabled.current && motionEnabled,
        onStarted = {
            if (!closing) {
                animation[0]?.cancel()
                trackingBack = true
                backStart[0] = closeProgress.floatValue
            }
        },
        onProgress = { progress ->
            if (trackingBack && !closing) {
                closeProgress.floatValue = backStart[0] + (1f - backStart[0]) * progress
            }
        },
        onCancelled = {
            if (trackingBack && !closing) {
                trackingBack = false
                settle(0f)
            }
        },
        onCompleted = requestDismiss,
    )
    val dragState = rememberDraggableState { delta ->
        closeProgress.floatValue = (closeProgress.floatValue + delta / distancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }
    Box(
        modifier = Modifier
            .graphicsLayer {
                val offset = if (motionEnabled) closeProgress.floatValue * distancePx else 0f
                if (sidePanel) translationX = offset else translationY = offset
            }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Back || event.key == Key.Escape) {
                    if (event.type == KeyEventType.KeyUp) requestDismiss()
                    true
                } else false
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = motionEnabled && sidePanel && dragEnabled && !closing && !trackingBack,
                onDragStarted = { animation[0]?.cancel() },
                onDragStopped = { velocity ->
                    if (closeProgress.floatValue >= 0.42f || velocity >= 900f) requestDismiss() else settle(0f)
                },
            )
            .then(if (closing) Modifier
                .clearAndSetSemantics { }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                } else Modifier),
    ) { content(requestDismiss) }
}
