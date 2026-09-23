package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem

/**
 * 「首页推荐流 App+Web 合并」策略。
 *
 * 移植自 BiliPai 首页推荐流合并模式：同时请求 web 端与 app 端两个推荐接口，
 * 将两者交错合并、按视频去重，从而让首页获得两平台推荐的整体并集。
 *
 * 纯函数、无副作用，便于单元测试。
 */
internal object HomeFeedMergePolicy {

    /**
     * 交错合并 web 与 app 两条推荐流，返回去重后的列表。
     *
     * - 按索引交替取 app[i]、web[i]（app 在前）。
     * - 按 [keyOf] 生成每种视频的唯一 key，重复项只保留首个。
     * - 一方耗尽后，将另一方剩余项追加到末尾。
     * - 任一输入为空时安全返回另一侧（或空列表）。
     */
    fun mergeFeeds(web: List<VideoItem>, app: List<VideoItem>): List<VideoItem> {
        if (web.isEmpty()) return app
        if (app.isEmpty()) return web

        val seen = hashSetOf<String>()
        val merged = ArrayList<VideoItem>(web.size + app.size)

        val maxLen = maxOf(web.size, app.size)
        for (i in 0 until maxLen) {
            if (i < app.size) {
                val item = app[i]
                if (seen.add(keyOf(item))) merged.add(item)
            }
            if (i < web.size) {
                val item = web[i]
                if (seen.add(keyOf(item))) merged.add(item)
            }
        }
        return merged
    }

    /**
     * 生成视频唯一 key，与 HomeViewModel.videoItemKey 保持一致：
     * 优先生态：dynamicId > bvid > aid > id。
     */
    fun keyOf(item: VideoItem): String {
        if (item.dynamicId.isNotBlank()) return "dyn:${item.dynamicId}"
        if (item.bvid.isNotBlank()) return "bvid:${item.bvid}"
        if (item.aid > 0) return "aid:${item.aid}"
        if (item.id > 0) return "id:${item.id}"
        return "${item.owner.mid}:${item.title}:${item.pubdate}"
    }
}