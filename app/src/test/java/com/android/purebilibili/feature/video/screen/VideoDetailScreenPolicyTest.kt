package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoDetailScreenPolicyTest {

    @Test
    fun portraitExitPlayerTarget_prefersCurrentInternalBvidOverRouteBvid() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = "BV_PORTRAIT_NEXT"
        )

        assertEquals("BV_PORTRAIT_NEXT", resolved.bvid)
        assertEquals("", resolved.entryCoverUrl)
    }

    @Test
    fun portraitExitPlayerTarget_keepsRouteCoverWhenStillShowingRouteVideo() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = "BV_ROUTE"
        )

        assertEquals("BV_ROUTE", resolved.bvid)
        assertEquals("https://img/route.jpg", resolved.entryCoverUrl)
    }

    @Test
    fun portraitExitPlayerTarget_fallsBackToRouteWhenInternalTargetMissing() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = ""
        )

        assertEquals("BV_ROUTE", resolved.bvid)
        assertEquals("https://img/route.jpg", resolved.entryCoverUrl)
    }
}
