package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandDanmakuOverlayPolicyTest {

    @Test
    fun `follow and triple command does not unfollow existing followers`() {
        val action = resolveAttentionCommandClickAction(attentionType = 2, isFollowing = true)

        assertEquals(false, action.shouldFollow)
        assertEquals(true, action.shouldTriple)
    }

    @Test
    fun `follow and triple command follows first when not following`() {
        val action = resolveAttentionCommandClickAction(attentionType = 2, isFollowing = false)

        assertEquals(true, action.shouldFollow)
        assertEquals(true, action.shouldTriple)
    }

    @Test
    fun `follow only command ignores already followed author`() {
        val action = resolveAttentionCommandClickAction(attentionType = 0, isFollowing = true)

        assertEquals(false, action.shouldFollow)
        assertEquals(false, action.shouldTriple)
    }

    @Test
    fun `command card horizontal offset is clamped inside player bounds`() {
        val containerWidthPx = 1080
        val cardWidthPx = 588

        assertEquals(492, resolveCommandDanmakuHorizontalOffsetPx(containerWidthPx, cardWidthPx, 0.82f))
        assertEquals(0, resolveCommandDanmakuHorizontalOffsetPx(containerWidthPx, cardWidthPx, -0.2f))
    }

    @Test
    fun `command card width is capped by a narrow player viewport`() {
        assertEquals(320, resolveCommandDanmakuCardWidthPx(320, 420))
        assertEquals(0, resolveCommandDanmakuCardWidthPx(320, -1))
    }

    @Test
    fun `command card vertical offset is clamped by measured card height`() {
        assertEquals(192, resolveCommandDanmakuVerticalOffsetPx(320, 128, 0.8f))
        assertEquals(0, resolveCommandDanmakuVerticalOffsetPx(320, 400, 0.8f))
        assertEquals(0, resolveCommandDanmakuVerticalOffsetPx(320, 128, -0.2f))
    }

    @Test
    fun `attention command container is transparent while info commands keep readable scrim`() {
        assertEquals(Color.Transparent, resolveCommandDanmakuContainerColor(CommandDanmakuType.ATTENTION))
        assertTrue(resolveCommandDanmakuContainerColor(CommandDanmakuType.UP).alpha > 0.5f)
    }
}
