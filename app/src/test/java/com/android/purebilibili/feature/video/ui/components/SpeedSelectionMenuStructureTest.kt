package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SpeedSelectionMenuStructureTest {

    @Test
    fun fullscreenSpeedMenu_usesRightPanelWithoutBlockingVideo() {
        val menuSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/QualityMenu.kt"
        ).readText()
        val fullscreenSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/FullscreenPlayerOverlay.kt"
        ).readText()
        val playerOverlaySource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt"
        ).readText()

        assertTrue(menuSource.contains("enum class SpeedSelectionMenuPlacement"))
        assertTrue(menuSource.contains("SpeedSelectionMenuPlacement.RIGHT_SIDE -> Alignment.CenterEnd"))
        assertTrue(menuSource.contains("SpeedSelectionMenuPlacement.RIGHT_SIDE -> Color.Transparent"))
        assertTrue(menuSource.contains(".padding(end = 24.dp)"))
        assertTrue(fullscreenSource.contains("placement = SpeedSelectionMenuPlacement.RIGHT_SIDE"))
        assertTrue(playerOverlaySource.contains("placement = if (isFullscreen)"))
        assertTrue(playerOverlaySource.contains("SpeedSelectionMenuPlacement.RIGHT_SIDE"))
    }

    @Test
    fun speedMenu_showsHigherSpeedsFirst() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/QualityMenu.kt"
        ).readText()

        assertTrue(source.contains("val speedOptions = PlaybackSpeed.OPTIONS.asReversed()"))
    }
}
