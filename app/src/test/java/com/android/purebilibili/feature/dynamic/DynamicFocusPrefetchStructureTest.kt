package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFocusPrefetchStructureTest {

    @Test
    fun `dynamic screen appends upstream pages when focus filtering leaves a sparse batch`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .replace("\r\n", "\n")
        val prefetchSource = source
            .substringAfter("LaunchedEffect(\n        accountIdentity.generation,")
            .substringBefore("val oldContentDividerLabel")

        assertTrue(prefetchSource.contains("unfilteredActivePresentation.items.size"))
        assertTrue(prefetchSource.contains("activePresentation.items.size"))
        assertTrue(prefetchSource.contains("shouldPrefetchMoreFocusDynamicItems("))
        assertTrue(prefetchSource.contains("refresh = false"))
        assertTrue(prefetchSource.contains("resolveDynamicFeedRequestType(displayedLogicalTab)"))
    }

    @Test
    fun `switching accounts resets prefetch progress even when first page size is unchanged`() {
        val previousAccount = FocusDynamicPrefetchProgress(
            accountGeneration = 4L,
            lastSourceItemCount = 20,
            extraPagesFetched = MAX_FOCUS_DYNAMIC_PREFETCH_PAGES,
        )

        val switchedAccount = resolveFocusDynamicPrefetchProgressForAccount(
            progress = previousAccount,
            accountGeneration = 5L,
        )

        assertEquals(FocusDynamicPrefetchProgress(accountGeneration = 5L), switchedAccount)
        assertTrue(
            shouldProcessFocusDynamicPrefetchBatch(
                sourceItemCount = 20,
                lastSourceItemCount = switchedAccount.lastSourceItemCount,
            ),
        )
        assertTrue(
            shouldPrefetchMoreFocusDynamicItems(
                visibleItemCount = 2,
                hasMore = true,
                filterEnabled = true,
                extraPagesFetched = switchedAccount.extraPagesFetched,
            ),
        )

        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .replace("\r\n", "\n")
        val prefetchStateSource = source
            .substringAfter("var focusPrefetchProgress by remember(")
            .substringBefore("val oldContentDividerLabel")
        assertTrue(prefetchStateSource.contains("accountIdentity.generation"))
        assertTrue(prefetchStateSource.contains("LaunchedEffect(\n        accountIdentity.generation,"))
    }

    @Test
    fun `dynamic presentation keeps upstream pagination state while filtering feed items`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicFocusFollowGroupPolicy.kt")
        val presentationFilter = source
            .substringAfter("internal fun DynamicPagePresentation.withFocusFollowGroupFilter(")
            .substringBefore("internal fun filterSidebarUsersByFocusFollowGroups(")

        assertTrue(presentationFilter.contains("copy("))
        assertTrue(presentationFilter.contains("items = filterDynamicItemsByFocusFollowGroups("))
        assertTrue(presentationFilter.contains("filterEnabled = filterEnabled"))
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
        assertTrue(pagePresentation.contains("withFocusFollowGroupFilter("))
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
