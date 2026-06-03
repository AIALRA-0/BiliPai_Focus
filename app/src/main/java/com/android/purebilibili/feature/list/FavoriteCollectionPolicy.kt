package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource
import com.android.purebilibili.data.model.response.VideoItem

data class FavoriteCollectionRoute(
    val type: String,
    val id: Long,
    val mid: Long,
    val title: String,
    val ownerName: String = ""
)

internal fun mergeFavoriteFoldersForDisplay(
    ownedFolders: List<FavFolder>,
    subscribedFolders: List<FavFolder>
): List<FavFolder> {
    val seenIds = HashSet<Long>()
    return (ownedFolders + subscribedFolders).filter { folder ->
        val valid = folder.id > 0L && folder.title.isNotBlank()
        valid && seenIds.add(folder.id)
    }
}

internal fun resolveFavoriteFolderTabLabel(folder: FavFolder): String {
    return if (folder.source == FavFolderSource.SUBSCRIBED) {
        "${folder.title} · 订阅"
    } else {
        folder.title
    }
}

internal fun resolveFavoriteFolderMediaId(folder: FavFolder): Long {
    if (folder.source != FavFolderSource.SUBSCRIBED) {
        return folder.id.takeIf { it > 0L } ?: folder.fid
    }

    val normalizedFromFid = when {
        folder.fid > 0L && folder.mid > 0L -> {
            val suffix = (folder.mid % 100L).toString().padStart(2, '0')
            "${folder.fid}$suffix".toLongOrNull()
        }
        else -> null
    }

    return when {
        folder.id > 0L && folder.id == normalizedFromFid -> folder.id
        folder.id > 0L && folder.id != folder.fid && folder.id > 100_000_000L -> folder.id
        normalizedFromFid != null -> normalizedFromFid
        folder.id > 0L -> folder.id
        else -> folder.fid
    }
}

internal fun resolveSubscribedFavoriteCollectionRoute(folder: FavFolder): FavoriteCollectionRoute? {
    if (folder.source != FavFolderSource.SUBSCRIBED || folder.type != 21) return null
    if (folder.id <= 0L || folder.title.isBlank()) return null
    return FavoriteCollectionRoute(
        type = "favorite_season",
        id = folder.id,
        mid = folder.mid,
        title = folder.title,
        ownerName = folder.upper?.name.orEmpty()
    )
}

internal fun filterFavoriteFoldersByQuery(
    folders: List<FavFolder>,
    query: String
): List<FavFolder> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return folders
    return folders.filter { folder ->
        folder.title.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun resolveFavoriteCollectionRoute(item: VideoItem): FavoriteCollectionRoute? {
    if (!item.isCollectionResource || item.collectionId <= 0L) return null
    return FavoriteCollectionRoute(
        type = "season",
        id = item.collectionId,
        mid = item.collectionMid,
        title = item.title,
        ownerName = item.owner.name
    )
}
