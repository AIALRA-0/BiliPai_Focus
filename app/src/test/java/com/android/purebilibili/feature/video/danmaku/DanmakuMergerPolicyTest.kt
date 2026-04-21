package com.android.purebilibili.feature.video.danmaku

import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuMergerPolicyTest {

    @Test
    fun merge_repeatedDanmakuStaysStandardAndDoesNotCreateYellowAdvancedDanmaku() {
        val items = List(5) { index ->
            TextData().apply {
                text = "重复弹幕"
                showAtTime = index * 100L
                textColor = 0xFFFFFFFF.toInt()
                layerType = com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
            }
        }

        val (standard, advanced) = DanmakuMerger.merge(items, intervalMs = 500L)

        assertEquals(1, standard.size)
        assertTrue(advanced.isEmpty())
        assertEquals("重复弹幕 x5", (standard.first() as TextData).text)
    }
}
