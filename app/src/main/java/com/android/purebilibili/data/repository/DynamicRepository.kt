// 文件路径: data/repository/DynamicRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.OPUS_DETAIL_FEATURES
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.DynamicFeedResponse
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.feature.article.shouldFetchArticleFallbackForOpus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 *  动态数据仓库
 * 
 * 负责从 B站 API 获取动态 Feed 数据
 *
 * 分页语义对齐 bilibili-API-collect `docs/dynamic/all.md`：
 * - `offset`：翻页偏移，等于末条动态 id
 * - `update_baseline`：更新基线，等于首条动态 id；获取新动态时传入
 * - `update_num`：本次在更新基线以上的新动态条数
 */
object DynamicRepository {
    private val feedPagination = DynamicFeedPaginationRegistry()
    private val userFeedPagination = DynamicUserPaginationRegistry()
    private val detailSeeds = object : LinkedHashMap<String, DynamicItem>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DynamicItem>?): Boolean {
            return size > 32
        }
    }
    private val detailSeedsLock = Any()

    fun rememberDynamicDetailSeed(item: DynamicItem) {
        val id = item.id_str.trim()
        if (id.isEmpty()) return
        synchronized(detailSeedsLock) {
            detailSeeds[id] = item
        }
        item.orig?.let(::rememberDynamicDetailSeed)
    }

    fun peekDynamicDetailSeed(dynamicId: String): DynamicItem? {
        val id = dynamicId.trim()
        if (id.isEmpty()) return null
        return synchronized(detailSeedsLock) {
            detailSeeds[id]
        }
    }
    
    /**
     * 获取动态列表
     * @param refresh 是否刷新 (重置分页)
     * @param incrementalRefresh 是否保留现有时间线，仅拉取更新基线之后的内容
     */
    suspend fun getDynamicFeed(
        refresh: Boolean = false,
        scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN,
        type: String = "all",
        incrementalRefresh: Boolean = false
    ): Result<DynamicFeedFetchResult> = withContext(Dispatchers.IO) {
        try {
            val paginationBeforeRefresh = feedPagination.snapshot(scope, type)
            val useIncrementalRefresh = shouldUseDynamicIncrementalRefresh(
                refresh = refresh,
                incrementalRefreshEnabled = incrementalRefresh,
                updateBaseline = paginationBeforeRefresh.updateBaseline
            )
            if (refresh && !useIncrementalRefresh) {
                feedPagination.reset(scope, type)
            }
            val paginationForPageUpdate = if (refresh && !useIncrementalRefresh) {
                DynamicPaginationState()
            } else {
                paginationBeforeRefresh
            }
            if (!feedPagination.hasMore(scope, type) && !refresh) {
                return@withContext Result.success(
                    DynamicFeedFetchResult(
                        items = emptyList(),
                        updateNum = 0,
                        usedUpdateBaseline = false,
                        nextOffset = feedPagination.offset(scope, type),
                        hasMore = false
                    )
                )
            }

            val visibleItems = mutableListOf<DynamicItem>()
            var pagesFetched = 0
            var fetchedItemCount = 0
            var reportedUpdateNum = 0
            var resolvedUpdateBaseline = paginationForPageUpdate.updateBaseline
            var requestOffset = if (refresh) "" else feedPagination.offset(scope, type)
            while (true) {
                val previousOffset = requestOffset
                val requestUpdateBaseline = if (previousOffset.isBlank() && useIncrementalRefresh) {
                    paginationBeforeRefresh.updateBaseline
                } else {
                    ""
                }
                val response = fetchDynamicFeedPageWithRetry {
                    NetworkModule.dynamicApi.getDynamicFeed(
                        type = type,
                        offset = previousOffset,
                        updateBaseline = requestUpdateBaseline
                    )
                }.getOrElse { error ->
                    return@withContext Result.failure(error)
                }

                val data = response.data
                if (data == null) {
                    feedPagination.updateState(
                        scope = scope,
                        type = type,
                        state = resolveDynamicPaginationStateAfterPage(
                            paginationBeforeRefresh = paginationForPageUpdate,
                            responseOffset = previousOffset,
                            responseUpdateBaseline = "",
                            responseHasMore = false,
                            preserveExistingPagination = useIncrementalRefresh,
                            reportedUpdateNum = reportedUpdateNum
                        )
                    )
                    break
                }

                if (pagesFetched == 0) {
                    // 首包的 update_num 才是「相对 update_baseline 的新动态数」
                    reportedUpdateNum = data.update_num.coerceAtLeast(0)
                }
                resolvedUpdateBaseline = resolveDynamicFeedUpdateBaseline(
                    currentBaseline = resolvedUpdateBaseline,
                    responseBaseline = data.update_baseline,
                    pagesFetched = pagesFetched
                )

                // Do not keep a broken cursor marked as loadable; otherwise a repeated
                // offset lets the next request return the same page forever.
                val pageHasMore = data.has_more && hasDynamicPaginationProgress(
                    previousOffset = previousOffset,
                    nextOffset = data.offset
                )
                requestOffset = data.offset
                feedPagination.updateState(
                    scope = scope,
                    type = type,
                    state = resolveDynamicPaginationStateAfterPage(
                        paginationBeforeRefresh = paginationForPageUpdate,
                        responseOffset = data.offset,
                        responseUpdateBaseline = resolvedUpdateBaseline,
                        responseHasMore = pageHasMore,
                        preserveExistingPagination = useIncrementalRefresh,
                        reportedUpdateNum = reportedUpdateNum
                    )
                )

                // Keep folded/hidden cards in the timeline. PiliPlus only shrinks
                // `visible == false` items in UI and unfolds them from module_fold;
                // dropping them here swallows the hours between two visible posts.
                visibleItems += data.items
                fetchedItemCount += data.items.size
                pagesFetched += 1

                val shouldContinue = if (useIncrementalRefresh) {
                    shouldContinueDynamicIncrementalFetch(
                        accumulatedItemCount = fetchedItemCount,
                        updateNum = reportedUpdateNum,
                        hasMore = pageHasMore,
                        previousOffset = previousOffset,
                        nextOffset = data.offset
                    )
                } else {
                    shouldContinueDynamicFetchAfterFilter(
                        accumulatedVisibleCount = visibleItems.size,
                        hasMore = pageHasMore,
                        previousOffset = previousOffset,
                        nextOffset = data.offset,
                        pagesFetched = pagesFetched
                    )
                }
                if (!shouldContinue) {
                    break
                }
            }

            Result.success(
                DynamicFeedFetchResult(
                    items = visibleItems,
                    updateNum = reportedUpdateNum,
                    usedUpdateBaseline = useIncrementalRefresh,
                    nextOffset = requestOffset,
                    hasMore = feedPagination.hasMore(scope, type)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun currentUpdateBaseline(
        scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN,
        type: String = "all"
    ): String = feedPagination.updateBaseline(scope, type)
    
    /**
     *  [新增] 获取指定用户的动态列表
     * @param hostMid UP主 mid
     * @param refresh 是否刷新 (重置分页)
     */
    suspend fun getUserDynamicFeed(hostMid: Long, refresh: Boolean = false): Result<List<DynamicItem>> = withContext(Dispatchers.IO) {
        try {
            if (refresh) {
                userFeedPagination.reset(hostMid)
            }
            
            if (!userFeedPagination.hasMore(hostMid) && !refresh) {
                return@withContext Result.success(emptyList())
            }

            val visibleItems = mutableListOf<DynamicItem>()
            var pagesFetched = 0
            while (true) {
                val previousOffset = userFeedPagination.offset(hostMid)
                val response = fetchDynamicFeedPageWithRetry {
                    NetworkModule.dynamicApi.getUserDynamicFeed(
                        params = buildSelectedUserDynamicFeedParams(
                            hostMid = hostMid,
                            offset = previousOffset
                        )
                    )
                }.getOrElse { error ->
                    return@withContext Result.failure(error)
                }

                val data = response.data
                if (data == null) {
                    userFeedPagination.update(
                        hostMid = hostMid,
                        offset = previousOffset,
                        hasMore = false
                    )
                    break
                }

                // 更新分页状态
                userFeedPagination.update(
                    hostMid = hostMid,
                    offset = data.offset,
                    hasMore = data.has_more
                )

                visibleItems += data.items
                pagesFetched += 1

                if (!shouldContinueDynamicFetchAfterFilter(
                        accumulatedVisibleCount = visibleItems.size,
                        hasMore = data.has_more,
                        previousOffset = previousOffset,
                        nextOffset = data.offset,
                        pagesFetched = pagesFetched
                    )
                ) {
                    break
                }
            }

            Result.success(visibleItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取单条动态详情。主路径对齐 PiliPlus 的 web `/v1/detail`，
     * 再按需降级到 opus、desktop，以及列表卡片缓存。
     */
    suspend fun getDynamicDetail(dynamicId: String): Result<DynamicItem> = withContext(Dispatchers.IO) {
        try {
            val cleanedId = dynamicId.trim()
            if (cleanedId.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("dynamicId 不能为空"))
            }

            val seed = peekDynamicDetailSeed(cleanedId)
            val candidates = mutableListOf<DynamicItem>()

            val webItem = fetchWebDetailItem(id = cleanedId)
            webItem?.let(candidates::add)

            var opusFallbackCvId: Long? = null
            if (shouldRequestOpusDetailForDynamicDetail(webItem = webItem, seedItem = seed)) {
                val opusFetch = fetchOpusDetail(cleanedId)
                opusFetch.item?.let(candidates::add)
                opusFallbackCvId = opusFetch.fallbackCvId
            }

            val preferredAfterWeb = resolvePreferredDynamicDetailItem(candidates)
            if (preferredAfterWeb != null &&
                shouldFetchStandardDetailForPlainTextDynamic(preferredAfterWeb)
            ) {
                fetchDesktopDetailItem(cleanedId)?.let { desktopItem ->
                    candidates += mergeDynamicDetailWithLongerDesc(
                        desktopItem = preferredAfterWeb,
                        standardItem = desktopItem,
                    )
                }
            }

            if (preferredAfterWeb == null || shouldFallbackForDynamicDetail(preferredAfterWeb)) {
                fetchDesktopDetailItem(cleanedId)?.let(candidates::add)
            }

            val rid = seed?.basic?.rid_str.orEmpty()
            if (shouldFetchDynamicDetailByRid(resolvePreferredDynamicDetailItem(candidates), rid)) {
                fetchWebDetailItem(id = null, rid = rid, type = 2)?.let(candidates::add)
            }

            seed?.let(candidates::add)
            val resolved = resolvePreferredDynamicDetailItem(candidates)
            if (resolved != null) {
                var merged = mergeRicherOpusDetailContent(resolved, candidates)
                val opusBlocks = merged.modules.module_dynamic?.major?.opus?.contentBlocks.orEmpty()
                val cvId = resolveOpusArticleFallbackCvId(
                    fallbackId = opusFallbackCvId,
                    commentType = merged.basic?.comment_type ?: 0,
                    commentIdStr = merged.basic?.comment_id_str.orEmpty()
                )
                if (cvId != null && shouldFetchArticleFallbackForOpus(opusBlocks, opusFallbackCvId)) {
                    ArticleRepository.getArticleDetail(cvId).getOrNull()?.let { article ->
                        merged = mergeArticleDetailIntoOpus(
                            base = merged,
                            title = article.title,
                            blocks = article.blocks
                        )
                    }
                }
                return@withContext Result.success(
                    mergeDynamicDetailInteractionMetadata(
                        detailItem = merged,
                        seedItem = seed
                    )
                )
            }

            Result.failure(Exception("动态详情为空"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun fetchWebDetailItem(
        id: String? = null,
        rid: String? = null,
        type: Int? = null
    ): DynamicItem? {
        return runCatching {
            val response = NetworkModule.dynamicApi.getDynamicDetail(
                id = id,
                rid = rid,
                type = type
            )
            response.data?.item?.takeIf { response.code == 0 }
        }.getOrNull()
    }

    private data class OpusDetailFetch(
        val item: DynamicItem?,
        val fallbackCvId: Long?
    )

    private suspend fun fetchOpusDetail(dynamicId: String): OpusDetailFetch {
        return runCatching {
            val response = NetworkModule.dynamicApi.getOpusDetail(
                signDynamicWbi(
                    mapOf(
                        "id" to dynamicId,
                        "timezone_offset" to "-480",
                        "features" to OPUS_DETAIL_FEATURES
                    )
                )
            )
            if (response.code != 0) {
                return@runCatching OpusDetailFetch(item = null, fallbackCvId = null)
            }
            OpusDetailFetch(
                item = response.data?.item,
                fallbackCvId = response.data?.fallback?.id?.takeIf { it > 0L }
            )
        }.getOrElse { error ->
            Logger.w(
                tag = "DynamicRepository",
                message = "解析图文动态全文失败: dynamicId=$dynamicId",
                throwable = error
            )
            OpusDetailFetch(item = null, fallbackCvId = null)
        }
    }

    private suspend fun signDynamicWbi(params: Map<String, String>): Map<String, String> {
        return try {
            val navResp = NetworkModule.api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            if (imgKey.isNotEmpty() && subKey.isNotEmpty()) {
                WbiUtils.sign(params, imgKey, subKey)
            } else {
                params
            }
        } catch (_: Exception) {
            params
        }
    }

    private suspend fun fetchDesktopDetailItem(dynamicId: String): DynamicItem? {
        return runCatching {
            val response = NetworkModule.dynamicApi.getDynamicDetailFallback(id = dynamicId)
            response.data?.item?.takeIf { response.code == 0 }
        }.getOrNull()
    }
    
    /**
     * 是否还有更多数据
     */
    fun hasMoreData(
        scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN,
        type: String = "all"
    ): Boolean {
        return feedPagination.hasMore(scope, type)
    }

    fun syncPaginationAfterRefresh(
        scope: DynamicFeedScope,
        type: String = "all",
        offset: String,
        updateBaseline: String = "",
        hasMore: Boolean = true
    ) {
        feedPagination.update(
            scope = scope,
            type = type,
            offset = offset,
            updateBaseline = updateBaseline.ifBlank { feedPagination.updateBaseline(scope, type) },
            hasMore = hasMore
        )
    }

    suspend fun getDynamicUpdateCount(
        scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN,
        type: String = "all",
        advanceBaseline: Boolean = true
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updateBaseline = feedPagination.updateBaseline(scope, type)
            if (!advanceBaseline) {
                // 轻量未读接口：只返回新动态条数，避免轮询时拉全量 feed。
                val updateResponse = NetworkModule.dynamicApi.getDynamicUpdateCount(
                    type = type,
                    updateBaseline = updateBaseline
                )
                if (updateResponse.code != 0) {
                    return@withContext Result.failure(
                        Exception(
                            resolveDynamicFriendlyErrorMessage(
                                updateResponse.code,
                                updateResponse.message
                            )
                        )
                    )
                }
                val updateData = updateResponse.data
                    ?: return@withContext Result.failure(Exception("动态更新数为空"))
                return@withContext Result.success(updateData.update_num.coerceAtLeast(0))
            }
            val response = fetchDynamicFeedPageWithRetry {
                NetworkModule.dynamicApi.getDynamicFeed(
                    type = type,
                    offset = "",
                    updateBaseline = updateBaseline
                )
            }.getOrElse { error ->
                return@withContext Result.failure(error)
            }
            val data = response.data ?: return@withContext Result.failure(Exception("动态更新数为空"))
            val nextBaseline = resolveDynamicUpdateCountBaseline(
                currentBaseline = updateBaseline,
                responseBaseline = data.update_baseline,
                advanceBaseline = advanceBaseline
            )
            if (nextBaseline.isNotBlank() && nextBaseline != updateBaseline) {
                feedPagination.updateBaseline(
                    scope = scope,
                    type = type,
                    updateBaseline = nextBaseline
                )
            }
            Result.success(data.update_num.coerceAtLeast(0))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     *  [新增] 用户动态是否还有更多
     */
    fun userHasMoreData(hostMid: Long?): Boolean {
        if (hostMid == null || hostMid <= 0L) return true
        return userFeedPagination.hasMore(hostMid)
    }
    
    /**
     * 重置分页状态
     */
    fun resetPagination(
        scope: DynamicFeedScope = DynamicFeedScope.DYNAMIC_SCREEN,
        type: String = "all"
    ) {
        feedPagination.reset(scope, type)
    }
    
    /**
     *  [新增] 重置用户动态分页状态
     */
    fun resetUserPagination(hostMid: Long? = null) {
        if (hostMid == null || hostMid <= 0L) {
            userFeedPagination.resetAll()
        } else {
            userFeedPagination.reset(hostMid)
        }
    }

    internal fun buildSelectedUserDynamicFeedParams(
        hostMid: Long,
        offset: String
    ): Map<String, String> {
        return mapOf(
            "host_mid" to hostMid.toString(),
            "offset" to offset,
            "features" to com.android.purebilibili.core.network.SPACE_DYNAMIC_FEATURES,
            "timezone_offset" to "-480",
            "platform" to "web",
            "web_location" to "333.1387"
        )
    }

    private suspend fun fetchDynamicFeedPageWithRetry(
        request: suspend () -> DynamicFeedResponse
    ): Result<DynamicFeedResponse> {
        var lastError: Throwable? = null
        for (attempt in 1..DYNAMIC_FETCH_MAX_ATTEMPTS) {
            try {
                val response = request()
                if (response.code == 0) {
                    return Result.success(response)
                }
                val shouldRetry = attempt < DYNAMIC_FETCH_MAX_ATTEMPTS &&
                    isRetryableDynamicApiError(response.code, response.message)
                if (shouldRetry) {
                    delay(resolveDynamicRetryDelayMs(attempt))
                    continue
                }
                val message = resolveDynamicFriendlyErrorMessage(response.code, response.message)
                return Result.failure(Exception(message))
            } catch (error: Exception) {
                lastError = error
                val shouldRetry = attempt < DYNAMIC_FETCH_MAX_ATTEMPTS &&
                    isRetryableDynamicException(error)
                if (shouldRetry) {
                    delay(resolveDynamicRetryDelayMs(attempt))
                    continue
                }
                val message = resolveDynamicFriendlyErrorMessage(code = -1, message = error.message.orEmpty())
                return Result.failure(Exception(message, error))
            }
        }
        val message = resolveDynamicFriendlyErrorMessage(code = -1, message = lastError?.message.orEmpty())
        return Result.failure(Exception(message, lastError))
    }
}

internal fun resolveDynamicUpdateCountBaseline(
    currentBaseline: String,
    responseBaseline: String,
    advanceBaseline: Boolean
): String {
    if (responseBaseline.isBlank()) return currentBaseline
    if (advanceBaseline) return responseBaseline
    return currentBaseline
}

internal fun shouldUseDynamicIncrementalRefresh(
    refresh: Boolean,
    incrementalRefreshEnabled: Boolean,
    updateBaseline: String
): Boolean {
    return refresh && incrementalRefreshEnabled && updateBaseline.isNotBlank()
}

internal fun resolveDynamicPaginationStateAfterPage(
    paginationBeforeRefresh: DynamicPaginationState,
    responseOffset: String,
    responseUpdateBaseline: String,
    responseHasMore: Boolean,
    preserveExistingPagination: Boolean,
    reportedUpdateNum: Int = -1
): DynamicPaginationState {
    val nextBaseline = responseUpdateBaseline.ifBlank {
        paginationBeforeRefresh.updateBaseline
    }
    val canPreserve = preserveExistingPagination &&
        paginationBeforeRefresh.offset.isNotBlank() &&
        reportedUpdateNum != 0

    return if (canPreserve) {
        paginationBeforeRefresh.copy(updateBaseline = nextBaseline)
    } else {
        DynamicPaginationState(
            offset = responseOffset,
            updateBaseline = nextBaseline,
            hasMore = responseHasMore
        )
    }
}

enum class DynamicFeedScope {
    DYNAMIC_SCREEN,
    HOME_FOLLOW
}

data class DynamicFeedFetchResult(
    val items: List<DynamicItem>,
    val updateNum: Int = 0,
    val usedUpdateBaseline: Boolean = false,
    val nextOffset: String = "",
    val hasMore: Boolean = true
)

internal data class DynamicPaginationState(
    var offset: String = "",
    var updateBaseline: String = "",
    var hasMore: Boolean = true
)

internal data class DynamicFeedPaginationKey(
    val scope: DynamicFeedScope,
    val type: String
)

internal class DynamicFeedPaginationRegistry {
    private val stateByScope = mutableMapOf<DynamicFeedPaginationKey, DynamicPaginationState>()

    fun reset(scope: DynamicFeedScope, type: String = "all") {
        stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)] = DynamicPaginationState()
    }

    fun update(
        scope: DynamicFeedScope,
        type: String = "all",
        offset: String,
        updateBaseline: String = "",
        hasMore: Boolean
    ) {
        stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)] =
            DynamicPaginationState(
                offset = offset,
                updateBaseline = updateBaseline,
                hasMore = hasMore
            )
    }

    fun updateState(
        scope: DynamicFeedScope,
        type: String = "all",
        state: DynamicPaginationState
    ) {
        stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)] = state.copy()
    }

    fun snapshot(
        scope: DynamicFeedScope,
        type: String = "all"
    ): DynamicPaginationState {
        return stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)]?.copy()
            ?: DynamicPaginationState()
    }

    fun offset(scope: DynamicFeedScope, type: String = "all"): String {
        return stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)]?.offset.orEmpty()
    }

    fun updateBaseline(scope: DynamicFeedScope, type: String = "all"): String {
        return stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)]?.updateBaseline.orEmpty()
    }

    fun updateBaseline(
        scope: DynamicFeedScope,
        type: String = "all",
        updateBaseline: String
    ) {
        val key = DynamicFeedPaginationKey(scope = scope, type = type)
        val current = stateByScope[key] ?: DynamicPaginationState()
        stateByScope[key] = current.copy(updateBaseline = updateBaseline)
    }

    fun hasMore(scope: DynamicFeedScope, type: String = "all"): Boolean {
        return stateByScope[DynamicFeedPaginationKey(scope = scope, type = type)]?.hasMore ?: true
    }
}

internal class DynamicUserPaginationRegistry {
    private val stateByUser = mutableMapOf<Long, DynamicPaginationState>()

    fun reset(hostMid: Long) {
        stateByUser[hostMid] = DynamicPaginationState()
    }

    fun resetAll() {
        stateByUser.clear()
    }

    fun update(hostMid: Long, offset: String, hasMore: Boolean) {
        stateByUser[hostMid] = DynamicPaginationState(offset = offset, hasMore = hasMore)
    }

    fun offset(hostMid: Long): String {
        return stateByUser[hostMid]?.offset.orEmpty()
    }

    fun hasMore(hostMid: Long): Boolean {
        return stateByUser[hostMid]?.hasMore ?: true
    }
}
