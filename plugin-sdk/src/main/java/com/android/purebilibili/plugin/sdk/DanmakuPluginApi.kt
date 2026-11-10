package com.android.purebilibili.plugin.sdk

interface DanmakuPluginApi : Plugin {

    fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem?

    fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle? = null
}

data class DanmakuItem(
    val id: Long,
    val content: String,
    val timeMs: Long,
    val type: Int = 1,
    /** RRGGBB source color; alpha is supplied by the host renderer. */
    val color: Int = 0xFFFFFF,
    val userId: String = ""
)

/** Optional colors are ARGB; only null means unspecified (transparent black is a color). */
data class DanmakuStyle(
    val textColor: Int? = null,
    val borderColor: Int? = null,
    val backgroundColor: Int? = null,
    val bold: Boolean = false,
    val scale: Float = 1.0f
)
