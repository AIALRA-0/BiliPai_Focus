package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardWidthPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeFeedGridPolicyTest {

    @Test
    fun autoPresetKeepsExistingAutomaticColumns() {
        assertEquals(
            2,
            resolveHomeFeedGridColumns(
                contentWidthDp = 393,
                displayMode = 0,
                fixedColumnCount = 0,
                cardWidthPreset = HomeFeedCardWidthPreset.AUTO
            )
        )
        assertEquals(
            6,
            resolveHomeFeedGridColumns(
                contentWidthDp = 1280,
                displayMode = 0,
                fixedColumnCount = 0,
                cardWidthPreset = HomeFeedCardWidthPreset.AUTO
            )
        )
    }

    @Test
    fun fixedColumnCountTakesPriorityOverCardWidthPreset() {
        assertEquals(
            5,
            resolveHomeFeedGridColumns(
                contentWidthDp = 1280,
                displayMode = 0,
                fixedColumnCount = 5,
                cardWidthPreset = HomeFeedCardWidthPreset.ULTRA_WIDE
            )
        )
    }

    @Test
    fun doubleGridKeepsTwoColumnsOnCompactPhones() {
        assertEquals(
            2,
            resolveHomeFeedGridColumns(
                contentWidthDp = 320,
                displayMode = 0,
                fixedColumnCount = 0,
                cardWidthPreset = HomeFeedCardWidthPreset.AUTO
            )
        )
    }

    @Test
    fun widePresetsReduceTabletColumnsAndMakeCardsWider() {
        val auto = resolveHomeFeedGridColumns(
            contentWidthDp = 1280,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.AUTO
        )
        val wide = resolveHomeFeedGridColumns(
            contentWidthDp = 1280,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.WIDE
        )
        val ultraWide = resolveHomeFeedGridColumns(
            contentWidthDp = 1280,
            displayMode = 0,
            fixedColumnCount = 0,
            cardWidthPreset = HomeFeedCardWidthPreset.ULTRA_WIDE
        )

        assertTrue(wide < auto)
        assertTrue(ultraWide <= wide)
        assertEquals(4, wide)
        assertEquals(4, ultraWide)
    }

    @Test
    fun storyDisplayModeKeepsSingleColumnPolicyAndIgnoresPreset() {
        assertEquals(
            1,
            resolveHomeFeedGridColumns(
                contentWidthDp = 393,
                displayMode = 1,
                fixedColumnCount = 0,
                cardWidthPreset = HomeFeedCardWidthPreset.ULTRA_WIDE
            )
        )
        assertEquals(
            2,
            resolveHomeFeedGridColumns(
                contentWidthDp = 1280,
                displayMode = 1,
                fixedColumnCount = 0,
                cardWidthPreset = HomeFeedCardWidthPreset.ULTRA_WIDE
            )
        )
    }
}
