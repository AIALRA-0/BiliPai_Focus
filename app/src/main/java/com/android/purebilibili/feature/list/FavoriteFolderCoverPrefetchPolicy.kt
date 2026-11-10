package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder

/** 一次补封面请求的目标：media_id 与其 media_count 快照。 */
internal data class FavoriteFolderCoverTarget(
    val mediaId: Long,
    val mediaCount: Int,
)

/** 进入页面时先补齐的收藏夹数量，保证首屏即可看到封面。 */
internal const val FAVORITE_FOLDER_COVER_INITIAL_WINDOW = 8

/** 可见范围之后额外预取的收藏夹数量。 */
internal const val FAVORITE_FOLDER_COVER_PREFETCH_DISTANCE = 4

/** 补封面请求的并发上限：fav/resource/list 对突发并发敏感，过高易触发 412/429。 */
internal const val FAVORITE_FOLDER_COVER_CONCURRENCY = 2

/**
 * 从候选收藏夹中挑出仍需要网络补封面的目标。
 *
 * 过滤规则：封面为空、media_count > 0、media_id 有效，且本次会话未解析过；
 * 同一 media_id 只保留一次。
 */
internal fun resolveFavoriteFolderCoverFetchTargets(
    folders: List<FavFolder>,
    alreadyResolved: Set<Long>,
): List<FavoriteFolderCoverTarget> {
    if (folders.isEmpty()) return emptyList()
    val seen = HashSet<Long>(folders.size)
    return folders.asSequence()
        .mapNotNull { folder ->
            val mediaId = resolveFavoriteFolderMediaId(folder)
            if (mediaId <= 0L) return@mapNotNull null
            if (!seen.add(mediaId)) return@mapNotNull null
            if (folder.cover.isNotBlank()) return@mapNotNull null
            if (folder.media_count <= 0) return@mapNotNull null
            if (mediaId in alreadyResolved) return@mapNotNull null
            FavoriteFolderCoverTarget(mediaId = mediaId, mediaCount = folder.media_count)
        }
        .toList()
}

/**
 * 需要补封面的收藏夹前缀终点下标（含）。
 *
 * 采用「从 0 补到可见前沿 + 预取距离」而不是只补当前可见窗口：快速滚动跳过的
 * 区间不会留下永久空封面，配合会话内去重集合，每个收藏夹最多只请求一次。
 * 返回 -1 表示暂无可补目标。
 */
internal fun resolveFavoriteFolderCoverPrefetchEnd(
    folderCount: Int,
    lastVisibleIndex: Int,
    prefetchDistance: Int = FAVORITE_FOLDER_COVER_PREFETCH_DISTANCE,
): Int {
    if (folderCount <= 0 || lastVisibleIndex < 0) return -1
    return (lastVisibleIndex + prefetchDistance.coerceAtLeast(0))
        .coerceIn(0, folderCount - 1)
}
