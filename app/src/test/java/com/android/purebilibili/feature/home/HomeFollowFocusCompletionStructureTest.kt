package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HomeFollowFocusCompletionStructureTest {

    @Test
    fun `home follow feed completes sparse focus-filtered pages before presentation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertTrue(followFeedSource.contains("resolveHomeFollowRequiredVisibleIncrement("))
        assertTrue(followFeedSource.contains("shouldContinueHomeFollowFetchAfterFocusFilter("))
        assertTrue(followFeedSource.contains("DynamicRepository.getDynamicFeed("))
        assertTrue(followFeedSource.contains("refresh = false"))
        assertTrue(followFeedSource.contains("accumulateHomeFollowRoundRawVideos("))
    }

    @Test
    fun `home follow refresh keeps raw focus pool when filtering is enabled`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertTrue(followFeedSource.contains("requestBaselineRawVideos"))
        assertTrue(followFeedSource.contains("focusFollowGroupFilteringEnabled"))
        assertTrue(followFeedSource.contains("resolveHomeFollowPresentedRawVideos("))
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
