package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.adaptive.MotionTier

internal data class TabletVideoInfoEntranceSpec(
    val enabled: Boolean,
    val durationMillis: Int,
    val staggerDelayMillis: Int,
    val initialOffsetDivisor: Int,
)

internal fun resolveTabletVideoInfoEntranceSpec(
    motionTier: MotionTier,
    systemReduceMotion: Boolean,
): TabletVideoInfoEntranceSpec {
    if (systemReduceMotion || motionTier == MotionTier.Reduced) {
        return TabletVideoInfoEntranceSpec(
            enabled = false,
            durationMillis = 0,
            staggerDelayMillis = 0,
            initialOffsetDivisor = 1,
        )
    }
    return when (motionTier) {
        MotionTier.Enhanced -> TabletVideoInfoEntranceSpec(
            enabled = true,
            durationMillis = 320,
            staggerDelayMillis = 60,
            initialOffsetDivisor = 10,
        )
        MotionTier.Normal -> TabletVideoInfoEntranceSpec(
            enabled = true,
            durationMillis = 260,
            staggerDelayMillis = 48,
            initialOffsetDivisor = 12,
        )
        MotionTier.Reduced -> error("Reduced motion is handled above")
    }
}
