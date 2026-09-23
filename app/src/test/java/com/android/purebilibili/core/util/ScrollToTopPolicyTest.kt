package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScrollToTopPolicyTest {

    @Test
    fun resolveScrollToTopPlan_noPreJumpWhenNearTop() {
        assertNull(resolveScrollToTopPlan(0).preJumpIndex)
        assertNull(resolveScrollToTopPlan(10).preJumpIndex)
        assertNull(resolveScrollToTopPlan(14).preJumpIndex)
    }

    @Test
    fun resolveScrollToTopPlan_usesTieredPreJumpForFarDistance() {
        assertEquals(6, resolveScrollToTopPlan(20).preJumpIndex)
        assertEquals(12, resolveScrollToTopPlan(40).preJumpIndex)
        assertEquals(20, resolveScrollToTopPlan(120).preJumpIndex)
        assertEquals(28, resolveScrollToTopPlan(220).preJumpIndex)
    }

    @Test
    fun resolveScrollToTopPlan_adaptsAnimatedWindowToViewportCapacity() {
        assertNull(resolveScrollToTopPlan(firstVisibleItemIndex = 18, visibleItemCount = 6).preJumpIndex)
        assertEquals(12, resolveScrollToTopPlan(firstVisibleItemIndex = 80, visibleItemCount = 6).preJumpIndex)
        assertEquals(24, resolveScrollToTopPlan(firstVisibleItemIndex = 80, visibleItemCount = 12).preJumpIndex)
    }

    @Test
    fun resolveScrollToTopPlan_boundsAnimatedWindowForExtremeViewportCounts() {
        assertEquals(8, resolveScrollToTopPlan(firstVisibleItemIndex = 80, visibleItemCount = 1).preJumpIndex)
        assertEquals(32, resolveScrollToTopPlan(firstVisibleItemIndex = 160, visibleItemCount = 100).preJumpIndex)
    }

    @Test
    fun resolveFastScrollToTopPlan_limitsAnimatedWindowToSmallBatch() {
        assertNull(resolveFastScrollToTopPlan(firstVisibleItemIndex = 1).preJumpIndex)
        assertNull(resolveFastScrollToTopPlan(firstVisibleItemIndex = 2).preJumpIndex)
        assertEquals(2, resolveFastScrollToTopPlan(firstVisibleItemIndex = 3).preJumpIndex)
        assertEquals(2, resolveFastScrollToTopPlan(firstVisibleItemIndex = 4).preJumpIndex)
        assertEquals(2, resolveFastScrollToTopPlan(firstVisibleItemIndex = 5).preJumpIndex)
        assertEquals(2, resolveFastScrollToTopPlan(firstVisibleItemIndex = 50).preJumpIndex)
        assertEquals(2, resolveFastScrollToTopPlan(firstVisibleItemIndex = 100, visibleItemCount = 8).preJumpIndex)
        assertEquals(1, resolveFastScrollToTopPlan(firstVisibleItemIndex = 100, visibleItemCount = 1).preJumpIndex)
    }

    @Test
    fun shouldShowScrollToTop_usesItemAndOffsetThresholds() {
        assertFalse(shouldShowScrollToTop(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 599))
        assertTrue(shouldShowScrollToTop(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 600))
        assertTrue(shouldShowScrollToTop(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0))
    }
}
