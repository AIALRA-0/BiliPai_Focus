package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeInteractionMotionBudgetPolicyTest {

    @Test
    fun activePagerOrFeedMotion_reducesHomeBudget() {
        assertEquals(
            HomeInteractionMotionBudget.REDUCED,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = true,
                isProgrammaticPageSwitchInProgress = false,
                isFeedScrolling = false
            )
        )
        assertEquals(
            HomeInteractionMotionBudget.REDUCED,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = false,
                isProgrammaticPageSwitchInProgress = true,
                isFeedScrolling = false
            )
        )
        assertEquals(
            HomeInteractionMotionBudget.REDUCED,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = false,
                isProgrammaticPageSwitchInProgress = false,
                isFeedScrolling = true
            )
        )
    }

    @Test
    fun idleHomeState_keepsFullBudget() {
        assertEquals(
            HomeInteractionMotionBudget.FULL,
            resolveHomeInteractionMotionBudget(
                isPagerScrolling = false,
                isProgrammaticPageSwitchInProgress = false,
                isFeedScrolling = false
            )
        )
    }

    @Test
    fun reducedBudget_onlyAutoScrollsTabsWhenTargetIsOutOfViewport() {
        assertFalse(
            shouldAnimateTopTabAutoScroll(
                selectedIndex = 2,
                firstVisibleIndex = 0,
                lastVisibleIndex = 4,
                budget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertTrue(
            shouldAnimateTopTabAutoScroll(
                selectedIndex = 5,
                firstVisibleIndex = 0,
                lastVisibleIndex = 4,
                budget = HomeInteractionMotionBudget.REDUCED
            )
        )
    }

    @Test
    fun fullBudget_keepsLeadingTabsVisibleWhenSelectionAlreadyInViewport() {
        assertFalse(
            shouldAnimateTopTabAutoScroll(
                selectedIndex = 1,
                firstVisibleIndex = 0,
                lastVisibleIndex = 3,
                budget = HomeInteractionMotionBudget.FULL
            )
        )
    }

    @Test
    fun collapsedTopTabs_disableViewportSync() {
        assertFalse(
            resolveHomeTopTabViewportSyncEnabled(
                currentTabHeightDp = 0f,
                tabAlpha = 1f,
                tabContentAlpha = 1f
            )
        )
        assertFalse(
            resolveHomeTopTabViewportSyncEnabled(
                currentTabHeightDp = 52f,
                tabAlpha = 0f,
                tabContentAlpha = 1f
            )
        )
    }

    @Test
    fun visibleTopTabs_keepViewportSyncEnabled() {
        assertTrue(
            resolveHomeTopTabViewportSyncEnabled(
                currentTabHeightDp = 52f,
                tabAlpha = 1f,
                tabContentAlpha = 1f
            )
        )
    }

    @Test
    fun pagerSwipe_skipsTopTabViewportSyncWhenTargetRemainsVisible() {
        assertFalse(
            shouldSyncHomeTopTabViewport(
                pagerIsScrolling = true,
                targetIsOutsideViewport = false
            )
        )
        assertTrue(
            shouldSyncHomeTopTabViewport(
                pagerIsScrolling = true,
                targetIsOutsideViewport = true
            )
        )
    }

    @Test
    fun idlePager_allowsTopTabViewportSettleCorrection() {
        assertTrue(
            shouldSyncHomeTopTabViewport(
                pagerIsScrolling = false,
                targetIsOutsideViewport = false
            )
        )
    }

    @Test
    fun activePagerSwipe_prefersPagerTargetForTopTabViewportAnchor() {
        assertEquals(
            4,
            resolveTopTabViewportAnchorIndex(
                selectedIndex = 2,
                pagerCurrentPage = 2,
                pagerTargetPage = 4,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun idlePagerViewportAnchor_prefersSettledPagerPage() {
        assertEquals(
            3,
            resolveTopTabViewportAnchorIndex(
                selectedIndex = 1,
                pagerCurrentPage = 3,
                pagerTargetPage = 4,
                pagerIsScrolling = false
            )
        )
    }

    @Test
    fun pagerSwipePosition_tracksTargetPageContinuously() {
        assertEquals(
            0.35f,
            resolveTopTabPagerPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
        assertEquals(
            0.65f,
            resolveTopTabPagerPosition(
                selectedIndex = 1,
                pagerCurrentPage = 1,
                pagerTargetPage = 0,
                pagerCurrentPageOffsetFraction = -0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun pagerSwipePosition_usesLiveOffsetWhenTargetDirectionIsStale() {
        assertEquals(
            2.65f,
            resolveTopTabPagerPosition(
                selectedIndex = 3,
                pagerCurrentPage = 3,
                pagerTargetPage = 4,
                pagerCurrentPageOffsetFraction = -0.35f,
                pagerIsScrolling = true
            )
        )
        assertEquals(
            4.35f,
            resolveTopTabPagerPosition(
                selectedIndex = 4,
                pagerCurrentPage = 4,
                pagerTargetPage = 3,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun pagerSwipePosition_tracksOffsetBeforeTargetPageChanges() {
        assertEquals(
            0.35f,
            resolveTopTabPagerPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 0,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
        assertEquals(
            0.65f,
            resolveTopTabPagerPosition(
                selectedIndex = 1,
                pagerCurrentPage = 1,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = -0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun idlePagerPosition_prefersSettledPagerPage() {
        assertEquals(
            1f,
            resolveTopTabPagerPosition(
                selectedIndex = 2,
                pagerCurrentPage = 1,
                pagerTargetPage = 3,
                pagerCurrentPageOffsetFraction = 0.4f,
                pagerIsScrolling = false
            )
        )
    }

    @Test
    fun topTabIndicatorRenderPosition_tracksPagerOffsetWhileUserSwipesContent() {
        assertEquals(
            0.35f,
            resolveTopTabIndicatorRenderPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun topTabSelectedContentPosition_tracksPagerOffsetWhileUserSwipesContent() {
        assertEquals(
            0.35f,
            resolveTopTabSelectedContentPosition(
                selectedIndex = 0,
                pagerCurrentPage = 0,
                pagerTargetPage = 1,
                pagerCurrentPageOffsetFraction = 0.35f,
                pagerIsScrolling = true
            )
        )
    }

    @Test
    fun topTabIndicatorRenderPosition_prefersSettledPagerPageWhenIdle() {
        assertEquals(
            2f,
            resolveTopTabIndicatorRenderPosition(
                selectedIndex = 0,
                pagerCurrentPage = 2,
                pagerTargetPage = 3,
                pagerCurrentPageOffsetFraction = 0.4f,
                pagerIsScrolling = false
            )
        )
    }

    @Test
    fun md3TopTabViewportPosition_matchesPagerProgressWithinVisibleSlots() {
        assertEquals(
            1.35f,
            resolveMd3TopTabViewportPosition(
                visibleIndices = listOf(0, 1, 2, 3),
                absolutePagerPosition = 1.35f
            ),
            0.001f
        )
    }

    @Test
    fun md3TopTabViewportPosition_interpolatesAcrossPinnedTailSlot() {
        assertEquals(
            2.6f,
            resolveMd3TopTabViewportPosition(
                visibleIndices = listOf(0, 1, 2, 4),
                absolutePagerPosition = 3.2f
            ),
            0.001f
        )
    }

    @Test
    fun md3TopTabIndicatorTranslation_tracksFractionalPagerPosition() {
        assertEquals(
            121f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 1.35f,
                itemWidthPx = 100f,
                rowScrollOffsetPx = 50f,
                indicatorWidthPx = 28f,
                contentPaddingPx = 0f
            ),
            0.001f
        )
    }

    @Test
    fun md3TopTabIndicatorTranslation_appliesContentPaddingAndViewportScroll() {
        assertEquals(
            125f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 1.35f,
                itemWidthPx = 100f,
                rowScrollOffsetPx = 50f,
                indicatorWidthPx = 28f,
                contentPaddingPx = 4f
            ),
            0.001f
        )
    }

    @Test
    fun md3TopTabIndicatorTranslation_keepsPaddingWhenSizeInvalid() {
        assertEquals(
            6f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 1.35f,
                itemWidthPx = 0f,
                rowScrollOffsetPx = 50f,
                indicatorWidthPx = 28f,
                contentPaddingPx = 6f
            ),
            0.001f
        )
    }

    @Test
    fun md3NativeUnderline_stretchesBetweenTabCentersThenSettles() {
        assertEquals(
            Md3TopTabUnderlineBounds(36f, 28f),
            resolveMd3TopTabUnderlineBounds(
                absolutePagerPosition = 0f,
                itemWidthPx = 100f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 28f,
            ),
        )
        val midpoint = resolveMd3TopTabUnderlineBounds(
                absolutePagerPosition = 0.5f,
                itemWidthPx = 100f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 28f,
            )
        assertEquals(65.289f, midpoint.translationXPx, 0.001f)
        assertEquals(69.421f, midpoint.widthPx, 0.001f)
        assertEquals(
            Md3TopTabUnderlineBounds(136f, 28f),
            resolveMd3TopTabUnderlineBounds(
                absolutePagerPosition = 1f,
                itemWidthPx = 100f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 28f,
            ),
        )
    }

    @Test
    fun iosTopTabCapsuleTranslation_prefersMeasuredSelectedItemLeft() {
        assertEquals(
            184f,
            resolveIosTopTabCapsuleTargetTranslationPx(
                measuredSelectedItemLeftPx = 184f,
                absolutePagerPosition = 0f,
                itemWidthPx = 160f,
                rowScrollOffsetPx = 0f,
                contentPaddingPx = 2f
            ),
            0.001f
        )
    }

    @Test
    fun iosTopTabCapsuleTranslation_ignoresMeasuredSelectedItemLeftDuringPagerSwipe() {
        assertEquals(
            66f,
            resolveIosTopTabCapsuleTargetTranslationPx(
                measuredSelectedItemLeftPx = 184f,
                absolutePagerPosition = 0.4f,
                itemWidthPx = 160f,
                rowScrollOffsetPx = 0f,
                contentPaddingPx = 2f,
                followPagerPosition = true
            ),
            0.001f
        )
    }

    @Test
    fun iosTopTabCapsule_disablesSpringAnimationDuringPagerDrag() {
        assertFalse(shouldAnimateIosTopTabCapsule(pagerIsDragging = true, pagerIsScrolling = false))
        assertFalse(shouldAnimateIosTopTabCapsule(pagerIsDragging = false, pagerIsScrolling = true))
        assertTrue(shouldAnimateIosTopTabCapsule(pagerIsDragging = false, pagerIsScrolling = false))
    }

    @Test
    fun shouldAnimateMd3TopTabUnderline_onlyWhenNotDraggingOrHoldingIndicator() {
        assertTrue(
            shouldAnimateMd3TopTabUnderline(
                pagerIsDragging = false,
                topTabIndicatorOwnsPosition = false
            )
        )
        assertFalse(
            shouldAnimateMd3TopTabUnderline(
                pagerIsDragging = true,
                topTabIndicatorOwnsPosition = false
            )
        )
        assertFalse(
            shouldAnimateMd3TopTabUnderline(
                pagerIsDragging = false,
                topTabIndicatorOwnsPosition = true
            )
        )
    }

    @Test
    fun resolveMd3TopTabTargetBounds_centersIndicatorOverTargetSlot() {
        val bounds = resolveMd3TopTabTargetBounds(
            targetIndex = 2,
            itemWidthPx = 100f,
            indicatorWidthPx = 28f,
            contentPaddingPx = 10f,
        )
        assertEquals(246f, bounds.leftPx, 0.001f)
        assertEquals(274f, bounds.rightPx, 0.001f)
    }

    @Test
    fun resolveMd3TopTabUnderlineTapBounds_computesOffsetAndWidth() {
        val bounds = resolveMd3TopTabUnderlineTapBounds(
            animatedLeftPx = 246f,
            animatedRightPx = 274f,
            rowScrollOffsetPx = 30f,
        )
        assertEquals(216f, bounds.translationXPx, 0.001f)
        assertEquals(28f, bounds.widthPx, 0.001f)
    }

    @Test
    fun resolveMd3TopTabTapContentPosition_recoversContinuousFraction() {
        val pos = resolveMd3TopTabTapContentPosition(
            animatedLeftPx = 246f,
            animatedRightPx = 274f,
            itemWidthPx = 100f,
            contentPaddingPx = 10f,
            fallbackIndex = 2,
            categoryCount = 5,
        )
        assertEquals(2f, pos, 0.001f)
    }

    @Test
    fun md3TapIndicatorEasing_leadingEdgeMovesFasterThanTrailingEdge() {
        for (i in 1..9) {
            val fraction = i / 10f
            val dec = Md3TopTabIndicatorDecelerate.transform(fraction)
            val acc = Md3TopTabIndicatorAccelerate.transform(fraction)
            assertTrue(
                dec > acc,
                "Decelerate ($dec) should be greater than Accelerate ($acc) at fraction $fraction to stretch underline"
            )
        }
    }
}
