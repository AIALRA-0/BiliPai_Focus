package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailShareSheetStructureTest {

    @Test
    fun ordinaryVideoDetailShareEntrypoints_openSystemShareChooser() {
        val source = loadVideoDetailSource()
        val phoneContentSource = loadVideoDetailPhoneContentSource()
        val overlayAdapterSource = loadVideoDetailCommonOverlayAdapterSource()

        assertTrue(source.contains("VideoDetailCommonOverlayAdapter("))
        assertTrue(source.contains("ShareUtils.shareVideo("))
        assertTrue(source.contains("title = payload.title"))
        assertTrue(source.contains("bvid = payload.bvid"))
        assertFalse(source.contains("pendingVideoShare"))
        assertFalse(
            source.contains("VideoShareSheet("),
            "VideoDetailScreenStateHolder should open the system chooser directly",
        )
        assertFalse(overlayAdapterSource.contains("VideoShareSheet("))
        assertFalse(overlayAdapterSource.contains("pendingVideoShare"))

        val detailActionShare = phoneContentSource
            .substringAfter("onDownloadClick = playbackActions.openDownloadDialog")
            .substringBefore("onTimestampClick = {")
        val bottomInputShare = phoneContentSource
            .substringAfter("BottomInputBar(")
            .substringBefore("onCommentClick = {")

        assertTrue(
            detailActionShare.contains("onShareVideo(") &&
                detailActionShare.contains("buildVideoSharePayload"),
            "Detail action row share should emit unified share payload"
        )
        assertTrue(
            detailActionShare.contains("coverUrl = success.info.pic"),
            "Detail action row share should include the current video cover"
        )
        assertTrue(
            bottomInputShare.contains("onShareVideo(") &&
                bottomInputShare.contains("buildVideoSharePayload"),
            "Bottom input bar share should emit unified share payload"
        )
        assertTrue(
            bottomInputShare.contains("coverUrl = success.info.pic"),
            "Bottom input bar share should include the current video cover"
        )
        assertTrue(source.contains("ShareUtils.shareVideo("))
    }

    private fun loadVideoDetailSource(): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate VideoDetailScreen.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }

    private fun loadVideoDetailPhoneContentSource(): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate VideoDetailPhoneContent.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }

    private fun loadVideoDetailCommonOverlayAdapterSource(): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailCommonOverlayAdapter.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailCommonOverlayAdapter.kt"),
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate VideoDetailCommonOverlayAdapter.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
