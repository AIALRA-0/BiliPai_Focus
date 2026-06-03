package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailSkeletonStructureTest {

    @Test
    fun detailSkeletonUsesSynchronizedPulseInsteadOfSweepShimmer() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/SkeletonComponents.kt"
        )

        assertTrue(source.contains("rememberVideoSkeletonPulse()"))
        assertTrue(source.contains("RepeatMode.Reverse"))
        assertTrue(source.contains("VIDEO_SKELETON_PULSE_DURATION_MILLIS"))
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
        assertTrue(source.contains("val relatedCoverWidth = 130.dp"))
        assertTrue(source.contains("relatedCoverWidth / VIDEO_SHARED_COVER_ASPECT_RATIO"))
        assertTrue(source.contains(".padding(5.dp)"))
        assertTrue(source.contains("RoundedCornerShape(12.dp)"))
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
