package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveAvatarContainerMotionPolicyTest {
    @Test
    fun avatarAndCompanionsNeverGrowIntoPageSizedContent() {
        // Phone, narrow split and tablet containers can be many times wider than an avatar.
        for (avatarWidth in listOf(40f, 56f, 80f)) {
            for (pageWidth in listOf(280f, 400f, 840f, 1200f)) {
                var lastAlpha = 1f
                for (step in 0..200) {
                    val scale = 1f + (pageWidth / avatarWidth - 1f) * step / 200f
                    val frame = resolveLiveAvatarContainerContentFrame(scale)
                    assertTrue(frame.sourceScale <= 1.2f)
                    assertTrue(frame.sourceAlpha in 0f..1f)
                    assertTrue(frame.sourceAlpha <= lastAlpha)
                    assertEquals(1f, frame.sourceAlpha + frame.contentAlpha, 0.0001f)
                    lastAlpha = frame.sourceAlpha
                }
                assertEquals(0f, lastAlpha)
            }
        }
    }

    @Test
    fun returnAndCancellationSeekTheSameContentHandoff() {
        val scales = listOf(1f, 1.08f, 1.2f, 1.4f, 1.6f, 4f)
        val opening = scales.map(::resolveLiveAvatarContainerContentFrame)
        val returning = scales.reversed().map(::resolveLiveAvatarContainerContentFrame)
        assertEquals(opening, returning.reversed())
        assertEquals(1f, opening.first().sourceAlpha)
        assertEquals(0f, opening.first().contentAlpha)
        assertEquals(0f, opening.last().sourceAlpha)
        assertEquals(1f, opening.last().contentAlpha)
        // A spring can compress the landing avatar slightly, without revealing detail pixels.
        val landing = resolveLiveAvatarContainerContentFrame(0.97f)
        assertEquals(0.97f, landing.sourceScale)
        assertEquals(1f, landing.sourceAlpha)
    }

    @Test
    fun handoffHasGentleEndpointsAndInvalidGeometryRevealsThePage() {
        assertTrue(1f - resolveLiveAvatarContainerContentFrame(1.081f).sourceAlpha < 0.001f)
        assertTrue(resolveLiveAvatarContainerContentFrame(1.599f).sourceAlpha < 0.001f)
        for (scale in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val frame = resolveLiveAvatarContainerContentFrame(scale)
            assertEquals(0f, frame.sourceAlpha)
            assertEquals(1f, frame.contentAlpha)
        }
    }
}
