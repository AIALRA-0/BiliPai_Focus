package com.android.purebilibili.feature.video.usecase

import androidx.media3.common.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class VideoPlaybackUserResumePolicyTest {

    @Test
    fun `playPlayerFromUserAction nudges current position before play when paused in ready state`() {
        val player = mockk<Player>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.mediaItemCount } returns 1
        every { player.currentPosition } returns 42_000L
        every { player.isPlaying } returns false
        every { player.playWhenReady } returns false

        playPlayerFromUserAction(player)

        verify(exactly = 1) { player.seekTo(42_000L) }
        verify(exactly = 1) { player.play() }
    }
}
