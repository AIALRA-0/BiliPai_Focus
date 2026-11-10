package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.DynamicRepostContentItem
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.DynamicCreateFeedContent
import com.android.purebilibili.data.model.response.DynamicCreateFeedReq
import com.android.purebilibili.data.model.response.DynamicCreateFeedRequest
import com.android.purebilibili.data.model.response.DynamicVideoRepostResource
import com.android.purebilibili.data.model.response.DynamicVideoRepostSource
import com.android.purebilibili.data.model.response.resolveCreatedDynamicId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Publishes a native video dynamic through the web video-repost protocol (scene 5). */
object VideoDynamicShareRepository {
    suspend fun share(bvid: String, text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache.orEmpty()
            check(csrf.isNotBlank()) { "请先登录后分享到动态" }
            require(bvid.isNotBlank()) { "视频信息不完整" }
            require(text.length <= 2000) { "分享文字不能超过 2000 字" }
            val video = NetworkModule.api.getVideoInfo(bvid)
            check(video.code == 0) { video.message.ifBlank { "获取视频信息失败" } }
            val aid = video.data?.aid?.takeIf { it > 0L } ?: error("未获取到视频编号")
            return@withContext shareResource(aid, 8, text)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }
    suspend fun shareResource(
        resourceId: Long,
        dynamicType: Int,
        text: String,
        contents: List<DynamicRepostContentItem> = emptyList(),
        topic: com.android.purebilibili.data.model.response.DynamicCreateTopic? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache.orEmpty()
            check(csrf.isNotBlank()) { "请先登录后分享到动态" }
            require(resourceId > 0) { "分享资源不可用" }
            require(text.length <= 2000) { "分享文字不能超过 2000 字" }
            val request = DynamicCreateFeedRequest(
                dyn_req = DynamicCreateFeedReq(
                    content = DynamicCreateFeedContent(
                        contents = contents.ifEmpty { listOf(DynamicRepostContentItem(text.trim().ifBlank { "分享内容" }, 1, "")) },
                    ),
                    scene = 5,
                    topic = topic,
                    upload_id = "0_${System.currentTimeMillis() / 1000}_${Random.nextInt(1000, 10000)}",
                ),
                web_repost_src = DynamicVideoRepostSource(DynamicVideoRepostResource(rid = resourceId, dyn_type = dynamicType)),
            )
            val response = NetworkModule.dynamicApi.createFeedDynamic(csrf = csrf, body = request)
            check(response.code == 0) { response.message.ifBlank { "分享到动态失败" } }
            Result.success(resolveCreatedDynamicId(response.data).ifBlank { "ok" })
        } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }
    }

}
