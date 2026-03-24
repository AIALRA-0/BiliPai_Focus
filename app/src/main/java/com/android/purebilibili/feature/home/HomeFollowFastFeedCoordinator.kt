package com.android.purebilibili.feature.home

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val HOME_FOLLOW_FAST_MAX_CONCURRENT_USERS = HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE

internal data class HomeFollowUserFeedPage(
    val videos: List<VideoItem> = emptyList(),
    val nextOffset: String = "",
    val hasMore: Boolean = false
)

internal interface HomeFollowFeedDataSource {
    suspend fun fetchUserFeedPage(
        hostMid: Long,
        offset: String
    ): Result<HomeFollowUserFeedPage>
}

internal data class HomeFollowUserCursor(
    val offset: String = "",
    val hasMore: Boolean = true,
    val initialized: Boolean = false
)

internal data class HomeFollowFastCursor(
    val visibleUserMidsInOrder: List<Long> = emptyList(),
    val userStates: Map<Long, HomeFollowUserCursor> = emptyMap(),
    val nextUserStartIndex: Int = 0,
    val seed: Long = 0L,
    val waveCount: Int = 0
)

internal data class HomeFollowFastSession(
    val baselineRawVideos: List<VideoItem>,
    val roundRawVideos: List<VideoItem>,
    val baselineVisibleCount: Int,
    val requiredVisibleIncrement: Int,
    val isLoadMore: Boolean,
    val cursor: HomeFollowFastCursor
)

internal data class HomeFollowFastWave(
    val session: HomeFollowFastSession,
    val presentedRawVideos: List<VideoItem>,
    val visibleVideos: List<VideoItem>,
    val visibleIncrement: Int,
    val hasMoreUsers: Boolean,
    val firstErrorMessage: String? = null,
    val requestedUserCount: Int = 0,
    val successfulUserCount: Int = 0
)

internal class NetworkHomeFollowFeedDataSource : HomeFollowFeedDataSource {
    override suspend fun fetchUserFeedPage(
        hostMid: Long,
        offset: String
    ): Result<HomeFollowUserFeedPage> {
        return runCatching {
            val response = NetworkModule.dynamicApi.getUserDynamicFeed(
                params = buildHomeFollowUserFeedParams(
                    hostMid = hostMid,
                    offset = offset
                )
            )
            if (response.code != 0) {
                error(response.message.ifBlank { "加载关注动态失败" })
            }

            val data = response.data ?: return@runCatching HomeFollowUserFeedPage(
                videos = emptyList(),
                nextOffset = offset,
                hasMore = false
            )

            HomeFollowUserFeedPage(
                videos = mapHomeFollowDynamicItemsToVideoItems(
                    data.items.filter { it.visible }
                ),
                nextOffset = data.offset,
                hasMore = data.has_more && data.offset != offset
            )
        }
    }

    private fun buildHomeFollowUserFeedParams(
        hostMid: Long,
        offset: String
    ): Map<String, String> {
        return mapOf(
            "host_mid" to hostMid.toString(),
            "offset" to offset,
            "page" to "1",
            "features" to "itemOpusStyle,listOnlyfans",
            "timezone_offset" to "-480",
            "platform" to "web",
            "web_location" to "333.1387"
        )
    }
}

