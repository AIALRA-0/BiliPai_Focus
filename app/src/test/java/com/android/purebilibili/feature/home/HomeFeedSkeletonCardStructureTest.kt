package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedSkeletonCardStructureTest {

    @Test
    fun homeLoadingGridUsesFeatureSkeletonInsteadOfCoreShimmerSkeleton() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val loadingGridSource = source
            .substringAfter("Loading Skeleton per page")
            .substringBefore("} else if (categoryState.error")

        assertTrue(loadingGridSource.contains("rememberHomeFeedSkeletonPulseState()"))
        assertTrue(loadingGridSource.contains("pulse = { skeletonPulseState.value }"))
        assertTrue(loadingGridSource.contains("HomeFeedSkeletonCard("))
        assertTrue(loadingGridSource.contains("contentType = { \"home_feed_skeleton_card\" }"))
        // 骨架不再自建独立网格：改为交给内容网格，在同一 LazyLayout 内原地铺占位卡，
        // 数据到达时是 item 原地替换而不是整屏重建。
        assertTrue(loadingGridSource.contains("initialSkeletonContent = initialSkeletonContent"))
        assertFalse(loadingGridSource.contains("LazyVerticalStaggeredGrid("))
        assertFalse(loadingGridSource.contains("VideoCardSkeleton("))
        assertFalse(loadingGridSource.contains("index % 10"))
    }

    @Test
    fun featureSkeletonUsesSharedHomeGeometryAndDrawSweep() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeFeedSkeletonCard.kt")

        assertTrue(source.contains("rememberSkeletonSweepState()"))
        assertTrue(source.contains("skeletonBackground(homeFeedSkeletonFill("))
        assertTrue(source.contains("videoCardCoverAspectRatio()"))
        // 骨架几何与真实视频卡共享 Card 语义，且卡片自身不再叠加底部 padding
        //（间距统一由网格 verticalItemSpacingDp 决定）。
        assertTrue(source.contains("cardCornerRadius = AppShapes.containerCornerDp(cardLevel)"))
        assertTrue(source.contains("cardShape = AppShapes.container(cardLevel)"))
        assertFalse(source.contains(".padding(bottom = AppSpacingTokens.Medium)"))
        assertTrue(source.contains("ContentSkeletonText("))
        assertTrue(source.contains("start = if (storyStyle) 0.dp else 10.dp"))
        assertTrue(source.contains("compactStatsOnCover"))
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
