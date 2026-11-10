package com.android.purebilibili.feature.video.ui.feedback

data class TripleActionMotionSpec(
    val activationDurationMillis: Int,
    val convergenceDurationMillis: Int,
    val resolutionDurationMillis: Int,
    val dwellDurationMillis: Int,
    val usesConvergence: Boolean
)

enum class TripleCelebrationPlacement {
    CenterOverlay,
    BottomTrailing
}

fun resolveTripleActionMotionSpec(reducedMotion: Boolean): TripleActionMotionSpec {
    return if (reducedMotion) {
        TripleActionMotionSpec(
            activationDurationMillis = 120,
            convergenceDurationMillis = 0,
            resolutionDurationMillis = 180,
            dwellDurationMillis = 1200,
            usesConvergence = false
        )
    } else {
        TripleActionMotionSpec(
            activationDurationMillis = 180,
            convergenceDurationMillis = 280,
            resolutionDurationMillis = 220,
            dwellDurationMillis = 1400,
            usesConvergence = true
        )
    }
}

fun resolveTripleCelebrationPlacement(
    isFullscreen: Boolean,
    isLandscape: Boolean
): TripleCelebrationPlacement {
    return TripleCelebrationPlacement.BottomTrailing
}
