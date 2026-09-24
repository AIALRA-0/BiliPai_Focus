package com.android.purebilibili.core.ui.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InteractionRefreshRatePolicyTest {

    @Test
    fun interactionUsesTheFastestModeAtTheCurrentResolution() {
        val mode = resolveActiveRefreshMode(
            currentModeId = 2,
            supportedModes = listOf(
                DisplayRefreshMode(modeId = 1, refreshRate = 60f, width = 1080, height = 2400),
                DisplayRefreshMode(modeId = 2, refreshRate = 120f, width = 1080, height = 2400),
                DisplayRefreshMode(modeId = 3, refreshRate = 144f, width = 1440, height = 3200),
            ),
        )

        assertEquals(2, mode?.modeId)
        assertEquals(120f, mode?.refreshRate)
    }

    @Test
    fun severalRates_pickTheFastestAtTheCurrentSize() {
        val mode = resolveActiveRefreshMode(
            currentModeId = 1,
            supportedModes = listOf(
                DisplayRefreshMode(modeId = 1, refreshRate = 60f, width = 1220, height = 2712),
                DisplayRefreshMode(modeId = 4, refreshRate = 90f, width = 1220, height = 2712),
                DisplayRefreshMode(modeId = 5, refreshRate = 144f, width = 1220, height = 2712),
            ),
        )

        assertEquals(5, mode?.modeId)
        assertEquals(144f, mode?.refreshRate)
    }

    @Test
    fun singleMode_isNotPinned() {
        assertNull(
            resolveActiveRefreshMode(
                currentModeId = 1,
                supportedModes = listOf(
                    DisplayRefreshMode(modeId = 1, refreshRate = 120f, width = 1080, height = 2400),
                ),
            ),
        )
    }

    @Test
    fun unknownCurrentMode_isIgnored() {
        assertNull(
            resolveActiveRefreshMode(
                currentModeId = 99,
                supportedModes = listOf(
                    DisplayRefreshMode(modeId = 1, refreshRate = 60f, width = 1080, height = 2400),
                    DisplayRefreshMode(modeId = 2, refreshRate = 120f, width = 1080, height = 2400),
                ),
            ),
        )
    }
}
