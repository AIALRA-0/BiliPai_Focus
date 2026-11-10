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

        assertTrue(source.contains("rememberVideoSkeletonPulse()"))
        assertTrue(source.contains("State<Float>?>"))
        assertTrue(source.contains("rememberSkeletonSweepState()"))
        assertTrue(source.contains(".skeletonBackground(rememberVideoSkeletonBlockColor())"))
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
        assertTrue(source.contains(".clip(VideoDetailShapes.contentCard())"))
        assertTrue(source.contains("shape = VideoDetailShapes.media()"))
    }

    @Test
    fun foldableAndLargeScreenPanesRenderLoadingBeforeSuccess() {
        val tablet = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt")
        val large = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/LargeScreenVideoLayout.kt")
        assertTrue(tablet.contains("uiState is VideoPlaybackUiState.Loading && !layoutPolicy.useTabletopLayout"))
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
