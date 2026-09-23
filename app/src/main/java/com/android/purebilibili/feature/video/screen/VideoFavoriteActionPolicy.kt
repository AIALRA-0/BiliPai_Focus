package com.android.purebilibili.feature.video.screen

internal enum class VideoFavoriteEntryPoint {
    FullscreenOverlay,
    DetailActionRow,
    BottomInputBar
}

internal enum class VideoFavoriteAction {
    /** 直接默认收藏夹开关（兼容旧路径）。 */
    ToggleFavorite,
    /** 打开收藏夹选择，支持多选到自己的收藏夹。 */
    OpenFavoriteFolders,
}

/**
 * 收藏入口：统一打开收藏夹选择，提示用户可收藏到自己的收藏夹。
 * 取消收藏可在面板中取消勾选后保存。
 */
internal fun resolveVideoFavoriteAction(
    entryPoint: VideoFavoriteEntryPoint
): VideoFavoriteAction {
    return when (entryPoint) {
        VideoFavoriteEntryPoint.FullscreenOverlay,
        VideoFavoriteEntryPoint.DetailActionRow,
        VideoFavoriteEntryPoint.BottomInputBar -> VideoFavoriteAction.OpenFavoriteFolders
    }
}
