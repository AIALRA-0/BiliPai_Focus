package com.android.purebilibili.feature.video.playback.resolver

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class NextPlaybackResolverTest {

    @Test
    fun `external playlist should switch to playlist-first audio continuation`() {
        assertEquals(
            AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE
            )
        )
    }

    @Test
    fun `non external session should keep page season related continuation`() {
        assertEquals(
            AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED,
            resolveAudioNextPlaybackStrategy(
                isExternalPlaylist = false,
                externalPlaylistSource = ExternalPlaylistSource.NONE
            )
        )
    }

    @Test
    fun `play in order should prefer page or season before playlist`() {
        assertEquals(
            PlayInOrderNextSource.PAGE_OR_SEASON,
            resolvePlayInOrderNextSource(
                isExternalPlaylist = false,
                hasNextPage = false,
                hasNextSeasonEpisode = true,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `play in order should fallback to playlist when collection has no next`() {
        assertEquals(
            PlayInOrderNextSource.PLAYLIST,
            resolvePlayInOrderNextSource(
                isExternalPlaylist = false,
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `play in order should stop when no source has next`() {
        assertEquals(
            PlayInOrderNextSource.NONE,
            resolvePlayInOrderNextSource(
                isExternalPlaylist = false,
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = false
            )
        )
    }

    @Test
    fun `play in order previous should prefer page or season before playlist`() {
        assertEquals(
            PlayInOrderNextSource.PAGE_OR_SEASON,
            resolvePlayInOrderPreviousSource(
                isExternalPlaylist = false,
                hasPreviousPage = true,
                hasPreviousSeasonEpisode = false,
                hasPreviousPlaylistItem = true
            )
        )
    }

    @Test
    fun `play in order previous should fallback to playlist when collection has no previous`() {
        assertEquals(
            PlayInOrderNextSource.PLAYLIST,
            resolvePlayInOrderPreviousSource(
                isExternalPlaylist = false,
                hasPreviousPage = false,
                hasPreviousSeasonEpisode = false,
                hasPreviousPlaylistItem = true
            )
        )
    }

    @Test
    fun `play in order previous should stop when no source has previous`() {
        assertEquals(
            PlayInOrderNextSource.NONE,
            resolvePlayInOrderPreviousSource(
                isExternalPlaylist = false,
                hasPreviousPage = false,
                hasPreviousSeasonEpisode = false,
                hasPreviousPlaylistItem = false
            )
        )
    }

    @Test
    fun `external order finishes parts but ignores season episodes`() {
        listOf(true to false, false to true, true to true).forEach { (hasPage, hasSeason) ->
            assertEquals(
                if (hasPage) PlayInOrderNextSource.PAGE_OR_SEASON else PlayInOrderNextSource.PLAYLIST,
                resolvePlayInOrderNextSource(
                    isExternalPlaylist = true,
                    hasNextPage = hasPage,
                    hasNextSeasonEpisode = hasSeason,
                    hasNextPlaylistItem = true
                )
            )
        }
    }

    @Test
    fun `external order at queue end finishes parts then stops before season episodes`() {
        listOf(true to false, false to true, true to true).forEach { (hasPage, hasSeason) ->
            assertEquals(
                if (hasPage) PlayInOrderNextSource.PAGE_OR_SEASON else PlayInOrderNextSource.NONE,
                resolvePlayInOrderNextSource(
                    isExternalPlaylist = true,
                    hasNextPage = hasPage,
                    hasNextSeasonEpisode = hasSeason,
                    hasNextPlaylistItem = false
                )
            )
        }
    }

    @Test
    fun `external previous finishes parts but ignores season episodes`() {
        listOf(true to false, false to true, true to true).forEach { (hasPage, hasSeason) ->
            assertEquals(
                if (hasPage) PlayInOrderNextSource.PAGE_OR_SEASON else PlayInOrderNextSource.PLAYLIST,
                resolvePlayInOrderPreviousSource(
                    isExternalPlaylist = true,
                    hasPreviousPage = hasPage,
                    hasPreviousSeasonEpisode = hasSeason,
                    hasPreviousPlaylistItem = true
                )
            )
        }
    }

    @Test
    fun `external previous at queue start keeps remaining parts but never enters season`() {
        listOf(true to false, false to true, true to true).forEach { (hasPage, hasSeason) ->
            assertEquals(
                if (hasPage) PlayInOrderNextSource.PAGE_OR_SEASON else PlayInOrderNextSource.NONE,
                resolvePlayInOrderPreviousSource(
                    isExternalPlaylist = true,
                    hasPreviousPage = hasPage,
                    hasPreviousSeasonEpisode = hasSeason,
                    hasPreviousPlaylistItem = false
                )
            )
        }
    }

    @Test
    fun `non external navigation should try page season then playlist then direct queue fallback`() {
        assertContentEquals(
            listOf(
                PlaybackNavigationTarget.PAGE_OR_SEASON,
                PlaybackNavigationTarget.PLAYLIST,
                PlaybackNavigationTarget.DIRECT_QUEUE
            ),
            resolvePlaybackNavigationTargets(
                strategy = AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED,
                hasPageTarget = false,
                hasSeasonTarget = true,
                hasPlaylistTarget = true
            )
        )
    }

    @Test
    fun `non external navigation should still expose direct queue fallback when no earlier source exists`() {
        assertContentEquals(
            listOf(PlaybackNavigationTarget.DIRECT_QUEUE),
            resolvePlaybackNavigationTargets(
                strategy = AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED,
                hasPageTarget = false,
                hasSeasonTarget = false,
                hasPlaylistTarget = false
            )
        )
    }

    @Test
    fun `external navigation completes parts without exposing a queue fallback`() {
        assertContentEquals(
            listOf(PlaybackNavigationTarget.PAGE_OR_SEASON),
            resolvePlaybackNavigationTargets(
                strategy = AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
                hasPageTarget = true,
                hasSeasonTarget = true,
                hasPlaylistTarget = true
            )
        )
    }

    @Test
    fun `external navigation crosses queue boundary and ignores season without direct fallback`() {
        assertContentEquals(
            listOf(
                PlaybackNavigationTarget.PLAYLIST
            ),
            resolvePlaybackNavigationTargets(
                strategy = AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
                hasPageTarget = false,
                hasSeasonTarget = true,
                hasPlaylistTarget = true
            )
        )
        assertContentEquals(
            emptyList(),
            resolvePlaybackNavigationTargets(
                strategy = AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST,
                hasPageTarget = false,
                hasSeasonTarget = true,
                hasPlaylistTarget = false
            )
        )
    }
}
