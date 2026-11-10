package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardWidthPreset
import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedGridPolicyTabletTest {

    @Test
    fun gridColumnsCapAtFourOnCompact() {
        val columns = resolveHomeFeedGridColumns(
            contentWidthDp = 1200,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Compact
        )
        assertEquals(4, columns)
    }

    @Test
    fun gridColumnsCapAtSixOnMediumAndExpanded() {
        val medium = resolveHomeFeedGridColumns(
            contentWidthDp = 1200,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Medium
        )
        val expanded = resolveHomeFeedGridColumns(
            contentWidthDp = 1200,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Expanded
        )
        assertEquals(6, medium)
        assertEquals(6, expanded)
    }

    @Test
    fun gridColumnsCapAtSevenOnLarge() {
        val columns = resolveHomeFeedGridColumns(
            contentWidthDp = 1400,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.Large
        )
        assertEquals(7, columns)
    }

    @Test
    fun gridColumnsCapAtEightOnExtraLarge() {
        val columns = resolveHomeFeedGridColumns(
            contentWidthDp = 1600,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO,
            widthSizeClass = WindowWidthSizeClass.ExtraLarge
        )
        assertEquals(8, columns)
    }

    @Test
    fun legacyWindowRatioDoesNotOverrideSelectedCoverStyle() {
        val compactRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Compact)
        val mediumRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Medium)
        val expandedRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Expanded)
        val largeRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.Large)
        val extraLargeRatio = resolveHomeFeedCardAspectRatio(WindowWidthSizeClass.ExtraLarge)

        assertEquals(16f / 10f, compactRatio)
        assertEquals(16f / 10f, mediumRatio)
        assertEquals(16f / 9f, expandedRatio)
        assertEquals(16f / 9f, largeRatio)
        assertEquals(16f / 9f, extraLargeRatio)
        assertEquals(
            16f / 10f,
            resolveHomeFeedCoverAspectRatio(
                style = HomeFeedCardStyle.BILIPAI,
                gridColumns = 6,
                widthSizeClass = WindowWidthSizeClass.Expanded,
            ),
        )
    }

    @Test
    fun fixedOddColumnPreferenceIsClampedToTheAvailableSafePane() {
        assertEquals(2, com.android.purebilibili.core.ui.adaptive.resolveHingeSafeFeedColumns(5, 440f))
        assertEquals(1, com.android.purebilibili.core.ui.adaptive.resolveHingeSafeFeedColumns(3, 170f))
        assertEquals(1, com.android.purebilibili.core.ui.adaptive.resolveHingeSafeFeedColumns(1, 640f))
        assertEquals(1, com.android.purebilibili.core.ui.adaptive.resolveHingeSafeFeedColumns(2, 440f, minCardWidthDp = 280))
    }
}
