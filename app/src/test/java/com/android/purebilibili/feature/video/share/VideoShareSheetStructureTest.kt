package com.android.purebilibili.feature.video.share

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VideoShareSheetStructureTest {

    @Test
    fun shareSheet_exposesLinkAndCardStylePicker() {
        val source = loadVideoShareSheetSource()

        assertTrue(
            source.contains("VideoShareStyle.LINK"),
            "Share sheet should offer the legacy link style"
        )
        assertTrue(
            source.contains("VideoShareStyle.CARD"),
            "Share sheet should offer the card style"
        )
        assertTrue(
            source.contains("AppNativeSegmentedControl"),
            "Share style should be selectable before choosing a target"
        )
        assertTrue(
            source.contains("prepareVideoShareCardFile"),
            "Card style should synthesize a share card image"
        )
        assertTrue(
            source.contains("prepareVideoShareMedia"),
            "Share sheet should resolve media by the selected style"
        )
    }

    @Test
    fun moreSharePath_preparesMediaBeforeOpeningSystemChooser() {
        val source = loadVideoShareSheetSource()
        val moreBranch = source
            .substringAfter("VideoShareTarget.MORE -> {")
            .substringBefore("VideoShareSheetItemView")
        val startMoreFunction = source
            .substringAfter("private fun Context.startMoreVideoShare")
            .substringBefore("private fun Context.startActivityWithTaskFlag")

        assertTrue(
            moreBranch.contains("prepareVideoShareMedia"),
            "More share should prepare style-aware media before opening the system sharesheet"
        )
        assertTrue(
            moreBranch.contains("hideSharePanel()"),
            "Share sheet must hide before programmatic dismiss so it can reopen later"
        )
        assertTrue(
            startMoreFunction.contains("shareMedia: VideoShareCoverFile?"),
            "More share should receive prepared share media"
        )
        assertTrue(
            startMoreFunction.contains("buildVideoCoverShareIntent"),
            "More share should use image intent when card media is available"
        )
        assertTrue(
            startMoreFunction.contains("buildVideoShareIntent(payload)"),
            "More share should fall back to link text intent"
        )
    }

    @Test
    fun targetedShare_resolvesFriendComponentWithoutMiniWindowLaunch() {
        val source = loadVideoShareSheetSource()
        val startTargetedFunction = source
            .substringAfter("private fun Context.startTargetedVideoShare")
            .substringBefore("private fun Context.startMoreVideoShare")

        assertTrue(
            source.contains("resolveShareActivityClassName"),
            "Targeted WeChat/QQ share should resolve a single friend-share activity"
        )
        assertTrue(
            source.contains("resolvePreferredShareActivity"),
            "Targeted WeChat/QQ share should prefer friend-share entries over favorites/tools"
        )
        assertTrue(
            startTargetedFunction.contains("activityClassName = activityClassName"),
            "Targeted share should pass the resolved activity class into the share intent"
        )
        assertTrue(
            startTargetedFunction.contains("startActivityWithTaskFlag(intent)"),
            "Targeted share should use ordinary activity start"
        )
        assertTrue(
            !source.contains("startActivityAsShareMiniWindow"),
            "Mini-window launch helper should stay removed"
        )
        assertTrue(
            !source.contains("createShareMiniWindowOptions"),
            "Freeform launch options should stay removed"
        )
        assertTrue(
            !source.contains("SHARE_LAUNCH_WINDOWING_MODE_FREEFORM"),
            "Freeform windowing mode should stay removed"
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
