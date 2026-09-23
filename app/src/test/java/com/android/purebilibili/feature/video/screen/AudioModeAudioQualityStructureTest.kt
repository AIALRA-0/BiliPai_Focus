package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AudioModeAudioQualityStructureTest {

    @Test
    fun `audio mode forwards current audio quality state and switch action`() {
        val source = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/screen/AudioModeMusicPlayer.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/AudioModeMusicPlayer.kt"
        )

        assertTrue(source.contains("resolveAudioQualityControlPresentation("))
        assertTrue(source.contains("audioQualityOptions = successState.availableAudioQualities"))
        assertTrue(source.contains("requestedAudioQuality = successState.requestedAudioQuality"))
        assertTrue(source.contains("onAudioQualitySelected = viewModel::setAudioQuality"))
    }

    @Test
    fun `music player page exposes direct audio quality control and shared menu`() {
        val source = loadSource(
            "src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt",
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt"
        )

        assertTrue(source.contains("MusicAudioQualityControl("))
        assertTrue(source.contains(".heightIn(min = 48.dp)"))
        assertTrue(source.contains("HiResBadge()"))
        assertTrue(source.contains("DolbyBadge()"))
        assertTrue(source.contains("AudioQualitySelectionMenu("))
        assertTrue(source.contains("options = audioQualityOptions"))
        assertTrue(source.contains("requestedAudioQuality = requestedAudioQuality"))
        assertTrue(source.contains("onAudioQualitySelected(quality)"))
        assertTrue(source.contains("showAudioQuality = false"))
    }

    private fun loadSource(vararg paths: String): String {
        val sourceFile = paths.map(::File).firstOrNull { it.exists() }
            ?: error("Cannot locate source from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
