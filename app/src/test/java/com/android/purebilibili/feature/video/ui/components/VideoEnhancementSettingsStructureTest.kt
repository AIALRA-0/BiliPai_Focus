package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VideoEnhancementSettingsStructureTest {

    @Test
    fun `竖屏设置提供算法模型与FSR锐化细节`() {
        val panelSource = source("VideoSettingsPanel.kt")
        val sharedSource = source("Anime4KSettingsUi.kt")

        assertTrue(panelSource.contains("VideoEnhancementAlgorithmOptions("))
        assertTrue(panelSource.contains("onAlgorithmChange = onVideoEnhancementAlgorithmChange"))
        assertTrue(panelSource.contains("Anime4KPresetOptions("))
        assertTrue(panelSource.contains("FsrSharpnessOptions("))
        assertTrue(panelSource.contains("onSharpnessChange = onFsrSharpnessChange"))
        assertTrue(sharedSource.contains("VideoEnhancementAlgorithm.entries.forEach"))
        assertTrue(sharedSource.contains("text = \"FSR 锐化\""))
        assertTrue(sharedSource.contains("onValueChange = onSharpnessChange"))
        assertTrue(sharedSource.contains("steps = FSR_SHARPNESS_SLIDER_STEPS"))
    }

    @Test
    fun `横屏与竖屏FSR锐化均使用零点一档位`() {
        val landscapeSource = overlaySource("BottomControlBar.kt")
        val portraitSource = source("Anime4KSettingsUi.kt")

        assertTrue(landscapeSource.contains("steps = FSR_SHARPNESS_SLIDER_STEPS"))
        assertTrue(portraitSource.contains("steps = FSR_SHARPNESS_SLIDER_STEPS"))
    }

    @Test
    fun `插件设置页FSR锐化也使用零点一档位`() {
        val pluginSource = pluginSource("Anime4KPlugin.kt")

        assertTrue(pluginSource.contains("override val version: String = \"0.4.0\""))
        assertTrue(pluginSource.contains("title = \"FSR 锐化强度\""))
        assertTrue(pluginSource.contains("steps = FSR_SHARPNESS_SLIDER_STEPS"))
    }

    private fun source(fileName: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/video/ui/components/$fileName"),
        File("src/main/java/com/android/purebilibili/feature/video/ui/components/$fileName")
    ).first { it.exists() }.readText()

    private fun overlaySource(fileName: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/$fileName"),
        File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/$fileName")
    ).first { it.exists() }.readText()

    private fun pluginSource(fileName: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/feature/plugin/$fileName"),
        File("src/main/java/com/android/purebilibili/feature/plugin/$fileName")
    ).first { it.exists() }.readText()
}
