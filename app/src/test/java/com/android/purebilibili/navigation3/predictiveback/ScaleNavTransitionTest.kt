package com.android.purebilibili.navigation3.predictiveback

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleNavTransitionTest {
    @Test
    fun `gesture card follows touch vertically and rotates around the touched edge`() {
        val left = resolveScaleGestureCardTransform(
            progress = 0.6f,
            touchY = 760f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 84f,
        )
        val right = resolveScaleGestureCardTransform(
            progress = 0.6f,
            touchY = 760f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = false,
            maxVerticalTravelPx = 84f,
        )

        assertTrue(left.translationX > 0f)
        assertTrue(left.translationY > 0f)
        assertTrue(left.rotationZ > 0f)
        assertEquals(-left.translationX, right.translationX, 0.001f)
        assertEquals(-left.rotationZ, right.rotationZ, 0.001f)
        assertEquals(0.8f, left.pivotFractionX)
        assertEquals(0.2f, right.pivotFractionX)
        assertEquals(left.pivotFractionY, right.pivotFractionY)
    }

    @Test
    fun `gesture card vertical travel and pivot stay within safe bounds`() {
        val transform = resolveScaleGestureCardTransform(
            progress = 1f,
            touchY = 4000f,
            initialTouchY = 0f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 84f,
        )

        assertTrue(abs(transform.translationY) <= 84f)
        assertTrue(abs(transform.rotationZ) <= 2.25f)
        assertEquals(0.9f, transform.pivotFractionY)
    }

    @Test
    fun `gesture pose fades continuously during settle`() {
        val released = resolveScaleGestureCardTransform(
            progress = 0.7f,
            touchY = 800f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 84f,
            settleWeight = 1f,
        )
        val settled = resolveScaleGestureCardTransform(
            progress = 0.7f,
            touchY = 800f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 84f,
            settleWeight = 0f,
        )

        assertTrue(abs(released.rotationZ) > 0f)
        assertEquals(0f, settled.translationX)
        assertEquals(0f, settled.translationY)
        assertEquals(0f, settled.rotationZ)
    }
}
