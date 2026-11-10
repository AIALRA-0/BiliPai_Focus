package com.android.purebilibili.feature.space

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpaceScrollPolicyTest {
    @Test
    fun `progressive blur retains flowing tabs while disabled mode pins them`() {
        assertFalse(shouldPinSpaceContentTabs(progressiveTopBlurEnabled = true))
        assertTrue(shouldPinSpaceContentTabs(progressiveTopBlurEnabled = false))
        assertFalse(shouldPinSpaceContentTabs(progressiveTopBlurEnabled = false, headerBlurEnabled = true))
        assertFalse(shouldPinSpaceContentTabs(progressiveTopBlurEnabled = true, headerBlurEnabled = true))
        assertTrue(shouldPinSpaceContentTabs(progressiveTopBlurEnabled = false, headerBlurEnabled = false))
    }

    @Test
    fun `solid top fade shares flowing layout with either blur and pins only when all are off`() {
        for (progressiveBlur in listOf(false, true)) {
            for (headerBlur in listOf(false, true)) {
                for (solidFade in listOf(false, true)) {
                    assertEquals(
                        !progressiveBlur && !headerBlur && !solidFade,
                        shouldPinSpaceContentTabs(
                            progressiveTopBlurEnabled = progressiveBlur,
                            headerBlurEnabled = headerBlur,
                            progressiveTopFadeEnabled = solidFade,
                        ),
                        "progressive=$progressiveBlur, header=$headerBlur, fade=$solidFade",
                    )
                }
            }
        }
    }

    @Test
    fun `tabs approach toolbar naturally and stop below its actual inset`() {
        assertFalse(shouldShowSpacePinnedTabs(240, 96, 96, 0))
        assertFalse(shouldShowSpacePinnedTabs(1, 96, 96, 0))
        assertTrue(shouldShowSpacePinnedTabs(0, 96, 96, 1))
        assertTrue(shouldShowSpacePinnedTabs(-40, 96, 96, 1))
        assertFalse(shouldShowSpacePinnedTabs(0, 144, 96, 1))
        assertFalse(shouldShowSpacePinnedTabs(null, 96, 96, 0))
        assertTrue(shouldShowSpacePinnedTabs(null, 96, 96, 2))
        // 回滚后原标签重新进入视口，顶层副本应撤回。
        assertFalse(shouldShowSpacePinnedTabs(40, 96, 96, 0))
    }

    @Test
    fun `long and short profiles fade only during final toolbar height`() {
        assertEquals(0f, resolveSpaceHeaderClosingFraction(640f, 56f))
        assertEquals(0f, resolveSpaceHeaderClosingFraction(56f, 56f))
        assertEquals(0.5f, resolveSpaceHeaderClosingFraction(28f, 56f))
        assertEquals(1f, resolveSpaceHeaderClosingFraction(0f, 56f))
        assertEquals(1f, resolveSpaceHeaderClosingFraction(-10f, 56f))
        assertEquals(0.5f, resolveSpaceHeaderClosingFraction(84f, 168f))
    }

}
