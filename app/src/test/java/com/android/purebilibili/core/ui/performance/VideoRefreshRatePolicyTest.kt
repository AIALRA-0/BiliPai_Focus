package com.android.purebilibili.core.ui.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoRefreshRatePolicyTest {
    private fun modes(vararg rates: Float) = rates.mapIndexed { index, rate ->
        AppDisplayMode(index + 1, rate, 1080, 2400)
    }

    private fun select(rates: List<AppDisplayMode>, fps: Float, speed: Float = 1f) =
        resolveIdleVideoDisplayMode(rates, 1080, 2400, fps, speed)?.refreshRate

    @Test
    fun standardAndFractionalVideoRatesChooseTheLowestMatchingMode() {
        val rates = modes(120f, 60f, 90f)
        assertEquals(60f, select(rates, 30f))
        assertEquals(60f, select(rates, 29.97f))
        assertEquals(60f, select(rates, 59.94f))
        assertEquals(60f, select(rates, 60f))
    }

    @Test
    fun filmRatesPreferAnIntegerMultipleToAvoidUnevenCadence() {
        assertEquals(48f, select(modes(60f, 120f, 48f), 23.976f))
        assertEquals(120f, select(modes(60f, 120f), 24f))
        assertEquals(50f, select(modes(60f, 100f, 50f), 25f))
    }

    @Test
    fun playbackSpeedIsIncludedAndVideoFramesAreNeverCapped() {
        val rates = modes(60f, 90f, 120f)
        assertEquals(120f, select(rates, 60f, 2f))
        assertEquals(90f, select(rates, 60f, 1.5f))
        assertNull(select(rates, 60f, 2.5f))
    }

    @Test
    fun unsupportedCadenceFallsBackWithoutChangingResolution() {
        val rates = modes(60f, 90f) + AppDisplayMode(9, 50f, 720, 1600)
        assertEquals(60f, select(rates, 25f))
        assertNull(select(listOf(AppDisplayMode(9, 60f, 720, 1600)), 30f))
    }

    @Test
    fun unknownRatesAndInvalidSpeedsDoNotRequestAMode() {
        val rates = modes(60f, 120f)
        for (fps in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(select(rates, fps))
        }
        for (speed in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(select(rates, 30f, speed))
        }
    }
}
