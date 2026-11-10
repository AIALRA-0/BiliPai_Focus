package com.android.purebilibili.feature.video.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion

// Local effects budget, independent of navigation speed and material geometry.
// 150ms follows Material's short effects tier; this is not an Apple HIG constant.
internal const val VIDEO_DETAIL_ENTRY_REVEAL_DURATION_MS = 150
private const val VIDEO_DETAIL_REDUCED_REVEAL_DURATION_MS = 100

/** Return State so animation ticks are consumed only by the media draw/layer providers. */
@Composable
internal fun rememberVideoDetailFirstFrameReveal(
    bvid: String,
    cid: Long,
    hasRenderedFirstFrame: Boolean,
    enabled: Boolean,
): State<Float> {
    val reduceMotion = rememberSystemReduceMotion()
    val reveal = remember(bvid, cid) {
        // Reused/live players already showing a frame must never acquire a new cover.
        Animatable(if (!enabled || hasRenderedFirstFrame) 1f else 0f)
    }
    LaunchedEffect(reveal, enabled, hasRenderedFirstFrame, reduceMotion) {
        when {
            !enabled -> reveal.snapTo(1f)
            !hasRenderedFirstFrame -> reveal.snapTo(0f)
            else -> reveal.animateTo(
                1f,
                tween(
                    durationMillis = if (reduceMotion) VIDEO_DETAIL_REDUCED_REVEAL_DURATION_MS
                    else VIDEO_DETAIL_ENTRY_REVEAL_DURATION_MS,
                    easing = AppMotionEasing.Md3StandardDecelerate,
                ),
            )
        }
    }
    return reveal.asState()
}

/**
 * Call inside the success-content identity. Decide once on mounting: early/cached content
 * continues to use the root morph; only content arriving after landing gets this fade.
 * [contentVisible] gates the animation start: content mounted while the entry transition
 * is still running (Loading skeleton already swapped out) holds at 0 and only fades in
 * once the gate opens, so the skeleton underlay bridges the whole wait without a black gap.
 * Refreshes and return-preview cancellation must not restart the reveal.
 */
@Composable
internal fun rememberVideoDetailLateContentReveal(
    animateOnMount: Boolean,
    contentVisible: Boolean = true,
): State<Float> {
    val reduceMotion = rememberSystemReduceMotion()
    val reveal = remember { Animatable(if (!animateOnMount && contentVisible) 1f else 0f) }
    LaunchedEffect(reveal, reduceMotion, contentVisible) {
        if (contentVisible && reveal.value < 1f) {
            reveal.animateTo(
                1f,
                tween(
                    durationMillis = if (reduceMotion) VIDEO_DETAIL_REDUCED_REVEAL_DURATION_MS
                    else VIDEO_DETAIL_ENTRY_REVEAL_DURATION_MS,
                    easing = AppMotionEasing.Md3StandardDecelerate,
                ),
            )
        }
    }
    return reveal.asState()
}
