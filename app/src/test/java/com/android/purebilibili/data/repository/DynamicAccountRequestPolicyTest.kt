package com.android.purebilibili.data.repository

import com.android.purebilibili.core.store.AccountSessionIdentity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicAccountRequestPolicyTest {
    @Test
    fun oldAccountResultIsRejectedAfterSwitching() {
        val first = AccountSessionIdentity(mid = 101L, generation = 1L)
        val second = AccountSessionIdentity(mid = 202L, generation = 2L)
        assertFalse(isDynamicAccountRequestCurrent(first, second, 202L))
    }

    @Test
    fun switchingBackDoesNotReviveAnEarlierRequest() {
        val earlier = AccountSessionIdentity(mid = 101L, generation = 1L)
        val returned = AccountSessionIdentity(mid = 101L, generation = 3L)
        assertFalse(isDynamicAccountRequestCurrent(earlier, returned, 101L))
        assertTrue(isDynamicAccountRequestCurrent(returned, returned, 101L))
    }

    @Test
    fun requestIsRejectedWhileCredentialsAreBeingReplaced() {
        val identity = AccountSessionIdentity(mid = 101L, generation = 1L)
        assertFalse(isDynamicAccountRequestCurrent(identity, identity, 202L))
    }
}
