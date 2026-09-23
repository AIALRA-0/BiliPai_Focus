package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNavigationBackHandlerStructureTest {

    @Test
    fun fullscreenPlayersUseNavigationEventBackHandling() {
        val paths = listOf(
            "feature/bangumi/BangumiPlayerScreen.kt",
            "feature/live/LivePlayerScreen.kt",
            "feature/download/OfflineVideoPlayerScreen.kt",
        )

        paths.forEach { relativePath ->
            val source = File("src/main/java/com/android/purebilibili/$relativePath").readText()
            assertTrue(source.contains("LocalNavigationBackHandler("), relativePath)
            assertFalse(source.contains("import androidx.activity.compose.BackHandler"), relativePath)
        }
    }

    @Test
    fun localNavigationBackHandlerRespectsGlobalPredictivePreference() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/ui/LocalNavigationBackHandler.kt"
        ).readText()

        assertTrue(source.contains("LocalPredictiveBackGestureEnabled.current"))
        assertTrue(source.contains("reportPredictiveProgress = predictiveBackGestureEnabled"))
    }
}
