package com.android.purebilibili.feature.video.share

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VideoShareSheetStructureTest {

    @Test
    fun moreSharePath_preparesCoverBeforeOpeningSystemChooser() {
        val source = loadVideoShareSheetSource()
        val moreBranch = source
            .substringAfter("VideoShareTarget.MORE -> {")
            .substringBefore("}")
        val startMoreFunction = source
            .substringAfter("private fun Context.startMoreVideoShare")
            .substringBefore("private fun Context.startActivityWithTaskFlag")

        assertTrue(
            moreBranch.contains("prepareVideoShareCoverFile"),
            "More share should prepare cover before opening the system sharesheet"
        )
        assertTrue(
            startMoreFunction.contains("coverFile: VideoShareCoverFile?"),
            "More share should receive a prepared cover file"
        )
        assertTrue(
            startMoreFunction.contains("buildVideoCoverShareIntent"),
            "More share should use cover image intent when cover is available"
        )
    }

    private fun loadVideoShareSheetSource(): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/share/VideoShareSheet.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/video/share/VideoShareSheet.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate VideoShareSheet.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
