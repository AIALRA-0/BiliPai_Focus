package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveBrowseRequestPolicyTest {
    @Test
    fun refreshAndLoadMoreAreSingleFlightBeforeCoroutineStarts() {
        val policy = LiveBrowseRequestPolicy()
        val first = assertNotNull(policy.refresh())
        assertNull(policy.refresh())
        assertNull(policy.loadMore(hasMore = true))
        assertTrue(policy.succeed(first))
        val more = assertNotNull(policy.loadMore(hasMore = true))
        assertEquals(2, more.page)
        assertNull(policy.loadMore(hasMore = true))
        assertNull(policy.loadMore(hasMore = false))
    }

    @Test
    fun rapidCategoryAndSortChangesOnlyCommitLatestResponse() {
        val policy = LiveBrowseRequestPolicy()
        val category = assertNotNull(policy.refresh())
        val otherCategory = assertNotNull(policy.refresh(replace = true))
        val otherSort = assertNotNull(policy.refresh(replace = true))
        var visibleRooms = emptyList<Long>()
        if (policy.succeed(otherCategory)) visibleRooms = listOf(20L)
        if (policy.succeed(category)) visibleRooms = listOf(10L)
        assertTrue(policy.owns(otherSort))
        if (policy.succeed(otherSort)) visibleRooms = listOf(30L)
        assertEquals(listOf(30L), visibleRooms)
        assertEquals(2, assertNotNull(policy.loadMore(hasMore = true)).page)
    }

    @Test
    fun refreshSupersedesAppendAndStaleFailureCannotUnlockNewRequest() {
        val policy = LiveBrowseRequestPolicy()
        policy.succeed(assertNotNull(policy.refresh()))
        val oldAppend = assertNotNull(policy.loadMore(hasMore = true))
        val refresh = assertNotNull(policy.refresh())
        assertFalse(policy.fail(oldAppend))
        assertFalse(policy.succeed(oldAppend))
        assertTrue(policy.owns(refresh))
        assertNull(policy.refresh())
        assertNull(policy.loadMore(hasMore = true))
        assertTrue(policy.succeed(refresh))
        assertEquals(2, assertNotNull(policy.loadMore(hasMore = true)).page)
    }

    @Test
    fun failedAppendRetriesTheSamePageAndAdvancesOnlyOnSuccess() {
        val policy = LiveBrowseRequestPolicy()
        policy.succeed(assertNotNull(policy.refresh()))
        val failed = assertNotNull(policy.loadMore(hasMore = true))
        assertEquals(2, failed.page)
        assertTrue(policy.fail(failed))
        val retry = assertNotNull(policy.loadMore(hasMore = true))
        assertEquals(2, retry.page)
        assertTrue(policy.succeed(retry))
        assertEquals(3, assertNotNull(policy.loadMore(hasMore = true)).page)
    }

    @Test
    fun failedRefreshPreservesSuccessfulCursorForRetainedRooms() {
        val policy = LiveBrowseRequestPolicy()
        policy.succeed(assertNotNull(policy.refresh()))
        policy.succeed(assertNotNull(policy.loadMore(hasMore = true)))
        assertTrue(policy.fail(assertNotNull(policy.refresh())))
        assertEquals(3, assertNotNull(policy.loadMore(hasMore = true)).page)
    }

    @Test
    fun clearingSearchInvalidatesFirstPageAndAppendWithoutChangingNewQuery() {
        val policy = LiveBrowseRequestPolicy()
        val first = assertNotNull(policy.refresh())
        policy.invalidate()
        assertFalse(policy.succeed(first))
        val newQuery = assertNotNull(policy.refresh())
        assertEquals(1, newQuery.page)
        assertTrue(policy.succeed(newQuery, followingPage = 4))
        val append = assertNotNull(policy.loadMore(hasMore = true))
        assertEquals(4, append.page)
        policy.invalidate()
        val replacement = assertNotNull(policy.refresh())
        assertFalse(policy.fail(append))
        assertTrue(policy.owns(replacement))
        assertTrue(policy.succeed(replacement))
        assertNull(policy.loadMore(hasMore = false))
        assertEquals(2, assertNotNull(policy.loadMore(hasMore = true)).page)
    }

    @Test
    fun failedFirstPageCanRefreshAgainAtPageOne() {
        val policy = LiveBrowseRequestPolicy()
        val failed = assertNotNull(policy.refresh())
        assertTrue(policy.fail(failed))
        val retry = assertNotNull(policy.refresh())
        assertEquals(1, retry.page)
        assertFalse(retry.append)
        assertTrue(policy.succeed(retry))
    }
}
