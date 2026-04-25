package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason

enum class CollectionSortMode(val label: String) {
    ASCENDING("正序"),
    DESCENDING("倒序"),
    RECENT("最近观看")
}

internal fun resolveCurrentUgcEpisodeIndex(
    episodes: List<UgcEpisode>,
    currentBvid: String,
    currentCid: Long
): Int {
    if (currentBvid.isBlank()) return -1
    if (currentCid > 0L) {
        val exactIndex = episodes.indexOfFirst { episode ->
            episode.bvid == currentBvid && episode.cid == currentCid
        }
        if (exactIndex >= 0) return exactIndex
    }
    return episodes.indexOfFirst { episode -> episode.bvid == currentBvid }
}

internal fun isCurrentUgcEpisode(
    currentBvid: String,
    currentCid: Long,
    episode: UgcEpisode
): Boolean {
    if (episode.bvid != currentBvid) return false
    return currentCid <= 0L || episode.cid <= 0L || episode.cid == currentCid
}

internal fun sortCollectionEpisodes(
    episodes: List<UgcEpisode>,
    sortMode: CollectionSortMode,
    currentBvid: String,
    currentCid: Long
): List<UgcEpisode> {
    return when (sortMode) {
        CollectionSortMode.ASCENDING -> episodes
        CollectionSortMode.DESCENDING -> episodes.asReversed()
        CollectionSortMode.RECENT -> {
            val currentIndex = resolveCurrentUgcEpisodeIndex(
                episodes = episodes,
                currentBvid = currentBvid,
                currentCid = currentCid
            )
            if (currentIndex !in episodes.indices) {
                episodes
            } else {
                buildList(episodes.size) {
                    add(episodes[currentIndex])
                    episodes.forEachIndexed { index, episode ->
                        if (index != currentIndex) add(episode)
                    }
                }
            }
        }
    }
}

internal fun resolveCollectionSortLabel(sortMode: CollectionSortMode): String {
    return when (sortMode) {
        CollectionSortMode.ASCENDING -> "正序"
        CollectionSortMode.DESCENDING -> "倒序"
        CollectionSortMode.RECENT -> "最近"
    }
}

internal fun resolveCollectionSubscriptionId(ugcSeason: UgcSeason): Long {
    if (ugcSeason.id > 0L) return ugcSeason.id
    return ugcSeason.sections
        .firstOrNull { section -> section.season_id > 0L }
        ?.season_id
        ?: 0L
}

internal fun resolveCurrentUgcEpisodeAid(
    episodes: List<UgcEpisode>,
    currentBvid: String,
    currentCid: Long
): Long {
    if (currentBvid.isBlank()) return 0L
    if (currentCid > 0L) {
        val exactEpisode = episodes.firstOrNull { episode ->
            episode.bvid == currentBvid && episode.cid == currentCid
        }
        if (exactEpisode != null) return exactEpisode.aid
    }
    return episodes.firstOrNull { episode -> episode.bvid == currentBvid }?.aid ?: 0L
}
