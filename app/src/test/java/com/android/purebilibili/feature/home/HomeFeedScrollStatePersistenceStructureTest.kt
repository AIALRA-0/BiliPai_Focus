package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedScrollStatePersistenceStructureTest {

    @Test
    fun `home category grid states are saveable per category`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")

        assertTrue(source.contains("rememberSaveable("))
        assertTrue(source.contains("category.name"))
        assertTrue(source.contains("saver = LazyGridState.Saver"))
        assertTrue(source.contains("LazyGridState()"))
        assertFalse(source.contains("gridStates[category] = rememberLazyGridState()"))
    }

    @Test
    fun `home skin atmosphere is fixed in header instead of pager backdrop`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val headerCallSource = source
            .substringAfter("iOSHomeHeader(")
            .substringBefore("AnimatedVisibility(")

        assertTrue(source.contains("val uiSkinState by rememberUiSkinState(context)"))
        assertTrue(source.contains("val homeUiSkinDecoration = rememberHomeUiSkinDecoration(uiSkinState)"))
        assertTrue(headerCallSource.contains("uiSkinDecoration = homeUiSkinDecoration"))
        assertFalse(source.contains("import com.android.purebilibili.feature.home.components.HomeSkinAtmosphere"))
        assertFalse(source.contains("HomeSkinAtmosphere(\n                        decoration = homeUiSkinDecoration"))
    }

    @Test
    fun `home skin feed atmosphere is drawn once behind grid container`() {
        val screenSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pageSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")
        val pageCallSource = screenSource
            .substringAfter("HomeCategoryPageContent(")
            .substringBefore("firstGridItemModifier = Modifier")
        val pageFunctionSource = pageSource
            .substringAfter("internal fun HomeCategoryPageContent(")
            .substringBefore("@Composable\nprivate fun PopularSubCategorySegmentedControl")
        val gridContainerSource = pageFunctionSource
            .substringAfter("val feedAtmosphereImagePath = resolveHomeFeedSkinAtmosphereImagePath(uiSkinDecoration)")
            .substringBefore("LazyVerticalGrid(")
        val videoItemSource = pageFunctionSource
            .substringAfter("categoryState.videos.forEachIndexed")

        assertTrue(pageCallSource.contains("uiSkinDecoration = homeUiSkinDecoration"))
        assertTrue(pageFunctionSource.contains("uiSkinDecoration: HomeUiSkinDecoration? = null"))
        assertTrue(pageFunctionSource.contains("val feedAtmosphereImagePath = resolveHomeFeedSkinAtmosphereImagePath(uiSkinDecoration)"))
        assertTrue(gridContainerSource.contains("AsyncImage("))
        assertTrue(gridContainerSource.contains("model = File(feedAtmosphereImagePath)"))
        assertFalse(videoItemSource.contains("resolveHomeFeedSkinAtmosphereImagePath(uiSkinDecoration)"))
        assertFalse(videoItemSource.contains("model = File(feedAtmosphereImagePath)"))
    }

    @Test
    fun `home pager page refresh uses page category instead of stale current state`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pagerPageSource = source
            .substringAfter("HorizontalPager(")
            .substringBefore("// Close HorizontalPager lambda")

        assertTrue(pagerPageSource.contains("if (category == HomeCategory.FOLLOW)"))
        assertTrue(pagerPageSource.contains("viewModel.refresh(category)"))
        assertTrue(pagerPageSource.contains("viewModel.refresh()"))
    }

    @Test
    fun `home feed enables scroll lite mode only from grid scroll state`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")
        val pageFunctionSource = source
            .substringAfter("internal fun HomeCategoryPageContent(")
            .substringBefore("val context = LocalContext.current")

        assertTrue(pageFunctionSource.contains("val scrollLiteModeEnabled by remember(gridState)"))
        assertTrue(pageFunctionSource.contains("derivedStateOf { gridState.isScrollInProgress }"))
        assertFalse(pageFunctionSource.contains("val scrollLiteModeEnabled = false"))
    }

    @Test
    fun `home follow refresh preserves dynamic update baseline for incremental content`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertFalse(followFeedSource.contains("DynamicRepository.resetPagination"))
    }

    @Test
    fun `home follow feed requests video dynamics instead of filtering all dynamics after baseline`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertTrue(followFeedSource.contains("type = \"video\""))
    }

    @Test
    fun `home follow manual refresh reports actual inserted video count`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val fetchDataSource = source
            .substringAfter("if (currentCategory == HomeCategory.FOLLOW)")
            .substringBefore("val currentCategoryState")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertTrue(fetchDataSource.contains("return fetchFollowFeed("))
        assertTrue(followFeedSource.contains("var addedCount = 0"))
        assertTrue(followFeedSource.contains("return addedCount"))
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
