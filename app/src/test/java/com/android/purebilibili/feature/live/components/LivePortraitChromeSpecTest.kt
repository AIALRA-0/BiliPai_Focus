package com.android.purebilibili.feature.live.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class LivePortraitChromeSpecTest {
    @Test
    fun portraitChromeRetainsControlAndSheetSizing() {
        assertEquals(14.dp, LivePortraitChromeSpec.StatusChevronIconSize)
        assertEquals(32.dp, LivePortraitChromeSpec.ReturnToBottomMinHeight)
        assertEquals(14.dp, LivePortraitChromeSpec.ReturnToBottomIconSize)
        assertEquals(32.dp, LivePortraitChromeSpec.EmoticonSize)
        assertEquals(520.dp, LivePortraitChromeSpec.MoreSheetMaxHeight)
    }
}
