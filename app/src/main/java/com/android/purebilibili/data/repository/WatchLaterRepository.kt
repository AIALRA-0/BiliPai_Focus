package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WatchLaterPage(
    val items: List<VideoItem>,
    val totalCount: Int,
    val hasMore: Boolean,
)

internal fun buildWatchLaterPageParams(
    page: Int,
    viewed: Int,
    keyword: String,
    ascending: Boolean,
): Map<String, String> = mapOf(
    "pn" to page.coerceAtLeast(1).toString(),
    "ps" to "20",
    "viewed" to viewed.toString(),
    "key" to keyword.trim(),
    "asc" to ascending.toString(),
    "need_split" to "true",
    "web_location" to "333.881",
)

object WatchLaterRepository {
    private const val PAGE_SIZE = 20
    private val api = NetworkModule.api

    suspend fun getPage(
        page: Int,
        viewed: Int,
        keyword: String,
        ascending: Boolean,
    ): Result<WatchLaterPage> = withContext(Dispatchers.IO) {
        apiCall {
            val params = buildWatchLaterPageParams(page, viewed, keyword, ascending)
            val keys = WbiKeyManager.getWbiKeys().getOrNull()
                ?: WbiKeyManager.refreshKeys().getOrThrow()
            val response = api.getWatchLaterPage(WbiUtils.sign(params, keys.first, keys.second))
            check(response.code == 0) { response.message.ifBlank { "加载稍后再看失败" } }
            val data = response.data
            val items = data?.list.orEmpty().map { item ->
                val badges = buildList {
                    if (item.chargingPay?.level != null) add("充电")
                    if (item.rights?.isCooperation == 1) add("合作")
                    item.pgcLabel?.takeIf(String::isNotBlank)?.let(::add)
                    if (item.isPugv == true) add("课程")
                }
                VideoItem(
                    id = item.aid,
                    aid = item.aid,
                    bvid = item.bvid.orEmpty(),
                    cid = item.cid ?: 0L,
                    title = item.title.orEmpty(),
                    pic = item.pic.orEmpty(),
                    duration = item.duration ?: 0,
                    progress = item.progress ?: -1,
                    owner = Owner(
                        mid = item.owner?.mid ?: 0L,
                        name = item.owner?.name.orEmpty(),
                        face = item.owner?.face.orEmpty(),
                    ),
                    stat = Stat(
                        view = item.stat?.view ?: 0,
                        danmaku = item.stat?.danmaku ?: 0,
                        reply = item.stat?.reply ?: 0,
                        like = item.stat?.like ?: 0,
                        coin = item.stat?.coin ?: 0,
                        favorite = item.stat?.favorite ?: 0,
                        share = item.stat?.share ?: 0,
                    ),
                    pubdate = item.pubdate ?: 0L,
                    contentType = badges.joinToString(" · "),
                    navigationUrl = item.redirectUrl.orEmpty(),
                )
            }
            val total = data?.count?.coerceAtLeast(items.size) ?: items.size
            WatchLaterPage(
                items = items,
                totalCount = total,
                hasMore = page * PAGE_SIZE < total,
            )
        }
    }

    suspend fun clear(cleanType: Int?): Result<Unit> = withContext(Dispatchers.IO) {
        apiCall {
            val response = api.clearWatchLater(cleanType = cleanType, csrf = requireCsrf())
            check(response.code == 0) { response.message.ifBlank { "清理稍后再看失败" } }
        }
    }

    suspend fun copyOrMoveToFavorite(
        targetMediaId: Long,
        aids: Set<Long>,
        copy: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        apiCall {
            check(aids.isNotEmpty()) { "请选择视频" }
            val csrf = requireCsrf()
            val resources = aids.joinToString(",")
            val response = if (copy) {
                api.copyWatchLaterToFavorite(
                    targetMediaId = targetMediaId,
                    mid = TokenManager.midCache ?: error("请先登录"),
                    resources = resources,
                    csrf = csrf,
                )
            } else {
                api.moveWatchLaterToFavorite(
                    targetMediaId = targetMediaId,
                    resources = resources,
                    csrf = csrf,
                )
            }
            check(response.code == 0) {
                response.message.ifBlank { if (copy) "复制失败" else "移动失败" }
            }
        }
    }

    private fun requireCsrf(): String = TokenManager.csrfCache.orEmpty().ifBlank { error("请先登录") }

    private suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
