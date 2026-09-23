package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.core.plugin.SkipAction
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.plugin.SponsorBlockVideoSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoPlaybackViewModelSponsorBlockPolicyTest {

    @Test
    fun sponsorSkip_clampsOrdinarySegmentBeforePlaybackEnd() {
        assertEquals(
            119_000L,
            resolveSponsorBlockSkipTargetPositionMs(
                requestedPositionMs = 121_500L,
                durationMs = 120_000L,
                category = SponsorCategory.SPONSOR,
            )
        )
    }

    @Test
    fun sponsorSkip_allowsOutroToFinishNaturally() {
        assertEquals(
            120_000L,
            resolveSponsorBlockSkipTargetPositionMs(
                requestedPositionMs = 121_500L,
                durationMs = 120_000L,
                category = SponsorCategory.OUTRO,
            )
        )
    }

    @Test
    fun showButtonAction_updatesSkipUiState() {
        val state = reduceSponsorSkipUiState(
            previous = SponsorSkipUiState(),
            action = SkipAction.ShowButton(
                skipToMs = 15_000L,
                label = "跳过恰饭",
                segmentId = "segment"
            )
        )

        assertTrue(state.visible)
        assertEquals("segment", state.segmentId)
        assertEquals(15_000L, state.skipToMs)
    }

    @Test
    fun noneAction_clearsSkipUiState() {
        val state = reduceSponsorSkipUiState(
            previous = SponsorSkipUiState(
                visible = true,
                segmentId = "segment",
                skipToMs = 15_000L,
                label = "跳过恰饭"
            ),
            action = SkipAction.None
        )

        assertFalse(state.visible)
        assertNull(state.segmentId)
    }

    @Test
    fun sponsorBlockSkip_forcesPlaybackResumeEvenWhenPlayerWasPaused() {
        assertTrue(
            shouldResumePlaybackAfterSponsorBlockSkip(
                playWhenReadyBeforeSkip = false
            )
        )
        assertTrue(
            shouldResumePlaybackAfterSponsorBlockSkip(
                playWhenReadyBeforeSkip = true
            )
        )
    }

    @Test
    fun sponsorBlockVideoSnapshot_capturesCoverAndUpFaceBeforeSeek() {
        val snapshot = buildSponsorBlockVideoSnapshot(
            currentState = VideoPlaybackUiState.Success(
                info = ViewInfo(
                    bvid = "BV1",
                    cid = 123L,
                    title = "测试视频",
                    pic = "https://cover.example/1.jpg",
                    owner = Owner(
                        mid = 456L,
                        name = "测试UP",
                        face = "https://face.example/up.jpg"
                    )
                ),
                playUrl = "https://video.example/play"
            )
        )

        assertEquals(
            SponsorBlockVideoSnapshot(
                videoTitle = "测试视频",
                bvid = "BV1",
                cid = 123L,
                videoCoverUrl = "https://cover.example/1.jpg",
                upName = "测试UP",
                upFaceUrl = "https://face.example/up.jpg",
                upMid = 456L
            ),
            snapshot
        )
    }
}
