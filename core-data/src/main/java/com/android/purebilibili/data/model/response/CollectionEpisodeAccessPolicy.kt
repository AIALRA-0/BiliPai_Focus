package com.android.purebilibili.data.model.response

/** 简略合集条目可能没有权限字段，只从同一视频的详情补充充电状态。分 P 不改变视频身份。 */
fun resolveCollectionEpisodeAccessLabel(episode: UgcEpisode, detail: ViewInfo?): String? {
    val matches = detail != null && when {
        episode.bvid.isNotBlank() && detail.bvid.isNotBlank() -> episode.bvid == detail.bvid &&
            (episode.aid <= 0 || detail.aid <= 0 || episode.aid == detail.aid)
        else -> episode.aid > 0 && episode.aid == detail.aid
    }
    if (matches && detail != null && (detail.isUpowerExclusive || detail.isUpowerExclusiveWithQa)) {
        return "充电专属"
    }
    return episode.accessLabel
}
