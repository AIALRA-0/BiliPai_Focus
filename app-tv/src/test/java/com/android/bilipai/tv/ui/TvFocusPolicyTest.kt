package com.android.bilipai.tv.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TvFocusPolicyTest {
    @Test fun `return targets the same video after reordering`() {
        assertEquals(2, resolveTvFocusIndex(listOf("c", "a", "b"), "b", 0))
    }
    @Test fun `removing the focused last card selects its nearest survivor`() {
        assertEquals(1, resolveTvFocusIndex(listOf("a", "b"), "c", 2))
        assertNull(resolveTvFocusIndex(emptyList(), "c", 2))
    }
    @Test fun `seeking clamps to content bounds and waits for known duration`() {
        assertEquals(0L, tvSeekTarget(4_000, -10_000, 120_000))
        assertEquals(120_000L, tvSeekTarget(115_000, 10_000, 120_000))
        assertNull(tvSeekTarget(0, 10_000, 0))
    }
    @Test fun `back closes each playback layer before leaving`() {
        assertEquals(TvPlayerBackAction.CloseDialog, resolveTvPlayerBack(true, true, true))
        assertEquals(TvPlayerBackAction.CancelSeek, resolveTvPlayerBack(false, true, true))
        assertEquals(TvPlayerBackAction.HideControls, resolveTvPlayerBack(false, false, true))
        assertEquals(TvPlayerBackAction.LeavePlayer, resolveTvPlayerBack(false, false, false))
    }
}
