package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.subtitle.*
import com.android.purebilibili.data.model.response.PlayerInfoData
import okhttp3.CacheControl
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

object SubtitleContentRepository {
    private val cache = ConcurrentHashMap<String, List<SubtitleCue>>()
    fun cachedCues(): List<List<SubtitleCue>> = cache.values.toList()
    fun clearCache() = cache.clear()
    suspend fun playerInfo(bvid: String, cid: Long): Result<PlayerInfoData> = dataRequest {
        val keys = WbiKeyManager.getWbiKeys().getOrThrow()
        val response = NetworkModule.api.getPlayerInfo(WbiUtils.sign(mapOf("bvid" to bvid, "cid" to cid.toString()), keys.first, keys.second))
        if (response.code != 0) throw ContentRequestException(response.code, response.message)
        response.data ?: error("播放器信息为空")
    }
    suspend fun cues(subtitleUrl: String, bvid: String, cid: Long, subtitleId: Long = 0,
        subtitleIdStr: String = "", subtitleLan: String = ""): Result<List<SubtitleCue>> = dataRequest {
        require(bvid.isNotBlank() && cid > 0) { "字幕归属视频信息缺失" }
        val url = normalizeBilibiliSubtitleUrl(subtitleUrl)
        require(isTrustedBilibiliSubtitleUrl(url)) { "字幕地址无效" }
        val key = "$bvid:$cid:$subtitleId:$subtitleIdStr:$subtitleLan:$url"
        cache[key] ?: NetworkModule.okHttpClient.newCall(Request.Builder().url(url)
            .cacheControl(CacheControl.FORCE_NETWORK).header("Referer", "https://www.bilibili.com").build())
            .execute().use { response ->
                check(response.isSuccessful) { "字幕请求失败（HTTP ${response.code}）" }
                parseBiliSubtitleBody(response.body.string()).also {
                    if (cache.size >= 48) cache.clear()
                    cache[key] = it
                }
            }
    }
}
