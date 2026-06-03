package com.android.purebilibili.feature.search

import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.repository.SearchUpOrder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenPolicyTest {

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
    fun searchFilterTabs_exposeFullSearchTypesInPlannedOrder() {
        assertEquals(
            listOf(
                SearchType.VIDEO,
                SearchType.UP,
                SearchType.BANGUMI,
                SearchType.MEDIA_FT,
                SearchType.LIVE,
                SearchType.LIVE_USER,
                SearchType.ARTICLE,
                SearchType.TOPIC,
                SearchType.PHOTO
            ),
            resolveSearchFilterTabs()
        )
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
            emptyList(),
            resolveSearchFilterControls(
                currentType = SearchType.PHOTO,
                currentUpOrder = SearchUpOrder.DEFAULT
            )
        )
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
    fun searchResultSwipe_switchesToAdjacentSearchType() {
        assertEquals(
            SearchType.VIDEO,
            resolveSearchSwipeTargetType(
                currentType = SearchType.UP,
                dragDistancePx = 120f
            )
        )
        assertEquals(
            SearchType.BANGUMI,
            resolveSearchSwipeTargetType(
                currentType = SearchType.UP,
                dragDistancePx = -120f
            )
        )
        assertEquals(
            SearchType.UP,
            resolveSearchSwipeTargetType(
                currentType = SearchType.VIDEO,
                dragDistancePx = -120f
            )
        )
    }

    @Test
    fun searchResultSwipe_ignoresWeakDragAndClampsEdges() {
        assertEquals(
            null,
            resolveSearchSwipeTargetType(
                currentType = SearchType.UP,
                dragDistancePx = -40f
            )
        )
        assertEquals(
            null,
            resolveSearchSwipeTargetType(
                currentType = SearchType.VIDEO,
                dragDistancePx = 120f
            )
        )
        assertEquals(
            null,
            resolveSearchSwipeTargetType(
                currentType = SearchType.PHOTO,
                dragDistancePx = -120f
            )
        )
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
