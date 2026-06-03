package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.HistoryData
import com.android.purebilibili.data.model.response.HistoryCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 历史记录数据结果（包含列表和游标）
 */
data class HistoryResult(
    val list: List<HistoryData>,
    val cursor: HistoryCursor?
)

internal data class HistoryCursorQuery(
    val max: Long?,
    val viewAt: Long?,
    val business: String?
)

internal fun resolveHistoryCursorQuery(
    max: Long,
    viewAt: Long,
    business: String?
): HistoryCursorQuery {
    val normalizedBusiness = business?.trim()?.takeIf { it.isNotEmpty() }
    val hasCursor = max > 0L || viewAt > 0L || normalizedBusiness != null
    if (!hasCursor) {
        return HistoryCursorQuery(
            max = null,
            viewAt = null,
            business = null
        )
    }
    return HistoryCursorQuery(
        max = max.takeIf { it > 0L },
        viewAt = viewAt.takeIf { it > 0L },
        business = normalizedBusiness
    )
}

object HistoryRepository {
    private val api = NetworkModule.api

    /**
     * 获取历史记录列表（支持游标分页）
     * @param ps 每页数量
     * @param max 游标: 上一页最后一条的 oid (首次请求传 0)
     * @param viewAt 游标: 上一页最后一条的 view_at (首次请求传 0)
     */
    suspend fun getHistoryList(
        ps: Int = 30,
        max: Long = 0,
        viewAt: Long = 0,
        business: String? = null
    ): Result<HistoryResult> {
        return withContext(Dispatchers.IO) {
            try {
                val cursorQuery = resolveHistoryCursorQuery(
                    max = max,
                    viewAt = viewAt,
                    business = business
                )
                com.android.purebilibili.core.util.Logger.d(
                    "HistoryRepo",
                    "🔴 Fetching history: ps=$ps, max=${cursorQuery.max}, viewAt=${cursorQuery.viewAt}, business=${cursorQuery.business}"
                )
                val response = api.getHistoryList(
                    ps = ps,
                    max = cursorQuery.max,
                    viewAt = cursorQuery.viewAt,
                    business = cursorQuery.business
                )
                com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Response code=${response.code}, items=${response.data?.list?.size ?: 0}")
                
                if (response.code == 0) {
                    val list = response.data?.list ?: emptyList()
                    val cursor = response.data?.cursor
                    com.android.purebilibili.core.util.Logger.d(
                        "HistoryRepo",
                        "🔴 Cursor: max=${cursor?.max}, view_at=${cursor?.view_at}, business=${cursor?.business}"
                    )
                    Result.success(HistoryResult(list, cursor))
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryRepo", " Error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun deleteHistoryItem(
        kid: String,
        csrf: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteHistoryItem(kid = kid, csrf = csrf)
                if (response.code == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "删除历史失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun clearHistory(csrf: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.clearHistory(csrf = csrf)
                if (response.code == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "清空历史失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getHistoryPaused(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getHistoryShadow()
                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "查询历史记录状态失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun setHistoryPaused(
        paused: Boolean,
        csrf: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.setHistoryShadow(shadowSwitch = paused, csrf = csrf)
                if (response.code == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message.ifEmpty { "设置历史记录状态失败: ${response.code}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
