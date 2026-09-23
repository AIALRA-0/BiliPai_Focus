package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveDanmakuBitmapLifecyclePolicyTest {

    @Test
    fun `controller attached live danmaku bitmaps should not be manually recycled`() {
        assertFalse(
            shouldManuallyRecycleLiveDanmakuBitmap(
                ownership = LiveDanmakuBitmapOwnership.CONTROLLER_ATTACHED
            )
        )
    }

    @Test
    fun `app queued live danmaku bitmaps can be manually recycled`() {
        assertTrue(
            shouldManuallyRecycleLiveDanmakuBitmap(
                ownership = LiveDanmakuBitmapOwnership.APP_QUEUE_ONLY
            )
        )
    }

    @Test
    fun `timeline discarded live danmaku bitmaps remain renderer owned`() {
        assertFalse(
            shouldManuallyRecycleLiveDanmakuBitmap(
                ownership = LiveDanmakuBitmapOwnership.TIMELINE_DISCARDED
            )
        )
    }
}
