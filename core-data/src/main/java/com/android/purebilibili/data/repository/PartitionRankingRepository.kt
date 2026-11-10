package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** 分区剧集榜单使用独立接口；只返回完整榜单，不伪造分页。 */
object PartitionRankingRepository {
    suspend fun getSeasonRanking(seasonType: Int): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.network.BuvidManager.ensureBuvid3()
            val keys = WbiKeyManager.getWbiKeys().getOrElse { throw it }
            val params = WbiUtils.sign(
                params = mapOf("day" to "3", "season_type" to seasonType.toString()),
                imgKey = keys.first,
                subKey = keys.second,
            )
            val response = if (seasonType == 1) NetworkModule.api.getPgcRanking(params)
                else NetworkModule.api.getPgcSeasonRanking(params)
            Result.success(decodePartitionSeasonRanking(response))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

internal fun decodePartitionSeasonRanking(response: JsonObject): List<VideoItem> {
    fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun JsonObject.number(key: String): Long = text(key).toLongOrNull() ?: 0L
    check(response.text("code") == "0") {
        response.text("message").ifBlank { "剧集排行榜加载失败" }
    }
    val payload = (response["result"] as? JsonObject) ?: (response["data"] as? JsonObject)
        ?: error("剧集排行榜数据缺失")
    val entries = payload["list"] as? JsonArray ?: error("剧集排行榜列表缺失")
    return entries.mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        val seasonId = item.number("season_id")
        if (seasonId <= 0) return@mapNotNull null
        val stats = item["stat"] as? JsonObject
        val newEpisode = item["new_ep"] as? JsonObject
        VideoItem(
            id = seasonId,
            title = item.text("title"),
            pic = item.text("cover"),
            owner = Owner(name = newEpisode?.text("index_show").orEmpty()),
            stat = Stat(
                view = (stats?.number("view") ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                favorite = (stats?.number("follow") ?: 0L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
            ),
            contentType = "pgc_rank",
            navigationUrl = "https://www.bilibili.com/bangumi/play/ss$seasonId",
        )
    }
}
