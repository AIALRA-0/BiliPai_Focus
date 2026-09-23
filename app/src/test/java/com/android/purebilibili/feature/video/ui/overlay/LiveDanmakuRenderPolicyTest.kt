package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveDanmakuRenderPolicyTest {

    @Test
    fun `live batch only appends and never restarts engine`() {
        val appended = mutableListOf<List<Int>>()

        appendLiveDanmakuBatch(listOf(1, 2, 3), appended::add)

        assertEquals(listOf(listOf(1, 2, 3)), appended)
    }

    @Test
    fun `plain text avoids bitmap while rich messages keep bitmap path`() {
        assertFalse(shouldRenderLiveDanmakuAsBitmap(isSuperChat = false, emoticonUrl = null))
        assertTrue(shouldRenderLiveDanmakuAsBitmap(isSuperChat = true, emoticonUrl = null))
        assertTrue(shouldRenderLiveDanmakuAsBitmap(isSuperChat = false, emoticonUrl = "https://example.test/e.png"))
    }
}
