package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.android.purebilibili.feature.video.danmaku.DanmakuTapAnchor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuActionToolbarPolicyTest {
    private val viewport = DanmakuTapAnchor(
        left = 20f, top = 50f, right = 120f, bottom = 75f, tapX = 25f,
        viewportLeft = 10f, viewportTop = 40f, viewportRight = 330f, viewportBottom = 220f,
    )

    @Test
    fun leftEdge_keepsToolbarInsidePlayerRatherThanWindow() {
        assertEquals(IntOffset(16, 81), resolveDanmakuToolbarPosition(
            viewport, IntSize(1000, 1000), IntSize(200, 40), 6,
        ))
    }

    @Test
    fun bottomRight_usesSpaceAboveSelectedItem() {
        val anchor = viewport.copy(top = 190f, bottom = 215f, tapX = 329f)
        assertEquals(IntOffset(124, 144), resolveDanmakuToolbarPosition(
            anchor, IntSize(1000, 1000), IntSize(200, 40), 6,
        ))
    }

    @Test
    fun narrowSplitWindow_neverPlacesToolbarOutsideVisibleWindow() {
        val anchor = viewport.copy(viewportRight = 200f, tapX = 195f)
        val position = resolveDanmakuToolbarPosition(anchor, IntSize(180, 300), IntSize(150, 48), 6)
        assertTrue(position.x >= 16)
        assertTrue(position.x + 150 <= 174)
        assertTrue(position.y >= 46)
        assertTrue(position.y + 48 <= 214)
    }
}
