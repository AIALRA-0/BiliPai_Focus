package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntRect
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerPanelViewportTest {
    @Test
    fun `video ends at panel left including the window safe inset`() {
        val panel = IntRect(680, 10, 1180, 590)
        val inset = resolvePlayerPanelEndInsetPx(1200, listOf(panel))

        assertEquals(680, 1200 - inset)
        assertEquals(520, inset)
    }

    @Test
    fun `player with a different window origin uses its own right edge`() {
        val panel = IntRect(680, 10, 1180, 590)

        assertEquals(500, resolvePlayerPanelEndInsetPx(1180, listOf(panel)))
        assertEquals(0, resolvePlayerPanelEndInsetPx(600, listOf(panel)))
    }

    @Test
    fun `closing a panel restores the full video viewport`() {
        val viewport = PlayerPanelViewportState()
        val panel = Any()
        viewport.reserve(panel, IntRect(400, 0, 800, 600))
        assertEquals(400, viewport.endInsetPx(800))

        viewport.release(panel)

        assertEquals(0, viewport.endInsetPx(800))
    }

    @Test
    fun `overlapping panel lifetimes do not add widths or clear another reservation`() {
        val viewport = PlayerPanelViewportState()
        val episodes = Any()
        val settings = Any()
        viewport.reserve(episodes, IntRect(680, 0, 1180, 600))
        viewport.reserve(settings, IntRect(700, 0, 1180, 600))
        assertEquals(520, viewport.endInsetPx(1200))

        viewport.release(episodes)
        assertEquals(500, viewport.endInsetPx(1200))

        viewport.release(settings)
        assertEquals(0, viewport.endInsetPx(1200))
    }

    @Test
    fun `resizing a panel updates its reservation and empty panels reserve nothing`() {
        val viewport = PlayerPanelViewportState()
        val panel = Any()
        viewport.reserve(panel, IntRect(680, 0, 1180, 600))
        viewport.reserve(panel, IntRect(400, 0, 780, 600))
        assertEquals(400, viewport.endInsetPx(800))

        viewport.reserve(panel, IntRect(400, 0, 400, 600))
        assertEquals(0, viewport.endInsetPx(800))
    }
}
