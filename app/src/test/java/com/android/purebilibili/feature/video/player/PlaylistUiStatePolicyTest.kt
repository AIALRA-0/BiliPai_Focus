package com.android.purebilibili.feature.video.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistUiStatePolicyTest {

    @Test
    fun resolvePlaylistUiState_mapsAllFields() {
        val playlist = listOf(
            PlaylistItem(
                bvid = "BV1xx411c7mD",
                title = "video",
                cover = "cover",
                owner = "owner"
            )
        )

        val result = resolvePlaylistUiState(
            playMode = PlayMode.SHUFFLE,
            playlist = playlist,
            currentIndex = 0,
            isExternalPlaylist = true,
            externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER
        )

        assertEquals(PlayMode.SHUFFLE, result.playMode)
        assertEquals(playlist, result.playlist)
        assertEquals(0, result.currentIndex)
        assertTrue(result.isExternalPlaylist)
        assertEquals(ExternalPlaylistSource.WATCH_LATER, result.externalPlaylistSource)
        assertFalse(result.shuffleEnabled)
    }

    @Test
    fun restoreTransport_promotesLegacyShuffleMode() {
        val restored = resolveRestoredPlayTransport(
            storedPlayMode = PlayMode.SHUFFLE,
            storedShuffleEnabled = false
        )
        assertEquals(PlayMode.REPEAT_ALL, restored.playMode)
        assertTrue(restored.shuffleEnabled)
    }

    @Test
    fun restoreTransport_keepsIndependentShuffleAndRepeatOne() {
        val restored = resolveRestoredPlayTransport(
            storedPlayMode = PlayMode.REPEAT_ONE,
            storedShuffleEnabled = true
        )
        assertEquals(PlayMode.REPEAT_ONE, restored.playMode)
        assertTrue(restored.shuffleEnabled)
    }

    @Test
    fun linearAdvance_wrapsOnlyWhenRequested() {
        assertEquals(1, resolveLinearPlayNextIndex(3, 0, wrap = false))
        assertEquals(null, resolveLinearPlayNextIndex(3, 2, wrap = false))
        assertEquals(0, resolveLinearPlayNextIndex(3, 2, wrap = true))
        assertEquals(null, resolveLinearPlayPreviousIndex(3, 0, wrap = false))
        assertEquals(2, resolveLinearPlayPreviousIndex(3, 0, wrap = true))
    }

    @Test
    fun resolvePlaylistUiState_defaultsRemainStable() {
        val result = resolvePlaylistUiState(
            playMode = PlayMode.SEQUENTIAL,
            playlist = emptyList(),
            currentIndex = -1,
            isExternalPlaylist = false,
            externalPlaylistSource = ExternalPlaylistSource.NONE
        )

        assertEquals(PlayMode.SEQUENTIAL, result.playMode)
        assertEquals(emptyList(), result.playlist)
        assertEquals(-1, result.currentIndex)
        assertFalse(result.isExternalPlaylist)
        assertEquals(ExternalPlaylistSource.NONE, result.externalPlaylistSource)
    }
}
