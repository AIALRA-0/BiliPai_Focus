package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipAspectRatioPolicyTest {

    @Test
    fun `standard 16 to 9 video resolves to 16 to 9`() {
        val rational = resolveSafePipRational(1920, 1080)
        assertEquals(16, rational.numerator)
        assertEquals(9, rational.denominator)
        assertEquals(16f / 9f, rational.ratio, 0.001f)
    }

    @Test
    fun `vertical 9 to 16 video resolves to 9 to 16`() {
        val rational = resolveSafePipRational(1080, 1920)
        assertEquals(9, rational.numerator)
        assertEquals(16, rational.denominator)
        assertEquals(9f / 16f, rational.ratio, 0.001f)
        assertTrue(rational.ratio in PIP_MIN_ASPECT_RATIO..PIP_MAX_ASPECT_RATIO)
    }

    @Test
    fun `classic 4 to 3 video resolves to 4 to 3`() {
        val rational = resolveSafePipRational(1440, 1080)
        assertEquals(4, rational.numerator)
        assertEquals(3, rational.denominator)
        assertEquals(4f / 3f, rational.ratio, 0.001f)
    }

    @Test
    fun `square 1 to 1 video resolves to 1 to 1`() {
        val rational = resolveSafePipRational(1080, 1080)
        assertEquals(1, rational.numerator)
        assertEquals(1, rational.denominator)
        assertEquals(1f, rational.ratio, 0.001f)
    }

    @Test
    fun `ultra wide 32 to 9 clamps to max PIP limit`() {
        val rational = resolveSafePipRational(3840, 1080) // 3.55 > 2.39
        assertEquals(239, rational.numerator)
        assertEquals(100, rational.denominator)
        assertEquals(2.39f, rational.ratio, 0.001f)
    }

    @Test
    fun `ultra tall 9 to 24 clamps to min PIP limit`() {
        val rational = resolveSafePipRational(540, 1920) // 0.28 < 0.418
        assertEquals(100, rational.numerator)
        assertEquals(239, rational.denominator)
        assertEquals(100f / 239f, rational.ratio, 0.001f)
    }

    @Test
    fun `uninitialized zero dimensions safely fallback to 16 to 9`() {
        val rational = resolveSafePipRational(0, 0)
        assertEquals(16, rational.numerator)
        assertEquals(9, rational.denominator)
    }
}
