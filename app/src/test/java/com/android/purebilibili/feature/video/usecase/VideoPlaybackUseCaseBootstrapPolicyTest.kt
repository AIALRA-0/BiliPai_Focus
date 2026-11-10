package com.android.purebilibili.feature.video.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlaybackUseCaseBootstrapPolicyTest {
    @Test
    fun missingCidCanReuseKnownCachedCidWithoutAnotherRequest() {
        assertEquals(1234L, resolvePlaybackBootstrapCid(requestCid = 0L, cachedCid = 1234L))
    }

    @Test
    fun explicitPageCidAlwaysWinsOverCachedDefaultPage() {
        assertEquals(5678L, resolvePlaybackBootstrapCid(requestCid = 5678L, cachedCid = 1234L))
    }

    @Test
    fun missingAndInvalidCachedCidsStayInDetailFirstMode() {
        assertEquals(0L, resolvePlaybackBootstrapCid(requestCid = 0L, cachedCid = -1L))
    }

    @Test
    fun avRouteResolvesCanonicalBvidBeforeRequestingPlaybackEvenWithKnownCid() {
        assertEquals(PlaybackBootstrapMode.DETAIL_ONLY, resolvePlaybackBootstrapMode("av123", 1234L))
    }

    @Test
    fun knownBvidAndCid_enableParallelDetailAndPlayUrlBootstrap() {
        assertEquals(
            PlaybackBootstrapMode.DETAIL_AND_PLAYURL_PARALLEL,
            resolvePlaybackBootstrapMode(
                bvid = "BV1parallel",
                cid = 1234L
            )
        )
    }

    @Test
    fun missingCid_keepsDetailFirstBootstrap() {
        assertEquals(
            PlaybackBootstrapMode.DETAIL_ONLY,
            resolvePlaybackBootstrapMode(
                bvid = "BV1parallel",
                cid = 0L
            )
        )
    }

    @Test
    fun missingBvid_keepsDetailFirstBootstrap() {
        assertEquals(
            PlaybackBootstrapMode.DETAIL_ONLY,
            resolvePlaybackBootstrapMode(
                bvid = "",
                cid = 1234L
            )
        )
    }

    @Test
    fun avRoute_defersRelatedVideosUntilCanonicalBvidIsAvailable() {
        assertEquals(
            true,
            shouldFetchRelatedVideosAfterVideoDetail("av115391124741470")
        )
        assertEquals(
            "BV1frWHzTERT",
            resolveRelatedVideosRequestBvid(
                requestBvid = "av115391124741470",
                canonicalBvid = "BV1frWHzTERT"
            )
        )
    }

    @Test
    fun bvidRoute_keepsParallelRelatedVideosRequest() {
        assertEquals(
            false,
            shouldFetchRelatedVideosAfterVideoDetail("BV1frWHzTERT")
        )
        assertEquals(
            "BV1frWHzTERT",
            resolveRelatedVideosRequestBvid(
                requestBvid = "BV1frWHzTERT",
                canonicalBvid = "BV1canonical"
            )
        )
    }
}
