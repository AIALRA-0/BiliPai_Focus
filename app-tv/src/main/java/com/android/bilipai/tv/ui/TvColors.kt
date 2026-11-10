package com.android.bilipai.tv.ui

import androidx.compose.ui.graphics.Color

/**
 * TV 媒体场景专用色：氛围背景与播放器遮罩共用的同一族暗色底。
 * 只收编媒体 scrim/遮罩类色值；界面角色色一律走 MaterialTheme.colorScheme，
 * 品牌色一律走 design-tokens（com.android.purebilibili.core.theme）。
 */
internal object TvMediaColors {
    /** 氛围背景/播放器的统一暗底（hero 兜底、字幕面板同源）。 */
    val Base = Color(0xFF10141F)

    /** 氛围渐变顶部（比 Base 更深的近黑）。 */
    val Deep = Color(0xFF0B0D14)

    /** 半透明媒体面板：进度轨面板与加载指示 pill。 */
    val Panel = Base.copy(alpha = 0.70f)

    /** 较实的媒体浮层：播放失败面板。 */
    val PanelStrong = Base.copy(alpha = 0.87f)

    /** 字幕/文字阴影底（纯黑半透明）。 */
    val OverlaidTextBackdrop = Color(0xCC000000)
}
