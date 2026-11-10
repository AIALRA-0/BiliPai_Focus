package com.android.purebilibili.core.ui.transition

/**
 * Media geometry shared by phone and tablet card handoffs.
 * Source pixels must use measured cover geometry on opening, depth-driven predictive seek,
 * and the committed endpoint. Ordinary playback keeps its detail viewport.
 */
internal fun resolveVideoDetailMediaLayoutHandoffProgress(
    morphDepthProgress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    isReturnGestureInProgress: Boolean,
    sourceLayout: VideoCardSourceLayout,
    sourceChromeAlpha: Float? = null,
    isCommittedCardReturn: Boolean = false,
    preserveNativeViewport: Boolean = false,
): Float {
    val depth = morphDepthProgress.coerceIn(0f, 1f)
    // The clock can settle before navigation removes the outgoing entry.
    if (isCommittedCardReturn && !isReturnGestureInProgress &&
        phase == VideoCardTransitionBackgroundPhase.IDLE
    ) return 1f
    // Native card chrome and the shell already follow body presence. Media geometry
    // must seek the same envelope, including cancellation; the legacy HELD shortcut
    // below would jump straight to source geometry as soon as early chrome appears.
    if (preserveNativeViewport && sourceLayout != VideoCardSourceLayout.COVER_ONLY &&
        (phase == VideoCardTransitionBackgroundPhase.OPENING ||
            isVideoCardReturnContentYieldActive(phase, isReturnGestureInProgress, depth))
    ) return 1f - resolveVideoCardNativeBodyPresence(depth)
    if (phase == VideoCardTransitionBackgroundPhase.OPENING && !isReturnGestureInProgress) {
        return sourceChromeAlpha?.coerceIn(0f, 1f) ?: (1f - resolveVideoCardDetailChromeAlpha(
            morphDepthProgress = depth,
            phase = phase,
            isReturnGestureInProgress = false,
            sourceLayout = sourceLayout,
        ))
    }
    // Chrome accepts depth-driven predictive seek even while the clock is HELD.
    // Visible source pixels require the measured cover; tiny dips keep the playback viewport.
    if (sourceLayout == VideoCardSourceLayout.STACKED &&
        phase == VideoCardTransitionBackgroundPhase.HELD &&
        (sourceChromeAlpha ?: resolveVideoCardSourceChromeReturnAlpha(depth)) > 0f
    ) return 1f
    return resolveVideoDetailReturnMediaLayoutHandoffProgress(
        morphDepthProgress = depth,
        phase = phase,
        isReturnGestureInProgress = isReturnGestureInProgress,
        sourceLayout = sourceLayout,
        sourceChromeAlpha = sourceChromeAlpha,
    )
}
