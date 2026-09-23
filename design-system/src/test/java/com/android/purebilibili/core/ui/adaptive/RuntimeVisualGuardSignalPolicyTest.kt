package com.android.purebilibili.core.ui.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeVisualGuardSignalPolicyTest {

    @Test
    fun cardTransitionAndScrollSignals_areTracked() {
        assertTrue(isRuntimeVisualGuardTrackedStateKey("VideoCardTransition"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("home:feed:recommend"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("home:pager_swipe"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("home:header_transition"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("video_detail:tab_swipe"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("video_detail:intro_scroll"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("video_detail:comment_scroll"))
        assertTrue(isRuntimeVisualGuardTrackedStateKey("video_detail:player_swipe_collapse"))
    }

    @Test
    fun semanticOnlyMarkers_areNotTracked() {
        // 这两个在界面静止时也长期挂着，纳入窗口会把静止帧算进分母、稀释真实掉帧率。
        assertFalse(isRuntimeVisualGuardTrackedStateKey("home:current_category"))
        assertFalse(isRuntimeVisualGuardTrackedStateKey("video_player:gesture_mode"))
        assertFalse(isRuntimeVisualGuardTrackedStateKey("video_player:gesture_visible"))
        assertFalse(isRuntimeVisualGuardTrackedStateKey("OtherTransition"))
        assertFalse(isRuntimeVisualGuardTrackedStateKey(""))
    }

    @Test
    fun motionTierOrder_isConservativeFirst() {
        // minMotionTier 依赖这个顺序；enum 一旦被重排，守卫会反向放大视觉而非降级。
        assertTrue(motionTierOrderIsConservativeFirst())
    }

    @Test
    fun minMotionTier_picksMoreConservative() {
        assertEquals(MotionTier.Reduced, minMotionTier(MotionTier.Normal, MotionTier.Reduced))
        assertEquals(MotionTier.Reduced, minMotionTier(MotionTier.Reduced, MotionTier.Enhanced))
        assertEquals(MotionTier.Normal, minMotionTier(MotionTier.Enhanced, MotionTier.Normal))
        assertEquals(MotionTier.Enhanced, minMotionTier(MotionTier.Enhanced, MotionTier.Enhanced))
    }

    @Test
    fun mergeWithoutDowngrade_keepsBaseTier() {
        val merged = mergeRuntimeVisualGuardDecisions(
            decisions = listOf(normal(MotionTier.Enhanced), normal(MotionTier.Enhanced)),
            baseTier = MotionTier.Enhanced,
        )

        assertFalse(merged.downgraded)
        assertFalse(merged.forceLowBlurBudget)
        assertEquals(MotionTier.Enhanced, merged.effectiveMotionTier)
    }

    @Test
    fun mergeWithEmptySignals_keepsBaseTier() {
        val merged = mergeRuntimeVisualGuardDecisions(emptyList(), MotionTier.Enhanced)

        assertFalse(merged.downgraded)
        assertEquals(MotionTier.Enhanced, merged.effectiveMotionTier)
    }

    @Test
    fun anySignalDowngraded_downgradesGlobally() {
        val merged = mergeRuntimeVisualGuardDecisions(
            decisions = listOf(normal(MotionTier.Enhanced), downgraded(atMs = 500L)),
            baseTier = MotionTier.Enhanced,
        )

        assertTrue(merged.downgraded)
        assertTrue(merged.forceLowBlurBudget)
        assertEquals(MotionTier.Reduced, merged.effectiveMotionTier)
        assertEquals(500L, merged.nextLastDowngradeAtMs)
    }

    @Test
    fun mergeKeepsLatestDowngradeTimestamp() {
        val merged = mergeRuntimeVisualGuardDecisions(
            decisions = listOf(downgraded(atMs = 100L), downgraded(atMs = 900L)),
            baseTier = MotionTier.Normal,
        )

        assertEquals(900L, merged.nextLastDowngradeAtMs)
    }

    private fun normal(tier: MotionTier) = RuntimeVisualGuardDecision(
        effectiveMotionTier = tier,
        forceLowBlurBudget = false,
        downgraded = false,
        nextLastDowngradeAtMs = null,
    )

    private fun downgraded(atMs: Long) = RuntimeVisualGuardDecision(
        effectiveMotionTier = MotionTier.Reduced,
        forceLowBlurBudget = true,
        downgraded = true,
        nextLastDowngradeAtMs = atMs,
    )
}
