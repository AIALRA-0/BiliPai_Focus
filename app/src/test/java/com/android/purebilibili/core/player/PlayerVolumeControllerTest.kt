package com.android.purebilibili.core.player

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class PlayerVolumeControllerTest {

    @Test
    fun `normal playback does not apply legacy player attenuation`() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/player/PlayerVolumeController.kt"
        ).readText()

        assertFalse(source.contains("PlayerSettingsStore"))
        assertFalse(source.contains("NetworkModule"))
        assertEquals(1f, PlayerVolumeController.preferredVolumeSync())
    }
}
