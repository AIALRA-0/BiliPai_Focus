package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SettingsIosPushTransitionPolicyTest {

    @Test
    fun resolveSettingsIosPushTransitionMillis_respectsReduceMotion() {
        assertEquals(0, resolveSettingsIosPushTransitionMillis(animationEnabled = true, reduceMotion = true))
        assertEquals(
            SETTINGS_IOS_PUSH_DURATION_MS,
            resolveSettingsIosPushTransitionMillis(animationEnabled = true, reduceMotion = false),
        )
    }

    @Test
    fun settingsIosPushForward_keepsBottomPageStill() {
        val transform = resolveSettingsIosPushForwardContentTransform()
        // 底层页（initial content）静止，只滑入顶层设置页。
        assertEquals(ExitTransition.None, transform.initialContentExit)
        assertNotEquals(EnterTransition.None, transform.targetContentEnter)
        // 时长守卫：<=0 时整段动画关闭。
        val zero = resolveSettingsIosPushForwardContentTransform(durationMillis = 0)
        assertEquals(EnterTransition.None, zero.targetContentEnter)
        assertEquals(ExitTransition.None, zero.initialContentExit)
    }

    @Test
    fun settingsIosPushPop_keepsBottomPageStill() {
        val transform = resolveSettingsIosPushPopContentTransform()
        // 底层页（target content）静止，设置页向右滑出。
        assertEquals(EnterTransition.None, transform.targetContentEnter)
        assertNotEquals(ExitTransition.None, transform.initialContentExit)
        val zero = resolveSettingsIosPushPopContentTransform(durationMillis = 0)
        assertEquals(EnterTransition.None, zero.targetContentEnter)
        assertEquals(ExitTransition.None, zero.initialContentExit)
    }

    @Test
    fun settingsIosPredictivePop_keepsTargetEnterNone() {
        val transform = resolveSettingsIosPredictivePopContentTransform()
        assertEquals(EnterTransition.None, transform.targetContentEnter)
        assertNotEquals(ExitTransition.None, transform.initialContentExit)
        val zero = resolveSettingsIosPredictivePopContentTransform(durationMillis = 0)
        assertEquals(EnterTransition.None, zero.targetContentEnter)
        assertEquals(ExitTransition.None, zero.initialContentExit)
    }
}
