package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionRepositoryFollowStateChangeAccountPolicyTest {

    @Test
    fun `follow state event remains tied to initiating account after account switch`() {
        val change = createFollowStateChangeIfAccountKnown(
            accountMid = 1001L,
            mid = 2001L,
            isFollowing = false
        )

        assertEquals(1001L, change?.accountMid)
        assertFalse(
            shouldApplyFollowStateChangeForAccount(
                change = requireNotNull(change),
                activeAccountMid = 1002L,
                isLoggedIn = true
            )
        )
        assertFalse(
            shouldApplyFollowStateChangeForAccount(
                change = requireNotNull(change),
                activeAccountMid = null,
                isLoggedIn = true
            )
        )
        assertTrue(
            shouldApplyFollowStateChangeForAccount(
                change = requireNotNull(change),
                activeAccountMid = 1001L,
                isLoggedIn = true
            )
        )
        assertFalse(
            shouldApplyFollowStateChangeForAccount(
                change = requireNotNull(change),
                activeAccountMid = 1001L,
                isLoggedIn = false
            )
        )
    }

    @Test
    fun `follow state event is not created without a confirmed account and target`() {
        assertNull(
            createFollowStateChangeIfAccountKnown(
                accountMid = null,
                mid = 2001L,
                isFollowing = true
            )
        )
        assertNull(
            createFollowStateChangeIfAccountKnown(
                accountMid = 0L,
                mid = 2001L,
                isFollowing = true
            )
        )
        assertNull(
            createFollowStateChangeIfAccountKnown(
                accountMid = 1001L,
                mid = 0L,
                isFollowing = true
            )
        )
    }
}
