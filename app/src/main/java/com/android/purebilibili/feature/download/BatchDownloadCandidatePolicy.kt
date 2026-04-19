package com.android.purebilibili.feature.download

import com.android.purebilibili.data.model.response.ViewInfo

internal data class BatchDownloadCandidate(
    val id: String,
    val bvid: String,
    val cid: Long,
    val title: String,
    val label: String,
    val groupKey: String = "",
    val groupTitle: String = "",
    val episodeSortIndex: Int = 0,
    val episodeCount: Int = 1,
    val cover: String,
    val ownerName: String,
    val durationSeconds: Int = 0,
    val isVerticalVideo: Boolean = false,
    val selected: Boolean
)

internal fun resolveBatchDownloadCandidates(info: ViewInfo): List<BatchDownloadCandidate> {
    val groupTitle = info.ugc_season?.title?.trim().orEmpty().ifBlank { info.title.trim() }
    val groupKey = info.ugc_season?.id
        ?.takeIf { it > 0L }
        ?.let { "ugc:$it" }
        ?: "ugc:${info.bvid}"

    val seasonCandidates = info.ugc_season
        ?.sections
        .orEmpty()
        .flatMap { section -> section.episodes }
        .mapNotNull { episode ->
            val bvid = episode.bvid.trim().ifBlank { return@mapNotNull null }
            val cid = episode.cid.takeIf { it > 0L } ?: return@mapNotNull null
            val title = episode.arc?.title?.trim().orEmpty().ifBlank {
                episode.title.trim().ifBlank { info.title }
            }
            val cover = episode.arc?.pic?.takeIf { it.isNotBlank() } ?: info.pic
            BatchDownloadCandidate(
                id = "$bvid#$cid",
                bvid = bvid,
                cid = cid,
                title = title,
                label = title,
                groupKey = groupKey,
                groupTitle = groupTitle,
                episodeSortIndex = 0,
                episodeCount = 1,
                cover = cover,
                ownerName = info.owner.name,
                durationSeconds = episode.arc?.duration?.coerceAtLeast(0) ?: 0,
                isVerticalVideo = info.dimension?.isVertical == true,
                selected = bvid == info.bvid && cid == info.cid
            )
        }
        .distinctBy { it.id }

    if (seasonCandidates.isNotEmpty()) {
        return seasonCandidates.mapIndexed { index, candidate ->
            candidate.copy(
                episodeSortIndex = index + 1,
                episodeCount = seasonCandidates.size
            )
        }
    }

    val pageCandidates = info.pages.mapIndexedNotNull { index, page ->
        val cid = page.cid.takeIf { it > 0L } ?: return@mapIndexedNotNull null
        val pageNo = page.page.takeIf { it > 0 } ?: (index + 1)
        val partLabel = page.part.trim().ifBlank { info.title }
        BatchDownloadCandidate(
            id = "${info.bvid}#$cid",
            bvid = info.bvid,
            cid = cid,
            title = info.title,
            label = "P$pageNo $partLabel".trim(),
            groupKey = "bvid:${info.bvid}",
            groupTitle = info.title.trim(),
            episodeSortIndex = pageNo,
            episodeCount = 1,
            cover = info.pic,
            ownerName = info.owner.name,
            durationSeconds = page.duration.toInt().coerceAtLeast(0),
            isVerticalVideo = info.dimension?.isVertical == true,
            selected = cid == info.cid
        )
    }.distinctBy { it.id }
    if (pageCandidates.isNotEmpty()) {
        return pageCandidates.map { candidate ->
            candidate.copy(episodeCount = pageCandidates.size)
        }
    }
    return emptyList()
}

internal fun resolveBatchDownloadCandidate(
    info: ViewInfo,
    targetBvid: String,
    targetCid: Long
): BatchDownloadCandidate? {
    return resolveBatchDownloadCandidates(info)
        .firstOrNull { it.bvid == targetBvid && it.cid == targetCid }
}
