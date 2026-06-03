package com.android.purebilibili.feature.video.player

import androidx.media3.common.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class MiniPlayerMediaControlPolicyTest {

    @Test
    fun `pip action remains pause when playback intent is active but isPlaying is stale`() {
        assertEquals(
            MiniPlayerManager.ACTION_PAUSE,
            resolvePipPlaybackControlType(
                isPlaying = false,
                playWhenReady = true,
                playbackState = Player.STATE_READY
            )
        )
        assertEquals(
            MiniPlayerManager.ACTION_PAUSE,
            resolvePipPlaybackControlType(
                isPlaying = false,
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING
            )
        )
    }

    @Test
    fun `pip action becomes play only for paused ready playback`() {
        assertEquals(
            MiniPlayerManager.ACTION_PLAY,
            resolvePipPlaybackControlType(
                isPlaying = false,
                playWhenReady = false,
                playbackState = Player.STATE_READY
            )
        )
    }

    @Test
    fun `explicit pause control pauses instead of toggling stale paused state back to play`() {
        val player = mockk<Player>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.mediaItemCount } returns 1
        every { player.isPlaying } returns false
        every { player.playWhenReady } returns false

        applyPlaybackMediaControlToPlayer(player, MediaControlType.PAUSE)

        verify(exactly = 1) { player.pause() }
        verify(exactly = 0) { player.play() }
    }

    @Test
    fun `explicit play control resumes without depending on current isPlaying`() {
        val player = mockk<Player>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.mediaItemCount } returns 1
        every { player.isPlaying } returns false
        every { player.playWhenReady } returns false

        applyPlaybackMediaControlToPlayer(player, MediaControlType.PLAY)

        verify(exactly = 1) { player.playWhenReady = true }
        verify(exactly = 1) { player.play() }
        verify(exactly = 0) { player.pause() }
    }
}
