package com.android.purebilibili.core.ui.transition

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingBarHandoffPolicyTest {
    @Test
    fun `live bar takes pixels before outgoing entry disposal and reverses with gesture`() {
        var depth = 0.02f
        val handoff = NowPlayingBarHandoffState.Returning(
            targetBvid = "BV123",
            isSourceOwner = true,
            morphDepthProvider = { depth },
        )
        assertEquals(0f, resolveNowPlayingBarReturnVisibility(handoff, "BV123"), 0.001f)
        depth = 0.01f
        assertEquals(0.5f, resolveNowPlayingBarReturnVisibility(handoff, "BV123"), 0.001f)
        depth = 0f
        assertEquals(1f, resolveNowPlayingBarReturnVisibility(handoff, "BV123"), 0.001f)
        depth = 0.02f
        assertEquals(0f, resolveNowPlayingBarReturnVisibility(handoff, "BV123"), 0.001f)
    }

    @Test
    fun capsuleCornerTracksActualHeightAndCompactWidth() {
        assertEquals(28, resolveNowPlayingBarSourceCornerDp(900f, 168f, 3f))
        assertEquals(32, resolveNowPlayingBarSourceCornerDp(900f, 192f, 3f))
        assertEquals(20, resolveNowPlayingBarSourceCornerDp(120f, 192f, 3f))
        assertEquals(0, resolveNowPlayingBarSourceCornerDp(900f, 192f, 0f))
    }


    @Test
    fun `idle handoff leaves the bar fully visible`() {
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(
                handoff = NowPlayingBarHandoffState.Idle,
                currentBvid = "BV123",
            ),
        )
    }

    @Test
    fun `return morph hides the real bar only for the source owner`() {
        val returning = NowPlayingBarHandoffState.Returning(
            targetBvid = "BV123",
            isSourceOwner = true,
        )
        assertEquals(
            0f,
            resolveNowPlayingBarReturnVisibility(handoff = returning, currentBvid = "BV123"),
        )
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(
                handoff = returning.copy(isSourceOwner = false),
                currentBvid = "BV123",
            ),
        )
    }

    @Test
    fun `return to another video does not hide this bar`() {
        val returning = NowPlayingBarHandoffState.Returning(
            targetBvid = "BV_OTHER",
            isSourceOwner = true,
        )
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(handoff = returning, currentBvid = "BV123"),
        )
    }

    @Test
    fun `blank target bvid makes every bar yield to the return morph`() {
        val returning = NowPlayingBarHandoffState.Returning(
            targetBvid = null,
            isSourceOwner = true,
        )
        assertEquals(
            0f,
            resolveNowPlayingBarReturnVisibility(handoff = returning, currentBvid = "BV123"),
        )
    }

    @Test
    fun `blank current bvid never hides`() {
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(
                handoff = NowPlayingBarHandoffState.Returning(
                    targetBvid = "BV123",
                    isSourceOwner = true,
                ),
                currentBvid = "",
            ),
        )
    }

    @Test
    fun `bar chrome follows the standard source chrome window`() {
        assertEquals(0f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 1f))
        // 壳仍接近详情页大小时不显现，避免文字被 inverse scale 放大成巨字。
        assertEquals(0f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0.7f))
        assertEquals(0.5f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0.1f), 0.001f)
        assertEquals(1f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0.02f))
        assertEquals(1f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0f))
    }

    @Test
    fun `bar layout reaches endpoint before snapshot chrome becomes visible`() {
        val revealStart = VideoCardTransitionVisualTimeline.SOURCE_CHROME_RETURN_START
        val depthAtRevealStart = 1f - revealStart
        assertEquals(1f, resolveNowPlayingBarMediaLayoutHandoff(depthAtRevealStart), 0.001f)
        assertEquals(0f, resolveNowPlayingBarSourceChromeReveal(depthAtRevealStart), 0.001f)
        assertEquals(1f, resolveNowPlayingBarMediaLayoutHandoff(0.1f), 0.001f)
        assertEquals(0.5f, resolveNowPlayingBarSourceChromeReveal(0.1f), 0.001f)
    }

    @Test
    fun `reversing return moves snapshot layout back along the same timeline`() {
        assertEquals(0f, resolveNowPlayingBarMediaLayoutHandoff(1f), 0.001f)
        assertEquals(0.5f, resolveNowPlayingBarMediaLayoutHandoff(0.59f), 0.001f)
        assertEquals(1f, resolveNowPlayingBarMediaLayoutHandoff(0f), 0.001f)
        assertEquals(0.5f, resolveNowPlayingBarMediaLayoutHandoff(0.59f), 0.001f)
    }

    @Test
    fun `loading detail shell fill ramps over the early settle window`() {
        assertEquals(0f, resolveSourceShellFillReveal(morphDepthProgress = 1f))
        assertEquals(0.5f, resolveSourceShellFillReveal(morphDepthProgress = 0.91f), 0.001f)
        assertEquals(1f, resolveSourceShellFillReveal(morphDepthProgress = 0.82f))
        assertEquals(1f, resolveSourceShellFillReveal(morphDepthProgress = 0f))
    }
}
