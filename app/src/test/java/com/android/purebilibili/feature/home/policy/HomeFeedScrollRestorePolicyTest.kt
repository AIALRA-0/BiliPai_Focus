package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.feature.home.HomeCategory
import com.android.purebilibili.feature.home.PopularSubCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedScrollRestorePolicyTest {

    @Test
    fun capture_normalizesNegativeScrollValues() {
        val anchor = captureHomeFeedScrollAnchor(
            category = HomeCategory.RECOMMEND,
            popularSubCategory = PopularSubCategory.COMPREHENSIVE,
            firstVisibleItemIndex = -2,
            firstVisibleItemScrollOffset = -8,
            headerOffsetPx = -42f
        )

        assertEquals(0, anchor.firstVisibleItemIndex)
        assertEquals(0, anchor.firstVisibleItemScrollOffset)
        assertEquals(-42f, anchor.headerOffsetPx)
    }

    @Test
    fun restoreGate_requiresTopLevelSyncedAnchor() {
        val anchor = captureHomeFeedScrollAnchor(
            category = HomeCategory.FOLLOW,
            popularSubCategory = PopularSubCategory.COMPREHENSIVE,
            firstVisibleItemIndex = 3,
            firstVisibleItemScrollOffset = 20,
            headerOffsetPx = -16f
        )

        assertFalse(
            shouldRestoreHomeFeedScrollAnchor(
                isTopLevelActive = false,
                hasSyncedPagerWithState = true,
                anchor = anchor
            )
        )
        assertFalse(
            shouldRestoreHomeFeedScrollAnchor(
                isTopLevelActive = true,
                hasSyncedPagerWithState = false,
                anchor = anchor
            )
        )
        assertFalse(
            shouldRestoreHomeFeedScrollAnchor(
                isTopLevelActive = true,
                hasSyncedPagerWithState = true,
                anchor = null
            )
        )
        assertTrue(
            shouldRestoreHomeFeedScrollAnchor(
                isTopLevelActive = true,
                hasSyncedPagerWithState = true,
                anchor = anchor
            )
        )
    }

    @Test
    fun apply_onlyWhenViewportDrifted() {
        val anchor = captureHomeFeedScrollAnchor(
            category = HomeCategory.RECOMMEND,
            popularSubCategory = PopularSubCategory.COMPREHENSIVE,
            firstVisibleItemIndex = 4,
            firstVisibleItemScrollOffset = 36,
            headerOffsetPx = -28f
        )

        assertFalse(
            shouldApplyHomeFeedScrollAnchor(
                currentIndex = 4,
                currentOffset = 36,
                currentHeaderOffsetPx = -28f,
                anchor = anchor
            )
        )
        assertTrue(
            shouldApplyHomeFeedScrollAnchor(
                currentIndex = 5,
                currentOffset = 36,
                currentHeaderOffsetPx = -28f,
                anchor = anchor
            )
        )
        assertTrue(
            shouldApplyHomeFeedScrollAnchor(
                currentIndex = 4,
                currentOffset = 80,
                currentHeaderOffsetPx = -28f,
                anchor = anchor
            )
        )
        assertTrue(
            shouldApplyHomeFeedScrollAnchor(
                currentIndex = 4,
                currentOffset = 36,
                currentHeaderOffsetPx = 0f,
                anchor = anchor
            )
        )
    }

    @Test
    fun bottomBarListPadding_staysReservedForScrollHideAndAlwaysVisible() {
        assertFalse(
            shouldReserveHomeBottomBarListPadding(
                useSideNavigation = true,
                bottomBarVisibilityMode = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
            )
        )
        assertFalse(
            shouldReserveHomeBottomBarListPadding(
                useSideNavigation = false,
                bottomBarVisibilityMode = SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN
            )
        )
        assertTrue(
            shouldReserveHomeBottomBarListPadding(
                useSideNavigation = false,
                bottomBarVisibilityMode = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
            )
        )
        assertTrue(
            shouldReserveHomeBottomBarListPadding(
                useSideNavigation = false,
                bottomBarVisibilityMode = SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE
            )
        )
    }
}
