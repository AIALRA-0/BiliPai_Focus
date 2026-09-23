package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.WatchLaterItem
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterStatusPolicyTest {

    @Test
    fun `watch later status matches the current aid from server items`() {
        assertTrue(isWatchLaterAid(listOf(WatchLaterItem(aid = 42L)), 42L))
        assertFalse(isWatchLaterAid(listOf(WatchLaterItem(aid = 41L)), 42L))
    }
}
