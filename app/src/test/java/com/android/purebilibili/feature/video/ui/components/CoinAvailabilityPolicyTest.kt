package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.*
import org.junit.Test

class CoinAvailabilityPolicyTest {
    @Test
    fun `coin choices respect cap previous contribution and fractional balance`() {
        assertTrue(canGiveCoins(1, 1, 2, 10.0))
        assertFalse(canGiveCoins(2, 1, 2, 10.0))
        assertFalse(canGiveCoins(2, 0, 1, 10.0))
        assertFalse(canGiveCoins(1, 1, 1, 10.0))
        assertFalse(canGiveCoins(2, 0, 2, 1.9))
        assertFalse(canGiveCoins(1, 0, 2, 0.0))
        assertFalse(canGiveCoins(0, 0, 2, 10.0))
        assertFalse(canGiveCoins(1, 0, 2, -2.0))
        assertTrue(canGiveCoins(1, 0, 2, null))
    }
}
