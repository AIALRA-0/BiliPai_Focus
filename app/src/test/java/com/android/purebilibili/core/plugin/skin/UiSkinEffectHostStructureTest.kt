package com.android.purebilibili.core.plugin.skin

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class UiSkinEffectHostStructureTest {

    @Test
    fun activeSkinStateIsProvidedToTheNavigationComposition() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("LocalUiSkinState provides uiSkinState"))
    }

    @Test
    fun loadingLikeAndPlayerProgressReadTheirSkinSurfaces() {
        val loadingSource = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/LottieComponents.kt"
        )
        val likeSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CelebrationAnimations.kt"
        )
        val progressSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt"
        )
        val playerOverlaySource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt"
        )

        assertTrue(loadingSource.contains("UiSkinSurface.LOADING_INDICATOR"))
        assertTrue(playerOverlaySource.contains("SkinAwareLoadingIndicator"))
        assertTrue(playerOverlaySource.contains("loadingAnimation ?: it.loadingFrame"))
        assertTrue(likeSource.contains("UiSkinSurface.LIKE_EFFECT"))
        assertTrue(progressSource.contains("UiSkinSurface.PLAYER_PROGRESS"))
        assertTrue(progressSource.contains("playerProgressDraggingIcon"))
    }

    private fun loadSource(path: String): String {
        val sourceFile = File(path)
        require(sourceFile.exists()) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
