package com.android.purebilibili.data.repository

import org.junit.Assert.*
import org.junit.Test

class FollowedLivePaginationPolicyTest {
    @Test
    fun `ten returned rooms without metadata do not end pagination`() {
        assertTrue(hasMoreFollowedLivePages(1, 10, 0))
        assertTrue(hasMoreFollowedLivePages(2, 3, 0))
        assertFalse(hasMoreFollowedLivePages(3, 0, 0))
    }
    @Test
    fun `explicit last page and empty responses terminate`() {
        assertTrue(hasMoreFollowedLivePages(1, 10, 3))
        assertFalse(hasMoreFollowedLivePages(3, 10, 3))
        assertFalse(hasMoreFollowedLivePages(2, 0, 3))
    }
}
