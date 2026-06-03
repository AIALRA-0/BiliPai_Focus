package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.BottomBarSearchAutoExpandMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BottomBarLayoutPolicyTest {

    @Test
    fun `floating five items keeps compact width with safe per-item size`() {
        val policy = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )

        val perItemWidth = (policy.maxBarWidth - (policy.rowPadding * 2)) / 5
        assertTrue(policy.maxBarWidth.value > 340f)
        assertTrue(policy.horizontalPadding.value < 26f)
        assertTrue(perItemWidth.value >= 52f)
    }

    @Test
    fun `floating four items can use wider bar than five items`() {
        val policyForFour = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 4,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )
        val policyForFive = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = true
        )

        assertTrue(policyForFour.maxBarWidth.value > policyForFive.maxBarWidth.value)
    }

    @Test
    fun `kernelsu floating width uses intrinsic item width when space allows`() {
        val width = resolveKernelSuFloatingBottomBarWidth(
            containerWidth = 393.dp,
            itemCount = 4,
            minEdgePadding = 20.dp
        )

        assertEquals(312.dp, width)
    }

    @Test
    fun `kernelsu floating width keeps safe edge padding on crowded phones`() {
        val width = resolveKernelSuFloatingBottomBarWidth(
            containerWidth = 393.dp,
            itemCount = 5,
            minEdgePadding = 20.dp
        )

        assertEquals(353.dp, width)
    }

    @Test
    fun `kernelsu item slot width matches indicator geometry on crowded phones`() {
        val slotWidth = resolveKernelSuBottomBarItemSlotWidth(
            dockWidth = 353.dp,
            horizontalPadding = 4.dp,
            itemCount = 5
        )

        assertEquals(69.dp, slotWidth)
        assertEquals(
            314.5.dp,
            resolveKernelSuBottomBarItemCenterX(
                itemIndex = 4,
                itemWidth = slotWidth,
                horizontalPadding = 4.dp
            )
        )
    }

    @Test
    fun `kernelsu search entry shares safe floating width while collapsed`() {
        val layout = resolveKernelSuBottomBarSearchLayout(
            containerWidth = 393.dp,
            itemCount = 4,
            minEdgePadding = 20.dp,
            searchEnabled = true,
            searchExpanded = false
        )

        assertEquals(279.dp, layout.dockWidth)
        assertEquals(64.dp, layout.searchWidth)
        assertEquals(10.dp, layout.gap)
    }

    @Test
    fun `kernelsu search entry collapses dock to home capsule when expanded`() {
        val layout = resolveKernelSuBottomBarSearchLayout(
            containerWidth = 393.dp,
            itemCount = 4,
            minEdgePadding = 20.dp,
            searchEnabled = true,
            searchExpanded = true
        )

        assertEquals(resolveKernelSuBottomBarSearchCircleSize(), layout.dockWidth)
        assertEquals(279.dp, layout.searchWidth)
        assertEquals(10.dp, layout.gap)
    }

    @Test
    fun `kernelsu expanded home dock copies search circle size`() {
        assertEquals(64.dp, resolveKernelSuBottomBarSearchCircleSize())
        assertEquals(64.dp, resolveKernelSuBottomBarDockHeight(searchExpanded = false))
        assertEquals(resolveKernelSuBottomBarSearchCircleSize(), resolveKernelSuBottomBarDockHeight(searchExpanded = true))
        assertEquals(64.dp, resolveKernelSuBottomBarSearchHeight(searchExpanded = false))
        assertEquals(64.dp, resolveKernelSuBottomBarSearchHeight(searchExpanded = true))
    }

    @Test
    fun `kernelsu expanded home icon matches compact search icon size`() {
        assertEquals(28.dp, resolveKernelSuExpandedHomeIconSize())
        assertEquals(0.92f, resolveKernelSuExpandedHomeIconScale(), 0.001f)
    }

    @Test
    fun `bottom bar refraction capture follows full visible bar while search is enabled`() {
        val captureWidth = resolveBottomBarRefractionCaptureWidth(
            dockWidth = 279.dp,
            launchAdjustedSearchGap = 10.dp,
            searchWidth = 64.dp,
            searchEnabled = true
        )

        assertEquals(353.dp, captureWidth)
    }

    @Test
    fun `bottom bar refraction capture matches dock width without search`() {
        val captureWidth = resolveBottomBarRefractionCaptureWidth(
            dockWidth = 353.dp,
            launchAdjustedSearchGap = 10.dp,
            searchWidth = 64.dp,
            searchEnabled = false
        )

        assertEquals(353.dp, captureWidth)
    }

    @Test
    fun `home top automatically expands bottom search`() {
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 24f
            )
        )
    }

    @Test
    fun `bottom search auto collapses away from home top in top expand mode`() {
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 96f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = false,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                homeScrollOffsetPx = 0f
            )
        )
    }

    @Test
    fun `bottom search auto expansion follows threshold bucket not raw scroll pixels`() {
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearchAtThreshold(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                isPastTopThreshold = false
            )
        )
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearchAtThreshold(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                isPastTopThreshold = true
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearchAtThreshold(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_AT_HOME_TOP,
                isPastTopThreshold = true
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearchAtThreshold(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.DISABLED,
                isPastTopThreshold = false
            )
        )
    }

    @Test
    fun `bottom search entry only renders on searchable home item`() {
        assertEquals(
            true,
            resolveBottomBarSearchEnabledForItem(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true
            )
        )
        assertEquals(
            false,
            resolveBottomBarSearchEnabledForItem(
                currentItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true
            )
        )
        assertEquals(
            false,
            resolveBottomBarSearchEnabledForItem(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = false
            )
        )
    }

    @Test
    fun `bottom search auto expands away from home top in scroll expand mode`() {
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            true,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                homeScrollOffsetPx = 96f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.EXPAND_WHEN_SCROLLING_DOWN,
                homeScrollOffsetPx = 96f
            )
        )
    }

    @Test
    fun `bottom search auto expansion can be disabled`() {
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.DISABLED,
                homeScrollOffsetPx = 0f
            )
        )
        assertEquals(
            false,
            shouldAutoExpandBottomBarSearch(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                autoExpandMode = BottomBarSearchAutoExpandMode.DISABLED,
                homeScrollOffsetPx = 96f
            )
        )
    }

    @Test
    fun `manual bottom search override wins over auto expansion`() {
        assertEquals(
            true,
            resolveEffectiveBottomBarSearchExpanded(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                shouldAutoExpand = false,
                expansionOverride = BottomBarSearchExpansionOverride.EXPANDED
            )
        )
        assertEquals(
            false,
            resolveEffectiveBottomBarSearchExpanded(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                shouldAutoExpand = true,
                expansionOverride = BottomBarSearchExpansionOverride.COLLAPSED
            )
        )
    }

    @Test
    fun `bottom search override reset only follows route enabled state and threshold bucket`() {
        assertEquals(
            false,
            shouldResetBottomBarSearchExpansionOverride(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                shouldAutoExpand = true,
                isPastTopThreshold = false
            )
        )
        assertEquals(
            true,
            shouldResetBottomBarSearchExpansionOverride(
                currentItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                shouldAutoExpand = false,
                isPastTopThreshold = true
            )
        )
        assertEquals(
            true,
            shouldResetBottomBarSearchExpansionOverride(
                currentItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                shouldAutoExpand = false,
                isPastTopThreshold = false
            )
        )
    }

    @Test
    fun `bottom bar search performance guards keep expensive layers transient`() {
        assertEquals(
            false,
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 0.01f,
                isFeedScrollInProgress = false
            )
        )
        assertEquals(
            true,
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 0.01f,
                isFeedScrollInProgress = false,
                isBottomBarInteractionActive = true
            )
        )
        assertEquals(
            false,
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 0.001f,
                isFeedScrollInProgress = false
            )
        )
        assertEquals(
            false,
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = false,
                hasBackdrop = true,
                captureProgress = 0.3f,
                isFeedScrollInProgress = false
            )
        )
        assertEquals(
            true,
            shouldComposeBottomBarDockContent(
                dockContentAlpha = 0.02f,
                effectiveSearchExpanded = true
            )
        )
        assertEquals(
            false,
            shouldComposeBottomBarDockContent(
                dockContentAlpha = 0f,
                effectiveSearchExpanded = true
            )
        )
        assertEquals(
            true,
            shouldComposeBottomBarDockContent(
                dockContentAlpha = 0f,
                effectiveSearchExpanded = false
            )
        )
    }

    @Test
    fun `feed scrolling skips idle bottom bar refraction capture`() {
        assertEquals(
            false,
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 0.01f,
                isFeedScrollInProgress = true,
                isBottomBarInteractionActive = false
            )
        )
    }

    @Test
    fun `feed scrolling keeps bottom bar refraction capture during direct interaction`() {
        assertEquals(
            true,
            shouldRenderBottomBarRefractionCapture(
                glassEnabled = true,
                hasBackdrop = true,
                captureProgress = 0.01f,
                isFeedScrollInProgress = true,
                isBottomBarInteractionActive = true
            )
        )
    }

    @Test
    fun `non home routes do not expand bottom search`() {
        assertEquals(
            false,
            resolveEffectiveBottomBarSearchExpanded(
                currentItem = BottomNavItem.HISTORY,
                bottomBarSearchEnabled = true,
                shouldAutoExpand = true,
                expansionOverride = BottomBarSearchExpansionOverride.EXPANDED
            )
        )
    }

    @Test
    fun `home icon click toggles search and dock only while already on home`() {
        assertEquals(
            BottomBarSearchExpansionOverride.EXPANDED,
            resolveBottomBarSearchExpansionOverrideOnNavItemClick(
                currentItem = BottomNavItem.HOME,
                clickedItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                effectiveSearchExpanded = false
            )
        )
        assertEquals(
            BottomBarSearchExpansionOverride.COLLAPSED,
            resolveBottomBarSearchExpansionOverrideOnNavItemClick(
                currentItem = BottomNavItem.HOME,
                clickedItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                effectiveSearchExpanded = true
            )
        )
        assertEquals(
            null,
            resolveBottomBarSearchExpansionOverrideOnNavItemClick(
                currentItem = BottomNavItem.HOME,
                clickedItem = BottomNavItem.DYNAMIC,
                bottomBarSearchEnabled = true,
                effectiveSearchExpanded = false
            )
        )
        assertEquals(
            null,
            resolveBottomBarSearchExpansionOverrideOnNavItemClick(
                currentItem = BottomNavItem.HISTORY,
                clickedItem = BottomNavItem.HOME,
                bottomBarSearchEnabled = true,
                effectiveSearchExpanded = false
            )
        )
    }

    @Test
    fun `docked mode stays full width with no horizontal inset`() {
        val policy = resolveBottomBarLayoutPolicy(
            containerWidth = 393.dp,
            itemCount = 5,
            isTablet = false,
            labelMode = 0,
            isFloating = false
        )

        assertEquals(0.dp, policy.horizontalPadding)
        assertEquals(393.dp, policy.maxBarWidth)
    }

    @Test
    fun `floating default bar trims height while keeping touch comfort`() {
        assertEquals(58f, resolveBottomBarFloatingHeightDp(labelMode = 1, isTablet = false))
        assertEquals(12f, resolveBottomBarBottomPaddingDp(isFloating = true, isTablet = false))
    }
}
