package com.android.bilipai.tv

import android.content.Context
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.repository.ReleaseVersionPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Request

data class TvUpdateState(val loading: Boolean = false, val message: String? = null, val pageUrl: String? = null)

/** Reject phone releases, even if their version is newer. No APK is downloaded or installed. */
internal fun matchingTvRelease(metadata: JsonObject, assetNames: Set<String>, packageName: String, versionCode: Long): Boolean {
    if (metadata["appId"]?.jsonPrimitive?.content != packageName) return false
    if ((metadata["versionCode"]?.jsonPrimitive?.longOrNull ?: 0) <= versionCode) return false
    return metadata["artifacts"]?.jsonArray.orEmpty().any {
        val name = it.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()
        name.endsWith(".apk", ignoreCase = true) && name in assetNames
    }
}

object TvUpdateRepository {
    const val feedbackUrl = "https://github.com/jay3-yy/BiliPai/issues/new"
    private const val releases = "https://api.github.com/repos/jay3-yy/BiliPai/releases?per_page=20"
    private fun read(url: String): String {
        require(url.startsWith("https://api.github.com/") || url.startsWith("https://github.com/")) { "发行地址无效" }
        return NetworkModule.okHttpClient.newCall(Request.Builder().url(url).header("Accept", "application/vnd.github+json").build())
            .execute().use { response -> check(response.isSuccessful) { "检查更新失败（HTTP ${response.code}）" }; response.body.string() }
    }
    suspend fun check(context: Context): TvUpdateState = withContext(Dispatchers.IO) {
        try {
            val local = context.packageManager.getPackageInfo(context.packageName, 0)
            val candidates = Json.parseToJsonElement(read(releases)).jsonArray
            val matches = candidates.mapNotNull { element ->
                val release = element.jsonObject
                if (release["draft"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
                val assets = release["assets"]?.jsonArray.orEmpty().map { it.jsonObject }
                val metadataAsset = assets.firstOrNull { it["name"]?.jsonPrimitive?.content == "tv-build-metadata.json" }
                    ?: return@mapNotNull null
                val metadata = Json.parseToJsonElement(read(metadataAsset["browser_download_url"]!!.jsonPrimitive.content)).jsonObject
                if (!matchingTvRelease(metadata, assets.map { it["name"]!!.jsonPrimitive.content }.toSet(), context.packageName, local.longVersionCode)) return@mapNotNull null
                val version = metadata["versionName"]?.jsonPrimitive?.content.orEmpty()
                if (!ReleaseVersionPolicy.isRemoteNewer(local.versionName.orEmpty(), version)) return@mapNotNull null
                val page = release["html_url"]?.jsonPrimitive?.content?.takeIf { it.startsWith("https://github.com/jay3-yy/BiliPai/releases/") }
                    ?: return@mapNotNull null
                Triple(metadata["versionCode"]!!.jsonPrimitive.long, version, page)
            }
            matches.maxByOrNull { it.first }?.let { TvUpdateState(message = "TV 新版本 ${it.second}，扫码进入下载页", pageUrl = it.third) }
                ?: TvUpdateState(message = "暂无更新的 TV 发行包")
        } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { TvUpdateState(message = error.message ?: "检查更新失败，请重试") }
    }
}
