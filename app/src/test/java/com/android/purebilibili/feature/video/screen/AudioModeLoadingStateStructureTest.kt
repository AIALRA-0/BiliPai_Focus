package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AudioModeLoadingStateStructureTest {

    @Test
    fun `audio mode loading and error states keep recovery actions`() {
        val source = loadSource()
        val screen = source.substringAfter("fun AudioModeScreen(")

        assertTrue(screen.contains("VideoPlaybackUiState.Error"))
        assertTrue(screen.contains("viewModel.retry()"))
        assertTrue(screen.contains("onBack = onBack"))
        assertTrue(screen.contains("正在加载音频"))
        assertTrue(screen.contains("重试"))
    }

    private fun loadSource(
        path: String = "app/src/main/java/com/android/purebilibili/feature/video/screen/AudioModeScreen.kt"
    ): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
