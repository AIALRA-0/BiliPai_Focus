package com.android.purebilibili.data.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 文章收藏列表提供图文身份，取消收藏按该身份提交。 */
internal fun buildFavoriteOpusRemovalRequest(opusId: Long): JsonObject {
    require(opusId > 0L) { "收藏内容编号无效" }
    return JsonObject(mapOf(
        "entity" to JsonObject(mapOf(
            "object_id_str" to JsonPrimitive(opusId.toString()),
            "type" to JsonObject(mapOf("biz" to JsonPrimitive(2))),
        )),
        "action" to JsonPrimitive(4),
    ))
}
