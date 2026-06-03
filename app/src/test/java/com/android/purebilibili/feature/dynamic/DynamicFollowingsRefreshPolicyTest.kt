package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFollowingsRefreshPolicyTest {

    @Test
    fun shouldReloadFollowings_returnsTrueWhenNeverLoaded() {
        assertTrue(
            shouldReloadFollowings(
                nowMs = 1_000L,
                lastLoadMs = 0L,
                ttlMs = 300_000L
            )
        )
    }

    @Test
    fun shouldReloadFollowings_returnsFalseWithinTtlWindow() {
        assertFalse(
            shouldReloadFollowings(
                nowMs = 250_000L,
                lastLoadMs = 100_000L,
                ttlMs = 300_000L
            )
        )
    }

    @Test
    fun shouldReloadFollowings_returnsTrueAfterTtlExpires() {
        assertTrue(
            shouldReloadFollowings(
                nowMs = 450_001L,
                lastLoadMs = 100_000L,
                ttlMs = 300_000L
            )
        )
    }
}
