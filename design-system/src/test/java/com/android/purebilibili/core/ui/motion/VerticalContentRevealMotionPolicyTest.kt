package com.android.purebilibili.core.ui.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class VerticalContentRevealMotionPolicyTest {

    @Test
    fun commentRevealKeepsDefaultExpandVerticallyBehavior() {
        val motion = resolveCommentVerticalContentRevealMotionSpec()

        assertEquals(VerticalContentRevealMode.DefaultExpand, motion.mode)
        assertEquals(0, motion.delayMillis)
        assertEquals(0, motion.durationMillis)
        assertEquals(0f, motion.slideOffsetDp)
        assertEquals(1f, motion.initialScale)
    }

    @Test
    fun detailRevealUsesCommentStyleVerticalRevealWithFloatUp() {
        val motion = resolveDetailVerticalContentRevealMotionSpec(
            delayMillis = 40,
            durationMillis = 220,
            slideOffsetDp = 14f,
            initialScale = 0.985f
        )

        assertEquals(VerticalContentRevealMode.FloatUp, motion.mode)
        assertEquals(40, motion.delayMillis)
        assertEquals(220, motion.durationMillis)
        assertEquals(14f, motion.slideOffsetDp)
        assertEquals(0.985f, motion.initialScale)
    }
}
