package com.android.purebilibili.feature.video.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlaybackUseCaseBootstrapPolicyTest {

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
}
