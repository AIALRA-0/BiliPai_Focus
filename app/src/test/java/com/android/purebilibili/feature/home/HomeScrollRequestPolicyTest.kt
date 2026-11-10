package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeScrollRequestPolicyTest {
    private val request = HomeScrollRequestIdentity(
        HomeTopTabEntry.Category(HomeCategory.RECOMMEND), null, 7L,
    )

    @Test
    fun visibleSettledPageAcceptsItsCurrentRequest() {
        assertTrue(shouldApplyHomeScrollRequest(request, request, true, false))
    }

    @Test
    fun switchingAwayAndReturningDoesNotReviveOldRequest() {
        assertFalse(shouldApplyHomeScrollRequest(request, request.copy(navigationRevision = 9L), true, false))
        val otherPage = request.copy(entry = HomeTopTabEntry.Category(HomeCategory.LIVE))
        assertFalse(shouldApplyHomeScrollRequest(request, otherPage, true, false))
    }

    @Test
    fun hiddenMovingAndRemovedPagesRejectRequests() {
        assertFalse(shouldApplyHomeScrollRequest(request, request, false, false))
        assertFalse(shouldApplyHomeScrollRequest(request, request, true, true))
        val removed = request.copy(entry = null)
        assertFalse(shouldApplyHomeScrollRequest(removed, removed, true, false))
    }

    @Test
    fun popularSubcategoryChangeInvalidatesRequestBeforePagerChanges() {
        val popular = request.copy(
            entry = HomeTopTabEntry.Category(HomeCategory.POPULAR),
            popularSubCategory = PopularSubCategory.COMPREHENSIVE,
        )
        assertFalse(shouldApplyHomeScrollRequest(
            popular, popular.copy(popularSubCategory = PopularSubCategory.RANKING), true, false,
        ))
    }
}
