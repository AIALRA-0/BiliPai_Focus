package com.android.purebilibili.danmaku.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuClickCaptureTest {
    @Test
    fun cancelledPress_cannotOpenMenu_andReleasesOnlyOnce() {
        var opened = 0
        var released = 0
        val press = DanmakuClickCapture({ opened++ }, { released++ })
        press.cancel()
        press()
        press.cancel()
        assertEquals(0, opened)
        assertEquals(1, released)
    }

    @Test
    fun confirmedPress_keepsItemFrozenUntilMenuDismissal() {
        var opened = 0
        var released = 0
        val press = DanmakuClickCapture({ opened++ }, { released++ })
        press()
        press()
        assertEquals(1, opened)
        assertEquals(0, released)
        press.cancel()
        assertEquals(1, released)
    }
}
