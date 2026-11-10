package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DynamicAccountCachePolicyTest {
    @Test
    fun accountsAndGuestNeverShareTheSameCacheFile() {
        val first = dynamicAccountStorageName("dynamic_cache", 101L)
        val second = dynamicAccountStorageName("dynamic_cache", 202L)
        val guest = dynamicAccountStorageName("dynamic_cache", null)

        assertNotEquals(first, second)
        assertNotEquals(first, guest)
        assertNotEquals("dynamic_cache", first)
        assertEquals(first, dynamicAccountStorageName("dynamic_cache", 101L))
    }

    @Test
    fun invalidAccountIdsUseGuestStorage() {
        assertEquals("dynamic_users_guest", dynamicAccountStorageName("dynamic_users", 0L))
        assertEquals("dynamic_users_guest", dynamicAccountStorageName("dynamic_users", -1L))
    }
}
