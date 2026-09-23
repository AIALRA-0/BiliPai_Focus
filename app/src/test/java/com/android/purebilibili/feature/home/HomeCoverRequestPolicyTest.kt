package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeCoverRequestPolicyTest {

    @Test
    fun `normal cover keeps sampling margin and selects nearest sufficient tier`() {
        val spec = resolveHomeCoverRequestSpec(
            cardWidthDp = 180f,
            density = 3f,
            useLowQualityCover = false,
        )

        assertEquals(960, spec.widthPx)
        assertEquals(600, spec.heightPx)
        assertEquals("960x600", spec.cacheKeySuffix)
    }

    @Test
    fun `large cover clamps to largest tier instead of requesting original`() {
        val spec = resolveHomeCoverRequestSpec(
            cardWidthDp = 600f,
            density = 3f,
            useLowQualityCover = false,
        )

        assertEquals(1280, spec.widthPx)
        assertTrue(spec.resolveUrl("https://example.com/cover.jpg").endsWith("@1280w_800h.webp"))
    }

    @Test
    fun `data saver continues using 240 tier`() {
        val spec = resolveHomeCoverRequestSpec(
            cardWidthDp = 600f,
            density = 4f,
            useLowQualityCover = true,
        )

        assertEquals(HomeCoverRequestSpec(240, 150), spec)
    }
}
