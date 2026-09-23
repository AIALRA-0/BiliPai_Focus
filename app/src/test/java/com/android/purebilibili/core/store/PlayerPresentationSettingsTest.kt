package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerPresentationSettingsTest {

    @Test
    fun `invalid progress placement falls back to current layout`() {
        assertEquals(
            PlayerProgressPlacement.ABOVE_CONTROLS,
            PlayerProgressPlacement.fromValue(Int.MAX_VALUE)
        )
    }

    @Test
    fun `invalid bottom progress behavior falls back to hidden`() {
        assertEquals(
            BottomProgressBehavior.ALWAYS_HIDE,
            BottomProgressBehavior.fromValue(Int.MAX_VALUE)
        )
    }

    @Test
    fun `player controls remain visible by default`() {
        val settings = PlayerControlVisibilitySettings()
        assertTrue(settings.showCastButton)
        assertTrue(settings.showFollowButton)
    }
}
