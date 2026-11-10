package com.android.purebilibili.core.ui.wallpaper

import androidx.compose.runtime.compositionLocalOf
import com.android.purebilibili.core.store.home.HomeWallpaperSelection

/** One atomic selection shared by the app root, home, chat and wallpaper surfaces. */
internal val LocalHomeWallpaperSelection = compositionLocalOf { HomeWallpaperSelection() }
