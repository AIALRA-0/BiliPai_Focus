package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiItem
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.data.model.response.BangumiType
import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.TimelineEpisode

const val MY_FOLLOW_TYPE_BANGUMI = 1
const val MY_FOLLOW_TYPE_CINEMA = 2

fun defaultMyFollowTypeForSeasonType(seasonType: Int): Int {
    return when (seasonType) {
        BangumiType.ANIME.value, BangumiType.GUOCHUANG.value -> MY_FOLLOW_TYPE_BANGUMI
        else -> MY_FOLLOW_TYPE_CINEMA
    }
}

fun resolveMyFollowRequestType(requestedType: Int?, currentType: Int): Int {
    return requestedType ?: currentType
}

fun resolveMyFollowItemLazyKey(
    index: Int,
    item: FollowBangumiItem
): String {
    val businessKey = when {
        item.seasonId > 0L -> "season_${item.seasonId}"
        item.mediaId > 0L -> "media_${item.mediaId}"
        item.firstEp > 0L -> "ep_${item.firstEp}"
        item.url.isNotBlank() -> "url_${item.url.hashCode()}"
        item.title.isNotBlank() -> "title_${item.title.hashCode()}"
        else -> "unknown"
    }
    return "my_follow_${businessKey}_$index"
}

fun resolveBangumiIndexItemLazyKey(
    index: Int,
    item: BangumiItem
): String {
    val businessKey = when {
        item.seasonId > 0L -> "season_${item.seasonId}"
        item.mediaId > 0L -> "media_${item.mediaId}"
        item.newEp?.id?.takeIf { it > 0L } != null -> "ep_${item.newEp.id}"
        item.title.isNotBlank() -> "title_${item.title.hashCode()}"
        item.cover.isNotBlank() -> "cover_${item.cover.hashCode()}"
        else -> "unknown"
    }
    return "bangumi_index_${businessKey}_$index"
}

fun resolveBangumiSearchItemLazyKey(
    index: Int,
    item: BangumiSearchItem
): String {
    val businessKey = when {
        item.seasonId > 0L -> "season_${item.seasonId}"
        item.pgcSeasonId > 0L -> "season_${item.pgcSeasonId}"
        item.mediaId > 0L -> "media_${item.mediaId}"
        item.episodes?.firstOrNull { it.id > 0L } != null -> "ep_${item.episodes.first { it.id > 0L }.id}"
        item.gotoUrl.isNotBlank() -> "url_${item.gotoUrl.hashCode()}"
        item.orgTitle.isNotBlank() -> "org_title_${item.orgTitle.hashCode()}"
        item.title.isNotBlank() -> "title_${item.title.hashCode()}"
        else -> "unknown"
    }
    return "bangumi_search_${businessKey}_$index"
}

fun resolveTimelineEpisodeLazyKey(
    index: Int,
    episode: TimelineEpisode
): String {
    val businessKey = when {
        episode.episodeId > 0L -> "episode_${episode.episodeId}"
        episode.seasonId > 0L -> "season_${episode.seasonId}"
        episode.pubTs > 0L && episode.pubIndex.isNotBlank() -> "pub_${episode.pubTs}_${episode.pubIndex.hashCode()}"
        episode.title.isNotBlank() -> "title_${episode.title.hashCode()}"
        else -> "unknown"
    }
    return "bangumi_timeline_${businessKey}_$index"
}
