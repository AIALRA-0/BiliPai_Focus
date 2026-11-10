package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.ContentDisplaySettings
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import kotlinx.serialization.json.contentOrNull

internal fun isGoodsReply(reply: ReplyItem): Boolean =
    reply.content.message.contains("https://gaoneng.bilibili.com/tetris") ||
        reply.content.urls.values.any { url ->
            url.isGoods || url.extra?.let { extra ->
                extra["goods_cm_control"]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.contentOrNull == "1" ||
                    extra.containsKey("goods_item_id") || extra.containsKey("goods_prefetched_cache")
            } == true
        }

/** 只过滤展示内容，保留服务端总数、页码、来源和继续加载游标。 */
internal fun filterGoodsReplies(data: ReplyData): ReplyData {
    fun clean(reply: ReplyItem?): ReplyItem? = reply?.takeUnless(::isGoodsReply)?.let {
        it.copy(replies = it.replies?.mapNotNull { child -> clean(child) })
    }
    return data.copy(
        replies = data.replies?.mapNotNull(::clean),
        hots = data.hots?.mapNotNull(::clean),
        topReplies = data.topReplies?.mapNotNull(::clean),
        root = clean(data.root),
        top = data.top?.let { it.copy(admin = clean(it.admin), upper = clean(it.upper), vote = clean(it.vote)) },
        upper = data.upper?.let { it.copy(top = clean(it.top)) },
    )
}

internal suspend fun filterGoodsRepliesIfEnabled(data: ReplyData): ReplyData {
    val context = NetworkModule.appContext ?: return data
    return if (ContentDisplaySettings.enabled(context, ContentDisplaySettings.BLOCK_GOODS_REPLY)) {
        filterGoodsReplies(data)
    } else data
}
