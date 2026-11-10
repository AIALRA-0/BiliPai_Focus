package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.MyFollowBangumiData
import com.android.purebilibili.data.model.response.UserStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiFollowStatusPolicyTest {

    @Test
    fun `followed should be true when follow equals one`() {
        assertTrue(isBangumiFollowed(UserStatus(follow = 1, followStatus = 0)))
    }

    @Test
    fun `followed should be true when follow_status is positive`() {
        assertTrue(isBangumiFollowed(UserStatus(follow = 0, followStatus = 2)))
    }

    @Test
    fun `followed should be false when follow and follow_status are both zero`() {
        assertFalse(isBangumiFollowed(UserStatus(follow = 0, followStatus = 0)))
    }

    @Test
    fun `follow status label should expose pili plus states`() {
        assertEquals("追番", resolveBangumiFollowStatusLabel(UserStatus(follow = 0, followStatus = 0)))
        assertEquals("想看", resolveBangumiFollowStatusLabel(UserStatus(follow = 1, followStatus = 1)))
        assertEquals("在看", resolveBangumiFollowStatusLabel(UserStatus(follow = 1, followStatus = 2)))
        assertEquals("看过", resolveBangumiFollowStatusLabel(UserStatus(follow = 1, followStatus = 3)))
        assertEquals("已追", resolveBangumiFollowStatusLabel(UserStatus(follow = 1, followStatus = 0)))
    }

    @Test
    fun `follow preload page count should cap by max pages`() {
        assertEquals(9, resolveFollowPreloadPageCount(total = 250, pageSize = 30, maxPages = 30))
        assertEquals(30, resolveFollowPreloadPageCount(total = 5000, pageSize = 30, maxPages = 30))
        assertEquals(0, resolveFollowPreloadPageCount(total = 0, pageSize = 30, maxPages = 30))
    }

    @Test
    fun `preload should merge ids across pages`() = runBlocking {
        val followedSeasonIds = mutableSetOf<Long>()
        val responses = mapOf(
            1 to Result.success(
                MyFollowBangumiData(
                    total = 3,
                    pn = 1,
                    ps = 2,
                    list = listOf(
                        FollowBangumiItem(seasonId = 11L),
                        FollowBangumiItem(seasonId = 22L)
                    )
                )
            ),
            2 to Result.success(
                MyFollowBangumiData(
                    total = 3,
                    pn = 2,
                    ps = 2,
                    list = listOf(FollowBangumiItem(seasonId = 33L))
                )
            )
        )

        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = followedSeasonIds,
            pageSize = 2,
            maxPages = 10,
            fetchPage = { _, page, _ ->
                responses[page] ?: Result.success(MyFollowBangumiData(total = 3, pn = page, ps = 2, list = emptyList()))
            }
        )

        assertTrue(result.requestSucceeded)
        assertEquals(3, result.total)
        assertEquals(setOf(11L, 22L, 33L), followedSeasonIds)
    }

    @Test
    fun `preload should report failed when first page request fails`() = runBlocking {
        val followedSeasonIds = mutableSetOf<Long>()
        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = followedSeasonIds,
            fetchPage = { _, _, _ -> Result.failure(Exception("network error")) }
        )

        assertFalse(result.requestSucceeded)
        assertEquals(0, result.total)
        assertTrue(followedSeasonIds.isEmpty())
    }

    @Test
    fun `preload should continue paging when server clamps page size`() = runBlocking {
        val followedSeasonIds = mutableSetOf<Long>()
        val firstPageIds = (1L..30L).map { FollowBangumiItem(seasonId = it) }
        val secondPageIds = (31L..60L).map { FollowBangumiItem(seasonId = it) }
        val thirdPageIds = (61L..65L).map { FollowBangumiItem(seasonId = it) }

        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = followedSeasonIds,
            pageSize = 100,
            maxPages = 10,
            fetchPage = { _, page, _ ->
                when (page) {
                    1 -> Result.success(MyFollowBangumiData(total = 65, pn = 1, ps = 30, list = firstPageIds))
                    2 -> Result.success(MyFollowBangumiData(total = 65, pn = 2, ps = 30, list = secondPageIds))
                    3 -> Result.success(MyFollowBangumiData(total = 65, pn = 3, ps = 30, list = thirdPageIds))
                    else -> Result.success(MyFollowBangumiData(total = 65, pn = page, ps = 30, list = emptyList()))
                }
            }
        )

        assertTrue(result.requestSucceeded)
        assertEquals(65, result.total)
        assertEquals(65, followedSeasonIds.size)
        assertTrue(followedSeasonIds.contains(65L))
    }

    @Test
    fun `target verification stops after finding season`() = runBlocking {
        val ids = mutableSetOf<Long>()
        val pages = mutableListOf<Int>()
        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = ids,
            targetSeasonId = 31L,
            fetchPage = { _, page, _ ->
                pages.add(page)
                val start = (page - 1) * 30L + 1
                Result.success(MyFollowBangumiData(
                    total = 900, pn = page, ps = 30,
                    list = (start until start + 30).map {
                        FollowBangumiItem(seasonId = it, followStatus = BANGUMI_FOLLOW_STATUS_WATCHED)
                    }
                ))
            }
        )
        assertEquals(listOf(1, 2), pages)
        assertTrue(31L in ids)
        assertEquals(BANGUMI_FOLLOW_STATUS_WATCHED, result.targetFollowStatus)
        val merged = resolveBangumiMergedFollowStatus(
            31L, UserStatus(follow = 1, followStatus = requireNotNull(result.targetFollowStatus)),
            emptyMap(), emptyMap(), ids
        )
        assertEquals(BANGUMI_FOLLOW_STATUS_WATCHED, merged.followStatus)
    }

    @Test
    fun `session switch rejects earlier pages without polluting caller ids`() = runBlocking {
        val ids = mutableSetOf(99L)
        var session = BangumiFollowSession(1L, "账号一")
        val pages = mutableListOf<Int>()
        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = ids,
            pageSize = 1,
            targetSeasonId = 2L,
            sessionProvider = { session },
            fetchPage = { _, page, _ ->
                pages.add(page)
                assertEquals(setOf(99L), ids)
                if (page == 2) session = BangumiFollowSession(2L, "账号二")
                Result.success(MyFollowBangumiData(
                    total = 3, pn = page, ps = 1,
                    list = listOf(FollowBangumiItem(
                        seasonId = page.toLong(), followStatus = BANGUMI_FOLLOW_STATUS_WATCHED
                    ))
                ))
            }
        )
        assertEquals(listOf(1, 2), pages)
        assertFalse(result.requestSucceeded)
        assertEquals(0, result.total)
        assertNull(result.targetFollowStatus)
        assertEquals(setOf(99L), ids)
    }

    @Test
    fun `result cancellation propagates and does not commit earlier pages`() = runBlocking {
        val ids = mutableSetOf(99L)
        val cancellation = CancellationException("取消后台查找")
        try {
            preloadFollowedSeasonsForType(
                type = MY_FOLLOW_TYPE_BANGUMI,
                followedSeasonIds = ids,
                pageSize = 1,
                targetSeasonId = 2L,
                sessionProvider = { BangumiFollowSession(1L, "固定账号") },
                fetchPage = { _, page, _ ->
                    if (page == 1) Result.success(MyFollowBangumiData(
                        total = 3, pn = page, ps = 1,
                        list = listOf(FollowBangumiItem(seasonId = 1L))
                    )) else Result.failure(cancellation)
                }
            )
            fail("取消异常必须传播")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
        assertEquals(setOf(99L), ids)
    }

    @Test
    fun `partial page failure retains ids and does not negate api watched status`() = runBlocking {
        val ids = mutableSetOf<Long>()
        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = ids,
            pageSize = 1,
            targetSeasonId = 2L,
            sessionProvider = { BangumiFollowSession(1L, "固定账号") },
            fetchPage = { _, page, _ ->
                if (page == 1) Result.success(MyFollowBangumiData(
                    total = 3, pn = page, ps = 1,
                    list = listOf(FollowBangumiItem(seasonId = 1L))
                )) else Result.failure(Exception("部分页失败"))
            }
        )
        assertTrue(result.requestSucceeded)
        assertNull(result.targetFollowStatus)
        assertEquals(setOf(1L), ids)
        val merged = resolveBangumiMergedFollowStatus(
            2L, UserStatus(follow = 1, followStatus = BANGUMI_FOLLOW_STATUS_WATCHED),
            emptyMap(), emptyMap(), ids
        )
        assertTrue(merged.isFollowing)
        assertEquals(BANGUMI_FOLLOW_STATUS_WATCHED, merged.followStatus)
    }

    @Test
    fun `partial list miss retains positive api status`() {
        val result = resolveBangumiMergedFollowStatus(
            seasonId = 99L,
            apiUserStatus = UserStatus(follow = 1, followStatus = BANGUMI_FOLLOW_STATUS_WATCHED),
            cachedFollow = emptyMap(), cachedStatus = emptyMap(), followedSeasonIds = setOf(1L)
        )
        assertTrue(result.isFollowing)
        assertEquals(BANGUMI_FOLLOW_STATUS_WATCHED, result.followStatus)
    }

    @Test
    fun `cached status should override followed list and api status`() {
        val result = resolveBangumiMergedFollowStatus(
            seasonId = 1L,
            apiUserStatus = UserStatus(follow = 1, followStatus = BANGUMI_FOLLOW_STATUS_WATCHING),
            cachedFollow = emptyMap(),
            cachedStatus = mapOf(1L to BANGUMI_FOLLOW_STATUS_WATCHED),
            followedSeasonIds = setOf(1L)
        )

        assertEquals(true, result.isFollowing)
        assertEquals(BANGUMI_FOLLOW_STATUS_WATCHED, result.followStatus)
    }

    @Test
    fun `local unfollow should override stale api follow`() {
        val result = resolveBangumiMergedFollowStatus(
            seasonId = 1L,
            apiUserStatus = UserStatus(follow = 1, followStatus = BANGUMI_FOLLOW_STATUS_WATCHING),
            cachedFollow = mapOf(1L to false),
            cachedStatus = emptyMap(),
            followedSeasonIds = setOf(1L)
        )

        assertEquals(false, result.isFollowing)
        assertEquals(0, result.followStatus)
    }

    @Test
    fun `followed list should repair missing api user status`() {
        val result = resolveBangumiMergedFollowStatus(
            seasonId = 1L,
            apiUserStatus = null,
            cachedFollow = emptyMap(),
            cachedStatus = emptyMap(),
            followedSeasonIds = setOf(1L)
        )

        assertEquals(true, result.isFollowing)
        assertEquals(BANGUMI_FOLLOW_STATUS_WANT, result.followStatus)
    }
}
