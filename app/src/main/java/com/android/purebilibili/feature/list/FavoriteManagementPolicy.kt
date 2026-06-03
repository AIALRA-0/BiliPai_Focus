package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource

internal enum class FavoriteResourceOrder(
    val apiValue: String,
    val label: String
) {
    FAVORITE_TIME("mtime", "收藏时间"),
    PLAY_COUNT("view", "播放量"),
    PUBLISH_TIME("pubtime", "投稿时间")
}

internal fun canCleanInvalidFavoriteResources(folder: FavFolder?): Boolean {
    return folder?.source == FavFolderSource.OWNED
}

internal fun resolveFavoriteCleanInvalidConfirmText(folderTitle: String): String {
    val title = folderTitle.trim().ifEmpty { "当前收藏夹" }
    return "确认清理「$title」里的失效内容吗？本操作会修改远端收藏夹。"
}
