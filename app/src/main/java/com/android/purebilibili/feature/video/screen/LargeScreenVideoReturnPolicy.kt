package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.resolveVideoDetailMediaLayoutHandoffProgress
import com.android.purebilibili.core.ui.transition.resolveVideoCardDetailChromeAlpha
import com.android.purebilibili.core.ui.transition.resolveNowPlayingBarMediaLayoutHandoff
import kotlin.math.roundToInt

internal fun resolveLargeScreenSidePanelRevealWidthPx(
    baseWidthPx: Int,
    windowWidthPx: Int,
    progress: Float,
): Int {
    val base = baseWidthPx.coerceAtLeast(0)
    val target = windowWidthPx.coerceAtLeast(base)
    return (base + (target - base) * progress.coerceIn(0f, 1f)).roundToInt()
}

internal fun resolveLargeScreenVideoReturnMediaProgress(
    hasLandingGeometry: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
    isReturning: Boolean,
    isCommittedReturn: Boolean,
    depth: Float,
    sourceLayout: VideoCardSourceLayout,
    sourceChromeAlpha: Float? = null,
    isNowPlayingBar: Boolean = false,
): Float {
    if (!hasLandingGeometry) return 0f
    if (isCommittedReturn && phase == VideoCardTransitionBackgroundPhase.IDLE) return 1f
    // Opening, predictive seek, and returning use the same ordinary-card geometry policy.
    if (isNowPlayingBar && phase == VideoCardTransitionBackgroundPhase.OPENING) {
        return sourceChromeAlpha ?: (1f - resolveVideoCardDetailChromeAlpha(
            morphDepthProgress = depth,
            phase = phase,
            isReturnGestureInProgress = false,
        ))
    }
    if (isNowPlayingBar) {
        return if (isReturning || phase == VideoCardTransitionBackgroundPhase.RETURNING) {
            resolveNowPlayingBarMediaLayoutHandoff(depth)
        } else 0f
    }
    return resolveVideoDetailMediaLayoutHandoffProgress(
        morphDepthProgress = depth,
        phase = phase,
        isReturnGestureInProgress = isReturning,
        sourceLayout = sourceLayout,
        sourceChromeAlpha = sourceChromeAlpha,
        isCommittedCardReturn = isCommittedReturn,
    )
}

internal fun shouldHideLargeScreenVideoAfterReturn(
    isCommittedReturn: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
): Boolean = isCommittedReturn && phase == VideoCardTransitionBackgroundPhase.IDLE
