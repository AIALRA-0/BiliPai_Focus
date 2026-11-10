package com.android.purebilibili.feature.following

import com.android.purebilibili.data.model.response.FollowingUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FansListMergePolicyTest {
    @Test
    fun latePageCannotRestoreRemovedFansOrDiscardLoadedPages() {
        val existing = listOf(FollowingUser(mid = 1L), FollowingUser(mid = 2L), FollowingUser(mid = 3L))
        val latePage = listOf(FollowingUser(mid = 2L), FollowingUser(mid = 4L))
        assertEquals(listOf(1L, 3L, 4L), mergeFansPage(existing, latePage, setOf(2L)).map { it.mid })
        assertEquals(listOf(1L, 2L, 3L), existing.map { it.mid })
    }

    @Test
    fun replayingOverlappingPagesCanFillShiftedOffsetsButRepeatedTailStops() {
        assertTrue(shouldLoadMoreFans(page = 1, furthestLoadedPage = 3, incomingCount = 20, previousSize = 59, mergedSize = 59, total = 100))
        assertTrue(shouldLoadMoreFans(page = 2, furthestLoadedPage = 3, incomingCount = 20, previousSize = 59, mergedSize = 59, total = 100))
        assertFalse(shouldLoadMoreFans(page = 4, furthestLoadedPage = 3, incomingCount = 20, previousSize = 59, mergedSize = 59, total = 100))
        assertTrue(shouldLoadMoreFans(page = 4, furthestLoadedPage = 3, incomingCount = 20, previousSize = 59, mergedSize = 79, total = 100))
        assertFalse(shouldLoadMoreFans(page = 4, furthestLoadedPage = 3, incomingCount = 0, previousSize = 59, mergedSize = 59, total = 100))
        assertFalse(shouldLoadMoreFans(page = 4, furthestLoadedPage = 3, incomingCount = 20, previousSize = 80, mergedSize = 100, total = 100))
    }
    @Test
    fun refreshFailureRetriesTheFirstPageAndRetainsReplaceMode() {
        val refresh = resolveFansPageRequest(nextPage = 4, refresh = true, failed = null)
        assertEquals(FansPageRequest(1, true), refresh)
        val retry = resolveFansPageRequest(nextPage = 4, refresh = false, failed = refresh)
        assertEquals(refresh, retry)
        assertEquals(FansPageRequest(4, false), resolveFansPageRequest(4, refresh = false, failed = null))
    }

    @Test
    fun paginationRetryRetainsItsPageAndExplicitRefreshOverridesIt() {
        val failed = FansPageRequest(3, false)
        assertEquals(failed, resolveFansPageRequest(4, refresh = false, failed = failed))
        assertEquals(FansPageRequest(1, true), resolveFansPageRequest(4, refresh = true, failed = failed))
    }

}
