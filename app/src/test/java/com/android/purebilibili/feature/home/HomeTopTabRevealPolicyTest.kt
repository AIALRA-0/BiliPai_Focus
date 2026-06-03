package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTopTabRevealPolicyTest {

    @Test
    fun returningFromVideo_withCardTransition_showsTopTabsImmediately() {
        assertEquals(
            0L,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun returningFromVideo_withoutCardTransition_showsTopTabsImmediately() {
        assertEquals(
            0L,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = true,
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun normalHomeEntry_keepsTopTabsImmediate() {
        assertEquals(
            0L,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = false,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
    }

    @Test
    fun quickReturn_withCardTransition_alsoShowsTopTabsImmediately() {
        assertEquals(
            0L,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = true,
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
    }

    @Test
    fun forwardNavigationToDetail_hidesTopTabsImmediately() {
        assertFalse(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = true,
                isReturningFromDetail = false
            )
        )
    }

    @Test
    fun settlingAfterReturn_hidesTopTabs() {
        assertFalse(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = true,
                isForwardNavigatingToDetail = false,
                isReturningFromDetail = false
            )
        )
    }

    @Test
    fun idleHome_showsTopTabs() {
        assertTrue(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = false,
                isReturningFromDetail = false
            )
        )
    }

    @Test
    fun idleHome_keepsCollapsedTopTabsHostVisibleForScrollRecovery() {
        assertTrue(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = false,
                isReturningFromDetail = false,
                topTabsCollapsed = true
            )
        )
    }

    @Test
    fun returningFromDetail_keepsVisibleTopTabsVisibleEvenIfFlagsWereHidden() {
        assertTrue(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = true,
                isForwardNavigatingToDetail = true,
                isReturningFromDetail = true,
                topTabsCollapsed = false
            )
        )
    }

    @Test
    fun returningFromDetail_keepsCollapsedTopTabsHostVisibleForRecovery() {
        assertTrue(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = false,
                isReturningFromDetail = true,
                topTabsCollapsed = true
            )
        )
    }

    @Test
    fun defaultScrollBehavior_collapsesSearchRowButKeepsTopTabsDockVisible() {
        assertTrue(shouldAutoCollapseHomeSearchRow())
        assertFalse(shouldCollapseHomeTopTabsWithSearchRow())
    }
}
