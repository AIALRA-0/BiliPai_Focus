package com.android.purebilibili.feature.personal

import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalListLayoutPolicyTest {

    @Test
    fun columnCount_matchesBiliPaiMaxExtentBehavior() {
        assertEquals(1, resolvePersonalListColumnCount(360f))
        assertEquals(1, resolvePersonalListColumnCount(480f))
        assertEquals(2, resolvePersonalListColumnCount(600f))
        assertEquals(2, resolvePersonalListColumnCount(960f))
        assertEquals(3, resolvePersonalListColumnCount(1000f))
    }

    @Test
    fun itemWidth_accountsForInterColumnSpacing() {
        assertEquals(294f, resolvePersonalListItemWidthDp(600f, 2, 12f))
    }

    @Test
    fun cardHeight_scalesOnlyWhenFontScaleGrows() {
        assertEquals(90f, resolvePersonalMediaCardMinHeightDp(0.85f))
        assertEquals(90f, resolvePersonalMediaCardMinHeightDp(1f))
        assertEquals(117f, resolvePersonalMediaCardMinHeightDp(1.3f), absoluteTolerance = 0.001f)
    }

    @Test
    fun mediaRatios_areStable() {
        assertEquals(16f / 10f, PERSONAL_LIST_HORIZONTAL_COVER_ASPECT_RATIO)
        assertEquals(140, PERSONAL_LIST_HORIZONTAL_COVER_WIDTH_DP)
        assertEquals(3f / 4f, PERSONAL_LIST_POSTER_ASPECT_RATIO)
        assertEquals(
            87.5f,
            PERSONAL_LIST_HORIZONTAL_COVER_WIDTH_DP / PERSONAL_LIST_HORIZONTAL_COVER_ASPECT_RATIO,
        )
    }
}
