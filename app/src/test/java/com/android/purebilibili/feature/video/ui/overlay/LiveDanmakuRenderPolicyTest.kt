package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDanmakuRenderPolicyTest {
    @Test
    fun emptyBatchDoesNotAppendAndTextBatchRetainsMessageOrder() {
        val appended = mutableListOf<List<String>>()
        appendLiveDanmakuBatch(emptyList<String>()) { appended.add(it) }
        appendLiveDanmakuBatch(listOf("[直播表情]", "文字")) { appended.add(it) }
        assertEquals(listOf(listOf("[直播表情]", "文字")), appended)
    }
}
