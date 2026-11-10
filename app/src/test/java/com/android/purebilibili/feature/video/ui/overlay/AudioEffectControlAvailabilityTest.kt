package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioEffectControlAvailabilityTest {
    @Test
    fun staleOrReleasedCallbacksNeverReadTheOldEffect() {
        val old = Any()
        val current = Any()
        assertNull(resolveAudioEffectControlAvailability(current, old, true) { error("stale read") })
        assertNull(resolveAudioEffectControlAvailability(null, old, true) { error("released read") })
    }

    @Test
    fun lossOfControlAndReleaseDuringTheReadAreUnavailable() {
        val effect = Any()
        assertEquals(false, resolveAudioEffectControlAvailability(effect, effect, false) { error("unexpected read") })
        assertEquals(false, resolveAudioEffectControlAvailability(effect, effect, true) {
            throw IllegalStateException("uninitialized AudioEffect")
        })
    }

    @Test
    fun activeCallbackReportsTheEffectState() {
        val effect = Any()
        assertTrue(resolveAudioEffectControlAvailability(effect, effect, true) { true } == true)
        assertFalse(resolveAudioEffectControlAvailability(effect, effect, true) { false } == true)
    }
}
