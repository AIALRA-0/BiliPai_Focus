package com.android.purebilibili.feature.video.screen

import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.android.purebilibili.core.util.Logger
import kotlin.math.roundToInt

/** Observes input without consuming it; gesture IDs match across root/body/player layers. */
internal fun Modifier.videoInputDiagnostics(
    layer: String,
    snapshot: () -> String,
): Modifier = composed {
    val latestSnapshot = rememberUpdatedState(snapshot)
    val lastSizeLogAt = remember(layer) { longArrayOf(0L) }
    DisposableEffect(layer) {
        Logger.d("VideoInputTrace") { "layer_attached layer=$layer ${latestSnapshot.value()}" }
        onDispose {
            Logger.d("VideoInputTrace") { "layer_detached layer=$layer ${latestSnapshot.value()}" }
        }
    }
    onSizeChanged { bounds ->
        if (!Logger.areVerboseRuntimeLogsEnabled()) return@onSizeChanged
        val now = SystemClock.elapsedRealtime()
        // Fullscreen morph changes bounds each frame; keep at most four size samples per second.
        if (lastSizeLogAt[0] != 0L && now - lastSizeLogAt[0] < 250L) return@onSizeChanged
        lastSizeLogAt[0] = now
        Logger.d("VideoInputTrace") {
            "layer_size layer=$layer bounds=${bounds.width}x${bounds.height} ${latestSnapshot.value()}"
        }
    }.pointerInput(layer) {
        var gestureId: Long? = null
        var startedAt = 0L
        var moved = false
        var consumed = false
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (!Logger.areVerboseRuntimeLogsEnabled()) {
                        gestureId = null
                        continue
                    }
                    val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                    if (gestureId == null && down != null) {
                        gestureId = down.uptimeMillis
                        startedAt = down.uptimeMillis
                        moved = false
                        consumed = false
                        Logger.d("VideoInputTrace") {
                            "touch_down gesture=$gestureId layer=$layer " +
                                "xy=${down.position.x.roundToInt()},${down.position.y.roundToInt()} " +
                                "bounds=${size.width}x${size.height} ${latestSnapshot.value()}"
                        }
                    }
                    // Final pass observes consumption by descendant scroll/click detectors.
                    val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                    if (gestureId != null) {
                        moved = moved || finalEvent.changes.any { it.position != it.previousPosition }
                        consumed = consumed || finalEvent.changes.any { it.isConsumed }
                        if (finalEvent.changes.none { it.pressed }) {
                            val endedAt = finalEvent.changes.maxOfOrNull { it.uptimeMillis } ?: startedAt
                            Logger.d("VideoInputTrace") {
                                "touch_end gesture=$gestureId layer=$layer durationMs=${endedAt - startedAt} " +
                                    "moved=$moved consumed=$consumed ${latestSnapshot.value()}"
                            }
                            gestureId = null
                        }
                    }
                }
            }
        } finally {
            if (gestureId != null) {
                Logger.d("VideoInputTrace") {
                    "touch_detached gesture=$gestureId layer=$layer ${latestSnapshot.value()}"
                }
            }
        }
    }
}
