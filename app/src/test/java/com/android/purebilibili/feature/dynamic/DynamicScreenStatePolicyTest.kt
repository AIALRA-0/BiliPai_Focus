package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicScreenStatePolicyTest {

    @Test
    fun `horizontal dynamic header should use tighter list top padding`() {
        assertEquals(
            168,
            resolveDynamicListTopPaddingExtraDp(
                isHorizontalMode = true,
                isHorizontalUserListCollapsed = false
            )
        )
        assertEquals(
            100,
            resolveDynamicListTopPaddingExtraDp(
                isHorizontalMode = true,
                isHorizontalUserListCollapsed = true
            )
        )
        assertEquals(
            100,
            resolveDynamicListTopPaddingExtraDp(
                isHorizontalMode = false,
                isHorizontalUserListCollapsed = false
            )
        )
    }

    @Test
    fun `horizontal user list should use compact vertical padding`() {
        assertEquals(4, resolveHorizontalUserListVerticalPaddingDp())
    }

    @Test
    fun `horizontal user list collapses once feed leaves top`() {
        assertFalse(
            shouldCollapseDynamicHorizontalUserList(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0
            )
        )
        assertTrue(
            shouldCollapseDynamicHorizontalUserList(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 12
            )
        )
        assertTrue(
            shouldCollapseDynamicHorizontalUserList(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `error overlay should show when active list is empty and error exists`() {
        assertTrue(
            shouldShowDynamicErrorOverlay(
                error = "加载失败",
                activeItemsCount = 0
            )
        )
    }

    @Test
    fun `error overlay should hide when active list has data`() {
        assertFalse(
            shouldShowDynamicErrorOverlay(
                error = "加载失败",
                activeItemsCount = 3
            )
        )
    }

    @Test
    fun `loading footer should follow active list size`() {
        assertTrue(shouldShowDynamicLoadingFooter(isLoading = true, activeItemsCount = 1))
        assertFalse(shouldShowDynamicLoadingFooter(isLoading = true, activeItemsCount = 0))
        assertFalse(shouldShowDynamicLoadingFooter(isLoading = false, activeItemsCount = 2))
    }

    @Test
    fun `no more footer should follow active hasMore and list size`() {
        assertTrue(shouldShowDynamicNoMoreFooter(hasMore = false, activeItemsCount = 1))
        assertFalse(shouldShowDynamicNoMoreFooter(hasMore = true, activeItemsCount = 1))
        assertFalse(shouldShowDynamicNoMoreFooter(hasMore = false, activeItemsCount = 0))
    }

    @Test
    fun `comment sheet should only show when a dynamic is selected`() {
        assertTrue(shouldShowDynamicCommentSheet("dyn:123"))
        assertFalse(shouldShowDynamicCommentSheet(null))
        assertFalse(shouldShowDynamicCommentSheet(""))
    }

    @Test
    fun `comment sheet total count should prefer live comment payload`() {
        assertEquals(26, resolveDynamicCommentSheetTotalCount(liveCount = 26, fallbackCount = 12))
        assertEquals(12, resolveDynamicCommentSheetTotalCount(liveCount = 0, fallbackCount = 12))
        assertEquals(0, resolveDynamicCommentSheetTotalCount(liveCount = 0, fallbackCount = -3))
    }

    @Test
    fun `followed user list reset should trigger only for fresh prepended refresh while viewing all`() {
        assertTrue(
            shouldResetFollowedUserListToTopOnRefresh(
                boundaryKey = "dyn:123",
                prependedCount = 3,
                selectedUserId = null,
                handledBoundaryKey = null
            )
        )
        assertFalse(
            shouldResetFollowedUserListToTopOnRefresh(
                boundaryKey = "dyn:123",
                prependedCount = 0,
                selectedUserId = null,
                handledBoundaryKey = null
            )
        )
        assertFalse(
            shouldResetFollowedUserListToTopOnRefresh(
                boundaryKey = "dyn:123",
                prependedCount = 2,
                selectedUserId = 10001L,
                handledBoundaryKey = null
            )
        )
        assertFalse(
            shouldResetFollowedUserListToTopOnRefresh(
                boundaryKey = "dyn:123",
                prependedCount = 2,
                selectedUserId = null,
                handledBoundaryKey = "dyn:123"
            )
        )
    }

    @Test
    fun `clicking the selected user again should return to all followed dynamics`() {
        assertNull(
            resolveDynamicSelectedUserIdAfterClick(
                selectedUserId = 10001L,
                clickedUserId = 10001L
            )
        )
    }

    @Test
    fun `clicking a different user should switch the dynamic filter`() {
        assertEquals(
            10002L,
            resolveDynamicSelectedUserIdAfterClick(
                selectedUserId = 10001L,
                clickedUserId = 10002L
            )
        )
        assertEquals(
            10003L,
            resolveDynamicSelectedUserIdAfterClick(
                selectedUserId = null,
                clickedUserId = 10003L
            )
        )
    }

    @Test
    fun `selected user feed is active only on up tab`() {
        assertTrue(shouldUseSelectedUserDynamicFeed(selectedTab = 4, selectedUserId = 10001L))
        assertFalse(shouldUseSelectedUserDynamicFeed(selectedTab = 0, selectedUserId = 10001L))
        assertFalse(shouldUseSelectedUserDynamicFeed(selectedTab = 4, selectedUserId = null))
    }

    @Test
    fun `clicking user avatar switches to up tab and clearing returns to all`() {
        assertEquals(
            4,
            resolveDynamicTabAfterUserSelection(
                selectedUserId = null,
                clickedUserId = 10001L,
                currentTab = 0
            )
        )
        assertEquals(
            0,
            resolveDynamicTabAfterUserSelection(
                selectedUserId = 10001L,
                clickedUserId = 10001L,
                currentTab = 4
            )
        )
        assertEquals(
            2,
            resolveDynamicTabAfterUserSelection(
                selectedUserId = 10001L,
                clickedUserId = 10001L,
                currentTab = 2
            )
        )
    }

    @Test
    fun `saved dynamic tab restores when index is valid`() {
        assertEquals(4, resolveDynamicSelectedTab(savedTab = 4, tabCount = 5))
    }

    @Test
    fun `saved dynamic tab falls back to all when index is invalid`() {
        assertEquals(0, resolveDynamicSelectedTab(savedTab = null, tabCount = 5))
        assertEquals(0, resolveDynamicSelectedTab(savedTab = -1, tabCount = 5))
        assertEquals(0, resolveDynamicSelectedTab(savedTab = 5, tabCount = 5))
        assertEquals(0, resolveDynamicSelectedTab(savedTab = 1, tabCount = 0))
    }

    @Test
    fun `dynamic horizontal swipe switches to adjacent tab`() {
        assertEquals(
            1,
            resolveDynamicSwipeTargetTab(
                currentTab = 0,
                tabCount = 5,
                dragDistancePx = -120f
            )
        )
        assertEquals(
            2,
            resolveDynamicSwipeTargetTab(
                currentTab = 3,
                tabCount = 5,
                dragDistancePx = 120f
            )
        )
    }

    @Test
    fun `dynamic horizontal swipe ignores weak drag and clamps edges`() {
        assertNull(
            resolveDynamicSwipeTargetTab(
                currentTab = 2,
                tabCount = 5,
                dragDistancePx = -40f
            )
        )
        assertNull(
            resolveDynamicSwipeTargetTab(
                currentTab = 0,
                tabCount = 5,
                dragDistancePx = 120f
            )
        )
        assertNull(
            resolveDynamicSwipeTargetTab(
                currentTab = 4,
                tabCount = 5,
                dragDistancePx = -120f
            )
        )
    }

    @Test
    fun `dynamic request type aligns with pili plus tab mapping`() {
        assertEquals("all", resolveDynamicFeedRequestType(selectedTab = 0))
        assertEquals("video", resolveDynamicFeedRequestType(selectedTab = 1))
        assertEquals("pgc", resolveDynamicFeedRequestType(selectedTab = 2))
        assertEquals("article", resolveDynamicFeedRequestType(selectedTab = 3))
        assertEquals("all", resolveDynamicFeedRequestType(selectedTab = 4))
    }

    @Test
    fun `only content tabs use server filtered dynamic feed`() {
        assertTrue(shouldUseServerFilteredDynamicFeed(selectedTab = 1))
        assertTrue(shouldUseServerFilteredDynamicFeed(selectedTab = 2))
        assertTrue(shouldUseServerFilteredDynamicFeed(selectedTab = 3))
        assertFalse(shouldUseServerFilteredDynamicFeed(selectedTab = 0))
        assertFalse(shouldUseServerFilteredDynamicFeed(selectedTab = 4))
    }

    @Test
    fun `incremental refresh prepends new items without dropping current list`() {
        val existing = listOf(buildDynamicItem("old_a"), buildDynamicItem("old_b"))
        val result = resolveDynamicFeedStateAfterSuccess(
            currentState = DynamicUiState(items = existing),
            incomingItems = listOf(buildDynamicItem("new_1"), buildDynamicItem("new_2")),
            isRefresh = true,
            requestType = "all",
            incrementalRefreshEnabled = true,
            hasMore = true
        )

        assertEquals(
            listOf("new_1", "new_2", "old_a", "old_b"),
            result.items.map { it.id_str }
        )
        assertEquals("old_a", result.incrementalRefreshBoundaryKey)
        assertEquals(2, result.incrementalPrependedCount)
        assertEquals(DynamicFeedErrorSource.NONE, result.errorSource)
        assertEquals("all", result.timelineRequestType)
    }

    @Test
    fun `incremental refresh does not merge items from a different dynamic feed type`() {
        val result = resolveDynamicFeedStateAfterSuccess(
            currentState = DynamicUiState(
                items = listOf(buildDynamicItem("pgc_old")),
                timelineRequestType = "pgc"
            ),
            incomingItems = listOf(buildDynamicItem("all_new")),
            isRefresh = true,
            requestType = "all",
            incrementalRefreshEnabled = true,
            hasMore = true
        )

        assertEquals(listOf("all_new"), result.items.map { it.id_str })
        assertEquals("all", result.timelineRequestType)
        assertEquals(null, result.incrementalRefreshBoundaryKey)
        assertEquals(0, result.incrementalPrependedCount)
    }

    @Test
    fun `pagination failure preserves existing items and marks append error`() {
        val existing = listOf(buildDynamicItem("keep_me"))
        val result = resolveDynamicFeedStateAfterFailure(
            currentState = DynamicUiState(items = existing),
            errorMessage = "网络错误",
            refresh = false
        )

        assertEquals(existing, result.items)
        assertEquals("网络错误", result.error)
        assertEquals(DynamicFeedErrorSource.APPEND, result.errorSource)
    }

    @Test
    fun `first load error is recorded as initial load source`() {
        val result = resolveDynamicFeedStateAfterFailure(
            currentState = DynamicUiState(),
            errorMessage = "未登录",
            refresh = true
        )

        assertEquals(DynamicFeedErrorSource.INITIAL_LOAD, result.errorSource)
        assertEquals("未登录", result.error)
    }

    @Test
    fun `refresh failure with existing items is not treated as append failure`() {
        val result = resolveDynamicFeedStateAfterFailure(
            currentState = DynamicUiState(items = listOf(buildDynamicItem("keep_me"))),
            errorMessage = "刷新失败",
            refresh = true
        )

        assertEquals(DynamicFeedErrorSource.REFRESH, result.errorSource)
        assertEquals("刷新失败", result.error)
    }

    @Test
    fun `selected user presentation state uses user scoped loading and error`() {
        val state = DynamicUiState(
            isLoading = false,
            error = "主时间线错误",
            userIsLoading = true,
            userError = "用户动态错误"
        )

        assertTrue(
            resolveDynamicActiveLoadingState(
                currentState = state,
                selectedUserId = 10001L
            )
        )
        assertEquals(
            "用户动态错误",
            resolveDynamicActiveError(
                currentState = state,
                selectedUserId = 10001L
            )
        )
        assertEquals(
            "主时间线错误",
            resolveDynamicActiveError(
                currentState = state,
                selectedUserId = null
            )
        )
    }
}

private fun buildDynamicItem(id: String) = DynamicItem(id_str = id)
