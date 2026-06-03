package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandDanmakuOverlayPolicyTest {

    @Test
    fun `attention command labels match action type`() {
        assertEquals("一键三连", resolveAttentionCommandLabel(1))
        assertEquals("关注并三连", resolveAttentionCommandLabel(2))
        assertEquals("关注 UP", resolveAttentionCommandLabel(0))
    }

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
    fun `follow and triple attention card has enough width for portrait details`() {
        assertTrue(resolveAttentionCommandCardWidthDp(2) > resolveAttentionCommandCardWidthDp(1))
        assertTrue(resolveAttentionCommandCardWidthDp(2) >= 188)
    }

    @Test
    fun `command card horizontal offset is clamped inside player bounds`() {
        val containerWidthPx = 1080
        val cardWidthPx = 588

        assertEquals(492, resolveCommandDanmakuHorizontalOffsetPx(containerWidthPx, cardWidthPx, 0.82f))
        assertEquals(0, resolveCommandDanmakuHorizontalOffsetPx(containerWidthPx, cardWidthPx, -0.2f))
    }

    @Test
    fun `attention command container is transparent while info commands keep readable scrim`() {
        assertEquals(Color.Transparent, resolveCommandDanmakuContainerColor(CommandDanmakuType.ATTENTION))
        assertTrue(resolveCommandDanmakuContainerColor(CommandDanmakuType.UP).alpha > 0.5f)
    }
}
