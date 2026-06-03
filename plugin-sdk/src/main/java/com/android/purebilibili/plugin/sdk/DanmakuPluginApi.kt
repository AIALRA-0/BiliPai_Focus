package com.android.purebilibili.plugin.sdk

interface DanmakuPluginApi {
    val capabilityManifest: PluginCapabilityManifest

    fun filterDanmaku(danmaku: DanmakuItem): DanmakuItem?

    fun styleDanmaku(danmaku: DanmakuItem): DanmakuStyle? = null
}

data class DanmakuItem(
    val id: Long,
    val content: String,
    val timeMs: Long,
    val type: Int = 1,
    val color: Int = 0xFFFFFF,
    val userId: String = ""
)

data class DanmakuStyle(
    val textColor: Int? = null,
    val borderColor: Int? = null,
    val backgroundColor: Int? = null,
    val bold: Boolean = false,
    val scale: Float = 1.0f
)