internal class HomeFollowFastFeedCoordinator(
    private val dataSource: HomeFollowFeedDataSource,
    private val nowMs: () -> Long = System::currentTimeMillis
) {
    fun startSession(
        existingRawVideos: List<VideoItem>,
        existingVisibleCount: Int,
        visibleUserMids: Collection<Long>,
        isLoadMore: Boolean,
        previousCursor: HomeFollowFastCursor? = null,
        requiredVisibleIncrement: Int = HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE,
        seed: Long = nowMs()
    ): HomeFollowFastSession {
        val normalizedVisibleUserMids = visibleUserMids
            .filter { it > 0L }
            .distinct()
        val cursor = if (isLoadMore) {
            resolveLoadMoreCursor(
                visibleUserMids = normalizedVisibleUserMids,
                previousCursor = previousCursor,
                seed = seed
            )
        } else {
            createFreshCursor(
                visibleUserMids = normalizedVisibleUserMids,
                seed = seed
            )
        }

        return HomeFollowFastSession(
            baselineRawVideos = existingRawVideos,
            roundRawVideos = emptyList(),
            baselineVisibleCount = existingVisibleCount,
            requiredVisibleIncrement = requiredVisibleIncrement.coerceAtLeast(0),
            isLoadMore = isLoadMore,
            cursor = cursor
        )
    }

    suspend fun fetchWave(
        session: HomeFollowFastSession,
        visibleVideoFilter: (List<VideoItem>) -> List<VideoItem>
    ): HomeFollowFastWave = coroutineScope {
        val requestUserMids = selectUserMidsForWave(session.cursor)
        if (requestUserMids.isEmpty()) {
            val presentedRawVideos = resolveHomeFollowPresentedRawVideos(
                baselineRawVideos = session.baselineRawVideos,
                roundRawVideos = session.roundRawVideos,
                isLoadMore = session.isLoadMore,
                keySelector = ::resolveHomeFollowVideoKey
            )
            val visibleVideos = visibleVideoFilter(presentedRawVideos)
            return@coroutineScope HomeFollowFastWave(
                session = session,
                presentedRawVideos = presentedRawVideos,
                visibleVideos = visibleVideos,
                visibleIncrement = resolveHomeFollowVisibleIncrement(
                    baselineVisibleCount = session.baselineVisibleCount,
                    currentVisibleCount = visibleVideos.size
                ),
                hasMoreUsers = false
            )
        }

        val requestResults = requestUserMids.map { hostMid ->
            async {
                val state = session.cursor.userStates[hostMid] ?: HomeFollowUserCursor()
                hostMid to dataSource.fetchUserFeedPage(
                    hostMid = hostMid,
                    offset = if (state.initialized) state.offset else ""
                )
            }
        }.awaitAll()

        val updatedUserStates = session.cursor.userStates.toMutableMap()
        var firstErrorMessage: String? = null
        var successfulUserCount = 0
        val waveVideos = buildList {
            requestResults.forEach { (hostMid, result) ->
                result.onSuccess { page ->
                    successfulUserCount += 1
                    updatedUserStates[hostMid] = HomeFollowUserCursor(
                        offset = page.nextOffset,
                        hasMore = page.hasMore,
                        initialized = true
                    )
                    addAll(page.videos)
                }.onFailure { error ->
                    if (firstErrorMessage == null) {
                        firstErrorMessage = error.message ?: "加载关注动态失败"
                    }
                    updatedUserStates[hostMid] = HomeFollowUserCursor(
                        offset = "",
                        hasMore = false,
                        initialized = true
                    )
                }
            }
        }

        val randomizedWaveVideos = randomizeHomeFollowIncomingVideos(
            videos = waveVideos,
            seed = session.cursor.seed + session.cursor.waveCount + 1L
        )
        val updatedRoundRawVideos = appendDistinctByKey(
            session.roundRawVideos,
            randomizedWaveVideos,
            ::resolveHomeFollowVideoKey
        )
        val updatedCursor = session.cursor.copy(
            userStates = updatedUserStates,
            nextUserStartIndex = resolveNextWaveStartIndex(
                currentStartIndex = session.cursor.nextUserStartIndex,
                requestedUserCount = requestUserMids.size,
                totalVisibleUserCount = session.cursor.visibleUserMidsInOrder.size
            ),
            waveCount = session.cursor.waveCount + 1
        )
        val updatedSession = session.copy(
            roundRawVideos = updatedRoundRawVideos,
            cursor = updatedCursor
        )
        val presentedRawVideos = resolveHomeFollowPresentedRawVideos(
            baselineRawVideos = updatedSession.baselineRawVideos,
            roundRawVideos = updatedSession.roundRawVideos,
            isLoadMore = updatedSession.isLoadMore,
            keySelector = ::resolveHomeFollowVideoKey
        )
        val visibleVideos = visibleVideoFilter(presentedRawVideos)

        HomeFollowFastWave(
            session = updatedSession,
            presentedRawVideos = presentedRawVideos,
            visibleVideos = visibleVideos,
            visibleIncrement = resolveHomeFollowVisibleIncrement(
                baselineVisibleCount = updatedSession.baselineVisibleCount,
                currentVisibleCount = visibleVideos.size
            ),
            hasMoreUsers = hasMoreUsers(updatedCursor),
            firstErrorMessage = firstErrorMessage,
            requestedUserCount = requestUserMids.size,
            successfulUserCount = successfulUserCount
        )
    }

    private fun createFreshCursor(
        visibleUserMids: List<Long>,
        seed: Long
    ): HomeFollowFastCursor {
        val randomizedUserMids = randomizeHomeFollowVisibleUserMids(
            userMids = visibleUserMids,
            seed = seed
        )
        return HomeFollowFastCursor(
            visibleUserMidsInOrder = randomizedUserMids,
            userStates = randomizedUserMids.associateWith { HomeFollowUserCursor() },
            seed = seed
        )
    }

    private fun resolveLoadMoreCursor(
        visibleUserMids: List<Long>,
        previousCursor: HomeFollowFastCursor?,
        seed: Long
    ): HomeFollowFastCursor {
        if (previousCursor == null) {
            return createFreshCursor(
                visibleUserMids = visibleUserMids,
                seed = seed
            )
        }

        val visibleUserSet = visibleUserMids.toSet()
        val preservedOrder = previousCursor.visibleUserMidsInOrder.filter { it in visibleUserSet }
        val newUserOrder = randomizeHomeFollowVisibleUserMids(
            userMids = visibleUserSet - preservedOrder.toSet(),
            seed = previousCursor.seed
        )
        val mergedOrder = preservedOrder + newUserOrder
        val mergedStates = mergedOrder.associateWith { hostMid ->
            previousCursor.userStates[hostMid] ?: HomeFollowUserCursor()
        }
        val normalizedNextIndex = if (mergedOrder.isEmpty()) {
            0
        } else {
            previousCursor.nextUserStartIndex % mergedOrder.size
        }

        return previousCursor.copy(
            visibleUserMidsInOrder = mergedOrder,
            userStates = mergedStates,
            nextUserStartIndex = normalizedNextIndex,
            seed = if (previousCursor.seed != 0L) previousCursor.seed else seed
        )
    }

    private fun selectUserMidsForWave(
        cursor: HomeFollowFastCursor,
        maxConcurrentUsers: Int = HOME_FOLLOW_FAST_MAX_CONCURRENT_USERS
    ): List<Long> {
        val order = cursor.visibleUserMidsInOrder
        if (order.isEmpty()) return emptyList()

        val selected = mutableListOf<Long>()
        var visitedCount = 0
        var index = if (order.isEmpty()) 0 else cursor.nextUserStartIndex % order.size
        while (visitedCount < order.size && selected.size < maxConcurrentUsers) {
            val hostMid = order[index]
            val state = cursor.userStates[hostMid] ?: HomeFollowUserCursor()
            if (!state.initialized || state.hasMore) {
                selected += hostMid
            }
            index = (index + 1) % order.size
            visitedCount += 1
        }
        return selected
    }

    private fun resolveNextWaveStartIndex(
        currentStartIndex: Int,
        requestedUserCount: Int,
        totalVisibleUserCount: Int
    ): Int {
        if (requestedUserCount <= 0 || totalVisibleUserCount <= 0) return 0
        return (currentStartIndex + requestedUserCount) % totalVisibleUserCount
    }

    private fun hasMoreUsers(cursor: HomeFollowFastCursor): Boolean {
        return cursor.visibleUserMidsInOrder.any { hostMid ->
            val state = cursor.userStates[hostMid] ?: HomeFollowUserCursor()
            !state.initialized || state.hasMore
        }
    }
}
