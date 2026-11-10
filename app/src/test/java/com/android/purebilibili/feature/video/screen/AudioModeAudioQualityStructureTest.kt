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

        // 音质入口收敛到顶栏右侧胶囊（GlassTextButton），菜单仍为共享的 AudioQualitySelectionMenu。
        assertTrue(source.contains("leadingActions"))
        assertTrue(source.contains("GlassTextButton("))
        assertTrue(source.contains("label = audioQualityLabel.ifBlank { \"音质\" }"))
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
