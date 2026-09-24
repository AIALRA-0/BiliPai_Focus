// 文件路径: data/repository/VideoRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.cache.PlayUrlCache
import com.android.purebilibili.core.coroutines.AppScope
import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.NetworkUtils
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.feature.video.progress.PbpProgressData
import com.android.purebilibili.feature.video.progress.parsePbpProgressData
import com.android.purebilibili.feature.video.subtitle.SubtitleCue
import com.android.purebilibili.feature.video.subtitle.normalizeBilibiliSubtitleUrl
import com.android.purebilibili.feature.video.subtitle.parseBiliSubtitleBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.purebilibili.feature.video.ui.pager.PORTRAIT_PLAYBACK_TARGET_QUALITY
import com.android.purebilibili.feature.video.ui.pager.shouldUsePortraitParallelPlaybackBootstrap
import kotlinx.coroutines.flow.first
import okhttp3.CacheControl
import okhttp3.Request
import java.io.InputStream
import java.security.MessageDigest
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

private const val SUBTITLE_CUE_CACHE_MAX_ENTRIES = 512
private const val SUBTITLE_CUE_CACHE_ENTRY_OVERHEAD_BYTES = 512L
private const val SUBTITLE_CUE_ESTIMATED_BYTES_PER_CUE = 160L
private val UGC_MAIN_REGION_TIDS = setOf(
    1, 3, 4, 5, 36, 119, 129, 155, 160, 181, 188, 202, 211, 217, 223, 234
)

/**
 * The ranking endpoint uses v2 region ids (100x), while the region feed uses
 * the legacy tid ids.  Keep the conversion in one place for the fallback so
 * a main region does not turn into a -400/-404 request (notably 资讯=202).
 */
private val REGION_TID_TO_RANKING_RID = mapOf(
    1 to 1005,   // 动画
    3 to 1003,   // 音乐
    4 to 1008,   // 游戏
    5 to 1002,   // 娱乐
    36 to 1010,  // 知识
    119 to 1007, // 鬼畜
    129 to 1004, // 舞蹈
    155 to 1014, // 时尚
    160 to 1015, // 生活
    181 to 1001, // 影视
    188 to 1012, // 科技
    202 to 1009, // 资讯
    211 to 1020, // 美食
    217 to 1024, // 动物圈
    223 to 1013, // 汽车
    234 to 1018  // 运动
)

internal fun resolveRegionRankingRid(tid: Int): Int? = REGION_TID_TO_RANKING_RID[tid]

internal fun shouldStartHomePreload(
    hasPreloadedData: Boolean,
    hasActivePreloadTask: Boolean
): Boolean {
    return !hasPreloadedData && !hasActivePreloadTask
}

internal fun shouldPrimeBuvidForHomePreload(feedApiType: SettingsManager.FeedApiType): Boolean {
    return feedApiType == SettingsManager.FeedApiType.MOBILE
}

internal fun shouldReuseInFlightPreloadForHomeRequest(
    idx: Int,
    isPreloading: Boolean,
    hasPreloadedData: Boolean
): Boolean {
    return idx == 0 && isPreloading && !hasPreloadedData
}

internal fun shouldFallbackRegionLatestToRanking(
    tid: Int,
    page: Int,
    latestVideoCount: Int,
    latestResponseCode: Int
): Boolean {
    return tid in UGC_MAIN_REGION_TIDS && page == 1 && (latestResponseCode != 0 || latestVideoCount == 0)
}

internal fun shouldReportHomeDataReadyForSplash(
    hasCompletedPreload: Boolean,
    hasPreloadedData: Boolean
): Boolean {
    return hasCompletedPreload || hasPreloadedData
}

internal fun resolveHomeFeedWbiKeys(
    cachedKeys: Pair<String, String>?,
    navWbiImg: WbiImg?
): Pair<String, String>? {
    if (cachedKeys != null) return cachedKeys
    val wbiImg = navWbiImg ?: return null
    val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
    val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
    return if (imgKey.isNotEmpty() && subKey.isNotEmpty()) imgKey to subKey else null
}

