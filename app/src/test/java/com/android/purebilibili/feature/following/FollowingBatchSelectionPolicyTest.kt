package com.android.purebilibili.feature.following

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowingBatchSelectionPolicyTest {

    @Test
    fun `toggle selection should add mid when not selected`() {
        val selected = toggleFollowingSelection(current = setOf(1L, 2L), mid = 3L)
        assertEquals(setOf(1L, 2L, 3L), selected)
    }

    @Test
    fun `toggle selection should remove mid when selected`() {
        val selected = toggleFollowingSelection(current = setOf(1L, 2L), mid = 2L)
        assertEquals(setOf(1L), selected)
    }

    @Test
    fun `select all should pick every visible user when not fully selected`() {
        val resolved = resolveFollowingSelectAll(
            visibleMids = listOf(10L, 20L, 30L),
            currentSelected = setOf(10L)
        )
        assertEquals(setOf(10L, 20L, 30L), resolved)
    }

    @Test
    fun `select all should clear visible users when already fully selected`() {
        val resolved = resolveFollowingSelectAll(
            visibleMids = listOf(10L, 20L, 30L),
            currentSelected = setOf(10L, 20L, 30L, 99L)
        )
        assertEquals(setOf(99L), resolved)
    }

    @Test
    fun `batch unfollow message should include success and failure`() {
        val message = buildBatchUnfollowResultMessage(successCount = 7, failedCount = 2)
        assertTrue(message.contains("7"))
        assertTrue(message.contains("2"))
    }

    @Test
    fun `group preselection should keep existing tags when every user has same groups`() {
        val initial = resolveFollowGroupInitialSelection(
            listOf(setOf(-10L, 1001L), setOf(1001L, -10L), setOf(1001L, -10L))
        )
        assertEquals(setOf(-10L, 1001L), initial)
        assertTrue(!hasMixedFollowGroupSelection(listOf(setOf(-10L, 1001L), setOf(1001L, -10L))))
    }

    @Test
    fun `group preselection should default empty when any user groups differ`() {
        val sets = listOf(setOf(-10L, 1001L), setOf(1001L), emptySet())
        val initial = resolveFollowGroupInitialSelection(sets)
        assertTrue(initial.isEmpty())
        assertTrue(hasMixedFollowGroupSelection(sets))
    }

    @Test
    fun `isRetryableBatchOperationError should detect risk-control messages`() {
        assertTrue(isRetryableBatchOperationError("请求过于频繁，请稍后重试"))
        assertTrue(isRetryableBatchOperationError("触发风控，请稍后再试"))
        assertTrue(isRetryableBatchOperationError("too many requests"))
        assertTrue(!isRetryableBatchOperationError("内容不存在"))
    }

    @Test
    fun `shouldSkipFollowingReload should skip when same mid and cached success`() {
        val skip = shouldSkipFollowingReload(
            cachedMid = 123L,
            targetMid = 123L,
            uiState = FollowingListUiState.Success(
                users = emptyList(),
                total = 0
            ),
            forceRefresh = false
        )
        assertTrue(skip)
    }

    @Test
    fun `shouldSkipFollowingReload should not skip when force refresh is enabled`() {
        val skip = shouldSkipFollowingReload(
            cachedMid = 123L,
            targetMid = 123L,
            uiState = FollowingListUiState.Success(
                users = emptyList(),
                total = 0
            ),
            forceRefresh = true
        )
        assertFalse(skip)
    }

    @Test
    fun `shouldUseFollowingPersistentCache should use cache when same mid and has users`() {
        val useCache = shouldUseFollowingPersistentCache(
            forceRefresh = false,
            requestMid = 123L,
            cachedMid = 123L,
            cachedUsersCount = 10
        )
        assertTrue(useCache)
    }

    @Test
    fun `shouldUseFollowingPersistentCache should ignore cache during force refresh`() {
        val useCache = shouldUseFollowingPersistentCache(
            forceRefresh = true,
            requestMid = 123L,
            cachedMid = 123L,
            cachedUsersCount = 10
        )
        assertFalse(useCache)
    }
}
