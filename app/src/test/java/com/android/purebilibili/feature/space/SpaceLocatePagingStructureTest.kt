package com.android.purebilibili.feature.space

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpaceLocatePagingStructureTest {

    @Test
    fun `locate paging observes completion and stops after a failed request`() {
        val screen = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt")
        val viewModel = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceViewModel.kt")
        val locateEffectKeys = screen
            .substringAfter("LaunchedEffect(\n        state.pendingLocateBvid,")
            .substringBefore(") {")

        assertTrue(locateEffectKeys.contains("state.videoPageLoadCompletionVersion"))
        assertFalse(locateEffectKeys.contains("state.isLoadingMore"))
        assertTrue(screen.contains("SpaceLocateTargetPageAction.LoadFailed ->"))
        assertTrue(viewModel.contains("lastVideoPageLoadFailed = true"))
        assertTrue(viewModel.contains("reportPendingLocateBvidLoadFailed"))
        assertTrue(viewModel.contains("加载投稿失败，请稍后重试定位"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
