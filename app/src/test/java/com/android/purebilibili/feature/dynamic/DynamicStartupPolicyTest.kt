package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicStartupPolicyTest {

    @Test
    fun startupPlan_prefetchesFollowingsAlongsidePrimaryFeed() {
        val plan = resolveDynamicStartupLoadPlan()

        assertTrue(plan.refreshFeedImmediately)
        assertTrue(plan.loadLiveStatusImmediately)
        assertTrue(plan.loadFollowingsImmediately)
        assertEquals(0L, plan.followingsHydrationDelayMs)
        assertEquals(1, plan.initialFollowingsPageLimit)
    }

    @Test
    fun followingsPageBudget_isConservativeDuringStartupHydration() {
        assertEquals(1, resolveDynamicFollowingsPageLimit(isStartupHydration = true))
        assertEquals(3, resolveDynamicFollowingsPageLimit(isStartupHydration = false))
    }
}
