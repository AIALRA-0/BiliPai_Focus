package com.android.purebilibili.feature.video.viewmodel

import androidx.media3.common.Player
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackCompletionPolicyTest {

    @Test
    fun `stop mode always stops after ended`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = true,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = true
            )
        )
    }

    @Test
    fun `play in order should request next in playlist`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
                autoPlayEnabled = false,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `repeat one should replay current`() {
        assertEquals(
            PlaybackEndAction.REPEAT_CURRENT,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.REPEAT_ONE,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `loop playlist should request next with loop`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.LOOP_PLAYLIST,
                autoPlayEnabled = false,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `auto continue uses legacy autoplay gate for normal videos`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = true
            )
        )
        assertEquals(
            PlaybackEndAction.AUTO_CONTINUE,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = true,
                isExternalPlaylist = false,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `auto continue stops for external playlist when dedicated switch is off`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false
            )
        )
    }

    @Test
    fun `auto continue still continues for external playlist when dedicated switch is on`() {
        assertEquals(
            PlaybackEndAction.AUTO_CONTINUE,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = true
            )
        )
    }

    @Test
    fun `favorite external playlist should respect explicit stop behavior`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndActionForSession(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
                playMode = PlayMode.SEQUENTIAL
            )
        )
    }

    @Test
    fun `favorite external playlist should respect play in order behavior`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            resolvePlaybackEndActionForSession(
                behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
                autoPlayEnabled = false,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = false,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
                playMode = PlayMode.REPEAT_ONE
            )
        )
    }

    @Test
    fun `non-favorite external playlist should still respect selected completion behavior`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndActionForSession(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = true,
                isExternalPlaylist = true,
                externalPlaylistAutoContinueEnabled = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playMode = PlayMode.SEQUENTIAL
            )
        )
    }

    @Test
    fun `repeat one maps to exoplayer repeat one mode`() {
        assertEquals(
            Player.REPEAT_MODE_ONE,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE)
        )
    }

    @Test
    fun `non repeat one completion modes keep exoplayer repeat off`() {
        listOf(
            PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
            PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
            PlaybackCompletionBehavior.PLAY_IN_ORDER,
            PlaybackCompletionBehavior.LOOP_PLAYLIST
        ).forEach { behavior ->
            assertEquals(Player.REPEAT_MODE_OFF, resolvePlaybackCompletionRepeatMode(behavior))
        }
    }
}
