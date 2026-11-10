package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QualitySelectionPolicyTest {
    @Test
    fun `actual server streams override stale membership and guest login flags`() {
        assertNull(qualityUnavailableReason(120, listOf(120, 80), isLoggedIn = true, isVip = false))
        assertNull(qualityUnavailableReason(80, listOf(80, 64), isLoggedIn = false, isVip = false))
    }

    @Test
    fun `advertised tiers without streams stay disabled even for vip or empty catalogue`() {
        assertNotNull(qualityUnavailableReason(120, listOf(80, 64), isLoggedIn = true, isVip = true))
        assertNotNull(qualityUnavailableReason(64, emptyList(), isLoggedIn = true, isVip = true))
        assertEquals("需要大会员", qualityUnavailableReason(120, listOf(80), true, false))
    }
}
