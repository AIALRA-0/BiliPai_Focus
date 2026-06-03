package com.android.purebilibili.feature.profile

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource
import com.android.purebilibili.feature.list.resolveFavoriteFolderMediaId

data class ProfileFavoriteFolderShortcut(
    val mediaId: Long,
    val ownerMid: Long,
    val title: String,
    val mediaCount: Int,
    val isSubscribed: Boolean
)

internal fun resolveProfileFavoriteFolderShortcuts(
    folders: List<FavFolder>,
    ownerMid: Long,
    maxCount: Int = 4
): List<ProfileFavoriteFolderShortcut> {
    if (maxCount <= 0) return emptyList()

    val seenMediaIds = HashSet<Long>()
    return folders.asSequence()
        .mapNotNull { folder ->
            val mediaId = resolveFavoriteFolderMediaId(folder)
            val title = folder.title.trim()
            if (mediaId <= 0L || title.isBlank() || folder.media_count <= 0) {
                null
            } else {
                ProfileFavoriteFolderShortcut(
                    mediaId = mediaId,
                    ownerMid = folder.mid.takeIf { it > 0L } ?: ownerMid,
                    title = title,
                    mediaCount = folder.media_count,
                    isSubscribed = folder.source == FavFolderSource.SUBSCRIBED
                )
            }
        }
        .filter { seenMediaIds.add(it.mediaId) }
        .take(maxCount)
        .toList()
}
