package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileWallpaperPolicyTest {

    @Test
    fun profileSkinVideoHonorsOnceAndLoopPlayModes() {
        assertEquals(Player.REPEAT_MODE_OFF, resolveProfileSkinVideoRepeatMode("once"))
        assertEquals(Player.REPEAT_MODE_ONE, resolveProfileSkinVideoRepeatMode("loop"))
        assertEquals(Player.REPEAT_MODE_ONE, resolveProfileSkinVideoRepeatMode(null))
    }

    @Test
    fun compactProfileTopBanner_usesHeroFractionAndClamp() {
        val compactHeight = resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Compact)
        assertEquals(resolveProfileLayoutTokens().heroMinHeightDp.toFloat(), compactHeight, 0.001f)
    }

    @Test
    fun profileTopBannerHeight_staysInsideHeroClampForAllBreakpoints() {
        WindowWidthSizeClass.entries.forEach { sizeClass ->
            val height = resolveProfileTopBannerHeightDp(sizeClass)
            assertTrue(height in 280f..360f)
        }
    }

    @Test
    fun profileStaticBackground_isRetainedDuringBottomPagerTransition() {
        assertEquals(
            true,
            shouldRenderProfileImmersiveBackground(
                hasTopPhoto = true,
                deferImmersiveRenderBudget = true
            )
        )
        assertEquals(
            true,
            shouldRenderProfileImmersiveBackground(
                hasTopPhoto = true,
                deferImmersiveRenderBudget = false
            )
        )
        assertEquals(
            false,
            shouldRenderProfileImmersiveBackground(
                hasTopPhoto = false,
                deferImmersiveRenderBudget = false
            )
        )
    }
}