internal fun buildSubtitleCueCacheKey(
    bvid: String,
    cid: Long,
    subtitleId: Long,
    subtitleIdStr: String,
    subtitleLan: String,
    normalizedSubtitleUrl: String
): String {
    val urlHash = MessageDigest.getInstance("SHA-1")
        .digest(normalizedSubtitleUrl.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { "%02x".format(it) }
    val idPart = subtitleIdStr.takeIf { it.isNotBlank() }
        ?: subtitleId.takeIf { it > 0L }?.toString()
        ?: "no-id"
    return "${bvid.ifBlank { "unknown" }}:${cid.coerceAtLeast(0L)}:${idPart}:${subtitleLan.ifBlank { "unknown" }}:$urlHash"
}

internal fun estimateSubtitleCueCacheBytes(
    entryCount: Int,
    totalCueCount: Int
): Long {
    val normalizedEntryCount = entryCount.coerceAtLeast(0)
    val normalizedCueCount = totalCueCount.coerceAtLeast(0)
    return normalizedEntryCount * SUBTITLE_CUE_CACHE_ENTRY_OVERHEAD_BYTES +
        normalizedCueCount * SUBTITLE_CUE_ESTIMATED_BYTES_PER_CUE
}

data class SubtitleCueCacheStats(
    val entryCount: Int,
    val totalCueCount: Int,
    val estimatedBytes: Long
)

data class CreatorCardStats(
    val followerCount: Int,
    val videoCount: Int,
    val vipStatus: Int = 0,
    val officialType: Int = -1,
    val pendantImage: String = "",
)

object VideoRepository {
    private val api = NetworkModule.api
    private val buvidApi = NetworkModule.buvidApi
    private val subtitleCueCache = ConcurrentHashMap<String, List<SubtitleCue>>()
    private val creatorCardStatsCache = ConcurrentHashMap<Long, CreatorCardStats>()
    private val verticalVideoCache = ConcurrentHashMap<String, Boolean>()

    private val QUALITY_CHAIN = listOf(120, 116, 112, 80, 74, 64, 32, 16)
    private const val APP_API_COOLDOWN_MS = 120_000L
    private var appApiCooldownUntilMs = 0L
    
    //  [新增] 确保 buvid3 来自 Bilibili SPI API + 激活（解决 412 问题）
    private var buvidInitialized = false

    internal fun playbackAccount() = NetworkModule.playbackAccount()

    internal fun isUsingDedicatedPlaybackAccount(): Boolean = playbackAccount() != null

    internal fun hasPlaybackSessionCookie(): Boolean =
        !playbackAccount()?.sessData.isNullOrEmpty() || !TokenManager.sessDataCache.isNullOrEmpty()

    internal fun playbackAccessToken(): String? =
        playbackAccount()?.accessToken?.takeIf { it.isNotBlank() } ?: TokenManager.accessTokenCache

    internal fun playbackAccessTokenPlatform(): String =
        playbackAccount()?.accessTokenPlatform ?: TokenManager.accessTokenPlatformCache

    internal fun isPlaybackVip(): Boolean =
        playbackAccount()?.isVip ?: TokenManager.isVipCache

    /** 播放会话是否已登录（优先播放账号，其次主账号）。 */
    internal fun isPlaybackLoggedIn(): Boolean =
        resolveVideoPlaybackAuthState(
            hasSessionCookie = hasPlaybackSessionCookie(),
            hasAccessToken = !playbackAccessToken().isNullOrEmpty()
        )

    fun getSubtitleCueCacheStats(): SubtitleCueCacheStats {
        val snapshot = subtitleCueCache.values.toList()
        val entryCount = snapshot.size
        val totalCueCount = snapshot.sumOf { it.size }
        return SubtitleCueCacheStats(
            entryCount = entryCount,
            totalCueCount = totalCueCount,
            estimatedBytes = estimateSubtitleCueCacheBytes(
                entryCount = entryCount,
                totalCueCount = totalCueCount
            )
        )
    }

    fun clearSubtitleCueCache() {
        subtitleCueCache.clear()
    }

    internal fun getAppApiCooldownRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        return (appApiCooldownUntilMs - nowMs).coerceAtLeast(0L)
    }

    internal fun isAppApiCoolingDown(nowMs: Long = System.currentTimeMillis()): Boolean {
        return getAppApiCooldownRemainingMs(nowMs) > 0L
    }

    private fun isDirectedTrafficModeActive(): Boolean {
        val context = NetworkModule.appContext ?: return false
        val enabled = SettingsManager.getBiliDirectedTrafficEnabledSync(context)
        val isOnMobileData = NetworkUtils.isMobileData(context)
        return shouldEnableDirectedTrafficMode(
            directedTrafficEnabled = enabled,
            isOnMobileData = isOnMobileData
        )
    }

    suspend fun getVideoTitle(
        bvid: String,
        aid: Long = 0L
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val lookup = resolveVideoInfoLookupInput(rawBvid = bvid, aid = aid)
                ?: throw Exception("无效的视频标识: bvid=$bvid, aid=$aid")
            val response = if (lookup.bvid.isNotEmpty()) {
                api.getVideoInfo(lookup.bvid)
            } else {
                api.getVideoInfoByAid(lookup.aid)
            }
            val info = response.data ?: throw Exception("视频详情为空: ${response.code}")
            val title = info.title.trim()
            if (title.isEmpty()) throw Exception("视频标题为空")
            Result.success(title)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun ensureBuvid3FromSpi() {
        if (buvidInitialized) return
        try {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Fetching buvid3 from SPI API...")
            val response = buvidApi.getSpi()
            if (response.code == 0 && response.data != null) {
                val b3 = response.data.b_3
                if (b3.isNotEmpty()) {
                    TokenManager.buvid3Cache = b3
                    com.android.purebilibili.core.util.Logger.d("VideoRepo", " buvid3 from SPI: ${b3.take(20)}...")
                    
                    //  [关键] 激活 buvid (参考 PiliPala)
                    try {
                        activateBuvid()
                        com.android.purebilibili.core.util.Logger.d("VideoRepo", " buvid activated!")
                    } catch (e: Exception) {
                        android.util.Log.w("VideoRepo", "buvid activation failed: ${e.message}")
                    }
                    
                    buvidInitialized = true
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("VideoRepo", " Failed to get buvid3 from SPI: ${e.message}")
        }
    }
    
    /**
     * 公开的 buvid3 初始化函数 - 供其他 Repository 调用
     */
    suspend fun ensureBuvid3() {
        ensureBuvid3FromSpi()
    }
    
    //  激活 buvid (参考 PiliPala buvidActivate)
    private suspend fun activateBuvid() {
        val random = java.util.Random()
        val randBytes = ByteArray(32) { random.nextInt(256).toByte() }
        val endBytes = byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68) + ByteArray(4) { random.nextInt(256).toByte() }
        val randPngEnd = android.util.Base64.encodeToString(randBytes + endBytes, android.util.Base64.NO_WRAP)
        
        val payload = org.json.JSONObject().apply {
            put("3064", 1)
            put("39c8", "333.999.fp.risk")
            put("3c43", org.json.JSONObject().apply {
                put("adca", "Windows") // 与 User-Agent (Windows NT 10.0) 保持一致
                put("bfe9", randPngEnd.takeLast(50))
            })
        }.toString()
        
        buvidApi.activateBuvid(payload)
    }

    // [新增] 预加载缓存
    @Volatile private var preloadedHomeVideos: Result<List<VideoItem>>? = null
    @Volatile private var homePreloadDeferred: Deferred<Result<List<VideoItem>>>? = null
    @Volatile private var hasCompletedHomePreload = false
    
    // [新增] 检查首页数据是否就绪
    fun isHomeDataReady(): Boolean {
        return shouldReportHomeDataReadyForSplash(
            hasCompletedPreload = hasCompletedHomePreload,
            hasPreloadedData = preloadedHomeVideos != null
        )
    }

    // [新增] 预加载首页数据 (在 MainActivity onCreate 调用)
    fun preloadHomeData(scope: CoroutineScope = AppScope.ioScope) {
        val activePreloadTask = homePreloadDeferred?.takeIf { it.isActive } != null
        if (!shouldStartHomePreload(preloadedHomeVideos != null, activePreloadTask)) return
        hasCompletedHomePreload = false

        com.android.purebilibili.core.util.Logger.d("VideoRepo", "🚀 Starting home data preload...")

        homePreloadDeferred = scope.async {
            try {
                val feedApiType = NetworkModule.appContext
                    ?.let { SettingsManager.getFeedApiTypeSync(it) }
                    ?: SettingsManager.FeedApiType.WEB
                if (shouldPrimeBuvidForHomePreload(feedApiType)) {
                    // 移动端推荐流可能依赖 buvid 会话，保留预热。
                    ensureBuvid3FromSpi()
                } else {
                    com.android.purebilibili.core.util.Logger.d(
                        "VideoRepo",
                        "🚀 Skip buvid warmup for WEB home preload"
                    )
                }

                val result = getHomeVideosInternal(idx = 0)
                preloadedHomeVideos = result

                com.android.purebilibili.core.util.Logger.d("VideoRepo", "🚀 Home data preload finished. Success=${result.isSuccess}")
                result
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("VideoRepo", "🚀 Home data preload failed", e)
                Result.failure<List<VideoItem>>(e).also { preloadedHomeVideos = it }
            } finally {
                hasCompletedHomePreload = true
            }
        }
    }

    suspend fun getVideoInfoOnly(
        bvid: String,
        aid: Long = 0L,
        requestedCid: Long = 0L
    ): Result<ViewInfo> = withContext(Dispatchers.IO) {
        try {
            val lookup = resolveVideoInfoLookupInput(rawBvid = bvid, aid = aid)
                ?: throw Exception("无效的视频标识: bvid=$bvid, aid=$aid")
            val viewResp = if (lookup.bvid.isNotEmpty()) {
                api.getVideoInfo(lookup.bvid)
            } else {
                api.getVideoInfoByAid(lookup.aid)
            }
            val rawInfo = viewResp.data ?: throw Exception("视频详情为空: ${viewResp.code}")
            val cid = resolveRequestedVideoCid(
                requestCid = requestedCid,
                infoCid = rawInfo.cid,
                pages = rawInfo.pages
            )
            if (cid == 0L) throw Exception("CID 获取失败")
            val info = if (cid > 0L && cid != rawInfo.cid) {
                rawInfo.copy(cid = cid)
            } else {
                rawInfo
            }
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInitialPlayUrlData(
        bvid: String,
        cid: Long,
        targetQuality: Int,
        audioLang: String? = null
    ): PlayUrlData? = withContext(Dispatchers.IO) {
        if (bvid.isBlank() || cid <= 0L) return@withContext null

        val isAutoHighestQuality = targetQuality >= 127
        val isLogin = resolveVideoPlaybackAuthState(
            hasSessionCookie = hasPlaybackSessionCookie(),
            hasAccessToken = !playbackAccessToken().isNullOrEmpty()
        )
        val isVip = isPlaybackVip()
        val auto1080pEnabled = try {
            val context = NetworkModule.appContext
            context?.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
                ?.getBoolean("exp_auto_1080p", true) ?: true
        } catch (e: Exception) {
            true
        }
        val startQuality = resolveInitialStartQuality(
            targetQuality = targetQuality,
            isAutoHighestQuality = isAutoHighestQuality,
            isLogin = isLogin,
            isVip = isVip,
            auto1080pEnabled = auto1080pEnabled
        )

        if (!shouldSkipPlayUrlCache(isAutoHighestQuality, isVip, audioLang)) {
            val cachedPlayData = PlayUrlCache.get(
                bvid = bvid,
                cid = cid,
                requestedQuality = startQuality
            )
            val cachedDashIds = cachedPlayData?.dash?.video?.map { it.id }.orEmpty()
            if (
                cachedPlayData != null &&
                shouldAcceptCachedPlayUrlForAutoHighest(
                    isAutoHighestQuality = isAutoHighestQuality,
                    isVip = isVip,
                    cachedDashVideoIds = cachedDashIds
                )
            ) {
                return@withContext cachedPlayData
            }
        }

        val fetchResult = fetchPlayUrlRecursive(
            bvid = bvid,
            cid = cid,
            targetQn = startQuality,
            audioLang = audioLang,
            requestKind = PlayUrlRequestKind.INITIAL
        ) ?: return@withContext null

        val dashVideoIds = fetchResult.data.dash?.video?.map { it.id }?.distinct() ?: emptyList()
        if (shouldCachePlayUrlResult(
                source = fetchResult.source,
                audioLang = audioLang,
                requestedQuality = startQuality,
                returnedQuality = fetchResult.data.quality,
                dashVideoIds = dashVideoIds
            )
        ) {
            PlayUrlCache.put(
                bvid = bvid,
                cid = cid,
                data = fetchResult.data,
                quality = startQuality
            )
        }
        fetchResult.data
    }

    suspend fun getPortraitPlaybackDetails(
        bvid: String,
        aid: Long = 0,
        requestedCid: Long = 0,
        targetQuality: Int = PORTRAIT_PLAYBACK_TARGET_QUALITY,
        audioLang: String? = null
    ): Result<Pair<ViewInfo, PlayUrlData>> = withContext(Dispatchers.IO) {
        try {
            if (shouldUsePortraitParallelPlaybackBootstrap(bvid, requestedCid)) {
                coroutineScope {
                    val infoDeferred = async {
                        getVideoInfoOnly(
                            bvid = bvid,
                            aid = aid,
                            requestedCid = requestedCid
                        )
                    }
                    val playUrlDeferred = async {
                        getInitialPlayUrlData(
                            bvid = bvid,
                            cid = requestedCid,
                            targetQuality = targetQuality,
                            audioLang = audioLang
                        ) ?: throw Exception("无法获取播放地址")
                    }
                    infoDeferred.await().fold(
                        onSuccess = { info ->
                            Result.success(info to playUrlDeferred.await())
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                }
            } else {
                getVideoDetails(
                    bvid = bvid,
                    aid = aid,
                    requestedCid = requestedCid,
                    targetQuality = targetQuality,
                    audioLang = audioLang
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun preloadPortraitPlayUrl(
        bvid: String,
        cid: Long,
        aid: Long = 0L,
        targetQuality: Int = PORTRAIT_PLAYBACK_TARGET_QUALITY
    ): PlayUrlData? {
        if (bvid.isBlank()) return null
        return withContext(Dispatchers.IO) {
            if (cid <= 0L) {
                val resolved = getPortraitPlaybackDetails(
                    bvid = bvid,
                    aid = aid,
                    requestedCid = 0L,
                    targetQuality = targetQuality
                ).getOrNull()?.second
                if (resolved != null) {
                    com.android.purebilibili.core.util.Logger.d(
                        "VideoRepo",
                        "🚀 Portrait preload resolved cid and playurl: bvid=$bvid"
                    )
                }
                return@withContext resolved
            }
            val cachedPlayData = PlayUrlCache.get(
                bvid = bvid,
                cid = cid,
                requestedQuality = targetQuality
            )
            if (cachedPlayData != null) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    "🚀 Portrait preload skip (cached): bvid=$bvid"
                )
                return@withContext cachedPlayData
            }
            val playData = getInitialPlayUrlData(
                bvid = bvid,
                cid = cid,
                targetQuality = targetQuality
            )
            if (playData != null) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    "🚀 Portrait preloaded playurl: bvid=$bvid"
                )
            }
            playData
        }
    }

    private suspend fun awaitHomePreloadResult(): Result<List<VideoItem>>? {
        val deferred = homePreloadDeferred ?: return null
        return runCatching { deferred.await() }.getOrNull()
    }

    private fun consumePreloadedHomeVideos(): Result<List<VideoItem>>? {
        val cached = preloadedHomeVideos ?: return null
        preloadedHomeVideos = null
        homePreloadDeferred = null
        return cached
    }

    // 1. 首页推荐 (修改为优先使用预加载数据)
    suspend fun getHomeVideos(idx: Int = 0, allowPreload: Boolean = true): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        // 如果是首次加载 (idx=0) 且有预加载数据，直接使用
        if (idx == 0 && allowPreload) {
            val cached = consumePreloadedHomeVideos()
            if (cached != null) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", "✅ Using preloaded home data!")
                return@withContext cached
            }

            val hasActivePreloadTask = homePreloadDeferred?.isActive == true
            if (shouldReuseInFlightPreloadForHomeRequest(idx, hasActivePreloadTask, hasPreloadedData = false)) {
                val awaited = awaitHomePreloadResult()
                if (awaited != null) {
                    com.android.purebilibili.core.util.Logger.d(
                        "VideoRepo",
                        "✅ Reused in-flight home preload result"
                    )
                    consumePreloadedHomeVideos()
                    return@withContext awaited
                }
            }
        }
        
        getHomeVideosInternal(idx)
    }

    // [重构] 内部加载逻辑
    private suspend fun getHomeVideosInternal(idx: Int): Result<List<VideoItem>> {
        try {
            //  读取推荐流类型设置
            val context = com.android.purebilibili.core.network.NetworkModule.appContext
            val feedApiType = if (context != null) {
                com.android.purebilibili.core.store.SettingsManager.getFeedApiTypeSync(context)
            } else {
                com.android.purebilibili.core.store.SettingsManager.FeedApiType.WEB
            }
            val refreshCount = if (context != null) {
                SettingsManager.getHomeRefreshCountSync(context)
            } else {
                com.android.purebilibili.core.store.DEFAULT_HOME_REFRESH_COUNT
            }
            
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                " getHomeVideos: feedApiType=$feedApiType, idx=$idx, refreshCount=$refreshCount"
            )
            
            when (feedApiType) {
                com.android.purebilibili.core.store.SettingsManager.FeedApiType.MOBILE -> {
                    // 尝试使用移动端 API
                    val mobileResult = fetchMobileFeed(idx = idx, refreshCount = refreshCount)
                    if (mobileResult.isSuccess && mobileResult.getOrNull()?.isNotEmpty() == true) {
                        return mobileResult
                    } else {
                        // 移动端 API 失败，回退到 Web API
                        com.android.purebilibili.core.util.Logger.d("VideoRepo", " Mobile API failed, fallback to Web API")
                        return fetchWebFeed(idx = idx, refreshCount = refreshCount)
                    }
                }
                com.android.purebilibili.core.store.SettingsManager.FeedApiType.MERGED -> {
                    // 合并模式(参数对齐 PiliNara 原版):
                    // Web 半边用 fresh_type=4/feed_version=V8/brush=idx 等参数, 携带 cookie;
                    // App 半边用 android_hd 身份头取流(剥离 cookie, 已登录时带 access_key),
                    // 并行请求后交错合并、按视频去重
                    return coroutineScope {
                        val webDeferred = async { fetchMergedWebFeed(idx = idx, refreshCount = refreshCount) }
                        val mobileDeferred = async { fetchMergedMobileFeed(idx = idx) }
                        val webResult = webDeferred.await()
                        val mobileResult = mobileDeferred.await()

                        val webList = webResult.getOrNull().orEmpty()
                        val mobileList = mobileResult.getOrNull().orEmpty()

                        if (webList.isEmpty() && mobileList.isEmpty()) {
                            // 两者都失败：优先返回 web 的失败原因，其次移动端
                            val error = webResult.exceptionOrNull() ?: mobileResult.exceptionOrNull()
                                ?: Exception("获取合并推荐流失败")
                            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Merged feed both failed: ${error.message}")
                            Result.failure(error)
                        } else {
                            com.android.purebilibili.core.util.Logger.d(
                                "VideoRepo",
                                " Merged feed: web=${webList.size}, mobile=${mobileList.size}"
                            )
                            Result.success(com.android.purebilibili.feature.home.HomeFeedMergePolicy.mergeFeeds(web = webList, app = mobileList))
                        }
                    }
                }
                else -> return fetchWebFeed(idx = idx, refreshCount = refreshCount)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }
    
    //  Web 端推荐流 (WBI 签名)
    private suspend fun fetchWebFeed(idx: Int, refreshCount: Int): Result<List<VideoItem>> {
        try {
            val cachedKeys = WbiKeyManager.getWbiKeys().getOrNull()
            val navWbiImg = if (cachedKeys == null) api.getNavInfo().data?.wbi_img else null
            val resolvedKeys = resolveHomeFeedWbiKeys(
                cachedKeys = cachedKeys,
                navWbiImg = navWbiImg
            ) ?: throw Exception("无法获取 Key")
            val (imgKey, subKey) = resolvedKeys

            val params = mapOf(
                "ps" to refreshCount.toString(), "fresh_type" to "3", "fresh_idx" to idx.toString(),
                "feed_version" to System.currentTimeMillis().toString(), "y_num" to idx.toString()
            )
            val signedParams = WbiUtils.sign(params, imgKey, subKey)
            val feedResp = api.getRecommendParams(signedParams)
            
            //  [调试] 检查 API 是否返回 dimension 字段
            feedResp.data?.item?.take(3)?.forEachIndexed { index, item ->
                com.android.purebilibili.core.util.Logger.d("VideoRepo", 
                    " 视频[$index]: ${item.title?.take(15)}... dimension=${item.dimension} isVertical=${item.dimension?.isVertical}")
            }
            
            val list = feedResp.data?.item?.map { it.toVideoItem() }?.filter { it.bvid.isNotEmpty() } ?: emptyList()
            
            //  [调试] 检查转换后的 VideoItem
            val verticalCount = list.count { it.isVertical }
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Web推荐: total=${list.size}, vertical=$verticalCount")
            
            return Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }
    
    //  移动端推荐流 (appkey + sign 签名)
    private suspend fun fetchMobileFeed(idx: Int, refreshCount: Int): Result<List<VideoItem>> {
        try {
            val accessToken = TokenManager.accessTokenCache
            if (accessToken.isNullOrEmpty()) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " No access_token, fallback to Web API")
                return Result.failure(Exception("需要登录才能使用移动端推荐流"))
            }
            
            val params = mapOf(
                "idx" to idx.toString(),
                "pull" to if (idx == 0) "1" else "0",  // 1=刷新, 0=加载更多
                "column" to "4",  // 4列布局
                "flush" to "5",   // 刷新间隔
                "autoplay_card" to "11",
                "ps" to refreshCount.toString(),
                "access_key" to accessToken,
                "appkey" to AppSignUtils.TV_APP_KEY,
                "ts" to AppSignUtils.getTimestamp().toString(),
                "mobi_app" to "android",
                "device" to "android",
                "build" to "8130300"
            )
            
            val signedParams = AppSignUtils.signForTvLogin(params)
            
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Mobile feed request: idx=$idx")
            val feedResp = api.getMobileFeed(signedParams)
            
            if (feedResp.code != 0) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " Mobile feed error: code=${feedResp.code}, msg=${feedResp.message}")
                return Result.failure(Exception(feedResp.message))
            }
            
            val list = feedResp.data?.items
                ?.filter { it.goto == "av" }  // 只保留视频类型
                ?.map { it.toVideoItem() }
                ?.filter { it.bvid.isNotEmpty() }
                ?: emptyList()
            
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Mobile推荐: total=${list.size}")
            
            return Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Mobile feed exception: ${e.message}")
            return Result.failure(e)
        }
    }
    
    //  [合并模式] Web 端推荐流 - 参数对齐 PiliNara 原版合并模式(携带 cookie)
    //  (feed_version=V8 常量会话、fresh_type=4、brush=idx、version=1、homepage_ver=1)
    //  仅用于 FeedApiType.MERGED, 不影响 Web 单独模式
    private suspend fun fetchMergedWebFeed(idx: Int, refreshCount: Int): Result<List<VideoItem>> {
        try {
            val cachedKeys = WbiKeyManager.getWbiKeys().getOrNull()
            val navWbiImg = if (cachedKeys == null) api.getNavInfo().data?.wbi_img else null
            val resolvedKeys = resolveHomeFeedWbiKeys(
                cachedKeys = cachedKeys,
                navWbiImg = navWbiImg
            ) ?: throw Exception("无法获取 Key")
            val (imgKey, subKey) = resolvedKeys

            val params = mapOf(
                "version" to "1",
                "feed_version" to "V8",
                "homepage_ver" to "1",
                "ps" to refreshCount.toString(),
                "fresh_idx" to idx.toString(),
                "brush" to idx.toString(),
                "fresh_type" to "4"
            )
            val signedParams = WbiUtils.sign(params, imgKey, subKey)
            val feedResp = api.getRecommendParams(signedParams)

            val list = feedResp.data?.item?.map { it.toVideoItem() }?.filter { it.bvid.isNotEmpty() } ?: emptyList()

            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Merged Web推荐: total=${list.size}")

            return Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    //  [合并模式] App 端推荐流 - 参数对齐 PiliNara 原版合并模式
    //  该 app 端点由 CookieJar 剥离 cookie(见 MergedAppFeedCookiePolicy), 身份由 HD 身份头承担;
    //  已登录时补 access_key。与 App 单独模式(TV appkey + mobi_app=android)完全无关。
    private suspend fun fetchMergedMobileFeed(idx: Int): Result<List<VideoItem>> {
        try {
            // app 取流依赖 buvid 会话, 缺失时先通过 SPI 获取
            if (TokenManager.buvid3Cache.isNullOrEmpty()) {
                ensureBuvid3FromSpi()
            }
            val params = mutableMapOf(
                "build" to "2001100",
                "c_locale" to "zh_CN",
                "channel" to "master",
                "column" to "4",
                "device" to "pad",
                "device_name" to "android",
                "device_type" to "0",
                "disable_rcmd" to "0",
                "flush" to "5",
                "fnval" to "976",
                "fnver" to "0",
                "force_host" to "2",
                "fourk" to "1",
                "guidance" to "0",
                "https_url_req" to "0",
                "idx" to idx.toString(),
                "mobi_app" to "android_hd",
                "network" to "wifi",
                "platform" to "android",
                "player_net" to "1",
                "pull" to if (idx == 0) "true" else "false",  // 首页 true=刷新, 往后 false=加载更多
                "qn" to "32",
                "recsys_mode" to "0",
                "s_locale" to "zh_CN",
                "splash_id" to "",
                "statistics" to "{\"appId\":5,\"platform\":3,\"version\":\"2.0.1\",\"abtest\":\"\"}",
                "ts" to AppSignUtils.getTimestamp().toString(),
                "voice_balance" to "0"
            )
            // 已登录时补 access_key: 对齐 PiliNara 原版(其对 app 端点补 access_key 后再签名)
            TokenManager.accessTokenCache
                ?.takeIf { it.isNotBlank() }
                ?.let { params["access_key"] = it }

            // key/value percent-encode 后拼接签名,
            // 再用 encoded=true 端点原样发送, 保证签名与线上 query 完全一致
            val signedParams = AppSignUtils.signForAndroidHdLogin(params)
            val encodedParams = signedParams.mapValues { (_, value) -> AppSignUtils.percentEncode(value) }

            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Merged Mobile feed request: idx=$idx")
            val feedResp = api.getMobileFeedEncoded(encodedParams)

            if (feedResp.code != 0) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " Merged Mobile feed error: code=${feedResp.code}, msg=${feedResp.message}")
                return Result.failure(Exception(feedResp.message))
            }

            val list = feedResp.data?.items
                ?.filter { it.goto == "av" }  // 只保留视频类型
                ?.map { it.toVideoItem() }
                ?.filter { it.bvid.isNotEmpty() }
                ?: emptyList()

            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Merged Mobile推荐: total=${list.size}")

            return Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " Merged Mobile feed exception: ${e.message}")
            return Result.failure(e)
        }
    }
    
    //  [新增] 热门视频
    suspend fun getPopularVideos(page: Int = 1): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getPopularVideos(pn = page, ps = 30)
            val list = resp.data?.list?.map { it.toVideoItem() }?.filter { it.bvid.isNotEmpty() } ?: emptyList()
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getRankingVideos(rid: Int = 0, type: String = "all"): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val keys = WbiKeyManager.getWbiKeys().getOrElse { throw it }
            val signedParams = WbiUtils.sign(
                params = mapOf("rid" to rid.toString(), "type" to type),
                imgKey = keys.first,
                subKey = keys.second,
            )
            val resp = api.getRankingVideos(signedParams)
            if (resp.code != 0) {
                return@withContext Result.failure(Exception(resp.message.ifBlank { "排行榜加载失败(${resp.code})" }))
            }
            val list = resp.data?.list
                ?.map { it.toVideoItem() }
                ?.filter { it.bvid.isNotEmpty() }
                ?: emptyList()
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getPreciousVideos(): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getPopularPreciousVideos()
            if (resp.code != 0) {
                return@withContext Result.failure(Exception(resp.message.ifBlank { "入站必刷加载失败(${resp.code})" }))
            }
            val list = resp.data?.list
                ?.map { it.toVideoItem() }
                ?.filter { it.bvid.isNotEmpty() }
                ?: emptyList()
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getWeeklyMustWatchVideos(number: Int? = null): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val targetNumber = number ?: run {
                val listResp = api.getWeeklySeriesList()
                if (listResp.code != 0) {
                    return@withContext Result.failure(Exception(listResp.message.ifBlank { "每周必看列表加载失败(${listResp.code})" }))
                }
                val latest = listResp.data?.list
                    ?.map { it.number }
                    ?.maxOrNull()
                latest ?: 1
            }
            val resp = api.getWeeklySeriesVideos(number = targetNumber)
            if (resp.code != 0) {
                return@withContext Result.failure(Exception(resp.message.ifBlank { "每周必看加载失败(${resp.code})" }))
            }
            val list = resp.data?.list
                ?.map { it.toVideoItem() }
                ?.filter { it.bvid.isNotEmpty() }
                ?: emptyList()
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  [新增] 分区视频（按分类 ID 获取视频）
    suspend fun getRegionVideos(tid: Int, page: Int = 1): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getRegionVideos(rid = tid, pn = page, ps = 30)
            val list = resp.data?.archives
                ?.map { it.toVideoItem() }
                ?.filter { it.bvid.isNotEmpty() }
                ?: emptyList()
            if (shouldFallbackRegionLatestToRanking(
                    tid = tid,
                    page = page,
                    latestVideoCount = list.size,
                    latestResponseCode = resp.code
                )
            ) {
                if (tid == 202) {
                    val legacy = api.getLegacyRegionVideos(rid = tid, pn = page, ps = 30)
                    if (legacy.code == 0) {
                        return@withContext Result.success(
                            legacy.data?.archives
                                ?.map { it.toVideoItem() }
                                ?.filter { it.bvid.isNotEmpty() }
                                ?: emptyList()
                        )
                    }
                }
                // dynamic/region 只稳定支持子分区；一级分区用排行榜兜底，避免标签页空白。
                val rankingRid = resolveRegionRankingRid(tid)
                if (rankingRid != null) {
                    return@withContext getRankingVideos(rid = rankingRid)
                }
            }
            if (resp.code != 0) {
                return@withContext Result.failure(Exception(resp.message.ifBlank { "分区视频加载失败(${resp.code})" }))
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  [新增] 上报播放心跳（记录到历史记录）
    suspend fun reportPlayHeartbeat(
        bvid: String,
        cid: Long,
        playedTime: Long = 0,
        realPlayedTime: Long = playedTime,
        startTsSec: Long = System.currentTimeMillis() / 1000L,
        aid: Long = 0L,
        epid: Long = 0L,
        sid: Long = 0L,
        videoType: Int = 3,
        subType: Int? = null
    ) = withContext(Dispatchers.IO) {
        try {
            //  隐私无痕模式检查：如果启用则跳过上报
            val context = com.android.purebilibili.core.network.NetworkModule.appContext
            if (context != null && com.android.purebilibili.core.store.SettingsManager.isPrivacyModeEnabledSync(context)) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " Privacy mode enabled, skipping heartbeat report")
                return@withContext true  // 返回成功但不实际上报
            }

            val fields = buildPlaybackHeartbeatFields(
                bvid = bvid,
                aid = aid,
                cid = cid,
                epid = epid,
                sid = sid,
                mid = com.android.purebilibili.core.store.TokenManager.midCache,
                playedTimeSec = playedTime,
                realPlayedTimeSec = realPlayedTime,
                startTsSec = startTsSec,
                csrf = com.android.purebilibili.core.store.TokenManager.csrfCache.orEmpty(),
                videoType = videoType,
                subType = subType
            )
            
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                "🔴 Reporting heartbeat: bvid=$bvid, aid=$aid, cid=$cid, epid=$epid, sid=$sid, type=$videoType, " +
                    "playedTime=$playedTime, realPlayedTime=$realPlayedTime, startTs=$startTsSec"
            )
            val resp = api.reportHeartbeat(fields)
            com.android.purebilibili.core.util.Logger.d("VideoRepo", "🔴 Heartbeat response: code=${resp.code}, msg=${resp.message}")
            if (resp.code == 0) {
                com.android.purebilibili.core.refresh.HistoryRefreshBus.notifyChanged()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepo", " Heartbeat failed: ${e.message}")
            false
        }
    }
    

    suspend fun getNavInfo(): Result<NavData> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getNavInfo()
            if (resp.code == 0 && resp.data != null) {
                Result.success(resp.data)
            } else {
                if (resp.code == -101) {
                    Result.success(NavData(isLogin = false))
                } else {
                    Result.failure(Exception("错误码: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 按「播放会话」（优先播放账号，其次主账号）查询 nav，用于决定播放画质/会员状态。
     * 与 [getNavInfo]（始终主账号）不同，这里跟随 [NetworkModule.playbackApi] 的账号归属。
     */
    internal suspend fun getPlaybackNavInfo(): Result<NavData> = withContext(Dispatchers.IO) {
        try {
            val resp = NetworkModule.playbackApi().getNavInfo()
            if (resp.code == 0 && resp.data != null) {
                Result.success(resp.data)
            } else {
                if (resp.code == -101) {
                    Result.success(NavData(isLogin = false))
                } else {
                    Result.failure(Exception("错误码: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshVipStatusForPreferredQualityIfNeeded(
        isLoggedIn: Boolean,
        cachedIsVip: Boolean,
        storedQuality: Int,
        autoHighestEnabled: Boolean
    ): Boolean {
        if (
            !com.android.purebilibili.core.util.shouldRefreshVipStatusBeforeResolvingDefaultQuality(
                storedQuality = storedQuality,
                autoHighestEnabled = autoHighestEnabled,
                isLoggedIn = isLoggedIn,
                cachedIsVip = cachedIsVip
            )
        ) {
            return cachedIsVip
        }

        // 按播放会话刷新：有独立播放账号时查播放账号的 nav，且不污染主账号 VIP 缓存。
        return getPlaybackNavInfo()
            .getOrNull()
            ?.takeIf { it.isLogin }
            ?.let { navData ->
                val isVip = navData.vip.status == 1
                if (!isUsingDedicatedPlaybackAccount()) {
                    TokenManager.isVipCache = isVip
                }
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    " Refreshed VIP status before quality resolution: cached=$cachedIsVip, refreshed=$isVip, storedQuality=$storedQuality, autoHighest=$autoHighestEnabled, dedicatedPlayback=${isUsingDedicatedPlaybackAccount()}"
                )
                isVip
            }
            ?: cachedIsVip
    }

    suspend fun getCreatorCardStats(mid: Long): Result<CreatorCardStats> = withContext(Dispatchers.IO) {
        if (mid <= 0L) return@withContext Result.failure(IllegalArgumentException("Invalid mid"))
        creatorCardStatsCache[mid]?.let { return@withContext Result.success(it) }
        try {
            val response = api.getUserCard(mid = mid, photo = false)
            val data = response.data
            if (response.code == 0 && data != null) {
                val stats = CreatorCardStats(
                    followerCount = data.follower.coerceAtLeast(0),
                    videoCount = data.archive_count.coerceAtLeast(0),
                    vipStatus = data.card?.vip?.status ?: 0,
                    officialType = data.card?.Official?.type ?: -1,
                    pendantImage = data.card?.pendant?.image.orEmpty(),
                )
                creatorCardStatsCache[mid] = stats
                Result.success(stats)
            } else {
                Result.failure(Exception(response.message.ifBlank { "UP主信息加载失败(${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isVerticalVideo(bvid: String, aid: Long = 0L): Boolean = withContext(Dispatchers.IO) {
        val normalizedBvid = bvid.trim()
        if (normalizedBvid.isEmpty() && aid <= 0L) return@withContext false
        val cacheKey = normalizedBvid.ifEmpty { "av$aid" }
        verticalVideoCache[cacheKey]?.let { return@withContext it }
        try {
            val lookup = resolveVideoInfoLookupInput(rawBvid = normalizedBvid, aid = aid)
                ?: return@withContext false
            val viewResp = if (lookup.bvid.isNotEmpty()) {
                api.getVideoInfo(lookup.bvid)
            } else {
                api.getVideoInfoByAid(lookup.aid)
            }
            val isVertical = viewResp.data?.dimension?.isVertical == true
            verticalVideoCache[cacheKey] = isVertical
            isVertical
        } catch (_: Exception) {
            false
        }
    }

    // [修复] 添加 aid 参数支持，修复移动端推荐流视频播放失败问题
    suspend fun getVideoDetails(
        bvid: String,
        aid: Long = 0,
        requestedCid: Long = 0L,
        targetQuality: Int? = null,
        audioLang: String? = null
    ): Result<Pair<ViewInfo, PlayUrlData>> = withContext(Dispatchers.IO) {
        try {
            val lookup = resolveVideoInfoLookupInput(rawBvid = bvid, aid = aid)
                ?: throw Exception("无效的视频标识: bvid=$bvid, aid=$aid")
            val viewResp = if (lookup.bvid.isNotEmpty()) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " getVideoDetails: using bvid=${lookup.bvid}")
                api.getVideoInfo(lookup.bvid)
            } else {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " getVideoDetails: using aid=${lookup.aid}")
                api.getVideoInfoByAid(lookup.aid)
            }
            
            val rawInfo = viewResp.data ?: throw Exception("视频详情为空: ${viewResp.code}")
            val cid = resolveRequestedVideoCid(
                requestCid = requestedCid,
                infoCid = rawInfo.cid,
                pages = rawInfo.pages
            )
            val info = if (cid > 0L && cid != rawInfo.cid) {
                rawInfo.copy(cid = cid)
            } else {
                rawInfo
            }
            val cacheBvid = info.bvid.ifBlank { lookup.bvid.ifBlank { bvid } }
            
            //  [调试] 记录视频信息
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                " getVideoDetails: bvid=${info.bvid}, aid=${info.aid}, requestCid=$requestedCid, infoCid=${rawInfo.cid}, resolvedCid=$cid, title=${info.title.take(20)}..."
            )
            
            if (cid == 0L) throw Exception("CID 获取失败")

            // 🚀 [修复] 自动最高画质模式：跳过缓存，确保获取最新的高清流
            val isAutoHighestQuality = targetQuality != null && targetQuality >= 127

            //  [优化] 根据登录和大会员状态选择起始画质
            val isLogin = resolveVideoPlaybackAuthState(
                hasSessionCookie = hasPlaybackSessionCookie(),
                hasAccessToken = !playbackAccessToken().isNullOrEmpty()
            )
            val isVip = isPlaybackVip()
            
            //  [实验性功能] 读取 auto1080p 设置
            val auto1080pEnabled = try {
                val context = com.android.purebilibili.core.network.NetworkModule.appContext
                context?.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE)
                    ?.getBoolean("exp_auto_1080p", true) ?: true // 默认开启
            } catch (e: Exception) {
                true // 出错时默认开启
            }
            
            val startQuality = resolveInitialStartQuality(
                targetQuality = targetQuality,
                isAutoHighestQuality = isAutoHighestQuality,
                isLogin = isLogin,
                isVip = isVip,
                auto1080pEnabled = auto1080pEnabled
            )
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                buildStartQualityDecisionSummary(
                    bvid = cacheBvid.ifBlank { bvid },
                    cid = cid,
                    userSettingQuality = targetQuality,
                    startQuality = startQuality,
                    isAutoHighestQuality = isAutoHighestQuality,
                    isLoggedIn = isLogin,
                    isVip = isVip,
                    auto1080pEnabled = auto1080pEnabled,
                    audioLang = audioLang
                )
            )

            // [优化] 默认语言优先走缓存；VIP 自动最高仅在缓存已含高码率轨时复用，避免每次冷拉。
            if (!shouldSkipPlayUrlCache(isAutoHighestQuality, isVip, audioLang)) {
                val cachedPlayData = PlayUrlCache.get(
                    bvid = cacheBvid,
                    cid = cid,
                    requestedQuality = startQuality
                )
                val cachedDashIds = cachedPlayData?.dash?.video?.map { it.id }.orEmpty()
                if (
                    cachedPlayData != null &&
                    shouldAcceptCachedPlayUrlForAutoHighest(
                        isAutoHighestQuality = isAutoHighestQuality,
                        isVip = isVip,
                        cachedDashVideoIds = cachedDashIds
                    )
                ) {
                    com.android.purebilibili.core.util.Logger.d(
                        "VideoRepo",
                        " Using cached PlayUrlData for bvid=$cacheBvid, requestedQuality=$startQuality"
                    )
                    return@withContext Result.success(Pair(info, cachedPlayData))
                }
            } else {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    "🚀 Skip cache: bvid=$cacheBvid, isAutoHighest=$isAutoHighestQuality, audioLang=${audioLang ?: "default"}"
                )
            }

            val playUrlBvid = cacheBvid.ifBlank { bvid }
            val fetchResult = fetchPlayUrlRecursive(
                bvid = playUrlBvid,
                cid = cid,
                targetQn = startQuality,
                audioLang = audioLang,
                requestKind = PlayUrlRequestKind.INITIAL
            )
                ?: throw Exception("无法获取任何画质的播放地址")
            val playData = fetchResult.data

            //  支持 DASH 和 durl 两种格式
            val hasDash = !playData.dash?.video.isNullOrEmpty()
            val hasDurl = !playData.durl.isNullOrEmpty()
            val dashVideoIds = playData.dash?.video?.map { it.id }?.distinct()?.sortedDescending() ?: emptyList()
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                buildPlayUrlFetchSummary(
                    bvid = playUrlBvid,
                    cid = cid,
                    source = fetchResult.source,
                    requestedQuality = startQuality,
                    returnedQuality = playData.quality,
                    acceptQualities = playData.accept_quality,
                    dashVideoIds = dashVideoIds,
                    hasDurl = hasDurl,
                    isLoggedIn = isLogin,
                    isVip = isVip,
                    audioLang = audioLang
                )
            )
            if (!hasDash && !hasDurl) throw Exception("播放地址解析失败 (无 dash/durl)")

            //  [优化] 缓存结果 (仅默认语言缓存)
            if (shouldCachePlayUrlResult(
                    source = fetchResult.source,
                    audioLang = audioLang,
                    requestedQuality = startQuality,
                    returnedQuality = playData.quality,
                    dashVideoIds = dashVideoIds
                )
            ) {
                PlayUrlCache.put(
                    bvid = cacheBvid,
                    cid = cid,
                    data = playData,
                    quality = startQuality
                )
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    " Cached PlayUrlData for bvid=$cacheBvid, cid=$cid, requestedQuality=$startQuality, actualQuality=${playData.quality}"
                )
            } else {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    " Skip cache write: source=${fetchResult.source}, audioLang=${audioLang ?: "default"}"
                )
            }

            Result.success(Pair(info, playData))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // [新增] 获取 AI 视频总结
    suspend fun getAiSummary(bvid: String, cid: Long, upMid: Long): Result<AiSummaryResponse> = withContext(Dispatchers.IO) {
        try {
            ensureBuvid3FromSpi()
            logAiSummaryPreflight(
                bvid = bvid,
                cid = cid,
                upMid = upMid
            )
            val (imgKey, subKey) = getWbiKeys()
            val params = buildAiSummaryParams(
                bvid = bvid,
                cid = cid,
                upMid = upMid
            )
            val signedParams = WbiUtils.sign(params, imgKey, subKey)

            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                "🤖 AI Summary request: bvid=$bvid cid=$cid upMidPresent=${upMid > 0L}"
            )
            val response = api.getAiConclusion(signedParams)
            val diagnosis = diagnoseAiSummaryResponse(response)
            logAiSummaryResponse(
                bvid = bvid,
                cid = cid,
                diagnosis = diagnosis,
                hasModelResult = response.data?.modelResult != null,
                summaryLength = response.data?.modelResult?.summary?.length ?: 0,
                outlineCount = response.data?.modelResult?.outline?.size ?: 0
            )

            Result.success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val diagnosis = diagnoseAiSummaryFailure(e)
            if ((e as? retrofit2.HttpException)?.code() == 412 || e.message.orEmpty().contains("412")) {
                wbiKeysCache = null
                wbiKeysTimestamp = 0L
            }
            com.android.purebilibili.core.util.Logger.w(
                "VideoRepo",
                "🤖 AI Summary request failed: bvid=$bvid cid=$cid status=${diagnosis.status} reason=${diagnosis.reason} retryable=${diagnosis.shouldRetryRequest}"
            )
            Result.failure(e)
        }
    }

    private fun buildAiSummaryParams(
        bvid: String,
        cid: Long,
        upMid: Long
    ): Map<String, String> {
        val params = linkedMapOf(
            "bvid" to bvid,
            "cid" to cid.toString()
        )
        if (upMid > 0L) {
            params["up_mid"] = upMid.toString()
        }
        return params
    }

    private fun logAiSummaryPreflight(
        bvid: String,
        cid: Long,
        upMid: Long
    ) {
        val hasSess = !TokenManager.sessDataCache.isNullOrEmpty()
        val hasCsrf = !TokenManager.csrfCache.isNullOrEmpty()
        val hasBuvid = !TokenManager.buvid3Cache.isNullOrEmpty()
        val hasAccessToken = !TokenManager.accessTokenCache.isNullOrEmpty()
        com.android.purebilibili.core.util.Logger.i(
            "VideoRepo",
            "🤖 AI Summary preflight: bvid=$bvid cid=$cid upMidPresent=${upMid > 0L} hasSess=$hasSess hasCsrf=$hasCsrf hasBuvid=$hasBuvid hasAccessToken=$hasAccessToken buvidInitialized=$buvidInitialized"
        )
    }

    private fun logAiSummaryResponse(
        bvid: String,
        cid: Long,
        diagnosis: AiSummaryFetchDiagnosis,
        hasModelResult: Boolean,
        summaryLength: Int,
        outlineCount: Int
    ) {
        com.android.purebilibili.core.util.Logger.i(
            "VideoRepo",
            "🤖 AI Summary response: bvid=$bvid cid=$cid status=${diagnosis.status} reason=${diagnosis.reason} rootCode=${diagnosis.rootCode} dataCode=${diagnosis.dataCode} stid=${diagnosis.stid ?: ""} hasModelResult=$hasModelResult summaryLength=$summaryLength outlineCount=$outlineCount retryLater=${diagnosis.shouldRetryLater}"
        )
    }

    //  [优化] WBI Key 缓存
    private var wbiKeysCache: Pair<String, String>? = null
    private var wbiKeysTimestamp: Long = 0
    private const val WBI_CACHE_DURATION = 1000 * 60 * 30 //  优化：30分钟缓存
    
    //  412 错误冷却期（避免过快重试触发风控）
    private var last412Time: Long = 0
    private const val COOLDOWN_412_MS = 5000L // 412 后等待 5 秒

    private suspend fun getWbiKeys(): Pair<String, String> {
        val currentCheck = System.currentTimeMillis()
        val cached = wbiKeysCache
        if (cached != null && (currentCheck - wbiKeysTimestamp < WBI_CACHE_DURATION)) {
            return cached
        }

        //  [优化] 增加重试逻辑，最多 3 次尝试
        val maxRetries = 3
        var lastError: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                val navResp = api.getNavInfo()
                val wbiImg = navResp.data?.wbi_img
                
                if (wbiImg != null) {
                    val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
                    val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
                    
                    wbiKeysCache = Pair(imgKey, subKey)
                    wbiKeysTimestamp = System.currentTimeMillis()
                    com.android.purebilibili.core.util.Logger.d("VideoRepo", " WBI Keys obtained successfully (attempt $attempt)")
                    return wbiKeysCache!!
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                android.util.Log.w("VideoRepo", "getWbiKeys attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(200L * attempt) // 递增延迟
                }
            }
        }
        
        throw Exception("Wbi Keys Error after $maxRetries attempts: ${lastError?.message}")
    }

    suspend fun getPlayUrlData(bvid: String, cid: Long, qn: Int, audioLang: String? = null): PlayUrlData? = withContext(Dispatchers.IO) {
        fetchPlayUrlRecursive(
            bvid = bvid,
            cid = cid,
            targetQn = qn,
            audioLang = audioLang,
            requestKind = PlayUrlRequestKind.EXPLICIT
        )?.data
    }

    /**
     * Fetch a playable stream while moving between parts of the same video.
     *
     * Unlike a user-requested quality change, a part transition may fall back when the target
     * part does not expose the quality that was playing in the previous part.
     */
    suspend fun getPlayUrlDataForPlaybackTransition(
        bvid: String,
        cid: Long,
        qn: Int,
        audioLang: String? = null
    ): PlayUrlData? = withContext(Dispatchers.IO) {
        fetchPlayUrlRecursive(
            bvid = bvid,
            cid = cid,
            targetQn = qn,
            audioLang = audioLang,
            requestKind = PlayUrlRequestKind.PLAYBACK_TRANSITION
        )?.data
    }

    /**
     * Non-blocking exact premium quality lookup for HDR auto-upgrade.
     *
     * Only requests the exact [targetQn] via APP access_token. Returns null
     * immediately if [targetQn] is not a premium tier (125/126/127), or if
     * the APP API does not return the exact track in DASH.
     *
     * This method reuses the existing [fetchPlayUrlWithAccessToken] token
     * refresh, cooldown, and error handling — no new API logic.
     */
    suspend fun getExactPremiumPlayUrl(
        bvid: String,
        cid: Long,
        targetQn: Int,
        audioLang: String? = null
    ): PlayUrlData? = withContext(Dispatchers.IO) {
        if (targetQn !in 125..127) {
            return@withContext null
        }

        val hasToken = !playbackAccessToken().isNullOrEmpty()
        val cooldownUntil = appApiCooldownUntilMs
        val now = System.currentTimeMillis()
        val canUseApp = shouldCallAccessTokenApi(
            nowMs = now,
            cooldownUntilMs = cooldownUntil,
            hasAccessToken = hasToken
        )
        if (!canUseApp) {
            return@withContext null
        }

        val payload = fetchPlayUrlWithAccessToken(
            bvid = bvid,
            cid = cid,
            qn = targetQn,
            audioLang = audioLang
        ) ?: return@withContext null

        val dashVideos = payload.dash?.video.orEmpty()
        val dashIds = dashVideos.map { it.id }.distinct()
        if (!hasExactPlayableRequestedTrack(targetQn, dashVideos)) {
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                " getExactPremiumPlayUrl: APP returned ${payload.quality}, dash=$dashIds — missing exact target $targetQn"
            )
            return@withContext null
        }

        com.android.purebilibili.core.util.Logger.d(
            "VideoRepo",
            " getExactPremiumPlayUrl: success — exact $targetQn track found via APP"
        )
        payload
    }

    suspend fun getTvCastPlayData(
        aid: Long,
        cid: Long,
        qn: Int
    ): PlayUrlData? = withContext(Dispatchers.IO) {
        if (aid <= 0L || cid <= 0L) return@withContext null

        try {
            val params = buildTvCastPlayUrlParams(
                aid = aid,
                cid = cid,
                qn = qn,
                accessToken = playbackAccessToken()
            )
            val signedParams = AppSignUtils.signForTvLogin(params)
            val response = NetworkModule.playbackApi().getTvPlayUrl(signedParams)
            if (response.code != 0) {
                com.android.purebilibili.core.util.Logger.w(
                    "VideoRepo",
                    " tvPlayUrl failed: code=${response.code}, msg=${response.message}"
                )
                return@withContext null
            }
            response.data
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.w("VideoRepo", " tvPlayUrl exception: ${e.message}")
            null
        }
    }

    suspend fun getTvCastPlayUrl(
        aid: Long,
        cid: Long,
        qn: Int
    ): String? = extractTvCastPlayableUrl(getTvCastPlayData(aid, cid, qn))


    private data class PlayUrlFetchResult(
        val data: PlayUrlData,
        val source: PlayUrlSource
    )

    //  [v2 优化] 核心播放地址获取逻辑 - 根据登录状态区分策略
    private suspend fun fetchPlayUrlRecursive(
        bvid: String,
        cid: Long,
        targetQn: Int,
        audioLang: String? = null,
        requestKind: PlayUrlRequestKind
    ): PlayUrlFetchResult? {
        //  关键：确保有正确的 buvid3 (来自 Bilibili SPI API)
        ensureBuvid3FromSpi()

        val isLoggedIn = resolveVideoPlaybackAuthState(
            hasSessionCookie = hasPlaybackSessionCookie(),
            hasAccessToken = !playbackAccessToken().isNullOrEmpty()
        )
        com.android.purebilibili.core.util.Logger.d("VideoRepo", " fetchPlayUrlRecursive: bvid=$bvid, isLoggedIn=$isLoggedIn, targetQn=$targetQn, audioLang=$audioLang")

        if (isStrictPremiumQualityRequest(requestKind, targetQn)) {
            return fetchDashWithFallback(
                bvid = bvid,
                cid = cid,
                targetQn = targetQn,
                audioLang = audioLang,
                requestKind = requestKind
            )
        }

        return if (isLoggedIn) {
            // 已登录：保持 Web/WBI 主路径，失败时再走最小 fallback
            fetchDashWithFallback(
                bvid = bvid,
                cid = cid,
                targetQn = targetQn,
                audioLang = audioLang,
                requestKind = requestKind
            )
        } else {
            // 未登录：保持 Web/WBI 主路径，再回退到最小游客 fallback
            fetchGuestPlaybackWithFallback(
                bvid = bvid,
                cid = cid,
                targetQn = targetQn,
                requestKind = requestKind
            )
        }
    }

    private fun hasPlayableStreams(data: PlayUrlData?): Boolean {
        if (data == null) return false
        return !data.durl.isNullOrEmpty() || !data.dash?.video.isNullOrEmpty()
    }

    // 已登录用户：保持 BiliPai 对齐的单条 Web/WBI 主路径。
    private suspend fun fetchDashWithFallback(
        bvid: String,
        cid: Long,
        targetQn: Int,
        audioLang: String? = null,
        requestKind: PlayUrlRequestKind
    ): PlayUrlFetchResult? {
        val fallbackOrder = buildLoggedInPlaybackFallbackOrder()
        val directedTrafficMode = isDirectedTrafficModeActive()
        com.android.purebilibili.core.util.Logger.d(
            "VideoRepo",
            " [LoggedIn] DASH-first strategy, qn=$targetQn, directedTrafficMode=$directedTrafficMode"
        )
        
        val strictPremiumRequest = isStrictPremiumQualityRequest(requestKind, targetQn)
        // 手动高级画质只能请求精确档位；首次加载和普通档位仍保留快速降级。
        val dashQualities = if (strictPremiumRequest) {
            listOf(targetQn)
        } else {
            buildDashAttemptQualities(targetQn)
        }
        for ((qualityIndex, dashQn) in dashQualities.withIndex()) {
            val retryDelays = resolveDashRetryDelays(
                targetQn = dashQn,
                isPrimaryAttempt = qualityIndex == 0
            )
            val retryOnlyTransientEmptyResponse = shouldRetryOnlyTransientEmptyDashResponse(
                targetQn = dashQn,
                isPrimaryAttempt = qualityIndex == 0,
            )
            dashRetry@ for ((attempt, delayMs) in retryDelays.withIndex()) {
                if (delayMs > 0L) {
                    com.android.purebilibili.core.util.Logger.d(
                        "VideoRepo",
                        " DASH retry ${attempt + 1} for qn=$dashQn..."
                    )
                    kotlinx.coroutines.delay(delayMs)
                }

                try {
                    val data = fetchPlayUrlWithWbiInternal(bvid, cid, dashQn, audioLang)
                    if (hasPlayableStreams(data)) {
                        val payload = data ?: continue
                        val dashVideoIds = payload.dash?.video.orEmpty()
                            .filter { it.getValidUrl().isNotEmpty() }
                            .map { it.id }
                            .distinct()
                        val shouldRetryTrackRecovery = shouldRetryDashTrackRecovery(
                            targetQn = dashQn,
                            returnedQuality = payload.quality,
                            acceptQualities = payload.accept_quality,
                            dashVideoIds = dashVideoIds
                        )
                        if (shouldRetryTrackRecovery && attempt < retryDelays.lastIndex) {
                            com.android.purebilibili.core.util.Logger.d(
                                "VideoRepo",
                                " [LoggedIn] DASH track recovery retry: requestedQn=$dashQn, returnedQuality=${payload.quality}, accept=${payload.accept_quality}, dashIds=$dashVideoIds"
                            )
                            continue
                        }
                        if (payload.quality < dashQn || dashQn !in dashVideoIds) {
                            com.android.purebilibili.core.util.Logger.d(
                                "VideoRepo",
                                " [LoggedIn] DASH returned downgraded playable result: requestedQn=$dashQn, quality=${payload.quality}, dashIds=$dashVideoIds; defer actual selection to playback layer"
                            )
                        }
                        if (!shouldAcceptAppApiResultForTargetQuality(
                                requestKind = requestKind,
                                targetQn = dashQn,
                                returnedQuality = payload.quality,
                                dashVideoIds = dashVideoIds
                            )
                        ) {
                            com.android.purebilibili.core.util.Logger.d(
                                "VideoRepo",
                                " [LoggedIn] Reject downgraded result for explicit quality request: requestedQn=$dashQn, quality=${payload.quality}, dashIds=$dashVideoIds"
                            )
                            if (retryOnlyTransientEmptyResponse) {
                                break@dashRetry
                            }
                            continue@dashRetry
                        }
                        com.android.purebilibili.core.util.Logger.d(
                            "VideoRepo",
                            " [LoggedIn] DASH success: quality=${payload.quality}, requestedQn=$dashQn"
                        )
                        return PlayUrlFetchResult(payload, PlayUrlSource.DASH)
                    }
                    android.util.Log.w("VideoRepo", " DASH qn=$dashQn attempt=${attempt + 1}: data is null or empty")
                    if (attempt < retryDelays.lastIndex) {
                        wbiKeysCache = null
                        wbiKeysTimestamp = 0L
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("VideoRepo", "DASH qn=$dashQn attempt ${attempt + 1} failed: ${e.message}")
                    if (e.message?.contains("412") == true) {
                        last412Time = System.currentTimeMillis()
                        if (attempt < retryDelays.lastIndex) {
                            wbiKeysCache = null
                            wbiKeysTimestamp = 0L
                        }
                    }
                    if (retryOnlyTransientEmptyResponse) {
                        break@dashRetry
                    }
                }
            }
        }

        if (PlayUrlSource.APP in fallbackOrder) {
            val canUseAppFallback = shouldCallAccessTokenApi(
                nowMs = System.currentTimeMillis(),
                cooldownUntilMs = appApiCooldownUntilMs,
                hasAccessToken = !playbackAccessToken().isNullOrEmpty()
            )
            if (canUseAppFallback) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    " [LoggedIn] WBI chain exhausted, trying APP access_token fallback..."
                )
                for (appQn in dashQualities) {
                    val appData = fetchPlayUrlWithAccessToken(bvid, cid, appQn, audioLang = audioLang)
                    if (hasPlayableStreams(appData)) {
                        val payload = appData ?: continue
                        val appDashIds = payload.dash?.video.orEmpty()
                            .filter { it.getValidUrl().isNotEmpty() }
                            .map { it.id }
                            .distinct()
                        if (!shouldAcceptAppApiResultForTargetQuality(
                                requestKind = requestKind,
                                targetQn = appQn,
                                returnedQuality = payload.quality,
                                dashVideoIds = appDashIds
                            )
                        ) {
                            com.android.purebilibili.core.util.Logger.w(
                                "VideoRepo",
                                " [LoggedIn] APP fallback rejected downgraded qn=$appQn result: quality=${payload.quality}, dashIds=$appDashIds"
                            )
                            continue
                        }
                        com.android.purebilibili.core.util.Logger.d(
                            "VideoRepo",
                            " [LoggedIn] APP fallback success: quality=${payload.quality}, requestedQn=$appQn"
                        )
                        return PlayUrlFetchResult(payload, PlayUrlSource.APP)
                    }
                }
            } else {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoRepo",
                    " [LoggedIn] Skip APP fallback: no access token or cooldown active"
                )
            }
        }

        if (strictPremiumRequest) {
            return null
        }

        if (PlayUrlSource.LEGACY in fallbackOrder) {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " [LoggedIn] DASH failed, trying Legacy API...")
            try {
                val legacyResult = NetworkModule.playbackApi().getPlayUrlLegacy(bvid = bvid, cid = cid, qn = 80)
                if (legacyResult.code == 0 && legacyResult.data != null) {
                    val data = legacyResult.data
                    if (hasPlayableStreams(data)) {
                        com.android.purebilibili.core.util.Logger.d("VideoRepo", " [LoggedIn] Legacy API success: quality=${data.quality}")
                        return PlayUrlFetchResult(data, PlayUrlSource.LEGACY)
                    }
                } else {
                    android.util.Log.w("VideoRepo", "Legacy API returned code=${legacyResult.code}, msg=${legacyResult.message}")
                }
            } catch (e: Exception) {
                android.util.Log.w("VideoRepo", "[LoggedIn] Legacy API failed: ${e.message}")
            }
        }

        if (PlayUrlSource.GUEST in fallbackOrder) {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " [LoggedIn] All auth methods failed! Trying GUEST fallback (no auth)...")
            val guestResult = fetchAsGuestFallback(bvid, cid)
            if (guestResult != null) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " [LoggedIn->Guest] Guest fallback success: quality=${guestResult.quality}")
                return PlayUrlFetchResult(guestResult, PlayUrlSource.GUEST)
            }
        }

        android.util.Log.e("VideoRepo", " [LoggedIn] All attempts failed for bvid=$bvid")
        return null
    }

    /**
     * [新增] 获取预览视频地址 (简单 MP4 URL)
     * 用于首页长按预览播放，优先尝试获取低画质 MP4
     */
    suspend fun getPreviewVideoUrl(bvid: String, cid: Long): String? {
        // 复用 fetchAsGuestFallback 逻辑获取简单 MP4
        val data = fetchAsGuestFallback(bvid, cid)
        // 返回第一个 durl 的 url
        return data?.durl?.firstOrNull()?.url
    }
    
    //  [新增] 以游客身份获取视频（忽略登录凭证）
    //  [修复] 使用 guestApi 确保不携带 SESSDATA/bili_jct
    private suspend fun fetchAsGuestFallback(bvid: String, cid: Long): PlayUrlData? {
        try {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " fetchAsGuestFallback: bvid=$bvid, cid=$cid (using guestApi)")
            
            // ✅ 使用 guestApi - 不携带登录凭证
            val guestApi = NetworkModule.guestApi

            for (guestQn in buildGuestFallbackQualities()) {
                val legacyResult = guestApi.getPlayUrlLegacy(
                    bvid = bvid,
                    cid = cid,
                    qn = guestQn,
                    fnval = 1, // MP4 格式
                    platform = "html5", // HTML5 平台
                    highQuality = if (guestQn >= 64) 1 else 0
                )

                if (legacyResult.code == 0 && legacyResult.data != null) {
                    val data = legacyResult.data
                    if (!data.durl.isNullOrEmpty()) {
                        com.android.purebilibili.core.util.Logger.d(
                            "VideoRepo",
                            " Guest fallback (Legacy ${guestQn}p) success: actual=${data.quality}"
                        )
                        return data
                    }
                } else {
                    com.android.purebilibili.core.util.Logger.d(
                        "VideoRepo",
                        " Guest fallback ${guestQn}p failed: code=${legacyResult.code}"
                    )
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.w("VideoRepo", "Guest fallback failed: ${e.message}")
        }
        
        return null
    }
    
    // 未登录用户：保持 BiliPai 对齐的单条 Web/WBI 主路径。
    private suspend fun fetchGuestPlaybackWithFallback(
        bvid: String,
        cid: Long,
        targetQn: Int,
        requestKind: PlayUrlRequestKind
    ): PlayUrlFetchResult? {
        val fallbackOrder = buildGuestPlaybackFallbackOrder()
        com.android.purebilibili.core.util.Logger.d("VideoRepo", " [Guest] WBI-first strategy")

        for (source in fallbackOrder) {
            when (source) {
                PlayUrlSource.DASH -> {
                    try {
                        val dashData = fetchPlayUrlWithWbiInternal(bvid, cid, targetQn, audioLang = null)
                        if (dashData != null && (!dashData.durl.isNullOrEmpty() || !dashData.dash?.video.isNullOrEmpty())) {
                            val dashIds = dashData.dash?.video?.map { it.id }?.distinct() ?: emptyList()
                            if (!shouldAcceptAppApiResultForTargetQuality(
                                    requestKind = requestKind,
                                    targetQn = targetQn,
                                    returnedQuality = dashData.quality,
                                    dashVideoIds = dashIds
                                )
                            ) {
                                com.android.purebilibili.core.util.Logger.d(
                                    "VideoRepo",
                                    " [Guest] Reject downgraded result for explicit quality request: requestedQn=$targetQn, quality=${dashData.quality}, dashIds=$dashIds"
                                )
                                continue
                            }
                            com.android.purebilibili.core.util.Logger.d("VideoRepo", " [Guest] DASH success: quality=${dashData.quality}")
                            return PlayUrlFetchResult(dashData, PlayUrlSource.DASH)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("VideoRepo", "[Guest] DASH failed: ${e.message}")
                    }
                }
                PlayUrlSource.LEGACY -> {
                    try {
                        com.android.purebilibili.core.util.Logger.d("VideoRepo", " [Guest] DASH failed, trying legacy playurl API...")
                        val legacyResult = api.getPlayUrlLegacy(bvid = bvid, cid = cid, qn = 80)
                        if (legacyResult.code == 0 && legacyResult.data != null) {
                            val data = legacyResult.data
                            if (!data.durl.isNullOrEmpty() || !data.dash?.video.isNullOrEmpty()) {
                                val dashIds = data.dash?.video?.map { it.id }?.distinct() ?: emptyList()
                                if (!shouldAcceptAppApiResultForTargetQuality(
                                        requestKind = requestKind,
                                        targetQn = targetQn,
                                        returnedQuality = data.quality,
                                        dashVideoIds = dashIds
                                    )
                                ) {
                                    com.android.purebilibili.core.util.Logger.d(
                                        "VideoRepo",
                                        " [Guest] Reject downgraded legacy result for explicit quality request: requestedQn=$targetQn, quality=${data.quality}, dashIds=$dashIds"
                                    )
                                    continue
                                }
                                com.android.purebilibili.core.util.Logger.d("VideoRepo", " [Guest] Legacy API success: quality=${data.quality}")
                                return PlayUrlFetchResult(data, PlayUrlSource.LEGACY)
                            }
                        } else {
                            android.util.Log.w("VideoRepo", "Legacy API returned code=${legacyResult.code}, msg=${legacyResult.message}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("VideoRepo", "[Guest] Legacy API failed: ${e.message}")
                    }
                }
                else -> Unit
            }
        }

        android.util.Log.e("VideoRepo", " [Guest] All attempts failed for bvid=$bvid")
        return null
    }

    //  内部方法：单次请求播放地址 (使用 fnval=4048 获取全部 DASH 流)
    private suspend fun fetchPlayUrlWithWbiInternal(bvid: String, cid: Long, qn: Int, audioLang: String? = null): PlayUrlData? {
        com.android.purebilibili.core.util.Logger.d("VideoRepo", "fetchPlayUrlWithWbiInternal: bvid=$bvid, cid=$cid, qn=$qn, audioLang=$audioLang")
        
        //  使用缓存的 Keys
        val (imgKey, subKey) = getWbiKeys()
        val isLoggedIn = resolveVideoPlaybackAuthState(
            hasSessionCookie = hasPlaybackSessionCookie(),
            hasAccessToken = !playbackAccessToken().isNullOrEmpty()
        )
        val auto1080pEnabled = NetworkModule.appContext?.let { context ->
            runCatching { SettingsManager.getAuto1080p(context).first() }.getOrDefault(true)
        } ?: true
        
        val params = buildPlayUrlWbiBaseParams(
            bvid = bvid,
            cid = cid,
            qn = qn,
            audioLang = audioLang,
            tryLook = shouldRequestPlayUrlTryLook(
                isLoggedIn = isLoggedIn,
                auto1080pEnabled = auto1080pEnabled
            )
        )

        val directedOverrides = buildDirectedTrafficWbiOverrides(
            directedTrafficEnabled = NetworkModule.appContext?.let {
                SettingsManager.getBiliDirectedTrafficEnabledSync(it)
            } ?: false,
            isOnMobileData = NetworkModule.appContext?.let {
                NetworkUtils.isMobileData(it)
            } ?: false
        )
        if (directedOverrides.isNotEmpty()) {
            params.putAll(directedOverrides)
            com.android.purebilibili.core.util.Logger.d(
                "VideoRepo",
                " Applied directed traffic WBI overrides: $directedOverrides"
            )
        }
        
        val signedParams = WbiUtils.sign(params, imgKey, subKey)
        val response = NetworkModule.playbackApi().getPlayUrl(signedParams)
        
        com.android.purebilibili.core.util.Logger.d("VideoRepo", " PlayUrl response: code=${response.code}, requestedQn=$qn, returnedQuality=${response.data?.quality}")
        com.android.purebilibili.core.util.Logger.d("VideoRepo", " accept_quality=${response.data?.accept_quality}, accept_description=${response.data?.accept_description}")
        //  [调试] 输出 DASH 视频流 ID 列表
        val dashIds = response.data?.dash?.video?.map { it.id }?.distinct()?.sortedDescending()
        com.android.purebilibili.core.util.Logger.d("VideoRepo", " DASH video IDs: $dashIds")
        
        if (response.code == 0) {
            val payload = response.data
            if (hasPlayableStreams(payload)) {
                return payload
            }
            com.android.purebilibili.core.util.Logger.w(
                "VideoRepo",
                " PlayUrl success but empty payload: requestedQn=$qn, returnedQuality=${payload?.quality}, dashIds=$dashIds"
            )
            return null
        }
        
        //  [优化] API 返回错误码分类处理，提供更明确的错误信息
        val errorMessage = classifyPlayUrlError(response.code, response.message)
        android.util.Log.e("VideoRepo", " PlayUrl API error: code=${response.code}, message=${response.message}, classified=$errorMessage")
        // 对于不可重试的错误，抛出明确异常
        if (response.code in listOf(-404, -403, -10403, -62002)) {
            throw Exception(errorMessage)
        }
        return null
    }
    
    //  [New] Context storage for Token Refresh
    private var applicationContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        applicationContext = context.applicationContext
        AppScope.ioScope.launch {
            val feedApiType = SettingsManager.getFeedApiTypeSync(context)
            // WEB 推荐流不依赖 buvid 预热；冷启动时跳过 SPI，把带宽留给首页 feed。
            if (shouldPrimeBuvidForHomePreload(feedApiType)) {
                runCatching { ensureBuvid3FromSpi() }
            }
            // 使用共享 WbiKeyManager（磁盘恢复已在启动任务中完成），避免再走私有 nav 链路。
            runCatching { WbiKeyManager.getWbiKeys() }
        }
    }

    //  [New] Use access_token to get high quality stream (4K/HDR/1080P60)
    private suspend fun fetchPlayUrlWithAccessToken(bvid: String, cid: Long, qn: Int, allowRetry: Boolean = true, audioLang: String? = null): PlayUrlData? {
        val accessToken = playbackAccessToken()
        if (accessToken.isNullOrEmpty()) {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " No access_token available, fallback to Web API")
            return null
        }
        
        com.android.purebilibili.core.util.Logger.d("VideoRepo", " fetchPlayUrlWithAccessToken: bvid=$bvid, qn=$qn, retry=$allowRetry")
        
        val tokenPlatform = playbackAccessTokenPlatform()
        val usesAndroidToken = tokenPlatform == com.android.purebilibili.core.store.TokenManager.ACCESS_TOKEN_PLATFORM_ANDROID
        val params = mapOf(
            "bvid" to bvid,
            "cid" to cid.toString(),
            "qn" to qn.toString(),
            // 4048 (Web DASH formats) + 16384 (APP-only HDR Vivid, qn=129).
            "fnval" to "20432",
            "fnver" to "0",
            "fourk" to "1",
            "access_key" to accessToken,
            "appkey" to if (usesAndroidToken) AppSignUtils.ANDROID_APP_KEY else AppSignUtils.TV_APP_KEY,
            "ts" to AppSignUtils.getTimestamp().toString(),
            "platform" to "android",
            "mobi_app" to if (usesAndroidToken) "android" else "android_tv_yst",
            "device" to "android"
        ).toMutableMap()
        
        if (!audioLang.isNullOrEmpty()) {
           params["cur_language"] = audioLang
           params["lang"] = audioLang
        }
        
        val signedParams = if (usesAndroidToken) {
            AppSignUtils.signForAndroidApi(params)
        } else {
            AppSignUtils.signForTvLogin(params)
        }
        
        try {
            val response = NetworkModule.playbackApi().getPlayUrlApp(signedParams)
            
            // Check for -101 (Invalid Access Key)
            if (response.code == -101 && allowRetry && applicationContext != null && playbackAccount() == null) {
                com.android.purebilibili.core.util.Logger.w("VideoRepo", " Access token invalid (-101), trying to refresh...")
                val success = com.android.purebilibili.core.network.TokenRefreshHelper.refresh(applicationContext!!)
                if (success) {
                    com.android.purebilibili.core.util.Logger.i("VideoRepo", " Token refreshed successfully, retrying request...")
                    return fetchPlayUrlWithAccessToken(bvid, cid, qn, false, audioLang)
                } else {
                    com.android.purebilibili.core.util.Logger.e("VideoRepo", " Token refresh failed, aborting retry.")
                }
            }
            
            val dashIds = response.data?.dash?.video?.map { it.id }?.distinct()?.sortedDescending()
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " APP PlayUrl response: code=${response.code}, qn=$qn, dashIds=$dashIds")
            
            if (response.code == 0 && response.data != null) {
                val payload = response.data
                if (hasPlayableStreams(payload)) {
                    appApiCooldownUntilMs = 0L
                    com.android.purebilibili.core.util.Logger.d("VideoRepo", " APP API success: returned quality=${payload.quality}, available: $dashIds")
                    return payload
                }
                com.android.purebilibili.core.util.Logger.w(
                    "VideoRepo",
                    " APP API success but empty payload: qn=$qn, quality=${payload.quality}"
                )
            } else {
                if (response.code == -351) {
                    appApiCooldownUntilMs = System.currentTimeMillis() + APP_API_COOLDOWN_MS
                    com.android.purebilibili.core.util.Logger.w(
                        "VideoRepo",
                        " APP API hit anti-risk (-351), cooldown ${APP_API_COOLDOWN_MS}ms"
                    )
                }
                com.android.purebilibili.core.util.Logger.d("VideoRepo", " APP API error: code=${response.code}, msg=${response.message}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", " APP API exception: ${e.message}")
        }
        
        return null
    }

    /**
     * 获取视频预览图数据 (Videoshot API)
     * 
     * 用于进度条拖动时显示视频缩略图预览
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     * @return VideoshotData 或 null（如果获取失败）
     */
    suspend fun getVideoshot(bvid: String, cid: Long): VideoshotData? = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.util.Logger.d("VideoRepo", "🖼️ getVideoshot: bvid=$bvid, cid=$cid")
            val response = api.getVideoshot(bvid = bvid, cid = cid)
            if (response.code == 0 && response.data != null && response.data.isValid) {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", "🖼️ Videoshot success: ${response.data.image.size} images, ${response.data.index.size} frames")
                response.data
            } else {
                com.android.purebilibili.core.util.Logger.d("VideoRepo", "🖼️ Videoshot failed: code=${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("VideoRepo", "🖼️ Videoshot exception: ${e.message}")
            null
        }
    }

    // [修复] 获取播放器信息 (BGM/ViewPoints/Etc) — WBI 签名
    suspend fun getPlayerInfo(bvid: String, cid: Long): Result<PlayerInfoData> = withContext(Dispatchers.IO) {
        try {
            val (imgKey, subKey) = getWbiKeys()
            val params = mapOf(
                "bvid" to bvid,
                "cid" to cid.toString()
            )
            val signedParams = WbiUtils.sign(params, imgKey, subKey)
            val response = api.getPlayerInfo(signedParams)
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("PlayerInfo error: ${response.code}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPbpProgressData(
        bvid: String,
        cid: Long,
        aid: Long = 0L
    ): Result<PbpProgressData> = withContext(Dispatchers.IO) {
        try {
            if (cid <= 0L) {
                return@withContext Result.failure(
                    IllegalArgumentException("PBP cid invalid: $cid")
                )
            }
            val body = api.getPbpData(
                cid = cid,
                bvid = bvid.takeIf { it.isNotBlank() },
                aid = aid.takeIf { it > 0L }
            )
            Result.success(parsePbpProgressData(body.string()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubtitleCues(
        subtitleUrl: String,
        bvid: String,
        cid: Long,
        subtitleId: Long = 0L,
        subtitleIdStr: String = "",
        subtitleLan: String = ""
    ): Result<List<SubtitleCue>> = withContext(Dispatchers.IO) {
        try {
            if (bvid.isBlank() || cid <= 0L) {
                return@withContext Result.failure(
                    IllegalArgumentException("字幕归属视频信息缺失: bvid=$bvid cid=$cid")
                )
            }
            val normalizedUrl = normalizeBilibiliSubtitleUrl(subtitleUrl)
            if (normalizedUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("字幕 URL 为空"))
            }

            val cacheKey = buildSubtitleCueCacheKey(
                bvid = bvid,
                cid = cid,
                subtitleId = subtitleId,
                subtitleIdStr = subtitleIdStr,
                subtitleLan = subtitleLan,
                normalizedSubtitleUrl = normalizedUrl
            )
            subtitleCueCache[cacheKey]?.let { cached ->
                return@withContext Result.success(cached)
            }

            val request = Request.Builder()
                .url(normalizedUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .header("Referer", "https://www.bilibili.com")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build()

            val response = NetworkModule.okHttpClient.newCall(request).execute()
            response.use { call ->
                if (!call.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("字幕请求失败: HTTP ${call.code}")
                    )
                }
                val rawJson = call.body.string()
                val cues = parseBiliSubtitleBody(rawJson)
                if (subtitleCueCache.size >= SUBTITLE_CUE_CACHE_MAX_ENTRIES) {
                    subtitleCueCache.clear()
                }
                subtitleCueCache[cacheKey] = cues
                Result.success(cues)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInteractEdgeInfo(
        bvid: String,
        graphVersion: Long,
        edgeId: Long? = null
    ): Result<InteractEdgeInfoData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getInteractEdgeInfo(bvid = bvid, graphVersion = graphVersion, edgeId = edgeId)
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "互动分支信息加载失败(${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRelatedVideos(bvid: String): List<RelatedVideo> = withContext(Dispatchers.IO) {
        try { api.getRelatedVideos(bvid).data ?: emptyList() } catch (e: Exception) { emptyList() }
    }


    //  [新增] API 错误码分类，提供用户友好的错误提示
    private fun classifyPlayUrlError(code: Int, message: String?): String {
        return when (code) {
            -404 -> "视频不存在或已被删除"
            -403 -> "视频暂不可用"
            -10403 -> {
                when {
                    message?.contains("地区") == true -> "该视频在当前地区不可用"
                    message?.contains("会员") == true || message?.contains("vip") == true -> "需要大会员才能观看"
                    else -> "视频需要特殊权限才能观看"
                }
            }
            -62002 -> "视频已设为私密"
            -62004 -> "视频正在审核中"
            -62012 -> "视频已下架"
            -400 -> "请求参数错误"
            -101 -> "未登录，请先登录"
            -352 -> "请求频率过高，请稍后再试"
            else -> "获取播放地址失败 (错误码: $code)"
        }
    }
}
