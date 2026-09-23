package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.BangumiReviewItem
import com.android.purebilibili.data.model.response.BangumiReviewType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BangumiReviewPage(
    val items: List<BangumiReviewItem>,
    val next: String,
    val count: Int,
    val hasMore: Boolean
)

object BangumiReviewRepository {
    suspend fun getReviews(
        mediaId: Long,
        type: BangumiReviewType,
        cursor: String = "",
        sort: Int = 0
    ): Result<BangumiReviewPage> = withContext(Dispatchers.IO) {
        runCatching {
            val response = if (type == BangumiReviewType.SHORT) {
                NetworkModule.bangumiApi.getBangumiShortReviews(
                    mediaId = mediaId,
                    sort = sort,
                    cursor = cursor
                )
            } else {
                NetworkModule.bangumiApi.getBangumiLongReviews(
                    mediaId = mediaId,
                    sort = sort,
                    cursor = cursor
                )
            }
            if (response.code != 0) {
                error(response.message.ifBlank { "点评加载失败" })
            }
            val data = response.data ?: error("点评为空")
            val count = if (data.count > 0) data.count else data.total
            BangumiReviewPage(
                items = data.list,
                next = data.next,
                count = count,
                hasMore = data.next.isNotBlank()
            )
        }
    }

    suspend fun likeReview(mediaId: Long, reviewId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val csrf = TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) error("请先登录")
            val response = NetworkModule.bangumiApi.likeBangumiReview(
                mediaId = mediaId,
                reviewId = reviewId,
                csrf = csrf
            )
            if (response.code != 0) {
                error(response.message.ifBlank { "点赞失败" })
            }
        }
    }

    suspend fun postShortReview(
        mediaId: Long,
        score: Int,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val csrf = TokenManager.csrfCache.orEmpty()
            if (csrf.isBlank()) error("请先登录")
            val response = NetworkModule.bangumiApi.postBangumiShortReview(
                mediaId = mediaId,
                score = score.coerceIn(2, 10),
                content = content.trim(),
                csrf = csrf
            )
            if (response.code != 0) {
                error(response.message.ifBlank { "发布失败" })
            }
        }
    }
}