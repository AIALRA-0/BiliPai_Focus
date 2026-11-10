package com.android.purebilibili.feature.following

import com.android.purebilibili.data.model.response.FollowingUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowingGroupPolicyTest {

    @Test
    fun `default group should require loaded mapping and empty groups`() {
        val userMid = 1001L
        assertFalse(
            isUserInDefaultFollowGroup(
                userMid = userMid,
                userFollowGroupIds = emptyMap()
            )
        )
        assertTrue(
            isUserInDefaultFollowGroup(
                userMid = userMid,
                userFollowGroupIds = mapOf(userMid to emptySet())
            )
        )
        assertFalse(
            isUserInDefaultFollowGroup(
                userMid = userMid,
                userFollowGroupIds = mapOf(userMid to setOf(-10L))
            )
        )
    }

    @Test
    fun `filter users by selected group should keep unknown users out of default tab`() {
        val users = listOf(
            FollowingUser(mid = 1L, uname = "A"),
            FollowingUser(mid = 2L, uname = "B"),
            FollowingUser(mid = 3L, uname = "C")
        )
        val groupMap = mapOf(
            1L to emptySet(),          // 默认分组
            2L to setOf(194111L)       // 自定义分组
            // 3L 缺失：分组信息未加载/失败
        )

        val defaultUsers = filterUsersBySelectedFollowGroup(
            users = users,
            selectedGroupFilter = 0L,
            userFollowGroupIds = groupMap,
            defaultGroupTagId = 0L,
            allGroupTagId = Long.MIN_VALUE
        )
        assertEquals(listOf(1L), defaultUsers.map { it.mid })

        val customUsers = filterUsersBySelectedFollowGroup(
            users = users,
            selectedGroupFilter = 194111L,
            userFollowGroupIds = groupMap,
            defaultGroupTagId = 0L,
            allGroupTagId = Long.MIN_VALUE
        )
        assertEquals(listOf(2L), customUsers.map { it.mid })
    }

    @Test
    fun `mergeFollowingUsersDistinct should append new users and ignore removed duplicates`() {
        val current = listOf(
            FollowingUser(mid = 1L, uname = "A"),
            FollowingUser(mid = 2L, uname = "B")
        )
        val incoming = listOf(
            FollowingUser(mid = 2L, uname = "B-new"),
            FollowingUser(mid = 3L, uname = "C"),
            FollowingUser(mid = 4L, uname = "D")
        )

        val merged = mergeFollowingUsersDistinct(
            currentUsers = current,
            incomingUsers = incoming,
            removedUserMids = setOf(4L)
        )

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.mid })
        assertEquals("B", merged[1].uname)
    }

    @Test
    fun `isFollowingListIncomplete should compare loaded users with server total`() {
        assertTrue(isFollowingListIncomplete(1000, 1100))
        assertFalse(isFollowingListIncomplete(1100, 1100))
        assertFalse(isFollowingListIncomplete(1110, 1100))
    }

    @Test
    fun `following pagination continues only while a page adds unique users`() {
        // 正常推进：上一页 50，本页新增 50，合并后 100，服务器总数 300 还有更多。
        assertTrue(
            shouldContinueFollowingPagination(
                previousCount = 50,
                mergedCount = 100,
                incomingCount = 50,
                total = 300
            )
        )
        // 服务器在边界重复返回同一页：去重后没有新增，必须停下，避免无限翻页。
        assertFalse(
            shouldContinueFollowingPagination(
                previousCount = 100,
                mergedCount = 100,
                incomingCount = 50,
                total = 300
            )
        )
        // 空页立即停下。
        assertFalse(
            shouldContinueFollowingPagination(
                previousCount = 100,
                mergedCount = 100,
                incomingCount = 0,
                total = 300
            )
        )
        // 合并后已达到服务器总数：到底。
        assertFalse(
            shouldContinueFollowingPagination(
                previousCount = 250,
                mergedCount = 300,
                incomingCount = 50,
                total = 300
            )
        )
    }

    @Test
    fun `shouldPublishFollowingLoadBatch should batch intermediate page updates`() {
        assertFalse(
            shouldPublishFollowingLoadBatch(
                loadedCount = 150,
                total = 1100,
                pagesSinceLastPublish = 1,
                publishIntervalPages = 3
            )
        )
        assertTrue(
            shouldPublishFollowingLoadBatch(
                loadedCount = 200,
                total = 1100,
                pagesSinceLastPublish = 3,
                publishIntervalPages = 3
            )
        )
        assertTrue(
            shouldPublishFollowingLoadBatch(
                loadedCount = 1100,
                total = 1100,
                pagesSinceLastPublish = 1,
                publishIntervalPages = 3
            )
        )
    }

    @Test
    fun `addFollowGroupMappingIfSuccess should skip failed lookups`() {
        val target = linkedMapOf<Long, Set<Long>>()
        val failed = Result.failure<Set<Long>>(IllegalStateException("rate limited"))

        val error = addFollowGroupMappingIfSuccess(
            target = target,
            userMid = 1001L,
            result = failed
        )

        assertTrue(error is IllegalStateException)
        assertTrue(!target.containsKey(1001L))
    }

    @Test
    fun `addFollowGroupMappingIfSuccess should store successful lookups`() {
        val target = linkedMapOf<Long, Set<Long>>()

        val error = addFollowGroupMappingIfSuccess(
            target = target,
            userMid = 1002L,
            result = Result.success(setOf(-10L, 194111L))
        )

        assertEquals(null, error)
        assertEquals(setOf(-10L, 194111L), target[1002L])
    }
}
