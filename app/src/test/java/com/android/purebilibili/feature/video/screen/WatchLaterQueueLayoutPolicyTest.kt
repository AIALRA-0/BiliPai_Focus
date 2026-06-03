package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterQueueLayoutPolicyTest {

    @Test
    fun listMaxHeightUsesScreenRatioOnCommonPhones() {
        assertEquals(562, resolveExternalPlaylistQueueListMaxHeightDp(screenHeightDp = 780))
    }

    @Test
    fun listMaxHeightHasMinimumBoundOnShortScreens() {
        assertEquals(420, resolveExternalPlaylistQueueListMaxHeightDp(screenHeightDp = 520))
    }

    @Test
    fun bottomSpacerAddsSafeInsetAndBaselineGap() {
        assertEquals(8, resolveExternalPlaylistQueueBottomSpacerDp(navigationBarBottomDp = 0))
        assertEquals(30, resolveExternalPlaylistQueueBottomSpacerDp(navigationBarBottomDp = 22))
    }
}
