package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailShareSheetStructureTest {

    @Test
    fun ordinaryVideoDetailShareEntrypoints_openShareSheetWithStylePicker() {
        val source = loadVideoDetailSource()
        val phoneContentSource = loadVideoDetailPhoneContentSource()
        val overlayAdapterSource = loadVideoDetailCommonOverlayAdapterSource()

        assertTrue(source.contains("VideoDetailCommonOverlayAdapter("))
        assertFalse(
            source.contains("VideoShareSheet("),
            "VideoDetailScreenStateHolder should keep the share sheet outside the state holder",
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
            detailActionShare.contains("pendingVideoShare =") &&
                detailActionShare.contains("buildVideoSharePayload"),
            "Detail action row share should open the in-app share sheet with a unified payload"
        )
        assertTrue(
            detailActionShare.contains("coverUrl = success.info.pic"),
            "Detail action row share should include the current video cover"
        )
        assertTrue(
            detailActionShare.contains("upName = success.info.owner.name"),
            "Detail action row share should include the uploader name for card mode"
        )
        assertTrue(
            bottomInputShare.contains("pendingVideoShare =") &&
                bottomInputShare.contains("buildVideoSharePayload"),
            "Bottom input bar share should open the in-app share sheet with a unified payload"
        )
        assertTrue(
            bottomInputShare.contains("coverUrl = success.info.pic"),
            "Bottom input bar share should include the current video cover"
        )
        assertTrue(
            phoneContentSource.contains("VideoShareSheet("),
            "Phone detail content should host VideoShareSheet for share-style selection"
        )
        assertTrue(
            phoneContentSource.contains("onDismiss = { pendingVideoShare = null }"),
            "Share sheet dismiss should clear the pending payload"
        )
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
