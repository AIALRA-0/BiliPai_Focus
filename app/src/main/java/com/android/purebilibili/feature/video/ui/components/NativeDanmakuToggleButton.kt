package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppIconButton

/** PiliPlus' dedicated dm_on / dm_off glyphs retain distinct on/off states. */
@Composable
fun NativeDanmakuToggleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    activeTint: Color,
    inactiveTint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
) {
    AppIconButton(
        onClick = onToggle,
        modifier = modifier.semantics {
            stateDescription = if (enabled) "弹幕已开启" else "弹幕已关闭"
        },
    ) {
        PiliPlusDanmakuGlyph(
            glyph = if (enabled) "\uE803" else "\uE802",
            description = if (enabled) "关闭弹幕" else "开启弹幕",
            tint = if (enabled) activeTint else inactiveTint,
            size = iconSize,
        )
    }
}
