package com.android.purebilibili.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible

/**
 * BiliPai 风格设置层级：单一不透明底色 + 透明导航 entry，避免全局壁纸半透明
 * Scaffold / 分组卡片在 push/pop 时叠色闪烁。
 */
@Composable
internal fun SettingsOpaqueSurfaceHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGlobalWallpaperBackdropVisible provides false) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppSurfaceTokens.groupedListContainer()),
        ) {
            content()
        }
    }
}
