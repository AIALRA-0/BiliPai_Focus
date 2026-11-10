package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.SplashItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class OfficialWallpaperCatalog(
    val items: List<SplashItem>,
    val failedSources: List<String> = emptyList(),
)

object SplashRepository {
    private val api = NetworkModule.splashApi

    /** Combine current official lists with complete community archive indexes. */
    suspend fun getOfficialWallpapers(): Result<OfficialWallpaperCatalog> = withContext(Dispatchers.IO) {
        coroutineScope {
            val brand = async {
                loadWallpaperSource {
                    val response = api.getSplashBrandList(signedWallpaperParams())
                    check(response.code == 0) { response.message.ifBlank { "品牌壁纸加载失败" } }
                    val data = checkNotNull(response.data) { "品牌壁纸数据为空" }
                    data.list.map {
                        SplashItem(id = it.id, thumb = it.thumb, logoUrl = it.logoUrl, title = "官方壁纸 #${it.id}")
                    }
                }
            }
            val splash = async {
                loadWallpaperSource {
                    val response = api.getSplashList(signedWallpaperParams(includeDevice = true))
                    check(response.code == 0) { response.message.ifBlank { "开屏壁纸加载失败" } }
                    checkNotNull(response.data) { "开屏壁纸数据为空" }.list
                }
            }
            val splashArchive = async { loadWallpaperSource { WallpaperArchiveRepository.loadSplashArchive() } }
            val posterArchive = async { loadWallpaperSource { WallpaperArchiveRepository.loadPosterArchive() } }
            val posterIndex = async { loadWallpaperSource { WallpaperArchiveRepository.loadPosterIndex() } }
            val results = listOf(
                "品牌壁纸" to brand.await(),
                "开屏壁纸" to splash.await(),
                "历史开屏与壁纸娘归档" to splashArchive.await(),
                "历史插画归档" to posterArchive.await(),
                "历史插画索引" to posterIndex.await(),
            )
            if (results.all { it.second.isFailure }) {
                Result.failure(results.first().second.exceptionOrNull() ?: IllegalStateException("壁纸加载失败"))
            } else {
                Result.success(
                    OfficialWallpaperCatalog(
                        items = mergeOfficialWallpaperCatalogs(
                            *results.map { it.second.getOrDefault(emptyList()) }.toTypedArray()
                        ),
                        failedSources = results.filter { it.second.isFailure }.map { it.first },
                    )
                )
            }
        }
    }

    private fun signedWallpaperParams(includeDevice: Boolean = false): Map<String, String> {
        val params = mutableMapOf(
            "appkey" to AppSignUtils.ANDROID_APP_KEY,
            "ts" to AppSignUtils.getTimestamp().toString(),
        )
        if (includeDevice) {
            params.putAll(
                mapOf(
                    "mobi_app" to "android", "device" to "android", "platform" to "android",
                    "build" to "7930010", "channel" to "master", "width" to "1080",
                    "height" to "1920", "ver" to "1055052953259468962",
                )
            )
        }
        return AppSignUtils.signForAndroidApi(params)
    }

    private suspend fun loadWallpaperSource(load: suspend () -> List<SplashItem>): Result<List<SplashItem>> =
        try {
            Result.success(load())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
}

/** URL aliases deduplicate thumbnails and originals while keeping unrelated items with the same ID. */
internal fun mergeOfficialWallpaperCatalogs(vararg catalogs: List<SplashItem>): List<SplashItem> {
    val items = mutableListOf<SplashItem>()
    val aliases = mutableMapOf<String, Int>()
    catalogs.forEach { catalog ->
        catalog.filterNot { it.isAd }.forEach itemLoop@ { item ->
            val normalized = item.copy(thumb = normalizeWallpaperUrl(item.thumb), image = normalizeWallpaperUrl(item.image))
            val urls = listOf(normalized.thumb, normalized.image).filter { it.isNotBlank() }
            val contentKeys = urls + normalized.archiveContentHash.takeIf { it.isNotBlank() }
                .let { hash -> if (hash == null) emptyList() else listOf("git-blob:$hash") }
            if (urls.isEmpty()) return@itemLoop
            val existingIndex = contentKeys.firstNotNullOfOrNull { aliases[it] }
            val index = existingIndex ?: items.size
            if (existingIndex == null) {
                items.add(normalized)
            } else {
                val existing = items[index]
                items[index] = existing.copy(
                    thumb = existing.thumb.ifBlank { normalized.thumb },
                    image = existing.image.ifBlank { normalized.image },
                    title = existing.title.ifBlank { normalized.title },
                    logoUrl = existing.logoUrl.ifBlank { normalized.logoUrl },
                )
            }
            contentKeys.forEach { aliases[it] = index }
        }
    }
    return items
}

private fun normalizeWallpaperUrl(url: String): String {
    val trimmed = url.trim()
    return when {
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.startsWith("http://") -> "https:" + trimmed.removePrefix("http:")
        else -> trimmed
    }
}
