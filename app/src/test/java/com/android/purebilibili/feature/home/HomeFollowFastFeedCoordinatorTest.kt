package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFollowFastFeedCoordinatorTest {

    @Test
    fun `first wave reaches eight visible videos within one second across common follow counts`() = runTest {
        listOf(10, 100, 200, 300).forEach { followCount ->
            val dataSource = FakeHomeFollowFeedDataSource(
                defaultDelayMs = 500L,
                pagesByUserAndOffset = (1..followCount).associate { index ->
                    val mid = index.toLong()
                    mid to mapOf(
                        "" to HomeFollowUserFeedPage(
                            videos = listOf(video(id = mid, dynamicId = "dyn-$mid", ownerMid = mid)),
                            nextOffset = "",
                            hasMore = false
                        )
                    )
                }
            )
            val coordinator = HomeFollowFastFeedCoordinator(
                dataSource = dataSource,
                nowMs = { testScheduler.currentTime }
            )

            val session = coordinator.startSession(
                existingRawVideos = emptyList(),
                existingVisibleCount = 0,
                visibleUserMids = (1..followCount).map { it.toLong() },
                isLoadMore = false,
                seed = 17L
            )

            val startedAt = testScheduler.currentTime
            val wave = coordinator.fetchWave(session) { videos -> videos }
            val elapsedMs = testScheduler.currentTime - startedAt

            assertTrue(
                elapsedMs <= 1_000L,
                "followCount=$followCount should deliver the first wave within 1000ms, actual=$elapsedMs"
            )
            assertEquals(8, wave.visibleVideos.size, "followCount=$followCount should expose 8 visible videos in the first wave")
            assertEquals(8, dataSource.requestHistory.size, "followCount=$followCount should only request the first 8 users in the first wave")
        }
    }

    @Test
    fun `load more session reuses per user offsets instead of restarting from first page`() = runTest {
        val dataSource = FakeHomeFollowFeedDataSource(
            defaultDelayMs = 0L,
            pagesByUserAndOffset = mapOf(
                1L to mapOf(
                    "" to HomeFollowUserFeedPage(
                        videos = listOf(video(id = 1L, dynamicId = "dyn-1a", ownerMid = 1L)),
                        nextOffset = "1-next",
                        hasMore = true
                    ),
                    "1-next" to HomeFollowUserFeedPage(
                        videos = listOf(video(id = 11L, dynamicId = "dyn-1b", ownerMid = 1L)),
                        nextOffset = "",
                        hasMore = false
                    )
                ),
                2L to mapOf(
                    "" to HomeFollowUserFeedPage(
                        videos = listOf(video(id = 2L, dynamicId = "dyn-2a", ownerMid = 2L)),
                        nextOffset = "2-next",
                        hasMore = true
                    ),
                    "2-next" to HomeFollowUserFeedPage(
                        videos = listOf(video(id = 22L, dynamicId = "dyn-2b", ownerMid = 2L)),
                        nextOffset = "",
                        hasMore = false
                    )
                )
            )
        )
        val coordinator = HomeFollowFastFeedCoordinator(
            dataSource = dataSource,
            nowMs = { testScheduler.currentTime }
        )

        val refreshSession = coordinator.startSession(
            existingRawVideos = emptyList(),
            existingVisibleCount = 0,
            visibleUserMids = listOf(1L, 2L),
            isLoadMore = false,
            seed = 9L
        )
        val refreshWave = coordinator.fetchWave(refreshSession) { videos -> videos }

        val loadMoreSession = coordinator.startSession(
            existingRawVideos = refreshWave.presentedRawVideos,
            existingVisibleCount = refreshWave.visibleVideos.size,
            visibleUserMids = listOf(1L, 2L),
            isLoadMore = true,
            previousCursor = refreshWave.session.cursor,
            seed = 9L
        )
        val loadMoreWave = coordinator.fetchWave(loadMoreSession) { videos -> videos }

        assertTrue(dataSource.requestHistory.contains(1L to "1-next"))
        assertTrue(dataSource.requestHistory.contains(2L to "2-next"))
        assertEquals(
            refreshWave.visibleVideos.map { it.dynamicId },
            loadMoreWave.visibleVideos.take(refreshWave.visibleVideos.size).map { it.dynamicId }
        )
        assertTrue(
            loadMoreWave.visibleVideos.takeLast(2).map { it.dynamicId }.toSet()
                .containsAll(setOf("dyn-1b", "dyn-2b"))
        )
    }

    private fun video(
        id: Long,
        dynamicId: String,
        ownerMid: Long
    ): VideoItem {
        return VideoItem(
            id = id,
            aid = id,
            bvid = "BV$id",
            dynamicId = dynamicId,
            title = "video-$id",
            owner = Owner(mid = ownerMid, name = "up-$ownerMid")
        )
    }
}

private class FakeHomeFollowFeedDataSource(
    private val defaultDelayMs: Long,
    private val pagesByUserAndOffset: Map<Long, Map<String, HomeFollowUserFeedPage>>
) : HomeFollowFeedDataSource {
    val requestHistory = mutableListOf<Pair<Long, String>>()

    override suspend fun fetchUserFeedPage(
        hostMid: Long,
        offset: String
    ): Result<HomeFollowUserFeedPage> {
        requestHistory += hostMid to offset
        delay(defaultDelayMs)
        return Result.success(
            pagesByUserAndOffset[hostMid]
                ?.get(offset)
                ?: HomeFollowUserFeedPage(
                    videos = emptyList(),
                    nextOffset = offset,
                    hasMore = false
                )
        )
    }
}
