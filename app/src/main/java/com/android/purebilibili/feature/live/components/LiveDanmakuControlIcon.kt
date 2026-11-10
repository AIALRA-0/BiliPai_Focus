package com.android.purebilibili.feature.live.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.video.ui.components.PiliPlusDanmakuGlyph

/** 直播弹幕入口复用普通视频字形，保留独立的开启与关闭状态。 */
@Composable
internal fun LiveDanmakuControlIcon(
    enabled: Boolean = true,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    PiliPlusDanmakuGlyph(
        glyph = if (enabled) "\uE803" else "\uE802",
        description = if (enabled) "关闭弹幕" else "开启弹幕",
        tint = tint,
        size = 22.dp,
        modifier = modifier,
    )
}
