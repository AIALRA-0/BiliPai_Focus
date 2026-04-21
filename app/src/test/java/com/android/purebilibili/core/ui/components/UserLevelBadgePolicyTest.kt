package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserLevelBadgePolicyTest {

    @Test
    fun `resolveUserLevelBadgeAsset maps normal levels`() {
        assertEquals(
            UserLevelBadgeAsset.LEVEL_4,
            resolveUserLevelBadgeAsset(level = 4, isSeniorMember = false)
        )
    }

    @Test
    fun `resolveUserLevelBadgeAsset prefers senior level six asset`() {
        assertEquals(
            UserLevelBadgeAsset.LEVEL_6_SENIOR,
            resolveUserLevelBadgeAsset(level = 6, isSeniorMember = true)
        )
    }

    @Test
    fun `resolveUserLevelBadgeAsset rejects out of range levels`() {
        assertNull(resolveUserLevelBadgeAsset(level = -1, isSeniorMember = false))
        assertNull(resolveUserLevelBadgeAsset(level = 7, isSeniorMember = false))
    }
}
