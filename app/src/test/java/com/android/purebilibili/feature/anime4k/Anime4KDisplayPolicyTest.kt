package com.android.purebilibili.feature.anime4k

import com.android.purebilibili.feature.anime4k.gl.Anime4KDisplayScaleMode
import com.android.purebilibili.feature.anime4k.gl.resolveAnime4KDisplayTransform
import kotlin.test.Test
import kotlin.test.assertEquals

class Anime4KDisplayPolicyTest {

    @Test
    fun `fit keeps 16 to 9 source inside ultrawide output`() {
        val transform = resolveAnime4KDisplayTransform(
            outputWidth = 2400,
            outputHeight = 1080,
            sourceWidth = 1920,
            sourceHeight = 1080,
            scaleMode = Anime4KDisplayScaleMode.FIT
        )

        assertEquals(0.8f, transform.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(1f, transform.scaleY)
    }

    @Test
    fun `crop covers ultrawide output without stretching source`() {
        val transform = resolveAnime4KDisplayTransform(
            outputWidth = 2400,
            outputHeight = 1080,
            sourceWidth = 1920,
            sourceHeight = 1080,
            scaleMode = Anime4KDisplayScaleMode.CROP
        )

        assertEquals(1f, transform.scaleX)
        assertEquals(1.25f, transform.scaleY, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `fit keeps vertical source inside landscape output`() {
        val transform = resolveAnime4KDisplayTransform(
            outputWidth = 1920,
            outputHeight = 1080,
            sourceWidth = 1080,
            sourceHeight = 1920,
            scaleMode = Anime4KDisplayScaleMode.FIT
        )

        assertEquals(0.31640625f, transform.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(1f, transform.scaleY)
    }

    @Test
    fun `stretch is the only mode using full output quad`() {
        val transform = resolveAnime4KDisplayTransform(
            outputWidth = 2400,
            outputHeight = 1080,
            sourceWidth = 1920,
            sourceHeight = 1080,
            scaleMode = Anime4KDisplayScaleMode.STRETCH
        )

        assertEquals(1f, transform.scaleX)
        assertEquals(1f, transform.scaleY)
    }
}
