package com.android.purebilibili.feature.common

internal fun resolveIndexedVideoLazyKey(
    namespace: String,
    index: Int,
    bvid: String,
    id: Long = 0L,
    aid: Long = 0L,
    cid: Long = 0L
): String {
    val identity = when {
        bvid.isNotBlank() -> bvid.trim()
        id > 0L -> "id_$id"
        aid > 0L -> "aid_$aid"
        cid > 0L -> "cid_$cid"
        else -> "unknown"
    }
    // 接口推荐流可能重复返回同一 BV，Lazy 容器的渲染 key 必须额外带位置去重。
    return "${namespace}_${identity}_${index.coerceAtLeast(0)}"
}
