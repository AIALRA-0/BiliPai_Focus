package com.android.purebilibili.feature.video.usecase

import androidx.media3.common.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class VideoPlaybackUserResumePolicyTest {

    @Test
    fun `playPlayerFromUserAction resumes paused ready playback without forcing compatibility seek`() {
        val player = mockk<Player>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.mediaItemCount } returns 1
        every { player.currentPosition } returns 42_000L
        every { player.isPlaying } returns false
        every { player.playWhenReady } returns false

        playPlayerFromUserAction(player)

        verify(exactly = 0) { player.seekTo(any<Long>()) }
        verify(exactly = 1) { player.play() }
    }

    @Test
    fun `togglePlayerPlaybackFromUserAction restarts ended playback from beginning`() {
        val player = mockk<Player>(relaxed = true)
        every { player.playbackState } returns Player.STATE_ENDED
        every { player.mediaItemCount } returns 1
        every { player.currentPosition } returns 216_000L
        every { player.isPlaying } returns false
        every { player.playWhenReady } returns false

        togglePlayerPlaybackFromUserAction(player)

        verify(exactly = 1) { player.seekTo(0L) }
        verify(exactly = 1) { player.play() }
    }
}
