package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun initialVerticalRouteHint_startsPortraitFullscreenBeforeApiDimensionArrives() {
        assertTrue(
            shouldStartInPortraitFullscreenFromRouteHint(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                initialVerticalFromRoute = true
            )
        )
    }

    @Test
    fun secondaryNavigationCallbacks_markNavigationLeaveBeforeRouting() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val userSpaceSource = source.substringAfter("val navigateToUserSpaceFromVideo")
            .substringBefore("val navigateToSearchFromVideo")
        val relatedVideoSource = source.substringAfter("val navigateToRelatedVideo")
            .substringBefore("LaunchedEffect(bvid, cid)")

        assertTrue(
            userSpaceSource.contains("markSecondaryNavigationLeave()") &&
                userSpaceSource.indexOf("markSecondaryNavigationLeave()") <
                userSpaceSource.indexOf("onUpClick(mid)")
        )
        assertTrue(
            relatedVideoSource.contains("markSecondaryNavigationLeave(expectedBvid = success?.info?.bvid ?: currentBvid)") &&
                relatedVideoSource.indexOf("markSecondaryNavigationLeave") <
                relatedVideoSource.indexOf("onVideoClick(targetBvid, navOptions)")
        )
    }
}
