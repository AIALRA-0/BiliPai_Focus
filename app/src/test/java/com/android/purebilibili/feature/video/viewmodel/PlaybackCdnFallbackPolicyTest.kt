package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.feature.plugin.CdnCandidateHealth
import com.android.purebilibili.feature.plugin.CdnHealthEvent
import com.android.purebilibili.feature.plugin.recordCdnHealthEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackCdnFallbackPolicyTest {

    @Test
    fun `cdn rewrite keeps original playback pair as fallback`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://cn-sh-ct-01-01.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = "上海"
        )

        assertTrue(state.usesCdnRewrite)
        assertEquals("https://upos-sz-mirrorali.bilivideo.com/video.m4s", state.fallbackVideoUrl)
        assertEquals("https://upos-sz-mirrorali.bilivideo.com/audio.m4s", state.fallbackAudioUrl)
        assertTrue(shouldFallbackFromCdnRewrite(state, playbackReady = false))
    }

    @Test
    fun `unchanged playback url does not arm cdn fallback`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = null
        )

        assertFalse(state.usesCdnRewrite)
        assertFalse(shouldFallbackFromCdnRewrite(state, playbackReady = false))
    }

    @Test
    fun `audio candidates use backup urls from selected audio track`() {
        val candidates = buildPlaybackAudioUrlCandidates(
            audioUrl = "https://audio.example.com/30280-base.m4s",
            cachedDashAudios = listOf(
                DashAudio(
                    id = 30232,
                    baseUrl = "https://audio.example.com/30232-base.m4s",
                    backupUrl = listOf("https://audio.example.com/30232-backup.m4s")
                ),
                DashAudio(
                    id = 30280,
                    baseUrl = "https://audio.example.com/30280-base.m4s",
                    backupUrl = listOf("https://audio.example.com/30280-backup.m4s")
                )
            )
        )

        assertEquals(
            listOf(
                "https://audio.example.com/30280-base.m4s",
                "https://audio.example.com/30280-backup.m4s"
            ),
            candidates
        )
    }

    @Test
    fun `same video url can arm audio fallback when selected audio has backup`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://audio.example.com/30280-base.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://audio.example.com/30280-base.m4s",
            regionLabel = null,
            audioFallbackUrl = "https://audio.example.com/30280-backup.m4s"
        )

        assertTrue(state.usesCdnRewrite)
        assertEquals("https://audio.example.com/30280-backup.m4s", state.fallbackAudioUrl)
        assertTrue(
            shouldFallbackFromCdnRewrite(
                state = state,
                playbackReady = true,
                expectedAudioTrack = true,
                hasSelectedAudioTrack = false,
                audioRendererError = false
            )
        )
    }

    @Test
    fun `cdn fallback can only fire once`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = null,
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = null,
            regionLabel = "上海"
        )

        val consumed = state.markFallbackConsumed()

        assertFalse(shouldFallbackFromCdnRewrite(consumed, playbackReady = false))
    }

    @Test
    fun `ready playback still falls back when rewritten audio track is missing`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://cn-sh-ct-01-01.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = "上海"
        )

        assertTrue(
            shouldFallbackFromCdnRewrite(
                state = state,
                playbackReady = true,
                expectedAudioTrack = true,
                hasSelectedAudioTrack = false,
                audioRendererError = false
            )
        )
    }

    @Test
    fun `ready playback falls back immediately after audio renderer error`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://cn-sh-ct-01-01.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = "上海"
        )

        assertTrue(
            shouldFallbackFromCdnRewrite(
                state = state,
                playbackReady = true,
                expectedAudioTrack = true,
                hasSelectedAudioTrack = true,
                audioRendererError = true
            )
        )
    }

    @Test
    fun `ready playback keeps rewritten source when audio track is selected`() {
        val state = buildPlaybackCdnFallbackState(
            selectedVideoUrl = "https://cn-sh-ct-01-01.bilivideo.com/video.m4s",
            selectedAudioUrl = "https://cn-sh-ct-01-01.bilivideo.com/audio.m4s",
            originalVideoUrl = "https://upos-sz-mirrorali.bilivideo.com/video.m4s",
            originalAudioUrl = "https://upos-sz-mirrorali.bilivideo.com/audio.m4s",
            regionLabel = "上海"
        )

        assertFalse(
            shouldFallbackFromCdnRewrite(
                state = state,
                playbackReady = true,
                expectedAudioTrack = true,
                hasSelectedAudioTrack = true,
                audioRendererError = false
            )
        )
    }

    @Test
    fun `cdn playback failures record negative health feedback`() {
        val initial = CdnCandidateHealth(host = "cn-sh-ct-01-01.bilivideo.com")
        val timeout = recordCdnHealthEvent(initial, CdnHealthEvent.FIRST_FRAME_TIMEOUT, nowMs = 1_000L)
        val audioMissing = recordCdnHealthEvent(timeout, CdnHealthEvent.AUDIO_TRACK_MISSING, nowMs = 2_000L)
        val playerError = recordCdnHealthEvent(audioMissing, CdnHealthEvent.PLAYER_ERROR, nowMs = 3_000L)

        assertEquals(1, playerError.firstFrameTimeoutCount)
        assertEquals(2, playerError.playbackErrorCount)
        assertEquals(3_000L, playerError.lastUpdatedAtMs)
    }

    @Test
    fun `ready playback records positive health feedback`() {
        val initial = CdnCandidateHealth(host = "cn-sh-ct-01-01.bilivideo.com")
        val ready = recordCdnHealthEvent(initial, CdnHealthEvent.PLAYBACK_READY, nowMs = 1_000L)

        assertEquals(1, ready.readyCount)
        assertEquals(0, ready.playbackErrorCount)
        assertEquals(1_000L, ready.lastUpdatedAtMs)
    }
}
