package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailSkeletonStructureTest {

    @Test
    fun detailSkeletonSharesSweepStateWithoutCompositionFrameReads() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/SkeletonComponents.kt"
        )

        assertTrue(source.contains("sharedPulse: androidx.compose.runtime.State<Float>? = null"))
        assertTrue(source.contains("sharedPulse ?: LocalVideoSkeletonPulse.current ?: com.android.purebilibili.core.ui.skeleton.rememberSkeletonSweepState("))
        assertTrue(source.contains("rememberSkeletonSweepState("))
        assertTrue(source.contains("LocalVideoSkeletonPulse provides pulse"))
        assertTrue(source.contains("sharedPulse = sharedPulse"))
        assertTrue(source.contains(".skeletonBackground(rememberVideoSkeletonBlockColor())"))
        assertTrue(source.contains("position = { pulse?.value ?: Float.NaN }"))
        assertFalse(source.contains("val pulse by"))
        assertFalse(source.contains("com.valentinilk.shimmer.shimmer"))
        assertFalse(source.contains("modifier.shimmer()"))
    }

    @Test
    fun detailSkeletonMatchesCurrentDetailAndRelatedItemGeometry() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/SkeletonComponents.kt"
        )

        assertTrue(source.contains("VideoDetailTabBarSkeleton()"))
        assertTrue(source.contains("VideoDetailUpInfoSkeleton()"))
        assertTrue(source.contains("VideoDetailActionButtonsSkeleton()"))
        assertTrue(source.contains("HorizontalVideoCardFrame("))
        assertTrue(source.contains("videoCardCoverAspectRatio()"))
        assertTrue(source.contains("coverAspectRatio = cardLayout.coverAspectRatio"))
        assertTrue(source.contains("cardLayout.outerPaddingDp.dp"))
        assertTrue(source.contains("plainHorizontal = true"))
        assertTrue(source.contains("fillInfoHeight = true"))
        assertTrue(source.contains("stacked = stacked"))
        assertTrue(source.contains("shape = androidx.compose.ui.graphics.RectangleShape"))
    }

    @Test
    fun foldableAndLargeScreenPanesRenderLoadingBeforeSuccess() {
        val tablet = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt")
        val large = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/LargeScreenVideoLayout.kt")
        assertTrue(tablet.contains("VideoDetailSkeletonHandoff("))
        assertTrue(tablet.contains("loading = contentState == null && uiState is VideoPlaybackUiState.Loading"))
        assertTrue(large.contains("VideoDetailSkeletonHandoff("))
        assertTrue(tablet.contains("VideoDetailInfoPaneSkeleton("))
        assertTrue(tablet.split("VideoDetailSecondaryPaneSkeleton(").size >= 3)
        assertTrue(large.contains("VideoDetailInfoPaneSkeleton("))
        assertTrue(large.contains("VideoDetailSecondaryPaneSkeleton("))
        assertTrue(tablet.contains("relatedLoadState = success.relatedLoadState"))
        assertTrue(tablet.contains("tablet_intro_related_loading"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
