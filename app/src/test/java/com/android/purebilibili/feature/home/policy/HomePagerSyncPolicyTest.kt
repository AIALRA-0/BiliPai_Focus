package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.feature.home.HomeCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomePagerSyncPolicyTest {

    @Test
    fun pagerToCategorySync_waitsUntilScrollingStops() {
        val shouldSwitch = shouldSwitchHomeCategoryFromPager(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 2,
            pagerScrolling = true,
            currentCategoryIndex = 1
        )

        assertFalse(shouldSwitch)
    }

    @Test
    fun pagerToCategorySync_requiresInitialSync() {
        val shouldSwitch = shouldSwitchHomeCategoryFromPager(
            hasSyncedPagerWithState = false,
            pagerCurrentPage = 2,
            pagerScrolling = false,
            currentCategoryIndex = 1
        )

        assertFalse(shouldSwitch)
    }

    @Test
    fun pagerToCategorySync_switchesOnlyWhenSettledPageDiffers() {
        val shouldSwitch = shouldSwitchHomeCategoryFromPager(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 2,
            pagerScrolling = false,
            currentCategoryIndex = 1
        )

        assertTrue(shouldSwitch)
    }

    @Test
    fun pagerSettledAction_routesToLivePage_whenSettledCategoryIsLive() {
        val action = resolveHomePagerSettledAction(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 2,
            pagerScrolling = false,
            currentCategoryIndex = 1,
            settledCategory = HomeCategory.LIVE
        )

        assertEquals(HomePagerSettledAction.OPEN_LIVE_PAGE, action)
    }

    @Test
    fun pagerSettledAction_switchesCategory_forRegularSettledCategory() {
        val action = resolveHomePagerSettledAction(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 2,
            pagerScrolling = false,
            currentCategoryIndex = 1,
            settledCategory = HomeCategory.POPULAR
        )

        assertEquals(HomePagerSettledAction.SWITCH_CATEGORY, action)
    }

    @Test
    fun pagerSettledAction_isNone_whenPagerShouldNotSync() {
        val action = resolveHomePagerSettledAction(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 1,
            pagerScrolling = false,
            currentCategoryIndex = 1,
            settledCategory = HomeCategory.LIVE
        )

        assertEquals(HomePagerSettledAction.NONE, action)
    }

    @Test
    fun initialPagerSync_usesSnapWhenTargetExists() {
        assertTrue(
            shouldUseInitialHomePagerSnap(
                hasSyncedPagerWithState = false,
                targetPage = 0
            )
        )
    }

    @Test
    fun pagerAnimation_skipsWhenAlreadyOnTarget() {
        assertFalse(
            shouldAnimateHomePagerToCategory(
                hasSyncedPagerWithState = true,
                targetPage = 2,
                pagerCurrentPage = 2,
                pagerScrolling = false,
                programmaticPageSwitchInProgress = false
            )
        )
    }

    @Test
    fun pagerAnimation_skipsWhileProgrammaticSwitchAlreadyRunning() {
        assertFalse(
            shouldAnimateHomePagerToCategory(
                hasSyncedPagerWithState = true,
                targetPage = 3,
                pagerCurrentPage = 1,
                pagerScrolling = false,
                programmaticPageSwitchInProgress = true
            )
        )
    }

    @Test
    fun pagerAnimation_runsAfterInitialSyncWhenPagerIsIdle() {
        assertTrue(
            shouldAnimateHomePagerToCategory(
                hasSyncedPagerWithState = true,
                targetPage = 3,
                pagerCurrentPage = 1,
                pagerScrolling = false,
                programmaticPageSwitchInProgress = false
            )
        )
    }
}
