package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.TopicTopDetails
import com.android.purebilibili.data.model.response.TopicSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TopicFeedPage(
    val items: List<DynamicItem>,
    val offset: String,
    val hasMore: Boolean,
    val sortOptions: List<TopicSortOption> = emptyList(),
    val selectedSortBy: Int = 0,
)

object TopicRepository {

    suspend fun getTopicDetail(topicId: Long): Result<TopicTopDetails> = withContext(Dispatchers.IO) {
        try {
            if (topicId <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("topicId 不能为空"))
            }
            val response = NetworkModule.dynamicApi.getTopicDetail(topicId = topicId)
            if (response.code != 0) {
                return@withContext Result.failure(Exception(response.message.ifBlank { "话题详情加载失败 (${response.code})" }))
            }
            val details = response.data?.topDetails
                ?: return@withContext Result.failure(Exception("话题详情为空"))
            Result.success(details)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getTopicFeed(
        topicId: Long,
        offset: String = "",
        sortBy: Int = 0,
    ): Result<TopicFeedPage> = withContext(Dispatchers.IO) {
        try {
            if (topicId <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("topicId 不能为空"))
            }
            val response = NetworkModule.dynamicApi.getTopicFeed(
                topicId = topicId,
                offset = offset,
                sortBy = sortBy,
            )
            if (response.code != 0) {
                return@withContext Result.failure(Exception(response.message.ifBlank { "话题动态加载失败 (${response.code})" }))
            }
            val cardList = response.data?.topicCardList
                ?: return@withContext Result.success(
                    TopicFeedPage(
                        items = emptyList(),
                        offset = offset,
                        hasMore = false,
                        sortOptions = emptyList(),
                        selectedSortBy = sortBy,
                    )
                )
            val sortConfig = cardList.topicSortByConf
            Result.success(
                TopicFeedPage(
                    items = cardList.items.mapNotNull { it.dynamicCardItem }.filter { it.visible },
                    offset = cardList.offset,
                    hasMore = cardList.hasMore,
                    sortOptions = sortConfig?.allSortBy.orEmpty(),
                    selectedSortBy = sortConfig?.showSortBy ?: sortBy,
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
