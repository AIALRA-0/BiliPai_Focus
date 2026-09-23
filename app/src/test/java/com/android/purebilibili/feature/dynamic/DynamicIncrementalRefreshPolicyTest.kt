package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicIncrementalRefreshPolicyTest {

    @Test
    fun resolveIncrementalRefreshBoundary_returnsFirstExistingAsBoundaryAndPrependedCount() {
        val result = resolveIncrementalRefreshBoundary(
            existingKeys = listOf("old_a", "old_b", "old_c"),
            mergedKeys = listOf("new_1", "new_2", "old_a", "old_b", "old_c")
        )

        assertEquals("old_a", result.boundaryKey)
        assertEquals(2, result.prependedCount)
    }

    @Test
    fun resolveIncrementalRefreshBoundary_countsAllPrependedItems() {
        val result = resolveIncrementalRefreshBoundary(
            existingKeys = listOf("old_a"),
            mergedKeys = listOf("new_1", "new_2", "new_3", "old_a")
        )

        assertEquals("old_a", result.boundaryKey)
        assertEquals(3, result.prependedCount)
    }

    @Test
    fun resolveIncrementalRefreshBoundary_returnsNoBoundaryWhenExistingEmpty() {
        val result = resolveIncrementalRefreshBoundary(
            existingKeys = emptyList(),
            mergedKeys = listOf("new_1", "new_2")
        )

        assertEquals(null, result.boundaryKey)
        assertEquals(0, result.prependedCount)
    }

    @Test
    fun resolveOldContentDividerIndex_returnsExpectedIndexOnlyWhenBoundaryVisible() {
        val index = resolveOldContentDividerIndex(
            displayKeys = listOf("new_1", "new_2", "old_a", "old_b"),
            boundaryKey = "old_a",
            showDivider = true
        )

        assertEquals(2, index)
    }

    @Test
    fun resolveOldContentDividerIndex_hidesDividerWhenBoundaryMissingOrInvalid() {
        val missing = resolveOldContentDividerIndex(
            displayKeys = listOf("new_1", "new_2"),
            boundaryKey = "old_a",
            showDivider = true
        )
        val top = resolveOldContentDividerIndex(
            displayKeys = listOf("old_a", "old_b"),
            boundaryKey = "old_a",
            showDivider = true
        )
        val disabled = resolveOldContentDividerIndex(
            displayKeys = listOf("new_1", "old_a"),
            boundaryKey = "old_a",
            showDivider = false
        )

        assertEquals(-1, missing)
        assertEquals(-1, top)
        assertEquals(-1, disabled)
    }

    @Test
    fun resolveDynamicRefreshDividerGridIndex_accountsForLeadingChromeInset() {
        assertEquals(4, resolveDynamicRefreshDividerGridIndex(dividerDataIndex = 3))
        assertEquals(3, resolveDynamicRefreshDividerGridIndex(dividerDataIndex = 3, leadingGridItemCount = 0))
        assertEquals(-1, resolveDynamicRefreshDividerGridIndex(dividerDataIndex = -1))
    }

    @Test
    fun shouldStartDynamicRefresh_requiresIdleRefreshAndUnlockedState() {
        assertEquals(true, shouldStartDynamicRefresh(isRefreshing = false, isLoadingLocked = false))
        assertEquals(false, shouldStartDynamicRefresh(isRefreshing = true, isLoadingLocked = false))
        assertEquals(false, shouldStartDynamicRefresh(isRefreshing = false, isLoadingLocked = true))
    }

    @Test
    fun refreshTarget_usesSelectedUserOnlyOnUserTab() {
        assertEquals(
            1001L,
            resolveDynamicRefreshUserId(selectedTab = 4, selectedUserId = 1001L)
        )
        assertEquals(
            null,
            resolveDynamicRefreshUserId(selectedTab = 0, selectedUserId = 1001L)
        )
        assertEquals(
            null,
            resolveDynamicRefreshUserId(selectedTab = 4, selectedUserId = null)
        )
    }
}
