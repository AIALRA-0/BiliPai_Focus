package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.danmaku.VoteOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandDanmakuOverlayStateTest {
    @Test
    fun `selected commands cannot submit again`() {
        val state = CommandDanmakuOverlayState()
        val option = VoteOption("four", "four", 8)
        assertTrue(state.select("grade", option))
        assertFalse(state.select("grade", VoteOption("five", "five", 10)))
        assertEquals(option, state.selection("grade"))
    }

    @Test
    fun `dismissed commands stay dismissed without affecting another command`() {
        val state = CommandDanmakuOverlayState()
        state.dismiss("first")
        assertTrue(state.isDismissed("first"))
        assertFalse(state.select("first", VoteOption("one", "one", 2)))
        assertNull(state.selection("first"))
        assertTrue(state.select("second", VoteOption("five", "five", 10)))
        assertFalse(state.isDismissed("second"))
    }
}
