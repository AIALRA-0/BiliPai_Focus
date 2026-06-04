package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicFocusPrefetchStructureTest {

    @Test
    fun `dynamic timeline completes sparse focus-filtered pages before publishing state`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
        val loadSource = source
            .substringAfter("private suspend fun loadDynamicFeedRoundWithFocusCompletion")
            .substringBefore("private fun requestFocusDynamicPrefetchIfSparse")

        assertTrue(loadSource.contains("baselineItemsForCompletion"))
        assertTrue(loadSource.contains("filterDynamicItemsForTimelineRequestType("))
        assertTrue(loadSource.contains("shouldPrefetchMoreFocusDynamicItems("))
        assertTrue(loadSource.contains("refresh = false"))
        assertTrue(loadSource.contains("prependDistinctByKey("))
    }

    @Test
    fun `dynamic sparse focus prefetch counts current tab items only`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicViewModel.kt")
        val prefetchSource = source
            .substringAfter("private fun requestFocusDynamicPrefetchIfSparse")
            .substringBefore("fun refresh()")

        assertTrue(prefetchSource.contains("if (selectedTab == 4) return"))
        assertTrue(prefetchSource.contains("filterDynamicItemsForTimelineRequestType(_uiState.value.items, requestType)"))
        assertTrue(prefetchSource.contains("shouldPrefetchMoreFocusDynamicItems("))
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
