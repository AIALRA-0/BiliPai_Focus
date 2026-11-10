package com.android.purebilibili.feature.video.playback.resolver

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource

internal enum class AudioNextPlaybackStrategy {
    PLAY_EXTERNAL_PLAYLIST,
    PAGE_THEN_SEASON_THEN_RELATED
}

internal enum class PlayInOrderNextSource {
    PAGE_OR_SEASON,
    PLAYLIST,
    NONE
}

internal enum class PlaybackNavigationTarget {
    PAGE_OR_SEASON,
    PLAYLIST,
    DIRECT_QUEUE
}

private fun resolvePlayInOrderSource(
    isExternalPlaylist: Boolean,
    hasPageTarget: Boolean,
    hasSeasonTarget: Boolean,
    hasPlaylistTarget: Boolean
): PlayInOrderNextSource {
    return when {
        hasPageTarget -> PlayInOrderNextSource.PAGE_OR_SEASON
        !isExternalPlaylist && hasSeasonTarget -> PlayInOrderNextSource.PAGE_OR_SEASON
        hasPlaylistTarget -> PlayInOrderNextSource.PLAYLIST
        else -> PlayInOrderNextSource.NONE
    }
}

internal fun resolvePlayInOrderNextSource(
    isExternalPlaylist: Boolean,
    hasNextPage: Boolean,
    hasNextSeasonEpisode: Boolean,
    hasNextPlaylistItem: Boolean,
): PlayInOrderNextSource {
    return resolvePlayInOrderSource(
        isExternalPlaylist = isExternalPlaylist,
        hasPageTarget = hasNextPage,
        hasSeasonTarget = hasNextSeasonEpisode,
        hasPlaylistTarget = hasNextPlaylistItem
    )
}

internal fun resolvePlayInOrderPreviousSource(
    isExternalPlaylist: Boolean,
    hasPreviousPage: Boolean,
    hasPreviousSeasonEpisode: Boolean,
    hasPreviousPlaylistItem: Boolean
): PlayInOrderNextSource {
    return resolvePlayInOrderSource(
        isExternalPlaylist = isExternalPlaylist,
        hasPageTarget = hasPreviousPage,
        hasSeasonTarget = hasPreviousSeasonEpisode,
        hasPlaylistTarget = hasPreviousPlaylistItem
    )
}

internal fun resolveAudioNextPlaybackStrategy(
    isExternalPlaylist: Boolean,
    externalPlaylistSource: ExternalPlaylistSource
): AudioNextPlaybackStrategy {
    if (!isExternalPlaylist || externalPlaylistSource == ExternalPlaylistSource.NONE) {
        return AudioNextPlaybackStrategy.PAGE_THEN_SEASON_THEN_RELATED
    }
    return AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST
}

internal fun resolvePlaybackNavigationTargets(
    strategy: AudioNextPlaybackStrategy,
    hasPageTarget: Boolean,
    hasSeasonTarget: Boolean,
    hasPlaylistTarget: Boolean
): List<PlaybackNavigationTarget> {
    if (strategy == AudioNextPlaybackStrategy.PLAY_EXTERNAL_PLAYLIST) {
        // Parts stay in the current video; only the video boundary belongs to the external queue.
        return when {
            hasPageTarget -> listOf(PlaybackNavigationTarget.PAGE_OR_SEASON)
            hasPlaylistTarget -> listOf(PlaybackNavigationTarget.PLAYLIST)
            else -> emptyList()
        }
    }

    return buildList {
        if (hasPageTarget || hasSeasonTarget) {
            add(PlaybackNavigationTarget.PAGE_OR_SEASON)
        }
        if (hasPlaylistTarget) {
            add(PlaybackNavigationTarget.PLAYLIST)
        }
        add(PlaybackNavigationTarget.DIRECT_QUEUE)
    }
}
