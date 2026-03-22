package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheClearUiPolicyTest {

    @Test
    fun markAnimationCompleteOnlyWhenClearSucceeded() {
        assertTrue(shouldMarkCacheClearAnimationComplete(clearSucceeded = true))
        assertFalse(shouldMarkCacheClearAnimationComplete(clearSucceeded = false))
    }

    @Test
    fun resolveFailureMessageWithFallback() {
        assertEquals(
            "清理缓存失败，请稍后重试",
            resolveCacheClearFailureMessage(null)
        )
        assertEquals(
            "磁盘被占用",
            resolveCacheClearFailureMessage(IllegalStateException("磁盘被占用"))
        )
    }

    @Test
    fun resolveCacheClearConfirmationMessage_mentionsExpandedSafeScope() {
        assertEquals(
            "确定要清除图片、网络、字幕/弹幕缓存和应用临时文件吗？不会删除离线缓存、下载内容和播放记录",
            resolveCacheClearConfirmationMessage()
        )
    }
}
