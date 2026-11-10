package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ColorScheme
import com.android.purebilibili.core.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun rememberDanmakuPanelColors(scheme: ColorScheme): DanmakuSettingsPanelSurfaceColors =
    remember(scheme) { resolveDanmakuSettingsPanelSurfaceColors(scheme) }

@Composable
internal fun DanmakuSyncButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentPadding: PaddingValues,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = contentPadding,
        content = content,
    )
}
