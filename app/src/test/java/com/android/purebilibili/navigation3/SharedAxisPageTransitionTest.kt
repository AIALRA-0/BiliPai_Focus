package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.sharedAxisIncomingAlpha
import com.android.purebilibili.navigation3.predictiveback.sharedAxisMovement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedAxisPageTransitionTest {
    @Test
    fun movementAndIncomingOpacityRemainBoundedAndReverseContinuously() {
        val samples = (0..100).map { it / 100f }
        val movements = samples.map(::sharedAxisMovement)
        val alphas = samples.map(::sharedAxisIncomingAlpha)
        assertEquals(0f, movements.first(), 0.0001f)
        assertEquals(1f, movements.last(), 0.0001f)
        assertEquals(0f, alphas.first(), 0.0001f)
        assertEquals(1f, alphas.last(), 0.0001f)
        assertTrue(movements.zipWithNext().all { (a, b) -> a <= b })
        assertTrue(alphas.zipWithNext().all { (a, b) -> a <= b })
        assertEquals(0f, sharedAxisIncomingAlpha(-1f), 0.0001f)
        assertEquals(1f, sharedAxisIncomingAlpha(2f), 0.0001f)
    }
}
