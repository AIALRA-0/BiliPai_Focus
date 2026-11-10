package com.android.purebilibili.core.store.home

import com.android.purebilibili.core.ui.wallpaper.WallpaperTransform

/** URI and both device transforms are published as one persisted selection. */
data class HomeWallpaperSelection(
    val uri: String = "",
    val mobileTransform: WallpaperTransform = WallpaperTransform(),
    val tabletTransform: WallpaperTransform = WallpaperTransform(),
)
