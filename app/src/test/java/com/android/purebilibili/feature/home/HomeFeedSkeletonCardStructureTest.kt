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
    fun featureSkeletonMatchesHomeVideoCardGeometryAndUsesReversePulse() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeFeedSkeletonCard.kt")

        assertTrue(source.contains("RepeatMode.Reverse"))
        assertTrue(source.contains("durationMillis = HOME_FEED_SKELETON_PULSE_DURATION_MILLIS"))
        assertTrue(source.contains("VIDEO_SHARED_COVER_ASPECT_RATIO"))
        assertTrue(source.contains("LocalCornerRadiusScale.current"))
        assertTrue(source.contains("val cardCornerRadius = 12.dp * cornerRadiusScale"))
        assertTrue(source.contains(".padding(bottom = 12.dp)"))
        assertTrue(source.contains(".padding(horizontal = 10.dp, vertical = 8.dp)"))
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
