package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioQualityVisibilityStructureTest {

    @Test
    fun `landscape audio quality control is not gated by available option count`() {
        val source = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt"
        )
        val controlBarSource = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt"
        )

        assertFalse(source.contains("availableAudioQualities.count { option ->"))
        assertTrue(source.contains("resolveAudioQualityControlPresentation("))
        assertFalse(controlBarSource.contains("if (currentAudioQualityLabel.isNotEmpty())"))
        assertTrue(controlBarSource.contains("currentAudioQualityLabel.ifBlank { \"音质\" }"))
    }

    @Test
    fun `portrait audio quality control is always rendered`() {
        val source = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitFullscreenOverlay.kt"
        )

        assertFalse(source.contains("showAudioQualityChip"))
        assertTrue(source.contains("label = currentAudioQualityLabel"))
    }

    @Test
    fun `advanced settings always contains audio quality section`() {
        val source = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/VideoSettingsPanel.kt"
        )

        assertFalse(source.contains("availableAudioQualities.count { it.preferenceId != -1 }"))
        assertTrue(source.contains("text = \"音频音质\""))
    }

    @Test
    fun `portrait video player opens audio quality menu in a full screen dialog`() {
        val overlaySource = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt"
        )
        val menuSource = loadSource(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/AudioQualitySelectionMenu.kt"
        )
        val audioMenuHost = overlaySource
            .substringAfter("if (showAudioQualityMenu)")
            .substringBefore("// --- 7.")

        assertTrue(audioMenuHost.contains("AudioQualitySelectionMenuDialog("))
        assertTrue(menuSource.contains("DialogProperties(usePlatformDefaultWidth = false)"))
    }

    private fun loadSource(relativePath: String): String {
        val moduleRelative = File(relativePath)
        val repositoryRelative = File("app/$relativePath")
        return listOf(moduleRelative, repositoryRelative)
            .first { it.exists() }
            .readText()
    }
}
