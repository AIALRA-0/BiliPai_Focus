package com.android.purebilibili.feature.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSuperChatExpiryPolicyTest {

    @Test
    fun durationFallsBackWhenMissing() {
        assertEquals(DEFAULT_LIVE_SUPER_CHAT_DURATION_SEC, resolveLiveSuperChatDurationSec(0))
        assertEquals(DEFAULT_LIVE_SUPER_CHAT_DURATION_SEC, resolveLiveSuperChatDurationSec(-1))
        assertEquals(30, resolveLiveSuperChatDurationSec(30))
    }

    @Test
    fun remainingAndExpiryTrackElapsedTime() {
        assertEquals(25, resolveLiveSuperChatRemainingSec(durationSec = 30, elapsedSec = 5))
        assertFalse(shouldExpireLiveSuperChat(durationSec = 30, elapsedSec = 5))
        assertTrue(shouldExpireLiveSuperChat(durationSec = 30, elapsedSec = 30))
        assertTrue(shouldExpireLiveSuperChat(durationSec = 30, elapsedSec = 40))
    }

    @Test
    fun countdownFormatting() {
        assertEquals("0s", formatLiveSuperChatCountdown(0))
        assertEquals("9s", formatLiveSuperChatCountdown(9))
        assertEquals("1:05", formatLiveSuperChatCountdown(65))
    }

    @Test
    fun flashFollowsDanmakuSwitch() {
        assertTrue(
            shouldShowLiveSuperChatFlash(
                showMediaOverlays = true,
                isDanmakuEnabled = true,
                flashEnabled = true
            )
        )
        assertFalse(
            shouldShowLiveSuperChatFlash(
                showMediaOverlays = true,
                isDanmakuEnabled = false,
                flashEnabled = true
            )
        )
    }

    @Test
    fun flashRespectsIndependentSettingAndClearScreen() {
        assertFalse(
            shouldShowLiveSuperChatFlash(
                showMediaOverlays = true,
                isDanmakuEnabled = true,
                flashEnabled = false
            )
        )
        assertFalse(
            shouldShowLiveSuperChatFlash(
                showMediaOverlays = false,
                isDanmakuEnabled = true,
                flashEnabled = true
            )
        )
    }
}
