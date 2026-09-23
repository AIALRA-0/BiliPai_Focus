package com.android.purebilibili.feature.watchlater

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterLayoutPolicyTest {
    @Test
    fun listWidth_isCappedForLargeScreens() {
        assertEquals(480.dp, resolveWatchLaterListMaxWidth())
    }

    @Test
    fun gridColumns_followPersonalListMaxExtent() {
        assertEquals(1, resolveWatchLaterColumnCount(479f))
        assertEquals(2, resolveWatchLaterColumnCount(840f))
    }
}
