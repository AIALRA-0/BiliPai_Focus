package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
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
        assertTrue(loadSource.contains("appendDistinctByKey("))
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

    @Test
    fun `dynamic top bar keeps focus follow group entry in both layouts`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
        val topBarSource = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt")

        assertTrue(source.contains("onFocusFollowGroupClick = { showFocusFollowGroupSheet = true }"))
        assertTrue(source.contains("FocusFollowGroupSheet("))
        assertTrue(topBarSource.contains("contentDescription = \"关注分组过滤设置\""))
    }

    @Test
    fun `sidebar pager filters hidden group items when stored config changes`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
        val sidebarBranch = source
            .substringAfter("DynamicDisplayMode.SIDEBAR,")
            .substringBefore("DynamicDisplayMode.HORIZONTAL ->")
        val pagePresentation = sidebarBranch
            .substringAfter("val pagePresentation = remember(")
            .substringBefore("val pageDividerIndex")

        assertTrue(pagePresentation.contains("focusFollowGroupConfig"))
        assertTrue(pagePresentation.contains("focusFollowGroupFilteringEnabled"))
        assertTrue(pagePresentation.contains("filterDynamicItemsByFocusFollowGroups("))
        assertTrue(pagePresentation.contains("filterEnabled = focusFollowGroupFilteringEnabled"))
        assertTrue(sidebarBranch.contains("filteredItems = pagePresentation.items"))
    }

    @Test
    fun `focus group sheet uses adaptive sheet image and touch target APIs`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/FocusFollowGroupSheet.kt"
        ).replace(Regex("\\s+"), " ")

        assertTrue(source.contains("AppModalBottomSheet(onDismissRequest = onDismissRequest)"))
        assertTrue(source.contains("import coil3.compose.AsyncImage"))
        assertTrue(source.contains("heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)"))
        assertTrue(source.contains("const val ActionButtonHeightDp = 52"))
        assertTrue(source.contains("val actionButtonHeight = FocusFollowGroupLayoutSpec.ActionButtonHeightDp.dp"))
        assertTrue(source.contains("const val InputHeightDp = 60"))
        assertTrue(source.contains("val inputHeight = FocusFollowGroupLayoutSpec.InputHeightDp.dp"))
        assertFalse(source.contains("IOSModalBottomSheet"))
        assertFalse(source.contains("import coil.compose.AsyncImage"))
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
