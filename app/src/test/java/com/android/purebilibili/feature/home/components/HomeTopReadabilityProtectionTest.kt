package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.blur.ProgressiveBlur

class HomeTopReadabilityProtectionTest {
    @Test
    fun searchStaysLightWhileLastTabRowIsProtectedAndTrailingEdgeClears() {
        val stops = homeTopReadabilityStops(152f, 104f, 152f, 196f, 16f, 0.24f, 1f)
        assertEquals(0.24f, stops.first().second, 0.0001f)
        val centerAlpha = stops.first { it.first == 128f / 196f }.second
        assertEquals(0.32f, centerAlpha, 0.0001f)
        assertTrue(stops.first { it.first == 104f / 196f }.second < centerAlpha)
        assertTrue(stops.first { it.first == 152f / 196f }.second < centerAlpha)
        assertTrue(stops.all { it.second in 0f..0.32f })
        assertEquals(0f, stops.last().second, 0.0001f)
        assertTrue(stops.zipWithNext().all { (a, b) -> a.first < b.first })
    }

    @Test
    fun tabsBeforeSearchDoNotApplyTheirStrongProtectionAcrossTheSearchRow() {
        val stops = homeTopReadabilityStops(152f, 24f, 64f, 196f, 16f, 0.24f, 1f)
        assertEquals(0.32f, stops.first { it.first == 44f / 196f }.second, 0.0001f)
        assertTrue(stops.first { it.first == 64f / 196f }.second < 0.32f)
        assertEquals(0.24f, stops.first { it.first == 80f / 196f }.second, 0.0001f)
    }

    @Test
    fun hiddenTabsDoNotLeaveAStrongBand() {
        val stops = homeTopReadabilityStops(80f, 80f, 80f, 112f, 16f, 0.24f, 0f)
        assertTrue(stops.all { it.second in 0f..0.24f })
        assertEquals(0f, stops.last().second, 0.0001f)
    }

    @Test
    fun shortCoverageRemainsContinuousInsteadOfBecomingAFullStrengthBand() {
        val original = ProgressiveBlur.Top.copy(endFraction = 0.5f, curve = 1.25f)
        val result = homeTopReadabilityBlurGradient(original, 0.75f)
        assertEquals(original.startFraction, result.startFraction, 0.0001f)
        assertEquals(0.5f, result.endFraction, 0.0001f)
        assertEquals(original.curve, result.curve, 0.0001f)
        assertTrue(result.endFraction > result.startFraction)
    }

    @Test
    fun fullCoverageClearsAtControlsInsteadOfExtendingIntoTheFeed() {
        val original = ProgressiveBlur.Top.copy(startFraction = 0f, endFraction = 1f)
        // 不同窗口密度及控件收起高度均按当前控件边界计算，标签顺序不参与曲线。
        for ((contentHeight, drawingHeight) in listOf(152f to 196f, 304f to 392f, 80f to 112f)) {
            val result = homeTopReadabilityBlurGradient(original, contentHeight / drawingHeight)
            assertEquals(0f, result.startFraction, 0.0001f)
            assertEquals(contentHeight, result.endFraction * drawingHeight, 0.0001f)
            assertEquals(original.curve, result.curve, 0.0001f)
        }
        assertEquals(1f, original.endFraction, 0.0001f)
    }

    @Test
    fun shortenedControlsKeepCustomStartBeforeTheClearEnd() {
        val result = homeTopReadabilityBlurGradient(
            ProgressiveBlur.Top.copy(startFraction = 0.8f, endFraction = 1f),
            contentBottomFraction = 0.4f,
        )
        assertEquals(0.4f, result.endFraction, 0.0001f)
        assertTrue(result.startFraction >= 0f && result.startFraction < result.endFraction)
    }
}
