package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeRefreshPolicyTest {

    @Test
    fun trimIncrementalRefreshVideosToEvenCount_keepsSingleItem() {
        val input = listOf(VideoItem(bvid = "BV1"))

        val output = trimIncrementalRefreshVideosToEvenCount(input)

        assertEquals(listOf("BV1"), output.map { it.bvid })
    }

    @Test
    fun trimIncrementalRefreshVideosToEvenCount_dropsLastWhenOddAndMoreThanOne() {
        val input = listOf(
            VideoItem(bvid = "BV1"),
            VideoItem(bvid = "BV2"),
            VideoItem(bvid = "BV3")
        )

        val output = trimIncrementalRefreshVideosToEvenCount(input)

        assertEquals(listOf("BV1", "BV2"), output.map { it.bvid })
    }

    @Test
    fun trimIncrementalRefreshVideosToEvenCount_keepsAllWhenEven() {
        val input = listOf(
            VideoItem(bvid = "BV1"),
            VideoItem(bvid = "BV2"),
            VideoItem(bvid = "BV3"),
            VideoItem(bvid = "BV4")
        )

        val output = trimIncrementalRefreshVideosToEvenCount(input)

        assertEquals(listOf("BV1", "BV2", "BV3", "BV4"), output.map { it.bvid })
    }

    @Test
    fun resolveRecommendFeedRequestIndex_advancesForManualRefreshAndLoadMore() {
        assertEquals(
            0,
            resolveRecommendFeedRequestIndex(
                isLoadMore = false,
                isManualRefresh = false,
                currentRefreshIndex = 0
            )
        )
        assertEquals(
            4,
            resolveRecommendFeedRequestIndex(
                isLoadMore = false,
                isManualRefresh = true,
                currentRefreshIndex = 3
            )
        )
        assertEquals(
            4,
            resolveRecommendFeedRequestIndex(
                isLoadMore = true,
                isManualRefresh = false,
                currentRefreshIndex = 3
            )
        )
    }

    @Test
    fun shouldAdvanceRecommendFeedRequestIndex_whenRecommendRequestReturnedAnyValidVideo() {
        assertTrue(
            shouldAdvanceRecommendFeedRequestIndex(
                category = HomeCategory.RECOMMEND,
                isLoadMore = false,
                isManualRefresh = true,
                validVideoCount = 8
            )
        )
        assertTrue(
            shouldAdvanceRecommendFeedRequestIndex(
                category = HomeCategory.RECOMMEND,
                isLoadMore = true,
                isManualRefresh = false,
                validVideoCount = 8
            )
        )
        assertFalse(
            shouldAdvanceRecommendFeedRequestIndex(
                category = HomeCategory.RECOMMEND,
                isLoadMore = false,
                isManualRefresh = true,
                validVideoCount = 0
            )
        )
        assertFalse(
            shouldAdvanceRecommendFeedRequestIndex(
                category = HomeCategory.POPULAR,
                isLoadMore = false,
                isManualRefresh = true,
                validVideoCount = 8
            )
        )
    }

    @Test
    fun shouldHandleRefreshNewItemsEvent_requiresPositiveAndGreaterKey() {
        assertFalse(shouldHandleRefreshNewItemsEvent(refreshKey = 0L, handledKey = 0L))
        assertFalse(shouldHandleRefreshNewItemsEvent(refreshKey = 10L, handledKey = 10L))
        assertFalse(shouldHandleRefreshNewItemsEvent(refreshKey = 9L, handledKey = 10L))
        assertTrue(shouldHandleRefreshNewItemsEvent(refreshKey = 11L, handledKey = 10L))
    }

    @Test
    fun homeScreen_waitsForNewItemsBeforeRefreshScrollsToTop() {
        val source = listOf(
            java.io.File("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"),
            java.io.File("src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        ).first { it.exists() }.readText()

        assertFalse(
            source.contains("LaunchedEffect(isRefreshing, state.currentCategory, state.popularSubCategory)"),
            "松手进入刷新时旧卡片应留在当前位置，不能在 isRefreshing 变 true 时立即回顶"
        )
        assertTrue(
            source.contains("LaunchedEffect(refreshNewItemsKey, isRefreshing, currentCategory)"),
            "新卡片到达后再处理回顶和新增提示"
        )
        assertTrue(source.indexOf("shouldResetToTopAfterIncrementalRefresh(") > source.indexOf("refreshNewItemsKey"))
    }

    @Test
    fun buildHomeRefreshUndoSnapshot_returnsNull_forNonRecommendCategory() {
        val snapshot = buildHomeRefreshUndoSnapshot(
            refreshingCategory = HomeCategory.POPULAR,
            recommendCategoryState = CategoryContent(
                videos = listOf(VideoItem(bvid = "BV1")).toImmutableList(),
                pageIndex = 3,
                hasMore = false
            ),
            fallbackVideos = listOf(VideoItem(bvid = "BV2")).toImmutableList()
        )

        assertEquals(null, snapshot)
    }

    @Test
    fun buildHomeRefreshUndoSnapshot_returnsNull_whenRecommendListEmpty() {
        val snapshot = buildHomeRefreshUndoSnapshot(
            refreshingCategory = HomeCategory.RECOMMEND,
            recommendCategoryState = CategoryContent(videos = persistentListOf()),
            fallbackVideos = persistentListOf()
        )

        assertEquals(null, snapshot)
    }

    @Test
    fun buildHomeRefreshUndoSnapshot_limitsItemsAndPreservesPagingState() {
        val videos = (1..25).map { index -> VideoItem(bvid = "BV$index") }
        val snapshot = buildHomeRefreshUndoSnapshot(
            refreshingCategory = HomeCategory.RECOMMEND,
            recommendCategoryState = CategoryContent(
                videos = videos.toImmutableList(),
                pageIndex = 4,
                hasMore = false
            ),
            fallbackVideos = persistentListOf()
        )

        assertEquals(20, snapshot?.videos?.size)
        assertEquals("BV1", snapshot?.videos?.firstOrNull()?.bvid)
        assertEquals("BV20", snapshot?.videos?.lastOrNull()?.bvid)
        assertEquals(4, snapshot?.pageIndex)
        assertFalse(snapshot?.hasMore ?: true)
    }

    @Test
    fun applyHomeRefreshUndoSnapshot_restoresVideosAndPagingState() {
        val oldState = CategoryContent(
            videos = listOf(VideoItem(bvid = "NEW")).toImmutableList(),
            pageIndex = 8,
            hasMore = true
        )
        val snapshot = HomeRefreshUndoSnapshot(
            videos = listOf(VideoItem(bvid = "OLD-1"), VideoItem(bvid = "OLD-2")),
            pageIndex = 2,
            hasMore = false
        )

        val restored = applyHomeRefreshUndoSnapshot(oldState, snapshot)

        assertEquals(listOf("OLD-1", "OLD-2"), restored.videos.map { it.bvid })
        assertEquals(2, restored.pageIndex)
        assertFalse(restored.hasMore)
    }

    @Test
    fun shouldExposeHomeRefreshUndo_onlyWhenRecommendAndSnapshotExists() {
        assertFalse(
            shouldExposeHomeRefreshUndo(
                refreshingCategory = HomeCategory.POPULAR,
                snapshot = HomeRefreshUndoSnapshot(videos = listOf(VideoItem(bvid = "BV1")), pageIndex = 1, hasMore = true)
            )
        )
        assertFalse(
            shouldExposeHomeRefreshUndo(
                refreshingCategory = HomeCategory.RECOMMEND,
                snapshot = null
            )
        )
        assertTrue(
            shouldExposeHomeRefreshUndo(
                refreshingCategory = HomeCategory.RECOMMEND,
                snapshot = HomeRefreshUndoSnapshot(videos = listOf(VideoItem(bvid = "BV1")), pageIndex = 1, hasMore = true)
            )
        )
    }

    @Test
    fun shouldShowRecommendOldContentDivider_requiresRevealForCurrentRefresh() {
        assertFalse(
            shouldShowRecommendOldContentDivider(
                currentCategory = HomeCategory.RECOMMEND,
                refreshNewItemsKey = 12L,
                revealedRefreshKey = 0L,
                anchorBvid = "BV1",
                oldContentStartIndex = 3
            )
        )
        assertFalse(
            shouldShowRecommendOldContentDivider(
                currentCategory = HomeCategory.RECOMMEND,
                refreshNewItemsKey = 12L,
                revealedRefreshKey = 11L,
                anchorBvid = "BV1",
                oldContentStartIndex = 3
            )
        )
        assertTrue(
            shouldShowRecommendOldContentDivider(
                currentCategory = HomeCategory.RECOMMEND,
                refreshNewItemsKey = 12L,
                revealedRefreshKey = 12L,
                anchorBvid = "BV1",
                oldContentStartIndex = 3
            )
        )
        assertFalse(
            shouldShowRecommendOldContentDivider(
                currentCategory = HomeCategory.POPULAR,
                refreshNewItemsKey = 12L,
                revealedRefreshKey = 12L,
                anchorBvid = "BV1",
                oldContentStartIndex = 3
            )
        )
    }
}
