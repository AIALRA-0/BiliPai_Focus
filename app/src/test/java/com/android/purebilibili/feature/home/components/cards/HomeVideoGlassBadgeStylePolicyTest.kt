package com.android.purebilibili.feature.home.components.cards

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeVideoGlassBadgeStylePolicyTest {

    @Test
    fun `cover badges stay visible but always use plain style after glass retirement`() {
        val policy = resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = true,
            showInfoGlassBadges = true
        )

        assertEquals(HomeVideoBadgeStyle.PLAIN, policy.coverStyle)
        assertEquals(HomeVideoBadgeStyle.PLAIN, policy.infoStyle)
    }

    @Test
    fun `legacy disabled preferences still resolve to plain badges`() {
        val policy = resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = false,
            showInfoGlassBadges = false
        )

        assertEquals(HomeVideoBadgeStyle.PLAIN, policy.coverStyle)
        assertEquals(HomeVideoBadgeStyle.PLAIN, policy.infoStyle)
    }
}
