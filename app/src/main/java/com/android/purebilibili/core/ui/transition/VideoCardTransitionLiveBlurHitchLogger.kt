package com.android.purebilibili.core.ui.transition

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.android.purebilibili.BuildConfig
import kotlinx.coroutines.isActive

private const val VIDEO_CARD_TRANSITION_HITCH_TAG = "VideoCardTransition"
private const val VIDEO_CARD_TRANSITION_HITCH_THRESHOLD_MS = 18f

/**
 * Debug 版专用：在来源页 live 模糊重录期间采样帧间隔。
 * Logcat 过滤：`adb logcat -s VideoCardTransition:W`
 *
 * 超过约 18ms（<60fps）打一条 hitch，便于中低端机核对 OPENING/RETURNING 是否掉帧。
 */
@Composable
internal fun VideoCardTransitionLiveBlurHitchLogger(
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    liveRecordingActive: Boolean,
) {
    if (!BuildConfig.DEBUG) return
    val hitchThresholdMs = remember { VIDEO_CARD_TRANSITION_HITCH_THRESHOLD_MS }
    LaunchedEffect(liveRecordingActive) {
        if (!liveRecordingActive) return@LaunchedEffect
        var lastFrameNanos = 0L
        var hitchCount = 0
        var sampleCount = 0
        var maxDeltaMs = 0f
        try {
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameNanos != 0L) {
                        val deltaMs = (frameTimeNanos - lastFrameNanos) / 1_000_000f
                        sampleCount += 1
                        if (deltaMs > maxDeltaMs) {
                            maxDeltaMs = deltaMs
                        }
                        if (deltaMs >= hitchThresholdMs) {
                            hitchCount += 1
                            Log.w(
                                VIDEO_CARD_TRANSITION_HITCH_TAG,
                                "live-blur hitch ${"%.1f".format(deltaMs)}ms " +
                                    "phase=${phaseProvider()} samples=$sampleCount hitches=$hitchCount",
                            )
                        }
                    }
                    lastFrameNanos = frameTimeNanos
                }
            }
        } finally {
            if (sampleCount > 0) {
                Log.i(
                    VIDEO_CARD_TRANSITION_HITCH_TAG,
                    "live-blur session end phase=${phaseProvider()} " +
                        "samples=$sampleCount hitches=$hitchCount max=${"%.1f".format(maxDeltaMs)}ms",
                )
            }
        }
    }
}
