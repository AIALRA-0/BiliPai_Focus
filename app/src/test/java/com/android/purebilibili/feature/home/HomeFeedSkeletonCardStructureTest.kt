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

        assertTrue(loadingGridSource.contains("val skeletonPulse = rememberHomeFeedSkeletonPulse()"))
        assertTrue(loadingGridSource.contains("HomeFeedSkeletonCard("))
        assertTrue(loadingGridSource.contains("contentType = { \"home_feed_skeleton_card\" }"))
        assertFalse(loadingGridSource.contains("VideoCardSkeleton("))
        assertFalse(loadingGridSource.contains("index % 10"))
    }

    @Test
    fun featureSkeletonUsesSharedHomeGeometryAndReversePulse() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeFeedSkeletonCard.kt")

        assertTrue(source.contains("RepeatMode.Reverse"))
        assertTrue(source.contains("durationMillis = HOME_FEED_SKELETON_PULSE_DURATION_MILLIS"))
        assertTrue(source.contains("VIDEO_SHARED_COVER_ASPECT_RATIO"))
        // 骨架几何与真实视频卡共享 Card 语义，且卡片自身不再叠加底部 padding
        //（间距统一由网格 verticalItemSpacingDp 决定）。
        assertTrue(source.contains("cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.Card)"))
        assertTrue(source.contains("cardShape = AppShapes.container(ContainerLevel.Card)"))
        assertFalse(source.contains(".padding(bottom = AppSpacingTokens.Medium)"))
        assertTrue(
            source.contains(
                ".padding(horizontal = AppSpacingTokens.Small + AppSpacingTokens.Micro, vertical = AppSpacingTokens.Small)"
            )
        )
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
