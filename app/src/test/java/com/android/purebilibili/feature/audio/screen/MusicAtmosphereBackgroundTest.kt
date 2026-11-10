package com.android.purebilibili.feature.audio.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicAtmosphereBackgroundTest {
    @Test
    fun transientsDoNotFlashAndSilenceSettlesWithoutAnArtificialBeat() {
        val transient = resolveMusicAtmosphereEnergy(0f, 1f, 50f)
        assertTrue(transient in 0f..0.13f)
        var energy = 1f
        repeat(160) {
            val next = resolveMusicAtmosphereEnergy(energy, 0f, 50f)
            assertTrue(next <= energy)
            energy = next
        }
        assertEquals(0f, energy)
        assertEquals(0f, resolveMusicAtmosphereEnergy(0f, 0f, 50f))
        assertEquals(0f, resolveMusicAtmosphereEnergy(Float.NaN, Float.NaN, 50f))
    }
}
