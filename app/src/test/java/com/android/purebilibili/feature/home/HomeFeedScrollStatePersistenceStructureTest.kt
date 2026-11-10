package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedScrollStatePersistenceStructureTest {

    @Test
    fun `bottom bar reselect scrolls the latest visible home category`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val scrollStateSource = source
            .substringAfter("// [新增] 监听全局回顶事件")
            .substringBefore("val homeTopTabSettings")
        val scrollCollectorSource = source
            .substringAfter("LaunchedEffect(scrollChannel)")
            .substringBefore("TrackJankStateFlag(")

        assertTrue(
            scrollStateSource.contains(
                "latestHomeScrollCategory by rememberUpdatedState(currentCategory)"
            )
        )
        assertTrue(
            scrollStateSource.contains(
                "latestHomeScrollPopularSubCategory by rememberUpdatedState(popularSubCategory)"
            )
        )
        assertTrue(scrollCollectorSource.contains("val activeCategory = latestHomeScrollCategory"))
        assertTrue(scrollCollectorSource.contains("gridStates[activeCategory]"))
    }

    @Test
    fun `bottom bar reselect reveals home chrome after scroll reaches top`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val lockSource = source
            .substringAfter("suspend fun withHomeScrollToTopLock")
            .substringBefore("// [新增] 监听全局回顶事件")

        assertTrue(lockSource.indexOf("block()") < lockSource.indexOf("revealHomeHeaderNow()"))
        assertFalse(lockSource.substringBefore("try {").contains("topTabsAutoCollapsedByScroll = false"))
        assertFalse(lockSource.substringBefore("try {").contains("globalScrollOffset.floatValue = 0f"))
        assertTrue(lockSource.indexOf("block()") < lockSource.indexOf("animate("))
        assertTrue(lockSource.contains("homeHeaderRevealLock = false"))
    }

    @Test
    fun `home double tap is coalesced before scroll to top starts`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val scrollCollectorSource = source
            .substringAfter("LaunchedEffect(scrollChannel)")
            .substringBefore("TrackJankStateFlag(")

        assertTrue(scrollCollectorSource.contains("for (initialRequest in channel)"))
        assertTrue(scrollCollectorSource.contains("withTimeoutOrNull(HOME_RESELECT_DOUBLE_TAP_WINDOW_MS)"))
        assertTrue(scrollCollectorSource.contains("mergeHomeScrollRequests(initialRequest, followUp)"))
        assertFalse(scrollCollectorSource.contains("collectLatest"))
    }

    @Test
    fun `avatar preference does not change card bounds during predictive return`() {
        val screenSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pageSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")
        val storySource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/cards/StoryVideoCard.kt"
        )
        val standardCardSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        )

        assertTrue(screenSource.contains("var settledShowHomeUpAvatars by rememberSaveable"))
        assertTrue(screenSource.contains("isReturnGestureInProgressProvider()"))
        assertTrue(screenSource.contains("!videoCardReturnGestureInProgress && !isReturningFromVideoDetail"))
        assertTrue(screenSource.contains("showUpAvatars = settledShowHomeUpAvatars"))

        // 关闭后不创建头像节点，也不创建兜底头像节点；Row 的 spacedBy 只作用于实际子项。
        assertTrue(pageSource.contains("if (showUpAvatars && video.owner.face.isNotBlank())"))
        assertTrue(pageSource.contains("} else if (showUpAvatars) {"))
        assertTrue(
            storySource.contains(
                "leadingContent = if ((showUpAvatar ?: com.android.purebilibili.core.ui.LocalUpBadgeVisibility.current.showAvatars) && video.owner.face.isNotEmpty())"
            )
        )
        assertTrue(standardCardSource.contains("leadingContent = if (showUpAvatar && video.owner.face.isNotEmpty())"))
    }

    @Test
    fun `home skin atmosphere is fixed in header instead of pager backdrop`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val headerCallSource = source
            .substringAfter("HomeHeader(")
            .substringBefore("AnimatedVisibility(")

        assertTrue(source.contains("val uiSkinState = LocalUiSkinState.current"))
        assertTrue(source.contains("val homeUiSkinDecoration = rememberHomeUiSkinDecoration(uiSkinState)"))
        assertTrue(headerCallSource.contains("uiSkinDecoration = homeUiSkinDecoration"))
        assertFalse(source.contains("import com.android.purebilibili.feature.home.components.HomeSkinAtmosphere"))
        assertFalse(source.contains("HomeSkinAtmosphere(\n                        decoration = homeUiSkinDecoration"))
    }

    @Test
    fun `home feed does not misuse drawer or profile skin backgrounds`() {
        val screenSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pageSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")
        val pageCallSource = screenSource
            .substringAfter("HomeCategoryPageContent(")
            .substringBefore("firstGridItemModifier = Modifier")
        val pageFunctionSource = pageSource
            .substringAfter("internal fun HomeCategoryPageContent(")
            .substringBefore("@Composable\nprivate fun PopularSubCategorySegmentedControl")
        assertFalse(pageCallSource.contains("uiSkinDecoration = homeUiSkinDecoration"))
        assertFalse(pageFunctionSource.contains("uiSkinDecoration: HomeUiSkinDecoration? = null"))
        assertFalse(pageSource.contains("resolveHomeFeedSkinAtmosphereImagePath"))
        assertFalse(pageSource.contains("sideBackgroundImagePath"))
        assertFalse(pageSource.contains("profileBackgroundImagePath"))
    }

    @Test
    fun `home pager page refresh uses page category instead of stale current state`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pagerPageSource = source
            .substringAfter("HorizontalPager(")
            .substringBefore("// Close HorizontalPager lambda")
        val onRefreshSource = pagerPageSource
            .substringAfter("onRefresh = {")
            .substringBefore("},")

        assertTrue(onRefreshSource.contains("viewModel.refresh(category)"))
        assertFalse(onRefreshSource.contains("viewModel.refresh()"))
        assertFalse(onRefreshSource.contains("if (category == HomeCategory.FOLLOW)"))
    }

    @Test
    fun `home feed samples scroll state without observing every scroll transition`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val scrollSamplingSource = source
            .substringAfter("val isFeedScrollInProgress by remember(activeGridState)")
            .substringBefore("val homeInteractionMotionBudget")

        assertTrue(scrollSamplingSource.contains("derivedStateOf { activeGridState?.isScrollInProgress == true }"))
        assertTrue(scrollSamplingSource.contains("globalFeedScrollInProgress.value = isFeedScrollInProgress"))
        assertFalse(scrollSamplingSource.contains("snapshotFlow"))
    }

    @Test
    fun `home pager frame state is read inside isolated motion layer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val tabsSource = source
            .substringAfter("private fun LightweightHomeTopTabs(")
            .substringBefore("internal enum class TopTabLiquidColorMode")
        val motionSource = tabsSource.substringAfter("HomeTopTabMotionLayer {")

        assertTrue(tabsSource.contains("val currentPositionProvider = remember(pagerState, selectedIndex)"))
        assertTrue(tabsSource.contains("val pagerScrollingProvider = remember(pagerState)"))
        assertTrue(motionSource.contains("val currentPositionFromPager = currentPositionProvider()"))
        assertTrue(motionSource.contains("val selectedContentPositionFromPager = selectedContentPositionProvider()"))
        assertTrue(motionSource.contains("val pagerIsScrolling = pagerScrollingProvider()"))
    }

    @Test
    fun `category initial load uses coordinated request ownership`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val initialLoadSource = source
            .substringAfter("private fun startCategoryLoad(")
            .substringBefore("private suspend fun fetchDataWithStartupRecovery")
        val requestSource = source
            .substringAfter("private fun launchFeedRequest(")
            .substringBefore("fun refresh(")

        assertTrue(source.contains("private val feedRequests = HomeFeedRequestCoordinator()"))
        assertTrue(initialLoadSource.contains("HomeFeedRequestKind.INITIAL"))
        assertTrue(initialLoadSource.contains("startupRecovery"))
        assertTrue(requestSource.contains("catch (error: CancellationException)"))
        assertTrue(requestSource.contains("copy(isLoading = false)"))
        assertTrue(requestSource.contains("throw error"))
    }

    @Test
    fun `home follow refresh preserves dynamic update baseline for incremental content`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertTrue(followFeedSource.contains("followFeedSource.load("))
        assertFalse(followFeedSource.contains("resetPagination"))
    }

    @Test
    fun `home follow feed requests video dynamics instead of filtering all dynamics after baseline`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")

        assertTrue(followFeedSource.contains("val followType = \"video\""))
        assertTrue(followFeedSource.contains("type = followType"))
    }

    @Test
    fun `home follow manual refresh reports api update_num via baseline probe`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt")
        val fetchDataSource = source
            .substringAfter("if (currentCategory == HomeCategory.FOLLOW)")
            .substringBefore("val currentCategoryState")
        val followFeedSource = source
            .substringAfter("private suspend fun fetchFollowFeed")
            .substringBefore("private fun videoItemKey")
        val baselineSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeFollowFeedSource.kt")

        assertTrue(fetchDataSource.contains("val result = fetchFollowFeed("))
        assertTrue(fetchDataSource.contains("return result"))
        assertTrue(followFeedSource.contains("resolveHomeFollowRefreshNewItemsCount("))
        assertTrue(followFeedSource.contains("response.refreshUpdateNum"))
        assertTrue(baselineSource.contains("probeWithBaseline"))
        assertTrue(baselineSource.contains("currentUpdateBaseline"))
        assertTrue(baselineSource.contains("HomeFollowFeedResponse(fullFeed, true, feed.updateNum)"))
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
