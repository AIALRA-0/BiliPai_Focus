package com.android.purebilibili.feature.anime4k

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEnhancementConfigLoadGuardTest {
    @Test
    fun `首次启用前没有本地修改时读取已保存配置`() {
        val guard = VideoEnhancementConfigLoadGuard()

        assertTrue(guard.shouldApplyLoadedConfig())
    }

    @Test
    fun `首次启用前刚选择算法时不允许旧配置覆盖`() {
        val guard = VideoEnhancementConfigLoadGuard()

        guard.markLocalChange()

        assertFalse(guard.shouldApplyLoadedConfig())
    }

    @Test
    fun `读取配置期间发生本地修改时仍保留当前选择`() {
        val guard = VideoEnhancementConfigLoadGuard()
        assertTrue(guard.shouldApplyLoadedConfig())

        guard.markLocalChange()

        assertFalse(guard.shouldApplyLoadedConfig())
    }
}
