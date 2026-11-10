package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileBackgroundMotionTest {
    @Test
    fun `scroll clips the header without recropping the wallpaper plane`() {
        val start = resolveProfileBackgroundFrame(336f, 0f, 0f, 800)
        val scrolled = resolveProfileBackgroundFrame(336f, 0f, 120f, 800)
        assertEquals(start.sourceHeight, scrolled.sourceHeight)
        assertEquals(216, scrolled.visibleHeight)
        assertEquals(0, resolveProfileBackgroundFrame(336f, 0f, Float.POSITIVE_INFINITY, 800).visibleHeight)
    }

    @Test
    fun `pull and rebound extend the image plane and visible header together`() {
        assertEquals(ProfileBackgroundFrame(416, 416), resolveProfileBackgroundFrame(336f, 80f, 0f, 800))
        assertEquals(ProfileBackgroundFrame(336, 336), resolveProfileBackgroundFrame(336f, 0f, 0f, 800))
        assertEquals(ProfileBackgroundFrame(400, 400), resolveProfileBackgroundFrame(336f, 160f, 0f, 400))
    }
}
