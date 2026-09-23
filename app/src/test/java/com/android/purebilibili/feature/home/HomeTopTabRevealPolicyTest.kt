package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppSpacingTokens
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

    @Test
    fun hideTopTabs_alwaysHidesTopTabsEvenWhenIdleOrReturningFromDetail() {
        assertFalse(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = false,
                isReturningFromDetail = false,
                hideTopTabs = true
            )
        )
        assertFalse(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = false,
                isReturningFromDetail = true,
                hideTopTabs = true
            )
        )
    }

    @Test
    fun resolveEffectiveHomeTabRowHeight_whenHidden_returnsNone() {
        assertEquals(
            AppSpacingTokens.None,
            resolveEffectiveHomeTabRowHeight(
                hideTopTabs = true,
                defaultTabRowHeight = 44.dp
            )
        )
        assertEquals(
            44.dp,
            resolveEffectiveHomeTabRowHeight(
                hideTopTabs = false,
                defaultTabRowHeight = 44.dp
            )
        )
    }

    @Test
    fun resolveEffectiveHomeTopChromeHeight_accountsForHiddenTabs() {
        val unifiedHidden = resolveEffectiveHomeTopChromeHeight(
            hideTopTabs = true,
            useUnifiedPanel = true,
            searchBarHeight = 44.dp,
            tabRowHeight = 36.dp,
            unifiedPanelInnerPadding = 6.dp,
            searchToTabsSpacing = 4.dp
        )
        assertEquals(56.dp, unifiedHidden) // 44 + 6*2

        val nonUnifiedHidden = resolveEffectiveHomeTopChromeHeight(
            hideTopTabs = true,
            useUnifiedPanel = false,
            searchBarHeight = 44.dp,
            tabRowHeight = 36.dp,
            unifiedPanelInnerPadding = 6.dp,
            searchToTabsSpacing = 4.dp
        )
        assertEquals(44.dp, nonUnifiedHidden) // 44

        val unifiedShown = resolveEffectiveHomeTopChromeHeight(
            hideTopTabs = false,
            useUnifiedPanel = true,
            searchBarHeight = 44.dp,
            tabRowHeight = 36.dp,
            unifiedPanelInnerPadding = 6.dp,
            searchToTabsSpacing = 4.dp
        )
        assertEquals(96.dp, unifiedShown) // 44 + 36 + 12 + 4
    }
}
