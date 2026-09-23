package com.android.purebilibili.core.util

import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomaticCacheClearPolicyTest {
    private val gb = 1024L * 1024L * 1024L

    @Test
    fun defaultThreshold_clearsAtFiveGbEvenWhenPeriodicClearIsDisabled() {
        assertTrue(
            shouldAutomaticallyClearCache(
                interval = SettingsManager.AutoCacheClearInterval.NEVER,
                lastClearAtMillis = 0L,
                nowMillis = 1L,
                reclaimableDiskBytes = 5L * gb,
                thresholdBytes = 5L * gb
            )
        )
    }

    @Test
    fun belowThreshold_doesNotClearWhenPeriodicClearIsDisabled() {
        assertFalse(
            shouldAutomaticallyClearCache(
                interval = SettingsManager.AutoCacheClearInterval.NEVER,
                lastClearAtMillis = 0L,
                nowMillis = 1L,
                reclaimableDiskBytes = 5L * gb - 1L,
                thresholdBytes = 5L * gb
            )
        )
    }

    @Test
    fun elapsedInterval_stillClearsBelowCapacityThreshold() {
        val week = 7L * 24L * 60L * 60L * 1000L
        assertTrue(
            shouldAutomaticallyClearCache(
                interval = SettingsManager.AutoCacheClearInterval.WEEKLY,
                lastClearAtMillis = 1L,
                nowMillis = week + 1L,
                reclaimableDiskBytes = 1L * gb,
                thresholdBytes = 5L * gb
            )
        )
    }
}
