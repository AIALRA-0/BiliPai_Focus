package com.android.purebilibili.core.lifecycle

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundMemoryTrimPolicyTest {

    @Test
    fun uiHidden_onlyTrimsImageCacheWithoutTouchingPlayer() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, plan.imageCacheTrimLevel)
        assertFalse(plan.clearImageMemoryCache)
        assertFalse(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
    }

    @Test
    fun runningLow_trimsImageAndNotifiesPlayerButKeepsIdleSession() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, plan.imageCacheTrimLevel)
        assertFalse(plan.clearImageMemoryCache)
        assertTrue(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
    }

    @Test
    fun background_clearsImageCacheAndNotifiesPlayerWithoutIdleRelease() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND, plan.imageCacheTrimLevel)
        assertTrue(plan.clearImageMemoryCache)
        assertTrue(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
    }

    @Test
    fun criticalPressure_requestsIdlePlaybackRelease() {
        listOf(
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ).forEach { level ->
            val plan = resolveBackgroundMemoryTrimPlan(level)
            assertTrue("level=$level", plan.notifyPlayerHeavyOptimization)
            assertTrue("level=$level", plan.requestIdlePlaybackRelease)
        }
    }

    @Test
    fun nonPressureLevels_produceNoOpPlan() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
        assertEquals(null, plan.imageCacheTrimLevel)
        assertFalse(plan.clearImageMemoryCache)
        assertFalse(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
    }
}
