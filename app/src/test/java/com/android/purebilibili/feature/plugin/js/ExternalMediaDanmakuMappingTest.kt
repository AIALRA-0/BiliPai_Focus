package com.android.purebilibili.feature.plugin.js

import com.android.purebilibili.core.plugin.js.BiliPaiJsDanmuComment
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_BOTTOM
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_TOP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalMediaDanmakuMappingTest {

    @Test
    fun mapsModeTextTimeAndColorIntoEngineItems() {
        val items = mapJsDanmuCommentsToItems(
            listOf(
                BiliPaiJsDanmuComment(timeMs = 1_000L, text = "滚动", mode = "scroll", color = "#FFFFFF"),
                BiliPaiJsDanmuComment(timeMs = 2_000L, text = "顶部", mode = "top"),
                BiliPaiJsDanmuComment(timeMs = 3_000L, text = "底部", mode = "bottom", color = "16711680"),
                BiliPaiJsDanmuComment(timeMs = -5L, text = "负时间", mode = "TOP")
            )
        )

        assertEquals(4, items.size)
        assertEquals(DANMAKU_LAYER_SCROLL, items[0].layerType)
        assertEquals(0xFFFFFFFF.toInt(), items[0].textColor)
        assertEquals(DANMAKU_LAYER_TOP, items[1].layerType)
        assertEquals(null, items[1].textColor)
        assertEquals(DANMAKU_LAYER_BOTTOM, items[2].layerType)
        assertEquals(16711680, items[2].textColor)
        assertEquals(DANMAKU_LAYER_TOP, items[3].layerType)
        assertEquals(0L, items[3].showAtTime)
    }

    @Test
    fun blankTextCommentsAreDropped() {
        val items = mapJsDanmuCommentsToItems(
            listOf(
                BiliPaiJsDanmuComment(timeMs = 1L, text = "   "),
                BiliPaiJsDanmuComment(timeMs = 2L, text = "有效")
            )
        )

        assertEquals(1, items.size)
        assertEquals("有效", items[0].text)
    }

    @Test
    fun idsStayStablePerListIndex() {
        val first = mapJsDanmuCommentsToItems(
            listOf(BiliPaiJsDanmuComment(timeMs = 1L, text = "a"), BiliPaiJsDanmuComment(timeMs = 2L, text = "b"))
        )
        val second = mapJsDanmuCommentsToItems(
            listOf(BiliPaiJsDanmuComment(timeMs = 1L, text = "a"))
        )

        assertEquals(first[0].danmakuId, second[0].danmakuId)
        assertTrue(first[1].danmakuId != first[0].danmakuId)
    }

    @Test
    fun parsesColorFormatsAndFallsBackToNull() {
        assertEquals(0xFF112233.toInt(), parseJsDanmuColorInt("#112233"))
        assertEquals(0x80112233.toInt(), parseJsDanmuColorInt("#80112233"))
        assertEquals(255, parseJsDanmuColorInt("255"))
        assertNull(parseJsDanmuColorInt(""))
        assertNull(parseJsDanmuColorInt("#12345"))
        assertNull(parseJsDanmuColorInt("not-a-color"))
    }
}
