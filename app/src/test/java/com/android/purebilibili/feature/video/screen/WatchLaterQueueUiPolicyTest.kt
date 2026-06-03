package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterQueueUiPolicyTest {

    @Test
    fun showQueueBarWhenExternalSourceIsWatchLaterAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 8
            )
        )
    }

    @Test
    fun showQueueBarWhenExternalSourceIsSpaceAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.SPACE,
                playlistSize = 8
            )
        )
    }

    @Test
    fun showQueueBarWhenExternalSourceIsFavoriteAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenExternalSourceIsUnknown() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.UNKNOWN,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenNotExternalPlaylist() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = false,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenPlaylistEmpty() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 0
            )
        )
    }

    @Test
    fun externalPlaylistQueueTitleMatchesSource() {
        assertEquals(
            "稍后再看",
            resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.WATCH_LATER)
        )
        assertEquals(
            "收藏夹",
            resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.FAVORITE)
        )
        assertEquals(
            "UP主视频",
            resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.SPACE)
        )
    }
}
