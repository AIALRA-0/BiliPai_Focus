package com.android.purebilibili.feature.home.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeScrollCoordinatorTest {

    @Test
    fun preScroll_updatesHeaderOffsetWithinBounds() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = -40f,
            deltaY = -80f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f
        )

        assertEquals(-120f, result.headerOffsetPx)
        assertNull(result.bottomBarVisibilityIntent)
        assertNull(result.globalScrollOffset)
    }

    @Test
    fun headerCollapseDisabled_resetsOffsetToZero() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = -64f,
            deltaY = -12f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = false,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 40f
        )

        assertEquals(0f, result.headerOffsetPx)
    }

    @Test
    fun bottomBarAutoHideDisabled_returnsNoIntent() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -48f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f
        )

        assertNull(result.bottomBarVisibilityIntent)
    }

    @Test
    fun tinyDelta_doesNotToggleBottomBarVisibility() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -4f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = true,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f,
            bottomBarVisibilityThresholdPx = 10f
        )

        assertNull(result.bottomBarVisibilityIntent)
    }

    @Test
    fun upwardScroll_hidesBottomBarWhenAutoHideEnabled() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -24f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = true,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 10f
        )

        assertEquals(BottomBarVisibilityIntent.HIDE, result.bottomBarVisibilityIntent)
    }

    @Test
    fun liquidGlassEnabled_updatesGlobalOffset() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = 0f,
            deltaY = -8f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = true,
            currentGlobalScrollOffset = 120f
        )

        assertEquals(128f, result.globalScrollOffset)
    }

    @Test
    fun settledPageAtTop_expandsCollapsedHeader() {
        val result = shouldExpandHomeHeaderForSettledPage(
            currentHeaderOffsetPx = -96f,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0
        )

        assertEquals(true, result)
    }

    @Test
    fun settledPageAwayFromTop_keepsCollapsedHeader() {
        val result = shouldExpandHomeHeaderForSettledPage(
            currentHeaderOffsetPx = -96f,
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 0
        )

        assertEquals(false, result)
    }

    @Test
    fun expandedHeader_doesNotNeedResetOnPageSettle() {
        val result = shouldExpandHomeHeaderForSettledPage(
            currentHeaderOffsetPx = 0f,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0
        )

        assertEquals(false, result)
    }

    @Test
    fun upwardScrollAwayFromTop_keepsHeaderCollapsed() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = -120f,
            deltaY = 36f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = false,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 40f
        )

        assertEquals(-120f, result.headerOffsetPx)
    }

    @Test
    fun upwardScrollAtTop_allowsHeaderToExpand() {
        val result = reduceHomePreScroll(
            currentHeaderOffsetPx = -120f,
            deltaY = 36f,
            minHeaderOffsetPx = -120f,
            canRevealHeader = true,
            isHeaderCollapseEnabled = true,
            isBottomBarAutoHideEnabled = false,
            useSideNavigation = false,
            liquidGlassEnabled = false,
            currentGlobalScrollOffset = 40f
        )

        assertEquals(-84f, result.headerOffsetPx)
    }

    @Test
    fun settledTopPage_resolvesExpandedHeaderOffset() {
        val result = resolveHomeHeaderOffsetForSettledPage(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            maxHeaderCollapsePx = 120f
        )

        assertEquals(0f, result)
    }

    @Test
    fun settledFirstItemScroll_keepsHeaderCollapsedUntilExactTop() {
        val result = resolveHomeHeaderOffsetForSettledPage(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 36,
            maxHeaderCollapsePx = 120f
        )

        assertEquals(-120f, result)
    }

    @Test
    fun settledPagePastFirstItem_resolvesFullyCollapsedHeaderOffset() {
        val result = resolveHomeHeaderOffsetForSettledPage(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 0,
            maxHeaderCollapsePx = 120f
        )

        assertEquals(-120f, result)
    }
}
