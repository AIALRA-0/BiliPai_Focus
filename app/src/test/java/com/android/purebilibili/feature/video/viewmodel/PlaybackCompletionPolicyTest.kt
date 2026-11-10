package com.android.purebilibili.feature.video.viewmodel

import androidx.media3.common.Player
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackCompletionPolicyTest {
    @Test
    fun `end timer only stops the selected video part`() {
        val selected = "BV1" to 123L
        assertTrue(shouldStopAfterCurrentPlayback(selected, "BV1", 123L))
        assertFalse(shouldStopAfterCurrentPlayback(selected, "BV1", 456L))
        assertFalse(shouldStopAfterCurrentPlayback(selected, "BV2", 123L))
        assertFalse(shouldStopAfterCurrentPlayback(null, "BV1", 123L))
    }

    @Test
    fun `audio mode leaves repeat decisions to audio queue`() {
        assertEquals(
            Player.REPEAT_MODE_OFF,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE, isInAudioMode = true),
        )
        assertEquals(
            Player.REPEAT_MODE_ONE,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE, isInAudioMode = false),
        )
    }

    @Test
    fun `comment interaction suppresses playback completion navigation`() {
        assertTrue(shouldSuppressPlaybackCompletionForCommentInteraction(true))
        assertFalse(shouldSuppressPlaybackCompletionForCommentInteraction(false))
    }

    @Test
    fun `explicit completion modes override both continuation switches at every boundary`() {
        val actions = mapOf(
            PlaybackCompletionBehavior.STOP_AFTER_CURRENT to PlaybackEndAction.STOP,
            PlaybackCompletionBehavior.PLAY_IN_ORDER to PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            PlaybackCompletionBehavior.REPEAT_ONE to PlaybackEndAction.REPEAT_CURRENT,
            PlaybackCompletionBehavior.LOOP_PLAYLIST to PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP
        )
        actions.forEach { (behavior, expected) ->
            listOf(false, true).forEach { external ->
                listOf(false, true).forEach { nextEnabled ->
                    listOf(false, true).forEach { listEnabled ->
                        listOf(false, true).forEach { hasPage ->
                            assertEquals(expected, resolvePlaybackEndAction(
                                behavior = behavior,
                                autoPlayEnabled = nextEnabled,
                                isExternalPlaylist = external,
                                externalPlaylistAutoContinueEnabled = listEnabled,
                                hasNextPage = hasPage,
                                hasNextSeasonEpisode = true
                            ))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `external auto remaining parts use next switch regardless of list switch`() {
        listOf(false, true).forEach { nextEnabled ->
            listOf(false, true).forEach { listEnabled ->
                assertEquals(
                    if (nextEnabled) PlaybackEndAction.AUTO_CONTINUE else PlaybackEndAction.STOP,
                    resolvePlaybackEndAction(
                        behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                        autoPlayEnabled = nextEnabled,
                        isExternalPlaylist = true,
                        externalPlaylistAutoContinueEnabled = listEnabled,
                        hasNextPage = true,
                        hasNextSeasonEpisode = true
                    ),
                    "remaining P: N=$nextEnabled L=$listEnabled"
                )
            }
        }
    }

    @Test
    fun `external auto video boundary uses list switch regardless of next switch or season`() {
        listOf(false, true).forEach { nextEnabled ->
            listOf(false, true).forEach { listEnabled ->
                listOf(false, true).forEach { hasSeason ->
                    assertEquals(
                        if (listEnabled) PlaybackEndAction.AUTO_CONTINUE else PlaybackEndAction.STOP,
                        resolvePlaybackEndAction(
                            behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                            autoPlayEnabled = nextEnabled,
                            isExternalPlaylist = true,
                            externalPlaylistAutoContinueEnabled = listEnabled,
                            hasNextPage = false,
                            hasNextSeasonEpisode = hasSeason
                        ),
                        "video boundary: N=$nextEnabled L=$listEnabled season=$hasSeason"
                    )
                }
            }
        }
    }

    @Test
    fun `non external auto retains page and season continuation but stops before related videos`() {
        listOf(false, true).forEach { nextEnabled ->
            listOf(false, true).forEach { listEnabled ->
                listOf(false to false, true to false, false to true, true to true).forEach { (page, season) ->
                    assertEquals(
                        if (nextEnabled && (page || season)) PlaybackEndAction.AUTO_CONTINUE else PlaybackEndAction.STOP,
                        resolvePlaybackEndAction(
                            behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                            autoPlayEnabled = nextEnabled,
                            isExternalPlaylist = false,
                            externalPlaylistAutoContinueEnabled = listEnabled,
                            hasNextPage = page,
                            hasNextSeasonEpisode = season
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `repeat one maps to exoplayer repeat one mode`() {
        assertEquals(Player.REPEAT_MODE_ONE,
            resolvePlaybackCompletionRepeatMode(PlaybackCompletionBehavior.REPEAT_ONE))
    }

    @Test
    fun `non repeat one completion modes keep exoplayer repeat off`() {
        PlaybackCompletionBehavior.entries.filter { it != PlaybackCompletionBehavior.REPEAT_ONE }.forEach {
            assertEquals(Player.REPEAT_MODE_OFF, resolvePlaybackCompletionRepeatMode(it))
        }
    }
}
