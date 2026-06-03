package com.android.purebilibili.core.ui.animation.gl

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticleRendererPolicyTest {

    @Test
    fun `shouldNotifyParticleAnimationComplete fires once after duration`() {
        assertFalse(
            shouldNotifyParticleAnimationComplete(
                hasAnimationCompleted = false,
                currentTimeSec = 0.40f,
                animationDurationSec = 0.82f
            )
        )
        assertTrue(
            shouldNotifyParticleAnimationComplete(
                hasAnimationCompleted = false,
                currentTimeSec = 0.90f,
                animationDurationSec = 0.82f
            )
        )
        assertFalse(
            shouldNotifyParticleAnimationComplete(
                hasAnimationCompleted = true,
                currentTimeSec = 1.20f,
                animationDurationSec = 0.82f
            )
        )
    }
}
