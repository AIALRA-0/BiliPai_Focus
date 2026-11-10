package com.android.purebilibili.feature.video.ambient

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmbientFramePolicyTest {
    @Test fun samplePreservesLandscapePortraitAndSquareAspect() {
        assertEquals(96 to 54, ambientSampleSize(1920, 1080))
        assertEquals(54 to 96, ambientSampleSize(1080, 1920))
        assertEquals(96 to 96, ambientSampleSize(1, 1))
        assertEquals(1 to 96, ambientSampleSize(1, 10000))
    }

    @Test fun defaultsAndIntervalsKeepLegacySamplingAndLimitGlow() {
        assertFalse(AmbientSettings().enabled)
        assertEquals(0.30f, AmbientSettings().opacity)
        // 模糊条在动态画面下及时更新，静态或省电时降低频率。
        assertEquals(125L, ambientIntervalMs(false, false, false))
        assertEquals(250L, ambientIntervalMs(false, false, true))
        assertEquals(250L, ambientIntervalMs(false, true, false))
        assertEquals(125L, ambientIntervalMs(true, false, false))
        assertEquals(250L, ambientIntervalMs(true, true, false))
        assertEquals(250L, ambientIntervalMs(true, false, true))
        assertEquals(500L, ambientIntervalMs(true, true, true))
        assertFalse(shouldSampleAmbientFrame(false, 2, 2))
        assertTrue(shouldSampleAmbientFrame(false, 3, 2))
    }

    @Test fun largeChangesUseShortTransitionsWithoutHardSwitching() {
        assertEquals(120L, ambientTransitionMs(0.005f))
        assertEquals(80L, ambientTransitionMs(0.12f))
        assertEquals(48L, ambientTransitionMs(0.40f))
        val analyzer = AmbientFrameAnalyzer()
        val dark = IntArray(96 * 54) { 0xff000000.toInt() }
        analyzer.analyze(dark, 96, 54, 0)
        assertEquals(120L, analyzer.transitionMs)
        analyzer.analyze(IntArray(dark.size) { 0xffffffff.toInt() }, 96, 54, 125)
        assertEquals(48L, analyzer.transitionMs)
        analyzer.analyze(IntArray(dark.size) { 0xffffffff.toInt() }, 96, 54, 250)
        assertEquals(120L, analyzer.transitionMs)
    }

    @Test fun barsRequireThreeDetectionsAndDarkSceneCannotChangeCrop() {
        val analyzer = AmbientFrameAnalyzer()
        val width = 96; val height = 54
        val pixels = IntArray(width * height) { if (it / width < 8 || it / width >= 46) 0xff000000.toInt() else 0xffffffff.toInt() }
        val full = AmbientCrop(0, 0, width, height)
        assertEquals(full, analyzer.analyze(pixels, width, height, 0))
        assertEquals(full, analyzer.analyze(pixels, width, height, 1000))
        val cropped = AmbientCrop(0, 8, width, 46)
        assertEquals(cropped, analyzer.analyze(pixels, width, height, 2000))
        assertEquals(cropped, analyzer.analyze(IntArray(pixels.size) { 0xff000000.toInt() }, width, height, 3000))
    }

    @Test fun staticDetectionRecoversImmediatelyOnMotion() {
        val analyzer = AmbientFrameAnalyzer()
        val pixels = IntArray(96 * 54) { 0xff777777.toInt() }
        analyzer.analyze(pixels, 96, 54, 0)
        analyzer.analyze(pixels, 96, 54, 125)
        analyzer.analyze(pixels, 96, 54, 3125)
        assertTrue(analyzer.static)
        analyzer.analyze(IntArray(pixels.size) { 0xffffffff.toInt() }, 96, 54, 3250)
        assertFalse(analyzer.static)
    }

    @Test fun blurKeepsSpatialColorAndRemovesEmbeddedBlackEdges() {
        val analyzer = AmbientFrameAnalyzer()
        val w = 96; val h = 54
        val pixels = IntArray(w * h) { i ->
            when { i / w < 8 -> 0xff000000.toInt(); i % w < w / 2 -> 0xff0000ff.toInt(); else -> 0xffff8800.toInt() }
        }
        val blurred = analyzer.blur(pixels, w, h, AmbientCrop(0, 8, w, h))
        assertEquals(0xff0000ff.toInt(), blurred[10])
        assertEquals(0xffff8800.toInt(), blurred[85])
    }
}
