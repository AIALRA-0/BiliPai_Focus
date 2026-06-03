package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.feature.home.HomeCategory
import com.android.purebilibili.feature.home.HomeTopTabEntry
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
    fun pagerToCategorySync_waitsDuringProgrammaticPageSwitch() {
        val shouldSwitch = shouldSwitchHomeCategoryFromPager(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 0,
            pagerScrolling = false,
            currentCategoryIndex = 1,
            programmaticPageSwitchInProgress = true
        )

        assertFalse(shouldSwitch)
    }

    @Test
    fun pagerSettledAction_switchesCategory_whenSettledCategoryIsLive() {
        val action = resolveHomePagerSettledAction(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 2,
            pagerScrolling = false,
            currentCategoryIndex = 1,
            settledCategory = HomeCategory.LIVE
        )

        assertEquals(HomePagerSettledAction.SWITCH_CATEGORY, action)
    }

    @Test
    fun homeTopLiveCategory_isDisplayedInline() {
        assertTrue(shouldDisplayHomeTopCategoryInline(HomeCategory.LIVE))
        assertTrue(shouldDisplayHomeTopCategoryInline(HomeCategory.RECOMMEND))
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
    fun pagerSettledAction_isNone_duringProgrammaticPageSwitch() {
        val action = resolveHomePagerSettledAction(
            hasSyncedPagerWithState = true,
            pagerCurrentPage = 0,
            pagerScrolling = false,
            currentCategoryIndex = 1,
            settledCategory = HomeCategory.RECOMMEND,
            programmaticPageSwitchInProgress = true
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
    fun pagerStateDrive_skipsWhenCategoryWasAlreadyDriven() {
        assertTrue(
            shouldSkipHomePagerStateDrive(
                hasSyncedPagerWithState = true,
                lastDrivenCategory = HomeCategory.RECOMMEND,
                currentCategory = HomeCategory.RECOMMEND
            )
        )
        assertFalse(
            shouldSkipHomePagerStateDrive(
                hasSyncedPagerWithState = true,
                lastDrivenCategory = HomeCategory.RECOMMEND,
                currentCategory = HomeCategory.LIVE
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
    fun pagerAnimation_skipsDuplicateStateSyncDuringProgrammaticTopTabSelection() {
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

    @Test
    fun initialTopTabPage_restoresPartitionDisplayedIndex() {
        val entries = listOf(
            HomeTopTabEntry.Category(HomeCategory.RECOMMEND),
            HomeTopTabEntry.Category(HomeCategory.POPULAR),
            HomeTopTabEntry.Partition
        )

        assertEquals(
            2,
            resolveHomeInitialTopTabPage(
                topTabEntries = entries,
                currentCategory = HomeCategory.RECOMMEND,
                displayedTabIndex = 2
            )
        )
        assertTrue(
            shouldTreatInitialHomePagerPageAsSyncedWithState(
                initialEntry = entries[2],
                currentCategory = HomeCategory.RECOMMEND
            )
        )
    }

    @Test
    fun initialTopTabPage_ignoresStaleCategoryDisplayedIndex() {
        val entries = listOf(
            HomeTopTabEntry.Category(HomeCategory.RECOMMEND),
            HomeTopTabEntry.Category(HomeCategory.POPULAR),
            HomeTopTabEntry.Partition
        )

        assertEquals(
            1,
            resolveHomeInitialTopTabPage(
                topTabEntries = entries,
                currentCategory = HomeCategory.POPULAR,
                displayedTabIndex = 0
            )
        )
    }
}
