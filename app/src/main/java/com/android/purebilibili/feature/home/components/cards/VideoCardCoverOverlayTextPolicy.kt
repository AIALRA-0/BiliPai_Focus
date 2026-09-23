package com.android.purebilibili.feature.home.components.cards

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

/**
 * 封面叠字（播放量 / 弹幕 / 时长）轻阴影：浅色封面上保证可读，又不抢 Hero 过渡。
 */
internal fun resolveVideoCardCoverOverlayTextShadow(): Shadow {
    return Shadow(
        color = Color.Black.copy(alpha = 0.55f),
        offset = Offset(0f, 1f),
        blurRadius = 3.2f,
    )
}
