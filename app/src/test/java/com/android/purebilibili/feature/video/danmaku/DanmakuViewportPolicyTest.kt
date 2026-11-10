package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuViewportPolicyTest {
    @Test
    fun `invalid geometry does not produce a placeholder viewport`() {
        assertNull(resolveDanmakuViewport(0, 608, 3f))
        assertNull(resolveDanmakuViewport(1080, 0, 3f))
        assertNull(resolveDanmakuViewport(1080, 608, 0f))
        assertNull(resolveDanmakuViewport(1080, 608, Float.NaN))
    }

    @Test
    fun `a band shorter than one row budgets no lines`() {
        val rowHeight = resolveDanmakuLayerLineHeightPx(fontSize = 45f, lineHeightMultiplier = 1.6f)
        assertEquals(
            0,
            resolveDanmakuVisibleLineCount(
                visibleHeightPx = rowHeight - 1f,
                areaRatioHint = 0.5f,
                fontSize = 45f,
                strokeWidth = 2f,
                strokeEnabled = true,
                lineHeight = 1.6f,
                massiveMode = false
            )
        )
    }

    @Test
    fun `a taller band never budgets fewer rows`() {
        fun rows(visibleHeightPx: Float) = resolveDanmakuVisibleLineCount(
            visibleHeightPx = visibleHeightPx,
            areaRatioHint = 0.5f,
            fontSize = 45f,
            strokeWidth = 2f,
            strokeEnabled = true,
            lineHeight = 1.6f,
            massiveMode = false
        )
        val rowHeight = resolveDanmakuLayerLineHeightPx(fontSize = 45f, lineHeightMultiplier = 1.6f)
        val tall = rows(rowHeight * 8f)
        val short = rows(rowHeight * 3f)
        assertTrue(tall >= short)
        assertTrue(short > 0)
    }

    @Test
    fun `native FIT reserves only the band above the actual picture not the full player host`() {
        val region = upperRegion()
        assertEquals(PortraitDanmakuUpperRegion(topPx = 96, heightPx = 800), region)
        assertTrue(requireNotNull(region).topPx + region.heightPx <= 896.25f)
    }

    @Test
    fun `glass upward video offset reduces the free band`() {
        assertEquals(
            PortraitDanmakuUpperRegion(topPx = 96, heightPx = 656),
            upperRegion(viewportHeight = 608, viewportOffset = -144f),
        )
    }

    @Test
    fun `fixed ratio frame includes its internal FIT letterbox`() {
        assertEquals(upperRegion(), upperRegion(viewportHeight = 810))
    }

    @Test
    fun `portrait full height cropped stretched and landscape pages have no invented upper area`() {
        assertNull(upperRegion(sourceWidth = 1080, sourceHeight = 1920))
        assertNull(upperRegion(fit = false))
        assertNull(upperRegion(containerWidth = 2400, containerHeight = 1080))
    }

    @Test
    fun `zoom and pan use the visible picture edge and fall back when less than one row fits`() {
        assertEquals(
            PortraitDanmakuUpperRegion(topPx = 96, heightPx = 496),
            upperRegion(scale = 2f),
        )
        assertEquals(
            PortraitDanmakuUpperRegion(topPx = 96, heightPx = 600),
            upperRegion(translationY = -200f),
        )
        assertNull(upperRegion(translationY = -750f))
        assertNull(upperRegion(scale = 4f))
    }

    @Test
    fun `unmeasured source or container does not create a placeholder band`() {
        assertNull(upperRegion(containerWidth = 0))
        assertNull(upperRegion(sourceWidth = 0, sourceHeight = 0))
    }

    private fun upperRegion(
        containerWidth: Int = 1080,
        containerHeight: Int = 2400,
        viewportHeight: Int = 2400,
        viewportOffset: Float = 0f,
        sourceWidth: Int = 1920,
        sourceHeight: Int = 1080,
        fit: Boolean = true,
        scale: Float = 1f,
        translationY: Float = 0f,
    ) = resolvePortraitDanmakuUpperRegion(
        containerWidthPx = containerWidth,
        containerHeightPx = containerHeight,
        videoViewportWidthPx = 1080,
        videoViewportHeightPx = viewportHeight,
        videoWidth = sourceWidth,
        videoHeight = sourceHeight,
        fitVideoInsideViewport = fit,
        viewportOffsetYPx = viewportOffset,
        safeTopPx = 96,
        minimumHeightPx = 72f,
        videoScale = scale,
        videoTranslationYPx = translationY,
    )
}
