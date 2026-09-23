package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabletPortraitFullscreenStructureTest {
    @Test
    fun `tablet players dispatch portrait fullscreen to the screen presentation state`() {
        val largeScreen = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/LargeScreenVideoLayout.kt"
        ).readText()
        val foldable = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        ).readText()
        val screen = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        ).readText()

        listOf(largeScreen, foldable).forEach { source ->
            assertTrue(source.contains("onPortraitFullscreen: () -> Unit"))
            assertTrue(source.contains("onPortraitFullscreen = onPortraitFullscreen"))
            assertFalse(source.contains("onPortraitFullscreen = { playerState.setPortraitFullscreen(true) }"))
        }
        assertTrue(
            screen.split("onPortraitFullscreen = { enterPortraitFullscreen() }").size - 1 >= 2
        )
    }
}
