package com.android.bilipai.tv.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TvDanmakuRenderConfigTest {

    @Test fun `default display area is the box friendly half screen`() {
        assertEquals(0.5f, TV_DANMAKU_DEFAULT_DISPLAY_AREA)
    }

    @Test fun `text size scales with density only`() {
        assertEquals(60f, resolveTvDanmakuTextSizePx(3f), 0.001f)
        assertEquals(20f, resolveTvDanmakuTextSizePx(1f), 0.001f)
    }

    @Test fun `line count balances estimate viewport budget and area minimum`() {
        // 1080p、density 1.5：textSize=30px、行高 48px、半屏可见带 540px
        // 预算 = (540-48)/48+1 = 11；估算 = 540/((30+1.5+12)x1.6) = 7；下限 3 → 取 7
        assertEquals(7, resolveTvDanmakuLineCount(1080, 0.5f, resolveTvDanmakuTextSizePx(1.5f)))
    }

    @Test fun `quarter area keeps a readable line minimum`() {
        val count = resolveTvDanmakuLineCount(1080, 0.25f, resolveTvDanmakuTextSizePx(1.5f))
        // 预算 = (270-48)/48+1 = 5；下限 2 → 预算内取估算 3 与下限 2 的较大者
        assertTrue(count in 2..5)
    }

    @Test fun `tiny viewports collapse to zero lines`() {
        assertEquals(0, resolveTvDanmakuLineCount(0, 0.5f, 30f))
        assertEquals(0, resolveTvDanmakuLineCount(1080, 0.5f, 0f))
    }
}
