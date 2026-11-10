package com.android.purebilibili.core.lifecycle

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundMemoryTrimPolicyTest {

    @Test
    fun backgroundCacheBudget_preservesWarmCoversAndNeverGrowsTheCache() {
        val mib = 1024L * 1024
        // 切后台立刻收敛到 64 MiB，足以覆盖底栏所有页面的可见封面。
        assertEquals(64 * mib, resolveBackgroundImageCacheTrimTargetBytes(200 * mib, 0L))
        assertEquals(64 * mib, resolveBackgroundImageCacheTrimTargetBytes(200 * mib, 44_999L))
        // 长时间挂后台才进一步收敛到 32 MiB。
        assertEquals(32 * mib, resolveBackgroundImageCacheTrimTargetBytes(200 * mib, 45_000L))
        assertEquals(7 * mib, resolveBackgroundImageCacheTrimTargetBytes(7 * mib, 120_000L))
        assertEquals(0L, resolveBackgroundImageCacheTrimTargetBytes(0L, 120_000L))
    }

    @Test
    fun backgroundCacheBudget_staysAboveTheDecodeFloorForReturnToHome() {
        val mib = 1024L * 1024
        // 回归护栏：预算必须显著高于「回前台必须重解码」的阈值，
        // 否则底栏切页动画会和封面解码争主线程。
        assertTrue(BACKGROUND_IMAGE_CACHE_LIGHT_MAX_BYTES >= 64 * mib)
        assertTrue(BACKGROUND_IMAGE_CACHE_MAX_BYTES >= 32 * mib)
    }

    @Test
    fun coverColorCacheBudget_keepsEnoughEntriesForEveryBottomTab() {
        // 5 个底栏页面各自可见卡片数合计通常低于 64，砍到 16 会造成大面积串行重算。
        assertTrue(BACKGROUND_COVER_COLOR_CACHE_MAX_ENTRIES >= 64)
    }

    @Test
    fun sustainedBackground_onlyTrimsAfterGracePeriod() {
        assertFalse(shouldTrimImageCacheAfterBackgroundDelay(true, false, 44_999L))
        assertTrue(shouldTrimImageCacheAfterBackgroundDelay(true, false, 45_000L))
        assertTrue(shouldTrimImageCacheAfterBackgroundDelay(true, false, 120_000L))
    }

    @Test
    fun foregroundOrPip_doesNotTrimEvenAfterGracePeriod() {
        assertFalse(shouldTrimImageCacheAfterBackgroundDelay(false, false, 120_000L))
        assertFalse(shouldTrimImageCacheAfterBackgroundDelay(true, true, 120_000L))
        assertFalse(shouldTrimImageCacheAfterBackgroundDelay(false, true, 120_000L))
    }

    @Test
    fun uiHidden_leavesEveryCacheAlone() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        // 切到桌面不等于内存吃紧；后台预算收敛由 BackgroundManager 单独负责。
        assertEquals(null, plan.imageCacheTrimLevel)
        assertFalse(plan.clearImageMemoryCache)
        assertFalse(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
        assertFalse(plan.releaseDerivedCaches)
        assertFalse(plan.releaseIdleNetworkConnections)
    }

    @Test
    fun runningLow_trimsImageAndNotifiesPlayerButKeepsDerivedCachesAndConnections() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, plan.imageCacheTrimLevel)
        assertFalse(plan.clearImageMemoryCache)
        assertTrue(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
        assertFalse(plan.releaseDerivedCaches)
        assertFalse(plan.releaseIdleNetworkConnections)
    }

    @Test
    fun background_clearsImageCacheAndReleasesDerivedCachesAndConnections() {
        val plan = resolveBackgroundMemoryTrimPlan(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND, plan.imageCacheTrimLevel)
        assertTrue(plan.clearImageMemoryCache)
        assertTrue(plan.notifyPlayerHeavyOptimization)
        assertFalse(plan.requestIdlePlaybackRelease)
        assertTrue(plan.releaseDerivedCaches)
        assertTrue(plan.releaseIdleNetworkConnections)
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
            assertTrue("level=$level", plan.releaseDerivedCaches)
            assertTrue("level=$level", plan.releaseIdleNetworkConnections)
        }
    }

    @Test
    fun nonPressureLevels_produceNoOpPlan() {
        listOf(
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            0
        ).forEach { level ->
            val plan = resolveBackgroundMemoryTrimPlan(level)
            assertEquals("level=$level", null, plan.imageCacheTrimLevel)
            assertFalse("level=$level", plan.clearImageMemoryCache)
            assertFalse("level=$level", plan.notifyPlayerHeavyOptimization)
            assertFalse("level=$level", plan.requestIdlePlaybackRelease)
            assertFalse("level=$level", plan.releaseDerivedCaches)
            assertFalse("level=$level", plan.releaseIdleNetworkConnections)
        }
    }
}
