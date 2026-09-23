package com.android.purebilibili.feature.search

import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.repository.SearchUpOrder
import com.android.purebilibili.data.repository.SearchUserType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenPolicyTest {

    @Test
    fun searchResultCardsUseSemanticTypographyOnlyForMiuixNonGlassMode() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")
        val typographyFacade = source
            .substringAfter("private enum class SearchResultTextRole")
            .substringBefore("/**\n *  搜索结果卡片")

        assertTrue(typographyFacade.contains("if (isMiuixNonGlassEnabled())"))
        assertTrue(typographyFacade.contains("MaterialTheme.typography.titleSmall"))
        assertTrue(typographyFacade.contains("MaterialTheme.typography.bodySmall"))
        assertTrue(typographyFacade.contains("MaterialTheme.typography.labelMedium"))
        assertTrue(typographyFacade.contains("MaterialTheme.typography.labelSmall"))
        assertTrue(typographyFacade.contains("fontSize = legacyFontSize"))
        assertTrue(typographyFacade.contains("lineHeight = legacyLineHeight"))

        listOf(
            "fun SearchResultCard(",
            "internal fun UpSearchResultCard(",
            "internal fun BangumiSearchResultCard(",
            "internal fun LiveSearchResultCard(",
            "internal fun LiveUserSearchResultCard(",
            "internal fun TopicSearchResultCard(",
            "internal fun PhotoSearchResultCard(",
            "internal fun ArticleSearchResultCard(",
        ).forEach { signature ->
            val card = source.substringAfter(signature).substringBefore("\n}\n")
            assertTrue(card.contains("SearchResultText("), "$signature should use semantic text facade")
        }

        assertTrue(source.contains("AppSpacingTokens.Medium"))
        assertTrue(source.contains("AppSpacingTokens.ExtraSmall"))
        assertTrue(source.contains("if (useMiuixNonGlassPresentation) AppSpacingTokens.Medium else 14.dp"))
    }

    @Test
    fun resetSearchScroll_onlyWhenShowingNonBlankResults() {
        assertTrue(
            shouldResetSearchResultScroll(
                searchSessionId = 1L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 0L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 2L,
                showResults = false,
                lastResetSessionId = 1L
            )
        )
    }

    @Test
    fun resetSearchScroll_preservesPositionWhenReturningFromVideoDetail() {
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 2L,
                showResults = true,
                lastResetSessionId = 0L,
                isReturningFromVideoDetail = true,
            )
        )
    }

    @Test
    fun backToTopButton_onlyShowsAfterResultListScrollsPastThreshold() {
        assertFalse(
            shouldShowSearchBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 180
            )
        )
        assertTrue(
            shouldShowSearchBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 320
            )
        )
        assertTrue(
            shouldShowSearchBackToTop(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun submitKeyword_prefersTypedQuery_thenFallsBackToSuggestedKeyword() {
        assertEquals(
            "黑神话悟空",
            resolveSearchSubmitKeyword(
                query = "  黑神话悟空 ",
                suggestedKeyword = "睡羊妹妹m"
            )
        )
        assertEquals(
            "睡羊妹妹m",
            resolveSearchSubmitKeyword(
                query = " ",
                suggestedKeyword = " 睡羊妹妹m "
            )
        )
        assertEquals(
            "",
            resolveSearchSubmitKeyword(
                query = "",
                suggestedKeyword = " "
            )
        )
    }

    @Test
    fun `searchDefaultPlaceholder covers all visible tabs`() {
        assertEquals(
            "搜索视频、番剧、影视、直播、UP主、专栏等...",
            resolveSearchDefaultPlaceholder()
        )
    }

    @Test
    fun `searchUpUserTypeFilterLabel avoids repeating tab name`() {
        assertEquals("用户类型", resolveSearchUpUserTypeFilterLabel(SearchUserType.ALL))
        assertEquals("仅UP主", resolveSearchUpUserTypeFilterLabel(SearchUserType.UP))
    }

    @Test
    fun searchFilterTabs_followBiliPaiPrimaryOrder() {
        assertEquals(
            listOf(
                SearchType.VIDEO,
                SearchType.BANGUMI,
                SearchType.MEDIA_FT,
                SearchType.LIVE,
                SearchType.LIVE_USER,
                SearchType.UP,
                SearchType.ARTICLE,
                SearchType.TOPIC,
                SearchType.PHOTO
            ),
            resolveSearchFilterTabs()
        )
    }

    @Test
    fun searchFilterTabs_exposeEverySupportedApiType() {
        val visibleTabs = resolveSearchFilterTabs()

        assertEquals(SearchType.entries.toSet(), visibleTabs.toSet())
    }

    @Test
    fun searchFilterControls_matchCurrentSearchType() {
        assertEquals(
            listOf(
                SearchFilterControl.VIDEO_ORDER,
                SearchFilterControl.VIDEO_DURATION,
                SearchFilterControl.VIDEO_TID
            ),
            resolveSearchFilterControls(
                currentType = SearchType.VIDEO,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
        assertEquals(
            listOf(
                SearchFilterControl.UP_ORDER,
                SearchFilterControl.UP_ORDER_SORT,
                SearchFilterControl.UP_USER_TYPE
            ),
            resolveSearchFilterControls(
                currentType = SearchType.UP,
                currentUpOrder = SearchUpOrder.FANS
            )
        )
        assertEquals(
            listOf(SearchFilterControl.LIVE_ORDER),
            resolveSearchFilterControls(
                currentType = SearchType.LIVE,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
        assertEquals(
            listOf(
                SearchFilterControl.PHOTO_ORDER,
                SearchFilterControl.PHOTO_CATEGORY
            ),
            resolveSearchFilterControls(
                currentType = SearchType.PHOTO,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
        assertEquals(
            listOf(
                SearchFilterControl.ARTICLE_ORDER,
                SearchFilterControl.ARTICLE_CATEGORY
            ),
            resolveSearchFilterControls(
                currentType = SearchType.ARTICLE,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
    }

    @Test
    fun videoResultCardsReceiveReturnStateForSharedElementBack() {
        val source = File("src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")
            .readText()

        assertTrue(source.contains("isReturningFromVideoDetail: Boolean = false"))
        assertTrue(source.contains("isQuickReturningFromVideoDetail: Boolean = false"))
        assertTrue(source.contains("isReturningFromVideoDetail = isReturningFromVideoDetail"))
        assertTrue(source.contains("isQuickReturningFromVideoDetail = isQuickReturningFromVideoDetail"))
    }

    @Test
    fun videoResultCardsFollowCompactCoverStatsSetting() {
        val source = File("src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")
            .readText()

        assertTrue(source.contains("getCompactVideoStatsOnCover(context)"))
        assertTrue(source.contains("compactStatsOnCover = compactVideoStatsOnCover"))
    }

    @Test
    fun searchResultLazyItemKey_prefersStableBusinessKeys() {
        assertEquals(
            "video:0:text:BV1xx411c7mD",
            resolveSearchResultLazyItemKey(
                searchType = SearchType.VIDEO,
                index = 0,
                textKey = " BV1xx411c7mD ",
                numericKey = 123L
            )
        )
        assertEquals(
            "video:0:id:123",
            resolveSearchResultLazyItemKey(
                searchType = SearchType.VIDEO,
                index = 0,
                textKey = "",
                numericKey = 123L
            )
        )
        assertEquals(
            "media_bangumi:0:secondary:456",
            resolveSearchResultLazyItemKey(
                searchType = SearchType.BANGUMI,
                index = 0,
                numericKey = 0L,
                secondaryNumericKey = 456L
            )
        )
    }

    @Test
    fun searchResultLazyItemKey_usesIndexedFallbackForMissingIds() {
        val first = resolveSearchResultLazyItemKey(
            searchType = SearchType.VIDEO,
            index = 0,
            textKey = "",
            numericKey = 0L
        )
        val second = resolveSearchResultLazyItemKey(
            searchType = SearchType.VIDEO,
            index = 1,
            textKey = "",
            numericKey = 0L
        )

        assertEquals("video:local:0", first)
        assertEquals("video:local:1", second)
        assertTrue(first != second)
    }

    @Test
    fun searchResultLazyItemKey_disambiguatesDuplicateBusinessKeys() {
        val first = resolveSearchResultLazyItemKey(
            searchType = SearchType.VIDEO,
            index = 0,
            textKey = "BV_DUPLICATE"
        )
        val second = resolveSearchResultLazyItemKey(
            searchType = SearchType.VIDEO,
            index = 1,
            textKey = "BV_DUPLICATE"
        )

        assertEquals("video:0:text:BV_DUPLICATE", first)
        assertEquals("video:1:text:BV_DUPLICATE", second)
        assertTrue(first != second)
    }

    @Test
    fun searchResultLazyItemKey_preventsBlankAndDuplicateVideoGridKeys() {
        val keys = listOf(
            resolveSearchResultLazyItemKey(SearchType.VIDEO, index = 0, textKey = "", numericKey = 0L),
            resolveSearchResultLazyItemKey(SearchType.VIDEO, index = 1, textKey = "", numericKey = 0L),
            resolveSearchResultLazyItemKey(SearchType.VIDEO, index = 2, textKey = "BV_DUPLICATE", numericKey = 100L),
            resolveSearchResultLazyItemKey(SearchType.VIDEO, index = 3, textKey = "BV_DUPLICATE", numericKey = 100L)
        )

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.none { it.isBlank() })
    }

    @Test
    fun searchHighlightedTextSegments_preserveEmphasisAndDecodeEntities() {
        assertEquals(
            listOf(
                SearchHighlightedTextSegment("这是", highlighted = false),
                SearchHighlightedTextSegment("关键词", highlighted = true),
                SearchHighlightedTextSegment("&结尾", highlighted = false)
            ),
            resolveSearchHighlightedTextSegments("这是<em class=\"keyword\">关键词</em>&amp;结尾")
        )
    }

    @Test
    fun searchTypeTabs_useCompactDensityOnNarrowScreens() {
        val compact = resolveSearchTypeTabLayoutSpec(widthDp = 360)
        val regular = resolveSearchTypeTabLayoutSpec(widthDp = 412)

        assertEquals(6, compact.horizontalSpacingDp)
        assertEquals(10, compact.horizontalPaddingDp)
        assertEquals(13, compact.fontSizeSp)
        assertEquals(36, compact.minHeightDp)

        assertEquals(8, regular.horizontalSpacingDp)
        assertEquals(16, regular.horizontalPaddingDp)
        assertEquals(14, regular.fontSizeSp)
        assertEquals(40, regular.minHeightDp)
    }

    @Test
    fun searchResultPager_mapsPageAndTypeUsingVisibleTabs() {
        assertEquals(
            SearchType.MEDIA_FT,
            resolveSearchTypeForPagerPage(2)
        )
        assertEquals(
            3,
            resolveSearchPagerPageForType(SearchType.LIVE)
        )
        assertEquals(
            8,
            resolveSearchPagerPageForType(SearchType.PHOTO)
        )
    }

    @Test
    fun searchResultPageState_usesCurrentMirrorForActiveType() {
        val state = SearchUiState(
            query = "动画",
            showResults = true,
            searchType = SearchType.LIVE,
            isSearching = false,
            currentPage = 2,
            totalPages = 5,
            hasMoreResults = true
        )

        assertEquals(
            2,
            resolveSearchResultPageState(state, SearchType.LIVE).currentPage
        )
        assertTrue(resolveSearchResultPageState(state, SearchType.LIVE).hasMoreResults)
    }

    @Test
    fun searchResultPageState_restoresCachedInactiveType() {
        val cached = SearchResultPageUiState(
            query = "动画",
            currentPage = 1,
            totalPages = 3,
            hasMoreResults = true
        )
        val state = SearchUiState(
            query = "动画",
            showResults = true,
            searchType = SearchType.VIDEO,
            resultPages = mapOf(SearchType.UP to cached)
        )

        assertEquals(
            cached,
            resolveSearchResultPageState(state, SearchType.UP)
        )
    }

    @Test
    fun filterAndLayoutActionsUseCircularLiquidSurfaces() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchVideoFilterSheet.kt")
            .substringBefore("if (showFilterSheet)")
        assertEquals(2, Regex("BottomBarMatchedReusableLiquidDock\\(").findAll(source).count())
        assertEquals(2, Regex("shape = CircleShape").findAll(source).count())
        assertEquals(2, Regex("backdrop = miuixBackdrop").findAll(source).count())
        assertTrue(source.contains("reuseEnabled = true"))
    }

    @Test
    fun searchResultTransition_usesPagerAndKeepsFilterBarOutsidePager() {
        val searchSource = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")
        val filterSheetSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchVideoFilterSheet.kt"
        )
        val resultPagerStart = searchSource.indexOf("HorizontalPager(")
        val videoFilterBeforePager = searchSource.lastIndexOf("SearchVideoFilterBar(", resultPagerStart)
        val legacyFilterBeforePager = searchSource.lastIndexOf("SearchFilterBar(", resultPagerStart)
        val filterBarDeclaration = searchSource.indexOf("fun SearchFilterBar(")
        val resultPagerBody = searchSource.substring(resultPagerStart, filterBarDeclaration)

        assertTrue(resultPagerStart > 0)
        assertTrue(videoFilterBeforePager > 0 || legacyFilterBeforePager > 0)
        assertFalse(resultPagerBody.contains("SearchFilterBar("))
        assertFalse(resultPagerBody.contains("SearchVideoFilterBar("))
        assertFalse(searchSource.contains("detectHorizontalDragGestures"))
        // 液态胶囊 Tab（primary 渐变选中态 + 颜色过渡，与液体分段控件同语言，
        // 可横向滚动；不再用静态 surfaceContainerHigh 灰胶囊）。
        assertTrue(searchSource.contains("private fun SearchResultTypeTabRow("))
        assertTrue(searchSource.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(
            searchSource.contains("allowNativeLabelOverflow = true"),
            "关闭液态玻璃后 MD3 下划线必须完整显示 直播间 / UP主 等标签",
        )
        assertTrue(searchSource.contains("miuixBackdrop = searchChromeBackdrop"))
        assertTrue(searchSource.contains(".layerBackdrop(searchChromeBackdrop)"))
        assertTrue(searchSource.contains("externalPagerMotionEffectsEnabled = true"))
        val filterBar = searchSource.substringAfter("fun SearchFilterBar(")
        assertTrue(filterBar.contains("isMiuixNonGlassEnabled()"))
        assertTrue(filterBar.contains("9.dp"))
        assertTrue(filterBar.contains("AppSpacingTokens.Large"))
        assertTrue(searchSource.contains("indicatorPositionProvider = {"))
        assertTrue(searchSource.contains("pagerState.currentPage + pagerState.currentPageOffsetFraction"))
        val typeTabRowBody = searchSource
            .substringAfter("private fun SearchResultTypeTabRow(")
            .substringBefore("private fun rememberSearchHighlightedTitle(")
        assertTrue(typeTabRowBody.contains("shouldScrollSearchTypeTabs("))
        assertTrue(typeTabRowBody.contains("resolveSearchTypeTabAdaptiveItemWidthDp("))
        assertTrue(typeTabRowBody.contains(".liquidDockViewport()"))
        assertTrue(typeTabRowBody.contains(".horizontalScroll(scrollState)"))
        assertTrue(typeTabRowBody.contains("KeepScrollableTabSelectionVisible("))
        assertTrue(typeTabRowBody.contains("onIndicatorPositionChanged = { position ->"))
        assertTrue(typeTabRowBody.contains("resolveSearchTypeTabDragScrollDeltaPx("))
        assertFalse(searchSource.contains("androidx.compose.material3.ScrollableTabRow("))
        assertFalse(searchSource.contains("tabIndicatorOffset("))
        // Top bar uses native BasicTextField + TextFieldValue (not AppSearchField wrapper).
        assertTrue(searchSource.contains("SearchTopBarInputField("))
        assertTrue(searchSource.contains("TextFieldValue("))
        assertFalse(searchSource.contains("AppSearchField("))
        // Video filters use neutral AppFilterChip; host follows the stage-3 contract
        // (MIUIX → OverlayBottomSheet, MATERIAL3 → AppModalBottomSheet).
        assertTrue(filterSheetSource.contains("AppFilterChip("))
        assertTrue(filterSheetSource.contains("AppModalBottomSheet("))
        assertTrue(filterSheetSource.contains("OverlayBottomSheet("))
        assertTrue(
            filterSheetSource.contains("BottomBarLiquidSegmentedControl(") ||
                filterSheetSource.contains("AppThemeAdaptiveTabRow("),
            "视频排序应使用负责选中项和标签完整显示的标签栏",
        )
        // History chips use the neutral AppInputChip (visuals follow the theme layer).
        assertTrue(searchSource.contains("AppInputChip("))
        assertFalse(searchSource.contains("androidx.compose.material3.InputChip("))
        assertFalse(searchSource.contains("SearchPagerTabIndicator("))
        assertFalse(searchSource.contains("val showStableFilterBar = !searchPagerState.isScrollInProgress"))
        // Exiting results must not reopen IME.
        assertTrue(searchSource.contains("exitResultsToLanding("))
        assertTrue(searchSource.contains("dismissSearchKeyboardAndFocus("))
    }

    @Test
    fun searchTopBar_inputUsesFixedHeightNotFillMaxSize() {
        val searchSource = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")
        val topBar = searchSource
            .substringAfter("fun SearchTopBar(")
            .substringBefore("private fun SearchTopBarIconButton(")
        // 回归：fillMaxSize 会让输入框在 Column 剩余高度里变成竖向长胶囊
        assertFalse(topBar.contains("Modifier = Modifier.fillMaxSize()"))
        assertTrue(topBar.contains(".height(chromeSpec.inputHeightDp.dp)"))
        assertTrue(topBar.contains(".fillMaxWidth()"))
        assertTrue(topBar.contains("TextFieldValue("))
        assertTrue(topBar.contains("resolveSearchInputShape(topChromePolicy)"))
        assertFalse(topBar.contains("RoundedCornerShape("))
        assertFalse(topBar.contains("searchTopChromeGlass(dockShape)"))
        assertTrue(topBar.contains("searchTopChromeGlass(inputShape, chromeSpec.inputHeightDp)"))
        assertTrue(topBar.contains("searchTopChromeGlass(actionShape, chromeSpec.submitActionSizeDp)"))
        assertEquals(2, Regex("searchTopChromeGlass\\(actionShape, chromeSpec.clearActionSizeDp\\)").findAll(topBar).count())
        assertTrue(topBar.contains("resolveHomeTopEdgeButtonShape(topChromePolicy)"))
        assertTrue(topBar.contains("liquidGlassEnabled"))
        assertTrue(topBar.contains("homeTopBottomBarMatchedSurface("))
        assertTrue(topBar.contains("drawShellLens = true"))
        assertTrue(topBar.contains("resolveFloatingDockGeometryScale(controlHeightDp.toFloat())"))
    }

    @Test
    fun bottomBarSearchEntry_usesDedicatedTopBarContinuityMotion() {
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val searchSource = loadSource("app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt")

        assertTrue(navigationSource.contains("fun navigateToSearchFromBottomBar()"))
        assertTrue(navigationSource.contains("fun requestSearchFromBottomBar()"))
        assertTrue(navigationSource.contains("bottomBarSearchLaunchKey += 1"))
        assertTrue(navigationSource.contains("navigateToSearchFromBottomBar()"))
        assertTrue(navigationSource.contains("pushNavigation3Key(BiliPaiNavKey.Search)"))
        assertTrue(navigationSource.contains("onSearchClick = { requestSearchFromBottomBar() }"))
        assertTrue(navigationSource.contains("searchLaunchKey = bottomBarSearchLaunchKey"))
        assertFalse(navigationSource.contains("pendingBottomBarSearchLaunchKey"))
        assertFalse(navigationSource.contains("if (pendingBottomBarSearchLaunchKey == completedKey)"))
        assertTrue(navigationSource.contains("searchEntryMotionSource = SearchEntryMotionSource.BOTTOM_BAR"))
        assertTrue(navigationSource.contains("searchEntryMotionKey += 1"))
        assertTrue(navigationSource.contains("entryMotionSource = searchEntryMotionSource"))
        assertTrue(navigationSource.contains("entryMotionKey = searchEntryMotionKey"))

        assertTrue(searchSource.contains("entryMotionSource: SearchEntryMotionSource = SearchEntryMotionSource.NONE"))
        assertTrue(searchSource.contains("entryMotionSpec = resolveSearchEntryMotionSpec("))
        assertTrue(searchSource.contains("entryMotionKey = entryMotionKey"))
        assertTrue(searchSource.contains("graphicsLayer"))
        assertTrue(searchSource.contains("TransformOrigin("))
        assertTrue(searchSource.contains("spec.transformOriginPivotX"))
        assertTrue(searchSource.contains("spec.transformOriginPivotY"))
    }

    @Test
    fun searchEntryMotion_onlyRunsForBottomBarSourceAndRespectsReducedBudget() {
        assertEquals(
            null,
            resolveSearchEntryMotionSpec(
                source = SearchEntryMotionSource.NONE,
                reducedMotionBudget = false
            )
        )

        val bottomBarSpec = requireNotNull(
            resolveSearchEntryMotionSpec(
                source = SearchEntryMotionSource.BOTTOM_BAR,
                reducedMotionBudget = false
            )
        )
        assertEquals(320, bottomBarSpec.durationMillis)
        assertEquals(0.58f, bottomBarSpec.initialAlpha)
        assertEquals(0.88f, bottomBarSpec.initialScale)
        assertEquals(360f, bottomBarSpec.initialTranslationYDp)
        assertEquals(0.5f, bottomBarSpec.transformOriginPivotX)
        assertEquals(1f, bottomBarSpec.transformOriginPivotY)

        val reducedSpec = requireNotNull(
            resolveSearchEntryMotionSpec(
                source = SearchEntryMotionSource.BOTTOM_BAR,
                reducedMotionBudget = true
            )
        )
        assertEquals(0, reducedSpec.durationMillis)
        assertEquals(1f, reducedSpec.initialAlpha)
        assertEquals(1f, reducedSpec.initialScale)
        assertEquals(0f, reducedSpec.initialTranslationYDp)
    }

    @Test
    fun searchExitMotion_returnsControlTowardBottomSearchSlot() {
        val entry = resolveSearchEntryMotionSpec(
            source = SearchEntryMotionSource.BOTTOM_BAR,
            reducedMotionBudget = false,
        )

        val exit = requireNotNull(
            resolveSearchExitMotionSpec(
                entrySpec = entry,
                screenHeightDp = 900,
            )
        )

        assertEquals(300, exit.durationMillis)
        assertEquals(764f, exit.initialTranslationYDp)
        assertEquals(0.74f, exit.initialScale)
        assertEquals(0.88f, exit.transformOriginPivotX)
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
