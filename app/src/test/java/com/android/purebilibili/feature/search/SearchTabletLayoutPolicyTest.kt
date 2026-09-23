package com.android.purebilibili.feature.search

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTabletLayoutPolicyTest {

    @Test
    fun coverWidth_followsExplicitColumnsOnLargeScreens() {
        assertEquals(458f, resolveSearchGridCardWidthDp(960f, 220f, 16f, 12f, fixedColumnCount = 2))
        assertEquals(928f, resolveSearchGridCardWidthDp(960f, 220f, 16f, 12f, fixedColumnCount = 1))
    }

    @Test
    fun coverWidth_accountsForPhoneColumnsPaddingAndSpacing() {
        assertEquals(184.5f, resolveSearchGridCardWidthDp(393f, 160f, 8f, 8f))
    }

    @Test
    fun coverWidth_usesActualNarrowPaneInsteadOfDeviceWidth() {
        assertEquals(264f, resolveSearchGridCardWidthDp(280f, 200f, 8f, 8f))
        assertEquals(223f, resolveSearchGridCardWidthDp(960f, 220f, 16f, 12f))
    }

    @Test
    fun compactWidth_usesPhoneDefaults() {
        val policy = resolveSearchLayoutPolicy(widthDp = 393)

        assertEquals(160, policy.resultGridMinItemWidthDp)
        assertEquals(8, policy.resultGridSpacingDp)
        assertEquals(8, policy.resultHorizontalPaddingDp)
        assertEquals(2, policy.hotSearchColumns)
    }

    @Test
    fun mediumTablet_keepsReadableDensity() {
        val policy = resolveSearchLayoutPolicy(widthDp = 720)

        assertEquals(200, policy.resultGridMinItemWidthDp)
        assertEquals(12, policy.resultGridSpacingDp)
        assertEquals(16, policy.resultHorizontalPaddingDp)
        assertEquals(2, policy.hotSearchColumns)
        assertEquals(1f, policy.leftPaneWeight)
        assertEquals(1f, policy.rightPaneWeight)
    }

    @Test
    fun expandedTablet_increasesColumnsForDiscovery() {
        val policy = resolveSearchLayoutPolicy(widthDp = 1280)

        assertEquals(220, policy.resultGridMinItemWidthDp)
        assertEquals(20, policy.resultHorizontalPaddingDp)
        assertEquals(3, policy.hotSearchColumns)
        assertEquals(1.05f, policy.leftPaneWeight)
        assertEquals(0.95f, policy.rightPaneWeight)
    }

    @Test
    fun ultraWideTablet_expandsColumnsAndCardWidth() {
        val policy = resolveSearchLayoutPolicy(widthDp = 1920)

        assertEquals(260, policy.resultGridMinItemWidthDp)
        assertEquals(24, policy.resultHorizontalPaddingDp)
        assertEquals(4, policy.hotSearchColumns)
        assertEquals(1.15f, policy.leftPaneWeight)
        assertEquals(0.85f, policy.rightPaneWeight)
    }

    @Test
    fun ultraWide_prefersHigherColumnCount() {
        val policy = resolveSearchLayoutPolicy(widthDp = 1920)

        assertTrue(policy.resultGridMinItemWidthDp >= 220)
        assertEquals(4, policy.hotSearchColumns)
    }

    @Test
    fun splitLayout_threshold_isExpanded_only() {
        assertEquals(false, shouldUseSearchSplitLayout(widthDp = 720))
        assertEquals(true, shouldUseSearchSplitLayout(widthDp = 1024))
    }

    @Test
    fun searchContentMaxWidth_isAlignedWithHomeFeed() {
        assertEquals(1280.dp, resolveSearchMaxContentWidth())
        assertEquals(840.dp, resolveSearchSingleColumnResultMaxWidth())

        assertEquals(393.dp, resolveSearchContentWidth(isExpandedScreen = false, widthDp = 393.dp))
        assertEquals(1280.dp, resolveSearchContentWidth(isExpandedScreen = true, widthDp = 1600.dp))
        assertEquals(1024.dp, resolveSearchContentWidth(isExpandedScreen = true, widthDp = 1024.dp))
    }

    @Test
    fun searchVideoGridColumns_adaptsForScreenSizesAndSettings() {
        // Phone (compact): 2 columns
        assertEquals(
            2,
            resolveSearchVideoGridColumns(
                singleColumn = false,
                contentWidthDp = 393,
                widthSizeClass = WindowWidthSizeClass.Compact
            )
        )

        // Single column toggle: strictly 1 column
        assertEquals(
            1,
            resolveSearchVideoGridColumns(
                singleColumn = true,
                contentWidthDp = 1280,
                widthSizeClass = WindowWidthSizeClass.Expanded
            )
        )

        // Tablet (Expanded, 1280dp): automatically resolves to 6 columns
        assertEquals(
            6,
            resolveSearchVideoGridColumns(
                singleColumn = false,
                contentWidthDp = 1280,
                widthSizeClass = WindowWidthSizeClass.Expanded
            )
        )

        // User explicit fixed column count is respected
        assertEquals(
            4,
            resolveSearchVideoGridColumns(
                singleColumn = false,
                contentWidthDp = 1280,
                fixedColumnCount = 4,
                widthSizeClass = WindowWidthSizeClass.Expanded
            )
        )
    }
}
