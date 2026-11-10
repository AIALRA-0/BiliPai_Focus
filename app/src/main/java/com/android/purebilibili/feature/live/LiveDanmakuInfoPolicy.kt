package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveMessageMedal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val liveDanmakuInfoJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/** 复制完整消息字段，同时保留服务端原始结构，便于检查表情、勋章及举报信息。 */
internal fun encodeLiveDanmakuInfo(item: LiveDanmakuItem): String = liveDanmakuInfoJson.encodeToString(item)

internal fun resolveLiveMessageMedal(item: LiveDanmakuItem): LiveMessageMedal? =
    item.medal ?: if (item.medalName.isNotBlank() && item.medalLevel > 0) {
        LiveMessageMedal(item.medalName, item.medalLevel, if (item.medalColor == 0) 0 else item.medalColor or 0xFF000000.toInt())
    } else null
