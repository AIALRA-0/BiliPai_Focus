package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailMiniPlayerSyncStructureTest {

    @Test
    fun videoInfoSyncHappensBeforeBackgroundUiStateCache() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
        val miniPlayerSyncBlock = source
            .substringAfter("val shouldCacheMiniPlayer = lastCachedMiniPlayerBvid != currentBvid")
            .substringBefore("} else if (miniPlayerManager == null)")

        val setVideoInfoIndex = miniPlayerSyncBlock.indexOf("miniPlayerManager.setVideoInfo(")
        val backgroundLaunchIndex = miniPlayerSyncBlock.indexOf("launch(Dispatchers.Default)")
        val cacheUiStateIndex = miniPlayerSyncBlock.indexOf("miniPlayerManager.cacheUiState(success)")

        assertTrue(setVideoInfoIndex >= 0)
        assertTrue(backgroundLaunchIndex >= 0)
        assertTrue(cacheUiStateIndex >= 0)
        assertTrue(setVideoInfoIndex < backgroundLaunchIndex)
        assertTrue(backgroundLaunchIndex < cacheUiStateIndex)
        assertFalse(miniPlayerSyncBlock.contains("withContext(Dispatchers.Main)"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
