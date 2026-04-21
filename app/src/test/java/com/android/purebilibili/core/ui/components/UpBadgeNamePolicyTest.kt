package com.android.purebilibili.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpBadgeNamePolicyTest {

    @Test
    fun `resolveUpStatsText returns null when both stats missing`() {
        assertNull(resolveUpStatsText(followerCount = null, videoCount = null))
        assertNull(resolveUpStatsText(followerCount = 0, videoCount = 0))
    }

    @Test
    fun `resolveUpStatsText joins follower and video count when available`() {
        assertEquals(
            "粉丝 1200 · 视频 56",
            resolveUpStatsText(followerCount = 1200, videoCount = 56)
        )
    }

    @Test
    fun `resolveUpStatsText keeps available part when only one stat exists`() {
        assertEquals(
            "粉丝 328",
            resolveUpStatsText(followerCount = 328, videoCount = null)
        )
        assertEquals(
            "视频 9",
            resolveUpStatsText(followerCount = null, videoCount = 9)
        )
    }

    @Test
    fun `up badge trailing slot stays reserved when requested`() {
        assertTrue(
            shouldRenderUpBadgeTrailingSlot(
                hasTrailingContent = false,
                reserveTrailingSlot = true
            )
        )
        assertTrue(
            shouldRenderUpBadgeTrailingSlot(
                hasTrailingContent = true,
                reserveTrailingSlot = false
            )
        )
        assertFalse(
            shouldRenderUpBadgeTrailingSlot(
                hasTrailingContent = false,
                reserveTrailingSlot = false
            )
        )
    }

    @Test
    fun `user up badge follows visibility setting`() {
        assertTrue(shouldRenderUserUpBadge(showUpBadge = true))
        assertFalse(shouldRenderUserUpBadge(showUpBadge = false))
    }

    @Test
    fun `user up badge spec matches pili plus small badge`() {
        assertEquals(
            UserUpBadgeVisualSpec(
                cornerRadiusDp = 3,
                horizontalPaddingDp = 3,
                verticalPaddingDp = 2,
                fontSp = 9
            ),
            resolveUserUpBadgeVisualSpec()
        )
    }
}
