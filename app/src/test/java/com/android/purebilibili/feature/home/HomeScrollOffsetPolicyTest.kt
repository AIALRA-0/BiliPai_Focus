package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeScrollOffsetPolicyTest {

    @Test
    fun liquidGlassDisabled_stillDrivesDockMotion() {
        val next = resolveNextHomeGlobalScrollOffset(
            currentOffset = 120f,
            scrollDeltaY = -15f,
            liquidGlassEnabled = false
        )

        assertEquals(135f, next)
    }

    @Test
    fun tinyDeltaBelowThreshold_skipsStateWrite() {
        val next = resolveNextHomeGlobalScrollOffset(
            currentOffset = 120f,
            scrollDeltaY = 0.3f,
            liquidGlassEnabled = true,
            minUpdateDeltaPx = 0.5f
        )

        assertNull(next)
    }

    @Test
    fun validDelta_updatesOffsetWithSameDirectionAsBefore() {
        val next = resolveNextHomeGlobalScrollOffset(
            currentOffset = 120f,
            scrollDeltaY = -8f,
            liquidGlassEnabled = true,
            minUpdateDeltaPx = 0.5f
        )

        assertEquals(128f, next)
    }
}
