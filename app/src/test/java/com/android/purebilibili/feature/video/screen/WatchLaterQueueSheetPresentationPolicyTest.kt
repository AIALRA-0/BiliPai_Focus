package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchLaterQueueSheetPresentationPolicyTest {

    @Test
    fun useInlineSheetWhenRealtimeHazeRequired() {
        assertEquals(
            ExternalPlaylistQueueSheetPresentation.INLINE_HAZE,
            resolveExternalPlaylistQueueSheetPresentation(requireRealtimeHaze = true)
        )
    }

    @Test
    fun canFallbackToModalWhenRealtimeHazeNotRequired() {
        assertEquals(
            ExternalPlaylistQueueSheetPresentation.MODAL,
            resolveExternalPlaylistQueueSheetPresentation(requireRealtimeHaze = false)
        )
    }

    @Test
    fun queueSheetConsumesBackToDismissBeforePageNavigation() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailOverlayHost.kt")
        val sheetSource = source
            .substringAfter("internal fun ExternalPlaylistQueueSheet(")
            .substringBefore("@Composable\ninternal fun ExternalPlaylistQueueSheetContent(")

        assertTrue(sheetSource.contains("BackHandler(enabled = visible)"))
        assertTrue(sheetSource.contains("onDismiss()"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
