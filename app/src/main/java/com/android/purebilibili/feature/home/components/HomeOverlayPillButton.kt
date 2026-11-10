// 文件路径: feature/home/components/HomeOverlayPillButton.kt
package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.feature.home.HomeGlassResolvedColors

/**
 * 首页/动态信息流悬浮胶囊按钮的统一样式：玻璃半透明容器 + 白色描边 + 药丸形状。
 *
 * 「撤销刷新」「定位上次刷新」等悬浮提示必须共用本组件，否则同一屏上会出现
 * 实心 primaryContainer 与毛玻璃两种胶囊并存的割裂观感（开关壁纸都一样）。
 * 颜色由 [HomeGlassResolvedColors]（rememberHomeGlassPillColors）解析，
 * 玻璃/模糊关闭时自然退化为半透明卡片色。
 */
@Composable
internal fun HomeOverlayPillButton(
    overlayPillColors: HomeGlassResolvedColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Pill),
        containerColor = overlayPillColors.containerColor,
        contentColor = AppContentColors.primaryText(),
        border = BorderStroke(AppSpacingTokens.Micro * 0.4f, overlayPillColors.borderColor),
        defaultElevation = AppSpacingTokens.ExtraSmall,
        pressedElevation = AppSpacingTokens.Micro,
        contentPadding = PaddingValues(
            horizontal = AppSpacingTokens.Large,
            vertical = AppSpacingTokens.Small + AppSpacingTokens.Micro,
        ),
        content = content,
    )
}
