package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntRect
import kotlin.test.Test
import kotlin.test.assertEquals

class EpisodeSheetLayoutPolicyTest {
    @Test
    fun `portrait phones reserve the upper forty percent and tablets cap sheet height`() {
        assertEquals(480, resolveEpisodeSheetHeightDp(800, landscape = false))
        assertEquals(560, resolveEpisodeSheetHeightDp(1400, landscape = false))
        assertEquals(216, resolveEpisodeSheetHeightDp(360, landscape = false))
        assertEquals(0, resolveEpisodeSheetHeightDp(0, landscape = false))
    }

    @Test
    fun `landscape retains usable list space within the current short window`() {
        assertEquals(306, resolveEpisodeSheetHeightDp(360, landscape = true))
        assertEquals(480, resolveEpisodeSheetHeightDp(800, landscape = true))
    }

    @Test
    fun `compact inline panel leaves the video uncovered and never exceeds available detail room`() {
        val safe = IntRect(0, 20, 400, 820)
        assertEquals(IntRect(0, 340, 400, 820),
            resolvePlayerPanelBounds(safe, IntRect(0, 20, 400, 250), false, false, 500, maxBottomHeight = 480))
        assertEquals(IntRect(0, 800, 400, 820),
            resolvePlayerPanelBounds(safe, IntRect(0, 20, 400, 800), false, false, 500, maxBottomHeight = 480))
    }

    @Test
    fun `collection height limit does not shrink landscape side panels`() {
        val safe = IntRect(20, 10, 1180, 590)
        assertEquals(IntRect(680, 10, 1180, 590),
            resolvePlayerPanelBounds(safe, null, true, true, 500, maxBottomHeight = 300))
    }
}
