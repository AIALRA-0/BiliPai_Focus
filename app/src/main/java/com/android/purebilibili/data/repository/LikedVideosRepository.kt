package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.getSpaceLikedArchive
import com.android.purebilibili.core.util.IdUtils
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LikedVideosRepository {
    data class Page(
        val items: List<VideoItem>,
        val total: Int,
    )

    suspend fun getLikedVideos(
        mid: Long,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<Page> = withContext(Dispatchers.IO) {
        runCatching {
            // 对齐桌面版 PiliPlus (MemberHttp.likeArc):
            // 优先使用客户端 /x/v2/space/likearc 接口进行分页拉取，突破 Web 接口只返回最近 20 条且不支持翻页的限制
            val appResponse = runCatching {
                NetworkModule.spaceApi.getSpaceLikedArchive(mid = mid, page = page, pageSize = pageSize)
            }.getOrNull()

            val response = if (
                appResponse != null &&
                appResponse.code == 0 &&
                (appResponse.data?.item?.isNotEmpty() == true || (appResponse.data?.count ?: 0) > 0 || page > 1)
            ) {
                appResponse
            } else {
                // 降级使用 Web 接口 /x/space/like/video
                NetworkModule.api.getLikedVideos(mid = mid, page = page, pageSize = pageSize)
            }

            check(response.code == 0) {
                response.message.ifBlank { "获取点赞视频失败：${response.code}" }
            }
            val data = response.data
            val detailedItems = data?.list.orEmpty().map { it.toVideoItem() }
            val aggregateItems = data?.item.orEmpty().map { item ->
                val resolvedAid = if (item.aid > 0L) item.aid else item.param.toLongOrNull() ?: 0L
                val resolvedBvid = item.bvid.ifBlank {
                    if (item.param.startsWith("BV", ignoreCase = true)) {
                        item.param
                    } else if (resolvedAid > 0L) {
                        IdUtils.av2bv(resolvedAid)
                    } else {
                        ""
                    }
                }
                VideoItem(
                    id = resolvedAid,
                    aid = resolvedAid,
                    bvid = resolvedBvid,
                    cid = item.firstCid,
                    title = item.title,
                    pic = item.cover,
                    owner = com.android.purebilibili.data.model.response.Owner(name = item.author),
                    stat = com.android.purebilibili.data.model.response.Stat(
                        view = item.play,
                        danmaku = item.danmaku,
                        reply = item.reply,
                    ),
                    duration = item.duration,
                    pubdate = item.ctime,
                    tname = item.tname,
                )
            }
            val items = detailedItems.ifEmpty { aggregateItems }
            Page(
                items = items,
                total = (data?.count ?: 0).takeIf { it > 0 } ?: items.size,
            )
        }
    }
}
