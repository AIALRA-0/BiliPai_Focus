package com.android.purebilibili.feature.video.usecase

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoRelationData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Unlike the legacy Boolean queries, failures never masquerade as an unspent/unfavorited video. */
internal suspend fun queryVideoEngagementRelationship(aid: Long, bvid: String): Result<VideoRelationData> =
    withContext(Dispatchers.IO) {
        try {
            val response = NetworkModule.api.getVideoRelation(aid = aid, bvid = bvid)
            val data = response.data
            if (response.code == 0 && data != null) Result.success(data)
            else Result.failure(IllegalStateException(response.message.ifBlank { "无法获取视频互动状态" }))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
