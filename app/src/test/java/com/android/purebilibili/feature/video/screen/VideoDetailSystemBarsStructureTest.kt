package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class VideoDetailSystemBarsStructureTest {

    @Test
    fun videoDetailScreenDoesNotApplySystemBarsFromSideEffect() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPlatformPolicy.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPlatformPolicy.kt")

        assertFalse(source.readText().contains("SideEffect {"))
    }

    @Test
    fun videoActivityDoesNotShowSystemBarsFromConfigurationUpdate() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/VideoActivity.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/android/purebilibili/feature/video/VideoActivity.kt")
        val updateStateBody = source.readText()
            .substringAfter("private fun updateStateFromConfig")
            .substringBefore("private fun toggleFullscreen")

        assertFalse(updateStateBody.contains("show(WindowInsetsCompat.Type.systemBars())"))
    }
}
