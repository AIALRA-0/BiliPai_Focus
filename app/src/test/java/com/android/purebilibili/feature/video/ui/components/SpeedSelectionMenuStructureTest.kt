package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SpeedSelectionMenuStructureTest {

    @Test
    fun fullscreenSpeedMenu_usesRightSideOfSharedPopupWithoutDimmingVideo() {
        val menuSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/QualityMenu.kt"
        ).readText()
        val fullscreenSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/FullscreenPlayerOverlay.kt"
        ).readText()
        val playerOverlaySource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt"
        ).readText()
        val popupSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/PlayerMiuixListPopup.kt"
        ).readText()

        assertTrue(menuSource.contains("enum class SpeedSelectionMenuPlacement"))
        assertTrue(menuSource.contains("placement == SpeedSelectionMenuPlacement.RIGHT_SIDE"))
        assertTrue(menuSource.contains("PlayerListPopupPlacement.END"))
        val alignmentMapping = popupSource.substringAfter("val alignment = when (placement)")
            .substringBefore("val horizontalMargin =")
        assertTrue(alignmentMapping.contains("PlayerListPopupPlacement.END"))
        assertTrue(alignmentMapping.contains("PopupPositionProvider.Align.End"))
        assertTrue(popupSource.contains("enableWindowDim = placement == PlayerListPopupPlacement.CENTER"))
        assertTrue(fullscreenSource.contains("placement = SpeedSelectionMenuPlacement.RIGHT_SIDE"))
        assertTrue(playerOverlaySource.contains("placement = if (isFullscreen)"))
        assertTrue(playerOverlaySource.contains("SpeedSelectionMenuPlacement.RIGHT_SIDE"))
    }

    @Test
    fun speedMenu_showsHigherSpeedsFirst() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/QualityMenu.kt"
        ).readText()

        assertTrue(source.contains("val options = PlaybackSpeed.OPTIONS.asReversed()"))
    }
}
