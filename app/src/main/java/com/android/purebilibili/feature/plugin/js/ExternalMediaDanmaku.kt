package com.android.purebilibili.feature.plugin.js

import com.android.purebilibili.core.plugin.js.BiliPaiJsDanmuComment
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_BOTTOM
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_TOP
import com.android.purebilibili.danmaku.engine.DanmakuItem

/** 把 JS 插件返回的弹幕条目映射成引擎渲染模型；空文本丢弃，时间钳制到非负。 */
internal fun mapJsDanmuCommentsToItems(comments: List<BiliPaiJsDanmuComment>): List<DanmakuItem> {
    return buildList {
        comments.forEachIndexed { index, comment ->
            val text = comment.text.trim()
            if (text.isEmpty()) return@forEachIndexed
            add(
                DanmakuItem().apply {
                    danmakuId = (index + 1).toLong()
                    this.text = text
                    showAtTime = comment.timeMs.coerceAtLeast(0L)
                    layerType = when (comment.mode.lowercase()) {
                        "top" -> DANMAKU_LAYER_TOP
                        "bottom" -> DANMAKU_LAYER_BOTTOM
                        else -> DANMAKU_LAYER_SCROLL
                    }
                    textColor = comment.color?.let(::parseJsDanmuColorInt)
                    textSizeScale = 1f
                }
            )
        }
    }
}

/** 支持 `#RRGGBB`、`#AARRGGBB` 和十进制整数；解析失败返回 null 用宿主默认色。 */
internal fun parseJsDanmuColorInt(raw: String): Int? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    val hex = value.removePrefix("#")
    return when (hex.length) {
        6 -> hex.toLongOrNull(16)?.let { (0xFF000000L or it).toInt() }
        8 -> hex.toLongOrNull(16)?.toInt()
        else -> value.toLongOrNull()?.toInt()
    }
}
