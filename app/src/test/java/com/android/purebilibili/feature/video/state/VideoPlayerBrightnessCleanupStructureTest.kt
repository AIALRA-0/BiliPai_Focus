package com.android.purebilibili.feature.video.state

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VideoPlayerBrightnessCleanupStructureTest {

    @Test
    fun `player disposal reapplies cleared brightness attributes to window`() {
        val source = File(
            "app/src/main/java/com/android/purebilibili/feature/video/state/VideoPlayerState.kt"
        ).readText()
        val disposalCleanup = source
            .substringAfter("DisposableEffect(player) {")
            .substringBefore("//  [后台恢复优化]")

        assertTrue(
            disposalCleanup.contains(
                "layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE"
            )
        )
        assertTrue(disposalCleanup.contains("window.attributes = layoutParams"))
    }
}
